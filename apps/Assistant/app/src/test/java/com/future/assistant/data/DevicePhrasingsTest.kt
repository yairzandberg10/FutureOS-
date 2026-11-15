package com.future.assistant.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DevicePhrasingsTest {
    private val knowledge = KnowledgeBase.load { name -> File("src/main/assets/knowledge/$name").readText() }
    private val chat = ChatBase.load(File("src/main/assets/knowledge/chat.tsv").readText())

    @Test fun everyDevicePhrasingReachesItsOwnCommand() {
        val failures = mutableListOf<String>()
        for ((list, phrasings) in DevicePhrasings.ALL) {
            for (p in phrasings) {
                val text = CommandProcessor.stripName(p)
                val stage = CommandProcessor.preIntentStageForTest(text)
                    ?: if (CommandProcessor.smsStageForTest(text)) "sms" else null
                    ?: if (KnowledgeBase.answer(knowledge, text) != null) "knowledge" else null
                val triggers = CommandProcessor.matchedDeviceTriggersForTest(text)
                when {
                    stage != null -> failures += "$list: \"$p\" -> $stage"
                    triggers == null || p !in triggers -> failures += "$list: \"$p\" -> ${triggers?.take(3)}"
                }
            }
        }
        assertTrue("${failures.size} misrouted:\n" + failures.joinToString("\n"), failures.isEmpty())
    }

    @Test fun openPrefixReachesDeviceCommand() {
        listOf("תפתח את הגדרות הוויפיי", "תעבור למצב טיסה", "פתח את המצלמה", "תעבור להגדרות המיקום", "לך לאנשי הקשר")
            .forEach { assertTrue(it, CommandProcessor.deviceForOpenTest(it)) }
    }

    @Test fun smsPrefixesRecognized() {
        DevicePhrasings.SMS_PREFIXES.forEach { assertTrue(it, CommandProcessor.smsStageForTest("${it}אמא שאני מאחר")) }
    }

    @Test fun noDuplicatesWithExistingPhrasings() {
        val device = DevicePhrasings.ALL.values.flatten()
        val dup = device.groupBy { it }.filter { it.value.size > 1 }.keys
        assertTrue("duplicates: $dup", dup.isEmpty())
        println("Device phrasings: ${device.size}")
        assertTrue(device.size >= 500)
    }

    @Test fun crisisIsDetected() {
        assertTrue(CommandProcessor.crisisForTest("אני רוצה למות"))
        assertTrue(CommandProcessor.crisisForTest("נמאס לי לחיות."))
        assertFalse(CommandProcessor.crisisForTest("אני רוצה לאכול"))
    }

    @Test fun chatAnswersConversation() {
        assertTrue("chat rows ${chat.entries.size}", chat.entries.size >= 1000)
        listOf("אני עצוב היום", "ספר לי עוד", "מה אתה אוהב לעשות", "היה לי יום קשה").forEach {
            assertNotNull(it, ChatBase.match(chat, it))
        }
        val exact = ChatBase.match(chat, "יותר מזה")
        assertNotNull(exact)
        assertTrue(exact!!.exact)
        assertEquals(null, ChatBase.match(chat, "קשקש בלבל"))
    }
}
