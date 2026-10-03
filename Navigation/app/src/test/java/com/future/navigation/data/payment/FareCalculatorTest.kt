package com.future.navigation.data.payment

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FareCalculatorTest {

    private val telAviv = LatLng(32.0853, 34.7818)
    private val ramatGan = LatLng(32.0684, 34.8248)   // ~4.5 ק"מ
    private val jerusalem = LatLng(31.7683, 35.2137)  // ~54 ק"מ
    private val haifa = LatLng(32.7940, 34.9896)      // ~81 ק"מ
    private val eilat = LatLng(29.5577, 34.9519)      // ~280 ק"מ

    private fun ride(from: LatLng, to: LatLng, departure: Int, routeType: Int = 3) = TransitLeg(
        type = LegType.RIDE, routeShortName = "1", fromStopName = "a", toStopName = "b",
        departureSeconds = departure, fromStopLocation = from, toStopLocation = to, routeType = routeType
    )

    private fun fare(vararg legs: TransitLeg) = FareCalculator.calculate(TransitItinerary(legs.toList(), 0, 0))

    @Test fun `yellow single ride`() {
        val f = fare(ride(telAviv, ramatGan, 8 * 3600))
        assertEquals(800, f.totalAgorot)
        assertEquals(FareRadius.YELLOW, f.rides.single().radius)
    }

    @Test fun `free transfer within 90 minutes`() {
        val f = fare(ride(telAviv, ramatGan, 8 * 3600), ride(ramatGan, telAviv, 8 * 3600 + 30 * 60))
        assertEquals(800, f.totalAgorot)
        assertTrue(f.rides[1].isFreeTransfer)
    }

    @Test fun `no free transfer after 90 minutes`() {
        assertEquals(1600, fare(ride(telAviv, ramatGan, 8 * 3600), ride(ramatGan, telAviv, 8 * 3600 + 91 * 60)).totalAgorot)
    }

    @Test fun `bus Tel Aviv - Jerusalem is light blue`() {
        val f = fare(ride(telAviv, jerusalem, 0))
        assertEquals(1900, f.totalAgorot)
        assertEquals(FareRadius.LIGHT_BLUE, f.rides[0].radius)
    }

    @Test fun `rail Tel Aviv - Haifa uses the rail column`() {
        val f = fare(ride(telAviv, haifa, 0, routeType = 2))
        assertEquals(3050, f.totalAgorot)
        assertEquals(FareMode.RAIL, f.rides[0].mode)
    }

    @Test fun `rail gives no free transfer`() {
        assertEquals(1950, fare(ride(telAviv, ramatGan, 0, routeType = 2), ride(ramatGan, telAviv, 600)).totalAgorot)
    }

    @Test fun `grey radius`() {
        assertEquals(8732, fare(ride(telAviv, eilat, 0)).totalAgorot)
    }

    @Test fun `unknown stop location`() {
        val f = fare(TransitLeg(LegType.RIDE, fromStopName = "a", toStopName = "b"))
        assertFalse(f.isComplete)
        assertNull(f.rides[0].agorot)
    }

    @Test fun format() {
        assertEquals("₪8", FareCalculator.format(800))
        assertEquals("₪14.5", FareCalculator.format(1450))
        assertEquals("₪87.32", FareCalculator.format(8732))
        assertEquals("₪3.05", FareCalculator.format(305))
    }
}
