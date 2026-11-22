package com.future.navigation.data.online

import android.util.Log
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.Locale

/**
 * שירותים פתוחים שעובדים בלי מפתח ובלי שרת משלנו, כך שהניווט עובד מהרגע הראשון:
 *
 * - Transitous (api.transitous.org, מנוע MOTIS): תכנון מסלולי תחבורה ציבורית,
 *   זמני יציאה מתחנות וחיפוש תחנות, על נתוני ה-GTFS של משרד התחבורה. החישוב
 *   רץ אצלם, והמכשיר מקבל רק את התשובה (עשרות KB), בלי להוריד את קובץ ה-GTFS.
 * - OSRM (router.project-osrm.org): מסלול נהיגה עם הוראות פנייה. בלי פקקים חיים.
 *
 * שני השירותים מבקשים User-Agent מזהה ולא להעמיס, ולכן הקריאות כאן הן רק
 * בעקבות פעולת משתמש או ריענון של פעם בחצי דקה לפחות.
 *
 * כל פונקציה מחזירה null בכל כשל (רשת, קוד שגיאה) ולא זורקת.
 */
object OpenApis {
    private const val TAG = "OpenApis"
    private const val TRANSITOUS = "https://api.transitous.org"
    private const val OSRM = "https://router.project-osrm.org"
    private const val USER_AGENT = "FutureOS-Navigation/1.0 (keypad phone app)"

    private fun coord(p: LatLng) = String.format(Locale.US, "%.6f,%.6f", p.lat, p.lon)

    /** MOTIS plan: מסלולי תחבורה ציבורית ממוצא ליעד. time = ISO-8601, null = עכשיו. */
    suspend fun transitPlanJson(
        from: LatLng,
        to: LatLng,
        time: String? = null,
        arriveBy: Boolean = false,
        extra: Map<String, String> = emptyMap(),
    ): String? {
        val url = "$TRANSITOUS/api/v5/plan".toHttpUrl().newBuilder()
            .addQueryParameter("fromPlace", coord(from))
            .addQueryParameter("toPlace", coord(to))
            .addQueryParameter("numItineraries", "8")
            .addQueryParameter("language", "he")
            .apply { if (time != null) addQueryParameter("time", time) }
            .apply { if (arriveBy) addQueryParameter("arriveBy", "true") }
            .apply { extra.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
        return get(url, "plan")
    }

    /** MOTIS stoptimes סביב נקודה: היציאות הבאות מכל התחנות ברדיוס (מטרים). */
    suspend fun stopTimesNearJson(at: LatLng, radiusMeters: Int, count: Int): String? {
        val url = "$TRANSITOUS/api/v5/stoptimes".toHttpUrl().newBuilder()
            .addQueryParameter("center", coord(at))
            .addQueryParameter("radius", radiusMeters.toString())
            .addQueryParameter("n", count.toString())
            .addQueryParameter("language", "he")
            .build()
        return get(url, "stoptimes")
    }

    /** MOTIS geocode: חיפוש תחנה לפי שם, עם עדיפות לתחנות שקרובות ל-near. */
    suspend fun stopSearchJson(text: String, near: LatLng): String? {
        val url = "$TRANSITOUS/api/v1/geocode".toHttpUrl().newBuilder()
            .addQueryParameter("text", text)
            .addQueryParameter("type", "STOP")
            .addQueryParameter("place", coord(near))
            .addQueryParameter("placeBias", "5")
            .addQueryParameter("language", "he")
            .build()
        return get(url, "geocode")
    }

    /** MOTIS trip: כל התחנות של נסיעה אחת (לפי tripId מ-stoptimes או מהתכנון). */
    suspend fun tripJson(tripId: String): String? {
        val url = "$TRANSITOUS/api/v5/trip".toHttpUrl().newBuilder()
            .addQueryParameter("tripId", tripId)
            .build()
        return get(url, "trip")
    }

    /** OSRM: מסלול נהיגה עם צעדים וקו המסלול (GeoJSON). */
    suspend fun drivingRouteJson(from: LatLng, to: LatLng): String? {
        // ב-OSRM הסדר הוא אורך,רוחב
        val path = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.lon, from.lat, to.lon, to.lat)
        val url = "$OSRM/route/v1/driving/$path".toHttpUrl().newBuilder()
            .addQueryParameter("overview", "full")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("steps", "true")
            .build()
        return get(url, "osrm")
    }

    private suspend fun get(url: HttpUrl, what: String): String? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "$what HTTP ${response.code}")
                    null
                } else {
                    Log.i(TAG, "$what ok")
                    response.body?.string()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "$what failed: ${e.javaClass.simpleName}")
            null
        }
    }
}
