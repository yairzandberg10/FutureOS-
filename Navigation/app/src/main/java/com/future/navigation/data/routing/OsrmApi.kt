package com.future.navigation.data.routing

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * שרת ההדגמה הציבורי של OSRM - חינמי, בלי מפתח, בלי חשבון. זהו שרת דמו
 * משותף ולא מיועד לעומס כבד; אם בעתיד יידרש שרת עצמאי, זהו הממשק היחיד
 * שצריך להחליף (הכתובת הבסיסית מוגדרת ב-RoutingRepository).
 */
interface OsrmApi {
    @GET("route/v1/driving/{coordinates}")
    suspend fun route(
        @Path("coordinates") coordinates: String,
        @Query("overview") overview: String = "full",
        @Query("geometries") geometries: String = "geojson",
        @Query("steps") steps: Boolean = true
    ): OsrmRouteResponse
}
