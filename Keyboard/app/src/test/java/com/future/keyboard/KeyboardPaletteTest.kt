package com.future.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * בדיקות לחישובי הצבע של פאנל המקלדת - ארבעת הצבעים כאן הם בדיוק אלה
 * שמוצגים בטבלת "צבע הטקסט על גבי ההדגשה" שבעיצוב (Tokens.dc.html).
 */
class KeyboardPaletteTest {

    private val darkPanel = 0xFF1C1C1E.toInt()
    private val lightPanel = 0xFFEFEFEF.toInt()
    private val white = 0xFFFFFFFF.toInt()

    @Test
    fun `טקסט על הדגשה בהירה יוצא כהה`() {
        assertEquals(KeyboardPalette.DARK_INK, KeyboardPalette.onAccent(white))
        assertEquals(KeyboardPalette.DARK_INK, KeyboardPalette.onAccent(0xFF64D2FF.toInt()))
        assertEquals(KeyboardPalette.DARK_INK, KeyboardPalette.onAccent(0xFFFFD60A.toInt()))
    }

    @Test
    fun `טקסט על הדגשה כהה יוצא לבן`() {
        assertEquals(KeyboardPalette.LIGHT_INK, KeyboardPalette.onAccent(0xFF1E8E3E.toInt()))
        assertEquals(KeyboardPalette.LIGHT_INK, KeyboardPalette.onAccent(0xFF000000.toInt()))
    }

    @Test
    fun `הדגשה לבנה במצב בהיר נופלת לצבע הטקסט כדי לא להיעלם ברקע`() {
        // ברירת המחדל של primary_color היא לבן; על פאנל בהיר היא בלתי נראית.
        assertEquals(0xFF000000.toInt(), KeyboardPalette.accentOn(white, lightPanel, 0xFF000000.toInt()))
    }

    @Test
    fun `הדגשה לבנה במצב כהה נשארת כמות שהיא`() {
        assertEquals(white, KeyboardPalette.accentOn(white, darkPanel, white))
    }

    @Test
    fun `הדגשה צבעונית לא משתנה בשני המצבים`() {
        val teal = 0xFF00A3A3.toInt()
        assertEquals(teal, KeyboardPalette.accentOn(teal, darkPanel, white))
        assertEquals(teal, KeyboardPalette.accentOn(teal, lightPanel, 0xFF000000.toInt()))
    }

    @Test
    fun `שקיפות משנה רק את ערוץ האלפא`() {
        assertEquals(0x28FFFFFF, KeyboardPalette.withAlpha(white, 0.16f))
        assertEquals(white, KeyboardPalette.withAlpha(white, 1f))
    }

    @Test
    fun `בהירות יחסית מסודרת משחור ללבן`() {
        assertEquals(0.0, KeyboardPalette.relativeLuminance(0xFF000000.toInt()), 0.0001)
        assertEquals(1.0, KeyboardPalette.relativeLuminance(white), 0.0001)
        assertTrue(KeyboardPalette.relativeLuminance(0xFF808080.toInt()) < 0.5)
    }
}
