package com.future.settings.update

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import com.future.settings.BuildConfig
import com.future.settings.futureserver.FutureServerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/**
 * מנוע העדכונים במכשיר (לפני ה-AOSP): עדכון של אפליקציות com.future.* אחת-אחת, כ-APK.
 * מסמך העיצוב: docs/update-feedback-design.md סעיף 2.5 (אושר 2026-10-16).
 *
 * הזרימה: check() -> הצעות מאומתות נשמרות -> install(offer): הורדה, בדיקת hash וגודל, בדיקת חבילה, גרסה
 * וחתימה -> PackageInstaller. אנדרואיד מציג מסך אישור התקנה (Settings אינה אפליקציית מערכת). ב-AOSP,
 * כשהיא תהיה privileged עם INSTALL_PACKAGES, אותה קריאה תתקין בלי מסך אישור.
 *
 * אין התקנה אוטומטית ברקע: הרקע רק בודק ומציג התראה. כל התקנה מתחילה בלחיצה של המשתמש.
 */
object UpdateRepository {

    private const val PREFS = "update_store"
    private const val KEY_OFFERS = "offers"
    private const val KEY_LAST_CHECK = "last_check"
    private const val KEY_LAST_RESULT = "last_result"
    private const val KEY_PENDING = "pending"
    private const val KEY_EVENTS = "event_outbox"
    private const val KEY_AUTO = "auto_check"
    private const val KEY_NOTIFIED = "notified_releases"
    private const val TIMEOUT_MS = 20_000
    const val ACTION_INSTALL_STATUS = "com.future.settings.update.INSTALL_STATUS"
    const val EXTRA_RELEASE_ID = "release_id"

    sealed interface CheckResult {
        /** הבנייה הזו בלי כתובת שרת או בלי מפתח ציבורי לעדכונים. */
        data object NotConfigured : CheckResult
        /** עוד לא נרשמנו מול השרת (הרישום רץ ברקע). */
        data object NotRegistered : CheckResult
        data object Failed : CheckResult
        data class Ok(val offers: List<UpdatePolicy.Offer>) : CheckResult
    }

    /** מצב של עדכון אחד במסך: לא נשמר, נבנה מחדש מה-pending אחרי הפעלה מחדש. */
    sealed interface Progress {
        data class Downloading(val percent: Int) : Progress
        data object Verifying : Progress
        data object WaitingForUser : Progress
        data object Installed : Progress
        data class Failed(val code: String) : Progress
    }

    private val _progress = MutableStateFlow<Map<String, Progress>>(emptyMap())
    val progress: StateFlow<Map<String, Progress>> = _progress.asStateFlow()

    private fun setProgress(releaseId: String, p: Progress?) {
        _progress.value = _progress.value.toMutableMap().apply { if (p == null) remove(releaseId) else put(releaseId, p) }
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isConfigured(): Boolean =
        FutureServerRegistration.resolvedBaseUrl() != null && BuildConfig.FUTURE_UPDATE_PUBKEY.isNotBlank()

    fun autoCheckEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO, true)
    fun setAutoCheck(context: Context, on: Boolean) = prefs(context).edit().putBoolean(KEY_AUTO, on).apply()
    fun lastCheck(context: Context): Long = prefs(context).getLong(KEY_LAST_CHECK, 0L)
    fun lastCheckFailed(context: Context): Boolean = prefs(context).getString(KEY_LAST_RESULT, "") == "failed"

    // ---------- מה מותקן ----------

    /** כל חבילות com.future.* שמותקנות וגלויות ל-Settings (ר' <queries> במניפסט). */
    fun installedFuturePackages(context: Context): Map<String, Long> =
        context.packageManager.getInstalledPackages(0)
            .filter { it.packageName.startsWith("com.future.") }
            .associate { it.packageName to it.longVersionCode }

    private fun installedVersion(context: Context, pkg: String): Long? = try {
        context.packageManager.getPackageInfo(pkg, 0).longVersionCode
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }

    fun appLabel(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationInfo(pkg, 0).loadLabel(pm).toString()
    } catch (e: Exception) {
        pkg
    }

    // ---------- בדיקה ----------

    /** בודק מול השרת. רץ על IO. מעדכן את ההצעות השמורות רק כשהתשובה עברה אימות מלא. */
    suspend fun check(context: Context): CheckResult = withContext(Dispatchers.IO) {
        reconcilePending(context)
        flushEvents(context)
        val base = FutureServerRegistration.resolvedBaseUrl()
        if (base == null || BuildConfig.FUTURE_UPDATE_PUBKEY.isBlank()) return@withContext CheckResult.NotConfigured
        val creds = FutureServerRegistration.loadCredentials(context)
        if (creds == null) {
            FutureServerRegistration.registerInBackgroundIfNeeded(context)
            return@withContext CheckResult.NotRegistered
        }
        val (deviceId, token) = creds
        val installed = installedFuturePackages(context)
        val apps = JSONArray()
        installed.forEach { (pkg, vc) -> apps.put(JSONObject().put("pkg", pkg).put("version_code", vc)) }
        val body = JSONObject().put("apps", apps).put("kinds", JSONArray().put("apk")).put("build", Build.DISPLAY.take(128))

        val response = post("$base/update/check", deviceId, token, body.toString())
        val result = response?.let { parseSigned(it, deviceId, installed) }
        prefs(context).edit()
            .putLong(KEY_LAST_CHECK, System.currentTimeMillis())
            .putString(KEY_LAST_RESULT, if (result == null) "failed" else "ok")
            .apply()
        if (result == null) return@withContext CheckResult.Failed
        saveOffers(context, result)
        CheckResult.Ok(result)
    }

    /** מאמת חתימה, מכשיר ותוקף, ואז כל הצעה בנפרד. null = כל התשובה נדחתה. */
    private fun parseSigned(responseText: String, deviceId: String, installed: Map<String, Long>): List<UpdatePolicy.Offer>? = try {
        val outer = JSONObject(responseText)
        val manifestText = outer.getString("manifest")
        if (!UpdatePolicy.verifySignature(manifestText, outer.getString("sig"), BuildConfig.FUTURE_UPDATE_PUBKEY)) {
            null
        } else {
            val m = JSONObject(manifestText)
            if (m.optInt("v") != 1 ||
                !UpdatePolicy.manifestValid(m.getString("device_id"), deviceId, m.getLong("issued_at"), m.getLong("expires_at"), System.currentTimeMillis())
            ) {
                null
            } else {
                val list = m.getJSONArray("updates")
                (0 until list.length()).mapNotNull { i ->
                    val offer = offerFromJson(list.getJSONObject(i))
                    offer.takeIf { UpdatePolicy.checkOffer(it, installed[it.pkg]) == null }
                }
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun offerFromJson(o: JSONObject) = UpdatePolicy.Offer(
        releaseId = o.getString("release_id"),
        kind = o.getString("kind"),
        pkg = o.getString("pkg"),
        versionCode = o.getLong("version_code"),
        versionName = o.getString("version_name").take(64),
        size = o.getLong("size"),
        sha256 = o.getString("sha256"),
        notesHe = o.optString("notes_he").take(2000),
        path = o.getString("path"),
    )

    private fun offerToJson(o: UpdatePolicy.Offer) = JSONObject()
        .put("release_id", o.releaseId).put("kind", o.kind).put("pkg", o.pkg)
        .put("version_code", o.versionCode).put("version_name", o.versionName)
        .put("size", o.size).put("sha256", o.sha256).put("notes_he", o.notesHe).put("path", o.path)

    private fun saveOffers(context: Context, offers: List<UpdatePolicy.Offer>) {
        val arr = JSONArray()
        offers.forEach { arr.put(offerToJson(it)) }
        prefs(context).edit().putString(KEY_OFFERS, arr.toString()).commit()
    }

    /** ההצעות האחרונות שאומתו, בלי אלה שכבר הותקנו בינתיים. */
    fun offers(context: Context): List<UpdatePolicy.Offer> {
        val arr = runCatching { JSONArray(prefs(context).getString(KEY_OFFERS, "[]")) }.getOrDefault(JSONArray())
        return (0 until arr.length()).mapNotNull { i ->
            runCatching { offerFromJson(arr.getJSONObject(i)) }.getOrNull()
        }.filter { UpdatePolicy.checkOffer(it, installedVersion(context, it.pkg)) == null }
    }

    /** הצעות שעוד לא הוצגה עליהן התראה. מסמן אותן כ"הוצגו". */
    fun takeUnnotified(context: Context, offers: List<UpdatePolicy.Offer>): List<UpdatePolicy.Offer> {
        val p = prefs(context)
        val seen = p.getStringSet(KEY_NOTIFIED, emptySet()).orEmpty()
        val fresh = offers.filter { it.releaseId !in seen }
        if (fresh.isNotEmpty()) {
            // שומרים רק מזהים של הצעות שעדיין קיימות, כדי שהקבוצה לא תגדל לנצח
            p.edit().putStringSet(KEY_NOTIFIED, offers.map { it.releaseId }.toSet()).apply()
        }
        return fresh
    }

    // ---------- התקנה ----------

    /** Settings רשאית להתקין? אם לא, המסך מפנה להגדרה "התקנת אפליקציות לא מוכרות". */
    fun canInstall(context: Context) = context.packageManager.canRequestPackageInstalls()

    /** הורדה, אימות ושליחה ל-PackageInstaller. רץ על IO. מחזיר את המצב הסופי מבחינת Settings. */
    suspend fun install(context: Context, offer: UpdatePolicy.Offer): Progress = withContext(Dispatchers.IO) {
        val result = runInstall(context, offer)
        if (result is Progress.Failed) {
            queueEvent(context, offer.releaseId, "failed", result.code)
            flushEvents(context)
        }
        setProgress(offer.releaseId, result)
        result
    }

    private fun runInstall(context: Context, offer: UpdatePolicy.Offer): Progress {
        if (UpdatePolicy.checkOffer(offer, installedVersion(context, offer.pkg)) != null) return Progress.Failed("version")
        val base = FutureServerRegistration.resolvedBaseUrl() ?: return Progress.Failed("network")
        val (deviceId, token) = FutureServerRegistration.loadCredentials(context) ?: return Progress.Failed("network")
        val url = UpdatePolicy.downloadUrl(base, offer) ?: return Progress.Failed("package")
        if (!UpdatePolicy.transportAllowed(url, BuildConfig.DEBUG)) return Progress.Failed("network")

        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "${offer.releaseId}.apk")
        if (dir.usableSpace < offer.size * 2) return Progress.Failed("storage")

        setProgress(offer.releaseId, Progress.Downloading(0))
        val downloaded = download(url, deviceId, token, file, offer) { pct -> setProgress(offer.releaseId, Progress.Downloading(pct)) }
        if (downloaded != null) {
            file.delete()
            return Progress.Failed(downloaded)
        }
        queueEvent(context, offer.releaseId, "downloaded", null)

        setProgress(offer.releaseId, Progress.Verifying)
        verifyApk(context, file, offer)?.let { code ->
            file.delete()
            return Progress.Failed(code)
        }

        return try {
            commitSession(context, file, offer)
            markPending(context, offer)
            Progress.WaitingForUser
        } catch (e: Exception) {
            file.delete()
            Progress.Failed("install")
        }
    }

    /** מוריד בזרימה, עם hash תוך כדי ותקרת גודל. מחזיר null בהצלחה, או קוד שגיאה. */
    private fun download(url: String, deviceId: String, token: String, file: File, offer: UpdatePolicy.Offer, onPct: (Int) -> Unit): String? {
        return try {
            downloadOrThrow(url, deviceId, token, file, offer, onPct)
        } catch (e: java.io.IOException) {
            if ((file.parentFile?.usableSpace ?: 0L) < 1024L * 1024L) "storage" else "network"
        } catch (e: Exception) {
            "network"
        }
    }

    private fun downloadOrThrow(url: String, deviceId: String, token: String, file: File, offer: UpdatePolicy.Offer, onPct: (Int) -> Unit): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = false // הפניה לשרת אחר לא מתקבלת
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Device-Id", deviceId)
        }
        try {
            if (conn.responseCode != 200) return "network"
            val digest = MessageDigest.getInstance("SHA-256")
            var total = 0L
            var lastPct = -1
            conn.inputStream.use { input ->
                file.outputStream().use { out ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        total += n
                        if (total > offer.size) return "size"
                        digest.update(buf, 0, n)
                        out.write(buf, 0, n)
                        val pct = (total * 100 / offer.size).toInt()
                        if (pct != lastPct) { lastPct = pct; onPct(pct) }
                    }
                }
            }
            val hash = digest.digest().joinToString("") { "%02x".format(it) }
            return when {
                total != offer.size -> "size"
                hash != offer.sha256 -> "hash"
                else -> null
            }
        } finally {
            conn.disconnect()
        }
    }

    /** החבילה, הגרסה והחותמים של הקובץ חייבים להתאים. null = תקין. */
    private fun verifyApk(context: Context, file: File, offer: UpdatePolicy.Offer): String? {
        val pm = context.packageManager
        val archive = pm.getPackageArchiveInfo(file.path, PackageManager.GET_SIGNING_CERTIFICATES) ?: return "package"
        if (archive.packageName != offer.pkg) return "package"
        if (archive.longVersionCode != offer.versionCode) return "version"
        val installed = try {
            pm.getPackageInfo(offer.pkg, PackageManager.GET_SIGNING_CERTIFICATES)
        } catch (e: PackageManager.NameNotFoundException) {
            return "package"
        }
        if (archive.longVersionCode <= installed.longVersionCode) return "version"
        val self = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
        val apkSigners = signers(archive)
        // אותו מפתח כמו האפליקציה המותקנת (אנדרואיד דורש את זה בכל מקרה) וגם כמו Settings עצמה:
        // כך עדכון לא יכול להיות חתום במפתח אחר גם אם מישהו התקין קודם גרסה זרה באותו שם חבילה.
        if (!UpdatePolicy.sameSigners(apkSigners, signers(installed))) return "signature"
        if (!UpdatePolicy.sameSigners(apkSigners, signers(self))) return "signature"
        return null
    }

    private fun signers(info: PackageInfo): Set<String> {
        // apkContentsSigners: החותמים הנוכחיים בלבד (בלי היסטוריית החלפת מפתח), גם כשיש חותם אחד
        val certs = info.signingInfo?.apkContentsSigners ?: return emptySet()
        return certs.map { UpdatePolicy.sha256Hex(it.toByteArray()) }.toSet()
    }

    private fun commitSession(context: Context, file: File, offer: UpdatePolicy.Offer) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(offer.pkg)
            setSize(file.length())
            setInstallReason(android.content.pm.PackageManager.INSTALL_REASON_USER)
        }
        val sessionId = installer.createSession(params)
        installer.openSession(sessionId).use { session ->
            session.openWrite("base.apk", 0, file.length()).use { out ->
                file.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            // FLAG_MUTABLE: PackageInstaller ממלא את ה-extras של התוצאה. ה-Intent מפורש ל-receiver לא חשוף.
            val status = Intent(context, UpdateInstallReceiver::class.java)
                .setAction(ACTION_INSTALL_STATUS)
                .putExtra(EXTRA_RELEASE_ID, offer.releaseId)
            val pi = PendingIntent.getBroadcast(
                context,
                offer.releaseId.hashCode(),
                status,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
            session.commit(pi.intentSender)
        }
    }

    /** נקרא מ-UpdateInstallReceiver. */
    fun onInstallStatus(context: Context, intent: Intent) {
        val releaseId = intent.getStringExtra(EXTRA_RELEASE_ID) ?: return
        when (val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                setProgress(releaseId, Progress.WaitingForUser)
                // מסך האישור של אנדרואיד. נפתח רק כתגובה ל-session שאנחנו פתחנו
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> {
                setProgress(releaseId, Progress.Installed)
                reconcilePending(context)
                Thread { flushEvents(context) }.start()
            }
            else -> {
                val code = if (status == PackageInstaller.STATUS_FAILURE_ABORTED) "aborted" else "install_$status"
                setProgress(releaseId, Progress.Failed(code))
                removePending(context, releaseId)
                queueEvent(context, releaseId, "failed", if (status == PackageInstaller.STATUS_FAILURE_ABORTED) "aborted" else "STATUS_$status")
                Thread { flushEvents(context) }.start()
            }
        }
    }

    // ---------- התקנות שממתינות לאישור ----------
    // עדכון של Settings עצמה הורג את התהליך לפני שה-receiver מקבל SUCCESS. לכן "הותקן" נקבע
    // לפי מה שבאמת מותקן, בכל בדיקה ובכל כניסה למסך.

    private fun markPending(context: Context, offer: UpdatePolicy.Offer) {
        val p = readObject(context, KEY_PENDING)
        p.put(offer.releaseId, JSONObject().put("pkg", offer.pkg).put("version_code", offer.versionCode).put("at", System.currentTimeMillis()))
        prefs(context).edit().putString(KEY_PENDING, p.toString()).commit()
    }

    private fun removePending(context: Context, releaseId: String) {
        val p = readObject(context, KEY_PENDING)
        p.remove(releaseId)
        prefs(context).edit().putString(KEY_PENDING, p.toString()).commit()
        File(File(context.filesDir, "updates"), "$releaseId.apk").delete()
    }

    @Synchronized
    fun reconcilePending(context: Context) {
        val p = readObject(context, KEY_PENDING)
        val keys = p.keys().asSequence().toList()
        for (releaseId in keys) {
            val item = p.optJSONObject(releaseId) ?: continue
            val installed = installedVersion(context, item.optString("pkg"))
            val stale = System.currentTimeMillis() - item.optLong("at") > 7L * 24 * 60 * 60 * 1000
            if (installed != null && installed >= item.optLong("version_code")) {
                queueEvent(context, releaseId, "installed", null)
                setProgress(releaseId, Progress.Installed)
                removePending(context, releaseId)
            } else if (stale) {
                removePending(context, releaseId)
            }
        }
        // קבצים שנשארו בלי pending (הורדה שנקטעה) נמחקים. קובץ שנכתב בשעה האחרונה אולי עדיין בהורדה
        val pendingNow = readObject(context, KEY_PENDING)
        val hourAgo = System.currentTimeMillis() - 60L * 60L * 1000L
        File(context.filesDir, "updates").listFiles()?.forEach { f ->
            if (!pendingNow.has(f.nameWithoutExtension) && f.lastModified() < hourAgo) f.delete()
        }
    }

    // ---------- דיווח לשרת (תור, כדי שלא יאבד בלי רשת) ----------

    @Synchronized
    private fun queueEvent(context: Context, releaseId: String, event: String, error: String?) {
        val arr = readArray(context, KEY_EVENTS)
        if (arr.length() >= 50) return
        arr.put(JSONObject().put("release_id", releaseId).put("event", event).apply { if (error != null) put("error", error.take(64)) })
        prefs(context).edit().putString(KEY_EVENTS, arr.toString()).commit()
    }

    @Synchronized
    fun flushEvents(context: Context) {
        val arr = readArray(context, KEY_EVENTS)
        if (arr.length() == 0) return
        val base = FutureServerRegistration.resolvedBaseUrl() ?: return
        val (deviceId, token) = FutureServerRegistration.loadCredentials(context) ?: return
        val kept = JSONArray()
        for (i in 0 until arr.length()) {
            val e = arr.getJSONObject(i)
            val code = postStatus("$base/update/event", deviceId, token, e.toString())
            // 2xx = נקלט, 4xx = לא תקין ולא יעזור לשלוח שוב. רק שגיאת רשת או 5xx נשארים בתור
            if (code == null || code >= 500) kept.put(e)
        }
        prefs(context).edit().putString(KEY_EVENTS, kept.toString()).commit()
    }

    // ---------- HTTP ----------

    private fun post(url: String, deviceId: String, token: String, body: String): String? = try {
        if (!UpdatePolicy.transportAllowed(url, BuildConfig.DEBUG)) null
        else {
            val conn = open(url, deviceId, token, body)
            try {
                if (conn.responseCode !in 200..299) null
                else conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            } finally {
                conn.disconnect()
            }
        }
    } catch (e: Exception) {
        null
    }

    private fun postStatus(url: String, deviceId: String, token: String, body: String): Int? = try {
        val conn = open(url, deviceId, token, body)
        try { conn.responseCode } finally { conn.disconnect() }
    } catch (e: Exception) {
        null
    }

    private fun open(url: String, deviceId: String, token: String, body: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
            instanceFollowRedirects = false
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-Device-Id", deviceId)
            outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        }

    private fun readArray(context: Context, key: String): JSONArray =
        runCatching { JSONArray(prefs(context).getString(key, "[]")) }.getOrDefault(JSONArray())

    private fun readObject(context: Context, key: String): JSONObject =
        runCatching { JSONObject(prefs(context).getString(key, "{}")!!) }.getOrDefault(JSONObject())
}
