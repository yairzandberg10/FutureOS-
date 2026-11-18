package com.future.navigation.data.routing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** תשובות OSRM אמיתיות: חלופות לנהיגה (תל אביב-ירושלים), הליכה ורכיבה (בתל אביב). */
class OsrmRoutesTest {

    private fun resource(name: String): JsonObject =
        Json.parseToJsonElement(javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)) as JsonObject

    @Test
    fun drivingAlternativesAreAllParsed() {
        val routes = OsrmParser.parseAll(resource("osrm_alternatives.json"))
        assertEquals(2, routes.size)
        assertEquals(65665.0, routes[0].distanceMeters, 1.0)
        assertEquals(66367.0, routes[1].distanceMeters, 1.0)
        routes.forEach { assertTrue(it.polyline.size > 2) }
    }

    @Test
    fun footRouteParsesWithWalkingDuration() {
        val lookup = OsrmParser.parse(resource("osrm_foot.json"))
        assertTrue(lookup is RouteLookup.Found)
        val route = (lookup as RouteLookup.Found).route
        assertEquals(1976.0, route.distanceMeters, 1.0)
        // הליכה איטית מנסיעה: בערך 1.4 מ"ש, כלומר יותר מ-20 דקות ל-2 ק"מ
        assertTrue(route.durationSeconds > 1200.0)
        assertEquals("depart", route.steps.first().type)
        assertEquals("בוגרשוב", route.steps.first().streetName)
    }

    @Test
    fun bikeRouteParses() {
        val route = (OsrmParser.parse(resource("osrm_bike.json")) as RouteLookup.Found).route
        assertEquals(2030.0, route.distanceMeters, 1.0)
        assertTrue(route.steps.size >= 2)
    }

    @Test
    fun errorResponsesGiveNoRoutes() {
        assertEquals(emptyList<DrivingRoute>(), OsrmParser.parseAll(Json.parseToJsonElement("""{"code":"NoRoute"}""") as JsonObject))
        assertEquals(emptyList<DrivingRoute>(), OsrmParser.parseAll(Json.parseToJsonElement("""{"code":"InvalidQuery"}""") as JsonObject))
    }
}
