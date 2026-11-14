package com.future.navigation.data.backend

import android.content.Context
import com.future.navigation.BuildConfig
import kotlinx.coroutines.launch

/**
 * מפתחות ה-API והמתגים המרוחקים מגיעים ממסמך אחד ב-Firestore,
 * `app_config/navigation`, במקום להיות מוטבעים ב-APK.
 *
 * (קודם זה היה Firebase Remote Config, אבל ה-SDK שלו דורש Google Play
 * Services, שאין במכשיר. מסמך Firestore נקרא ב-REST כמו כל השאר, וחוקי
 * הגישה ב-firestore.rules מאפשרים לקרוא רק אותו.)
 *
 * למה בכלל, אם יש proxy: הנתיב הרגיל הוא ש-Cloud Functions מחזיקות את
 * המפתחות והמכשיר לא רואה אותם בכלל (ר' [NavigationFunctions]). המסמך הזה
 * הוא הנתיב השני - כשה-proxy כבוי או נופל, הריפוזיטורי חוזר לקרוא ישירות
 * לשירות, וגם אז עדיף מפתח שאפשר להחליף מרחוק בלי בנייה מחדש ובלי לשלוח
 * גרסה חדשה למכשיר, על פני מפתח שנצרב ב-BuildConfig.
 *
 * ברירות המחדל הן בדיוק הערכים הקיימים מ-local.properties, כך שבלי Firebase
 * (או לפני הקריאה הראשונה) ההתנהגות זהה למה שהיה. הערך האחרון שנקרא נשמר
 * במכשיר, כדי שגם בלי רשת יחול המפתח המעודכן.
 */
object RemoteKeys {

    private const val PREFS = "remote_keys"
    private const val CONFIG_DOCUMENT = "app_config/navigation"

    /** מספיק תכוף כדי להחליף מפתח שנשרף באותו יום, ולא קריאה בכל פתיחה. */
    private const val MIN_FETCH_INTERVAL_MS = 60 * 60 * 1000L
    private const val KEY_FETCHED_AT = "fetched_at"

    const val KEY_HERE_API_KEY = "here_api_key"
    const val KEY_SIRI_BASE_URL = "siri_base_url"
    const val KEY_SIRI_API_KEY = "siri_api_key"
    const val KEY_USE_PROXY = "use_functions_proxy"
    const val KEY_TRANSIT_SOURCE_SERVER = "transit_data_from_server"

    private val prefs
        get() = FirebaseBackend.appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * נקרא פעם אחת בעלייה, אחרי [FirebaseBackend.init]. לא חוסם: עד
     * שהקריאה חוזרת, [hereApiKey] וחבריו מחזירים את מה שנשמר או את ברירות
     * המחדל המקומיות.
     */
    fun refresh() {
        if (!FirebaseBackend.isAvailable) return
        val stored = prefs ?: return
        val last = stored.getLong(KEY_FETCHED_AT, 0L)
        if (System.currentTimeMillis() - last < MIN_FETCH_INTERVAL_MS) return

        FirebaseBackend.scope.launch {
            val document = FirebaseBackend.firestoreDocument(CONFIG_DOCUMENT) ?: return@launch
            stored.edit().apply {
                listOf(KEY_HERE_API_KEY, KEY_SIRI_BASE_URL, KEY_SIRI_API_KEY).forEach { key ->
                    val value = document.string(key)
                    if (value.isNullOrBlank()) remove(key) else putString(key, value)
                }
                listOf(KEY_USE_PROXY, KEY_TRANSIT_SOURCE_SERVER).forEach { key ->
                    val value = document.boolean(key)
                    if (value == null) remove(key) else putBoolean(key, value)
                }
                putLong(KEY_FETCHED_AT, System.currentTimeMillis())
            }.apply()
        }
    }

    private fun string(key: String, localDefault: String): String {
        val value = prefs?.getString(key, null)
        return if (value.isNullOrBlank()) localDefault else value
    }

    private fun boolean(key: String, localDefault: Boolean): Boolean {
        val stored = prefs ?: return localDefault
        return if (stored.contains(key)) stored.getBoolean(key, localDefault) else localDefault
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
