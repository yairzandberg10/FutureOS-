package com.future.clock.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** הקלדת ספרות בגלגל השעה והדקות: ערכים כמו 40 ו-25 חייבים להישמר, לא ליפול ל-00. */
class TypeDigitTest {

    @Test
    fun `minutes 4 then 0 gives 40`() {
        val first = typeDigit("", 4, 59)
        assertEquals(4, first.value)
        assertFalse(first.complete)
        val second = typeDigit(first.buffer, 0, 59)
        assertEquals(40, second.value)
        assertTrue(second.complete)
    }

    @Test
    fun `minutes 2 then 5 gives 25`() {
        val first = typeDigit("", 2, 59)
        val second = typeDigit(first.buffer, 5, 59)
        assertEquals(25, second.value)
        assertTrue(second.complete)
    }

    @Test
    fun `minutes 4 then 7 gives 47`() {
        val first = typeDigit("", 4, 59)
        val second = typeDigit(first.buffer, 7, 59)
        assertEquals(47, second.value)
    }

    @Test
    fun `a digit that cannot start a number completes at once`() {
        val only = typeDigit("", 7, 59)
        assertEquals(7, only.value)
        assertTrue(only.complete)
        assertEquals("", only.buffer)
    }

    @Test
    fun `hours 2 then 3 gives 23`() {
        val first = typeDigit("", 2, 23)
        val second = typeDigit(first.buffer, 3, 23)
        assertEquals(23, second.value)
        assertTrue(second.complete)
    }

    @Test
    fun `hours 2 then 5 starts again with 5 and does not clamp to 23`() {
        val first = typeDigit("", 2, 23)
        val second = typeDigit(first.buffer, 5, 23)
        assertEquals(5, second.value)
        assertTrue(second.complete)
        assertEquals("", second.buffer)
    }

    @Test
    fun `hours digit above the first place completes at once`() {
        val only = typeDigit("", 3, 23)
        assertEquals(3, only.value)
        assertTrue(only.complete)
    }
}
