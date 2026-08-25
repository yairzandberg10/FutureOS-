package com.future.navigation.data.routing

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import java.util.Locale

data class Maneuver(
    val type: String,
    val modifier: String?,
    val streetName: String,
    val location: LatLng,
    val distanceMeters: Double,
    val durationSeconds: Double
)

data class DrivingRoute(
    val polyline: List<LatLng>,
    val steps: List<Maneuver>,
    val distanceMeters: Double,
    val durationSeconds: Double
)

interface RoutingRepository {
    suspend fun getDrivingRoute(from: LatLng, to: LatLng): DrivingRoute?
}

/**
 * מימוש דרך שרת ההדגמה הציבורי של OSRM. שמור מאחורי הממשק RoutingRepository
 * כדי שהחלפה לשרת עצמאי או ספק אחר תהיה שינוי בקובץ הזה בלבד.
 */
class OsrmRoutingRepository : RoutingRepository {
    private val api: OsrmApi = NetworkModule
        .retrofit("https://router.project-osrm.org/")
        .create(OsrmApi::class.java)

    override suspend fun getDrivingRoute(from: LatLng, to: LatLng): DrivingRoute? {
        val coords = String.format(
            Locale.US,
            "%f,%f;%f,%f",
            from.lon, from.lat, to.lon, to.lat
        )
        val response = try {
            api.route(coords)
        } catch (e: Exception) {
            return null
        }
        if (response.code != "Ok") return null
        val route = response.routes.firstOrNull() ?: return null

        val polyline = route.geometry.coordinates.map { LatLng(lat = it[1], lon = it[0]) }
        val steps = route.legs.flatMap { it.steps }.map { step ->
            Maneuver(
                type = step.maneuver.type,
                modifier = step.maneuver.modifier,
                streetName = step.name,
                location = LatLng(lat = step.maneuver.location[1], lon = step.maneuver.location[0]),
                distanceMeters = step.distance,
                durationSeconds = step.duration
            )
        }

        return DrivingRoute(
            polyline = polyline,
            steps = steps,
            distanceMeters = route.distance,
            durationSeconds = route.duration
        )
    }
}
