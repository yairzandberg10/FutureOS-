package com.future.navigation.data.gtfs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** פירוק תשובה אמיתית של Transitous (MOTIS plan): תל אביב - ירושלים, אוטובוס, רכבת ואוטובוס. */
class TransitServerPlannerTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    @Test
    fun parsesRealItinerary() {
        val root = Json.parseToJsonElement(resource("motis_plan.json")) as JsonObject
        val itineraries = TransitServerPlanner().parse(root)
        assertEquals(1, itineraries.size)
        val it = itineraries[0]
        assertEquals(listOf(LegType.WALK, LegType.RIDE, LegType.WALK, LegType.RIDE, LegType.WALK, LegType.RIDE, LegType.WALK), it.legs.map { l -> l.type })

        val rides = it.legs.filter { l -> l.type == LegType.RIDE }
        assertEquals(listOf("56", "רכבת", "15"), rides.map { r -> r.routeShortName })
        // 21:09Z = 23:09 שעון ישראל; ההגעה 00:18 אחרי חצות
        assertEquals(23 * 3600 + 9 * 60, it.departureSeconds)
        assertEquals(24 * 3600 + 18 * 60, it.arrivalSeconds)
        assertEquals(69 * 60, it.totalDurationSeconds)

        assertEquals(5, rides[0].intermediateStopNames.size)
        assertNotNull(rides[0].toLocation)
        assertEquals("20699", rides[0].fromStopId)
        // ברכבת headsign הוא מספר רכבת, ומוצג שם המסלול במקומו
        assertFalse(rides[1].routeLongName.orEmpty().all { c -> c.isDigit() })
        assertTrue(rides[1].routeLongName.orEmpty().contains(" - "))
        assertEquals("המיקום שלך", it.legs.first().fromStopName)
        assertEquals("היעד", it.legs.last().toStopName)
        assertEquals(75.0, it.legs.first().walkDistanceMeters!!, 0.01)
    }

    @Test
    fun emptyOrBrokenAnswerGivesNoItineraries() {
        assertTrue(TransitServerPlanner().parse(Json.parseToJsonElement("{}") as JsonObject).isEmpty())
        assertTrue(TransitServerPlanner().parse(Json.parseToJsonElement("""{"itineraries":[{"legs":[{"mode":"WALK"}]}]}""") as JsonObject).isEmpty())
    }
}
