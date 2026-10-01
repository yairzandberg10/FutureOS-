package com.future.navigation.data.backend

import android.util.Log
import com.future.navigation.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.remoteConfigSettings

/**
 * מפתחות ה-API מגיעים מ-Firebase Remote Config במקום להיות מוטבעים ב-APK.
 *
 * למה בכלל, אם יש proxy: הנתיב הרגיל הוא ש-Cloud Functions מחזיקות את
 * המפתחות והמכשיר לא רואה אותם בכלל (ר' [NavigationFunctions]). Remote Config
 * הוא הנתיב השני - כשה-proxy כבוי או נופל, הריפוזיטורי חוזר לקרוא ישירות
 * לשירות, וגם אז עדיף מפתח שאפשר להחליף מרחוק בלי בנייה מחדש ובלי לשלוח
 * גרסה חדשה למכשיר, על פני מפתח שנצרב ב-BuildConfig.
 *
 * ברירות המחדל הן בדיוק הערכים הקיימים מ-local.properties, כך שבלי Firebase
 * (או לפני ה-fetch הראשון) ההתנהגות זהה למה שהיה.
 */
object RemoteKeys {

    private const val TAG = "RemoteKeys"

    const val KEY_HERE_API_KEY = "here_api_key"
    const val KEY_SIRI_BASE_URL = "siri_base_url"
    const val KEY_SIRI_API_KEY = "siri_api_key"
    const val KEY_USE_PROXY = "use_functions_proxy"
    const val KEY_TRANSIT_SOURCE_SERVER = "transit_data_from_server"

    private val config: FirebaseRemoteConfig?
        get() = if (FirebaseBackend.isAvailable) {
            runCatching { FirebaseRemoteConfig.getInstance() }.getOrNull()
        } else null

    /**
     * נקרא פעם אחת בעלייה, אחרי [FirebaseBackend.init]. לא חוסם: עד
     * שה-fetch חוזר, [hereApiKey] וחבריו מחזירים את ברירות המחדל המקומיות.
     */
    fun refresh() {
        val remote = config ?: return
        runCatching {
            remote.setConfigSettingsAsync(
                remoteConfigSettings {
                    // שעה - מספיק תכוף כדי להחליף מפתח שנשרף באותו יום, ורחוק
                    // מספיק מהמכסה של Remote Config.
                    minimumFetchIntervalInSeconds = 3600
                }
            )
            remote.setDefaultsAsync(
                mapOf(
                    KEY_HERE_API_KEY to BuildConfig.HERE_API_KEY,
                    KEY_SIRI_BASE_URL to BuildConfig.SIRI_BASE_URL,
                    KEY_SIRI_API_KEY to BuildConfig.SIRI_API_KEY,
                    KEY_USE_PROXY to true,
                    KEY_TRANSIT_SOURCE_SERVER to true,
                )
            )
            remote.fetchAndActivate().addOnFailureListener { Log.w(TAG, "remote config fetch failed", it) }
        }.onFailure { Log.w(TAG, "remote config unavailable", it) }
    }

    private fun string(key: String, localDefault: String): String {
        val remote = config ?: return localDefault
        val value = runCatching { remote.getString(key) }.getOrNull()
        return if (value.isNullOrBlank()) localDefault else value
    }

    private fun boolean(key: String, localDefault: Boolean): Boolean {
        val remote = config ?: return localDefault
        return runCatching { remote.getBoolean(key) }.getOrDefault(localDefault)
    }

    val hereApiKey: String get() = string(KEY_HERE_API_KEY, BuildConfig.HERE_API_KEY)
    val siriBaseUrl: String get() = string(KEY_SIRI_BASE_URL, BuildConfig.SIRI_BASE_URL)
    val siriApiKey: String get() = string(KEY_SIRI_API_KEY, BuildConfig.SIRI_API_KEY)

    /** כיבוי חירום מרחוק ל-proxy (למשל אם המכסה של Cloud Functions נגמרה). */
    val useProxy: Boolean get() = FirebaseBackend.isAvailable && boolean(KEY_USE_PROXY, true)

    /** כיבוי חירום מרחוק לשליפת נתוני התחבורה מהשרת (חזרה לייבוא GTFS מקומי). */
    val transitDataFromServer: Boolean
        get() = FirebaseBackend.isAvailable && boolean(KEY_TRANSIT_SOURCE_SERVER, true)
}
