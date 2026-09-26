package com.future.futureui.lockscreen.logic

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings

/**
 * כל ההגדרות של מסך הנעילה - אבטחה (קוד, זיהוי פנים, מתי לנעול) והתאמה
 * אישית (שעון, צבע, רקע, ווידג'טים, קיצורים). השירות, מסך הנעילה וההגדרות
 * רצים באותו תהליך וקוראים את אותו קובץ.
 */
class LockSettings(context: Context) {
    private val appContext = context.applicationContext
    val prefs: SharedPreferences = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val pin = PinStore(prefs)

    // --- אבטחה ---
    var enabled: Boolean
        get() = prefs.getBoolean("enabled", true)
        set(v) = prefs.edit().putBoolean("enabled", v).apply()

    /** כמה זמן אחרי כיבוי המסך הוא ננעל (0 = מיד). */
    var autoLockDelayMs: Long
        get() = prefs.getLong("auto_lock_ms", 0L)
        set(v) = prefs.edit().putLong("auto_lock_ms", v).apply()

    var faceEnabled: Boolean
        get() = prefs.getBoolean("face_enabled", false)
        set(v) = prefs.edit().putBoolean("face_enabled", v).apply()

    /** אחרי זיהוי פנים: להישאר במסך הנעילה (כמו אייפון) או לפתוח מיד (כמו סמסונג). */
    var faceStayOnLock: Boolean
        get() = prefs.getBoolean("face_stay", true)
        set(v) = prefs.edit().putBoolean("face_stay", v).apply()

    /** 0 מחמירה, 1 רגילה, 2 מקלה. */
    var faceSensitivity: Int
        get() = prefs.getInt("face_sensitivity", 1)
        set(v) = prefs.edit().putInt("face_sensitivity", v).apply()

    /** 0 הצג הכל, 1 תוכן רק אחרי זיהוי, 2 הסתר תוכן תמיד. */
    var notificationPrivacy: Int
        get() = prefs.getInt("notif_privacy", 1)
        set(v) = prefs.edit().putInt("notif_privacy", v).apply()

    var ownerMessage: String
        get() = prefs.getString("owner_message", "") ?: ""
        set(v) = prefs.edit().putString("owner_message", v.take(60)).apply()

    // --- התאמה אישית ---
    var clockStyle: Int
        get() = prefs.getInt("clock_style", 0)
        set(v) = prefs.edit().putInt("clock_style", v).apply()

    var clockColor: Int
        get() = prefs.getInt("clock_color", 0)
        set(v) = prefs.edit().putInt("clock_color", v).apply()

    var background: Int
        get() = prefs.getInt("background", 1)
        set(v) = prefs.edit().putInt("background", v).apply()

    var widgets: List<String>
        get() = (prefs.getString("widgets", "battery,alarm,hebdate") ?: "").split(",").let { list ->
            List(WIDGET_SLOTS) { i -> list.getOrNull(i)?.takeIf { it.isNotBlank() } ?: "none" }
        }
        set(v) = prefs.edit().putString("widgets", v.joinToString(",")).apply()

    var leftShortcut: String
        get() = prefs.getString("left_shortcut", "flashlight") ?: "flashlight"
        set(v) = prefs.edit().putString("left_shortcut", v).apply()

    var rightShortcut: String
        get() = prefs.getString("right_shortcut", "camera") ?: "camera"
        set(v) = prefs.edit().putString("right_shortcut", v).apply()

    // --- מתי זיהוי פנים לא מספיק ---
    /**
     * כמו באייפון/סמסונג: אחרי הפעלה מחדש, אחרי 48 שעות בלי קוד, או אחרי 5
     * זיהויים כושלים ברצף - חייבים להזין את הקוד, גם אם הפנים מזוהות.
     */
    fun strongAuthRequired(): Boolean {
        if (!pin.hasPin()) return false
        if (prefs.getInt("strong_boot", -1) != bootCount()) return true
        if (System.currentTimeMillis() - prefs.getLong("strong_time", 0L) > STRONG_AUTH_TIMEOUT_MS) return true
        return prefs.getInt("face_failures", 0) >= MAX_FACE_FAILURES
    }

    /** הקוד הוזן נכון - זיהוי פנים חוזר לעבוד. */
    fun onStrongAuth() {
        prefs.edit()
            .putInt("strong_boot", bootCount())
            .putLong("strong_time", System.currentTimeMillis())
            .putInt("face_failures", 0)
            .apply()
    }

    fun onFaceFailure() {
        prefs.edit().putInt("face_failures", prefs.getInt("face_failures", 0) + 1).apply()
    }

    fun onFaceSuccess() {
        prefs.edit().putInt("face_failures", 0).apply()
    }

    private fun bootCount(): Int =
        runCatching { Settings.Global.getInt(appContext.contentResolver, Settings.Global.BOOT_COUNT) }.getOrDefault(0)

    companion object {
        const val PREFS = "lockscreen2_prefs"
        const val WIDGET_SLOTS = 3
        const val MAX_FACE_FAILURES = 5
        private const val STRONG_AUTH_TIMEOUT_MS = 48L * 60 * 60 * 1000

        val AUTO_LOCK_OPTIONS = listOf(0L, 5_000L, 30_000L, 60_000L, 300_000L)
        fun autoLockLabel(ms: Long): String = when (ms) {
            0L -> "מיד"
            5_000L -> "אחרי 5 שניות"
            30_000L -> "אחרי 30 שניות"
            60_000L -> "אחרי דקה"
            else -> "אחרי 5 דקות"
        }
        val SENSITIVITY_LABELS = listOf("מחמירה", "רגילה", "מקלה")
        val PRIVACY_LABELS = listOf("הצג הכל", "תוכן רק אחרי זיהוי", "הסתר תוכן")
    }
}
