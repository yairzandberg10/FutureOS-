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
        FutureShapes.radiusMd, FutureShapes.radiusChip, FutureShapes.radiusLg, FutureShapes.radiusRow,
        FutureShapes.radiusDialog, FutureShapes.radiusXl, FutureShapes.radiusXxl)

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
        assertEquals(20.dp, FutureDimens.itemCornerRadius)
    }

    /**
     * tokens/shape.css: "no tight corners anywhere" - 12dp היא הרצפה, שדה
     * קלט/לשונית/צ'יפ יושבים על 16dp, ושורת רשימה על 20dp.
     */
    @Test
    fun `the design-system radii match tokens shape css`() {
        for (r in radii) assertTrue("$r", r >= 12.dp)
        assertEquals(12.dp, FutureShapes.radiusSm)
        assertEquals(20.dp, FutureShapes.radiusRow)
        assertEquals(16.dp, FutureShapes.radiusTextField)
        assertEquals(16.dp, FutureShapes.radiusMd)
        assertEquals(16.dp, FutureShapes.radiusChip)
        assertEquals(20.dp, FutureShapes.radiusDialog)
        assertEquals(12.dp, FutureShapes.snap(4.dp))
    }

    @Test
    fun `list row height follows the row list token`() {
        assertEquals(65.dp, FutureDimens.rowHeightList)
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
