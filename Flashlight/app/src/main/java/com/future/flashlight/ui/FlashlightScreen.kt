package com.future.flashlight.ui
import com.future.flashlight.data.TorchService
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
import com.future.sharednav.icons.FutureIcons

@Composable
fun FlashlightScreen(theme: FutureTheme) {
    val context = LocalContext.current
    val controller = remember { FlashlightController(context) }
    var isOn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // אין כאן בקשת הרשאה: setTorchMode לא צריך הרשאת CAMERA, והמניפסט כבר
    // לא מבקש אותה - כך שהבקשה הקודמת תמיד נדחתה והפנס לא הוצג בכלל.
    // המצב נקרא מהמערכת (TorchCallback), כולל הדלקה/כיבוי ממרכז הבקרה, וכשהמצלמה
    // תופסת את הפנס.
    DisposableEffect(Unit) {
        val stop = controller.observe { on, available ->
            isOn = on
            errorMessage = if (!available) "הפנס לא זמין כשהמצלמה פתוחה" else null
        }
        onDispose { stop() }
    }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ScreenTopBar(title = "פנס", textColor = theme.textColor, accentColor = theme.accentColor, onBack = null)

                if (!controller.hasFlash()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("לא נמצא פנס במכשיר הזה", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            FlashlightToggle(isOn = isOn, theme = theme, focusRequester = focusRequester, onToggle = {
                                // הדלקה דרך שירות קדמי - כך הפנס נשאר דלוק גם כשהמסך
                                // נכבה/ננעל והמערכת מפנה את האפליקציה מהזיכרון.
                                if (isOn) TorchService.turnOff(context) else TorchService.turnOn(context)
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
            if (isOn) FutureIcons.FlashlightOn else FutureIcons.FlashlightOff,
            contentDescription = "הפעל/כבה פנס",
            tint = if (isOn) theme.onStatusColor(theme.warningColor) else theme.textColor,
            modifier = Modifier.size(64.dp)
        )
    }
}


