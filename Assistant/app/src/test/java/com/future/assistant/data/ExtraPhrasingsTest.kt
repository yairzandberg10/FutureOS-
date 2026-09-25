package com.future.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExtraPhrasingsTest {
    private val knowledge = KnowledgeBase.load { name -> File("src/main/assets/knowledge/$name").readText() }

    /** כל הרשימות שמתחברות לפקודה רגילה (VoiceIntent). */
    private val intentLists = with(ExtraPhrasings) {
        mapOf(
            "TIME" to TIME, "DATE" to DATE, "WEEKDAY" to WEEKDAY, "YEAR" to YEAR, "MONTH" to MONTH,
            "CHARGING" to CHARGING, "BATTERY" to BATTERY, "STORAGE" to STORAGE, "DEVICE_MODEL" to DEVICE_MODEL,
            "FLASHLIGHT_OFF" to FLASHLIGHT_OFF, "FLASHLIGHT_ON" to FLASHLIGHT_ON,
            "VOLUME_UP" to VOLUME_UP, "VOLUME_DOWN" to VOLUME_DOWN, "UNMUTE" to UNMUTE, "MUTE" to MUTE,
            "HOW_ARE_YOU" to HOW_ARE_YOU, "THANKS" to THANKS, "WHO_ARE_YOU" to WHO_ARE_YOU,
            "NAME_MEANING" to NAME_MEANING, "CREATOR" to CREATOR, "ROBOT" to ROBOT, "WHERE" to WHERE, "AGE" to AGE,
            "LOVE" to LOVE, "FAV_COLOR" to FAV_COLOR, "FAV_FOOD" to FAV_FOOD, "HOBBY" to HOBBY, "YES_NO" to YES_NO,
            "BORED" to BORED, "TIRED" to TIRED, "HUNGRY" to HUNGRY, "SORRY" to SORRY, "HELP" to HELP, "JOKE" to JOKE,
            "RANDOM_NUMBER" to RANDOM_NUMBER, "COIN_FLIP" to COIN_FLIP, "DICE" to DICE, "NO_INTERNET" to NO_INTERNET,
            "CALENDAR_TOMORROW" to CALENDAR_TOMORROW, "CALENDAR_TODAY" to CALENDAR_TODAY,
            "CALENDAR_CREATE" to CALENDAR_CREATE, "ALARM" to ALARM, "MUSIC" to MUSIC, "NETWORK" to NETWORK,
            "APP_COUNT" to APP_COUNT, "UPTIME" to UPTIME, "FUN_FACT" to FUN_FACT, "COMPLIMENT" to COMPLIMENT,
            "MOTIVATION" to MOTIVATION, "GREETING" to GREETING,
        )
    }

    /** מה המשפט יפגוש לפני הפקודות הרגילות - בדיוק לפי הסדר ב-process(). */
    private fun interceptedBy(text: String): String? =
        CommandProcessor.preIntentStageForTest(text)
            ?: if (KnowledgeBase.answer(knowledge, text) != null) "knowledge" else null

    @Test fun everyPhrasingReachesItsOwnCommand() {
        val failures = mutableListOf<String>()
        for ((list, phrasings) in intentLists) {
            for (p in phrasings) {
                val text = CommandProcessor.stripName(p)
                val stage = interceptedBy(text)
                val triggers = CommandProcessor.matchedTriggersForTest(text)
                when {
                    stage != null -> failures += "$list: \"$p\" -> $stage"
                    triggers == null || p !in triggers -> failures += "$list: \"$p\" -> ${triggers?.take(3)}"
                }
            }
        }
        assertTrue("${failures.size} misrouted:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun closeWordsAndPhrasesClose() {
        (ExtraPhrasings.CLOSE_WORDS + ExtraPhrasings.CLOSE_PHRASES).forEach {
            assertEquals(it, "close", CommandProcessor.preIntentStageForTest(it))
        }
    }

    @Test fun openAndDialPrefixesAreRecognized() {
        ExtraPhrasings.OPEN_APP_PREFIXES.forEach {
            assertEquals(it, "open-app", CommandProcessor.preIntentStageForTest("$it שעון".replace("ה שעון", "השעון")))
        }
        ExtraPhrasings.DIAL_PREFIXES.forEach {
            assertEquals(it, "dial", CommandProcessor.preIntentStageForTest("${it}אמא"))
        }
    }

    @Test fun nameIsStripped() {
        assertEquals("מה השעה", CommandProcessor.stripName("עוזרי, מה השעה?"))
        assertEquals("מה השעה", CommandProcessor.stripName("היי עוזרי מה השעה"))
        assertEquals("תודה", CommandProcessor.stripName("תודה עוזרי"))
        assertEquals("", CommandProcessor.stripName("עוזרי"))
        assertEquals("", CommandProcessor.stripName("שלום עוזרי!"))
        assertEquals("איך קוראים לך", CommandProcessor.stripName("עוזריי איך קוראים לך"))
    }

    @Test fun atLeastThousandNewPhrasings() {
        val all = intentLists.values.flatten() + ExtraPhrasings.CLOSE_WORDS + ExtraPhrasings.CLOSE_PHRASES +
            ExtraPhrasings.OPEN_APP_PREFIXES + ExtraPhrasings.DIAL_PREFIXES
        val distinct = all.distinct()
        assertEquals("duplicates: ${all.groupBy { it }.filter { it.value.size > 1 }.keys}", all.size, distinct.size)
        val base = CommandProcessor.baseTriggersForTest
        val new = distinct.filter { it !in base }
        println("New phrasings: ${new.size} (of ${distinct.size})")
        assertTrue("new=${new.size}", new.size >= 1000)
    }
}
