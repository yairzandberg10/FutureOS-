package com.future.sharednav.theme

import androidx.compose.ui.graphics.Color

/**
 * חישובי ניגודיות - מה מותר לצייר על גבי מה. זה לא נוי: צבע ההדגשה
 * (primary_color) הוא בחירה חופשית של המשתמש וברירת המחדל שלו היא לבן,
 * ולכן כל מקום שמניח "טקסט לבן על ההדגשה" או "ההדגשה על משטח בהיר" יוצא
 * לבן-על-לבן בהתקנה סטנדרטית במצב בהיר. באג כזה כבר נמצא ותוקן פעם אחת
 * בפאנל המקלדת; אותו כלל בדיוק חסר בשאר המערכת (למשל
 * NavigationBarItemDefaults עם selectedIconColor=Color.Black קבוע, או
 * ConfirmDialog שכותב Color.Black על כפתור צבעוני).
 *
 * הפונקציות עובדות על ARGB גולמי (Int) ומקבלות גם Color של Compose, כדי
 * שאותה מתמטיקה תשרת גם את המקלדת (View-based, בלי Compose) וגם את שאר
 * המערכת - ותהיה ניתנת לבדיקה ב-unit test רגיל בלי מכשיר.
 *
 * הערכים והספים זהים למה שהיה ב-Keyboard/KeyboardPalette (שנשאר כמעטפת
 * דקה מעל הקובץ הזה) - ר' design/keyboard-panel/Tokens.dc.html.
 */
object FutureContrast {

    /** דיו כהה לצבע הדגשה בהיר - הערך מהעיצוב, לא שחור מלא. */
    const val DARK_INK: Int = 0xFF101012.toInt()
    const val LIGHT_INK: Int = 0xFFFFFFFF.toInt()

    /** מתחת ליחס הזה שני הצבעים כבר לא נבדלים זה מזה בעין. */
    const val MIN_VISIBLE_RATIO: Double = 1.6

    /** בהירות יחסית לפי WCAG. */
    fun relativeLuminance(argb: Int): Double {
        fun channel(value: Int): Double {
            val c = value / 255.0
            return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4)
        }
        val r = channel((argb shr 16) and 0xFF)
        val g = channel((argb shr 8) and 0xFF)
        val b = channel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    fun contrastRatio(first: Int, second: Int): Double {
        val a = relativeLuminance(first)
        val b = relativeLuminance(second)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /** צבע הטקסט/האייקון שמונח על גבי [background]. */
    fun onColor(background: Int): Int =
        if (relativeLuminance(background) > 0.45) DARK_INK else LIGHT_INK

    /**
     * [accent] כפי שאפשר להשתמש בו בפועל על גבי [surface]. אם הוא נבלע בו
     * (לבן על לבן), נופלים ל-[ink] - צבע הטקסט של הערכה, שתמיד מנוגד.
     */
    fun readableAccent(accent: Int, surface: Int, ink: Int): Int =
        if (contrastRatio(accent, surface) < MIN_VISIBLE_RATIO) ink else accent

    fun withAlpha(argb: Int, alpha: Float): Int {
        val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
        return (a shl 24) or (argb and 0x00FFFFFF)
    }

    // ---- מעטפות ל-Compose Color ----

    fun onColor(background: Color): Color = Color(onColor(background.toArgb()))

    fun contrastRatio(first: Color, second: Color): Double =
        contrastRatio(first.toArgb(), second.toArgb())

    fun readableAccent(accent: Color, surface: Color, ink: Color): Color =
        Color(readableAccent(accent.toArgb(), surface.toArgb(), ink.toArgb()))

    /**
     * לרכיב שמכיר רק את צבע הטקסט ולא את המשטח שמתחתיו (TopBarIconButton
     * מקבל textColor/accentColor בלבד): המשטח משוער כהיפוך של צבע הטקסט -
     * טקסט בהיר יושב על משטח כהה ולהפך.
     */
    fun accentForText(accent: Color, textColor: Color): Color =
        readableAccent(accent, onColor(textColor), textColor)

    /** ממיר בלי לעבור דרך android.graphics, כדי שהקובץ יישאר בדיק ב-JVM. */
    private fun Color.toArgb(): Int {
        val a = (alpha * 255f + 0.5f).toInt() and 0xFF
        val r = (red * 255f + 0.5f).toInt() and 0xFF
        val g = (green * 255f + 0.5f).toInt() and 0xFF
        val b = (blue * 255f + 0.5f).toInt() and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }
}
