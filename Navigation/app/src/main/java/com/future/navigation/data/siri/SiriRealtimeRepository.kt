package com.future.navigation.data.siri

import com.future.navigation.data.backend.NavigationFunctions
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * מקור אמת לתחזיות הגעה בזמן אמת (SIRI Stop Monitoring) - שכבה נוספת מעל
 * הלו"ז הסטטי של GTFS, לא תחליף לו (ר' TransitRealtimeEnricher.kt לאופן
 * השילוב). "best effort" בכוונה: כל כשל (רשת, מפתח לא מוגדר, תשובה לא
 * צפויה) חוזר כרשימה ריקה בלי לזרוק, כדי שתכנון מסלול לעולם לא ייכשל בגלל
 * שהשכבה בזמן אמת לא זמינה כרגע.
 */
class SiriRealtimeRepository {
    private val client = NetworkModule.okHttpClient

    suspend fun stopMonitoring(stopId: String, lineRef: String? = null): List<RealtimeArrival> {
        if (!SiriConfig.isConfigured) return emptyList()

        // נתיב ראשי: Cloud Function שמחזיקה את כתובת ה-SIRI ואת המפתח בסוד
        // בצד השרת ומחזירה את ה-XML הגולמי - אותו פירוק בדיוק (SiriXml).
        NavigationFunctions.siriStopMonitoringXml(stopId, lineRef)?.let { xml ->
            return runCatching { SiriXml.parseStopMonitoringResponse(xml) }.getOrDefault(emptyList())
        }

        if (SiriConfig.baseUrl.isBlank() || SiriConfig.apiKey.isBlank()) return emptyList()

        return withContext(Dispatchers.IO) {
            try {
                val requestXml = SiriXml.buildStopMonitoringRequest(stopId, SiriConfig.apiKey, lineRef)
                val request = Request.Builder()
                    .url(SiriConfig.baseUrl)
                    .post(requestXml.toRequestBody(XML_MEDIA_TYPE))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext emptyList()
                    val xml = response.body?.string() ?: return@withContext emptyList()
                    SiriXml.parseStopMonitoringResponse(xml)
                }
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    companion object {
        private val XML_MEDIA_TYPE = "text/xml; charset=utf-8".toMediaType()
    }
}
