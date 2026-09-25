package com.android.sistemui.controlcenter.logic

import android.app.NotificationManager
import android.app.UiModeManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.runtime.mutableStateMapOf
import com.future.sharednav.root.RootShell
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * הקטלוג של רשת האייקונים במרכז הבקרה - עד [GridCatalog.MAX_CONTROLS] פקדים
 * שהמשתמש מוסיף בעצמו במצב עריכה (הרשת ריקה כברירת מחדל). שורות הגלולות
 * הקבועות ממשיכות להשתמש ב-ControlLayoutManager.allAvailableControls.
 *
 * [isToggle] - פקד עם מצב פועל/כבוי אמיתי שנקרא מהמערכת. השאר הם פעולות:
 * פתיחת אפליקציה או מסך הגדרות, או פקודה חד-פעמית. פעולה שפותחת משהו
 * מעל מרכז הבקרה מסומנת ב-[closesPanel] כדי שהפאנל ייסגר ולא יסתיר אותה.
 */
data class GridControl(
    val id: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val isToggle: Boolean,
    val closesPanel: Boolean = !isToggle
)

object GridCatalog {
    const val MAX_CONTROLS = 50

    val all: List<GridControl> = listOf(
        // מצבי מערכת
        GridControl("dark_theme", "ערכת נושא כהה", Icons.Rounded.DarkMode, true),
        GridControl("power_saver", "חיסכון בסוללה", Icons.Rounded.EnergySavingsLeaf, true),
        GridControl("data_saver", "חוסך נתונים", Icons.Rounded.DataSaverOn, true),
        GridControl("auto_sync", "סנכרון אוטומטי", Icons.Rounded.Sync, true),
        GridControl("auto_time", "שעה אוטומטית", Icons.Rounded.AccessTime, true),
        GridControl("heads_up", "התראות קופצות", Icons.Rounded.NotificationsActive, true),
        // קול ורטט
        GridControl("silent", "מצב שקט", Icons.Rounded.NotificationsOff, true),
        GridControl("vibrate", "מצב רטט", Icons.Rounded.Vibration, true),
        GridControl("mute_media", "השתקת מדיה", Icons.Rounded.MusicOff, true),
        GridControl("vibrate_ring", "רטט בצלצול", Icons.Rounded.RingVolume, true),
        GridControl("haptic", "משוב רטט", Icons.Rounded.TouchApp, true),
        GridControl("touch_sounds", "צלילי מקשים", Icons.Rounded.SurroundSound, true),
        GridControl("dial_tones", "צלילי חייגן", Icons.Rounded.Dialpad, true),
        GridControl("mono_audio", "שמע מונו", Icons.Rounded.Hearing, true),
        // מסך
        GridControl("auto_brightness", "בהירות אוטומטית", Icons.Rounded.BrightnessAuto, true),
        GridControl("night_light", "תאורת לילה", Icons.Rounded.Bedtime, true),
        GridControl("extra_dim", "עמעום נוסף", Icons.Rounded.BrightnessLow, true),
        GridControl("long_timeout", "מסך דלוק 10 דק'", Icons.Rounded.Timer, true),
        GridControl("stay_awake", "דלוק בטעינה", Icons.Rounded.Coffee, true),
        GridControl("animations_off", "ללא אנימציות", Icons.Rounded.Animation, true),
        // נגישות
        GridControl("large_text", "טקסט גדול", Icons.Rounded.FormatSize, true),
        GridControl("high_contrast", "ניגודיות גבוהה", Icons.Rounded.Contrast, true),
        GridControl("invert_colors", "היפוך צבעים", Icons.Rounded.InvertColors, true),
        GridControl("grayscale", "גווני אפור", Icons.Rounded.Tonality, true),
        // פעולות
        GridControl("screenshot", "צילום מסך", Icons.Rounded.Screenshot, false),
        GridControl("screen_off", "כיבוי מסך", Icons.Rounded.MobileOff, false),
        GridControl("play_pause", "נגן / השהה", Icons.Rounded.PlayArrow, true, closesPanel = false),
        GridControl("next_track", "שיר הבא", Icons.Rounded.SkipNext, false, closesPanel = false),
        GridControl("prev_track", "שיר קודם", Icons.Rounded.SkipPrevious, false, closesPanel = false),
        GridControl("kill_background", "ניקוי זיכרון", Icons.Rounded.CleaningServices, false, closesPanel = false),
        // אפליקציות FutureOS
        GridControl("app_calculator", "מחשבון", Icons.Rounded.Calculate, false),
        GridControl("app_clock", "שעון", Icons.Rounded.Alarm, false),
        GridControl("app_notes", "פתקים", Icons.Rounded.StickyNote2, false),
        GridControl("app_messages", "הודעות", Icons.Rounded.Sms, false),
        GridControl("app_dialer", "טלפון", Icons.Rounded.Call, false),
        GridControl("app_contacts", "אנשי קשר", Icons.Rounded.Contacts, false),
        GridControl("app_gallery", "גלריה", Icons.Rounded.PhotoLibrary, false),
        GridControl("app_files", "קבצים", Icons.Rounded.Folder, false),
        GridControl("app_navigation", "ניווט", Icons.Rounded.Navigation, false),
        GridControl("app_translate", "תרגום", Icons.Rounded.Translate, false),
        GridControl("app_assistant", "עוזרי", Icons.Rounded.Mic, false),
        GridControl("app_remote", "שלט", Icons.Rounded.SettingsRemote, false),
        GridControl("app_tasks", "משימות", Icons.Rounded.Checklist, false),
        // מסכי הגדרות
        GridControl("set_wifi", "רשתות Wi-Fi", Icons.Rounded.NetworkWifi, false),
        GridControl("set_sound", "הגדרות צליל", Icons.Rounded.Tune, false),
        GridControl("set_display", "הגדרות תצוגה", Icons.Rounded.DisplaySettings, false),
        GridControl("set_storage", "אחסון", Icons.Rounded.Storage, false),
        GridControl("set_apps", "ניהול אפליקציות", Icons.Rounded.Apps, false),
        GridControl("set_accessibility", "נגישות", Icons.Rounded.Accessibility, false),
        GridControl("set_date_time", "תאריך ושעה", Icons.Rounded.Schedule, false),
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
 */
class GridControlManager(private val context: Context) {

    private val resolver: ContentResolver = context.contentResolver
    private val audio by lazy { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager }
    private val power by lazy { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }
    private val uiMode by lazy { context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager }
    private val connectivity by lazy { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager }
    private val notifications by lazy { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null

    /** מצב פועל/כבוי לפי מזהה - נקרא מה-UI, מתעדכן ב-[refresh]. */
    val states = mutableStateMapOf<String, Boolean>()

    fun isOn(id: String): Boolean = states[id] ?: false

    fun refresh(ids: List<String>) {
        val toggles = ids.filter { GridCatalog.get(it)?.isToggle == true }
        if (toggles.isEmpty() || refreshJob?.isActive == true) return
        refreshJob = scope.launch {
            val read = withContext(Dispatchers.IO) { toggles.associateWith { readState(it) } }
            states.putAll(read)
        }
    }

    fun dispose() = scope.cancel()

    fun activate(id: String) {
        val control = GridCatalog.get(id) ?: return
        if (control.isToggle) states[id] = !isOn(id)
        val target = isOn(id)
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

    private fun readState(id: String): Boolean = try {
        when (id) {
            "dark_theme" -> uiMode?.nightMode == UiModeManager.MODE_NIGHT_YES
            "power_saver" -> power?.isPowerSaveMode == true
            "data_saver" -> connectivity?.restrictBackgroundStatus != ConnectivityManager.RESTRICT_BACKGROUND_STATUS_DISABLED
            "auto_sync" -> ContentResolver.getMasterSyncAutomatically()
            "auto_time" -> readInt(NS_GLOBAL, Settings.Global.AUTO_TIME, 1) == 1
            "heads_up" -> readInt(NS_GLOBAL, "heads_up_notifications_enabled", 1) == 1
            "silent" -> audio?.ringerMode == AudioManager.RINGER_MODE_SILENT
            "vibrate" -> audio?.ringerMode == AudioManager.RINGER_MODE_VIBRATE
            "mute_media" -> audio?.isStreamMute(AudioManager.STREAM_MUSIC) == true
            "vibrate_ring" -> readInt(NS_SYSTEM, "vibrate_when_ringing", 0) == 1
            "haptic" -> readInt(NS_SYSTEM, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) == 1
            "touch_sounds" -> readInt(NS_SYSTEM, Settings.System.SOUND_EFFECTS_ENABLED, 1) == 1
            "dial_tones" -> readInt(NS_SYSTEM, Settings.System.DTMF_TONE_WHEN_DIALING, 1) == 1
            "mono_audio" -> readInt(NS_SYSTEM, "master_mono", 0) == 1
            "auto_brightness" -> readInt(NS_SYSTEM, Settings.System.SCREEN_BRIGHTNESS_MODE, 0) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            "night_light" -> readInt(NS_SECURE, "night_display_activated", 0) == 1
            "extra_dim" -> readInt(NS_SECURE, "reduce_bright_colors_activated", 0) == 1
            "long_timeout" -> readInt(NS_SYSTEM, Settings.System.SCREEN_OFF_TIMEOUT, 30_000) >= LONG_TIMEOUT_MS
            "stay_awake" -> readInt(NS_GLOBAL, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0
            "animations_off" -> readFloat(NS_GLOBAL, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
            "large_text" -> readFloat(NS_SYSTEM, Settings.System.FONT_SCALE, 1f) > 1.05f
            "high_contrast" -> readInt(NS_SECURE, "high_text_contrast_enabled", 0) == 1
            "invert_colors" -> readInt(NS_SECURE, "accessibility_display_inversion_enabled", 0) == 1
            "grayscale" -> readInt(NS_SECURE, "accessibility_display_daltonizer_enabled", 0) == 1 &&
                readInt(NS_SECURE, "accessibility_display_daltonizer", -1) == 0
            "play_pause" -> audio?.isMusicActive == true
            else -> false
        }
    } catch (t: Throwable) {
        Log.w(TAG, "read $id failed", t)
        false
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
            "dark_theme" -> if (!root("cmd uimode night ${if (on) "yes" else "no"}")) openSettings(Settings.ACTION_DISPLAY_SETTINGS)
            "power_saver" -> if (!root("cmd power set-mode $v") && !root("settings put global low_power $v")) {
                openSettings(Settings.ACTION_BATTERY_SAVER_SETTINGS)
            }
            "data_saver" -> if (!root("cmd netpolicy set restrict-background $on")) openSettings(Settings.ACTION_DATA_USAGE_SETTINGS)
            "auto_sync" -> ContentResolver.setMasterSyncAutomatically(on)
            "auto_time" -> root("settings put global ${Settings.Global.AUTO_TIME} $v")
            "heads_up" -> root("settings put global heads_up_notifications_enabled $v")
            "silent" -> setRinger(if (on) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL)
            "vibrate" -> setRinger(if (on) AudioManager.RINGER_MODE_VIBRATE else AudioManager.RINGER_MODE_NORMAL)
            "mute_media" -> audio?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                if (on) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE,
                0
            )
            "vibrate_ring" -> putSystem("vibrate_when_ringing", v)
            "haptic" -> putSystem(Settings.System.HAPTIC_FEEDBACK_ENABLED, v)
            "touch_sounds" -> {
                putSystem(Settings.System.SOUND_EFFECTS_ENABLED, v)
                if (on) audio?.loadSoundEffects() else audio?.unloadSoundEffects()
            }
            "dial_tones" -> putSystem(Settings.System.DTMF_TONE_WHEN_DIALING, v)
            "mono_audio" -> root("settings put system master_mono $v")
            "auto_brightness" -> putSystem(
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (on) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            "night_light" -> root("settings put secure night_display_activated $v")
            "extra_dim" -> root("settings put secure reduce_bright_colors_activated $v")
            "long_timeout" -> putSystem(Settings.System.SCREEN_OFF_TIMEOUT, if (on) LONG_TIMEOUT_MS else DEFAULT_TIMEOUT_MS)
            "stay_awake" -> root("settings put global ${Settings.Global.STAY_ON_WHILE_PLUGGED_IN} ${if (on) 7 else 0}")
            "animations_off" -> {
                val scale = if (on) "0" else "1"
                root(
                    "settings put global window_animation_scale $scale; " +
                        "settings put global transition_animation_scale $scale; " +
                        "settings put global animator_duration_scale $scale"
                )
            }
            "large_text" -> root("settings put system font_scale ${if (on) "1.3" else "1.0"}")
            "high_contrast" -> root("settings put secure high_text_contrast_enabled $v")
            "invert_colors" -> root("settings put secure accessibility_display_inversion_enabled $v")
            "grayscale" -> root(
                if (on) "settings put secure accessibility_display_daltonizer 0; settings put secure accessibility_display_daltonizer_enabled 1"
                else "settings put secure accessibility_display_daltonizer_enabled 0"
            )

            // הפאנל נסגר קודם ([GridControl.closesPanel]) - מחכים שייעלם מהמסך
            "screenshot" -> { SystemClock.sleep(700); root("input keyevent ${KeyEvent.KEYCODE_SYSRQ}") }
            "screen_off" -> { SystemClock.sleep(300); root("input keyevent ${KeyEvent.KEYCODE_SLEEP}") }
            "play_pause" -> mediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            "next_track" -> mediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            "prev_track" -> mediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            "kill_background" -> root("am kill-all")

            "app_calculator" -> openApp("com.future.calculator")
            "app_clock" -> openApp("com.future.clock")
            "app_notes" -> openApp("com.future.notes")
            "app_messages" -> openApp("com.future.messages")
            "app_dialer" -> openApp("com.future.dialer")
            "app_contacts" -> openApp("com.future.contact")
            "app_gallery" -> openApp("com.future.gallery")
            "app_files" -> openApp("com.future.files")
            "app_navigation" -> openApp("com.future.navigation")
            "app_translate" -> openApp("com.future.translate")
            "app_assistant" -> openApp("com.future.assistant")
            "app_remote" -> openApp("com.future.remote")
            "app_tasks" -> openApp("com.future.tasks")

            "set_wifi" -> openSettings(Settings.ACTION_WIFI_SETTINGS)
            "set_sound" -> openSettings(Settings.ACTION_SOUND_SETTINGS)
            "set_display" -> openSettings(Settings.ACTION_DISPLAY_SETTINGS)
            "set_storage" -> openSettings(Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
            "set_apps" -> openSettings(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)
            "set_accessibility" -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            "set_date_time" -> openSettings(Settings.ACTION_DATE_SETTINGS)
        }
    }

    private fun root(command: String): Boolean = RootShell.run(command).success

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

    private fun mediaKey(keyCode: Int) {
        val a = audio ?: return
        a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        a.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
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
    }
}
