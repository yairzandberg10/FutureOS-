package com.future.navigation.data.transit

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import com.future.navigation.data.online.OpenApis
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.OffsetDateTime
import java.time.ZoneId

/** תחנה אחת בדרך של אוטובוס/רכבת: שם, מיקום ושעה (שעון ישראל, שניות מחצות). */
data class TripStop(
    val name: String,
    val location: LatLng,
    val stopId: String?,
    val arrivalSeconds: Int?,
    val departureSeconds: Int?,
)

/** לוח הנסיעה המלא של קו: כל התחנות לפי הסדר. */
data class TripSchedule(
    val tripId: String,
    val line: String,
    val headsign: String,
    val stops: List<TripStop>,
    /** שם המפעיל (למשל "מטרופולין"), null אם השרת לא סיפק. */
    val agency: String? = null,
)

/**
 * לוח נסיעה מלא (בסגנון Moovit): כל התחנות של הנסיעה עם השעות, מ-Transitous
 * (`/api/v5/trip`). null = אין רשת, או שהנסיעה לא נמצאה (למשל תאריך שאינו בלוח).
 */
object TripScheduleSource {

    suspend fun load(tripId: String): TripSchedule? {
        val body = OpenApis.tripJson(tripId) ?: return null
        return try {
            parseTrip(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null, tripId)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * תשובת MOTIS trip: רשימת legs, ולכל רגל from/to ו-intermediateStops. לוקחים
     * את הרגל של אותה נסיעה (tripId) ומרכיבים ממנה את רשימת התחנות.
     */
    internal fun parseTrip(root: JsonObject, tripId: String): TripSchedule? {
        val legs = (root["legs"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val leg = legs.firstOrNull { it.text("tripId") == tripId } ?: legs.firstOrNull() ?: return null
        val from = leg["from"] as? JsonObject ?: return null
        val to = leg["to"] as? JsonObject ?: return null
        val stops = buildList {
            stop(from, from.text("departure") ?: from.text("arrival"), from.text("departure"))?.let { add(it) }
            (leg["intermediateStops"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }.forEach { s ->
                stop(s, s.text("arrival"), s.text("departure"))?.let { add(it) }
            }
            stop(to, to.text("arrival"), null)?.let { add(it) }
        }
        if (stops.size < 2) return null
        return TripSchedule(
            tripId = tripId,
            line = leg.text("routeShortName")?.takeIf { it.isNotBlank() } ?: leg.text("displayName").orEmpty(),
            headsign = leg.text("headsign")?.replace('_', ' ')?.trim().orEmpty(),
            stops = stops,
            agency = leg.text("agencyName")?.takeIf { it.isNotBlank() },
        )
    }

    private fun stop(place: JsonObject, arrival: String?, departure: String?): TripStop? {
        val name = place.text("name")?.takeIf { it.isNotBlank() } ?: return null
        val lat = place.number("lat") ?: return null
        val lon = place.number("lon") ?: return null
        return TripStop(
            name = name,
            location = LatLng(lat, lon),
            stopId = place.text("stopId"),
            arrivalSeconds = secondsOfDay(arrival),
            departureSeconds = secondsOfDay(departure),
        )
    }

    /** שעת ISO (UTC) לשניות מחצות בשעון ישראל. */
    internal fun secondsOfDay(iso: String?): Int? {
        if (iso == null) return null
        val t = try { OffsetDateTime.parse(iso) } catch (e: Exception) { return null }
        return t.atZoneSameInstant(ISRAEL).toLocalTime().toSecondOfDay()
    }

    private val ISRAEL: ZoneId = ZoneId.of("Asia/Jerusalem")

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
}
