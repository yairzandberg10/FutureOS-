package com.future.gallery.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** GAL-27: מיון שמות "טבעי" - IMG_2 לפני IMG_10, בלי קשר לאותיות גדולות/קטנות. */
class NaturalCompareTest {

    @Test
    fun numbersCompareByValue() {
        assertTrue(naturalCompare("IMG_2.jpg", "IMG_10.jpg") < 0)
        assertTrue(naturalCompare("IMG_10.jpg", "IMG_2.jpg") > 0)
    }

    @Test
    fun equalNamesAreZero() {
        assertEquals(0, naturalCompare("IMG_5.jpg", "IMG_5.jpg"))
    }

    @Test
    fun caseIsIgnored() {
        assertEquals(0, naturalCompare("IMG", "img"))
    }

    @Test
    fun leadingZerosDoNotMatter() {
        assertEquals(0, naturalCompare("IMG_02", "IMG_2"))
    }
}
