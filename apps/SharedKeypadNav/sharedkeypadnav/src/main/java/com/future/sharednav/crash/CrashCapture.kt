package com.future.sharednav.crash

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Process
import java.io.File
import java.util.UUID

/**
 * תפיסת קריסות בכל אפליקציות FutureOS, בלי שינוי באף אפליקציה: [CrashCaptureInstaller] מתקין את זה
 * עם עליית התהליך (ספק במניפסט של הספרייה).
 *
 * כשאפליקציה קורסת: נשמר דוח קצר (חבילה, גרסה, thread, stack trace) בתיקייה הפרטית שלה, ונשלח שידור
 * מפורש ל-Settings, שמוגן בהרשאת חתימה (רק אפליקציות שלנו יכולות לשלוח אותו). Settings מסננת פרטים
 * אישיים, שומרת, ומציגה התראה "X נסגרה בגלל תקלה". שום דבר לא נשלח לשרת בלי שהמשתמש פותח את ההתראה,
 * רואה תצוגה מקדימה ולוחץ "שלח".
 *
 * אם השידור לא יצא (Settings לא מותקנת, התהליך נהרג באמצע), הדוח נשאר בקובץ ונשלח בעלייה הבאה.
 * אחרי השמירה הקריסה ממשיכה למטפל הקודם, כך שההתנהגות של אנדרואיד ("האפליקציה נעצרה") לא משתנה.
 */
object CrashCapture {
    const val ACTION_CRASH_REPORT = "com.future.settings.action.CRASH_REPORT"
    const val EXTRA_ID = "crash_id"
    const val EXTRA_PKG = "pkg"
    const val EXTRA_VERSION_NAME = "version_name"
    const val EXTRA_VERSION_CODE = "version_code"
    const val EXTRA_AT = "at"
    const val EXTRA_TRACE = "trace"
    const val MAX_TRACE = 16 * 1024
    private const val SETTINGS_PKG = "com.future.settings"
    private const val RECEIVER = "com.future.settings.feedback.CrashReportReceiver"
    private const val DIR = "future_crash"
    private const val FILE = "pending.txt"

    @Volatile
    private var installed = false

    fun install(context: Context) {
        if (installed) return
        installed = true
        val app = context.applicationContext ?: context
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, error ->
            try {
                record(app, thread, error)
            } catch (_: Throwable) {
                // דוח קריסה לעולם לא מפיל את מה שהיה קורה בלעדיו
            }
            if (previous != null) {
                previous.uncaughtException(thread, error)
            } else {
                Process.killProcess(Process.myPid())
                System.exit(10)
            }
        }
    }

    private fun record(context: Context, thread: Thread, error: Throwable) {
        val report = Report(
            id = UUID.randomUUID().toString(),
            pkg = context.packageName,
            versionName = versionName(context),
            versionCode = versionCode(context),
            at = System.currentTimeMillis(),
            trace = buildTrace(thread.name, error),
        )
        val file = pendingFile(context)
        // קודם לדיסק (שורד גם אם השידור נכשל), ואז שידור
        runCatching { file.parentFile?.mkdirs(); file.writeText(report.encode()) }
        if (send(context, report)) file.delete()
    }

    /** נקרא בעלייה: דוח שלא נשלח בפעם הקודמת. רץ על thread רקע. */
    fun flushPending(context: Context) {
        val file = pendingFile(context)
        if (!file.exists()) return
        val report = runCatching { Report.decode(file.readText()) }.getOrNull()
        if (report == null || send(context, report)) file.delete()
    }

    private fun send(context: Context, report: Report): Boolean = try {
        val intent = Intent(ACTION_CRASH_REPORT)
            .setComponent(ComponentName(SETTINGS_PKG, RECEIVER))
            .putExtra(EXTRA_ID, report.id)
            .putExtra(EXTRA_PKG, report.pkg)
            .putExtra(EXTRA_VERSION_NAME, report.versionName)
            .putExtra(EXTRA_VERSION_CODE, report.versionCode)
            .putExtra(EXTRA_AT, report.at)
            .putExtra(EXTRA_TRACE, report.trace)
        // בלי Settings מותקנת אין למי לשלוח: הדוח נשאר בקובץ
        if (context.packageManager.queryBroadcastReceivers(intent, 0).isEmpty()) {
            false
        } else {
            context.sendBroadcast(intent)
            true
        }
    } catch (_: Throwable) {
        false
    }

    /** thread + stack trace, כולל causes, חתוך לגודל קבוע. stackTraceToString כולל גם UnknownHostException. */
    fun buildTrace(threadName: String, error: Throwable): String {
        val text = "thread: $threadName\n${error.stackTraceToString()}"
        return if (text.length <= MAX_TRACE) text else text.substring(0, MAX_TRACE) + "\n…"
    }

    private fun pendingFile(context: Context) = File(File(context.filesDir, DIR), FILE)

    private fun versionName(context: Context): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    }.getOrDefault("")

    private fun versionCode(context: Context): Long = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    }.getOrDefault(0L)

    /** פורמט שורות פשוט: חמש שורות כותרת ואז ה-trace. בלי org.json כדי שיעבוד בבדיקת JVM. */
    data class Report(val id: String, val pkg: String, val versionName: String, val versionCode: Long, val at: Long, val trace: String) {
        fun encode(): String =
            listOf(id, pkg, versionName.replace('\n', ' '), versionCode.toString(), at.toString(), trace).joinToString("\n")

        companion object {
            fun decode(text: String): Report? {
                val parts = text.split("\n", limit = 6)
                if (parts.size < 6) return null
                return Report(parts[0], parts[1], parts[2], parts[3].toLongOrNull() ?: 0L, parts[4].toLongOrNull() ?: 0L, parts[5])
            }
        }
    }
}
