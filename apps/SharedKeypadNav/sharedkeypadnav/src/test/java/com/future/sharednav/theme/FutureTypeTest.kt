package com.future.sharednav.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * מחוון גודל הגופן בהגדרות לא השפיע על שום רכיב משותף, כי הרכיבים קבעו
 * גדלים קשיחים. הבדיקה הזו מקבעת את החוזה שהחליף אותם: כל גודל בסקאלה
 * הוא פונקציה ליניארית של המכפיל, ומכפיל 1.0 משמר בדיוק את הגדלים
 * שהיו קשיחים בקוד (ScreenTopBar 20, EmptyState 17/14, ConfirmDialog 15).
 */
class FutureTypeTest {

    @Test
    fun `default multiplier keeps the previously hardcoded sizes`() {
        val type = FutureType()
        assertEquals(20f, type.screenTitleFontSize.value, 0.001f)
        assertEquals(17f, type.titleFontSize.value, 0.001f)
        // 15sp הוא דרגה בסקאלה (tokens/typography.css), לא ערך יתום.
        assertEquals(15f, type.dialogFontSize.value, 0.001f)
        assertEquals(14f, type.bodyFontSize.value, 0.001f)
        assertEquals(16f, type.baseFontSize.value, 0.001f)
    }

    @Test
    fun `every size scales with the multiplier`() {
        val normal = FutureType(1.0f)
        val large = FutureType(1.4f)
        assertEquals(normal.screenTitleFontSize.value * 1.4f, large.screenTitleFontSize.value, 0.001f)
        assertEquals(normal.titleFontSize.value * 1.4f, large.titleFontSize.value, 0.001f)
        assertEquals(normal.summaryFontSize.value * 1.4f, large.summaryFontSize.value, 0.001f)
        assertEquals(normal.headerFontSize.value * 1.4f, large.headerFontSize.value, 0.001f)
    }

    @Test
    fun `smaller multiplier shrinks every size`() {
        val small = FutureType(0.8f)
        val normal = FutureType(1.0f)
        assertTrue(small.baseFontSize.value < normal.baseFontSize.value)
        assertTrue(small.headerFontSize.value < normal.headerFontSize.value)
    }
}
