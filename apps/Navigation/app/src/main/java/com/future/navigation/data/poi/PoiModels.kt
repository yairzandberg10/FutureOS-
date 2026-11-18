package com.future.navigation.data.poi

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import java.util.Locale
import kotlin.math.roundToInt

/**
 * קטגוריה לחיפוש מקומות בקרבת מקום. [filters] הם תגיות OSM (מפתח, ערך);
 * קטגוריה עם כמה תגיות (בית קפה ומסעדה) מאחדת את התוצאות.
 *
 * [radiusMeters] לפי כמה מקומות יש בפועל: חניה, כספומט וקפה הם בכל רחוב, אז
 * רדיוס קצר; בתי חולים וטעינה לרכב חשמלי נדירים, ולכן רדיוס רחב. השרת מחזיר
 * את הקרובים ליעד בתוך הרדיוס, ולא מדלג על התוצאות הקרובות.
 */
enum class PoiCategory(val label: String, val filters: List<Pair<String, String>>, val radiusMeters: Int) {
    FUEL("דלק", listOf("amenity" to "fuel"), 3000),
    PARKING("חניה", listOf("amenity" to "parking"), 1000),
    PHARMACY("מרקחת", listOf("amenity" to "pharmacy"), 3000),
    SUPERMARKET("סופר", listOf("shop" to "supermarket"), 3000),
    ATM("כספומט", listOf("amenity" to "atm"), 1000),
    CAFE_RESTAURANT("קפה ואוכל", listOf("amenity" to "cafe", "amenity" to "restaurant"), 1000),
    HOSPITAL("בית חולים", listOf("amenity" to "hospital"), 15000),
    EV_CHARGING("טעינה לחשמלי", listOf("amenity" to "charging_station"), 12000),
}

/** מקום בקרבת מקום: שם, מיקום והמרחק מהמשתמש במטרים. */
data class PoiResult(val name: String, val location: LatLng, val distanceMeters: Double)

/** גבולות ישראל (כולל יהודה ושומרון) - החיפוש בקרבת מקום מוגבל לתחום הזה. */
object IsraelBounds {
    fun contains(point: LatLng): Boolean =
        point.lat in 29.4..33.4 && point.lon in 34.2..35.9
}

/** פעולות טהורות על רשימות מקומות: מיון לפי מרחק, הסרת כפילויות ותצוגת מרחק. */
object PlaceSorting {

    /** "350 מ׳" עד קילומטר, ומעליו "1.4 ק״מ". */
    fun formatDistance(meters: Double): String =
        if (meters < 1000) "${meters.roundToInt()} מ׳"
        else String.format(Locale.US, "%.1f", meters / 1000) + " ק״מ"

    /** בלי מיקום של המשתמש (עדיין לא ידוע) הסדר נשאר כמו שהגיע. */
    fun <T> sortNearest(items: List<T>, origin: LatLng?, locationOf: (T) -> LatLng): List<T> {
        if (origin == null) return items
        return items.sortedBy { origin.distanceMetersTo(locationOf(it)) }
    }

    /**
     * מסיר כפילות: אותו שם ובמרחק קטן מ-[radiusMeters] ממקום שכבר נשמר. נשמר
     * המופע הראשון. מתאים לרשימות קטנות (עשרות פריטים).
     */
    fun <T> dedupeNearby(
        items: List<T>,
        nameOf: (T) -> String,
        locationOf: (T) -> LatLng,
        radiusMeters: Double = 50.0,
    ): List<T> {
        val kept = ArrayList<T>()
        for (item in items) {
            val duplicate = kept.any { other ->
                nameOf(other) == nameOf(item) && locationOf(other).distanceMetersTo(locationOf(item)) < radiusMeters
            }
            if (!duplicate) kept.add(item)
        }
        return kept
    }
}
