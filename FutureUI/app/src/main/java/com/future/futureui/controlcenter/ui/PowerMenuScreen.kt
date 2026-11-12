package com.future.futureui.controlcenter.ui

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
import androidx.compose.material.icons.rounded.Close
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
import com.future.futureui.controlcenter.ui.components.focusEffect
import kotlinx.coroutines.delay
import com.future.sharednav.focus.animateFocusColor

/**
 * תפריט הכיבוי של FutureUI. נפתח משני מקומות: כפתור הכיבוי במרכז הבקרה,
 * והחזקת מקש ההפעלה (שירות שורת המצב מזהה את חלון הכיבוי של אנדרואיד, סוגר
 * אותו ופותח את זה במקומו - ר' StatusBarAccessibilityService). הדיאלוג
 * המקורי לא בנוי לניווט במקשים ולא תואם ויזואלית לשאר המערכת.
 *
 * שורות: ביטול (הפוקוס ההתחלתי), כיבוי והפעלה מחדש (שתיהן פותחות דיאלוג אישור
 * שגם בו הפוקוס על "ביטול", כדי שלחיצה בטעות לא תכבה את הטלפון), ואחריהן מצב טיסה, מצב שקט וצילום
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
    // הפוקוס ההתחלתי על "ביטול" ולא על "כיבוי": OK כפול (או OK שנשאר מהחזקת מקש ההפעלה)
    // כיבה את הטלפון בלילה, והמעורר לא צלצל (UI-PWR-01).
    val focusRequester = remember { FocusRequester() }
    val confirmFocusRequester = remember { FocusRequester() }
    // איזו פעולה מחכה לאישור בדיאלוג ("power" / "restart"), אם בכלל.
    var armed by remember { mutableStateOf<String?>(null) }
    // OK בחצי השנייה הראשונה אחרי פתיחת הדיאלוג לא מאשר - לחיצה כפולה מהירה לא תכבה.
    var armedAt by remember { mutableStateOf(0L) }
    LaunchedEffect(armed) {
        delay(100)
        try {
            if (armed == null) focusRequester.requestFocus() else confirmFocusRequester.requestFocus()
        } catch (t: Throwable) {
            android.util.Log.w("PowerMenuScreen", "PowerMenuScreen failed", t)
        }
    }
    fun confirmArmed() {
        if (android.os.SystemClock.uptimeMillis() - armedAt < 500) return
        when (armed) {
            "power" -> onPowerOff()
            "restart" -> onRestart()
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
                  if (armed != null) {
                    Text(
                        text = if (armed == "power") "לכבות את הטלפון?" else "להפעיל מחדש את הטלפון?",
                        color = Color.White,
                        fontSize = FutureTypography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.Close,
                        label = "ביטול",
                        tint = Color.White,
                        circle = Color.White.copy(alpha = 0.12f),
                        onClick = { armed = null },
                        modifier = Modifier.focusRequester(confirmFocusRequester)
                    )
                    PowerMenuRow(
                        icon = if (armed == "power") Icons.Rounded.PowerSettingsNew else Icons.Rounded.RestartAlt,
                        label = if (armed == "power") "כיבוי" else "הפעלה מחדש",
                        tint = if (armed == "power") Color(0xFFFF453A) else Color.White,
                        circle = if (armed == "power") Color(0xFFFF453A).copy(alpha = 0.18f) else Color(0xFF30D158).copy(alpha = 0.2f),
                        onClick = { confirmArmed() },
                    )
                  } else {
                    Text(
                        text = "אפשרויות כיבוי",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = FutureTypography.label,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.Close,
                        label = "ביטול",
                        tint = Color.White,
                        circle = Color.White.copy(alpha = 0.12f),
                        onClick = onCancel,
                        modifier = Modifier.focusRequester(focusRequester)
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.PowerSettingsNew,
                        label = "כיבוי",
                        tint = Color(0xFFFF453A),
                        circle = Color(0xFFFF453A).copy(alpha = 0.18f),
                        onClick = { armedAt = android.os.SystemClock.uptimeMillis(); armed = "power" },
                    )
                    PowerMenuRow(
                        icon = Icons.Rounded.RestartAlt,
                        label = "הפעלה מחדש",
                        tint = Color.White,
                        circle = Color(0xFF30D158).copy(alpha = 0.2f),
                        onClick = { armedAt = android.os.SystemClock.uptimeMillis(); armed = "restart" },
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
            .focusEffect(isFocused, shape, focusedScale = 1.02f)
            .clip(shape)
            .background(animateFocusColor(isFocused, Color.White.copy(alpha = 0.15f), Color.Transparent).value)
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
