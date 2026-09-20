package com.future.navigation.data.backend

import android.util.Log
import com.future.navigation.data.gtfs.BoundingBox
import com.future.navigation.data.gtfs.CalendarEntity
import com.future.navigation.data.gtfs.GtfsDatabase
import com.future.navigation.data.gtfs.ImportPhase
import com.future.navigation.data.gtfs.ImportProgress
import com.future.navigation.data.gtfs.RouteEntity
import com.future.navigation.data.gtfs.StopEntity
import com.future.navigation.data.gtfs.StopTimeEntity
import com.future.navigation.data.gtfs.TripEntity
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import java.util.zip.GZIPInputStream

/**
 * נתוני התחבורה הציבורית מגיעים מהשרת במקום מייבוא מקומי.
 *
 * עד כה כל מכשיר הוריד את קובץ ה-GTFS הארצי (~130MB ZIP, ר' GtfsConfig) ופרק
 * אותו בעצמו - הורדה ארוכה ברשת סלולרית, וקריאת CSV של מיליוני שורות על
 * מעבד של מכשיר בסיסי, פעם אחר פעם בכל מכשיר. עכשיו Cloud Function מתוזמנת
 * עושה את זה פעם ביום בצד השרת, מסננת לאזור המבוקש, וכותבת חבילה דחוסה
 * (NDJSON ב-gzip) ל-Cloud Storage; המכשיר מוריד רק אותה.
 *
 * הפורמט הוא NDJSON ולא JSON אחד גדול בכוונה: המכשיר מפרק שורה-שורה בזיכרון
 * קבוע, בלי להחזיק את כל החבילה בזיכרון בבת אחת.
 *
 * הטבלאות המקומיות (Room) נשארות בדיוק כפי שהן - כל שאר האפליקציה
 * (GtfsDao/TransitJourneyPlanner) לא יודעת מאיפה הגיעו הנתונים, וגם אחרי
 * המעבר תכנון מסלול עדיין עובד אופליין.
 */
class TransitBundleSource(private val database: GtfsDatabase) {

    data class BundleRef(
        val regionId: String,
        val storagePath: String,
        val version: String,
        val bbox: BoundingBox,
    )

    /**
     * החבילה שמכסה את [bbox] המבוקש, או null אם אין כזו (ואז חוזרים לייבוא
     * המקומי). הרשימה קטנה (אזור אחד או שניים), אז הסינון נעשה כאן ולא
     * בשאילתה - Firestore לא יודע לשאול טווח על ארבעה שדות שונים בבת אחת.
     */
    suspend fun findBundle(bbox: BoundingBox): BundleRef? {
        if (!RemoteKeys.transitDataFromServer) return null
        val firestore = FirebaseBackend.firestore ?: return null
        return try {
            val snapshot = firestore.collection(COLLECTION_BUNDLES).get().awaitResult()
            snapshot.documents.mapNotNull { document ->
                val path = document.getString("storagePath") ?: return@mapNotNull null
                val minLat = document.getDouble("minLat") ?: return@mapNotNull null
                val maxLat = document.getDouble("maxLat") ?: return@mapNotNull null
                val minLon = document.getDouble("minLon") ?: return@mapNotNull null
                val maxLon = document.getDouble("maxLon") ?: return@mapNotNull null
                BundleRef(
                    regionId = document.id,
                    storagePath = path,
                    version = document.getString("version") ?: "",
                    bbox = BoundingBox(minLat, maxLat, minLon, maxLon),
                )
            }.firstOrNull { it.bbox.covers(bbox) }
        } catch (e: Exception) {
            Log.w(TAG, "could not read the transit bundle index", e)
            null
        }
    }

    /**
     * מוריד ומייבא את החבילה. מדווח באותו ImportProgress שהמסך כבר
     * מציג, כדי שמסך ההתקנה לא ידע להבדיל בין שני המקורות.
     */
    suspend fun import(bundle: BundleRef, collector: FlowCollector<ImportProgress>) {
        val storage = FirebaseBackend.storage
            ?: throw IllegalStateException("אחסון Firebase לא זמין")

        collector.emit(ImportProgress(ImportPhase.DOWNLOADING, 0f, message = "מוריד נתוני תחבורה"))
        val downloadUrl = storage.reference.child(bundle.storagePath).downloadUrl.awaitResult()

        val request = Request.Builder().url(downloadUrl.toString()).build()
        NetworkModule.okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("הורדת חבילת התחבורה נכשלה: ${response.code}")
            }
            val body = response.body ?: throw IllegalStateException("תשובה ריקה מהשרת")
            val dao = database.gtfsDao()

            dao.clearStops()
            dao.clearStopTimes()
            dao.clearTrips()
            dao.clearRoutes()
            dao.clearCalendar()

            val stops = ArrayList<StopEntity>(CHUNK_SIZE)
            val routes = ArrayList<RouteEntity>(CHUNK_SIZE)
            val trips = ArrayList<TripEntity>(CHUNK_SIZE)
            val stopTimes = ArrayList<StopTimeEntity>(CHUNK_SIZE)
            val calendar = ArrayList<CalendarEntity>(CHUNK_SIZE)

            suspend fun flush() {
                if (stops.isNotEmpty()) { dao.insertStops(ArrayList(stops)); stops.clear() }
                if (routes.isNotEmpty()) { dao.insertRoutes(ArrayList(routes)); routes.clear() }
                if (trips.isNotEmpty()) { dao.insertTrips(ArrayList(trips)); trips.clear() }
                if (stopTimes.isNotEmpty()) { dao.insertStopTimes(ArrayList(stopTimes)); stopTimes.clear() }
                if (calendar.isNotEmpty()) { dao.insertCalendar(ArrayList(calendar)); calendar.clear() }
            }

            var lineCount = 0L
            var lastReportedPercent = -1

            // לולאה מפורשת ולא forEachLine: הגוף כאן קורא ל-suspend (כתיבה
            // ל-Room, דיווח התקדמות), ולולאה רגילה היא הדרך הברורה לעשות את
            // זה בלי runBlocking בתוך קורוטינה.
            GZIPInputStream(body.byteStream()).bufferedReader().use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    if (line.isBlank()) continue
                    val row = try {
                        NetworkModule.json.parseToJsonElement(line) as? JsonObject
                    } catch (e: Exception) {
                        null
                    } ?: continue

                    when (row.string("t")) {
                        "stop" -> {
                            val id = row.string("id")
                            val lat = row.double("lat")
                            val lon = row.double("lon")
                            if (id != null && lat != null && lon != null) {
                                stops.add(StopEntity(id, row.string("name") ?: "", lat, lon))
                            }
                        }
                        "route" -> {
                            val id = row.string("id")
                            if (id != null) {
                                routes.add(
                                    RouteEntity(
                                        routeId = id,
                                        shortName = row.string("short") ?: "",
                                        longName = row.string("long") ?: "",
                                        type = row.int("type") ?: 3,
                                    )
                                )
                            }
                        }
                        "trip" -> {
                            val id = row.string("id")
                            val routeId = row.string("route")
                            if (id != null && routeId != null) {
                                trips.add(
                                    TripEntity(
                                        tripId = id,
                                        routeId = routeId,
                                        serviceId = row.string("service") ?: "",
                                        headsign = row.string("headsign") ?: "",
                                    )
                                )
                            }
                        }
                        "time" -> {
                            val tripId = row.string("trip")
                            val stopId = row.string("stop")
                            val arrival = row.int("arr")
                            if (tripId != null && stopId != null && arrival != null) {
                                stopTimes.add(
                                    StopTimeEntity(
                                        tripId = tripId,
                                        stopId = stopId,
                                        arrivalSeconds = arrival,
                                        departureSeconds = row.int("dep") ?: arrival,
                                        stopSequence = row.int("seq") ?: 0,
                                    )
                                )
                            }
                        }
                        "cal" -> {
                            // days הוא שבעה תווי 0/1 מיום שני עד ראשון, באותו
                            // סדר של calendar.txt במפרט GTFS.
                            val serviceId = row.string("service")
                            val days = row.string("days") ?: "0000000"
                            if (serviceId != null) {
                                calendar.add(
                                    CalendarEntity(
                                        serviceId = serviceId,
                                        monday = days.getOrNull(0) == '1',
                                        tuesday = days.getOrNull(1) == '1',
                                        wednesday = days.getOrNull(2) == '1',
                                        thursday = days.getOrNull(3) == '1',
                                        friday = days.getOrNull(4) == '1',
                                        saturday = days.getOrNull(5) == '1',
                                        sunday = days.getOrNull(6) == '1',
                                        startDate = row.int("start") ?: 0,
                                        endDate = row.int("end") ?: 99991231,
                                    )
                                )
                            }
                        }
                    }

                    lineCount++
                    if (lineCount % FLUSH_EVERY_LINES == 0L) {
                        flush()
                        // אין אחוז אמיתי (לא ידוע כמה שורות יש בחבילה) - מתקדמים
                        // בקצב דועך בין 0.1 ל-0.95, כדי שהמד לא ייתקע ולא יגיע
                        // ל-100% לפני הסוף.
                        val fraction = 0.1f + 0.85f * (1f - 1f / (1f + lineCount / 200_000f))
                        val percent = (fraction * 100).toInt()
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            collector.emit(
                                ImportProgress(ImportPhase.PARSING_STOP_TIMES, fraction, message = "מייבא נתוני תחבורה")
                            )
                        }
                    }
                }
            }
            flush()
        }
        collector.emit(ImportProgress(ImportPhase.DONE, 1f))
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }

    private fun JsonObject.double(key: String): Double? = string(key)?.toDoubleOrNull()

    private fun JsonObject.int(key: String): Int? = string(key)?.toIntOrNull()

    companion object {
        private const val TAG = "TransitBundleSource"
        const val COLLECTION_BUNDLES = "transit_bundles"
        private const val CHUNK_SIZE = 800
        private const val FLUSH_EVERY_LINES = 2_000L
    }
}
