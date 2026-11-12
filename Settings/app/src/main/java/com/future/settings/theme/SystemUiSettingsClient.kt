package com.future.settings.theme

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import com.future.sharednav.systemui.SystemUiTarget

/**
 * לקוח ל-SystemUiSettingsProvider של FutureUI - שולט ישירות בהגדרות שורת
 * המצב בלי להפנות את המשתמש ל-FutureUI עצמה או לבקש הרשאת
 * נגישות מתוך ההגדרות (FutureUI מוגדרת כ-System UI בפני עצמה).
 */
data class SystemUiSettings(
    val showBattery: Boolean,
    val showBluetooth: Boolean,
    val use24HourClock: Boolean,
    val suppressSystemBars: Boolean,
    // "classic" / "quiet" / "capsules" / "centered" - ר' StatusBarLayoutManager ב-FutureUI
    val barStyle: String = "classic"
)

object SystemUiSettingsClient {
    private val URI = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}/settings")

    fun get(context: Context): SystemUiSettings {
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    SystemUiSettings(
                        showBattery = cursor.getInt(cursor.getColumnIndexOrThrow("show_battery")) == 1,
                        showBluetooth = cursor.getInt(cursor.getColumnIndexOrThrow("show_bluetooth")) == 1,
                        use24HourClock = cursor.getInt(cursor.getColumnIndexOrThrow("use_24_hour_clock")) == 1,
                        suppressSystemBars = cursor.getInt(cursor.getColumnIndexOrThrow("suppress_system_bars")) == 1,
                        // FutureUI ישנה בלי העמודה - נשארים על הסגנון הקלאסי
                        barStyle = cursor.getColumnIndex("bar_style").takeIf { it >= 0 }?.let { cursor.getString(it) } ?: "classic"
                    )
                } else null
            } ?: SystemUiSettings(true, true, true, true)
        } catch (e: Exception) {
            SystemUiSettings(true, true, true, true)
        }
    }

    fun setShowBattery(context: Context, value: Boolean) = update(context, "show_battery", if (value) 1 else 0)
    fun setShowBluetooth(context: Context, value: Boolean) = update(context, "show_bluetooth", if (value) 1 else 0)
    fun setUse24HourClock(context: Context, value: Boolean) = update(context, "use_24_hour_clock", if (value) 1 else 0)
    fun setSuppressSystemBars(context: Context, value: Boolean) = update(context, "suppress_system_bars", if (value) 1 else 0)

    fun setBarStyle(context: Context, value: String) = update(context, ContentValues().apply { put("bar_style", value) })

    private fun update(context: Context, key: String, value: Int) = update(context, ContentValues().apply { put(key, value) })

    private fun update(context: Context, values: ContentValues) {
        try {
            context.contentResolver.update(URI, values, null, null)
        } catch (e: Exception) {
            android.util.Log.w("SystemUiSettingsClient", "update failed", e)
        }
    }
}
