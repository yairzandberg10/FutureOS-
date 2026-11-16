package com.future.navigation.data.gtfs

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** פירוק תשובת HERE Public Transit v8 (הצורה המתועדת: routes[].sections[] עם type/departure/arrival/transport). */
class TransitServerPlannerTest {

    private val sample = """
        {"routes":[{"id":"r1","sections":[
          {"id":"s1","type":"pedestrian",
           "departure":{"time":"2026-11-16T08:00:00+02:00","place":{"name":"","type":"place","location":{"lat":32.08,"lng":34.78}}},
           "arrival":{"time":"2026-11-16T08:05:00+02:00","place":{"name":"דיזנגוף סנטר","type":"station","location":{"lat":32.07,"lng":34.77}}},
           "travelSummary":{"duration":300,"length":350}},
          {"id":"s2","type":"transit",
           "departure":{"time":"2026-11-16T08:07:00+02:00","place":{"name":"דיזנגוף סנטר","type":"station"}},
           "arrival":{"time":"2026-11-16T08:25:00+02:00","place":{"name":"תחנה מרכזית","type":"station"}},
           "transport":{"mode":"bus","name":"5","shortName":"5","headsign":"תחנה מרכזית"},
           "intermediateStops":[{"departure":{"place":{"name":"כיכר רבין"}}},{"departure":{"place":{"name":"אבן גבירול"}}}]}
        ]},
        {"id":"r2","sections":[{"id":"w","type":"pedestrian",
           "departure":{"time":"2026-11-16T08:00:00+02:00","place":{"name":"a"}},
           "arrival":{"time":"2026-11-16T09:00:00+02:00","place":{"name":"b"}}}]}
        ]}
    """.trimIndent()

    @Test
    fun parsesWalkAndRideLegs() {
        val root = Json.parseToJsonElement(sample) as JsonObject
        val itineraries = TransitServerPlanner().parse(root)

        // מסלול של הליכה בלבד לא נחשב מסלול תחבורה ציבורית
        assertEquals(1, itineraries.size)
        val itinerary = itineraries[0]
        assertEquals(2, itinerary.legs.size)

        val walk = itinerary.legs[0]
        assertEquals(LegType.WALK, walk.type)
        assertEquals(300, walk.walkDurationSeconds)
        assertEquals(350.0, walk.walkDistanceMeters!!, 0.1)

        val ride = itinerary.legs[1]
        assertEquals(LegType.RIDE, ride.type)
        assertEquals("5", ride.routeShortName)
        assertEquals("דיזנגוף סנטר", ride.fromStopName)
        assertEquals("תחנה מרכזית", ride.toStopName)
        assertEquals(listOf("כיכר רבין", "אבן גבירול"), ride.intermediateStopNames)

        // 08:00 שעון ישראל = 28800 שניות מחצות; 08:25 = 30300
        assertEquals(8 * 3600, itinerary.departureSeconds)
        assertEquals(8 * 3600 + 25 * 60, itinerary.arrivalSeconds)
    }

    @Test
    fun emptyOrBrokenResponseGivesNoItineraries() {
        assertTrue(TransitServerPlanner().parse(Json.parseToJsonElement("{}") as JsonObject).isEmpty())
        assertTrue(TransitServerPlanner().parse(Json.parseToJsonElement("""{"routes":[{"sections":"x"}]}""") as JsonObject).isEmpty())
    }
}
