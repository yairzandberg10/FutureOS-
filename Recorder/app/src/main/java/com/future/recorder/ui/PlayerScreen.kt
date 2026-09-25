package com.future.recorder.ui

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.recorder.data.Recordings
import com.future.recorder.data.formatDuration
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.subtleTextColor
import kotlinx.coroutines.delay
import java.io.File

/**
 * נגן הקלטה. OK - הפעלה/השהיה; חצים - 5 שניות אחורה/קדימה; 4/6 - 30 שניות.
 * סרגל ההתקדמות נשאר משמאל לימין גם בממשק RTL, כמו בכל נגני המדיה.
 */
@Composable
fun PlayerScreen(theme: FutureTheme, file: File, onBack: () -> Unit) {
    val context = LocalContext.current
    var durationMs by remember { mutableIntStateOf(0) }
    var positionMs by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }
    val player = remember(file) { MediaPlayer() }

    DisposableEffect(file) {
        runCatching {
            player.setDataSource(file.path)
            player.prepare()
            durationMs = player.duration
            player.setOnCompletionListener { isPlaying = false; positionMs = durationMs }
            player.start()
            isPlaying = true
        }.onFailure {
            Toast.makeText(context, "לא ניתן להשמיע את ההקלטה", Toast.LENGTH_SHORT).show()
        }
        onDispose { runCatching { player.stop() }; player.release() }
    }
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            positionMs = runCatching { player.currentPosition }.getOrDefault(positionMs)
            delay(200)
        }
    }

    fun toggle() {
        runCatching {
            if (player.isPlaying) { player.pause(); isPlaying = false }
            else {
                if (positionMs >= durationMs - 50) player.seekTo(0)
                player.start(); isPlaying = true
            }
        }
    }
    fun seekBy(deltaMs: Int) {
        val target = (runCatching { player.currentPosition }.getOrDefault(positionMs) + deltaMs).coerceIn(0, durationMs)
        runCatching { player.seekTo(target) }
        positionMs = target
    }

    val playButton = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { playButton.requestFocus() } }

    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    onOptionsKeyPress { menuOpen = !menuOpen }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.nativeKeyEvent.keyCode) {
                        android.view.KeyEvent.KEYCODE_DPAD_RIGHT -> { seekBy(5_000); true }
                        android.view.KeyEvent.KEYCODE_DPAD_LEFT -> { seekBy(-5_000); true }
                        android.view.KeyEvent.KEYCODE_6 -> { seekBy(30_000); true }
                        android.view.KeyEvent.KEYCODE_4 -> { seekBy(-30_000); true }
                        android.view.KeyEvent.KEYCODE_5 -> { toggle(); true }
                        else -> false
                    }
                }
        ) {
            ScreenTopBar(title = file.nameWithoutExtension, textColor = theme.textColor, accentColor = theme.accentColor, onBack = onBack)
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = FutureDimens.screenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                FutureAvatar(theme = theme, icon = FutureIcons.GraphicEq, size = 72.dp)
                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
                Text(
                    formatDuration(positionMs.toLong()),
                    color = theme.textColor,
                    fontSize = FutureTypography.display,
                    fontWeight = FutureTypography.weightLight,
                    fontFamily = FutureTypography.monoFamily,
                )
                Text(
                    "מתוך ${formatDuration(durationMs.toLong())}",
                    color = theme.mutedTextColor,
                    fontSize = FutureTypography.summary,
                )
                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ProgressBar(fraction = if (durationMs > 0) positionMs / durationMs.toFloat() else 0f, theme = theme)
                }
                Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
                FutureButton(
                    if (isPlaying) "השהה" else "הפעל", theme, ::toggle,
                    fillMaxWidth = true, focusRequester = playButton,
                )
            }
            Text(
                "חצים 5 שניות · 4/6 חצי דקה · אפשרויות",
                color = theme.subtleTextColor,
                fontSize = FutureTypography.caption,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(vertical = FutureDimens.spacingMd),
            )
        }
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = file.nameWithoutExtension) {
            FutureMenuRow("שתף", FutureIcons.Share, theme, { menuOpen = false; shareRecording(context, file) })
            FutureMenuRow("מחק", FutureIcons.Delete, theme, { menuOpen = false; confirmDelete = true }, destructive = true)
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            message = "למחוק את \"${file.nameWithoutExtension}\"?",
            theme = theme,
            onCancel = { confirmDelete = false },
            onConfirm = {
                confirmDelete = false
                runCatching { player.stop() }
                Recordings.delete(file)
                onBack()
            },
        )
    }
}

@Composable
private fun ProgressBar(fraction: Float, theme: FutureTheme) {
    val track = theme.textColor.copy(alpha = 0.15f)
    val fill = theme.readableAccentColor
    Box(modifier = Modifier.fillMaxWidth().height(12.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxWidth().height(6.dp)) {
            val r = CornerRadius(size.height / 2f)
            drawRoundRect(track, cornerRadius = r)
            drawRoundRect(fill, size = Size(size.width * fraction.coerceIn(0f, 1f), size.height), cornerRadius = r)
        }
    }
}
