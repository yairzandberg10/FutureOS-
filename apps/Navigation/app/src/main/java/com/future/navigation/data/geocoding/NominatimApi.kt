package com.future.navigation.data.geocoding

import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Nominatim (חיפוש/גיאוקוד של OpenStreetMap) - חינמי, בלי מפתח, בלי חשבון.
 * מדיניות השימוש דורשת User-Agent מזהה ולא יותר מבקשה אחת בשנייה - ר'
 * GeocodingRepository, שאוכף את שני הדברים (לא רק ה-header כאן).
 */
interface NominatimApi {
    @GET("search")
    suspend fun search(
        @Header("User-Agent") userAgent: String,
        @Query("q") query: String,
        @Query("format") format: String = "jsonv2",
        @Query("limit") limit: Int = 5,
        @Query("accept-language") acceptLanguage: String = "he"
    ): List<NominatimResult>
}
