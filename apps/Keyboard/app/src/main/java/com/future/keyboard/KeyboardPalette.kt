package com.future.keyboard

import com.future.sharednav.theme.FutureContrast

/**
 * חישובי הצבע של פאנל המקלדת (ר' design/keyboard-panel/Tokens.dc.html).
 *
 * החישוב עצמו עבר ל-FutureContrast במודול המשותף, כי אותה בעיה בדיוק -
 * טקסט לבן על צבע ההדגשה הלבן של ברירת המחדל - קיימת בכל המערכת ולא רק
 * בפאנל. הקובץ נשאר כמעטפת כדי שהקריאות ב-KeyboardService (ערכי ARGB
 * גולמיים, בלי Compose) והבדיקות ב-KeyboardPaletteTest לא ישתנו.
 */
internal object KeyboardPalette {

    const val DARK_INK: Int = FutureContrast.DARK_INK
    const val LIGHT_INK: Int = FutureContrast.LIGHT_INK

    fun relativeLuminance(color: Int): Double = FutureContrast.relativeLuminance(color)

    fun contrastRatio(first: Int, second: Int): Double = FutureContrast.contrastRatio(first, second)

    fun onAccent(background: Int): Int = FutureContrast.onColor(background)

    fun accentOn(accent: Int, panel: Int, ink: Int): Int = FutureContrast.readableAccent(accent, panel, ink)

    fun withAlpha(color: Int, alpha: Float): Int = FutureContrast.withAlpha(color, alpha)
}
