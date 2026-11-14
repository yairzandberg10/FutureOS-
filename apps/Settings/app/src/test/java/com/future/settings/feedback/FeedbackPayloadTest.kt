package com.future.settings.feedback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackPayloadTest {

    private fun payload(text: String = "קורס", log: String? = null, version: String? = "1.4") = FeedbackPayload(
        clientId = "11111111-2222-3333-4444-555555555555",
        type = FeedbackType.Bug,
        appPkg = "com.future.camera",
        appLabel = "מצלמה",
        appVersion = version,
        build = "FutureOS 0.9",
        model = "F22 Pro",
        text = text,
        log = log,
    )

    @Test
    fun redactsPhonesEmailsIdsLocationsAndSecrets() {
        val out = FeedbackRedactor.redact(
            "054-1234567 +972 54 123 4567 a@b.co 352099001761481 31.77685,35.23456 AA:BB:CC:DD:EE:FF 10.0.0.1 token=abc",
        )
        for (leak in listOf("1234567", "4567", "a@b.co", "352099001761481", "31.77685", "AA:BB", "10.0.0.1", "abc")) {
            assertFalse("$leak leaked: $out", out.contains(leak))
        }
    }

    @Test
    fun keepsVersionNumbers() {
        assertEquals("גרסה 1.4.2 של המצלמה", FeedbackRedactor.redact("גרסה 1.4.2 של המצלמה"))
    }

    @Test
    fun jsonEscapesQuotesNewlinesAndControlChars() {
        val json = payload(text = "שורה \"א\"\nשורה ב\\\u0001").toJson()
        assertTrue(json, json.contains("\"text\":\"שורה \\\"א\\\"\\nשורה ב\\\\\\u0001\""))
        assertFalse(json.contains("\"log\""))
    }

    @Test
    fun jsonIncludesLogOnlyWhenPresentAndOmitsMissingVersion() {
        val json = payload(log = "E/x: boom", version = null).toJson()
        assertTrue(json.contains("\"log\":\"E/x: boom\""))
        assertFalse(json.contains("app_version"))
        assertTrue(json.startsWith("{\"client_id\":\"11111111-2222-3333-4444-555555555555\",\"type\":\"bug\""))
    }

    @Test
    fun cleanVersionMatchesServerRules() {
        assertEquals("1.4-beta (3)", FeedbackPayload.cleanVersion("1.4-beta (3)"))
        assertEquals("1.4script", FeedbackPayload.cleanVersion("1.4<script>"))
        assertNull(FeedbackPayload.cleanVersion("   "))
        assertNull(FeedbackPayload.cleanVersion(null))
        assertEquals(64, FeedbackPayload.cleanVersion("9".repeat(100))!!.length)
    }

    @Test
    fun tailLogKeepsTheEndOnALineBoundary() {
        val log = (1..1000).joinToString("\n") { "line $it" }
        val tail = FeedbackPayload.tailLog(log, 100)
        assertTrue(tail.length <= 100)
        assertTrue(tail.endsWith("line 1000"))
        assertTrue(tail.startsWith("line "))
        assertEquals("short", FeedbackPayload.tailLog("short", 100))
    }
}
