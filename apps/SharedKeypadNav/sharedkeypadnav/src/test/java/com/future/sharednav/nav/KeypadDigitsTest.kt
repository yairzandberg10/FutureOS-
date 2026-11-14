package com.future.sharednav.nav

import androidx.compose.ui.input.key.Key
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * digitForKey היה מועתק בשבעה קבצים זהים. עכשיו יש מימוש אחד - ובדיקה
 * אחת ששומרת על שני הדברים שהוא חייב לקיים: שתי שורות המקשים הפיזיות
 * (שורה עליונה ומקלדת מספרית) ממופות לאותה ספרה, ומקש שאינו ספרה מחזיר
 * null במקום להיבלע בשקט.
 */
class KeypadDigitsTest {

    @Test
    fun `top row digits map to their character`() {
        assertEquals("0", digitForKey(Key.Zero))
        assertEquals("1", digitForKey(Key.One))
        assertEquals("5", digitForKey(Key.Five))
        assertEquals("9", digitForKey(Key.Nine))
    }

    @Test
    fun `numpad keys map to the same digit as the top row`() {
        val pairs = listOf(
            Key.Zero to Key.NumPad0,
            Key.One to Key.NumPad1,
            Key.Two to Key.NumPad2,
            Key.Three to Key.NumPad3,
            Key.Four to Key.NumPad4,
            Key.Five to Key.NumPad5,
            Key.Six to Key.NumPad6,
            Key.Seven to Key.NumPad7,
            Key.Eight to Key.NumPad8,
            Key.Nine to Key.NumPad9,
        )
        pairs.forEach { (topRow, numPad) ->
            assertEquals(digitForKey(topRow), digitForKey(numPad))
        }
    }

    @Test
    fun `non digit keys return null`() {
        assertNull(digitForKey(Key.DirectionDown))
        assertNull(digitForKey(Key.Enter))
        assertNull(digitForKey(Key.Back))
        assertNull(digitForKey(Key.Pound))
    }

    @Test
    fun `digit value mirrors the character mapping`() {
        assertEquals(7, digitValueForKey(Key.Seven))
        assertEquals(0, digitValueForKey(Key.NumPad0))
        assertNull(digitValueForKey(Key.Menu))
    }
}
