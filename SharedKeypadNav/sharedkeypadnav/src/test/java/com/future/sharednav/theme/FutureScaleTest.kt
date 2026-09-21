package com.future.sharednav.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * כל רדיוס וכל גודל גופן שנמצאו בקוד לפני האיחוד חייבים להיות ממופים
 * לדרגה אחת בסקאלה - אחרת המיגרציה משאירה ערכים "יתומים" והסקאלה לא
 * באמת מחליפה אותם.
 */
class FutureScaleTest {

    private val radii = listOf(FutureShapes.radiusXs, FutureShapes.radiusSm, FutureShapes.radiusTextField,
        FutureShapes.radiusMd, FutureShapes.radiusChip, FutureShapes.radiusLg, FutureShapes.radiusDialog,
        FutureShapes.radiusXl, FutureShapes.radiusXxl)

    private val sizes = listOf(FutureTypography.badge, FutureTypography.caption, FutureTypography.label,
        FutureTypography.summary, FutureTypography.body, FutureTypography.dialog, FutureTypography.bodyLarge,
        FutureTypography.title, FutureTypography.screenTitle, FutureTypography.headline,
        FutureTypography.display, FutureTypography.hero)

    @Test
    fun `every radius found in the code snaps onto the scale`() {
        for (r in listOf(2, 3, 4, 5, 6, 8, 9, 10, 12, 14, 16, 17, 18, 20, 24, 28, 30, 35)) {
            assertTrue("$r dp", FutureShapes.snap(r.dp) in radii)
        }
    }

    @Test
    fun `existing radius tokens are fixed points of the snap`() {
        for (r in radii) assertEquals(r, FutureShapes.snap(r))
        assertEquals(22.dp, FutureDimens.borderRadius)
        assertEquals(16.dp, FutureDimens.cardCornerRadius)
        assertEquals(8.dp, FutureDimens.itemCornerRadius)
    }

    /**
     * שלוש הדרגות שנוספו מהדיזיין סיסטם. קודם הן נבלעו בדרגה הסמוכה,
     * ולכן דווקא הן הבדיקה שהמיפוי באמת משתמש בהן.
     */
    @Test
    fun `the design-system radii each get their own step`() {
        assertEquals(10.dp, FutureShapes.snap(10.dp))
        assertEquals(14.dp, FutureShapes.snap(14.dp))
        assertEquals(20.dp, FutureShapes.snap(20.dp))
    }

    @Test
    fun `focus border keeps both widths the focus spec defines`() {
        assertEquals(1.5.dp, FutureDimens.focusBorderItem)
        assertEquals(2.dp, FutureDimens.focusBorderControl)
    }

    @Test
    fun `every interface font size found in the code snaps onto the scale`() {
        for (s in listOf(7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 22, 24, 26, 28, 32, 36, 40, 42, 48, 52)) {
            assertTrue("$s sp", FutureTypography.snap(s.sp) in sizes)
        }
    }

    @Test
    fun `display numerals above the scale are left alone`() {
        assertEquals(64.sp, FutureTypography.snap(64.sp))
        assertEquals(96.sp, FutureTypography.snap(96.sp))
    }

    /** 15sp היא דרגה בסקאלה ולא ערך יתום, ולכן snap לא מזיז אותה. */
    @Test
    fun `the dialog size is its own step`() {
        assertEquals(15.sp, FutureTypography.snap(15.sp))
        assertEquals(15f, FutureType().dialogFontSize.value, 0.001f)
    }

    @Test
    fun `sizes already approved on the device do not move`() {
        assertEquals(13.sp, FutureTypography.snap(13.sp))
        assertEquals(17.sp, FutureTypography.snap(17.sp))
        assertEquals(20.sp, FutureTypography.snap(20.sp))
        assertEquals(34.sp, FutureTypography.snap(34.sp))
    }

    @Test
    fun `motion durations are ordered`() {
        assertTrue(FutureMotion.DurationInstant < FutureMotion.DurationFast)
        assertTrue(FutureMotion.DurationFast < FutureMotion.DurationStandard)
        assertTrue(FutureMotion.DurationStandard < FutureMotion.DurationSlow)
    }
}
