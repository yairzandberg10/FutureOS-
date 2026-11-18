package com.future.navigation.data.gtfs

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import com.future.navigation.data.online.OpenApis
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.OffsetDateTime
import java.time.ZoneId

/**
 * תכנון מסלול תחבורה ציבורית ברשת (Transitous / MOTIS, ר' OpenApis): המכשיר
 * שולח מוצא ויעד ומקבל רק את המסלולים, בלי להוריד ולייבא את נתוני ה-GTFS
 * (~130MB). עובד בלי מפתח ובלי שרת משלנו. null = אין רשת או שהשירות לא ענה.
 */
class TransitServerPlanner {

    /** departAt: שעת יציאה ISO-8601, null = עכשיו. */
    suspend fun findJourneys(from: LatLng, to: LatLng, departAt: String? = null): List<TransitItinerary>? {
        val body = OpenApis.transitPlanJson(from, to, departAt) ?: return null
        return try {
            parse(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    internal fun parse(root: JsonObject): List<TransitItinerary> {
        val itineraries = root["itineraries"] as? JsonArray ?: return emptyList()
        return itineraries.mapNotNull { (it as? JsonObject)?.let(::itinerary) }
    }

    private fun itinerary(item: JsonObject): TransitItinerary? {
        val legs = (item["legs"] as? JsonArray).orEmpty().mapNotNull { (it as? JsonObject)?.let(::leg) }
        if (legs.none { it.type == LegType.RIDE }) return null
        val departure = secondsOfDay(item.text("startTime")) ?: legs.first().departureSeconds ?: return null
        val arrival = secondsOfDay(item.text("endTime")) ?: legs.last().arrivalSeconds ?: return null
        // נסיעה שעוברת חצות: ההגעה "קטנה" מהיציאה בשניות-מחצות
        return TransitItinerary(legs, departure, if (arrival < departure) arrival + DAY else arrival)
    }

    private fun leg(leg: JsonObject): TransitLeg? {
        val from = leg["from"] as? JsonObject ?: return null
        val to = leg["to"] as? JsonObject ?: return null
        val dep = secondsOfDay(leg.text("startTime"))
        val arr = secondsOfDay(leg.text("endTime"))
        val fromName = from.text("name").orEmpty().let { if (it == "START") "המיקום שלך" else it }
        val toName = to.text("name").orEmpty().let { if (it == "END") "היעד" else it }
        return when (val mode = leg.text("mode").orEmpty()) {
            "WALK", "BIKE", "CAR", "FLEX", "RENTAL" -> TransitLeg(
                type = LegType.WALK,
                fromStopName = fromName,
                toStopName = toName,
                departureSeconds = dep,
                arrivalSeconds = arr,
                walkDistanceMeters = leg.number("distance"),
                walkDurationSeconds = leg.number("duration")?.toInt(),
                // מיקומי ההתחלה והסיום: ניווט ברגל לתחנת העלייה, בין תחנות, וליעד
                fromLocation = from.location(),
                toLocation = to.location(),
            )
            "" -> null
            else -> {
                val stops = (leg["intermediateStops"] as? JsonArray).orEmpty()
                    .mapNotNull { (it as? JsonObject)?.text("name")?.takeIf { n -> n.isNotBlank() } }
                // זמן אמת: MOTIS מסמן realTime ונותן גם את הזמן המתוכנן
                val realtime = leg.bool("realTime")
                val scheduled = secondsOfDay(leg.text("scheduledStartTime"))
                val delay = if (realtime && dep != null && scheduled != null) dep - scheduled else null
                TransitLeg(
                    type = LegType.RIDE,
                    routeShortName = lineName(leg, mode),
                    routeLongName = headsign(leg),
                    fromStopName = fromName,
                    toStopName = toName,
                    departureSeconds = dep,
                    arrivalSeconds = arr,
                    intermediateStopNames = stops,
                    fromStopId = from.text("stopCode"),
                    isRealtime = realtime,
                    delaySeconds = delay,
                    toLocation = to.location(),
                    tripId = leg.text("tripId")?.takeIf { it.isNotBlank() },
                )
            }
        }
    }

    /** מספר הקו. ברכבת אין מספר קו, אז מציגים "רכבת" (או מספר הרכבת אם יש). */
    private fun lineName(leg: JsonObject, mode: String): String {
        // displayName ברכבת הוא שם המסלול המלא ("א<->ב"), לא מספר - לוקחים אותו רק כשהוא קצר
        val name = leg.text("routeShortName")?.takeIf { it.isNotBlank() }
            ?: leg.text("tripShortName")?.takeIf { it.isNotBlank() }
            ?: leg.text("displayName")?.takeIf { it.isNotBlank() && it.length <= 6 }
        val kind = MODE_NAMES[mode]
        return when {
            name != null && kind != null && mode != "BUS" -> "$kind $name"
            name != null -> name
            else -> kind ?: "קו"
        }
    }

    /** כיוון הנסיעה. ברכבת headsign הוא מספר הרכבת, ואז מציגים את שם המסלול. */
    private fun headsign(leg: JsonObject): String? {
        val headsign = leg.text("headsign")?.replace('_', ' ')?.trim()
        if (!headsign.isNullOrBlank() && headsign.any { it.isLetter() }) return headsign
        return leg.text("routeLongName")?.replace("<->", " - ")?.takeIf { it.isNotBlank() }
    }

    private fun secondsOfDay(iso: String?): Int? {
        if (iso == null) return null
        val t = try { OffsetDateTime.parse(iso) } catch (e: Exception) { return null }
        return t.atZoneSameInstant(ISRAEL).toLocalTime().toSecondOfDay()
    }

    private fun JsonObject.location(): LatLng? {
        val lat = number("lat") ?: return null
        val lon = number("lon") ?: return null
        return LatLng(lat, lon)
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun JsonObject.bool(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.content == "true"

    private companion object {
        val ISRAEL: ZoneId = ZoneId.of("Asia/Jerusalem")
        const val DAY = 24 * 3600
        val MODE_NAMES = mapOf(
            "BUS" to "אוטובוס",
            "COACH" to "אוטובוס",
            "RAIL" to "רכבת",
            "REGIONAL_RAIL" to "רכבת",
            "REGIONAL_FAST_RAIL" to "רכבת",
            "LONG_DISTANCE" to "רכבת",
            "HIGHSPEED_RAIL" to "רכבת",
            "SUBURBAN" to "רכבת",
            "METRO" to "רכבת קלה",
            "SUBWAY" to "רכבת קלה",
            "TRAM" to "רכבת קלה",
            "CABLE_CAR" to "רכבל",
            "FERRY" to "מעבורת",
        )
    }
}
