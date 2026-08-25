package com.future.tools.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@OptIn(ExperimentalGetImage::class)
@Composable
fun TextScannerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val hasPermission by rememberRuntimePermission(Manifest.permission.CAMERA)
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    var liveText by remember { mutableStateOf("") }
    var isFrozen by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { recognizer.close() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(
                    title = "סורק טקסט", theme = theme, onBack = onBack,
                    trailing = {
                        if (liveText.isNotBlank()) {
                            ToolsIconButton(Icons.Rounded.ContentCopy, "העתק", theme = theme) {
                                val clipboard = context.getSystemService(ClipboardManager::class.java)
                                clipboard.setPrimaryClip(ClipData.newPlainText("scanned_text", liveText))
                                Toast.makeText(context, "הטקסט הועתק", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )

                if (!hasPermission) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("נדרשת הרשאת מצלמה כדי לסרוק טקסט", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                    ) {
                        if (!isFrozen) {
                            CameraAnalyzerView(modifier = Modifier.fillMaxSize()) { proxy ->
                                val mediaImage = proxy.image
                                if (mediaImage != null && !isProcessing) {
                                    isProcessing = true
                                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                                    recognizer.process(image)
                                        .addOnSuccessListener { result ->
                                            if (result.text.isNotBlank()) liveText = result.text
                                        }
                                        .addOnCompleteListener {
                                            isProcessing = false
                                            proxy.close()
                                        }
                                } else {
                                    proxy.close()
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize().background(theme.textColor.copy(alpha = 0.06f))) {
                                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                                    item {
                                        Text(liveText.ifBlank { "לא זוהה טקסט" }, color = theme.textColor, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        "התמיכה כרגע היא לטקסט לטיני וספרות (לא עברית) · הפניה למצלמה",
                        color = theme.textColor.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)) {
                        FreezeButton(isFrozen = isFrozen, theme = theme) { isFrozen = !isFrozen }
                    }
                }
            }
        }
    }
}

@Composable
private fun FreezeButton(isFrozen: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor = if (isFocused) theme.accentColor else theme.textColor.copy(alpha = 0.12f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(if (isFrozen) "חזור לסריקה חיה" else "הקפא ועיין בטקסט", color = if (isFocused) Color.Black else theme.textColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
