package com.android.sistemui.controlcenter.ui

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Screenshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.android.sistemui.controlcenter.ui.components.focusEffect
import kotlinx.coroutines.delay

/**
 * תפריט הכיבוי של FutureUI. נפתח משני מקומות: כפתור הכיבוי במרכז הבקרה,
 * והחזקת מקש ההפעלה (שירות שורת המצב מזהה את חלון הכיבוי של אנדרואיד, סוגר
 * אותו ופותח את זה במקומו - ר' StatusBarAccessibilityService). הדיאלוג
 * המקורי לא בנוי לניווט במקשים ולא תואם ויזואלית לשאר המערכת.
 *
 * שורות: כיבוי והפעלה מחדש (שתיהן מבקשות OK שני לאישור - כמו ההחלקה ב-Redmi,
 * כדי שלחיצה בטעות לא תכבה את הטלפון), ואחריהן מצב טיסה, מצב שקט וצילום
 * מסך. שורה של מתג מראה את המצב הנוכחי. כיבוי והפעלה מחדש דורשים root (כמו
 * שאר הפעולות ב-ControlManager).
 */
@Composable
fun PowerMenuScreen(
    onPowerOff: () -> Unit,
    onRestart: () -> Unit,
    onCancel: () -> Unit,
    airplaneOn: Boolean? = null,
    onAirplane: (() -> Unit)? = null,
    silentOn: Boolean? = null,
    onSilent: (() -> Unit)? = null,
    onScreenshot: (() -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    // איזו שורה מחכה ל-OK שני ("power" / "restart"), אם בכלל.
    var armed by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        delay(100)
        try { focusRequester.requestFocus() } catch (t: Throwable) {
            android.util.Log.w("PowerMenuScreen", "PowerMenuScreen failed", t)
        }
    }
    LaunchedEffect(armed) {
        if (armed != null) {
            delay(4000)
            armed = null
        }
    }
    val shown = remember { MutableTransitionState(false).apply { targetState = true } }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(visibleState = shown, enter = fadeIn() + scaleIn(initialScale = 0.92f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .clip(FutureShapes.xxl)
                        .background(Color(0xEE1C1C1E))
                        .border(0.5.dp, Color.White.copy(alpha = 0.15f), FutureShapes.xxl)
                        .padding(vertical = 12.dp)
                ) {
                    Text(
                        text = "אפשרויות כיבוי",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = FutureTypography.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = if (armed == "power") "לחצו OK שוב לכיבוי" else "כיבוי",
                        tint = Color(0xFFFF453A),
                        circle = Color(0xFFFF453A).copy(alpha = 0.18f),
                        onClick = { if (armed == "power") onPowerOff() else armed = "power" },
                        onFocusLost = { if (armed == "power") armed = null },
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.RestartAlt,
                        label = if (armed == "restart") "לחצו OK שוב להפעלה מחדש" else "הפעלה מחדש",
                        tint = Color.White,
                        circle = Color(0xFF30D158).copy(alpha = 0.2f),
                        onClick = { if (armed == "restart") onRestart() else armed = "restart" },
                        onFocusLost = { if (armed == "restart") armed = null },
                    )
                    if (onAirplane != null || onSilent != null || onScreenshot != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 22.dp, vertical = 6.dp)
                                .height(0.5.dp)
                                .background(Color.White.copy(alpha = 0.12f))
                        )
                    }
                    if (onAirplane != null) {
                        PowerMenuRow(
                            icon = Icons.Rounded.AirplanemodeActive,
                            label = "מצב טיסה",
                            status = if (airplaneOn == true) "פועל" else "כבוי",
                            statusOn = airplaneOn == true,
                            tint = Color.White,
                            circle = Color(0xFF0A84FF).copy(alpha = if (airplaneOn == true) 0.45f else 0.18f),
                            onClick = onAirplane,
                        )
                    }
                    if (onSilent != null) {
                        PowerMenuRow(
                            icon = Icons.Rounded.NotificationsOff,
                            label = "מצב שקט",
                            status = if (silentOn == true) "פועל" else "כבוי",
                            statusOn = silentOn == true,
                            tint = Color.White,
                            circle = Color(0xFFBF5AF2).copy(alpha = if (silentOn == true) 0.45f else 0.18f),
                            onClick = onSilent,
                        )
                    }
                    if (onScreenshot != null) {
                        PowerMenuRow(
                            icon = Icons.Rounded.Screenshot,
                            label = "צילום מסך",
                            tint = Color.White,
                            circle = Color.White.copy(alpha = 0.12f),
                            onClick = onScreenshot,
                        )
                    }
                    Text(
                        "חזרה לביטול",
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = FutureTypography.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PowerMenuRow(
    icon: ImageVector,
    label: String,
    tint: Color,
    circle: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    status: String? = null,
    statusOn: Boolean = false,
    onFocusLost: () -> Unit = {},
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.lg

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 3.dp)
            .focusEffect(isFocused, shape)
            .clip(shape)
            .background(if (isFocused) Color.White.copy(alpha = 0.15f) else Color.Transparent)
            .onFocusChanged { if (!it.isFocused) onFocusLost() }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = tint, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        if (status != null) {
            Text(
                status,
                color = if (statusOn) Color.White else Color.White.copy(alpha = 0.45f),
                fontSize = FutureTypography.summary,
                fontWeight = if (statusOn) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
