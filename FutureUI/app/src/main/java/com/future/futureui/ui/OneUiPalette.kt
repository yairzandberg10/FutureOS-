package com.future.futureui.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.future.futureui.theme.ThemeProvider
import com.future.sharednav.focus.animateFocusColor
import com.future.sharednav.focus.focusScale
import com.future.sharednav.theme.FutureContrast

/**
 * פלטת One UI של מרכז הבקרה, מרכז ההתראות והבאנר הקופץ: או בהירה (לבן, טקסט שחור)
 * או כהה (שחור, טקסט לבן) - בלי הזכוכית האפורה-שקופה שהייתה קודם. הבהיר/כהה נקרא
 * מהעדפת המצב המשותפת של FutureOS (ThemeProvider), וצבע ההדגשה משם הוא צבע הפוקוס.
 */
@Immutable
class OneUiPalette(
    val isDark: Boolean,
    accent: Color,
) {
    val scrim: Color = if (isDark) Color(0xE6000000) else Color(0xF0F5F5F7)
    val text: Color = if (isDark) Color.White else Color(0xFF111113)
    val subText: Color = if (isDark) Color(0xFFB4B4BA) else Color(0xFF6A6A70)

    /** משטח כרטיס/אריח במנוחה. */
    val tile: Color = if (isDark) Color(0xFF26262A) else Color.White
    /** משטח אריח ממוקד - בהיר/כהה מעט יותר, כך שהפוקוס נראה גם בלי הטבעת. */
    val tileFocused: Color = if (isDark) Color(0xFF3A3A40) else Color(0xFFE9EAEE)

    /** עיגול האייקון: כבוי / פעיל (בצבע ההדגשה כשהוא קריא, אחרת הדיו ההפוך). */
    val chipOff: Color = if (isDark) Color(0xFF3E3E44) else Color(0xFFE3E3E8)
    val chipOffIcon: Color = text
    val chipOn: Color = FutureContrast.readableAccent(
        accent.toArgb(), tile.toArgb(), if (isDark) FutureContrast.LIGHT_INK else FutureContrast.DARK_INK
    ).let { Color(it) }
    val chipOnIcon: Color = FutureContrast.onColor(chipOn)

    /** טבעת הפוקוס: תמיד נבדלת מהמשטח. */
    val ring: Color = chipOn

    val sliderTrack: Color = if (isDark) Color(0xFF3A3A40) else Color(0xFFDADADF)

    companion object {
        val Default = OneUiPalette(isDark = true, accent = Color.White)
    }
}

val LocalOneUiPalette = compositionLocalOf { OneUiPalette.Default }

private const val THEME_PREFS = "shared_theme_prefs"

/** פלטה חיה: מתעדכנת מיד כשהמשתמש מחליף מצב בהיר/כהה או צבע הדגשה. */
@Composable
fun rememberOneUiPalette(): OneUiPalette {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE) }
    var palette by remember { mutableStateOf(readPalette(prefs)) }
    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { p, _ -> palette = readPalette(p) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    return palette
}

private fun readPalette(prefs: SharedPreferences) = OneUiPalette(
    isDark = prefs.getBoolean(ThemeProvider.COL_IS_DARK_MODE, true),
    accent = Color(prefs.getInt(ThemeProvider.COL_PRIMARY_COLOR, android.graphics.Color.WHITE)),
)

/**
 * סימון פוקוס אחיד בסגנון One UI: האריח גדל מעט, הרקע מתחלף לגוון הממוקד וטבעת
 * עבה בצבע ההדגשה נצבעת סביבו - הכול מונפש, ובלי ה-LightGray הישן.
 */
@Composable
fun Modifier.oneUiFocus(isFocused: Boolean, shape: Shape, focusedScale: Float = 1.04f): Modifier {
    val palette = LocalOneUiPalette.current
    val ring by animateFocusColor(isFocused, palette.ring, palette.ring.copy(alpha = 0f))
    return this
        .zIndex(if (isFocused) 1f else 0f)
        .focusScale(isFocused, focusedScale)
        .drawWithContent {
            // הטבעת מצוירת מעל התוכן הפנימי (הרקע של האריח) - אחרת הוא היה מכסה אותה.
            drawContent()
            if (ring.alpha > 0f) {
                val outline = shape.createOutline(size, layoutDirection, this)
                drawOutline(outline, ring, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5.dp.toPx()))
            }
        }
}

/** צבע משטח אריח לפי מצב הפוקוס. */
@Composable
fun oneUiTileColor(isFocused: Boolean): Color {
    val palette = LocalOneUiPalette.current
    return animateFocusColor(isFocused, palette.tileFocused, palette.tile).value
}
