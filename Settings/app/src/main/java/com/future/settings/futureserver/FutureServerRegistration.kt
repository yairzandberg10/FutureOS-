package com.future.settings.futureserver

import android.content.Context
import android.os.Build
import com.future.settings.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * רישום המכשיר מול השרת ברקע: POST {base}/register, ושמירת {device_id, token} ב-SharedPreferences פרטיים.
 * כללים: thread יחיד, single-flight (לא רץ יותר מניסיון אחד בכל רגע), ניסיון אחד לכל 15 דקות.
 * אסור לרשום את הטוקן ביומן או ב-Intent extras. בשגיאה לא עושים כלום.
 */
object FutureServerRegistration {
    const val PREFS_NAME = "futureserver_credentials"
    const val KEY_DEVICE_ID = "device_id"
    const val KEY_TOKEN = "token"
    private const val KEY_LAST_ATTEMPT_MS = "last_attempt_ms"
    private const val SYSTEM_PROP_API_BASE = "ro.futureos.api_base"
    private const val TIMEOUT_MS = 10_000

    // thread יחיד: הרישומים רצים אחד אחרי השני, ולעולם לא במקביל
    private val executor: Executor = Executors.newSingleThreadExecutor { task ->
        Thread(task, "FutureServerRegister").apply { isDaemon = true }
    }
    private val inFlight = AtomicBoolean(false)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** הטוקן השמור כ-(device_id, token), או null אם עוד לא נרשמנו. */
    fun loadCredentials(context: Context): Pair<String, String>? {
        val p = prefs(context)
        val deviceId = p.getString(KEY_DEVICE_ID, null)
        val token = p.getString(KEY_TOKEN, null)
        if (deviceId.isNullOrEmpty() || token.isNullOrEmpty()) return null
        return deviceId to token
    }

    /**
     * מתחיל רישום ברקע אם צריך. חוזר מיד, בלי רשת על ה-thread של הקורא.
     * בלי כתובת שרת - לא עושה כלום, וגם לא מסמן ניסיון.
     */
    fun registerInBackgroundIfNeeded(context: Context) {
        val appContext = context.applicationContext
        if (loadCredentials(appContext) != null) return

        val base = FutureServerPolicy.resolveBaseUrl(
            readSystemProperty(SYSTEM_PROP_API_BASE),
            BuildConfig.FUTURE_API_BASE,
        ) ?: return

        // single-flight: אם רישום כבר רץ או בתור, מדלגים
        if (!inFlight.compareAndSet(false, true)) return

        val p = prefs(appContext)
        val now = System.currentTimeMillis()
        if (!FutureServerPolicy.canAttempt(now, p.getLong(KEY_LAST_ATTEMPT_MS, 0L))) {
            inFlight.set(false)
            return
        }
        p.edit().putLong(KEY_LAST_ATTEMPT_MS, now).apply()

        try {
            executor.execute {
                try {
                    register(appContext, base)
                } finally {
                    inFlight.set(false)
                }
            }
        } catch (e: RejectedExecutionException) {
            inFlight.set(false)
        }
    }

    private fun register(context: Context, base: String) {
        try {
            val body = JSONObject()
                .put("model", Build.MODEL)
                .put("build", Build.DISPLAY)
                .toString()
            val conn = (URL("$base/register").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
            }
            try {
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                if (conn.responseCode !in 200..299) return
                val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                val json = JSONObject(text)
                val deviceId = json.optString("device_id").trim()
                val token = json.optString("token").trim()
                if (deviceId.isEmpty() || token.isEmpty()) return
                prefs(context).edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .putString(KEY_TOKEN, token)
                    .commit()
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            // שגיאה = לא עושים כלום. בכוונה בלי לוג: גם שגיאה עלולה להכיל חלקים מהתשובה (הטוקן).
        }
    }

    /** קורא מאפיין מערכת דרך reflection (android.os.SystemProperties); אם לא נגיש - null. */
    private fun readSystemProperty(key: String): String? = try {
        Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String
    } catch (e: Exception) {
        null
    }
}
