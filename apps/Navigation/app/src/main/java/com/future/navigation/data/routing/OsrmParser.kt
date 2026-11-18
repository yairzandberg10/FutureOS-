package com.future.navigation.data.routing

import com.future.navigation.data.common.LatLng
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * פירוק תשובת OSRM (route/v1, geometries=geojson, steps=true) למודל הנהיגה.
 * אוצר המילים של OSRM (type/modifier) הוא בדיוק מה ש-maneuverText מציג.
 */
object OsrmParser {

    /** המסלול הראשון בתשובה (לנהיגה רגילה, בלי חלופות). */
    fun parse(root: JsonObject): RouteLookup {
        if ((root["code"] as? JsonPrimitive)?.content != "Ok") {
            return if ((root["code"] as? JsonPrimitive)?.content == "NoRoute") RouteLookup.NoRoute else RouteLookup.ServerError
        }
        val route = (root["routes"] as? JsonArray)?.firstOrNull() as? JsonObject ?: return RouteLookup.NoRoute
        val parsed = parseRoute(route) ?: return RouteLookup.NoRoute
        return RouteLookup.Found(parsed)
    }

    /** כל המסלולים בתשובה (עם alternatives=true יש עד 3). ריק = אין מסלול או תשובה לא תקינה. */
    fun parseAll(root: JsonObject): List<DrivingRoute> {
        if ((root["code"] as? JsonPrimitive)?.content != "Ok") return emptyList()
        return (root["routes"] as? JsonArray).orEmpty()
            .mapNotNull { (it as? JsonObject)?.let(::parseRoute) }
    }

    private fun parseRoute(route: JsonObject): DrivingRoute? {
        val polyline = ((route["geometry"] as? JsonObject)?.get("coordinates") as? JsonArray).orEmpty()
            .mapNotNull { point -> (point as? JsonArray)?.toLatLng() }
        if (polyline.size < 2) return null

        val steps = (route["legs"] as? JsonArray).orEmpty()
            .flatMap { leg -> ((leg as? JsonObject)?.get("steps") as? JsonArray).orEmpty() }
            .mapNotNull { step ->
                val s = step as? JsonObject ?: return@mapNotNull null
                val maneuver = s["maneuver"] as? JsonObject ?: return@mapNotNull null
                Maneuver(
                    type = maneuver.text("type") ?: "turn",
                    modifier = maneuver.text("modifier"),
                    streetName = s.text("name").orEmpty(),
                    location = (maneuver["location"] as? JsonArray)?.toLatLng() ?: return@mapNotNull null,
                    distanceMeters = s.number("distance") ?: 0.0,
                    durationSeconds = s.number("duration") ?: 0.0,
                    exit = s.number("exit")?.toInt() ?: maneuver.number("exit")?.toInt(),
                )
            }
            .ifEmpty {
                listOf(
                    Maneuver("depart", null, "", polyline.first(), 0.0, 0.0),
                    Maneuver("arrive", null, "", polyline.last(), 0.0, 0.0),
                )
            }

        return DrivingRoute(
            polyline = polyline,
            steps = steps,
            distanceMeters = route.number("distance") ?: 0.0,
            durationSeconds = route.number("duration") ?: 0.0,
        )
    }

    /** ב-GeoJSON הסדר הוא [אורך, רוחב]. */
    private fun JsonArray.toLatLng(): LatLng? {
        val lon = (getOrNull(0) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        val lat = (getOrNull(1) as? JsonPrimitive)?.content?.toDoubleOrNull() ?: return null
        return LatLng(lat, lon)
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
}
