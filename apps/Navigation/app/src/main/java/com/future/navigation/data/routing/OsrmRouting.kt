package com.future.navigation.data.routing

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import java.util.Locale

/**
 * קריאות OSRM למסלולי הליכה, רכיבה והחלופות לנהיגה. כל כשל ברשת מחזיר null
 * (שגיאת שרת) ולא זורק, והתשובה מפורקת ב-OsrmParser.
 */
object OsrmRouting {
    private const val USER_AGENT = "FutureOS-Navigation/1.0 (keypad phone app)"

    /**
     * כל המסלולים שהשרת מחזיר. null = שגיאת שרת/רשת, רשימה ריקה = אין מסלול.
     * alternatives=true מבקש עד 3 חלופות (נתמך בנהיגה, הליכה ורכיבה).
     */
    suspend fun fetchAll(from: LatLng, to: LatLng, profile: RoutingProfile, alternatives: Boolean): List<DrivingRoute>? {
        val root = fetchRoot(from, to, profile, alternatives) ?: return null
        return OsrmParser.parseAll(root)
    }

    /** המסלול הראשון בלבד, כמו RoutingRepository (RouteLookup). */
    suspend fun route(from: LatLng, to: LatLng, profile: RoutingProfile): RouteLookup {
        val root = fetchRoot(from, to, profile, alternatives = false) ?: return RouteLookup.ServerError
        return OsrmParser.parse(root)
    }

    private suspend fun fetchRoot(from: LatLng, to: LatLng, profile: RoutingProfile, alternatives: Boolean): JsonObject? {
        // ב-OSRM הסדר הוא אורך,רוחב
        val coords = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f", from.lon, from.lat, to.lon, to.lat)
        val url = "${profile.baseUrl}/route/v1/${profile.path}/$coords".toHttpUrl().newBuilder()
            .addQueryParameter("overview", "full")
            .addQueryParameter("geometries", "geojson")
            .addQueryParameter("steps", "true")
            .apply { if (alternatives) addQueryParameter("alternatives", "true") }
            .build()
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).header("User-Agent", USER_AGENT).build()
                NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val body = response.body?.string() ?: return@use null
                    NetworkModule.json.parseToJsonElement(body) as? JsonObject
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                null
            }
        }
    }
}
