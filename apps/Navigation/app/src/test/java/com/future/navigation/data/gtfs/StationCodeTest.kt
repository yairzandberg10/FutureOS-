package com.future.navigation.data.gtfs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

/** מספר תחנה (stopCode), חיפוש לפי מספר, ו"היציאה הבאה" של אותו קו. */
class StationCodeTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    private val now = OffsetDateTime.parse("2026-11-17T20:39:00Z").toInstant().toEpochMilli()

    @Test
    fun boardCarriesStopCodeOfItsStop() {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        val boards = ServerDepartures().parse(root, now)
        val avenue = boards.first { it.name == "אבן גבירול/ארלוזורוב" }
        assertEquals("20700", avenue.stopCode)
    }

    @Test
    fun stopCodesAreDigitsOnly() {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        val boards = ServerDepartures().parse(root, now)
        assertTrue(boards.all { it.stopCode == null || it.stopCode!!.all(Char::isDigit) })
    }

    @Test
    fun hasStopCodeMatchesOnlyExactCode() {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        val servers = ServerDepartures()
        assertTrue(servers.hasStopCode(root, "20700"))
        assertFalse(servers.hasStopCode(root, "2070"))
        assertFalse(servers.hasStopCode(root, "99999"))
    }

    @Test
    fun stationCodeQueryIsThreeToSixDigits() {
        assertTrue(isStationCodeQuery("123"))
        assertTrue(isStationCodeQuery("63273"))
        assertTrue(isStationCodeQuery(" 63273 "))
        assertFalse(isStationCodeQuery("12"))
        assertFalse(isStationCodeQuery("1234567"))
        assertFalse(isStationCodeQuery("12a45"))
        assertFalse(isStationCodeQuery("אריאל"))
        assertFalse(isStationCodeQuery(""))
    }

    @Test
    fun nextDepartureIsSameLineAndDirectionOnly() {
        val first = StationDeparture("21", "קדמה", 5, departsAtSeconds = 5 * 3600 + 44 * 60)
        val otherLine = StationDeparture("56", "קדמה", 6, departsAtSeconds = 5 * 3600 + 45 * 60)
        val otherDirection = StationDeparture("21", "כרמלית", 7, departsAtSeconds = 5 * 3600 + 47 * 60)
        val next = StationDeparture("21", "קדמה", 20, departsAtSeconds = 6 * 3600 + 53 * 60)
        val departures = listOf(first, otherLine, otherDirection, next)

        assertEquals(next, nextDepartureOfSameLine(departures, 0))
        assertNull(nextDepartureOfSameLine(departures, 1))
        assertNull(nextDepartureOfSameLine(departures, 3))
        assertNull(nextDepartureOfSameLine(emptyList(), 0))
    }
}
