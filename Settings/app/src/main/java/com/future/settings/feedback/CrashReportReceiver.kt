package com.future.settings.feedback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.future.settings.futureserver.FutureNotifications

/**
 * מקבל דוחות קריסה מ-CrashCapture של SharedKeypadNav. חשוף, אבל רק לשולח שמחזיק את הרשאת החתימה
 * com.future.settings.permission.FUTURE_SERVER (כלומר אפליקציה שחתומה במפתח שלנו).
 *
 * גם כך התוכן לא אמין: החבילה נבדקת מול תבנית, ה-trace נחתך ועובר סינון פרטים אישיים, ושום דבר
 * לא נשלח מכאן. רק התראה מקומית, שפותחת את מסך המשוב עם תצוגה מקדימה.
 */
class CrashReportReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_CRASH_REPORT) return
        val id = intent.getStringExtra(EXTRA_ID)?.takeIf { ID_RE.matches(it) } ?: return
        val pkg = intent.getStringExtra(EXTRA_PKG)?.takeIf { PKG_RE.matches(it) } ?: return
        val trace = intent.getStringExtra(EXTRA_TRACE)?.take(MAX_TRACE)?.takeIf { it.isNotBlank() } ?: return
        val crash = CrashStore.Crash(
            id = id,
            pkg = pkg,
            versionName = FeedbackPayload.cleanVersion(intent.getStringExtra(EXTRA_VERSION_NAME)) ?: "",
            versionCode = intent.getLongExtra(EXTRA_VERSION_CODE, 0L).coerceAtLeast(0L),
            at = intent.getLongExtra(EXTRA_AT, System.currentTimeMillis()).coerceIn(0L, System.currentTimeMillis() + 60_000L),
            trace = FeedbackRedactor.redact(trace),
        )
        val app = context.applicationContext
        if (CrashStore.add(app, crash)) FutureNotifications.showCrash(app, crash)
    }

    companion object {
        // אותם ערכים כמו com.future.sharednav.crash.CrashCapture
        const val ACTION_CRASH_REPORT = "com.future.settings.action.CRASH_REPORT"
        private const val EXTRA_ID = "crash_id"
        private const val EXTRA_PKG = "pkg"
        private const val EXTRA_VERSION_NAME = "version_name"
        private const val EXTRA_VERSION_CODE = "version_code"
        private const val EXTRA_AT = "at"
        private const val EXTRA_TRACE = "trace"
        private const val MAX_TRACE = 16 * 1024 + 8
        private val ID_RE = Regex("""^[0-9a-f-]{36}$""")
        private val PKG_RE = Regex("""^com\.future\.[a-z0-9_.]{1,64}$""")
    }
}
