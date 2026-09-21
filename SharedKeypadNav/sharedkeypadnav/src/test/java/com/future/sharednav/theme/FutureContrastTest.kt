package com.future.sharednav.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * החוזה של מה-מותר-על-מה. ארבעת צבעי ההדגשה כאן הם אלה שבטבלת "צבע
 * הטקסט על גבי ההדגשה" בעיצוב (design/keyboard-panel/Tokens.dc.html),
 * והמקרים של מצב בהיר הם בדיוק הבאגים שהקובץ הזה נכתב כדי למנוע.
 */
class FutureContrastTest {

    private val white = 0xFFFFFFFF.toInt()
    private val black = 0xFF000000.toInt()

    @Test
    fun `light accents get dark ink`() {
        assertEquals(FutureContrast.DARK_INK, FutureContrast.onColor(white))
        assertEquals(FutureContrast.DARK_INK, FutureContrast.onColor(0xFF64D2FF.toInt()))
        assertEquals(FutureContrast.DARK_INK, FutureContrast.onColor(0xFFFFD60A.toInt()))
    }

    @Test
    fun `dark accents get white ink`() {
        assertEquals(FutureContrast.LIGHT_INK, FutureContrast.onColor(0xFF1E8E3E.toInt()))
        assertEquals(FutureContrast.LIGHT_INK, FutureContrast.onColor(black))
    }

    @Test
    fun `default white accent falls back to ink on a light surface`() {
        assertEquals(black, FutureContrast.readableAccent(white, 0xFFEFEFEF.toInt(), black))
    }

    @Test
    fun `colored accent is kept on both surfaces`() {
        val teal = 0xFF00A3A3.toInt()
        assertEquals(teal, FutureContrast.readableAccent(teal, 0xFF1C1C1E.toInt(), white))
        assertEquals(teal, FutureContrast.readableAccent(teal, 0xFFEFEFEF.toInt(), black))
    }

    @Test
    fun `compose overloads agree with the int math`() {
        assertEquals(Color(FutureContrast.DARK_INK), FutureContrast.onColor(Color.White))
        assertEquals(Color.Black, FutureContrast.accentForText(Color.White, Color.Black))
        assertEquals(Color.White, FutureContrast.accentForText(Color.White, Color.White))
    }

    @Test
    fun `light theme roles never paint white on white`() {
        val light = FutureTheme(isDarkMode = false, accentColor = Color.White)
        assertTrue(FutureContrast.contrastRatio(light.readableAccentColor, light.surfaceColor) >= FutureContrast.MIN_VISIBLE_RATIO)
        assertTrue(FutureContrast.contrastRatio(light.onAccentColor, light.accentColor) >= 4.5)
    }

    @Test
    fun `material scheme text is readable on its container`() {
        for (dark in listOf(true, false)) {
            for (accent in listOf(Color.White, Color(0xFF64D2FF), Color(0xFF1E8E3E), Color.Black)) {
                val scheme = FutureTheme(isDarkMode = dark, accentColor = accent).toColorScheme()
                // 3:1 ולא 4.5:1: onPrimary משמש לתוויות מודגשות ואייקונים על
                // מחוון, שם הסף של WCAG הוא 3:1. העיצוב עצמו קובע לבן על
                // #1E8E3E (כ-4.3:1), ובדיקה של 4.5 הייתה פוסלת את העיצוב המאושר.
                assertTrue(
                    "onPrimary on primary, dark=$dark accent=$accent",
                    FutureContrast.contrastRatio(scheme.onPrimary, scheme.primary) >= 3.0,
                )
                assertTrue(
                    "primary on surface, dark=$dark accent=$accent",
                    FutureContrast.contrastRatio(scheme.primary, scheme.surface) >= FutureContrast.MIN_VISIBLE_RATIO,
                )
            }
        }
    }
}
