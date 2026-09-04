package com.future.sharednav.t9

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * בדיקות למיפוי T9 המשותף - המודול הזה נצרך על ידי 16 אפליקציות (dialer,
 * Contact, Music, Keyboard ואחרות) ועד כה לא היה לו אף בדיקה, למרות שה-
 * CHANGELOG של הפרויקט מתעד באג אמיתי שכבר קרה כאן בעבר (מיפוי Music
 * שחסר אותיות סופיות עבריות) - בדיוק סוג הרגרסיה שבדיקות כאלה תופסות.
 */
class T9DigitMapTest {

    @Test
    fun `english map covers digits 2 through 9`() {
        assertEquals(setOf('2', '3', '4', '5', '6', '7', '8', '9'), T9DigitMap.ENGLISH.keys)
    }

    @Test
    fun `hebrew map covers digits 2 through 9`() {
        assertEquals(setOf('2', '3', '4', '5', '6', '7', '8', '9'), T9DigitMap.HEBREW.keys)
    }

    @Test
    fun `hebrew map includes final-form letters on the key of their base letter`() {
        // באג היסטורי בפרויקט: המיפוי של Music היה חסר בדיוק את האותיות
        // הסופיות האלה - בדיקה זו נועדה לתפוס רגרסיה כזאת מראש.
        assertTrue("מקש 4 צריך לכלול ם/ן", T9DigitMap.HEBREW.getValue('4').contains('ם'))
        assertTrue("מקש 4 צריך לכלול ן", T9DigitMap.HEBREW.getValue('4').contains('ן'))
        assertTrue("מקש 5 צריך לכלול ך", T9DigitMap.HEBREW.getValue('5').contains('ך'))
        assertTrue("מקש 8 צריך לכלול ץ", T9DigitMap.HEBREW.getValue('8').contains('ץ'))
        assertTrue("מקש 9 צריך לכלול ף", T9DigitMap.HEBREW.getValue('9').contains('ף'))
    }

    @Test
    fun `isHebrew recognizes hebrew letters`() {
        assertTrue(T9DigitMap.isHebrew('א'))
        assertTrue(T9DigitMap.isHebrew('ת'))
        assertTrue(T9DigitMap.isHebrew('ך')) // אות סופית
    }

    @Test
    fun `isHebrew rejects latin letters and digits`() {
        assertFalse(T9DigitMap.isHebrew('a'))
        assertFalse(T9DigitMap.isHebrew('Z'))
        assertFalse(T9DigitMap.isHebrew('5'))
    }
}
