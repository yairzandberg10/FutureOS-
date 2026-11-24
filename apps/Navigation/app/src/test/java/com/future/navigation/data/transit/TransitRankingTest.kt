package com.future.navigation.data.transit

import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import org.junit.Assert.assertEquals
import org.junit.Test

class TransitRankingTest {

    private fun t(h: Int, m: Int) = h * 3600 + m * 60

    private fun trip(dep: Int, arr: Int, vararg lines: String) = TransitItinerary(
        legs = lines.map { TransitLeg(type = LegType.RIDE, routeShortName = it, fromStopName = "a", toStopName = "b") },
        departureSeconds = dep,
        arrivalSeconds = arr,
    )

    @Test
    fun `slow two-bus trip right after a direct one is dropped`() {
        // התשובה האמיתית של MOTIS ממעלה לבונה לאריאל (23.11.2026, 08:00)
        val input = listOf(
            trip(t(8, 6), t(8, 38), "462"),
            trip(t(8, 15), t(9, 55), "462", "464"),
            trip(t(9, 6), t(9, 38), "462"),
            trip(t(9, 25), t(10, 48), "462", "461"),
        )
        val out = TransitRanking.rank(input, requestedSeconds = t(8, 0))
        assertEquals(listOf(t(8, 6), t(9, 6)), out.map { it.departureSeconds })
    }

    @Test
    fun `trips leaving before the requested time are dropped`() {
        val input = listOf(trip(t(6, 46), t(7, 18), "462"), trip(t(9, 6), t(9, 38), "462"))
        val out = TransitRanking.rank(input, requestedSeconds = t(8, 0))
        assertEquals(listOf(t(9, 6)), out.map { it.departureSeconds })
    }

    @Test
    fun `overnight wait is dropped when a normal trip exists`() {
        val input = listOf(
            trip(t(23, 7), t(6, 25) + 24 * 3600, "462", "161", "161", "60"),
            trip(t(23, 57), t(24, 29), "461"),
        )
        val out = TransitRanking.rank(input, requestedSeconds = t(23, 0))
        assertEquals(listOf(t(23, 57)), out.map { it.departureSeconds })
    }

    @Test
    fun `after midnight departures sort after the evening ones`() {
        val input = listOf(trip(t(0, 37), t(1, 9), "461"), trip(t(23, 57), t(24, 29), "461"))
        val out = TransitRanking.rank(input, requestedSeconds = t(23, 0))
        assertEquals(listOf(t(23, 57), t(0, 37)), out.map { it.departureSeconds })
    }

    @Test
    fun `never returns empty for non empty input`() {
        val input = listOf(trip(t(6, 0), t(6, 30), "1"), trip(t(6, 10), t(6, 40), "2"))
        val out = TransitRanking.rank(input, requestedSeconds = t(12, 0))
        assertEquals(2, out.size)
    }

    @Test
    fun `same departure with an extra transfer for a few minutes is dropped`() {
        // MOTIS לתל אביב: 17:31 עם 2 החלפות (126 דק') ו-17:31 עם 3 (120 דק')
        val input = listOf(
            trip(t(17, 31), t(19, 37), "462", "110", "1"),
            trip(t(17, 31), t(19, 31), "462", "110", "2", "1"),
            trip(t(18, 31), t(20, 38), "462", "110", "1"),
        )
        val out = TransitRanking.rank(input, requestedSeconds = t(17, 20))
        assertEquals(listOf(3, 3), out.map { it.legs.size })
    }
}
