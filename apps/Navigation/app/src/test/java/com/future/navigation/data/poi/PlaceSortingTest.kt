package com.future.navigation.data.poi

import com.future.navigation.data.common.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaceSortingTest {

    private val origin = LatLng(32.0, 34.8)

    @Test
    fun formatsMetersThenKilometres() {
        assertEquals("350 מ׳", PlaceSorting.formatDistance(350.4))
        assertEquals("1.2 ק״מ", PlaceSorting.formatDistance(1234.0))
    }

    @Test
    fun sortsNearestFirstAndKeepsOrderWithoutOrigin() {
        val far = LatLng(32.01, 34.8)   // כ-1.1 ק"מ
        val near = LatLng(32.001, 34.8) // כ-110 מ'
        assertEquals(listOf(near, far), PlaceSorting.sortNearest(listOf(far, near), origin) { it })
        assertEquals(listOf(far, near), PlaceSorting.sortNearest(listOf(far, near), null) { it })
    }

    @Test
    fun dedupesSameNameWithinFiftyMetersOnly() {
        val here = LatLng(32.0, 34.8)
        val tenMetersAway = LatLng(32.00009, 34.8)
        val kmAway = LatLng(32.01, 34.8)
        val items = listOf(
            "A" to here,
            "A" to tenMetersAway,
            "A" to kmAway,
            "B" to here,
        )
        val kept = PlaceSorting.dedupeNearby(items, nameOf = { it.first }, locationOf = { it.second })
        assertEquals(listOf("A" to here, "A" to kmAway, "B" to here), kept)
    }
}
