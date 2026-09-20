package com.future.navigation.data.siri

import com.future.navigation.data.backend.RemoteKeys

/**
 * הגדרות שירות ה-SIRI (Service Interface for Real Time Information) של משרד
 * התחבורה - מיקומי GPS/תחזיות הגעה בזמן אמת של האוטובוסים, בהמשך ל-GTFS
 * הסטטי הקיים (ר' data/gtfs/GtfsConfig.kt).
 *
 * בזמן כתיבת הקוד הזה אומת ידנית שהמארח siri.motrealtime.co.il:8081 חי
 * ומריץ שירות SOAP אמיתי (GlassFish, HTTP GET אמיתי לנתיב /Siri/SiriServices.asmx
 * החזיר 404 - כלומר השרת קיים ומגיב, אבל הנתיב/הגוף המדויק של הבקשה לא
 * אומתו בפועל (זה דורש מפתח רשום, שעוד לא סופק). המבנה כאן עוקב אחרי תקן
 * ה-SIRI 2.0 הרשמי (OASIS, StopMonitoringRequest) - ר' SiriRequestBuilder.
 * ה-base URL ומפתח ה-API מגיעים מ-local.properties (SIRI_BASE_URL/SIRI_API_KEY),
 * לא כתובים בקוד - כדי שיהיה קל לתקן אותם ברגע שמגיע מייל האישור מהרישום
 * מול משרד התחבורה, בלי לגעת בקוד.
 */
object SiriConfig {
    /** מ-Remote Config כשיש Firebase, אחרת מ-local.properties כמו קודם. */
    val baseUrl: String get() = RemoteKeys.siriBaseUrl
    val apiKey: String get() = RemoteKeys.siriApiKey

    /** דרך ה-proxy הכתובת והמפתח יושבים בשרת, ולכן אין צורך בהם על המכשיר. */
    val isConfigured: Boolean
        get() = RemoteKeys.useProxy || (baseUrl.isNotBlank() && apiKey.isNotBlank())
}
