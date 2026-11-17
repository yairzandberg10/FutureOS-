package com.future.navigation.data.gtfs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

/** פירוק תשובות HERE Next Departures (boards[]) ו-Station Search (stations[]), לפי הצורה המתועדת. */
class ServerDeparturesTest {

    private val now = OffsetDateTime.parse("2026-11-16T08:00:00+02:00").toInstant().toEpochMilli()

    @Test
    fun parsesBoardsAndMinutesAway() {
        val json = """
            {"boards":[{"place":{"name":"ארלוזורוב","type":"station","location":{"lat":32.08,"lng":34.78},"id":"s1"},
              "departures":[
                {"time":"2026-11-16T08:12:00+02:00","transport":{"mode":"bus","name":"18","headsign":"בת ים"}},
                {"time":"2026-11-16T08:03:00+02:00","delay":120,"transport":{"mode":"bus","name":"5","shortName":"5","headsign":"דיזנגוף"}},
                {"time":"2026-11-16T07:50:00+02:00","transport":{"name":"past"}}
              ]}]}
        """.trimIndent()
        val boards = ServerDepartures().parse(Json.parseToJsonElement(json) as JsonObject, now)
        assertEquals(1, boards.size)
        val departures = boards[0].departures
        // יציאה שכבר עברה לא מוצגת, והרשימה ממוינת לפי זמן כולל עיכוב
        assertEquals(listOf("5", "18"), departures.map { it.line })
        assertEquals(5, departures[0].minutesAway)
        assertEquals(12, departures[1].minutesAway)
        assertEquals("דיזנגוף", departures[0].headsign)
    }

    @Test
    fun parsesStationSearch() {
        val json = """
            {"stations":[{"place":{"name":"תחנה מרכזית","location":{"lat":32.05,"lng":34.77}},
              "transports":[{"name":"1"},{"name":"4","shortName":"4"},{"name":"1"}]}]}
        """.trimIndent()
        val hits = ServerDepartures().parseStations(Json.parseToJsonElement(json) as JsonObject)
        assertEquals(1, hits.size)
        assertEquals("תחנה מרכזית", hits[0].name)
        assertEquals(listOf("1", "4"), hits[0].lines)
        assertTrue(ServerDepartures().parseStations(Json.parseToJsonElement("{}") as JsonObject).isEmpty())
    }
}
