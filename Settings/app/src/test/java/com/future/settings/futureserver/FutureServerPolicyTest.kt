package com.future.settings.futureserver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

/** בדיקות לוגיקה טהורה: חלון ה-15 דקות ובחירת כתובת הבסיס. בלי Android ובלי Robolectric. */
class FutureServerPolicyTest {

    private val minute = 60_000L
    private val start = 1_000_000_000L

    @Test
    fun neverAttempted_allowsAttempt() {
        assertTrue(FutureServerPolicy.canAttempt(start, 0L))
    }

    @Test
    fun attemptJustNow_blocksAttempt() {
        assertFalse(FutureServerPolicy.canAttempt(start, start))
    }

    @Test
    fun attemptFourteenMinutesAgo_blocksAttempt() {
        assertFalse(FutureServerPolicy.canAttempt(start + 14 * minute, start))
    }

    @Test
    fun attemptFifteenMinutesAgo_allowsAttempt() {
        assertTrue(FutureServerPolicy.canAttempt(start + 15 * minute, start))
    }

    @Test
    fun intervalConstantIsFifteenMinutes() {
        assertEquals(15 * minute, FutureServerPolicy.MIN_ATTEMPT_INTERVAL_MS)
    }

    @Test
    fun clockMovedBackwards_allowsAttempt() {
        assertTrue(FutureServerPolicy.canAttempt(start - 5 * minute, start))
    }

    @Test
    fun systemPropWins_overBuildConfig() {
        assertEquals(
            "https://api.example.com/v1",
            FutureServerPolicy.resolveBaseUrl("https://api.example.com/v1", "https://fallback.example.com/v1"),
        )
    }

    @Test
    fun emptySystemProp_fallsBackToBuildConfig() {
        assertEquals(
            "https://fallback.example.com/v1",
            FutureServerPolicy.resolveBaseUrl("", "https://fallback.example.com/v1"),
        )
        assertEquals(
            "https://fallback.example.com/v1",
            FutureServerPolicy.resolveBaseUrl(null, "https://fallback.example.com/v1"),
        )
        assertEquals(
            "https://fallback.example.com/v1",
            FutureServerPolicy.resolveBaseUrl("   ", "https://fallback.example.com/v1"),
        )
    }

    @Test
    fun bothEmpty_returnsNull() {
        assertNull(FutureServerPolicy.resolveBaseUrl(null, ""))
        assertNull(FutureServerPolicy.resolveBaseUrl("", null))
        assertNull(FutureServerPolicy.resolveBaseUrl("  ", "   "))
    }

    @Test
    fun trailingSlashIsRemoved() {
        assertEquals(
            "https://api.example.com/v1",
            FutureServerPolicy.resolveBaseUrl("https://api.example.com/v1/", null),
        )
    }

    @Test
    fun onlySlash_returnsNull() {
        assertNull(FutureServerPolicy.resolveBaseUrl("/", null))
    }
}
