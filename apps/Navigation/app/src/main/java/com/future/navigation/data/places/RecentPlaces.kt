package com.future.navigation.data.places

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.math.abs

/** יעד שנבחר לאחרונה: השם לתצוגה, הכתובת המלאה והקואורדינטות. */
@Serializable
data class RecentPlace(val title: String, val address: String, val lat: Double, val lon: Double)

/**
 * פעולות טהורות על רשימת היעדים האחרונים (בלי Android, כדי שיהיו ניתנות לבדיקה
 * ביחידה). החדש ביותר בראש, בלי כפילויות, עד [MAX_ITEMS] פריטים.
 */
object RecentPlaceList {
    const val MAX_ITEMS = 15

    /** אותו מקום = אותה כתובת ובתוך כ-50 מ' (שתי "Shell" באותו שם הן שני מקומות). */
    fun sameSpot(a: RecentPlace, b: RecentPlace): Boolean =
        a.address == b.address && abs(a.lat - b.lat) < 0.0005 && abs(a.lon - b.lon) < 0.0005

    fun add(list: List<RecentPlace>, place: RecentPlace): List<RecentPlace> =
        (listOf(place) + list.filterNot { sameSpot(it, place) }).take(MAX_ITEMS)

    fun remove(list: List<RecentPlace>, place: RecentPlace): List<RecentPlace> =
        list.filterNot { sameSpot(it, place) }

    private val json = Json { ignoreUnknownKeys = true }
    private val serializer = ListSerializer(RecentPlace.serializer())

    fun encode(list: List<RecentPlace>): String = json.encodeToString(serializer, list)

    /** שורה פגומה או ריקה = רשימה ריקה, ולא קריסה של מסך הבית. */
    fun decode(text: String?): List<RecentPlace> {
        if (text.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(serializer, text).take(MAX_ITEMS)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
