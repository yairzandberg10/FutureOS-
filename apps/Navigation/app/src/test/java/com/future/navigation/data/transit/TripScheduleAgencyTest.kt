package com.future.navigation.data.transit

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** שם המפעיל של הנסיעה מגיע מ-agencyName של הרגל (לכותרת דף הקו). */
class TripScheduleAgencyTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    @Test
    fun agencyNameComesFromTheTripLeg() {
        val root = Json.parseToJsonElement(resource("motis_trip.json")) as JsonObject
        val trip = TripScheduleSource.parseTrip(root, "20261118_23:55_il-Israel-MOT_584906196_181126")
        assertEquals("מטרופולין", trip?.agency)
    }

    @Test
    fun missingAgencyIsNull() {
        val root = Json.parseToJsonElement(
            """{"legs":[{"tripId":"t1","routeShortName":"5","headsign":"X",
                "from":{"name":"A","lat":32.0,"lon":34.7,"departure":"2026-11-19T18:00:00Z"},
                "to":{"name":"B","lat":32.1,"lon":34.8,"arrival":"2026-11-19T18:20:00Z"}}]}""",
        ) as JsonObject
        val trip = TripScheduleSource.parseTrip(root, "t1")
        assertEquals("5", trip?.line)
        assertNull(trip?.agency)
    }
}
