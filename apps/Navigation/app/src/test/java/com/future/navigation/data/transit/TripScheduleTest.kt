package com.future.navigation.data.transit

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/** פירוק תשובה אמיתית של Transitous (MOTIS trip): קו 126, 58 תחנות ביניים. */
class TripScheduleTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    @Test
    fun parsesAllStopsOfTheTrip() {
        val root = Json.parseToJsonElement(resource("motis_trip.json")) as JsonObject
        val trip = TripScheduleSource.parseTrip(root, "20261118_23:55_il-Israel-MOT_584906196_181126")
        assertNotNull(trip)
        trip!!
        assertEquals("126", trip.line)
        // תחנת עלייה + 58 ביניים + תחנת ירידה
        assertEquals(60, trip.stops.size)
        assertEquals("il-Israel-MOT_52692", trip.stops.first().stopId)
        // 21:55 UTC = 23:55 בשעון ישראל (חורף), 23:05 UTC = 01:05 למחרת
        assertEquals(23 * 3600 + 55 * 60, trip.stops.first().departureSeconds)
        assertEquals(1 * 3600 + 5 * 60, trip.stops.last().arrivalSeconds)
        assertNotNull(trip.stops.first().location)
    }

    @Test
    fun brokenOrEmptyAnswerGivesNoTrip() {
        assertNull(TripScheduleSource.parseTrip(Json.parseToJsonElement("{}") as JsonObject, "x"))
        assertNull(TripScheduleSource.parseTrip(Json.parseToJsonElement("""{"legs":[]}""") as JsonObject, "x"))
    }
}
