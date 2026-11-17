package com.future.navigation.data.gtfs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

/** פירוק תשובות אמיתיות של Transitous (MOTIS stoptimes ו-geocode), שנשמרו כקבצים. */
class ServerDeparturesTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    private val now = OffsetDateTime.parse("2026-11-17T20:39:00Z").toInstant().toEpochMilli()

    @Test
    fun groupsStopTimesIntoBoards() {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        val boards = ServerDepartures().parse(root, now)
        assertEquals(4, boards.size)
        assertEquals(listOf("21", "56", "126"), boards[0].departures.map { it.line })
        assertEquals(listOf(1, 2, 2), boards[0].departures.map { it.minutesAway })
        assertEquals(listOf("125", "79", "55", "3"), boards[1].departures.map { it.line })
        // כיוון הנסיעה בלי הקו התחתון של משרד התחבורה
        assertTrue(boards.flatMap { it.departures }.none { it.headsign.contains('_') })
    }

    @Test
    fun pastDeparturesAreDropped() {
        val root = Json.parseToJsonElement(resource("motis_stoptimes.json")) as JsonObject
        val later = OffsetDateTime.parse("2026-11-17T20:40:30Z").toInstant().toEpochMilli()
        val lines = ServerDepartures().parse(root, later).flatMap { it.departures }.map { it.line }
        assertEquals(listOf("56", "126", "3", "21"), lines)
    }

    @Test
    fun parsesStopSearch() {
        val matches = Json.parseToJsonElement(resource("motis_geocode.json")) as JsonArray
        val hits = ServerDepartures().parseStations(matches)
        assertEquals(3, hits.size)
        assertEquals(31.790768, hits[0].location.lat, 1e-6)
        // שם העיר מצורף, כדי להבדיל בין תחנות באותו שם
        assertTrue(hits.all { it.name.contains(", ") })
        assertTrue(ServerDepartures().parseStations(JsonArray(emptyList())).isEmpty())
    }
}
