package com.future.navigation.data.scan

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.gtfs.DepartureRow
import com.future.navigation.data.gtfs.GtfsDao
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.StopEntity
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import com.future.navigation.data.gtfs.isServiceActive
import com.future.navigation.data.payment.FareCalculator
import com.future.navigation.data.payment.RideFare
import com.future.navigation.data.siri.SiriConfig
import com.future.navigation.data.siri.SiriRealtimeRepository
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.abs

/** נסיעה שאפשר לעלות עליה עכשיו: קו, נסיעה (trip) ותחנת העלייה הקרובה. */
data class BoardingOption(
    val stop: StopEntity,
    val distanceMeters: Double,
    val departure: DepartureRow,
) {
    val lineName: String get() = departure.routeShortName.ifBlank { departure.routeLongName }
}

/** תחנת ירידה אפשרית בהמשך הנסיעה, עם המחיר המדויק עד אליה. */
data class AlightingOption(
    val stop: StopEntity,
    val arrivalSeconds: Int,
    val fare: RideFare,
)

/**
 * מאחורי מסך סריקת הברקוד: מה אפשר לעלות עליו כאן ועכשיו (GTFS), איזה
 * מהם הוא האוטובוס שנסרק (SIRI), ולאן אפשר לנסוע בו ובכמה.
 */
class BusScanRepository(
    private val dao: GtfsDao,
    private val siri: SiriRealtimeRepository,
) {

    /**
     * יציאות מהתחנות הקרובות, מרבע שעה אחורה (האוטובוס כבר בתחנה או קצת
     * מאחר) עד 45 דקות קדימה. קו אחד לכל תחנה - היציאה הקרובה ביותר לעכשיו.
     */
    suspend fun boardingOptions(location: LatLng, nowEpochSeconds: Long): List<BoardingOption> {
        val now = Instant.ofEpochSecond(nowEpochSeconds).atZone(ZONE)
        val secondsNow = now.hour * 3600 + now.minute * 60 + now.second
        val dateInt = now.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()

        val latDelta = BOARDING_RADIUS_METERS / 111_320.0
        val lonDelta = BOARDING_RADIUS_METERS / (111_320.0 * Math.cos(Math.toRadians(location.lat)).coerceAtLeast(0.1))
        val stops = dao.stopsInBoundingBox(
            minLat = location.lat - latDelta, maxLat = location.lat + latDelta,
            minLon = location.lon - lonDelta, maxLon = location.lon + lonDelta
        )
            .map { it to location.distanceMetersTo(LatLng(it.lat, it.lon)) }
            .filter { it.second <= BOARDING_RADIUS_METERS }
            .sortedBy { it.second }
            .take(MAX_STOPS)

        val options = mutableListOf<BoardingOption>()
        for ((stop, distance) in stops) {
            dao.departuresFromStop(stop.stopId, secondsNow - LOOKBACK_SECONDS, limit = DEPARTURES_PER_STOP)
                .filter { it.departureSeconds <= secondsNow + LOOKAHEAD_SECONDS }
                .filter { dao.isServiceActive(it.serviceId, dateInt, now.dayOfWeek) }
                .groupBy { it.routeId }
                .values
                .map { sameLine -> sameLine.minBy { abs(it.departureSeconds - secondsNow) } }
                .forEach { options += BoardingOption(stop, distance, it) }
        }
        return options.sortedWith(compareBy({ it.distanceMeters }, { abs(it.departure.departureSeconds - secondsNow) })).take(MAX_OPTIONS)
    }

    /**
     * הקו של האוטובוס שנסרק: SIRI מחזיר לכל אוטובוס שמגיע לתחנה את
     * VehicleRef (מספר הרכב) ואת LineRef (route_id של GTFS). אם הרכב
     * מהברקוד מופיע באחת התחנות הקרובות - זה הקו. null אם אין מספר רכב,
     * אין SIRI, או שהרכב לא נמצא - ואז המשתמש בוחר מהרשימה.
     */
    suspend fun identify(qr: BusQr, options: List<BoardingOption>): BoardingOption? {
        val vehicle = qr.vehicleNumber ?: return null
        if (!SiriConfig.isConfigured) return null
        for (stop in options.map { it.stop }.distinctBy { it.stopId }.take(SIRI_STOPS)) {
            val arrivals = runCatching { siri.stopMonitoring(stop.stopId) }.getOrDefault(emptyList())
            val lineRef = arrivals.firstOrNull { sameVehicle(it.vehicleRef, vehicle) }?.lineRef ?: continue
            options.firstOrNull { it.stop.stopId == stop.stopId && it.departure.routeId == lineRef }?.let { return it }
            options.firstOrNull { it.departure.routeId == lineRef }?.let { return it }
        }
        return null
    }

    /** כל התחנות שאחרי תחנת העלייה בנסיעה הזו, כל אחת עם המחיר עד אליה. */
    suspend fun alightingOptions(option: BoardingOption): List<AlightingOption> {
        val from = LatLng(option.stop.lat, option.stop.lon)
        return dao.stopTimesForTrip(option.departure.tripId)
            .filter { it.stopSequence > option.departure.stopSequence }
            .mapNotNull { row ->
                val stop = dao.stopById(row.stopId) ?: return@mapNotNull null
                val leg = TransitLeg(
                    type = LegType.RIDE,
                    routeShortName = option.departure.routeShortName,
                    routeLongName = option.departure.routeLongName,
                    fromStopName = option.stop.name,
                    toStopName = stop.name,
                    departureSeconds = option.departure.departureSeconds,
                    arrivalSeconds = row.arrivalSeconds,
                    fromStopId = option.stop.stopId,
                    fromStopLocation = from,
                    toStopLocation = LatLng(stop.lat, stop.lon),
                    routeType = option.departure.routeType,
                )
                val fare = FareCalculator.calculate(TransitItinerary(listOf(leg), option.departure.departureSeconds, row.arrivalSeconds))
                AlightingOption(stop, row.arrivalSeconds, fare.rides.single())
            }
    }

    /** "7801501" מול "78-015-01" או "07801501" - רק הספרות, בלי אפסים מובילים. */
    private fun sameVehicle(siriRef: String?, scanned: String): Boolean {
        val a = siriRef?.filter { it.isDigit() }?.trimStart('0') ?: return false
        return a.isNotEmpty() && a == scanned.filter { it.isDigit() }.trimStart('0')
    }

    private companion object {
        val ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")
        const val BOARDING_RADIUS_METERS = 400.0
        const val MAX_STOPS = 6
        const val DEPARTURES_PER_STOP = 40
        const val LOOKBACK_SECONDS = 15 * 60
        const val LOOKAHEAD_SECONDS = 45 * 60
        const val MAX_OPTIONS = 30
        const val SIRI_STOPS = 3
    }
}
