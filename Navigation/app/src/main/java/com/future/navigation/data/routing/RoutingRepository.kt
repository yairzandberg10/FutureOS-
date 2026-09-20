package com.future.navigation.data.routing

import com.future.navigation.data.backend.NavigationFunctions
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
 * מימוש דרך HERE Technologies Routing & Traffic API (v8) - מנוע נהיגה אמיתי
 * עם פקקים בזמן אמת (ר' HereApi.kt לפירוט return=turnByTurnActions ולמה זה
 * מבטיח שיקלול פקקים חי, לא רק לוח זמנים היסטורי). דורש מפתח API אישי
 * (HereConfig.apiKey, מגיע מ-local.properties) - ר' HereConfig.kt.
 *
 * שמור מאחורי הממשק RoutingRepository כדי שהחלפה לספק אחר תהיה שינוי בקובץ
 * הזה בלבד.
 */
class HereRoutingRepository : RoutingRepository {
    private val api: HereApi = NetworkModule
        .retrofit(HereConfig.BASE_URL)
        .create(HereApi::class.java)

    override suspend fun getDrivingRoute(from: LatLng, to: LatLng): DrivingRoute? {
        if (!HereConfig.isConfigured) return null

        val response = fetchViaProxy(from, to) ?: fetchDirectly(from, to) ?: return null

        val route = response.routes.firstOrNull() ?: return null
        val section = route.sections.firstOrNull() ?: return null
        val polyline = FlexiblePolyline.decode(section.polyline)
        if (polyline.isEmpty()) return null

        val steps = section.turnByTurnActions.map { action ->
            Maneuver(
                type = mapActionType(action.action),
                modifier = mapModifier(action.action, action.direction, action.severity),
                streetName = streetNameOf(action),
                location = polyline.getOrElse(action.offset) { polyline.last() },
                distanceMeters = action.length,
                durationSeconds = action.duration
            )
        }

        val summary = section.travelSummary ?: section.summary
        return DrivingRoute(
            polyline = polyline,
            steps = steps,
            distanceMeters = summary?.length ?: 0.0,
            durationSeconds = summary?.duration ?: 0.0
        )
    }

    /**
     * הנתיב הראשי: Cloud Function שמחזיקה את מפתח HERE בסוד בצד השרת. היא
     * מחזירה את גוף התשובה הגולמי, ולכן הפירוק נשאר בדיוק אותו קוד של הנתיב
     * הישיר - אין שתי גרסאות של אותו פירוק. null = אין שרת/נכשל.
     */
    private suspend fun fetchViaProxy(from: LatLng, to: LatLng): HereRouteResponse? {
        val body = NavigationFunctions.drivingRouteJson(from, to) ?: return null
        return try {
            NetworkModule.json.decodeFromString(HereRouteResponse.serializer(), body)
        } catch (e: Exception) {
            null
        }
    }

    /** נתיב הנפילה לאחור: קריאה ישירה ל-HERE עם המפתח שעל המכשיר. */
    private suspend fun fetchDirectly(from: LatLng, to: LatLng): HereRouteResponse? {
        if (HereConfig.apiKey.isBlank()) return null
        return try {
            api.routes(
                origin = formatCoordinate(from),
                destination = formatCoordinate(to),
                apiKey = HereConfig.apiKey
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun formatCoordinate(point: LatLng): String =
        String.format(Locale.US, "%f,%f", point.lat, point.lon)

    private fun streetNameOf(action: HereAction): String =
        action.nextRoad?.name?.firstOrNull()?.value
            ?: action.currentRoad?.name?.firstOrNull()?.value
            ?: ""

    /**
     * ממפה את אוצר המילים של HERE (action) לזה של OSRM שהמסך הקיים
     * (NavigateScreen.maneuverText) כבר יודע להציג - כך שאין צורך לגעת במסך
     * הניווט בכלל. roundaboutEnter/Pass/Exit כולם "כיכר, המשך לפי השילוט"
     * (יותר שימושי ממחוון כיוון גולמי לכיכר).
     */
    private fun mapActionType(hereAction: String): String = when (hereAction) {
        "depart" -> "depart"
        "arrive" -> "arrive"
        "roundaboutEnter", "roundaboutPass", "roundaboutExit" -> "roundabout"
        else -> "turn"
    }

    /**
     * HERE מפרק את מה ש-OSRM קורא לו "modifier" יחיד (למשל "slight left") לשני
     * שדות נפרדים: direction (left/right/middle) + severity (light/quite/heavy
     * - כן, "quite" ולא "quiet", כך במפרט הרשמי). מרכיבים אותם מחדש כאן.
     */
    private fun mapModifier(hereAction: String, direction: String?, severity: String?): String? {
        if (hereAction == "uTurn") return "uturn"
        val side = when (direction) {
            "left" -> "left"
            "right" -> "right"
            else -> return null
        }
        return when (severity) {
            "light" -> "slight $side"
            "heavy" -> "sharp $side"
            else -> side
        }
    }
}
