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

/**
 * הגנה בזמן אמת: FutureUI רצה כל הזמן, אז היא שמה לב ברגע שאפליקציה מקבלת
 * יכולת מסוכנת - ולא רק כשהמשתמש נכנס למרכז האבטחה.
 *
 * מאזין לשינויים בהגדרות שנוזקות משנות (שירותי נגישות, מאזיני התראות, שרת מתווך,
 * ניפוי באגים ב-Wi-Fi), להתקנת אפליקציות, לשינוי במאגר התעודות ולהדלקת המסך
 * (שם נתפסים מנהלי מכשיר ו-VPN, שאין עליהם אירוע). אחרי כל אירוע - סריקה, ועל
 * ממצא חמור חדש: התראה שפותחת את מרכז האבטחה בהגדרות.
 *
 * רק ממצאים שעוד לא דווחו: כל ממצא מדווח פעם אחת, וממצא שנעלם ואז חוזר מדווח שוב.
 * השומר לא משנה שום דבר בטלפון בעצמו - ההחלטה תמיד של המשתמש.
 */
class ThreatWatcher(context: Context) {
    private val app = context.applicationContext
    private val thread = HandlerThread("ThreatWatcher").apply { start() }
    private val handler = Handler(thread.looper)
    private val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val rescan = Runnable { check() }
    private var started = false

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean) = schedule(DEBOUNCE_MS)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = schedule(DEBOUNCE_MS)
    }

    fun start() {
        if (started) return
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
            app.registerReceiver(receiver, IntentFilter().apply {
                addAction(Intent.ACTION_PACKAGE_ADDED)
                addAction(Intent.ACTION_PACKAGE_REPLACED)
                addDataScheme("package")
            }, null, handler)
            app.registerReceiver(receiver, IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(KeyChain.ACTION_TRUST_STORE_CHANGED)
            }, null, handler)
        }.onFailure { Log.w(TAG, "register receivers", it) }

        schedule(STARTUP_DELAY_MS)
    }

    fun stop() {
        if (!started) return
        started = false
        runCatching { app.contentResolver.unregisterContentObserver(observer) }
        runCatching { app.unregisterReceiver(receiver) }
        handler.removeCallbacks(rescan)
        thread.quitSafely()
    }

    private fun schedule(delayMs: Long) {
        handler.removeCallbacks(rescan)
        handler.postDelayed(rescan, delayMs)
    }

    private fun check() {
        val result = runCatching { ThreatScanner.scan(app, withAppOps = false) }
            .getOrElse { Log.w(TAG, "scan failed", it); return }
        val serious = result.findings.filter { it.kind.severity >= 2 }
        val seen = prefs.getStringSet(KEY_SEEN, emptySet()).orEmpty()
        val fresh = serious.filter { it.key !in seen }
        prefs.edit().putStringSet(KEY_SEEN, serious.map { it.key }.toSet()).apply()
        if (fresh.isNotEmpty()) alert(fresh)
    }

    private fun alert(fresh: List<ThreatScanner.Finding>) {
        val first = fresh.first()
        val title = if (fresh.size == 1) "התראת אבטחה" else "${fresh.size} התראות אבטחה"
        val text = describe(first)
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
                    .setStyle(Notification.BigTextStyle().bigText("$text\nפתח את מרכז האבטחה כדי לבדוק."))
                    .setCategory(Notification.CATEGORY_ERROR)
                    .setContentIntent(pi)
                    .setAutoCancel(true)
                    .build()
            )
        }.onFailure { Log.w(TAG, "notify failed", it) }
        // FutureUI לא מקפיצה באנר להתראות של עצמה - אז גם הודעה קצרה על המסך
        Handler(Looper.getMainLooper()).post {
            runCatching { Toast.makeText(app, "$title: $text", Toast.LENGTH_LONG).show() }
        }
    }

    private fun describe(f: ThreatScanner.Finding): String = when (f.kind) {
        ThreatScanner.Kind.ACCESSIBILITY -> "${f.label} קיבלה שליטה במסך (שירות נגישות)"
        ThreatScanner.Kind.NOTIFICATION_LISTENER -> "${f.label} קוראת את ההתראות"
        ThreatScanner.Kind.DEVICE_ADMIN -> "${f.label} הפכה למנהלת המכשיר"
        ThreatScanner.Kind.USER_CA -> "הותקנה תעודת אבטחה: ${f.label}"
        ThreatScanner.Kind.PROXY -> "התעבורה מנותבת דרך שרת מתווך"
        ThreatScanner.Kind.ADB_WIFI -> "ניפוי באגים דרך Wi-Fi הופעל"
        else -> "${f.label} קיבלה הרשאה רגישה"
    }

    companion object {
        private const val TAG = "ThreatWatcher"
        private const val PREFS = "threat_watch"
        private const val KEY_SEEN = "seen"
        private const val CHANNEL = "futureos_security_alerts"
        private const val NOTIFICATION_ID = 7301
        private const val DEBOUNCE_MS = 2_000L
        private const val STARTUP_DELAY_MS = 15_000L
        const val SETTINGS_PACKAGE = "com.future.settings"
        const val SETTINGS_ACTIVITY = "com.future.settings.MainActivity"
        /** חייב להיות זהה ל-MainActivity.ACTION_OPEN_SECURITY בהגדרות. */
        const val ACTION_OPEN_SECURITY = "com.future.settings.action.OPEN_SECURITY"
    }
}
