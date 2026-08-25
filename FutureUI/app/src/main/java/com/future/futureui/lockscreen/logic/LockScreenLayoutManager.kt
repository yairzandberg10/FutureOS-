package com.future.futureui.lockscreen.logic

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest

class LockScreenLayoutManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lock_screen_prefs", Context.MODE_PRIVATE)

    // --- נעילת קוד PIN ---
    // לא שומרים את הקוד עצמו - רק hash (SHA-256 + salt אקראי שנוצר פעם אחת
    // למכשיר), כדי שגם מי שקורא את קובץ ההעדפות לא יראה את הקוד בגלוי.

    fun hasPin(): Boolean = prefs.getString("pin_hash", null) != null

    fun setPin(pin: String) {
        val salt = getOrCreateSalt()
        prefs.edit().putString("pin_hash", hashPin(pin, salt)).apply()
    }

    fun clearPin() {
        prefs.edit().remove("pin_hash").apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString("pin_hash", null) ?: return false
        val salt = getOrCreateSalt()
        return stored == hashPin(pin, salt)
    }

    private fun getOrCreateSalt(): String {
        val existing = prefs.getString("pin_salt", null)
        if (existing != null) return existing
        val salt = java.util.UUID.randomUUID().toString()
        prefs.edit().putString("pin_salt", salt).apply()
        return salt
    }

    private fun hashPin(pin: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((salt + pin).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

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
