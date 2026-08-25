package com.future.navigation.data.geocoding

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

    suspend fun search(query: String): List<GeocodeResult> {
        if (query.isBlank()) return emptyList()

        val elapsed = System.currentTimeMillis() - lastRequestAtMs
        if (elapsed < MIN_INTERVAL_MS) delay(MIN_INTERVAL_MS - elapsed)
        lastRequestAtMs = System.currentTimeMillis()

        return try {
            api.search(userAgent = USER_AGENT, query = query)
                .mapNotNull { result ->
                    val lat = result.lat.toDoubleOrNull() ?: return@mapNotNull null
                    val lon = result.lon.toDoubleOrNull() ?: return@mapNotNull null
                    GeocodeResult(label = result.display_name, location = LatLng(lat, lon))
                }
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        private const val MIN_INTERVAL_MS = 1100L
        private const val USER_AGENT = "FutureOS-Navigation/1.0 (keys-only device app; no contact configured)"
    }
}
