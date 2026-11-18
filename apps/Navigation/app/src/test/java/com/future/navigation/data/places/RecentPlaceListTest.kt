package com.future.navigation.data.places

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentPlaceListTest {

    private fun place(n: Int) = RecentPlace(title = "מקום $n", address = "כתובת $n", lat = 32.0 + n * 0.01, lon = 34.8)

    @Test
    fun newestGoesFirstAndDuplicatesMoveToTheFront() {
        val list = RecentPlaceList.add(RecentPlaceList.add(emptyList(), place(1)), place(2))
        assertEquals(listOf(place(2), place(1)), list)

        val moved = RecentPlaceList.add(list, place(1))
        assertEquals(listOf(place(1), place(2)), moved)
    }

    @Test
    fun capsAtFifteenItems() {
        var list: List<RecentPlace> = emptyList()
        for (n in 1..20) list = RecentPlaceList.add(list, place(n))
        assertEquals(RecentPlaceList.MAX_ITEMS, list.size)
        assertEquals(place(20), list.first())
        assertTrue(list.none { it == place(1) })
    }

    @Test
    fun sameAddressFarAwayIsADifferentPlace() {
        // שתי תחנות "פז" באותה כתובת-שם, אבל בקילומטרים זו מזו - שתיהן נשמרות
        val a = RecentPlace("פז", "פז", 32.0, 34.8)
        val b = RecentPlace("פז", "פז", 32.05, 34.8)
        assertEquals(2, RecentPlaceList.add(listOf(a), b).size)
    }

    @Test
    fun removeDropsOnlyThatPlace() {
        val list = listOf(place(1), place(2), place(3))
        assertEquals(listOf(place(1), place(3)), RecentPlaceList.remove(list, place(2)))
    }

    @Test
    fun encodeAndDecodeRoundTrip() {
        val list = listOf(place(1), RecentPlace("עזריאלי", "עזריאלי, תל אביב", 32.0745, 34.7917))
        assertEquals(list, RecentPlaceList.decode(RecentPlaceList.encode(list)))
    }

    @Test
    fun brokenOrMissingStorageGivesEmptyList() {
        assertEquals(emptyList<RecentPlace>(), RecentPlaceList.decode(null))
        assertEquals(emptyList<RecentPlace>(), RecentPlaceList.decode("   "))
        assertEquals(emptyList<RecentPlace>(), RecentPlaceList.decode("{not a list"))
    }
}
