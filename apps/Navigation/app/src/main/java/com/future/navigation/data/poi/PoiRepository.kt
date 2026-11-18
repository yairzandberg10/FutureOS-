package com.future.navigation.data.poi

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * מקומות בקרבת מקום מ-Overpass. שרת ציבורי בלי מפתח, ולכן: בקשה אחת לכל היותר
 * בשנייה (דרך [Mutex]), ומטמון של כמה דקות לפי קטגוריה ונקודה מעוגלת (כ-110 מ').
 *
 * הבקשה בטלה כשהקורא מתבטל (קטגוריה אחרת נבחרה): כך שהשער משתחרר מיד ולא
 * נתקע עד שהשרת עונה. שגיאת רשת נזרקת למעלה - המסך מציג "אין חיבור", ושגיאה
 * לא נשמרת במטמון.
 */
class PoiRepository {
    private val gate = Mutex()
    private var lastRequestAtMs = 0L

    private class Cached(val atMs: Long, val results: List<PoiResult>)

    private val cache = object : LinkedHashMap<String, Cached>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Cached>) =
            size > MAX_CACHED_SEARCHES
    }

    suspend fun nearby(category: PoiCategory, origin: LatLng): List<PoiResult> {
        val key = "${category.name}|${roundedKey(origin.lat)},${roundedKey(origin.lon)}"
        val now = System.currentTimeMillis()
        synchronized(cache) { cache[key] }
            ?.takeIf { now - it.atMs < CACHE_TTL_MS }
            ?.let { return it.results }

        return gate.withLock {
            val elapsed = System.currentTimeMillis() - lastRequestAtMs
            if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
            lastRequestAtMs = System.currentTimeMillis()

            val body = fetch(OverpassParser.buildQuery(category, origin))
            val results = OverpassParser.parse(body, origin, category)
            synchronized(cache) { cache[key] = Cached(System.currentTimeMillis(), results) }
            results
        }
    }

    /** בקשת POST אסינכרונית שמתבטלת יחד עם הקורא (call.cancel). */
    private suspend fun fetch(query: String): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder()
            .url(OverpassParser.ENDPOINT)
            .header("User-Agent", USER_AGENT)
            .post(FormBody.Builder().add("data", query).build())
            .build()
        val call = NetworkModule.okHttpClient.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { r ->
                    try {
                        if (!r.isSuccessful) throw IOException("Overpass HTTP ${r.code}")
                        val text = r.body?.string() ?: throw IOException("Overpass empty body")
                        if (continuation.isActive) continuation.resume(text)
                    } catch (e: IOException) {
                        if (continuation.isActive) continuation.resumeWithException(e)
                    }
                }
            }
        })
    }

    private fun roundedKey(value: Double): String = String.format(Locale.US, "%.3f", value)

    private companion object {
        const val MIN_INTERVAL_MS = 1100L
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val MAX_CACHED_SEARCHES = 30
        const val USER_AGENT = "FutureOS-Navigation/1.0 (keys-only device app; no contact configured)"
    }
}
