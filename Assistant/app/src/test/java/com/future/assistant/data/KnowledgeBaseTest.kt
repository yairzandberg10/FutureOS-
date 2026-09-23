package com.future.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KnowledgeBaseTest {
    private val data = KnowledgeBase.load { name -> File("src/main/assets/knowledge/$name").readText() }

    private fun ask(q: String) = KnowledgeBase.answer(data, q)?.responseText

    @Test fun commandCountIsAtLeast5000() {
        val total = data.commandCount + UnitConverter.commandCount + 23 * 2 + 1
        println("Knowledge commands: ${data.commandCount}, units: ${UnitConverter.commandCount}, total new: $total")
        assertTrue("total=$total", total >= 5000)
    }

    @Test fun capitals() {
        assertEquals("עיר הבירה של צרפת היא פריז", ask("מה הבירה של צרפת?"))
        assertEquals("פריז היא עיר הבירה של צרפת", ask("פריז היא הבירה של איזו מדינה"))
        assertEquals("יפן נמצאת באסיה", ask("באיזו יבשת נמצאת יפן"))
    }

    @Test fun dictionary() {
        assertEquals("כלב באנגלית: dog", ask("איך אומרים כלב באנגלית"))
        assertEquals("תפוח אדמה באנגלית: potato", ask("תרגם תפוח אדמה"))
        assertEquals("מה באנגלית: what", ask("איך אומרים מה באנגלית"))
        assertTrue(ask("איך כותבים חתול באנגלית")!!.contains("C-A-T"))
    }

    @Test fun opposites() {
        assertEquals("ההפך של גדול הוא קטן", ask("מה ההפך של גדול"))
        assertTrue(ask("מה ההפך של קטן")!!.startsWith("ההפך של קטן הוא גדול"))
    }

    @Test fun blessings() {
        assertEquals("על תפוח מברכים בורא פרי העץ", ask("מה מברכים על תפוח"))
        assertEquals("אחרי ענבים מברכים ברכה מעין שלוש, על העץ", ask("מה הברכה אחרונה על ענבים"))
        assertEquals("על בירה מברכים שהכל נהיה בדברו", ask("מה מברכים על בירה"))
    }

    @Test fun elementsBooksParashot() {
        assertEquals("הסמל של זהב הוא Au", ask("מה הסמל של זהב"))
        assertEquals("המספר האטומי של חמצן הוא 8", ask("מה המספר האטומי של חמצן"))
        assertTrue(ask("איזה יסוד מספר 26")!!.contains("ברזל"))
        assertEquals("בספר תהילים יש 150 פרקים", ask("כמה פרקים יש בתהילים"))
        assertEquals("אחרי פרשת נח באה פרשת לך לך", ask("איזו פרשה באה אחרי נח"))
        assertEquals("אחרי פרשת בא באה פרשת בשלח", ask("מה הפרשה שאחרי פרשת בא"))
    }

    @Test fun animalsPlanetsPeopleTrivia() {
        assertEquals("כלב: נובח, הב הב", ask("איך עושה הכלב"))
        assertEquals("הצאצא של סוס נקרא סייח", ask("איך קוראים לגור של סוס"))
        assertEquals("לצדק יש בערך 95 ירחים ידועים", ask("כמה ירחים יש לצדק"))
        assertTrue(ask("מי היה הרמב\"ם")!!.contains("משה בן מימון"))
        assertEquals("הגימטריה של שלום היא 376", ask("מה הגימטריה של שלום"))
        assertEquals("שמונה", ask("כמה רגליים יש לעכביש?"))
    }

    @Test fun unrelatedTextFallsThrough() {
        assertNull(ask("מה השעה"))
        assertNull(ask("תגביר קול"))
        assertNull(ask("ספר לי בדיחה"))
    }

    @Test fun units() {
        assertEquals("1 שעה זה 60 דקה", UnitConverter.tryConvert("כמה דקות יש בשעה")?.responseText)
        assertEquals("5 קילומטר זה 5000 מטר", UnitConverter.tryConvert("כמה מטרים יש ב-5 קילומטר")?.responseText)
        assertNotNull(UnitConverter.tryConvert("המר 3 גלון לליטר"))
        assertNull(UnitConverter.tryConvert("מה השעה היום"))
    }
}
