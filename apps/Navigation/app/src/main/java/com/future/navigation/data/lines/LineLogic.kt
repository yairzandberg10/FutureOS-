package com.future.navigation.data.lines

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import java.time.Instant
import java.time.ZoneId

/**
 * לוגיקה טהורה של קווים (בלי רשת ובלי Android): חיפוש לפי מספר, כיוון נגדי, לוח
 * יומי ומציאת נסיעה. כל הפונקציות מקבלות את "עכשיו" כפרמטר, כדי שייבדקו ביחידה.
 */
object LineLogic {
    private val ISRAEL: ZoneId = ZoneId.of("Asia/Jerusalem")

    /** יציאה שהתחילה לפני פחות מדקה עדיין נחשבת "עכשיו". */
    private const val GRACE_MS = 60_000L

    /** כיוון נסיעה כפי שמוצג: קו תחתון הופך לרווח, בלי רווחים בקצוות. */
    fun headsignKey(raw: String?): String = raw.orEmpty().replace('_', ' ').trim()

    fun sameDirection(a: String?, b: String?): Boolean = headsignKey(a) == headsignKey(b)

    /**
     * תוצאות חיפוש קו לפי תחילת המספר ("5" מתאים גם ל-"50"). לכל (קו, כיוון) שורה
     * אחת - מהתחנה הקרובה ביותר ל-[at]. הקו המדויק קודם, ואחריו לפי מספר.
     */
    fun distinctLines(departures: List<LineDeparture>, query: String, at: LatLng?, nowMillis: Long): List<LineDeparture> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val origin = at ?: LatLng(0.0, 0.0)
        val nearest = compareBy<LineDeparture>({ it.location.distanceMetersTo(origin) }, { it.departsAtMillis })
        return departures
            .filter { it.tripId != null && it.departsAtMillis >= nowMillis - GRACE_MS && it.line.startsWith(q, ignoreCase = true) }
            .groupBy { it.line to headsignKey(it.headsign) }
            .values
            .mapNotNull { group -> group.minWithOrNull(nearest) }
            .sortedWith(
                compareBy<LineDeparture>(
                    { if (it.line.equals(q, ignoreCase = true)) 0 else 1 },
                    { it.line.toIntOrNull() ?: Int.MAX_VALUE },
                    { it.line },
                    { headsignKey(it.headsign) },
                ),
            )
    }

    /** היציאות הבאות של קו בכיוון מסוים מתוך יציאות של תחנה אחת, לפי הזמן. */
    fun nextForLine(stopDepartures: List<LineDeparture>, line: String, headsign: String, nowMillis: Long, count: Int): List<LineDeparture> =
        stopDepartures
            .filter { it.line == line && sameDirection(it.headsign, headsign) && it.departsAtMillis >= nowMillis }
            .sortedBy { it.departsAtMillis }
            .take(count)

    /** כל יציאות הקו בכיוון מסוים ביום שבין [dayStart] ל-[dayEnd], לפי הזמן. */
    fun dailyFor(stopDepartures: List<LineDeparture>, line: String, headsign: String, dayStart: Long, dayEnd: Long): List<LineDeparture> =
        stopDepartures
            .filter { it.line == line && sameDirection(it.headsign, headsign) && it.departsAtMillis in dayStart until dayEnd }
            .sortedBy { it.departsAtMillis }

    /**
     * נסיעה של אותו קו בכיוון ההפוך (אחר headsign) מתוך יציאות קרובות. הקרובה ביותר
     * ל-[at] (התחנה שלי) נבחרת, ואחריה הקרובה בזמן.
     */
    fun oppositeDirection(nearby: List<LineDeparture>, line: String, headsign: String, at: LatLng, nowMillis: Long): LineDeparture? =
        nearby
            .filter {
                it.line == line &&
                    headsignKey(it.headsign).isNotEmpty() &&
                    !sameDirection(it.headsign, headsign) &&
                    it.tripId != null &&
                    it.departsAtMillis >= nowMillis
            }
            .minWithOrNull(compareBy<LineDeparture>({ it.location.distanceMetersTo(at) }, { it.departsAtMillis }))

    /** מזהה הנסיעה של היציאה הקרובה של קו בכיוון מסוים (נסיעה חיה, לא של אתמול). */
    fun firstTrip(candidates: List<LineDeparture>, line: String, headsign: String, nowMillis: Long): String? =
        candidates
            .filter { it.line == line && sameDirection(it.headsign, headsign) && it.tripId != null && it.departsAtMillis >= nowMillis }
            .minByOrNull { it.departsAtMillis }
            ?.tripId

    /** תחילת היום ותחילת המחרת לפי שעון ישראל, במילישניות. */
    fun dayBounds(nowMillis: Long): Pair<Long, Long> {
        val date = Instant.ofEpochMilli(nowMillis).atZone(ISRAEL).toLocalDate()
        val start = date.atStartOfDay(ISRAEL).toInstant().toEpochMilli()
        val end = date.plusDays(1).atStartOfDay(ISRAEL).toInstant().toEpochMilli()
        return start to end
    }

    /** שעה בשעון ישראל בפורמט HH:mm. */
    fun clock(millis: Long): String {
        val time = Instant.ofEpochMilli(millis).atZone(ISRAEL).toLocalTime()
        return "%02d:%02d".format(time.hour, time.minute)
    }
}
