package com.future.navigation.data.transit

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.ZonedDateTime

class TransitPlanningTest {

    /** יום שני 19.11.2026 12:00 בשעון ישראל (חורף, UTC+2). */
    private val now: ZonedDateTime = ZonedDateTime.of(2026, 11, 19, 12, 0, 0, 0, ISRAEL_ZONE)

    @Test
    fun fareTableByDistance() {
        assertEquals(8.0, TransitPlanning.estimatedFareShekel(5_000.0), 0.0)
        assertEquals(8.0, TransitPlanning.estimatedFareShekel(15_000.0), 0.0)
        assertEquals(12.5, TransitPlanning.estimatedFareShekel(15_001.0), 0.0)
        assertEquals(12.5, TransitPlanning.estimatedFareShekel(40_000.0), 0.0)
        assertEquals(19.0, TransitPlanning.estimatedFareShekel(40_001.0), 0.0)
        assertEquals(19.0, TransitPlanning.estimatedFareShekel(120_000.0), 0.0)
        assertEquals(27.5, TransitPlanning.estimatedFareShekel(120_001.0), 0.0)
    }

    @Test
    fun formatShekelShowsDecimalOnlyWhenNeeded() {
        assertEquals("₪8", TransitPlanning.formatShekel(8.0))
        assertEquals("₪12.5", TransitPlanning.formatShekel(12.5))
    }

    @Test
    fun departNowWithoutShiftSendsNoTime() {
        val plan = TransitPlanning.planTime(TripWhen(), now)
        assertNull(plan.iso)
        assertEquals(false, plan.arriveBy)
    }

    @Test
    fun departNowWithShiftSendsExplicitTime() {
        val plan = TransitPlanning.planTime(TripWhen(shiftMinutes = 10), now)
        assertEquals("2026-11-19T12:10:00+02:00", plan.iso)
        assertEquals(false, plan.arriveBy)
    }

    @Test
    fun lastLinesIsTwentyThreeOClockToday() {
        val plan = TransitPlanning.planTime(TripWhen(kind = TripWhenKind.LAST_LINES), now)
        assertEquals("2026-11-19T23:00:00+02:00", plan.iso)
        assertEquals(false, plan.arriveBy)
    }

    @Test
    fun shiftMovesLastLinesBackTenMinutes() {
        val plan = TransitPlanning.planTime(TripWhen(kind = TripWhenKind.LAST_LINES, shiftMinutes = -10), now)
        assertEquals("2026-11-19T22:50:00+02:00", plan.iso)
    }

    @Test
    fun departAtPastTimeRollsToTomorrow() {
        val plan = TransitPlanning.planTime(TripWhen(TripWhenKind.DEPART_AT, 8, 30), now)
        assertEquals("2026-11-20T08:30:00+02:00", plan.iso)
        assertEquals(false, plan.arriveBy)
    }

    @Test
    fun arriveByUsesArriveFlag() {
        val plan = TransitPlanning.planTime(TripWhen(TripWhenKind.ARRIVE_BY, 9, 15), now)
        assertEquals("2026-11-19T09:15:00+02:00", plan.iso)
        assertEquals(true, plan.arriveBy)
    }

    @Test
    fun whenLabelShowsShift() {
        assertEquals("יציאה עכשיו", TransitPlanning.whenLabel(TripWhen()))
        assertEquals("יציאה ב-08:30 (+10 דק׳)", TransitPlanning.whenLabel(TripWhen(TripWhenKind.DEPART_AT, 8, 30, 10)))
    }

    @Test
    fun defaultPrefsSendNoParams() {
        assertEquals(emptyMap<String, String>(), TransitPlanning.planParams(TransitPrefs()))
    }

    @Test
    fun preferenceParams() {
        assertEquals(
            mapOf("maxPreTransitTime" to "600", "maxPostTransitTime" to "600"),
            TransitPlanning.planParams(TransitPrefs(preference = RoutePreference.LESS_WALKING)),
        )
        assertEquals(mapOf("maxTransfers" to "1"), TransitPlanning.planParams(TransitPrefs(preference = RoutePreference.FEWER_TRANSFERS)))
        assertEquals(mapOf("pedestrianProfile" to "WHEELCHAIR"), TransitPlanning.planParams(TransitPrefs(preference = RoutePreference.WHEELCHAIR)))
    }

    @Test
    fun disabledBusSendsOnlyRailModes() {
        assertEquals(
            mapOf("transitModes" to "RAIL,TRAM,SUBWAY"),
            TransitPlanning.planParams(TransitPrefs(bus = false)),
        )
    }

    @Test
    fun minutesUntilWrapsAroundMidnight() {
        // יציאה ב-00:10, עכשיו 23:50 -> 20 דקות
        assertEquals(20, TransitPlanning.minutesUntil(10 * 60, 23 * 3600 + 50 * 60))
        assertEquals(30, TransitPlanning.minutesUntil(10 * 3600 + 30 * 60, 10 * 3600))
    }

    @Test
    fun clockFormatsSecondsOfDay() {
        assertEquals("23:50", TransitPlanning.clock(23 * 3600 + 50 * 60))
    }
}
