package com.future.navigation.data.lines

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import com.future.navigation.data.online.OpenApis
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

/** יציאה אחת של קו מתחנה: מה שצריך לחיפוש, לתחנה, לכיוון הנגדי ולפתיחת הנסיעה. */
data class LineDeparture(
    val line: String,
    val headsign: String,
    val stopName: String,
    val stopId: String?,
    val location: LatLng,
    val departsAtMillis: Long,
    val tripId: String?,
    val agency: String?,
    val realtime: Boolean,
)

/**
 * שאילתות יציאות של קווים מ-Transitous (MOTIS stoptimes). הקריאה לתחנות הקרובות
 * משתמשת באותו שירות של OpenApis; השאילתות לפי stopId ולפי יום כאן, כי OpenApis
 * לא מעביר שעה ולא מזהה תחנה. כל פונקציה מחזירה null בכשל רשת.
 */
object LineTimes {
    private const val TRANSITOUS = "https://api.transitous.org"
    private const val USER_AGENT = "FutureOS-Navigation/1.0 (keypad phone app)"
    private const val NEARBY_RADIUS_METERS = 800
    private const val NEARBY_COUNT = 240
    /** כמה יציאות מביאים בתחנה אחת לרענון קבוע (מספיק לארבע היציאות הבאות של הקו). */
    private const val POLL_COUNT = 120
    private const val DAY_PAGE_SIZE = 400
    private const val MAX_DAY_PAGES = 8

    /** היציאות הקרובות מכל התחנות ברדיוס של 800 מ' מנקודה (לחיפוש קו ולכיוון הנגדי). */
    suspend fun nearby(at: LatLng): List<LineDeparture>? {
        val body = OpenApis.stopTimesNearJson(at, NEARBY_RADIUS_METERS, NEARBY_COUNT) ?: return null
        return parseBody(body)
    }

    /** היציאות הבאות בתחנה אחת (לפי stopId), החל מהשעה [nowMillis]. */
    suspend fun atStop(stopId: String, nowMillis: Long, count: Int = POLL_COUNT): List<LineDeparture>? {
        val url = stopTimesUrl(stopId, time = isoInstant(nowMillis), cursor = null, count = count)
        return get(url)?.let { parseBody(it) }
    }

    /**
     * כל היציאות של תחנה ביום [dayStartMillis, dayEndMillis). מדפדפים בעמודים
     * (pageCursor) עד שמגיעים לסוף היום, עם תקרה על מספר העמודים.
     */
    suspend fun dayAtStop(stopId: String, dayStartMillis: Long, dayEndMillis: Long): List<LineDeparture>? {
        val collected = mutableListOf<LineDeparture>()
        var cursor: String? = null
        for (page in 0 until MAX_DAY_PAGES) {
            val time = if (cursor == null) isoInstant(dayStartMillis) else null
            val root = get(stopTimesUrl(stopId, time = time, cursor = cursor, count = DAY_PAGE_SIZE))?.let { parseRoot(it) }
            if (root == null) {
                if (page == 0) return null
                break
            }
            val rows = parseStopTimes(root)
            collected += rows
            cursor = root.text("nextPageCursor")
            val reachedEnd = (rows.maxOfOrNull { it.departsAtMillis } ?: Long.MAX_VALUE) >= dayEndMillis
            if (cursor == null || reachedEnd) break
        }
        // דפים חוזרים (אם השרת מתעלם מהסמן) לא יכפילו שורות בלוח
        return collected
            .distinctBy { Triple(it.line, it.departsAtMillis, it.tripId ?: it.stopName) }
            .filter { it.departsAtMillis in dayStartMillis until dayEndMillis }
    }

    /**
     * מזהה נסיעה חיה של קו שנשמר (הנסיעה משתנה כל יום): קודם מהיציאות בתחנה שלו,
     * ואם אין - מהיציאות הקרובות לנקודה שלו.
     */
    suspend fun resolveTrip(line: SavedLine, nowMillis: Long): String? {
        val fromStop = line.stopId?.let { atStop(it, nowMillis) }.orEmpty()
        LineLogic.firstTrip(fromStop, line.line, line.headsign, nowMillis)?.let { return it }
        val around = nearby(LatLng(line.lat, line.lon)).orEmpty()
        return LineLogic.firstTrip(around, line.line, line.headsign, nowMillis)
    }

    /** פירוק תשובת stoptimes: שורה לכל יציאה, עם התחנה שלה בתוך place. */
    internal fun parseStopTimes(root: JsonObject): List<LineDeparture> =
        (root["stopTimes"] as? JsonArray).orEmpty().mapNotNull { element ->
            val row = element as? JsonObject ?: return@mapNotNull null
            if (row.bool("cancelled") || row.bool("tripCancelled")) return@mapNotNull null
            val place = row["place"] as? JsonObject ?: return@mapNotNull null
            val line = row.text("routeShortName")?.takeIf { it.isNotBlank() }
                ?: row.text("displayName")?.takeIf { it.isNotBlank() && it.length <= 6 }
                ?: return@mapNotNull null
            val stopName = place.text("name")?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val lat = place.number("lat") ?: return@mapNotNull null
            val lon = place.number("lon") ?: return@mapNotNull null
            val departsAt = place.text("departure")
                ?.let { runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() }
                ?: return@mapNotNull null
            LineDeparture(
                line = line,
                headsign = LineLogic.headsignKey(row.text("headsign")),
                stopName = stopName,
                stopId = place.text("stopId")?.takeIf { it.isNotBlank() },
                location = LatLng(lat, lon),
                departsAtMillis = departsAt,
                tripId = row.text("tripId")?.takeIf { it.isNotBlank() },
                agency = row.text("agencyName")?.takeIf { it.isNotBlank() },
                realtime = row.bool("realTime"),
            )
        }

    private fun parseBody(body: String): List<LineDeparture>? = parseRoot(body)?.let { parseStopTimes(it) }

    private fun parseRoot(body: String): JsonObject? =
        try {
            NetworkModule.json.parseToJsonElement(body) as? JsonObject
        } catch (e: Exception) {
            null
        }

    private fun stopTimesUrl(stopId: String, time: String?, cursor: String?, count: Int): HttpUrl =
        "$TRANSITOUS/api/v5/stoptimes".toHttpUrl().newBuilder()
            .addQueryParameter("stopId", stopId)
            .addQueryParameter("n", count.toString())
            .addQueryParameter("language", "he")
            .apply {
                if (cursor != null) {
                    addQueryParameter("pageCursor", cursor)
                } else if (time != null) {
                    addQueryParameter("time", time)
                }
            }
            .build()

    /** שעה בפורמט ISO בלי מילישניות ובלי "+" (נשלח כ-Z כדי שלא ייהפך לרווח בשאילתה). */
    private fun isoInstant(millis: Long): String =
        Instant.ofEpochMilli(millis).truncatedTo(ChronoUnit.SECONDS).toString()

    private suspend fun get(url: HttpUrl): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()

    private fun JsonObject.bool(key: String): Boolean =
        (this[key] as? JsonPrimitive)?.content == "true"
}
