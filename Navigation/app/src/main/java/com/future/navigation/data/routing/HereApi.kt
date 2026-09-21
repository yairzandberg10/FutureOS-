package com.future.navigation.data.routing

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * HERE Routing API v8 - חישוב מסלול נהיגה עם פקקים בזמן אמת. return=turnByTurnActions
 * (ולא רק return=actions) הוא זה שמחזיר currentRoad/nextRoad/direction/severity
 * לכל תמרון, וגם דורש polyline (שמסופק תמיד כאן). departureTime לא מצוין
 * בכוונה - לפי המפרט הרשמי, כשלא מצוינים departureTime/arrivalTime הזמן
 * הנוכחי בפועל משמש לחישוב, כלומר פקקים בזמן אמת נלקחים בחשבון כברירת מחדל
 * (traffic[mode]=default מצוין כאן רק לשם פירוש מפורש, זו גם ברירת המחדל).
 */
interface HereApi {
    @GET("v8/routes")
    suspend fun routes(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("transportMode") transportMode: String = "car",
        @Query("routingMode") routingMode: String = "fast",
        @Query("return") returnFields: String = "polyline,summary,travelSummary,turnByTurnActions",
        @Query("lang") lang: String = "he",
        @Query("traffic[mode]") trafficMode: String = "default",
        @Query("apikey") apiKey: String
    ): HereRouteResponse
}
