package com.future.navigation.data.backend

import com.future.navigation.data.common.LatLng
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * עטיפה לשלוש ה-Cloud Functions שמחליפות קריאה ישירה לשירות חיצוני
 * (ר' Navigation/firebase/functions/src/index.ts). כולן callable (נקראות ב-REST
 * דרך [FirebaseBackend.callFunction], בלי SDK), כולן
 * מחזירות את התשובה הגולמית של השירות - הפירוק נשאר בדיוק איפה שהיה
 * (HereModels/SiriXml), כדי שלא יהיו שתי גרסאות של אותו פירוק.
 *
 * כל פונקציה כאן מחזירה null כשהשרת לא זמין/נכשל, ולא זורקת. זה החוזה מול
 * הריפוזיטוריז: null פירושו "תעבוד ישירות מול השירות, כמו קודם".
 */
object NavigationFunctions {

    private const val FN_GEOCODE = "geocodeSearch"
    private const val FN_ROUTE = "drivingRoute"
    private const val FN_SIRI = "siriStopMonitoring"

    /** תוצאת geocoding מהשרת - אותו מבנה שה-UI כבר מכיר, בלי תלות ב-Nominatim. */
    data class RemotePlace(val label: String, val lat: Double, val lon: Double)

    private suspend fun call(name: String, payload: Map<String, Any?>): JsonObject? {
        if (!RemoteKeys.useProxy) return null
        return FirebaseBackend.callFunction(name, payload).asObject()
    }

    private fun JsonObject.text(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun JsonObject.number(key: String): Double? =
        (this[key] as? JsonPrimitive)?.content?.toDoubleOrNull()

    suspend fun geocodeSearch(query: String): List<RemotePlace>? {
        val data = call(FN_GEOCODE, mapOf("query" to query)) ?: return null
        val results = data["results"] as? JsonArray ?: return null
        return results.mapNotNull { entry ->
            val row = entry as? JsonObject ?: return@mapNotNull null
            val label = row.text("label") ?: return@mapNotNull null
            val lat = row.number("lat") ?: return@mapNotNull null
            val lon = row.number("lon") ?: return@mapNotNull null
            RemotePlace(label, lat, lon)
        }
    }

    suspend fun drivingRouteJson(from: LatLng, to: LatLng): String? {
        val data = call(
            FN_ROUTE,
            mapOf(
                "originLat" to from.lat, "originLon" to from.lon,
                "destinationLat" to to.lat, "destinationLon" to to.lon,
            ),
        ) ?: return null
        return data.text("route")
    }

    /** גוף ה-XML הגולמי של תשובת SIRI StopMonitoring, לפירוק ב-SiriXml. */
    suspend fun siriStopMonitoringXml(stopId: String, lineRef: String?): String? {
        val data = call(FN_SIRI, mapOf("stopId" to stopId, "lineRef" to lineRef)) ?: return null
        return data.text("xml")
    }
}
