package com.future.navigation.data.backend

import android.content.Context
import android.net.Uri
import android.util.Log
import com.future.navigation.BuildConfig
import com.future.navigation.data.common.LatLng
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * שרת FutureOS - הנתיב הראשון לניתוב ולתחבורה בזמן אמת. השרת מחזיק את מפתחות
 * HERE ו-SIRI, והמכשיר מדבר איתו עם טוקן שמגיע מ-com.future.settings.
 *
 * כתובת השרת: ro.futureos.api_base ב-build.prop של ה-AOSP, ואם היא ריקה -
 * BuildConfig.FUTURE_API_BASE (מ-local.properties, future.api.base).
 *
 * הטוקן והמזהה נקראים מ-ContentProvider בכתובת content://com.future.settings.futureserver/credentials
 * עם העמודות token ו-device_id. אם הספק לא קיים, החזיר ריק, זרק SecurityException
 * או כל שגיאה אחרת - זה null, והאפליקציה נופלת לנתיב הקיים (HERE/SIRI ישירות
 * או דרך ה-Firebase). לעולם לא מדפיסים את הטוקן ללוג.
 *
 * כל פונקציה מחזירה null בכל כשל (כולל 503 ו-429) ולא זורקת - הקורא ממשיך לנתיב הבא.
 */
object FutureServer {

    private const val TAG = "FutureServer"
    private const val PROP_API_BASE = "ro.futureos.api_base"
    private const val AUTHORITY = "com.future.settings.futureserver"
    private const val CREDENTIALS_PATH = "credentials"
    private const val COL_TOKEN = "token"
    private const val COL_DEVICE_ID = "device_id"

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    @Volatile
    private var appContext: Context? = null

    /** נקרא פעם אחת מ-MainActivity.onCreate. בלי זה אין גישה לספק, והשרת לא זמין. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** כתובת הבסיס של השרת, בלי / בסוף. ריקה = השרת לא מוגדר בבנייה הזו. */
    fun baseUrl(): String = resolveBaseUrl(systemProperty(PROP_API_BASE), BuildConfig.FUTURE_API_BASE)

    /** השרת זמין רק כשיש כתובת וגם טוקן. */
    val isAvailable: Boolean
        get() = baseUrl().isNotEmpty() && token() != null

    /** הטוקן מהספק, או null אם אין ספק / אין טוקן / הקריאה נכשלה. */
    fun token(): String? = credentials()?.token

    /** המזהה של המכשיר מהספק, או null. */
    fun deviceId(): String? = credentials()?.deviceId

    /**
     * POST {baseUrl}/nav/route עם {from:{lat,lng}, to:{lat,lng}, mode:"car"}.
     * מחזיר את גוף ה-JSON של HERE כמו שהוא, לפירוק ב-HereRouteResponse.
     */
    suspend fun routeJson(from: LatLng, to: LatLng): String? {
        val body = buildJsonObject {
            put("from", buildJsonObject { put("lat", from.lat); put("lng", from.lon) })
            put("to", buildJsonObject { put("lat", to.lat); put("lng", to.lon) })
            put("mode", "car")
        }.toString()
        return post("/nav/route", body)
    }

    /**
     * POST {baseUrl}/nav/siri/stop עם {stopId, lineRef?}. מחזיר את גוף ה-XML של
     * SIRI SM כמו שהוא, לפירוק ב-SiriXml.
     */
    suspend fun siriStopMonitoringXml(stopId: String, lineRef: String?): String? {
        val body = buildJsonObject {
            put("stopId", stopId)
            if (lineRef != null) put("lineRef", lineRef)
        }.toString()
        return post("/nav/siri/stop", body)
    }

    /**
     * שליחת בקשה אחת לשרת. על 401 קוראים את הטוקן מחדש מהספק (אולי הוחלף) ומנסים
     * פעם אחת נוספת בלבד. כל כשל אחר - 503, 429, רשת, ספק חסר - מחזיר null.
     */
    private suspend fun post(path: String, jsonBody: String): String? = withContext(Dispatchers.IO) {
        val base = baseUrl()
        if (base.isEmpty()) return@withContext null

        for (attempt in 0..1) {
            val creds = credentials() ?: return@withContext null
            val request = Request.Builder()
                .url(base + path)
                .header("Authorization", "Bearer ${creds.token}")
                .header("X-Device-Id", creds.deviceId)
                .post(jsonBody.toRequestBody(jsonType))
                .build()

            val outcome = try {
                NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                    when {
                        response.isSuccessful -> Outcome.Body(response.body?.string())
                        response.code == 401 && attempt == 0 -> Outcome.Unauthorized
                        else -> {
                            Log.i(TAG, "$path: HTTP ${response.code}")
                            Outcome.Failed
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "$path: ${e.javaClass.simpleName}")
                Outcome.Failed
            }

            when (outcome) {
                is Outcome.Body -> return@withContext outcome.text?.takeIf { it.isNotBlank() }
                Outcome.Unauthorized -> continue
                Outcome.Failed -> return@withContext null
            }
        }
        null
    }

    private sealed interface Outcome {
        data class Body(val text: String?) : Outcome
        data object Unauthorized : Outcome
        data object Failed : Outcome
    }

    private data class Credentials(val token: String, val deviceId: String)

    /** קורא את הטוקן והמזהה מהספק. כל כשל = null, בלי לוג של ערכים. */
    private fun credentials(): Credentials? {
        val context = appContext ?: return null
        return try {
            val uri = Uri.parse("content://$AUTHORITY/$CREDENTIALS_PATH")
            context.contentResolver
                .query(uri, arrayOf(COL_TOKEN, COL_DEVICE_ID), null, null, null)
                ?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val token = cursor.getString(cursor.getColumnIndexOrThrow(COL_TOKEN))
                    val deviceId = cursor.getString(cursor.getColumnIndexOrThrow(COL_DEVICE_ID))
                    if (token.isNullOrBlank() || deviceId.isNullOrBlank()) null
                    else Credentials(token, deviceId)
                }
        } catch (e: Exception) {
            null
        }
    }

    /** קורא system property בלי תלות בממשק הסודי: reflection על android.os.SystemProperties. */
    private fun systemProperty(key: String): String = try {
        val cls = Class.forName("android.os.SystemProperties")
        (cls.getMethod("get", String::class.java).invoke(null, key) as? String).orEmpty()
    } catch (e: Exception) {
        ""
    }

    internal fun resolveBaseUrl(property: String, buildConfigValue: String): String {
        val chosen = property.trim().ifEmpty { buildConfigValue.trim() }
        return chosen.trimEnd('/')
    }
}
