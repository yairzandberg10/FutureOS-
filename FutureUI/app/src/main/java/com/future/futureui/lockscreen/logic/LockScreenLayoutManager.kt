package com.future.futureui.lockscreen.logic

import android.content.Context
import android.content.SharedPreferences

class LockScreenLayoutManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lock_screen_prefs", Context.MODE_PRIVATE)

    // --- נעילת קוד PIN ---
    // כל הלוגיקה יושבת ב-PinStore: HMAC במפתח Android Keystore שלא ניתן
    // לייצוא (במקום SHA-256 בסיבוב אחד שנשבר offline מיידית), השוואה בזמן
    // קבוע, וחסימה מצטברת אחרי חמישה ניסיונות כושלים.
    private val pinStore = PinStore(prefs)

    fun hasPin(): Boolean = pinStore.hasPin()

    fun setPin(pin: String) = pinStore.setPin(pin)

    fun clearPin() = pinStore.clearPin()

    fun verifyPin(pin: String): Boolean = pinStore.verifyPin(pin)

    /** כמה מילישניות נותרו עד שמותר לנסות קוד שוב; 0 כשאין חסימה פעילה. */
    fun remainingPinLockoutMs(): Long = pinStore.remainingLockoutMs()

    fun getClockStyle(): Int = prefs.getInt("clock_style", 0)
    fun saveClockStyle(style: Int) = prefs.edit().putInt("clock_style", style).apply()

    fun getLeftShortcut(): String = prefs.getString("left_shortcut", "phone") ?: "phone"
    fun saveLeftShortcut(shortcut: String) = prefs.edit().putString("left_shortcut", shortcut).apply()

    fun getRightShortcut(): String = prefs.getString("right_shortcut", "camera") ?: "camera"
    fun saveRightShortcut(shortcut: String) = prefs.edit().putString("right_shortcut", shortcut).apply()

    fun getWallpaperPath(): String? = prefs.getString("wallpaper_path", null)
    fun saveWallpaperPath(path: String?) = prefs.edit().putString("wallpaper_path", path).apply()
    
    fun getWidgetIds(): List<String> = prefs.getString("widget_ids", "weather,battery")?.split(",") ?: listOf("weather", "battery")
    fun saveWidgetIds(ids: List<String>) = prefs.edit().putString("widget_ids", ids.joinToString(",")).apply()
}
