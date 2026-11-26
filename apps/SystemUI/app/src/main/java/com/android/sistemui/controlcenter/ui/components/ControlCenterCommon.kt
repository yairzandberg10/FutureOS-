package com.android.sistemui.controlcenter.ui.components

import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.future.sharednav.focus.animateFocusFloat
import com.future.sharednav.focus.focusScale

@Composable
fun HeaderActionButton(icon: ImageVector, color: Color, onClick: () -> Unit, isPower: Boolean = false) {
    // עטיפה דקה סביב TopBarIconButton המשותף (מודול SharedKeypadNav) - חתימת
    // הקריאה נשארת זהה כדי שקריאות קיימות ב-FutureUI לא ישתנו.
    com.future.sharednav.components.TopBarIconButton(
        icon = icon,
        contentDescription = "",
        textColor = if (isPower) Color.Red else color,
        accentColor = Color.White,
        onClick = onClick,
    )
}

/**
 * פוקוס של פקד במרכז הבקרה ובשאר חלונות המערכת: הטבעת נצבעת פנימה (ולא
 * קופצת בבת אחת), והפקד גדל בקפיץ press - אותה תנועה כמו בשאר המערכת
 * (ר' focusMotion ב-SharedKeypadNav). zIndex נשאר מורם עד שהטבעת דועכת,
 * כדי שהפקד שיוצא מפוקוס לא ייחתך מתחת לשכן בזמן שהוא מתכווץ.
 */
@Composable
fun Modifier.focusEffect(
    isFocused: Boolean,
    shape: androidx.compose.ui.graphics.Shape = FutureShapes.lg,
    focusedScale: Float = 1.04f,
): Modifier {
    val ring = animateFocusFloat(isFocused, 1f, 0f)
    val accent = rememberSystemAccent()
    val dark = LocalOverlayIsDark.current
    // ההדגשה כמו שהיא, אלא אם היא לא נראית על הרקע (לבן על הזכוכית הבהירה,
    // כהה על הכהה) - אז צבע הטקסט, כמו readableAccentColor בשאר המערכת.
    val ringColor = remember(accent, dark) {
        com.future.sharednav.theme.FutureContrast.readableAccent(
            accent,
            if (dark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7),
            if (dark) Color.White else Color.Black,
        )
    }
    return this
        .zIndex(if (isFocused || ring.value > 0f) 1f else 0f)
        .focusScale(isFocused, focusedScale)
        .controlFocusRing(shape, ringColor) { ring.value }
}

/** האם השכבה (מרכז הבקרה / ההתראות) כהה - קובע את צבע טבעת הפוקוס. ברירת מחדל: כהה. */
val LocalOverlayIsDark = staticCompositionLocalOf { true }

/**
 * טבעת הפוקוס של מרכז הבקרה ושאר חלונות המערכת - כמו בכל המערכת
 * (--fos-focus-border-control): קו אחד של 2dp בצבע ההדגשה, שנצבע פנימה.
 *
 * הטבעת מצוירת כולה בתוך גבולות הפקד, כך שגם פקד שנחתך (clip) לפני הטבעת -
 * הגלולות העליונות, כרטיסי ההתראות - מקבל טבעת שלמה ואחידה. קודם היו כאן
 * פס כהה של 3dp וקו לבן בתוכו, ונראו כמו מסגרת כפולה וכבדה.
 */
fun Modifier.controlFocusRing(
    shape: androidx.compose.ui.graphics.Shape,
    color: Color = Color.White,
    alpha: () -> Float = { 1f },
): Modifier = this.drawWithContent {
    drawContent()
    val a = alpha()
    if (a <= 0f) return@drawWithContent
    val stroke = 2.dp.toPx()
    val half = stroke / 2f
    if (size.width <= stroke || size.height <= stroke) return@drawWithContent
    val outline = shape.createOutline(
        androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
        layoutDirection,
        this,
    )
    translate(half, half) {
        drawOutline(outline, color, alpha = a, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
    }
}

/**
 * צבע ההדגשה של המערכת (ערכת הנושא, ThemeProvider) - מתעדכן כשהוא משתנה
 * בהגדרות. FutureUI הוא הספק של ערכת הנושא, ולכן קורא את ההעדפות ישירות.
 */
@Composable
fun rememberSystemAccent(): Color {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = remember { context.getSharedPreferences("shared_theme_prefs", android.content.Context.MODE_PRIVATE) }
    fun read() = Color(prefs.getInt(com.android.sistemui.theme.ThemeProvider.COL_PRIMARY_COLOR, android.graphics.Color.WHITE))
    var accent by remember { mutableStateOf(read()) }
    // המאזין נשמר ב-remember: SharedPreferences מחזיק מאזינים בהפניה חלשה בלבד.
    val listener = remember {
        android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == com.android.sistemui.theme.ThemeProvider.COL_PRIMARY_COLOR) accent = read()
        }
    }
    DisposableEffect(prefs) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return accent
}
