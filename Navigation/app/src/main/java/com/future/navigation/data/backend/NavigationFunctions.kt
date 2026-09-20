package com.future.navigation.data.backend

import android.util.Log
import com.future.navigation.data.common.LatLng

/**
 * עטיפה לשלוש ה-Cloud Functions שמחליפות קריאה ישירה לשירות חיצוני
 * (ר' Navigation/firebase/functions/src/index.ts). כולן callable, כולן
 * מחזירות את התשובה הגולמית של השירות - הפירוק נשאר בדיוק איפה שהיה
 * (HereModels/SiriXml), כדי שלא יהיו שתי גרסאות של אותו פירוק.
 *
 * כל פונקציה כאן מחזירה null כשהשרת לא זמין/נכשל, ולא זורקת. זה החוזה מול
 * הריפוזיטוריז: null פירושו "תעבוד ישירות מול השירות, כמו קודם".
 */
object NavigationFunctions {

    private const val TAG = "NavigationFunctions"

    private const val FN_GEOCODE = "geocodeSearch"
    private const val FN_ROUTE = "drivingRoute"
    private const val FN_SIRI = "siriStopMonitoring"

    /** תוצאת geocoding מהשרת - אותו מבנה שה-UI כבר מכיר, בלי תלות ב-Nominatim. */
    data class RemotePlace(val label: String, val lat: Double, val lon: Double)

    private suspend fun call(name: String, payload: Map<String, Any?>): Map<*, *>? {
        if (!RemoteKeys.useProxy) return null
        val functions = FirebaseBackend.functions ?: return null
        return try {
            val result = functions.getHttpsCallable(name).call(payload).awaitResult()
            result.getData() as? Map<*, *>
        } catch (e: Exception) {
            Log.w(TAG, "$name failed - falling back to the direct call", e)
            null
        }
    }

    suspend fun geocodeSearch(query: String): List<RemotePlace>? {
        val data = call(FN_GEOCODE, mapOf("query" to query)) ?: return null
        val results = data["results"] as? List<*> ?: return null
        return results.mapNotNull { entry ->
            val row = entry as? Map<*, *> ?: return@mapNotNull null
            val label = row["label"] as? String ?: return@mapNotNull null
            val lat = (row["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
            val lon = (row["lon"] as? Number)?.toDouble() ?: return@mapNotNull null
            RemotePlace(label, lat, lon)
        }
    }

    /** גוף התשובה הגולמי של HERE Routing v8 כמחרוזת JSON, לפירוק ב-HereModels. */
    suspend fun drivingRouteJson(from: LatLng, to: LatLng): String? {
        val data = call(
            FN_ROUTE,
            mapOf(
                "originLat" to from.lat, "originLon" to from.lon,
                "destinationLat" to to.lat, "destinationLon" to to.lon,
            ),
        ) ?: return null
        return data["route"] as? String
    }

    /** גוף ה-XML הגולמי של תשובת SIRI StopMonitoring, לפירוק ב-SiriXml. */
    suspend fun siriStopMonitoringXml(stopId: String, lineRef: String?): String? {
        val data = call(FN_SIRI, mapOf("stopId" to stopId, "lineRef" to lineRef)) ?: return null
        return data["xml"] as? String
    }
}
