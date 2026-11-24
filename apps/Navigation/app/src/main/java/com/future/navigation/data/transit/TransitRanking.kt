package com.future.navigation.data.transit

import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary

/**
 * סינון ומיון של המסלולים שהשרת (MOTIS) מחזיר, כמו שאפליקציות התחבורה עושות:
 * השרת מחזיר "חלון" של מסלולים, וחלק מהם גרועים בבירור. בבדיקה (מעלה לבונה ->
 * אריאל) הופיעו 462+464 ב-06:55 (100 דק') אחרי 462 ישיר ב-06:46 (32 דק'), מסלולים
 * שיוצאים לפני השעה שביקשו, ומסלולים של לילה שלם בהמתנה. כאן:
 *  1. מסלול שיוצא לפני הזמן המבוקש (יותר מ-2 דק') יוצא מהרשימה.
 *  2. מסלול "נשלט": יש אחר שיוצא כמעט באותו זמן (עד 20 דק' קודם) או אחריו, מגיע
 *     לפחות דקה קודם, ולא עם יותר החלפות - מיותר.
 *  3. מסלול ארוך בהרבה מהמהיר ביותר (פי 2.5, ולפחות 90 דק' יותר) - מיותר.
 *  4. מיון לפי שעת יציאה (בזמן הגעה - לפי שעת הגעה, המאוחר ראשון), עד 6 מסלולים.
 * אף פעם לא מחזיר רשימה ריקה כשהקלט לא ריק.
 */
object TransitRanking {
    private const val DAY = 24 * 3600
    private const val EARLY_TOLERANCE_S = 120
    private const val DEPART_SLACK_S = 20 * 60
    private const val ARRIVE_GAIN_S = 60
    private const val SLOW_FACTOR = 2.5
    private const val SLOW_MIN_EXTRA_S = 90 * 60
    private const val SAME_DEPART_S = 10 * 60
    private const val TRANSFER_WORTH_S = 15 * 60
    const val MAX_RESULTS = 6

    /** requestedSeconds: הזמן המבוקש בשניות מחצות (שעון ישראל), null = לא ידוע. */
    fun rank(
        itineraries: List<TransitItinerary>,
        requestedSeconds: Int?,
        arriveBy: Boolean = false,
    ): List<TransitItinerary> {
        if (itineraries.size <= 1) return itineraries
        val ref = requestedSeconds ?: itineraries.minOf { it.departureSeconds }
        val items = itineraries.distinctBy { key(it) }.map { Item(it, ref) }

        var kept = items
        if (!arriveBy && requestedSeconds != null) {
            kept = kept.filter { it.dep >= ref - EARLY_TOLERANCE_S }
        }
        kept = kept.filter { b -> kept.none { a -> a !== b && dominates(a, b) } }
        // אותה יציאה בערך, עוד החלפה בשביל כמה דקות - מבלבל ולא שווה: נשאר הפשוט
        kept = kept.filter { b -> kept.none { a -> a !== b && simplerAlmostAsFast(a, b) } }
        val fastest = kept.minOfOrNull { it.duration }
        if (fastest != null) {
            val limit = maxOf((fastest * SLOW_FACTOR).toInt(), fastest + SLOW_MIN_EXTRA_S)
            kept = kept.filter { it.duration <= limit }
        }
        if (kept.isEmpty()) kept = items

        val sorted = if (arriveBy) kept.sortedWith(compareByDescending<Item> { it.arr }.thenBy { it.duration })
        else kept.sortedWith(compareBy<Item> { it.dep }.thenBy { it.arr })
        return sorted.take(MAX_RESULTS).map { it.source }
    }

    private fun simplerAlmostAsFast(a: Item, b: Item): Boolean =
        a.rides < b.rides && kotlin.math.abs(a.dep - b.dep) <= SAME_DEPART_S && a.arr <= b.arr + TRANSFER_WORTH_S

    private fun dominates(a: Item, b: Item): Boolean =
        a.arr <= b.arr - ARRIVE_GAIN_S && a.dep >= b.dep - DEPART_SLACK_S && a.rides <= b.rides

    private fun key(it: TransitItinerary): String =
        "${it.departureSeconds}-${it.arrivalSeconds}-" +
            it.legs.filter { l -> l.type == LegType.RIDE }.joinToString("+") { l -> l.routeShortName.orEmpty() }

    /** זמנים מנורמלים ביחס לזמן המבוקש: יציאה אחרי חצות נספרת ביום הבא. */
    private class Item(val source: TransitItinerary, ref: Int) {
        val dep: Int
        val arr: Int
        val rides = source.legs.count { it.type == LegType.RIDE }
        val duration get() = arr - dep

        init {
            val shift = if (source.departureSeconds < ref - 6 * 3600) DAY else 0
            dep = source.departureSeconds + shift
            arr = source.arrivalSeconds + shift
        }
    }
}
