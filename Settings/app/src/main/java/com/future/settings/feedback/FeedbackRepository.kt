package com.future.settings.feedback

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.future.settings.futureserver.FutureServerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * משוב במכשיר: הכנה (איסוף גרסה ולוג, סינון), שליחה לשרת FutureOS, תור offline, והודעות חוזרות.
 *
 * הכתובת והטוקן מגיעים מ-FutureServerRegistration (אותו רישום שמשמש את הניווט). כל עוד אין כתובת שרת
 * בבנייה (ro.futureos.api_base / future.api.base ריקים) המשוב נשמר בתור ויישלח כשהשרת יוגדר.
 * בלי GMS: אין push. ההודעות ("בטיפול", "תוקן בגרסה X") נמשכות כשנכנסים למסך המשוב.
 */
object FeedbackRepository {

    private const val PREFS = "feedback_store"
    private const val KEY_OUTBOX = "outbox"
    private const val KEY_HISTORY = "history"
    private const val KEY_LAST_INCLUDE_LOG = "last_include_log"
    private const val TIMEOUT_MS = 15_000
    private const val MAX_HISTORY = 30
    private const val LOG_LINES = 400

    /** "מערכת כללית" - משוב שלא שייך לאפליקציה מסוימת. */
    const val SYSTEM_PKG = "com.future.system"

    data class AppChoice(val pkg: String, val label: String, val version: String?)

    data class HistoryItem(
        val clientId: String,
        val type: FeedbackType,
        val appLabel: String,
        val preview: String,
        val createdAt: Long,
        val status: String,
        val message: String?,
    )

    enum class SubmitResult { Sent, Queued, Rejected }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun lastIncludeLog(context: Context): Boolean = prefs(context).getBoolean(KEY_LAST_INCLUDE_LOG, false)

    /** אפליקציות FutureOS שאפשר לדווח עליהן, ממוינות לפי שם, עם "מערכת כללית" ראשונה. */
    fun appChoices(context: Context): List<AppChoice> {
        val pm = context.packageManager
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(launcher, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it.startsWith("com.future.") }
            .mapNotNull { pkg -> appChoice(context, pkg) }
            .sortedBy { it.label }
        return listOf(AppChoice(SYSTEM_PKG, "מערכת כללית", null)) + apps
    }

    fun appChoice(context: Context, pkg: String): AppChoice? {
        if (pkg == SYSTEM_PKG) return AppChoice(SYSTEM_PKG, "מערכת כללית", null)
        return try {
            val pm = context.packageManager
            val info = pm.getPackageInfo(pkg, 0)
            val label = info.applicationInfo?.loadLabel(pm)?.toString() ?: pkg
            AppChoice(pkg, label, info.versionName)
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /** האם אפשר לצרף לוג של האפליקציה שנבחרה. בלי READ_LOGS (לפני ה-AOSP) logcat מחזיר רק את Settings עצמה. */
    fun canAttachLog(context: Context, pkg: String): Boolean =
        pkg == context.packageName || hasReadLogs(context)

    private fun hasReadLogs(context: Context) =
        context.checkSelfPermission(Manifest.permission.READ_LOGS) == PackageManager.PERMISSION_GRANTED

    /** מכין את המשוב לתצוגה המקדימה: סינון, גרסאות, ולוג (רק אם המשתמש ביקש). רץ על IO. */
    suspend fun prepare(
        context: Context,
        type: FeedbackType,
        app: AppChoice,
        text: String,
        includeLog: Boolean,
    ): FeedbackPayload = withContext(Dispatchers.IO) {
        prefs(context).edit().putBoolean(KEY_LAST_INCLUDE_LOG, includeLog).apply()
        val log = if (includeLog && canAttachLog(context, app.pkg)) collectLog(context, app.pkg) else null
        FeedbackPayload(
            clientId = UUID.randomUUID().toString(),
            type = type,
            appPkg = app.pkg,
            appLabel = app.label,
            appVersion = FeedbackPayload.cleanVersion(app.version),
            build = Build.DISPLAY.take(128),
            model = Build.MODEL,
            text = FeedbackRedactor.redact(text.trim()).take(FeedbackPayload.MAX_TEXT),
            log = log?.let { FeedbackRedactor.redact(it) }?.takeIf { it.isNotBlank() },
        )
    }

    private fun collectLog(context: Context, pkg: String): String? = try {
        val cmd = mutableListOf("logcat", "-d", "-t", LOG_LINES.toString(), "-v", "threadtime")
        if (pkg != context.packageName) pidOf(pkg)?.let { cmd += "--pid=$it" }
        val process = ProcessBuilder(cmd).redirectErrorStream(true).start()
        val text = process.inputStream.bufferedReader().use { it.readText() }
        process.waitFor()
        FeedbackPayload.tailLog(text)
    } catch (e: Exception) {
        null
    }

    private fun pidOf(pkg: String): String? = try {
        val p = ProcessBuilder("pidof", pkg).start()
        p.inputStream.bufferedReader().use { it.readText() }.trim().split(' ').firstOrNull()?.takeIf { it.all(Char::isDigit) && it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    /** שולח עכשיו אם אפשר; אחרת שומר בתור. בכל מקרה נרשם בהיסטוריה ("המשובים שלי"). */
    suspend fun submit(context: Context, payload: FeedbackPayload): SubmitResult = withContext(Dispatchers.IO) {
        addHistory(context, payload)
        enqueue(context, payload)
        val result = sendOne(context, payload.clientId, payload.toJson())
        when (result) {
            SubmitResult.Sent, SubmitResult.Rejected -> dequeue(context, payload.clientId)
            SubmitResult.Queued -> FutureServerRegistration.registerInBackgroundIfNeeded(context)
        }
        result
    }

    /** מנסה לשלוח את כל מה שבתור. נקרא בכל כניסה למסך המשוב. מחזיר כמה נשלחו. */
    suspend fun flushOutbox(context: Context): Int = withContext(Dispatchers.IO) {
        val outbox = readArray(context, KEY_OUTBOX)
        if (outbox.length() == 0) return@withContext 0
        FutureServerRegistration.registerInBackgroundIfNeeded(context)
        var sent = 0
        for (i in 0 until outbox.length()) {
            val item = outbox.getJSONObject(i)
            val clientId = item.getString("client_id")
            when (sendOne(context, clientId, item.getString("json"))) {
                SubmitResult.Sent -> { dequeue(context, clientId); sent++ }
                SubmitResult.Rejected -> dequeue(context, clientId)
                SubmitResult.Queued -> return@withContext sent // השרת לא זמין: לא ממשיכים לנסות את השאר עכשיו
            }
        }
        sent
    }

    fun queuedCount(context: Context): Int = readArray(context, KEY_OUTBOX).length()

    private fun sendOne(context: Context, clientId: String, json: String): SubmitResult {
        val base = FutureServerRegistration.resolvedBaseUrl() ?: return SubmitResult.Queued
        val (deviceId, token) = FutureServerRegistration.loadCredentials(context) ?: return SubmitResult.Queued
        return try {
            val conn = (URL("$base/feedback").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("X-Device-Id", deviceId)
            }
            try {
                conn.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                when (conn.responseCode) {
                    in 200..299 -> {
                        val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                        val status = runCatching { JSONObject(body).optString("status", "new") }.getOrDefault("new")
                        updateHistory(context, clientId, status = status)
                        SubmitResult.Sent
                    }
                    // 400/413: המשוב עצמו לא תקין, ניסיון חוזר לא יעזור
                    400, 413 -> {
                        updateHistory(context, clientId, status = "rejected")
                        SubmitResult.Rejected
                    }
                    else -> SubmitResult.Queued // 401, 429, 5xx: ננסה שוב בפעם הבאה
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            SubmitResult.Queued
        }
    }

    /**
     * מושך הודעות חדשות מהשרת ומעדכן את ההיסטוריה. ההודעות מאושרות (ack) רק אחרי שנשמרו מקומית,
     * כדי שהודעה לא תאבד אם הבקשה נקטעה באמצע.
     */
    suspend fun pollInbox(context: Context): Int = withContext(Dispatchers.IO) {
        val base = FutureServerRegistration.resolvedBaseUrl() ?: return@withContext 0
        val (deviceId, token) = FutureServerRegistration.loadCredentials(context) ?: return@withContext 0
        val messages = getJson("$base/inbox", deviceId, token)?.optJSONArray("messages") ?: return@withContext 0
        if (messages.length() == 0) return@withContext 0

        val serverIdToClient = getJson("$base/feedback", deviceId, token)?.optJSONArray("feedback")?.let { list ->
            (0 until list.length()).associate { list.getJSONObject(it).let { f -> f.getString("id") to f.optString("client_id") } }
        } ?: emptyMap()

        val ackIds = mutableListOf<String>()
        for (i in 0 until messages.length()) {
            val m = messages.getJSONObject(i)
            val clientId = serverIdToClient[m.optString("feedback_id")]
            if (clientId != null) {
                val status = when (m.optString("kind")) {
                    "in_progress" -> "in_progress"
                    "fixed" -> "fixed"
                    "needs_info" -> "needs_info"
                    else -> null
                }
                updateHistory(context, clientId, status = status, message = m.optString("text_he").take(300))
            }
            ackIds += m.getString("id")
        }
        getJson("$base/inbox?ack=${ackIds.joinToString(",")}", deviceId, token)
        ackIds.size
    }

    private fun getJson(url: String, deviceId: String, token: String): JSONObject? = try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Device-Id", deviceId)
        }
        try {
            if (conn.responseCode !in 200..299) null
            else JSONObject(conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) })
        } finally {
            conn.disconnect()
        }
    } catch (e: Exception) {
        null
    }

    fun history(context: Context): List<HistoryItem> {
        val arr = readArray(context, KEY_HISTORY)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            HistoryItem(
                clientId = o.getString("client_id"),
                type = FeedbackType.entries.firstOrNull { it.wire == o.optString("type") } ?: FeedbackType.Other,
                appLabel = o.optString("app_label"),
                preview = o.optString("preview"),
                createdAt = o.optLong("created_at"),
                status = o.optString("status", "queued"),
                message = o.optString("message").takeIf { it.isNotEmpty() },
            )
        }
    }

    fun statusLabel(status: String): String = when (status) {
        "queued" -> "ממתין לשליחה"
        "new", "triaged" -> "נשלח"
        "in_progress" -> "בטיפול"
        "needs_info" -> "צריך עוד פרטים"
        "wontfix" -> "נסגר"
        "fixed" -> "תוקן"
        "rejected" -> "לא נשלח"
        else -> "נשלח"
    }

    // ---- אחסון מקומי (SharedPreferences פרטי, מוחרג מגיבוי) ----

    private fun readArray(context: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(context).getString(key, "[]")) }.getOrDefault(JSONArray())

    private fun writeArray(context: Context, key: String, arr: JSONArray) {
        prefs(context).edit().putString(key, arr.toString()).commit()
    }

    @Synchronized
    private fun enqueue(context: Context, payload: FeedbackPayload) {
        val arr = readArray(context, KEY_OUTBOX)
        arr.put(JSONObject().put("client_id", payload.clientId).put("json", payload.toJson()))
        writeArray(context, KEY_OUTBOX, arr)
    }

    @Synchronized
    private fun dequeue(context: Context, clientId: String) {
        val arr = readArray(context, KEY_OUTBOX)
        val kept = JSONArray()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("client_id") != clientId) kept.put(arr.get(i))
        writeArray(context, KEY_OUTBOX, kept)
    }

    @Synchronized
    private fun addHistory(context: Context, payload: FeedbackPayload) {
        val arr = readArray(context, KEY_HISTORY)
        val next = JSONArray().put(
            JSONObject()
                .put("client_id", payload.clientId)
                .put("type", payload.type.wire)
                .put("app_label", payload.appLabel)
                .put("preview", payload.text.take(80))
                .put("created_at", System.currentTimeMillis())
                .put("status", "queued"),
        )
        for (i in 0 until minOf(arr.length(), MAX_HISTORY - 1)) next.put(arr.get(i))
        writeArray(context, KEY_HISTORY, next)
    }

    @Synchronized
    private fun updateHistory(context: Context, clientId: String, status: String? = null, message: String? = null) {
        val arr = readArray(context, KEY_HISTORY)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("client_id") == clientId) {
                if (status != null) o.put("status", status)
                if (!message.isNullOrBlank()) o.put("message", message)
            }
        }
        writeArray(context, KEY_HISTORY, arr)
    }
}
