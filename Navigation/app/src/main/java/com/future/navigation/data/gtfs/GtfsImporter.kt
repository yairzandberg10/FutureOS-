package com.future.navigation.data.gtfs

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

private fun ZipFile.requiredEntry(name: String): ZipEntry =
    getEntry(name) ?: throw IllegalStateException("קובץ $name חסר בתוך ה-GTFS שהתקבל")

enum class ImportPhase { DOWNLOADING, PARSING_STOPS, PARSING_STOP_TIMES, PARSING_TRIPS, PARSING_ROUTES, PARSING_CALENDAR, DONE, ERROR }

data class ImportProgress(
    val phase: ImportPhase,
    val fraction: Float,
    val message: String = "",
    val error: String? = null
)

/**
 * מוריד את קובץ ה-GTFS הארצי האמיתי (~130MB, ר' GtfsConfig.FEED_URL), ומייבא
 * אליו אך ורק את הנתונים שנופלים בתוך התיבה הגיאוגרפית שהוגדרה - כדי
 * שההורדה/הפירוק/האחסון יישארו סבירים על מכשיר בודד, במקום לייבא את כל הארץ.
 *
 * שרשרת הסינון: stops (לפי bbox) -> stop_times (רק תחנות שנשמרו) -> trips
 * (רק נסיעות שנשארו אחרי הסינון של stop_times) -> routes/calendar (רק
 * מזהים שנסיעות שנשמרו בפועל מפנות אליהם).
 */
class GtfsImporter(private val database: GtfsDatabase) {
    private val client = com.future.navigation.data.network.NetworkModule.okHttpClient

    fun importFeed(context: Context, bbox: BoundingBox = GtfsConfig.TEL_AVIV_BBOX): Flow<ImportProgress> = flow {
        val dao = database.gtfsDao()
        val zipFile: File

        try {
            emit(ImportProgress(ImportPhase.DOWNLOADING, 0f))
            zipFile = downloadFeed(context) { fraction ->
                emit(ImportProgress(ImportPhase.DOWNLOADING, fraction))
            }
        } catch (e: Exception) {
            emit(ImportProgress(ImportPhase.ERROR, 0f, error = e.message ?: "שגיאת הורדה"))
            return@flow
        }

        try {
            ZipFile(zipFile).use { zip ->
                // --- שלב 1: תחנות בתוך התיבה הגיאוגרפית ---
                emit(ImportProgress(ImportPhase.PARSING_STOPS, 0.05f))
                val keptStopIds = HashSet<String>()
                val stopsBuffer = ArrayList<StopEntity>(CHUNK_SIZE)
                dao.clearStops()
                zip.getInputStream(zip.requiredEntry("stops.txt")).use { input ->
                    val reader = GtfsCsvReader(input)
                    reader.forEachRow { row ->
                        val lat = row["stop_lat"]?.toDoubleOrNull()
                        val lon = row["stop_lon"]?.toDoubleOrNull()
                        val stopId = row["stop_id"]
                        if (lat != null && lon != null && stopId != null &&
                            lat in bbox.minLat..bbox.maxLat && lon in bbox.minLon..bbox.maxLon
                        ) {
                            keptStopIds.add(stopId)
                            stopsBuffer.add(StopEntity(stopId, row["stop_name"] ?: "", lat, lon))
                            if (stopsBuffer.size >= CHUNK_SIZE) {
                                dao.insertStops(ArrayList(stopsBuffer))
                                stopsBuffer.clear()
                            }
                        }
                    }
                    reader.close()
                }
                if (stopsBuffer.isNotEmpty()) dao.insertStops(stopsBuffer)

                // --- שלב 2: זמני עצירה - רק לתחנות שנשמרו, אוספים בדרך את הנסיעות הרלוונטיות ---
                emit(ImportProgress(ImportPhase.PARSING_STOP_TIMES, 0.20f))
                val keptTripIds = HashSet<String>()
                val stopTimesBuffer = ArrayList<StopTimeEntity>(CHUNK_SIZE)
                dao.clearStopTimes()
                zip.getInputStream(zip.requiredEntry("stop_times.txt")).use { input ->
                    val reader = GtfsCsvReader(input)
                    reader.forEachRow { row ->
                        val stopId = row["stop_id"]
                        if (stopId != null && stopId in keptStopIds) {
                            val tripId = row["trip_id"] ?: return@forEachRow
                            val arrival = parseGtfsTimeToSeconds(row["arrival_time"] ?: "") ?: return@forEachRow
                            val departure = parseGtfsTimeToSeconds(row["departure_time"] ?: "") ?: arrival
                            val seq = row["stop_sequence"]?.toIntOrNull() ?: 0
                            keptTripIds.add(tripId)
                            stopTimesBuffer.add(StopTimeEntity(tripId = tripId, stopId = stopId, arrivalSeconds = arrival, departureSeconds = departure, stopSequence = seq))
                            if (stopTimesBuffer.size >= CHUNK_SIZE) {
                                dao.insertStopTimes(ArrayList(stopTimesBuffer))
                                stopTimesBuffer.clear()
                            }
                        }
                    }
                    reader.close()
                }
                if (stopTimesBuffer.isNotEmpty()) dao.insertStopTimes(stopTimesBuffer)

                // --- שלב 3: נסיעות - רק אלה שבאמת עוצרות באחת התחנות שנשמרו ---
                emit(ImportProgress(ImportPhase.PARSING_TRIPS, 0.55f))
                val keptRouteIds = HashSet<String>()
                val keptServiceIds = HashSet<String>()
                val tripsBuffer = ArrayList<TripEntity>(CHUNK_SIZE)
                dao.clearTrips()
                zip.getInputStream(zip.requiredEntry("trips.txt")).use { input ->
                    val reader = GtfsCsvReader(input)
                    reader.forEachRow { row ->
                        val tripId = row["trip_id"]
                        if (tripId != null && tripId in keptTripIds) {
                            val routeId = row["route_id"] ?: return@forEachRow
                            val serviceId = row["service_id"] ?: return@forEachRow
                            keptRouteIds.add(routeId)
                            keptServiceIds.add(serviceId)
                            tripsBuffer.add(TripEntity(tripId, routeId, serviceId, row["trip_headsign"] ?: ""))
                            if (tripsBuffer.size >= CHUNK_SIZE) {
                                dao.insertTrips(ArrayList(tripsBuffer))
                                tripsBuffer.clear()
                            }
                        }
                    }
                    reader.close()
                }
                if (tripsBuffer.isNotEmpty()) dao.insertTrips(tripsBuffer)

                // --- שלב 4: קווים - רק אלה שנסיעה שנשמרה שייכת אליהם ---
                emit(ImportProgress(ImportPhase.PARSING_ROUTES, 0.75f))
                val routesBuffer = ArrayList<RouteEntity>(CHUNK_SIZE)
                dao.clearRoutes()
                zip.getInputStream(zip.requiredEntry("routes.txt")).use { input ->
                    val reader = GtfsCsvReader(input)
                    reader.forEachRow { row ->
                        val routeId = row["route_id"]
                        if (routeId != null && routeId in keptRouteIds) {
                            routesBuffer.add(
                                RouteEntity(
                                    routeId = routeId,
                                    shortName = row["route_short_name"] ?: "",
                                    longName = row["route_long_name"] ?: "",
                                    type = row["route_type"]?.toIntOrNull() ?: 3
                                )
                            )
                        }
                    }
                    reader.close()
                }
                if (routesBuffer.isNotEmpty()) dao.insertRoutes(routesBuffer)

                // --- שלב 5: לוח שנה - רק שירותים שנסיעה שנשמרה משתמשת בהם ---
                emit(ImportProgress(ImportPhase.PARSING_CALENDAR, 0.9f))
                val calendarBuffer = ArrayList<CalendarEntity>(CHUNK_SIZE)
                dao.clearCalendar()
                val calendarEntry = zip.getEntry("calendar.txt")
                if (calendarEntry != null) {
                    zip.getInputStream(calendarEntry).use { input ->
                        val reader = GtfsCsvReader(input)
                        reader.forEachRow { row ->
                            val serviceId = row["service_id"]
                            if (serviceId != null && serviceId in keptServiceIds) {
                                calendarBuffer.add(
                                    CalendarEntity(
                                        serviceId = serviceId,
                                        monday = row["monday"] == "1",
                                        tuesday = row["tuesday"] == "1",
                                        wednesday = row["wednesday"] == "1",
                                        thursday = row["thursday"] == "1",
                                        friday = row["friday"] == "1",
                                        saturday = row["saturday"] == "1",
                                        sunday = row["sunday"] == "1",
                                        startDate = row["start_date"]?.toIntOrNull() ?: 0,
                                        endDate = row["end_date"]?.toIntOrNull() ?: 99991231
                                    )
                                )
                            }
                        }
                        reader.close()
                    }
                }
                if (calendarBuffer.isNotEmpty()) dao.insertCalendar(calendarBuffer)
            }

            zipFile.delete()
            emit(ImportProgress(ImportPhase.DONE, 1f))
        } catch (e: Exception) {
            emit(ImportProgress(ImportPhase.ERROR, 0f, error = e.message ?: "שגיאת ייבוא"))
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun downloadFeed(context: Context, onProgress: suspend (Float) -> Unit): File {
        val request = Request.Builder().url(GtfsConfig.FEED_URL).build()
        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            response.close()
            throw IllegalStateException("הורדת קובץ ה-GTFS נכשלה: ${response.code}")
        }
        val body = response.body ?: run {
            response.close()
            throw IllegalStateException("תשובה ריקה מהשרת")
        }

        val totalBytes = body.contentLength()
        val outFile = File(context.cacheDir, "israel-public-transportation.zip")
        var lastReportedPercent = -1
        outFile.outputStream().use { output ->
            body.byteStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                var bytesRead = 0L
                var read = input.read(buffer)
                while (read >= 0) {
                    output.write(buffer, 0, read)
                    bytesRead += read
                    if (totalBytes > 0) {
                        val fraction = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                        val percent = (fraction * 100).toInt()
                        // מדווח לכל אחוז שלם, לא לכל chunk - כדי לא להציף את ה-Flow.
                        if (percent != lastReportedPercent) {
                            lastReportedPercent = percent
                            onProgress(fraction)
                        }
                    }
                    read = input.read(buffer)
                }
            }
        }
        response.close()
        return outFile
    }

    companion object {
        private const val CHUNK_SIZE = 800
    }
}
