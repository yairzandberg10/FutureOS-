package com.future.futureui.controlcenter.logic

import com.future.sharednav.i18n.tr

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.UiModeManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NoteAdd
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.sharednav.root.RootShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** קבוצות ברשימת "הוספת כפתורים" במצב עריכה - כל קבוצה מוצגת עם כותרת משלה. */
enum class GridCategory(val title: String) {
    SOUND(tr("קול")),
    DISPLAY(tr("מסך")),
    QUICK(tr("כלים מהירים")),
    ACTIONS(tr("פעולות")),
    APPS(tr("אפליקציות")),
    SYSTEM(tr("מערכת")),
}

/**
 * הקטלוג של רשת האייקונים במרכז הבקרה - עד [GridCatalog.MAX_CONTROLS] פקדים
 * שהמשתמש מוסיף בעצמו במצב עריכה (הרשת ריקה כברירת מחדל). שורות הגלולות
 * הקבועות ממשיכות להשתמש ב-ControlLayoutManager.allAvailableControls, ולכן
 * פקד שכבר יש לו גלולה (סוללה, לילה, נא לא להפריע) לא נמצא כאן שוב.
 *
 * [isToggle] - פקד עם מצב פועל/כבוי אמיתי שנקרא מהמערכת. השאר הם פעולות:
 * פתיחת אפליקציה או מסך הגדרות, או פקודה חד-פעמית. פעולה שפותחת משהו
 * מעל מרכז הבקרה מסומנת ב-[closesPanel] כדי שהפאנל ייסגר ולא יסתיר אותה.
 * [optimistic] - הופכים את המצב מיד בלחיצה, לפני שהמערכת מאשרת. כבוי לפקד
 * שמציג מצב אבל רק פותח מסך (נקודה חמה), או שמחליף בין כמה מצבים.
 */
data class GridControl(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isToggle: Boolean,
    val category: GridCategory,
    val closesPanel: Boolean = !isToggle,
    val optimistic: Boolean = isToggle,
)

object GridCatalog {
    /** 3 שורות של 5. במצב מקופל רואים 2 שורות, ויותר מזה דורש הרבה לחיצות חץ. */
    const val MAX_CONTROLS = 15

    val all: List<GridControl> = listOf(
        // קול
        GridControl("ringer_mode", tr("צלצול"), Icons.AutoMirrored.Rounded.VolumeUp, true, GridCategory.SOUND, optimistic = false),
        GridControl("mute_media", tr("השתקת מדיה"), Icons.Rounded.MusicOff, true, GridCategory.SOUND),
        // מסך
        GridControl("auto_brightness", tr("בהירות אוטומטית"), Icons.Rounded.BrightnessAuto, true, GridCategory.DISPLAY),
        GridControl("extra_dim", tr("עמעום נוסף"), Icons.Rounded.BrightnessLow, true, GridCategory.DISPLAY),
        GridControl("long_timeout", tr("מסך דלוק 10 דק'"), Icons.Rounded.Timer, true, GridCategory.DISPLAY),
        GridControl("dark_theme", tr("ערכת נושא כהה"), Icons.Rounded.DarkMode, true, GridCategory.DISPLAY),
        GridControl("text_size", tr("גודל טקסט"), Icons.Rounded.FormatSize, true, GridCategory.DISPLAY, optimistic = false),
        GridControl("sleep_mode", tr("מצב שינה"), Icons.Rounded.Bedtime, true, GridCategory.DISPLAY),
        // כלים מהירים - פותחים אפליקציה ישר על הפעולה
        GridControl("quick_timer", tr("טיימר 5 דק'"), Icons.Rounded.HourglassTop, false, GridCategory.QUICK),
        GridControl("quick_record", tr("הקלטה מהירה"), Icons.Rounded.FiberManualRecord, false, GridCategory.QUICK),
        GridControl("new_note", tr("פתק חדש"), Icons.AutoMirrored.Rounded.NoteAdd, false, GridCategory.QUICK),
        GridControl("next_alarm", tr("אין מעורר"), Icons.Rounded.AlarmOff, true, GridCategory.QUICK, closesPanel = true, optimistic = false),
        // פעולות
        GridControl("keypad_lock", tr("נעילת מקשים"), Icons.Rounded.Lock, false, GridCategory.ACTIONS),
        GridControl("screenshot", tr("צילום מסך"), Icons.Rounded.Screenshot, false, GridCategory.ACTIONS),
        GridControl("screen_record", tr("הקלטת מסך"), Icons.Rounded.Videocam, true, GridCategory.ACTIONS, closesPanel = true),
        GridControl("screen_off", tr("כיבוי מסך"), Icons.Rounded.MobileOff, false, GridCategory.ACTIONS),
        // פותח "שלח משוב" ב-Settings על האפליקציה שהייתה בחזית. שום דבר לא נשלח בלי תצוגה מקדימה
        GridControl("report_bug", tr("דווח על באג"), Icons.Rounded.BugReport, false, GridCategory.ACTIONS),
        // אפליקציות - רק כלים שפותחים "על הדרך"; השאר כבר במסך הבית
        GridControl("app_calculator", tr("מחשבון"), Icons.Rounded.Calculate, false, GridCategory.APPS),
        GridControl("app_clock", tr("שעון"), Icons.Rounded.Alarm, false, GridCategory.APPS),
        GridControl("app_notes", tr("פתקים"), Icons.Rounded.StickyNote2, false, GridCategory.APPS),
        GridControl("app_assistant", tr("עוזרי"), Icons.Rounded.Mic, false, GridCategory.APPS),
        GridControl("app_remote", tr("שלט"), Icons.Rounded.SettingsRemote, false, GridCategory.APPS),
        // מערכת
        GridControl("hotspot", tr("נקודה חמה"), Icons.Rounded.WifiTethering, true, GridCategory.SYSTEM, closesPanel = true, optimistic = false),
        GridControl("data_saver", tr("חוסך נתונים"), Icons.Rounded.DataSaverOn, true, GridCategory.SYSTEM),
        GridControl("set_wifi", tr("רשתות Wi-Fi"), Icons.Rounded.NetworkWifi, false, GridCategory.SYSTEM),
        GridControl("set_accessibility", tr("נגישות"), Icons.Rounded.Accessibility, false, GridCategory.SYSTEM),
    )

    private val byId = all.associateBy { it.id }

    fun get(id: String): GridControl? = byId[id]
}

/**
 * מבצע את פקדי הרשת וקורא את מצבם. כל הכתיבות להגדרות מערכת עוברות קודם
 * דרך root (`settings put` - המכשיר rooted ורוב המפתחות כאן לא ניתנים לכתיבה
 * לאפליקציה רגילה), עם נפילה ל-API הציבורי כשיש כזה.
 *
 * קריאת המצב רצה ברקע: מפתחות Settings מוסתרים זורקים SecurityException על
 * targetSdk 31, ואז הקריאה נופלת ל-`settings get` דרך root - לכן הרענון
 * נעשה רק על הפקדים שברשת, ולא בקצב של לולאת 500ms של מרכז הבקרה.
 *
 * חלק מהפקדים מחליפים שם ואייקון לפי המצב (מצב צלצול, גודל טקסט, המעורר
 * הבא) - ה-UI קורא אותם דרך [labelOf] ו-[iconOf].
 */
class GridControlManager(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver
    private val audio by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private val uiMode by lazy { context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager }
    private val connectivity by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    private val notifications by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }
    private val alarms by lazy { context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager }
    private val prefs by lazy { context.getSharedPreferences("grid_controls", Context.MODE_PRIVATE) }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

    /** מצב פועל/כבוי לפי מזהה - נקרא מה-UI, מתעדכן ב-[refresh]. */
    val states = mutableStateMapOf<String, Boolean>()
    private val labels = mutableStateMapOf<String, String>()
    private val icons = mutableStateMapOf<String, ImageVector>()

    fun isOn(id: String): Boolean = states[id] ?: false
    fun labelOf(control: GridControl): String = labels[control.id] ?: control.label
    fun iconOf(control: GridControl): ImageVector = icons[control.id] ?: control.icon

    private data class Reading(val on: Boolean, val label: String? = null, val icon: ImageVector? = null)

    fun refresh(ids: List<String>) {
        val toggles = ids.filter { GridCatalog.get(it)?.isToggle == true }
        if (toggles.isEmpty() || refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            val read = withContext(Dispatchers.IO) { toggles.associateWith { readState(it) } }
            read.forEach { (id, r) ->
                states[id] = r.on
                if (r.label != null) labels[id] = r.label else labels.remove(id)
                if (r.icon != null) icons[id] = r.icon else icons.remove(id)
            }
        }
    }

    fun dispose() = scope.cancel()

    fun activate(id: String) {
        val control = GridCatalog.get(id) ?: return
        if (control.optimistic) states[id] = !isOn(id)
        val target = if (control.optimistic) isOn(id) else !isOn(id)
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    perform(id, target)
                } catch (t: Throwable) {
                    Log.w(TAG, "pill $id failed", t)
                }
            }
            if (control.isToggle) {
                // המערכת מחילה חלק מהשינויים באיחור (uimode, font_scale) - קוראים שוב אחרי רגע
                delay(700)
                refreshJob?.cancel()
                refresh(listOf(id))
            }
        }
    }

    // ---- קריאת מצב ----

    private fun readState(id: String): Reading = try {
        when (id) {
            "ringer_mode" -> when (audio?.ringerMode) {
                AudioManager.RINGER_MODE_VIBRATE -> Reading(true, tr("רטט"), Icons.Rounded.Vibration)
                AudioManager.RINGER_MODE_SILENT -> Reading(true, tr("שקט"), Icons.Rounded.NotificationsOff)
                else -> Reading(false)
            }
            "text_size" -> {
                val scale = readFloat(NS_SYSTEM, Settings.System.FONT_SCALE, 1f)
                when {
                    scale < 1.07f -> Reading(false, tr("טקסט רגיל"))
                    scale < 1.22f -> Reading(true, tr("טקסט גדול"))
                    else -> Reading(true, tr("טקסט ענק"))
                }
            }
            "next_alarm" -> {
                val at = alarms?.nextAlarmClock?.triggerTime
                if (at == null) Reading(false) else Reading(true, tr("מעורר {0}", formatAlarm(at)), Icons.Rounded.AlarmOn)
            }
            "sleep_mode" -> prefs.getBoolean(KEY_SLEEP_ON, false) &&
                readInt(NS_SECURE, "reduce_bright_colors_activated", 0) == 1
            "hotspot" -> isHotspotOn()
            "screen_record" -> root("pidof screenrecord")
            "dark_theme" -> uiMode?.nightMode == UiModeManager.MODE_NIGHT_YES
            "data_saver" -> connectivity?.restrictBackgroundStatus != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED
            "mute_media" -> audio?.isStreamMute(AudioManager.STREAM_MUSIC) == true
            "auto_brightness" -> readInt(NS_SYSTEM, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            "extra_dim" -> readInt(NS_SECURE, "reduce_bright_colors_activated", 0) == 1
            "long_timeout" -> readInt(NS_SYSTEM, Settings.System.SCREEN_OFF_TIMEOUT, 30_000) >= LONG_TIMEOUT_MS
            else -> false
        }.let { if (it is Reading) it else Reading(it as Boolean) }
    } catch (t: Throwable) {
        Log.w(TAG, "read $id failed", t)
        Reading(false)
    }

    /** היום - רק השעה; אחרת גם היום בשבוע ("ב' 07:30"). */
    private fun formatAlarm(at: Long): String {
        val time = android.text.format.DateFormat.getTimeFormat(context).format(java.util.Date(at))
        val now = java.util.Calendar.getInstance()
        val then = java.util.Calendar.getInstance().apply { timeInMillis = at }
        val sameDay = now.get(java.util.Calendar.YEAR) == then.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == then.get(java.util.Calendar.DAY_OF_YEAR)
        if (sameDay) return time
        val day = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault()).format(java.util.Date(at))
        return "$day $time"
    }

    /** WifiManager.isWifiApEnabled מוסתר אבל נגיש ב-reflection; אם נחסם - בודקים אם ממשק ה-AP למעלה. */
    private fun isHotspotOn(): Boolean {
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE)
            if (wifi != null) return wifi.javaClass.getMethod("isWifiApEnabled").invoke(wifi) as Boolean
        } catch (t: Throwable) {
            Log.d(TAG, "isWifiApEnabled unavailable", t)
        }
        return try {
            java.net.NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().any {
                it.isUp && (it.name.startsWith("swlan") || it.name.startsWith("ap") || it.name.startsWith("softap"))
            }
        } catch (t: Throwable) {
            false
        }
    }

    private fun readRaw(ns: String, key: String): String? {
        val direct = try {
            when (ns) {
                NS_SYSTEM -> Settings.System.getString(resolver, key)
                NS_SECURE -> Settings.Secure.getString(resolver, key)
                else -> Settings.Global.getString(resolver, key)
            }
        } catch (e: SecurityException) {
            // מפתח מוסתר (לא @Readable) - root רואה הכל
            val r = RootShell.run("settings get $ns $key")
            return if (r.success) r.output.trim().takeIf { it.isNotEmpty() && it != "null" } else null
        }
        return direct
    }

    private fun readInt(ns: String, key: String, def: Int): Int =
        readRaw(ns, key)?.toFloatOrNull()?.toInt() ?: def

    private fun readFloat(ns: String, key: String, def: Float): Float =
        readRaw(ns, key)?.toFloatOrNull() ?: def

    // ---- ביצוע ----

    private fun perform(id: String, on: Boolean) {
        val v = if (on) 1 else 0
        when (id) {
            // צלצול -> רטט -> שקט -> צלצול
            "ringer_mode" -> setRinger(
                when (audio?.ringerMode) {
                    AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
                    AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
                    else -> AudioManager.RINGER_MODE_NORMAL
                }
            )
            "mute_media" -> audio?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (on) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )

            "auto_brightness" -> putSystem(
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            "extra_dim" -> root("settings put secure reduce_bright_colors_activated $v")
            "long_timeout" -> putSystem(Settings.System.SCREEN_OFF_TIMEOUT, if (on) LONG_TIMEOUT_MS else DEFAULT_TIMEOUT_MS)
            "dark_theme" -> setDarkTheme(on)
            // 1.0 -> 1.15 -> 1.3 -> 1.0
            "text_size" -> {
                val scale = readFloat(NS_SYSTEM, Settings.System.FONT_SCALE, 1f)
                val next = when {
                    scale < 1.07f -> "1.15"
                    scale < 1.22f -> "1.3"
                    else -> "1.0"
                }
                root("settings put system font_scale ${RootShell.quote(next)}")
            }
            "sleep_mode" -> setSleepMode(on)

            "quick_timer" -> startActivity(
                Intent().setClassName("com.future.clock", "com.future.clock.MainActivity")
                    .putExtra("com.future.clock.START_TIMER_SECONDS", QUICK_TIMER_SECONDS)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            "quick_record" -> startActivity(
                Intent().setClassName("com.future.recorder", "com.future.recorder.MainActivity")
                    .putExtra("com.future.recorder.AUTO_START", true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            "new_note" -> startActivity(
                Intent("com.future.notes.NEW_NOTE").setClassName("com.future.notes", "com.future.notes.MainActivity")
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            // בלי מעורר - פותחים את השעון כדי להוסיף; עם מעורר - כדי לכבות או לשנות אותו
            "next_alarm" -> openApp("com.future.clock")

            // הפאנל נסגר קודם ([GridControl.closesPanel]) - מחכים שייעלם מהמסך
            "keypad_lock" -> {
                SystemClock.sleep(300)
                if (com.future.futureui.lockscreen.logic.LockSettings(context).enabled) {
                    context.sendBroadcast(
                        Intent(com.future.futureui.statusbar.service.StatusBarAccessibilityService.ACTION_LOCK_NOW)
                            .setPackage(context.packageName)
                    )
                } else {
                    // מסך הנעילה כבוי בהגדרות - כיבוי המסך הוא הנעילה היחידה שיש
                    root("input keyevent ${KeyEvent.KEYCODE_SLEEP}")
                }
            }
            "screenshot" -> { SystemClock.sleep(700); root("input keyevent ${KeyEvent.KEYCODE_SYSRQ}") }
            "screen_record" -> if (on) startScreenRecord() else root("pkill -INT screenrecord")
            "screen_off" -> { SystemClock.sleep(300); root("input keyevent ${KeyEvent.KEYCODE_SLEEP}") }
            "report_bug" -> reportBug()

            "app_calculator" -> openApp("com.future.calculator")
            "app_clock" -> openApp("com.future.clock")
            "app_notes" -> openApp("com.future.notes")
            "app_assistant" -> openApp("com.future.assistant")
            "app_remote" -> openApp("com.future.remote")

            // אין דרך ציבורית (או פקודת shell) להדליק נקודה חמה עם ההגדרות של המשתמש - פותחים את המסך שלה
            "hotspot" -> {
                val tether = Intent().setClassName("com.android.settings", "com.android.settings.TetherSettings")
                if (tether.resolveActivity(context.packageManager) != null) startActivity(tether)
                else openSettings(Settings.ACTION_WIRELESS_SETTINGS)
            }
            "data_saver" -> if (!root("cmd netpolicy set restrict-background $on")) openSettings(Settings.ACTION_DATA_USAGE_SETTINGS)
            "set_wifi" -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            "set_accessibility" -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        }
    }

    private fun root(command: String): Boolean = RootShell.run(command).success

    private fun setDarkTheme(on: Boolean) {
        if (!root("cmd uimode night ${if (on) "yes" else "no"}")) openSettings(Settings.ACTION_DISPLAY_SETTINGS)
    }

    /**
     * מצב שינה = נא לא להפריע + עמעום נוסף + ערכת נושא כהה, בלחיצה אחת. המצב
     * הקודם של שלושתם נשמר, וכיבוי מחזיר בדיוק אותו (ולא מכבה ערכה כהה שהייתה
     * דלוקה עוד לפני כן).
     */
    private fun setSleepMode(on: Boolean) {
        val nm = notifications
        if (on) {
            prefs.edit()
                .putBoolean(KEY_SLEEP_ON, true)
                .putBoolean(KEY_PREV_DARK, uiMode?.nightMode == UiModeManager.MODE_NIGHT_YES)
                .putBoolean(KEY_PREV_DIM, readInt(NS_SECURE, "reduce_bright_colors_activated", 0) == 1)
                .putInt(KEY_PREV_DND, nm?.currentInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL)
                .apply()
            setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            root("settings put secure reduce_bright_colors_activated 1")
            setDarkTheme(true)
        } else {
            val prevDnd = prefs.getInt(KEY_PREV_DND, NotificationManager.INTERRUPTION_FILTER_ALL)
            setInterruptionFilter(
                if (prevDnd == NotificationManager.INTERRUPTION_FILTER_UNKNOWN) NotificationManager.INTERRUPTION_FILTER_ALL else prevDnd
            )
            root("settings put secure reduce_bright_colors_activated ${if (prefs.getBoolean(KEY_PREV_DIM, false)) 1 else 0}")
            setDarkTheme(prefs.getBoolean(KEY_PREV_DARK, false))
            prefs.edit().putBoolean(KEY_SLEEP_ON, false).apply()
        }
    }

    private fun setInterruptionFilter(filter: Int) {
        val nm = notifications ?: return
        if (!nm.isNotificationPolicyAccessGranted) root("cmd notification allow_dnd ${context.packageName}")
        try {
            nm.setInterruptionFilter(filter)
        } catch (e: SecurityException) {
            Log.w(TAG, "no DND access", e)
        }
    }

    /**
     * `screenrecord` רץ כ-root ברקע (עד 3 דקות - המגבלה של Android 12). אחרי
     * שהוא נגמר - בעצירה מהפקד (SIGINT סוגר את הקובץ תקין) או במגבלת הזמן -
     * אותו shell מודיע ל-MediaStore כדי שההקלטה תופיע בגלריה.
     */
    private fun startScreenRecord() {
        SystemClock.sleep(700)
        val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        val path = "$SCREEN_RECORD_DIR/screen_$stamp.mp4"
        root(
            "mkdir -p $SCREEN_RECORD_DIR; nohup sh -c 'screenrecord $path; " +
                "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://$path' " +
                "</dev/null >/dev/null 2>&1 &"
        )
    }

    /** מפתחות System ציבוריים: root קודם, ואם אין - WRITE_SETTINGS. */
    private fun putSystem(key: String, value: Int) {
        if (root("settings put system $key $value")) return
        if (Settings.System.canWrite(context)) {
            Settings.System.putInt(resolver, key, value)
        } else {
            startActivity(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS))
        }
    }

    /** מעבר אל/מ-מצב שקט דורש גישה למדיניות "נא לא להפריע" - נותנים אותה דרך root אם חסרה. */
    private fun setRinger(mode: Int) {
        val nm = notifications
        if (nm != null && !nm.isNotificationPolicyAccessGranted) {
            root("cmd notification allow_dnd ${context.packageName}")
        }
        try {
            audio?.ringerMode = mode
        } catch (e: SecurityException) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
        }
    }

    private fun openApp(pkg: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg)
        if (intent == null) {
            Log.w(TAG, "app $pkg is not installed")
            return
        }
        startActivity(intent)
    }

    private fun openSettings(action: String) = startActivity(Intent(action))

    /** האפליקציה שבחזית (לפני שמרכז הבקרה נפתח). מסך הבית ו-FutureUI עצמה = "מערכת כללית". */
    private fun reportBug() {
        val fg = com.future.futureui.utils.FutureUIState.foregroundPackage
        val about = fg?.takeIf { it.startsWith("com.future.") && it != context.packageName && it != "com.future.futurelauncher" }
        startActivity(com.future.sharednav.feedback.FeedbackShortcut.intent(about))
    }

    private fun startActivity(intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (t: Throwable) {
            Log.w(TAG, "startActivity ${intent.action ?: intent.`package`} failed", t)
        }
    }

    companion object {
        private const val TAG = "GridControls"
        private const val NS_SYSTEM = "system"
        private const val NS_SECURE = "secure"
        private const val NS_GLOBAL = "global"
        private const val LONG_TIMEOUT_MS = 600_000
        private const val DEFAULT_TIMEOUT_MS = 30_000
        private const val QUICK_TIMER_SECONDS = 5 * 60
        private const val SCREEN_RECORD_DIR = "/sdcard/Movies/ScreenRecords"
        private const val KEY_SLEEP_ON = "sleep_on"
        private const val KEY_PREV_DARK = "sleep_prev_dark"
        private const val KEY_PREV_DIM = "sleep_prev_dim"
        private const val KEY_PREV_DND = "sleep_prev_dnd"
    }
}
