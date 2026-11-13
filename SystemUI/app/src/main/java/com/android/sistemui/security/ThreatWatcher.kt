package com.android.sistemui.security

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.provider.Settings
import android.security.KeyChain
import android.util.Log
import android.widget.Toast
import com.future.sharednav.systemui.SystemUiTarget

/**
 * הגנה בזמן אמת: FutureUI רצה כל הזמן, אז היא שמה לב ברגע שאפליקציה מקבלת
 * יכולת מסוכנת - ולא רק כשהמשתמש נכנס למרכז האבטחה.
 *
 * מאזין לשינויים בהגדרות שנוזקות משנות (שירותי נגישות, מאזיני התראות, שרת מתווך,
 * ניפוי באגים ב-Wi-Fi), להתקנה ועדכון של אפליקציות, לשינוי במאגר התעודות ולהדלקת המסך
 * (שם נתפסים מנהלי מכשיר ו-VPN, שאין עליהם אירוע). אחרי כל אירוע - סריקה, ו-ThreatGuard
 * מחליט מה לחסום. על כל ממצא חדש (בכל חומרה) - התראה אחת מקובצת שפותחת את מרכז האבטחה.
 *
 * בהתקנה/עדכון של אפליקציה ובהפעלה הסריקה כוללת גם חלון צף והתקנת אפליקציות (דרך root),
 * כי שם אפליקציה חדשה מקבלת אותן. בשאר האירועים לא, כדי לא להריץ su שוב ושוב.
 */
class ThreatWatcher(context: Context) {
    private val app = context.applicationContext
    private val thread = HandlerThread("ThreatWatcher").apply { start() }
    private val handler = Handler(thread.looper)
    @Volatile private var pendingAppOps = true
    private val rescan = Runnable {
        val withAppOps = pendingAppOps
        pendingAppOps = false
        check(withAppOps)
    }
    private var started = false

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = schedule(DEBOUNCE_MS, withAppOps = false)
    }

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_PACKAGE_FULLY_REMOVED) {
                runCatching { ThreatGuard.pruneUninstalled(app) }
                return
            }
            schedule(DEBOUNCE_MS, withAppOps = true)
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = schedule(DEBOUNCE_MS, withAppOps = false)
    }

    fun start() {
        if (started) return
        // FutureUI ו-SystemUI (העותק) מותקנות שתיהן: רק אחת שומרת, כדי שלא יהיו שתי התראות ושתי חסימות
        if (!isPrimary(app)) {
            Log.i(TAG, "${app.packageName} is not the active system UI - watcher off")
            return
        }
        started = true
        val cr = app.contentResolver
        listOf(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            Settings.Secure.getUriFor("enabled_notification_listeners"),
            Settings.Global.getUriFor(Settings.Global.HTTP_PROXY),
            Settings.Global.getUriFor("adb_wifi_enabled"),
        ).forEach { uri -> runCatching { cr.registerContentObserver(uri, false, observer) } }

        // שידורי מערכת מוגנים - רק המערכת שולחת אותם
        runCatching {
            app.registerReceiver(packageReceiver, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
                addDataScheme("package")
            }, null, handler)
            app.registerReceiver(receiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(KeyChain.ACTION_TRUST_STORE_CHANGED)
            }, null, handler)
        }.onFailure { Log.w(TAG, "register receivers", it) }

        schedule(STARTUP_DELAY_MS, withAppOps = true)
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { app.contentResolver.unregisterContentObserver(observer) }
        runCatching { app.unregisterReceiver(packageReceiver) }
        runCatching { app.unregisterReceiver(receiver) }
        handler.removeCallbacks(rescan)
        thread.quitSafely()
    }

    private fun schedule(delayMs: Long, withAppOps: Boolean) {
        if (withAppOps) pendingAppOps = true
        handler.removeCallbacks(rescan)
        handler.postDelayed(rescan, delayMs)
    }

    private fun check(withAppOps: Boolean) {
        val result = runCatching { ThreatScanner.scan(app, withAppOps) }
            .getOrElse { Log.w(TAG, "scan failed", it); return }
        val outcome = runCatching { ThreatGuard.evaluate(app, result, withAppOps) }
            .getOrElse { Log.w(TAG, "evaluate failed", it); return }
        when {
            outcome.baseline.isNotEmpty() -> alertBaseline(outcome.baseline)
            outcome.fresh.isNotEmpty() || outcome.blocked.isNotEmpty() -> alert(outcome)
        }
    }

    private fun alert(outcome: ThreatGuard.Outcome) {
        val blockedKeys = outcome.blocked.map { it.key }.toSet()
        val all = (outcome.blocked + outcome.fresh).distinctBy { it.key }.sortedByDescending { it.kind.severity }
        val first = all.first()
        val blockedCount = blockedKeys.size
        val openCount = all.size - blockedCount
        val title = when {
            blockedCount == 1 && openCount == 0 -> "נחסם ניסיון השתלטות"
            blockedCount > 1 && openCount == 0 -> "נחסמו $blockedCount ניסיונות השתלטות"
            blockedCount > 0 -> "נחסמו $blockedCount, ${openCount} ממתינים לבדיקה"
            all.size == 1 -> "התראת אבטחה"
            else -> "${all.size} התראות אבטחה"
        }
        val text = describe(first) + if (first.key in blockedKeys) " · נחסם" else ""
        val more = all.drop(1).take(4).joinToString("\n") { describe(it) + if (it.key in blockedKeys) " · נחסם" else "" }
        val footer = if (blockedCount > 0) "אם זה היה אתה, אפשר לבטל את החסימה במרכז האבטחה." else "פתח את מרכז האבטחה כדי לבדוק."
        notify(title, text, listOf(text, more, footer).filter { it.isNotBlank() }.joinToString("\n"))
        // FutureUI לא מקפיצה באנר להתראות של עצמה - אז על חסימה או ממצא חמור גם הודעה קצרה על המסך
        if (blockedCount > 0 || first.kind.severity >= 2) toast("$title: $text")
    }

    private fun alertBaseline(pending: List<ThreatScanner.Finding>) {
        val title = if (pending.size == 1) "ממצא אחד מחכה להחלטה שלך" else "${pending.size} ממצאים מחכים להחלטה שלך"
        val list = pending.take(5).joinToString("\n") { describe(it) }
        notify(
            title,
            describe(pending.first()),
            "$list\nהם היו פעילים לפני שההגנה הופעלה, אז לא נחסמו. במרכז האבטחה אפשר לחסום או לאשר כל אחד.",
        )
        toast(title)
    }

    private fun notify(title: String, text: String, big: String) {
        runCatching {
            val nm = app.getSystemService(NotificationManager::class.java) ?: return@runCatching
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "התראות אבטחה", NotificationManager.IMPORTANCE_HIGH)
            )
            val open = Intent(ACTION_OPEN_SECURITY)
                .setClassName(SETTINGS_PACKAGE, SETTINGS_ACTIVITY)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            val pi = PendingIntent.getActivity(
                app, NOTIFICATION_ID, open, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            nm.notify(
                NOTIFICATION_ID,
                Notification.Builder(app, CHANNEL)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(text)
                    .setStyle(Notification.BigTextStyle().bigText(big))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
            )
        }.onFailure { Log.w(TAG, "notify failed", it) }
    }

    private fun toast(text: String) {
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(app, text, Toast.LENGTH_LONG).show() }
        }
    }

    private fun describe(f: ThreatScanner.Finding): String = when (f.kind) {
        ThreatScanner.Kind.KNOWN_MALWARE -> "${f.label} היא נוזקת ריגול ידועה (${f.detail})"
        ThreatScanner.Kind.ACCESSIBILITY -> "${f.label} קיבלה שליטה במסך (שירות נגישות)"
        ThreatScanner.Kind.NOTIFICATION_LISTENER -> "${f.label} קוראת את ההתראות"
        ThreatScanner.Kind.DEVICE_ADMIN -> "${f.label} הפכה למנהלת המכשיר"
        ThreatScanner.Kind.USER_CA -> "הותקנה תעודת אבטחה: ${f.label}"
        ThreatScanner.Kind.PROXY -> "התעבורה מנותבת דרך שרת מתווך"
        ThreatScanner.Kind.ADB_WIFI -> "ניפוי באגים דרך Wi-Fi הופעל"
        ThreatScanner.Kind.SMS_READER -> "${f.label} קוראת הודעות SMS"
        ThreatScanner.Kind.OVERLAY -> "${f.label} מציגה חלונות מעל אפליקציות"
        ThreatScanner.Kind.INSTALLER -> "${f.label} יכולה להתקין אפליקציות"
        ThreatScanner.Kind.VPN -> "VPN הופעל"
    }

    companion object {
        private const val TAG = "ThreatWatcher"
        private const val CHANNEL = "futureos_security_alerts"
        private const val NOTIFICATION_ID = 7301
        private const val DEBOUNCE_MS = 2_000L
        private const val STARTUP_DELAY_MS = 15_000L
        const val SETTINGS_PACKAGE = "com.future.settings"
        const val SETTINGS_ACTIVITY = "com.future.settings.MainActivity"
        /** חייב להיות זהה ל-MainActivity.ACTION_OPEN_SECURITY בהגדרות. */
        const val ACTION_OPEN_SECURITY = "com.future.settings.action.OPEN_SECURITY"

        /**
         * FutureUI היא ממשק המערכת הראשי. העותק (SystemUI) שומר רק כשהשירותים של FutureUI
         * לא פעילים. SystemUiTarget.PACKAGE בא מ-SharedKeypadNav, אז סקריפט הסנכרון לא משנה אותו.
         */
        fun isPrimary(context: Context): Boolean {
            val primary = SystemUiTarget.PACKAGE
            if (context.packageName == primary) return true
            val enabled = runCatching {
                Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            }.getOrNull().orEmpty()
            return enabled.split(':').none { it.startsWith("$primary/") }
        }
    }
}
