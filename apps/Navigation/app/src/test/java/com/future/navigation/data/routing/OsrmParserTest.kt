package com.future.navigation.data.routing

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** פירוק תשובה אמיתית של OSRM (נסיעה קצרה בתל אביב), שנשמרה כקובץ. */
class OsrmParserTest {

    private fun resource(name: String): String =
        javaClass.classLoader!!.getResource("online/$name")!!.readText(Charsets.UTF_8)

    @Test
    fun parsesRealRoute() {
        val lookup = OsrmParser.parse(Json.parseToJsonElement(resource("osrm_route.json")) as JsonObject)
        assertTrue(lookup is RouteLookup.Found)
        val route = (lookup as RouteLookup.Found).route
        assertEquals(4149.4, route.distanceMeters, 0.1)
        assertEquals(105, route.polyline.size)
        // GeoJSON הוא [אורך, רוחב] - בישראל רוחב ~32 ואורך ~34.8
        assertEquals(32.0, route.polyline.first().lat, 0.2)
        assertEquals(34.8, route.polyline.first().lon, 0.2)
        assertEquals(listOf("depart", "turn", "continue", "turn", "continue", "turn", "end of road", "arrive"), route.steps.map { it.type })
        assertEquals("left", route.steps[1].modifier)
    }

    @Test
    fun noRouteAndErrors() {
        assertEquals(RouteLookup.NoRoute, OsrmParser.parse(Json.parseToJsonElement("""{"code":"NoRoute"}""") as JsonObject))
        assertEquals(RouteLookup.ServerError, OsrmParser.parse(Json.parseToJsonElement("""{"code":"InvalidQuery"}""") as JsonObject))
    }
}
