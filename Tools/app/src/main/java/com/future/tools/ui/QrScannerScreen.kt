package com.future.tools.ui

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.ExperimentalGetImage
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalGetImage::class)
@Composable
fun QrScannerScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val hasPermission by rememberRuntimePermission(Manifest.permission.CAMERA)
    val scanner = remember { BarcodeScanning.getClient() }
    var scannedValue by remember { mutableStateOf<String?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    DisposableEffect(Unit) { onDispose { scanner.close() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "סורק קודים", theme = theme, onBack = onBack)

                if (!hasPermission) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("נדרשת הרשאת מצלמה כדי לסרוק קודים", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        if (scannedValue == null) {
                            CameraAnalyzerView(modifier = Modifier.fillMaxSize()) { proxy ->
                                val mediaImage = proxy.image
                                if (mediaImage != null && !isProcessing) {
                                    isProcessing = true
                                    val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val value = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                            if (value != null) scannedValue = value
                                        }
                                        .addOnCompleteListener {
                                            isProcessing = false
                                            proxy.close()
                                        }
                                } else {
                                    proxy.close()
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .size(200.dp)
                                    .border(width = 2.dp, color = theme.accentColor.copy(alpha = 0.7f), shape = RoundedCornerShape(16.dp))
                            )
                        } else {
                            QrResultView(value = scannedValue!!, theme = theme, onScanAgain = { scannedValue = null })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QrResultView(value: String, theme: FutureTheme, onScanAgain: () -> Unit) {
    val context = LocalContext.current
    val isUrl = value.startsWith("http://") || value.startsWith("https://")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.surfaceColor)
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("נסרק בהצלחה", color = theme.accentColor, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, color = theme.textColor, fontSize = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(20.dp))

        QrActionButton("העתק", theme = theme) {
            val clipboard = context.getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("qr", value))
            Toast.makeText(context, "הועתק", Toast.LENGTH_SHORT).show()
        }
        Spacer(modifier = Modifier.height(10.dp))
        if (isUrl) {
            QrActionButton("פתח קישור", theme = theme) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(value)))
                } catch (e: Exception) {}
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        QrActionButton("סרוק שוב", theme = theme, isPrimary = false, onClick = onScanAgain)
    }
}

@Composable
private fun QrActionButton(label: String, theme: FutureTheme, isPrimary: Boolean = true, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val baseColor = if (isPrimary) theme.accentColor else theme.textColor.copy(alpha = 0.12f)
    val bgColor = if (isFocused) baseColor.copy(alpha = 1f) else baseColor.copy(alpha = if (isPrimary) 0.85f else 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth(0.8f)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isPrimary) Color.Black else theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}
