package com.future.navigation.data.geocoding

import com.future.navigation.data.backend.NavigationFunctions
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.delay

data class GeocodeResult(val label: String, val location: LatLng)

/**
 * עוטף את Nominatim ואוכף את מדיניות השימוש שלו (בקשה אחת לכל היותר בשנייה,
 * User-Agent מזהה) - לא רק תלוי בכך שהמסך לא יקרא לזה בקצב גבוה יותר.
 */
class GeocodingRepository {
    private val api: NominatimApi = NetworkModule
        .retrofit("https://nominatim.openstreetmap.org/")
        .create(NominatimApi::class.java)

    private var lastRequestAtMs = 0L

    // מטמון תוצאות: בהקלדה במקלדת T9 אותו שאילתה חוזרת שוב ושוב (תיקון
    // הקלדה, חזרה למסך), וכל חזרה כזו הייתה בקשה נוספת לשרת הדגמה ציבורי
    // שמדיניותו מגבילה לבקשה אחת בשנייה. LinkedHashMap במצב access-order
    // הוא LRU: הרשומה הכי ותיקה יוצאת כשהמטמון מתמלא.
    private val cache = object : LinkedHashMap<String, List<GeocodeResult>>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<GeocodeResult>>) =
            size > MAX_CACHED_QUERIES
    }

    suspend fun search(query: String): List<GeocodeResult> {
        if (query.isBlank()) return emptyList()

        val key = query.trim().lowercase()
        synchronized(cache) { cache[key] }?.let { return it }

        // נתיב ראשי: Cloud Function שמחזיקה מטמון משותף לכל המכשירים ומכבדת
        // את מדיניות Nominatim בצד אחד מרוכז, במקום שכל מכשיר ידבר מול שרת
        // הדגמה ציבורי בעצמו. null = אין שרת/השרת נכשל -> ממשיכים ישירות.
        NavigationFunctions.geocodeSearch(query)?.let { remote ->
            val results = remote
                .map { GeocodeResult(label = it.label, location = LatLng(it.lat, it.lon)) }
                .distinct()
            synchronized(cache) { cache[key] = results }
            return results
        }

        val elapsed = System.currentTimeMillis() - lastRequestAtMs
        if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
        lastRequestAtMs = System.currentTimeMillis()

        return try {
            val results = api.search(userAgent = USER_AGENT, query = query)
                .mapNotNull { result ->
                    val lat = result.lat.toDoubleOrNull() ?: return@mapNotNull null
                    val lon = result.lon.toDoubleOrNull() ?: return@mapNotNull null
                    GeocodeResult(label = result.display_name, location = LatLng(lat, lon))
                }
                // Nominatim מחזיר לעיתים את אותו מקום פעמיים (רשומות OSM שונות
                // לאותה כתובת). הרשימה משמשת כמפתחות ב-LazyColumn של מסך הבית,
                // ומפתח כפול הוא קריסה - אז הכפילויות מסוננות כאן, במקור.
                .distinct()
            synchronized(cache) { cache[key] = results }
            results
        } catch (e: Exception) {
            android.util.Log.w("GeocodingRepository", "search failed for $query", e)
            emptyList()
        }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 1100L
        private const val MAX_CACHED_QUERIES = 50
        private const val USER_AGENT = "FutureOS-Navigation/1.0 (keys-only device app; no contact configured)"
    }
}
