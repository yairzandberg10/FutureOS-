package com.future.flashlight.ui
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onStatusColor

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.flashlight.data.FlashlightController
import com.future.sharednav.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun FlashlightScreen(theme: FutureTheme) {
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

    // אם המשתמש יוצא מהאפליקציה (מקש הבית וכו') כשהפנס דלוק, מכבים אותו -
    // אחרת הוא נשאר דלוק לצמיתות בלי שום דרך לכבות אותו חזרה מהאפליקציה עצמה.
    DisposableEffect(Unit) {
        onDispose { if (isOn) controller.setTorch(false) }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(hasPermission) { focusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "פנס", textColor = theme.textColor, accentColor = theme.accentColor, onBack = null)

                if (!hasPermission) {
                    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "כדי להשתמש בפנס צריך לאשר הרשאת מצלמה",
                                color = theme.textColor.copy(alpha = 0.7f),
                                fontSize = FutureTypography.bodyLarge,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            FlashlightPermissionButton(theme = theme, focusRequester = focusRequester, onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) })
                        }
                    }
                } else if (!controller.hasFlash()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("לא נמצא פנס במכשיר הזה", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlashlightToggle(isOn = isOn, theme = theme, focusRequester = focusRequester, onToggle = {
                                val target = !isOn
                                if (controller.setTorch(target)) {
                                    isOn = target
                                    errorMessage = null
                                } else {
                                    errorMessage = "לא ניתן להפעיל את הפנס"
                                }
                            })
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(if (isOn) "דלוק" else "כבוי", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.body)
                            errorMessage?.let {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(it, color = theme.dangerColor, fontSize = FutureTypography.label)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlashlightToggle(isOn: Boolean, theme: FutureTheme, onToggle: () -> Unit, focusRequester: FocusRequester? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        if (isOn) theme.warningColor else if (isFocused) theme.focusFillChipColor else theme.idleFieldColor,
        FutureMotion.focusColorSpec,
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
                if (isFocused) Modifier.border(FutureDimens.focusBorderControl, if (isOn) theme.textColor else theme.readableAccentColor, CircleShape)
                else Modifier
            )
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            if (isOn) Icons.Rounded.FlashlightOn else Icons.Rounded.FlashlightOff,
            contentDescription = "הפעל/כבה פנס",
            tint = if (isOn) theme.onStatusColor(theme.warningColor) else theme.textColor,
            modifier = Modifier.size(64.dp)
        )
    }
}

@Composable
private fun FlashlightPermissionButton(theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    FutureButton("אשר הרשאה", theme, onClick, focusRequester = focusRequester)
}
