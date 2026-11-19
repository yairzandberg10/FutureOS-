package com.future.navigation.data.lines

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** פירוק תשובה אמיתית של Transitous (MOTIS stoptimes) לשורות יציאה של קווים. */
class LineTimesParseTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    private fun rows(): List<LineDeparture> {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        return LineTimes.parseStopTimes(root)
    }

    @Test
    fun parsesLinesStopsAndTimesFromTheFixture() {
        val rows = rows()
        assertTrue(rows.isNotEmpty())
        val lines = rows.map { it.line }.toSet()
        assertTrue("126" in lines)
        assertTrue("21" in lines)
        assertTrue(rows.all { it.stopName.isNotBlank() })
        assertTrue(rows.all { it.departsAtMillis > 0L })
        assertTrue(rows.all { it.location.lat in 29.0..34.5 && it.location.lon in 34.0..36.0 })
    }

    @Test
    fun headsignUnderscoresAreNormalised() {
        val rows = rows()
        assertTrue(rows.none { it.headsign.contains('_') })
        assertTrue(rows.any { it.headsign.isNotBlank() })
    }

    @Test
    fun rowsKeepTripIdAndStopId() {
        val rows = rows()
        assertTrue(rows.any { it.tripId != null })
        assertTrue(rows.any { it.stopId != null })
    }

    @Test
    fun brokenAnswerGivesNoRows() {
        val empty = Json.parseToJsonElement("""{"stopTimes":[]}""") as JsonObject
        assertEquals(0, LineTimes.parseStopTimes(empty).size)
        assertFalse(LineTimes.parseStopTimes(Json.parseToJsonElement("{}") as JsonObject).any())
    }
}
