package com.future.navigation.data.lines

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** קידוד ושינוי רשימות "לאחרונה" ו"מועדפים" (בלי SharedPreferences). */
class LineHistoryCodecTest {

    private fun line(n: Int, headsign: String = "תל אביב", agency: String? = "מטרופולין") = SavedLine(
        line = "$n",
        headsign = headsign,
        agency = agency,
        stopName = "תחנה $n",
        stopId = "stop-$n",
        lat = 32.0 + n / 1000.0,
        lon = 34.7 + n / 1000.0,
    )

    @Test
    fun encodeThenDecodeRoundTrips() {
        val lines = listOf(line(5), line(9, agency = null))
        val decoded = LineHistoryCodec.decode(LineHistoryCodec.encode(lines))
        assertEquals(lines, decoded)
    }

    @Test
    fun tabsAndNewlinesInsideFieldsDoNotBreakRows() {
        val tricky = line(1).copy(stopName = "תחנה\tעם טאב\nושורה")
        val decoded = LineHistoryCodec.decode(LineHistoryCodec.encode(listOf(tricky, line(2))))
        assertEquals(2, decoded.size)
        assertEquals("תחנה עם טאב ושורה", decoded[0].stopName)
        assertEquals("1", decoded[0].line)
    }

    @Test
    fun malformedRowsAreSkipped() {
        val text = listOf(
            "",
            "5\tתל אביב\tמטרופולין\tתחנה\tstop\tnot-a-number\t34.7",
            "\tתל אביב\t\tתחנה\t\t32.0\t34.7",
            "5\tתל אביב",
            "9\tתל אביב\t\tתחנה\t\t32.1\t34.8",
        ).joinToString("\n")

        val decoded = LineHistoryCodec.decode(text)
        assertEquals(listOf("9"), decoded.map { it.line })
        assertNull(decoded.first().agency)
        assertNull(decoded.first().stopId)
    }

    @Test
    fun pushRecentPutsNewestFirstWithoutDuplicatesAndCaps() {
        var list = emptyList<SavedLine>()
        for (n in 1..12) list = LineHistoryCodec.pushRecent(list, line(n))
        assertEquals(LineHistoryCodec.MAX_RECENT, list.size)
        assertEquals("12", list.first().line)
        assertEquals(listOf("12", "11", "10", "9", "8", "7", "6", "5", "4", "3"), list.map { it.line })

        // פתיחה חוזרת של קו קיים מעלה אותו לראש בלי כפילות
        val again = LineHistoryCodec.pushRecent(list, line(5))
        assertEquals("5", again.first().line)
        assertEquals(1, again.count { it.line == "5" })
        assertEquals(LineHistoryCodec.MAX_RECENT, again.size)
    }

    @Test
    fun sameLineInAnotherDirectionIsADifferentEntry() {
        val a = line(5, headsign = "תל אביב")
        val b = line(5, headsign = "חולון")
        val list = LineHistoryCodec.pushRecent(listOf(a), b)
        assertEquals(2, list.size)
    }

    @Test
    fun toggleAddsThenRemoves() {
        val item = line(7)
        val added = LineHistoryCodec.toggle(emptyList(), item)
        assertEquals(listOf(item), added)
        assertEquals(emptyList<SavedLine>(), LineHistoryCodec.toggle(added, item))
    }
}
