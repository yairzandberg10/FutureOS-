package com.future.navigation.data.lines

import com.future.navigation.data.common.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** לוגיקה טהורה של קווים: חיפוש, כיוון נגדי, לוח יומי ומציאת נסיעה. */
class LineLogicTest {

    private val here = LatLng(32.08, 34.78)
    private val far = LatLng(32.20, 34.90)
    private val now = Instant.parse("2026-11-19T18:00:00Z").toEpochMilli()

    private fun dep(
        line: String,
        headsign: String = "Tel Aviv",
        minutes: Int = 10,
        at: LatLng = here,
        stopName: String = "תחנה",
        tripId: String? = "trip-$line-$headsign",
    ) = LineDeparture(
        line = line,
        headsign = headsign,
        stopName = stopName,
        stopId = "stop-$stopName",
        location = at,
        departsAtMillis = now + minutes * 60_000L,
        tripId = tripId,
        agency = null,
        realtime = false,
    )

    @Test
    fun headsignUnderscoresBecomeSpaces() {
        assertEquals("Tel Aviv Center", LineLogic.headsignKey(" Tel_Aviv Center "))
        assertEquals("", LineLogic.headsignKey(null))
        assertTrue(LineLogic.sameDirection("A_B", "A B"))
    }

    @Test
    fun searchKeepsOnePerDirectionAtNearestStopAndExactLineFirst() {
        val all = listOf(
            dep("50", "Bat Yam", minutes = 3),
            dep("5", "Tel Aviv", minutes = 9, at = far, stopName = "רחוק"),
            dep("5", "Tel Aviv", minutes = 4, at = here, stopName = "קרוב"),
            dep("5", "Holon", minutes = 6),
            dep("6", "Tel Aviv", minutes = 2),
        )
        val hits = LineLogic.distinctLines(all, "5", here, now)

        // "5" בשני כיוונים (כל כיוון פעם אחת, מהתחנה הקרובה), ואחריו "50". "6" לא מתאים.
        assertEquals(listOf("5", "5", "50"), hits.map { it.line })
        val tlv = hits.first { it.headsign == "Tel Aviv" }
        assertEquals("קרוב", tlv.stopName)
    }

    @Test
    fun searchIgnoresRowsWithoutTripAndPastRows() {
        val all = listOf(
            dep("7", tripId = null),
            LineDeparture(
                line = "7",
                headsign = "X",
                stopName = "ישן",
                stopId = null,
                location = here,
                departsAtMillis = now - 60 * 60_000L,
                tripId = "old",
                agency = null,
                realtime = false,
            ),
        )
        assertTrue(LineLogic.distinctLines(all, "7", here, now).isEmpty())
    }

    @Test
    fun blankQueryHasNoHits() {
        assertTrue(LineLogic.distinctLines(listOf(dep("5")), "  ", here, now).isEmpty())
    }

    @Test
    fun nextForLineFiltersDirectionSortsAndTakesCount() {
        val stop = listOf(
            dep("5", "Tel Aviv", minutes = 30),
            dep("5", "Tel Aviv", minutes = 5),
            dep("5", "Holon", minutes = 1),
            dep("5", "Tel Aviv", minutes = -20),
            dep("9", "Tel Aviv", minutes = 2),
            dep("5", "Tel Aviv", minutes = 50),
            dep("5", "Tel Aviv", minutes = 70),
        )
        val next = LineLogic.nextForLine(stop, "5", "Tel Aviv", now, count = 4)

        assertEquals(listOf(5L, 30L, 50L, 70L), next.map { (it.departsAtMillis - now) / 60_000L })
    }

    @Test
    fun oppositeDirectionPicksNearestOtherHeadsignWithTrip() {
        val nearby = listOf(
            dep("5", "Tel Aviv", minutes = 4),
            dep("5", "Holon", minutes = 12, at = far),
            dep("5", "Holon", minutes = 8, at = here),
            dep("5", "Holon", minutes = 3, at = here, tripId = null),
            dep("6", "Holon", minutes = 1, at = here),
        )
        val opposite = LineLogic.oppositeDirection(nearby, "5", "Tel Aviv", here, now)

        assertNotNull(opposite)
        assertEquals("Holon", opposite!!.headsign)
        assertEquals(8L, (opposite.departsAtMillis - now) / 60_000L)
    }

    @Test
    fun oppositeDirectionNullWhenOnlySameDirection() {
        val nearby = listOf(dep("5", "Tel Aviv", minutes = 4))
        assertNull(LineLogic.oppositeDirection(nearby, "5", "Tel Aviv", here, now))
    }

    @Test
    fun firstTripIsEarliestUpcomingForLineAndDirection() {
        val candidates = listOf(
            dep("5", "Tel Aviv", minutes = 20, tripId = "late"),
            dep("5", "Tel Aviv", minutes = 6, tripId = "early"),
            dep("5", "Holon", minutes = 1, tripId = "other"),
            dep("5", "Tel Aviv", minutes = -5, tripId = "gone"),
        )
        assertEquals("early", LineLogic.firstTrip(candidates, "5", "Tel Aviv", now))
        assertNull(LineLogic.firstTrip(candidates, "99", "Tel Aviv", now))
    }

    @Test
    fun dailyForKeepsOnlyDayWindowSortedByTime() {
        val (start, end) = LineLogic.dayBounds(now)
        val stop = listOf(
            dep("5", "Tel Aviv", minutes = 500),
            dep("5", "Tel Aviv", minutes = 0),
            dep("5", "Holon", minutes = 60),
            dep("5", "Tel Aviv", minutes = 1_440 * 3),
        )
        val day = LineLogic.dailyFor(stop, "5", "Tel Aviv", start, end)

        assertTrue(day.all { it.departsAtMillis in start until end })
        assertEquals(day.sortedBy { it.departsAtMillis }, day)
        assertTrue(day.none { it.headsign == "Holon" })
    }

    @Test
    fun dayBoundsSpanOneDayAndContainNow() {
        val (start, end) = LineLogic.dayBounds(now)
        assertTrue(now in start until end)
        val hours = (end - start) / 3_600_000.0
        assertTrue(hours in 23.0..25.0)
    }

    @Test
    fun clockIsIsraelTime() {
        // 18:00 UTC בנובמבר = 20:00 בשעון ישראל
        assertEquals("20:00", LineLogic.clock(now))
    }
}
