package com.future.navigation.data.routing

import com.future.navigation.data.backend.RemoteKeys

/**
 * הגדרות HERE Technologies Routing & Traffic API (v8). כתובת ה-API אומתה
 * ידנית מול השרת האמיתי בזמן כתיבת הקוד (GET אמיתי ל-router.hereapi.com/v8/routes
 * החזיר 401 - "unauthorized", כלומר השרת קיים ומחכה למפתח, לא כתובת מומצאת).
 *
 * המפתח עצמו (HERE_API_KEY) לא נכלל בקוד - הוא מגיע דרך local.properties
 * (קובץ שכבר ב-.gitignore בכל הפרויקט) ונחשף כ-BuildConfig field, כדי שלא
 * יגיע בטעות ל-git. ר' app/build.gradle.kts לאופן הקריאה מהקובץ.
 */
object HereConfig {
    const val BASE_URL = "https://router.hereapi.com/"
    const val TRAFFIC_BASE_URL = "https://data.traffic.hereapi.com/"

    /** מ-Remote Config כשיש Firebase, אחרת מ-local.properties כמו קודם. */
    val apiKey: String get() = RemoteKeys.hereApiKey

    /**
     * דרך ה-proxy המכשיר לא צריך מפתח בכלל - המפתח יושב בסוד של
     * Cloud Functions - ולכן גם בלי HERE_API_KEY מקומי יש ניתוב.
     */
    val isConfigured: Boolean get() = RemoteKeys.useProxy || apiKey.isNotBlank()
}
