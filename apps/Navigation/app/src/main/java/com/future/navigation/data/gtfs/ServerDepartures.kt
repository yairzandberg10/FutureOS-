package com.future.navigation.data.gtfs

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.network.NetworkModule
import com.future.navigation.data.online.OpenApis
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId

/** יציאה אחת מתחנה: מספר הקו, היעד, ובעוד כמה דקות (כולל זמן אמת כשיש). */
data class StationDeparture(
    val line: String,
    val headsign: String,
    val minutesAway: Int,
    /** מזהה הנסיעה (ל"לוח נסיעה מלא" של הקו), null אם לא ידוע. */
    val tripId: String? = null,
    /** שעת היציאה בשניות מחצות (שעון ישראל). */
    val departsAtSeconds: Int? = null,
    /** true כשהשעה מגיעה מזמן אמת ולא מהלו"ז. */
    val realtime: Boolean = false,
    /** רציף/מסלול, כשמשרד התחבורה מספק אותו. */
    val platform: String? = null,
)

/** תחנה שנמצאה בחיפוש לפי שם, עם הקווים שעוברים בה (אם ידועים). */
data class StationHit(val name: String, val location: LatLng, val lines: List<String>)

/** תחנה קרובה עם היציאות הבאות ממנה - מסך הבית במצב תחבורה ציבורית (בסגנון Moovit). */
data class StationBoard(
    val name: String,
    val location: LatLng,
    val departures: List<StationDeparture>,
)

/**
 * תחנות קרובות, לוח יציאות וחיפוש תחנות ברשת (Transitous, ר' OpenApis), בלי
 * נתוני GTFS על המכשיר ובלי מפתח. null = אין רשת או שהשירות לא ענה.
 */
class ServerDepartures {

    suspend fun near(at: LatLng, nowMillis: Long = System.currentTimeMillis()): List<StationBoard>? {
        val body = OpenApis.stopTimesNearJson(at, NEAR_RADIUS_METERS, NEAR_COUNT) ?: return null
        return try {
            parse(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null, nowMillis)
        } catch (e: Exception) {
            null
        }
    }

    /** לוח היציאות של תחנה מסוימת: התחנה הקרובה ביותר למיקום שלה. */
    suspend fun boardAt(station: LatLng, name: String? = null, nowMillis: Long = System.currentTimeMillis()): StationBoard? {
        val body = OpenApis.stopTimesNearJson(station, SAME_STATION_METERS, BOARD_COUNT) ?: return null
        val boards = try {
            parse(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null, nowMillis)
        } catch (e: Exception) {
            return null
        }
        // קודם התחנה באותו שם: תחנה סמוכה עם יציאות מוקדמות יותר "גנבה" קודם את הלוח
        if (name != null) {
            // אין יציאות קרובות מהתחנה הזו - לוח ריק שלה, לא לוח של תחנה אחרת
            // בחיפוש השם כולל את העיר ("תחנה מרכזית, אשקלון")
            return boards.firstOrNull { it.name == name || name.startsWith(it.name + ", ") }
                ?: StationBoard(name, station, emptyList())
        }
        return boards.minByOrNull { it.location.distanceMetersTo(station) }
    }

    /** חיפוש תחנה לפי שם (לפחות 2 תווים), עם עדיפות לתחנות קרובות. null = כשל. */
    suspend fun searchStations(around: LatLng, name: String): List<StationHit>? {
        val body = OpenApis.stopSearchJson(name, around) ?: return null
        return try {
            parseStations(NetworkModule.json.parseToJsonElement(body) as? JsonArray ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    internal fun parseStations(matches: JsonArray): List<StationHit> =
        matches.mapNotNull { element ->
            val match = element as? JsonObject ?: return@mapNotNull null
            val name = match.text("name") ?: return@mapNotNull null
            // שם העיר מבדיל בין "תחנה מרכזית" באשקלון לזו שבעפולה
            val city = (match["areas"] as? JsonArray).orEmpty()
                .mapNotNull { it as? JsonObject }
                .firstOrNull { it.bool("default") }
                ?.text("name")
            val lat = match.number("lat") ?: return@mapNotNull null
            val lon = match.number("lon") ?: return@mapNotNull null
            // Transitous מכסה את כל העולם - "Ariel" החזיר תחנות בצרפת ובמקסיקו
            if (lat !in 29.4..33.4 || lon !in 34.2..35.95) return@mapNotNull null
            StationHit(
                name = if (city != null && city != name) "$name, $city" else name,
                location = LatLng(lat, lon),
                lines = emptyList(),
            )
        }.distinctBy { it.name to it.location }

    /**
     * stopTimes של MOTIS: שורה לכל יציאה, עם התחנה שלה ב-place. מקבצים לפי שם
     * התחנה (שני צדי הרחוב עם אותו שם הם תחנה אחת בעיני המשתמש).
     */
    internal fun parse(root: JsonObject, nowMillis: Long): List<StationBoard> {
        val rows = (root["stopTimes"] as? JsonArray).orEmpty().mapNotNull { it as? JsonObject }
        val byStation = LinkedHashMap<String, Pair<LatLng, MutableList<StationDeparture>>>()
        for (row in rows) {
            if (row.bool("cancelled") || row.bool("tripCancelled")) continue
            val place = row["place"] as? JsonObject ?: continue
            val name = place.text("name") ?: continue
            val location = LatLng(place.number("lat") ?: continue, place.number("lon") ?: continue)
            val departure = departure(row, place, nowMillis) ?: continue
            byStation.getOrPut(name) { location to mutableListOf() }.second += departure
        }
        return byStation.map { (name, entry) ->
            StationBoard(
                name = name,
                location = entry.first,
                departures = entry.second.sortedBy { it.minutesAway }.distinct(),
            )
        }
    }

    private fun departure(row: JsonObject, place: JsonObject, nowMillis: Long): StationDeparture? {
        val time = place.text("departure") ?: return null
        val departsAt = try { OffsetDateTime.parse(time).toInstant().toEpochMilli() } catch (e: Exception) { return null }
        // בדיקה במילישניות: חלוקה שלמה הייתה מעגלת "לפני חצי דקה" ל-0 ומציגה אוטובוס שכבר יצא
        if (departsAt < nowMillis) return null
        val minutes = ((departsAt - nowMillis) / 60_000).toInt()
        return StationDeparture(
            line = row.text("routeShortName")?.takeIf { it.isNotBlank() }
                ?: row.text("displayName")?.takeIf { it.isNotBlank() && it.length <= 6 }
                ?: "רכבת",
            headsign = row.text("headsign")?.replace('_', ' ')?.trim().orEmpty(),
            minutesAway = minutes,
            tripId = row.text("tripId")?.takeIf { it.isNotBlank() },
            departsAtSeconds = Instant.ofEpochMilli(departsAt).atZone(ISRAEL).toLocalTime().toSecondOfDay(),
            realtime = row.bool("realTime"),
            platform = place.text("platformCode")?.takeIf { it.isNotBlank() }
                ?: place.text("track")?.takeIf { it.isNotBlank() },
        )
    }

    private companion object {
        val ISRAEL: ZoneId = ZoneId.of("Asia/Jerusalem")
        const val NEAR_RADIUS_METERS = 500
        const val NEAR_COUNT = 60
        const val SAME_STATION_METERS = 150
        const val BOARD_COUNT = 80
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun JsonObject.bool(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.content == "true"
}
