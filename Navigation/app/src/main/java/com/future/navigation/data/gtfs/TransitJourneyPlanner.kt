package com.future.navigation.data.gtfs

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

enum class LegType { WALK, RIDE }

data class TransitLeg(
    val type: LegType,
    val routeShortName: String? = null,
    val routeLongName: String? = null,
    val fromStopName: String,
    val toStopName: String,
    val departureSeconds: Int? = null,
    val arrivalSeconds: Int? = null,
    val intermediateStopNames: List<String> = emptyList(),
    /** רלוונטי רק לרגלי הליכה (WALK) - למשל "4 דק' · 300 מ'". */
    val walkDistanceMeters: Double? = null,
    val walkDurationSeconds: Int? = null
)

data class TransitItinerary(
    val legs: List<TransitLeg>,
    val departureSeconds: Int,
    val arrivalSeconds: Int
) {
    val totalDurationSeconds: Int get() = arrivalSeconds - departureSeconds
}

/**
 * חיפוש מסלול תחבורה ציבורית פשוט: מסלול ישיר, ואם לא נמצא - עם החלפה אחת.
 * לא מנוע RAPTOR מלא (זה לא מבטיח את המסלול המהיר ביותר בכל מקרה), אבל
 * מחשב מסלולים אמיתיים על בסיס נתוני GTFS אמיתיים שיובאו בפועל - ר' התיעוד
 * ב-GtfsImporter/GtfsConfig על היקף הסינון הגיאוגרפי.
 */
class TransitJourneyPlanner(private val dao: GtfsDao) {

    suspend fun findJourneys(from: LatLng, to: LatLng, atEpochSeconds: Long): List<TransitItinerary> {
        val zone = ZoneId.of("Asia/Jerusalem")
        val instant = Instant.ofEpochSecond(atEpochSeconds)
        val zoned = instant.atZone(zone)
        val secondsSinceMidnight = zoned.hour * 3600 + zoned.minute * 60 + zoned.second
        val dateInt = zoned.format(DateTimeFormatter.ofPattern("yyyyMMdd")).toInt()
        val dayOfWeek = zoned.dayOfWeek

        val originStops = nearbyWalkableStops(from)
        val destStops = nearbyWalkableStops(to)
        if (originStops.isEmpty() || destStops.isEmpty()) return emptyList()

        val destStopIds = destStops.map { it.stop.stopId }.toSet()
        val results = mutableListOf<TransitItinerary>()

        for (origin in originStops) {
            val departures = dao.departuresFromStop(origin.stop.stopId, secondsSinceMidnight, limit = MAX_DIRECT_CANDIDATES)
                .filter { isServiceActive(it.serviceId, dateInt, dayOfWeek) }

            for (dep in departures) {
                val tripStops = dao.stopTimesForTrip(dep.tripId)
                val originIndex = tripStops.indexOfFirst { it.stopSequence == dep.stopSequence }
                if (originIndex < 0) continue

                // מסלול ישיר: אותה נסיעה ממשיכה לאחת מתחנות היעד
                val directHit = tripStops.drop(originIndex + 1).firstOrNull { it.stopId in destStopIds }
                if (directHit != null) {
                    val destStop = destStops.first { it.stop.stopId == directHit.stopId }
                    results += buildDirectItinerary(origin, destStop, dep, directHit, tripStops, originIndex)
                    continue
                }

                // החלפה אחת: מהתחנות שהנסיעה עוברת בהן אחרי המוצא, מחפשים נסיעה אחרת שמגיעה ליעד
                val transferCandidates = tripStops.drop(originIndex + 1).take(MAX_TRANSFER_STOPS_PER_TRIP)
                for (transfer in transferCandidates) {
                    val transferBuffer = 120 // שניות מינימום למעבר בין נסיעות באותה תחנה
                    val onwardDepartures = dao.departuresFromStop(
                        transfer.stopId,
                        transfer.arrivalSeconds + transferBuffer,
                        limit = MAX_TRANSFER_CANDIDATES
                    ).filter { it.tripId != dep.tripId && isServiceActive(it.serviceId, dateInt, dayOfWeek) }

                    for (onward in onwardDepartures) {
                        val onwardTripStops = dao.stopTimesForTrip(onward.tripId)
                        val onwardOriginIndex = onwardTripStops.indexOfFirst { it.stopSequence == onward.stopSequence }
                        if (onwardOriginIndex < 0) continue
                        val onwardHit = onwardTripStops.drop(onwardOriginIndex + 1).firstOrNull { it.stopId in destStopIds }
                        if (onwardHit != null) {
                            val destStop = destStops.first { it.stop.stopId == onwardHit.stopId }
                            val transferStopName = dao.stopById(transfer.stopId)?.name ?: transfer.stopId
                            results += buildTransferItinerary(
                                origin, destStop, transferStopName,
                                firstLegDep = dep, firstLegTripStops = tripStops, firstLegOriginIndex = originIndex, transferStop = transfer,
                                secondLegDep = onward, secondLegTripStops = onwardTripStops, secondLegOriginIndex = onwardOriginIndex, secondLegArrival = onwardHit
                            )
                            break
                        }
                    }
                    if (results.size >= MAX_RESULTS) break
                }
                if (results.size >= MAX_RESULTS) break
            }
            if (results.size >= MAX_RESULTS) break
        }

        return results.sortedBy { it.arrivalSeconds }.take(MAX_RESULTS)
    }

    private suspend fun isServiceActive(serviceId: String, dateInt: Int, dayOfWeek: java.time.DayOfWeek): Boolean {
        val calendar = dao.calendarForService(serviceId) ?: return false
        if (dateInt < calendar.startDate || dateInt > calendar.endDate) return false
        return when (dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> calendar.monday
            java.time.DayOfWeek.TUESDAY -> calendar.tuesday
            java.time.DayOfWeek.WEDNESDAY -> calendar.wednesday
            java.time.DayOfWeek.THURSDAY -> calendar.thursday
            java.time.DayOfWeek.FRIDAY -> calendar.friday
            java.time.DayOfWeek.SATURDAY -> calendar.saturday
            java.time.DayOfWeek.SUNDAY -> calendar.sunday
        }
    }

    private data class NearbyStop(val stop: StopEntity, val distanceMeters: Double)

    private suspend fun nearbyWalkableStops(point: LatLng): List<NearbyStop> {
        val latDelta = WALK_RADIUS_METERS / 111_320.0
        val lonDelta = WALK_RADIUS_METERS / (111_320.0 * Math.cos(Math.toRadians(point.lat)).coerceAtLeast(0.1))
        val candidates = dao.stopsInBoundingBox(
            minLat = point.lat - latDelta, maxLat = point.lat + latDelta,
            minLon = point.lon - lonDelta, maxLon = point.lon + lonDelta
        )
        return candidates
            .map { NearbyStop(it, point.distanceMetersTo(LatLng(it.lat, it.lon))) }
            .filter { it.distanceMeters <= WALK_RADIUS_METERS }
            .sortedBy { it.distanceMeters }
            .take(MAX_NEARBY_STOPS)
    }

    private suspend fun resolveStopNames(stopIds: List<String>): List<String> =
        stopIds.map { dao.stopById(it)?.name ?: it }

    private suspend fun buildDirectItinerary(
        origin: NearbyStop,
        dest: NearbyStop,
        dep: DepartureRow,
        arrival: DepartureRow,
        tripStops: List<DepartureRow>,
        originIndex: Int
    ): TransitItinerary {
        val arrivalIndex = tripStops.indexOfFirst { it.stopId == arrival.stopId && it.stopSequence == arrival.stopSequence }
        val intermediate = resolveStopNames(
            tripStops.subList(originIndex + 1, arrivalIndex.coerceAtLeast(originIndex + 1)).map { it.stopId }
        )

        val legs = mutableListOf<TransitLeg>()
        legs += walkLeg("המיקום שלך", origin.stop.name, origin.distanceMeters)
        legs += TransitLeg(
            type = LegType.RIDE,
            routeShortName = dep.routeShortName,
            routeLongName = dep.routeLongName,
            fromStopName = origin.stop.name,
            toStopName = dest.stop.name,
            departureSeconds = dep.departureSeconds,
            arrivalSeconds = arrival.arrivalSeconds,
            intermediateStopNames = intermediate
        )
        legs += walkLeg(dest.stop.name, "היעד שלך", dest.distanceMeters)

        return TransitItinerary(
            legs = legs,
            departureSeconds = dep.departureSeconds - walkDurationSeconds(origin.distanceMeters),
            arrivalSeconds = arrival.arrivalSeconds + walkDurationSeconds(dest.distanceMeters)
        )
    }

    private suspend fun buildTransferItinerary(
        origin: NearbyStop,
        dest: NearbyStop,
        transferStopEntityName: String,
        firstLegDep: DepartureRow,
        firstLegTripStops: List<DepartureRow>,
        firstLegOriginIndex: Int,
        transferStop: DepartureRow,
        secondLegDep: DepartureRow,
        secondLegTripStops: List<DepartureRow>,
        secondLegOriginIndex: Int,
        secondLegArrival: DepartureRow
    ): TransitItinerary {
        val firstArrivalIndex = firstLegTripStops.indexOfFirst { it.stopId == transferStop.stopId && it.stopSequence == transferStop.stopSequence }
        val firstIntermediate = resolveStopNames(
            firstLegTripStops.subList(firstLegOriginIndex + 1, firstArrivalIndex.coerceAtLeast(firstLegOriginIndex + 1)).map { it.stopId }
        )

        val secondArrivalIndex = secondLegTripStops.indexOfFirst { it.stopId == secondLegArrival.stopId && it.stopSequence == secondLegArrival.stopSequence }
        val secondIntermediate = resolveStopNames(
            secondLegTripStops.subList(secondLegOriginIndex + 1, secondArrivalIndex.coerceAtLeast(secondLegOriginIndex + 1)).map { it.stopId }
        )

        val legs = mutableListOf<TransitLeg>()
        legs += walkLeg("המיקום שלך", origin.stop.name, origin.distanceMeters)
        legs += TransitLeg(
            type = LegType.RIDE,
            routeShortName = firstLegDep.routeShortName,
            routeLongName = firstLegDep.routeLongName,
            fromStopName = origin.stop.name,
            toStopName = transferStopEntityName,
            departureSeconds = firstLegDep.departureSeconds,
            arrivalSeconds = transferStop.arrivalSeconds,
            intermediateStopNames = firstIntermediate
        )
        legs += TransitLeg(
            type = LegType.RIDE,
            routeShortName = secondLegDep.routeShortName,
            routeLongName = secondLegDep.routeLongName,
            fromStopName = transferStopEntityName,
            toStopName = dest.stop.name,
            departureSeconds = secondLegDep.departureSeconds,
            arrivalSeconds = secondLegArrival.arrivalSeconds,
            intermediateStopNames = secondIntermediate
        )
        legs += walkLeg(dest.stop.name, "היעד שלך", dest.distanceMeters)

        return TransitItinerary(
            legs = legs,
            departureSeconds = firstLegDep.departureSeconds - walkDurationSeconds(origin.distanceMeters),
            arrivalSeconds = secondLegArrival.arrivalSeconds + walkDurationSeconds(dest.distanceMeters)
        )
    }

    private fun walkLeg(from: String, to: String, distanceMeters: Double) = TransitLeg(
        type = LegType.WALK,
        fromStopName = from,
        toStopName = to,
        walkDistanceMeters = distanceMeters,
        walkDurationSeconds = walkDurationSeconds(distanceMeters)
    )

    private fun walkDurationSeconds(distanceMeters: Double): Int {
        val walkSpeedMetersPerSecond = 1.3
        return (distanceMeters / walkSpeedMetersPerSecond).toInt()
    }

    companion object {
        private const val WALK_RADIUS_METERS = 800.0
        private const val MAX_NEARBY_STOPS = 5
        private const val MAX_DIRECT_CANDIDATES = 20
        private const val MAX_TRANSFER_STOPS_PER_TRIP = 8
        private const val MAX_TRANSFER_CANDIDATES = 8
        private const val MAX_RESULTS = 5
    }
}
