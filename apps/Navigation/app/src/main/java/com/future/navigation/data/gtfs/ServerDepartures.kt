package com.future.navigation.data.gtfs

import com.future.navigation.data.backend.FutureServer
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.network.NetworkModule
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.OffsetDateTime

/** יציאה אחת מתחנה: מספר הקו, היעד, ובעוד כמה דקות (לפי שעון השרת של HERE, כולל עיכוב אם דווח). */
data class StationDeparture(val line: String, val headsign: String, val minutesAway: Int)

/** תחנה שנמצאה בחיפוש לפי שם, עם הקווים שעוברים בה. */
data class StationHit(val name: String, val location: LatLng, val lines: List<String>)

/** תחנה קרובה עם היציאות הבאות ממנה - מסך הבית במצב תחבורה ציבורית (בסגנון Moovit). */
data class StationBoard(
    val name: String,
    val location: LatLng,
    val departures: List<StationDeparture>,
)

/**
 * תחנות קרובות וזמני יציאה מהשרת (HERE Public Transit Next Departures), בלי
 * נתוני GTFS על המכשיר. null = אין שרת או שהבקשה נכשלה, והמסך חוזר לנתונים המקומיים.
 */
class ServerDepartures {

    suspend fun near(at: LatLng, nowMillis: Long = System.currentTimeMillis()): List<StationBoard>? {
        val body = FutureServer.departuresJson(at) ?: return null
        return try {
            parse(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null, nowMillis)
        } catch (e: Exception) {
            null
        }
    }

    /** לוח היציאות של תחנה מסוימת: הלוח הקרוב ביותר למיקום שלה. */
    suspend fun boardAt(station: LatLng): StationBoard? {
        val boards = near(station) ?: return null
        return boards.minByOrNull { it.location.distanceMetersTo(station) }
            ?.takeIf { it.location.distanceMetersTo(station) <= SAME_STATION_METERS }
    }

    /** חיפוש תחנה לפי שם (לפחות 2 תווים) סביב המשתמש. null = אין שרת או כשל. */
    suspend fun searchStations(around: LatLng, name: String): List<StationHit>? {
        val body = FutureServer.stationsJson(around, name) ?: return null
        return try {
            parseStations(NetworkModule.json.parseToJsonElement(body) as? JsonObject ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    internal fun parseStations(root: JsonObject): List<StationHit> {
        val stations = root["stations"] as? JsonArray ?: return emptyList()
        return stations.mapNotNull { element ->
            val station = element as? JsonObject ?: return@mapNotNull null
            val place = station["place"] as? JsonObject ?: return@mapNotNull null
            val location = place["location"] as? JsonObject ?: return@mapNotNull null
            val lines = (station["transports"] as? JsonArray).orEmpty()
                .mapNotNull { (it as? JsonObject)?.let { t -> t.text("shortName") ?: t.text("name") } }
                .distinct()
            StationHit(
                name = place.text("name") ?: return@mapNotNull null,
                location = LatLng(location.number("lat") ?: return@mapNotNull null, location.number("lng") ?: return@mapNotNull null),
                lines = lines,
            )
        }
    }

    internal fun parse(root: JsonObject, nowMillis: Long): List<StationBoard> {
        val boards = root["boards"] as? JsonArray ?: return emptyList()
        return boards.mapNotNull { element ->
            val board = element as? JsonObject ?: return@mapNotNull null
            val place = board["place"] as? JsonObject ?: return@mapNotNull null
            val location = place["location"] as? JsonObject ?: return@mapNotNull null
            val departures = (board["departures"] as? JsonArray).orEmpty().mapNotNull { departure(it as? JsonObject, nowMillis) }
            StationBoard(
                name = place.text("name") ?: return@mapNotNull null,
                location = LatLng(location.number("lat") ?: return@mapNotNull null, location.number("lng") ?: return@mapNotNull null),
                departures = departures.sortedBy { it.minutesAway },
            )
        }
    }

    private fun departure(row: JsonObject?, nowMillis: Long): StationDeparture? {
        row ?: return null
        val time = row.text("time") ?: return null
        val departsAt = try { OffsetDateTime.parse(time).toInstant().toEpochMilli() } catch (e: Exception) { return null }
        // delay (שניות) מופיע רק כשיש נתון זמן אמת
        val delayMs = (row.number("delay") ?: 0.0) * 1000
        val minutes = ((departsAt + delayMs - nowMillis) / 60_000).toInt()
        if (minutes < 0) return null
        val transport = row["transport"] as? JsonObject
        return StationDeparture(
            line = transport?.text("shortName") ?: transport?.text("name") ?: return null,
            headsign = transport?.text("headsign").orEmpty(),
            minutesAway = minutes,
        )
    }

    private companion object {
        /** תחנה בחיפוש ולוח היציאות שלה נחשבים אותה תחנה עד 120 מ' זה מזה. */
        const val SAME_STATION_METERS = 120.0
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()
}
