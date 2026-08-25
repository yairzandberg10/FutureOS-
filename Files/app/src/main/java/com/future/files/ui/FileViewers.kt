package com.future.files.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.media.MediaPlayer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.files.data.FileRepository
import com.future.files.ui.theme.FutureTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File

@Composable
private fun ViewerHeader(title: String, theme: FutureTheme, onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        val bgColor by animateColorAsState(if (isFocused) theme.accentColor.copy(alpha = 0.3f) else theme.textColor.copy(alpha = 0.08f))
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(bgColor)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onBack)
                .focusable(interactionSource = interactionSource),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "חזור", tint = theme.textColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, color = theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun TextViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    var content by remember(file) { mutableStateOf<String?>(null) }

    LaunchedEffect(file) {
        content = withContext(Dispatchers.IO) {
            try {
                val maxBytes = 300_000L
                if (file.length() > maxBytes) {
                    file.readText().take(maxBytes.toInt()) + "\n\n… (הקובץ נחתך, גדול מדי להצגה מלאה)"
                } else {
                    file.readText()
                }
            } catch (e: Exception) {
                "לא ניתן לקרוא את הקובץ"
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            ViewerHeader(file.name, theme, onBack)
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp)) {
                Text(
                    text = content ?: "טוען...",
                    color = theme.textColor,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.TopStart)
                )
            }
        }
    }
}

@Composable
fun ImageFileViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(file) {
        bitmap = withContext(Dispatchers.IO) {
            try {
                BitmapFactory.decodeFile(file.absolutePath)
            } catch (e: Exception) {
                null
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
                ViewerBackChip(theme, onBack)
            }
        }
    }
}

@Composable
private fun ViewerBackChip(theme: FutureTheme, onBack: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor else Color.Black.copy(alpha = 0.5f))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onBack)
            .focusable(interactionSource = interactionSource)
            .padding(10.dp)
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "חזור", tint = if (isFocused) Color.Black else Color.White)
    }
}

@Composable
fun AudioPlayerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    val player = remember { MediaPlayer() }
    val context = androidx.compose.ui.platform.LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var isPrepared by remember { mutableStateOf(false) }
    var loadFailed by remember(file) { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0) }
    var positionMs by remember { mutableStateOf(0) }

    DisposableEffect(file) {
        try {
            player.reset()
            player.setDataSource(file.absolutePath)
            player.setOnPreparedListener {
                isPrepared = true
                durationMs = player.duration
            }
            player.setOnCompletionListener { isPlaying = false }
            player.setOnErrorListener { _, _, _ ->
                loadFailed = true
                android.widget.Toast.makeText(context, "לא ניתן לנגן קובץ זה", android.widget.Toast.LENGTH_SHORT).show()
                true
            }
            player.prepareAsync()
        } catch (e: Exception) {
            loadFailed = true
            android.widget.Toast.makeText(context, "לא ניתן לנגן קובץ זה", android.widget.Toast.LENGTH_SHORT).show()
        }
        onDispose {
            player.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = try { player.currentPosition } catch (e: Exception) { positionMs }
            delay(400)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            ViewerHeader(file.name, theme, onBack)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(140.dp).clip(RoundedCornerShape(28.dp)).background(theme.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    val interactionSource = remember { MutableInteractionSource() }
                    val isFocused by interactionSource.collectIsFocusedAsState()
                    val bgColor by animateColorAsState(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.6f))
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(bgColor)
                            .clickable(interactionSource = interactionSource, indication = null, enabled = isPrepared && !loadFailed) {
                                if (isPlaying) player.pause() else player.start()
                                isPlaying = !isPlaying
                            }
                            .focusable(interactionSource = interactionSource),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = if (isPlaying) "השהה" else "נגן",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(file.name, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 2)
                Spacer(modifier = Modifier.height(12.dp))
                if (loadFailed) {
                    Text("לא ניתן לנגן קובץ זה", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                }
                if (durationMs > 0) {
                    val progress = (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(theme.textColor.copy(alpha = 0.15f))) {
                        Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress).background(theme.accentColor, RoundedCornerShape(2.dp)))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${formatMs(positionMs)} / ${formatMs(durationMs)}", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            }
        }
    }
}

private fun formatMs(ms: Int): String {
    val totalSeconds = ms / 1000
    return "%d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}

@Composable
fun PdfViewerScreen(file: File, theme: FutureTheme, onBack: () -> Unit) {
    var pages by remember(file) { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember(file) { mutableStateOf(true) }

    LaunchedEffect(file) {
        pages = withContext(Dispatchers.IO) {
            try {
                ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                    PdfRenderer(pfd).use { renderer ->
                        (0 until renderer.pageCount).map { index ->
                            renderer.openPage(index).use { page ->
                                val scale = 2
                                val bitmap = Bitmap.createBitmap(page.width * scale, page.height * scale, Bitmap.Config.ARGB_8888)
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmap
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
        isLoading = false
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFF3A3A3C))) {
            ViewerHeader(file.name, theme, onBack)
            when {
                isLoading -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("טוען PDF...", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                pages.isEmpty() -> Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("לא ניתן להציג את הקובץ", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(pages) { pageBitmap ->
                        Image(
                            bitmap = pageBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth(),
                            contentScale = ContentScale.FillWidth
                        )
                    }
                }
            }
        }
    }
}
