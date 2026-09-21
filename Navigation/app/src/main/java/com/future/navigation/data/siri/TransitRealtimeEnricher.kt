package com.future.navigation.data.siri

import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlin.math.abs

/**
 * שכבה שנייה, נפרדת מ-TransitJourneyPlanner: הפלאנר קובע טופולוגיה (אילו
 * קווים/תחנות/החלפות) מנתוני GTFS הסטטיים; השכבה הזו רק שואלת SIRI "כמה
 * מאחר הקו הזה עכשיו" ומעדכנת את הזמנים בהתאם - כדי לא לערבב שני מקורות
 * נתונים שונים (טופולוגיה לעומת זמן אמת) בתוך אלגוריתם חיפוש המסלול עצמו.
 *
 * בכוונה מעדכן רק את קטע הנסיעה (RIDE) הראשון בכל מסלול - זה שהמשתמש עולה
 * עליו עכשיו מהתחנה הקרובה אליו, שבו לתחזית בזמן אמת יש את הערך הכי גבוה
 * ואת הביטחון הכי גבוה. קטעי המשך אחרי החלפה נשארים לפי הלו"ז הסטטי (לא
 * "מדמים" איחור שרשרתי בלי נתון אמיתי על הקו השני) - רק סיכום זמן היציאה/הגעה
 * ברמת המסלול כולו מוזז באותו הפרש, כקירוב סביר.
 */
class TransitRealtimeEnricher(private val siriRepository: SiriRealtimeRepository) {

    suspend fun enrich(itineraries: List<TransitItinerary>): List<TransitItinerary> = coroutineScope {
        itineraries.map { itinerary -> async { enrichOne(itinerary) } }.awaitAll()
    }

    private suspend fun enrichOne(itinerary: TransitItinerary): TransitItinerary {
        val firstRideIndex = itinerary.legs.indexOfFirst { it.type == LegType.RIDE }
        if (firstRideIndex < 0) return itinerary
        val leg = itinerary.legs[firstRideIndex]
        val stopId = leg.fromStopId ?: return itinerary
        val scheduledDeparture = leg.departureSeconds ?: return itinerary

        val live = runCatching { siriRepository.stopMonitoring(stopId, leg.routeShortName) }
            .getOrDefault(emptyList())
            .filter { it.aimedDepartureSeconds != null && it.expectedDepartureSeconds != null }
            .minByOrNull { abs(it.aimedDepartureSeconds!! - scheduledDeparture) }
            ?.takeIf { abs(it.aimedDepartureSeconds!! - scheduledDeparture) <= MATCH_WINDOW_SECONDS }
            ?: return itinerary

        val delay = live.delaySeconds ?: return itinerary
        val updatedLeg = leg.copy(
            departureSeconds = scheduledDeparture + delay,
            arrivalSeconds = leg.arrivalSeconds?.plus(delay),
            isRealtime = true,
            delaySeconds = delay
        )
        val updatedLegs = itinerary.legs.toMutableList().also { it[firstRideIndex] = updatedLeg }

        return itinerary.copy(
            legs = updatedLegs,
            departureSeconds = itinerary.departureSeconds + delay,
            arrivalSeconds = itinerary.arrivalSeconds + delay
        )
    }

    companion object {
        /** חלון התאמה בין הנסיעה שהפלאנר בחר לבין ה-MonitoredStopVisit הקרוב אליה - נסיעות אחרות של אותו קו באותה תחנה לא יתאימו בטעות. */
        private const val MATCH_WINDOW_SECONDS = 15 * 60
    }
}
