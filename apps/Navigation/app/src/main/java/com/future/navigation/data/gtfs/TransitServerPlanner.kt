package com.future.navigation.data.gtfs

import com.future.navigation.data.backend.FutureServer
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * תכנון מסלול תחבורה ציבורית בצד השרת: המכשיר שולח מוצא ויעד ומקבל רק את
 * המסלולים, בלי להוריד ולייבא את נתוני ה-GTFS (~130MB). null = השרת לא ענה,
 * והקורא חוזר לתכנון המקומי מנתוני GTFS.
 */
class TransitServerPlanner {

    suspend fun findJourneys(from: LatLng, to: LatLng): List<TransitItinerary>? {
        val body = FutureServer.transitJson(from, to) ?: return null
        return try {
            parse(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    internal fun parse(root: JsonObject): List<TransitItinerary> {
        val routes = root["routes"] as? JsonArray ?: return emptyList()
        return routes.mapNotNull { (it as? JsonObject)?.let(::itinerary) }
    }

    private fun itinerary(route: JsonObject): TransitItinerary? {
        val sections = (route["sections"] as? JsonArray)?.mapNotNull { it as? JsonObject } ?: return null
        val legs = sections.mapNotNull(::leg)
        if (legs.none { it.type == LegType.RIDE }) return null
        val departure = legs.first().departureSeconds ?: return null
        val arrival = legs.last().arrivalSeconds ?: return null
        return TransitItinerary(legs, departure, arrival)
    }

    private fun leg(section: JsonObject): TransitLeg? {
        val departure = section["departure"] as? JsonObject
        val arrival = section["arrival"] as? JsonObject
        val dep = secondsOfDay(departure?.text("time"))
        val arr = secondsOfDay(arrival?.text("time"))
        val fromName = departure.placeName()
        val toName = arrival.placeName()
        val summary = section["travelSummary"] as? JsonObject ?: section["summary"] as? JsonObject
        return when (section.text("type")) {
            "transit" -> {
                val transport = section["transport"] as? JsonObject
                val stops = (section["intermediateStops"] as? JsonArray).orEmpty()
                    .mapNotNull { ((it as? JsonObject)?.get("departure") as? JsonObject).placeName().takeIf { n -> n.isNotBlank() } }
                TransitLeg(
                    type = LegType.RIDE,
                    routeShortName = transport?.text("shortName") ?: transport?.text("name"),
                    routeLongName = transport?.text("headsign"),
                    fromStopName = fromName,
                    toStopName = toName,
                    departureSeconds = dep,
                    arrivalSeconds = arr,
                    intermediateStopNames = stops,
                )
            }
            "pedestrian" -> TransitLeg(
                type = LegType.WALK,
                fromStopName = fromName,
                toStopName = toName,
                departureSeconds = dep,
                arrivalSeconds = arr,
                walkDistanceMeters = summary?.number("length"),
                walkDurationSeconds = summary?.number("duration")?.toInt(),
            )
            else -> null
        }
    }

    /** HERE מחזיר זמן ISO עם הפרש אזור; המודל המקומי עובד בשניות מחצות לפי שעון ישראל. */
    private fun secondsOfDay(iso: String?): Int? {
        if (iso == null) return null
        val t = try { OffsetDateTime.parse(iso) } catch (e: Exception) { return null }
        return t.atZoneSameInstant(ZoneId.of("Asia/Jerusalem")).toLocalTime().toSecondOfDay()
    }

    private fun JsonObject?.placeName(): String =
        ((this?.get("place") as? JsonObject)?.text("name")).orEmpty()

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
}
