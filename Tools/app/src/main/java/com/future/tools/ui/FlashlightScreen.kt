package com.future.tools.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.data.FlashlightController
import com.future.tools.ui.theme.FutureTheme

@Composable
fun FlashlightScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val controller = remember { FlashlightController(context) }
    var isOn by remember { mutableStateOf(false) }
    var hasPermission by remember {
        mutableStateOf(context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    DisposableEffect(Unit) {
        onDispose { if (isOn) controller.setTorch(false) }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "פנס", theme = theme, onBack = onBack)

                if (!hasPermission) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("כדי להשתמש בפנס צריך לאשר הרשאת מצלמה", color = theme.textColor.copy(alpha = 0.7f), fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(16.dp))
                            FlashlightPermissionButton(theme = theme) { permissionLauncher.launch(Manifest.permission.CAMERA) }
                        }
                    }
                } else if (!controller.hasFlash()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("לא נמצא פנס במכשיר הזה", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlashlightToggle(isOn = isOn, theme = theme, onToggle = {
                                val target = !isOn
                                if (controller.setTorch(target)) {
                                    isOn = target
                                    errorMessage = null
                                } else {
                                    errorMessage = "לא ניתן להפעיל את הפנס"
                                }
                            })
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(if (isOn) "דלוק" else "כבוי", color = theme.textColor.copy(alpha = 0.6f), fontSize = 14.sp)
                            errorMessage?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, color = Color(0xFFFF6B6B), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashlightToggle(isOn: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isOn) Color(0xFFFFD60A) else if (isFocused) theme.textColor.copy(alpha = 0.18f) else theme.textColor.copy(alpha = 0.08f),
        label = "flashlightBg"
    )
    val scale by animateFloatAsState(if (isFocused) 1.06f else 1f, label = "flashlightScale")

    Box(
        modifier = Modifier
            .size(150.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(bgColor)
            .then(
                if (isFocused) Modifier.border(3.dp, if (isOn) Color(0xFFFFD60A) else theme.textColor.copy(alpha = 0.4f), CircleShape)
                else Modifier
            )
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            contentDescription = "הפעל/כבה פנס",
            tint = if (isOn) Color.Black else theme.textColor,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun FlashlightPermissionButton(theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.7f), label = "permBtnBg")
    Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Text("אשר הרשאה", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
