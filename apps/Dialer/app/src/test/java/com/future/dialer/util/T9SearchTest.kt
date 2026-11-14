package com.future.dialer.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * בדיקות ל-T9Search - כולל רגרסיה על הפער שנמצא: matches() בודק רק מהתחלת
 * המחרוזת כולה, כך שחיפוש T9 בחייגן לא מצא אנשי קשר לפי שם משפחה (בניגוד
 * ל-Contact/Music, ששם matchesAnyWord כבר קיים). matchesAnyWord כאן סוגר
 * את הפער.
 *
 * מיפוי T9 עברי רלוונטי לבדיקות: ד/ה/ו=2, א/ב/ג=3, מ/נ/ם/ן=4, י/כ/ל/ך=5,
 * ז/ח/ט=6, ר/ש/ת=7, צ/ק/ץ=8, ס/ע/פ/ף=9 (ר' T9DigitMap.HEBREW).
 */
class T9SearchTest {

    @Test
    fun `empty query matches everything`() {
        assertTrue(T9Search.matches("יוסי", ""))
        assertTrue(T9Search.matchesAnyWord("יוסי דרור", ""))
    }

    @Test
    fun `matches only checks from the start of the whole string`() {
        // ד=2 ר=7 -> "27" תואם תחילת "דרור", אבל לא תחילת "יוסי דרור" (מתחיל ב-י).
        assertTrue(T9Search.matches("דרור", "27"))
        assertFalse(T9Search.matches("יוסי דרור", "27"))
    }

    @Test
    fun `matchesAnyWord finds a match on any word, not just the first`() {
        // "יוסי דרור" - חיפוש לפי הספרות של "דרור" (שם המשפחה, המילה השנייה) חייב למצוא.
        assertTrue(T9Search.matchesAnyWord("יוסי דרור", "27"))
        // וגם לפי "יוסי" (השם הפרטי, המילה הראשונה) כרגיל: י=5.
        assertTrue(T9Search.matchesAnyWord("יוסי דרור", "5"))
    }

    @Test
    fun `matchesAnyWord splits on hyphen and quotes too`() {
        // כהן: כ=5 ה=2 -> "52" צריך למצוא גם כשהוא אחרי מקף/גרש.
        assertTrue(T9Search.matchesAnyWord("לוי-כהן", "52"))
        assertTrue(T9Search.matchesAnyWord("ד\"ר כהן", "52"))
    }

    @Test
    fun `english names are matched case-insensitively`() {
        assertTrue(T9Search.matches("David", "3"))
        assertTrue(T9Search.matches("david", "3"))
    }

    @Test
    fun `query longer than the name never matches`() {
        assertFalse(T9Search.matches("א", "22"))
    }

    @Test
    fun `unmapped digits only match themselves literally`() {
        // ספרה 0/1 אין להן מיפוי T9 (בדיוק כמו על מקלדת פיזית אמיתית).
        assertFalse(T9Search.matches("א", "0"))
    }
}
