package com.future.settings.theme

import android.content.ContentValues
import android.content.Context
import android.net.Uri

/**
 * לקוח ל-SystemUiSettingsProvider של FutureUI - שולט ישירות בהגדרות שורת
 * המצב ומסך הנעילה בלי להפנות את המשתמש ל-FutureUI עצמה או לבקש הרשאת
 * נגישות מתוך ההגדרות (FutureUI מוגדרת כ-System UI בפני עצמה).
 */
data class SystemUiSettings(
    val showBattery: Boolean,
    val showBluetooth: Boolean,
    val use24HourClock: Boolean,
    val suppressSystemBars: Boolean,
    val clockStyle: Int
)

object SystemUiSettingsClient {
    private val URI = Uri.parse("content://com.future.futureui.systemui/settings")

    fun get(context: Context): SystemUiSettings {
        return try {
            context.contentResolver.query(URI, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    SystemUiSettings(
                        showBattery = cursor.getInt(cursor.getColumnIndexOrThrow("show_battery")) == 1,
                        showBluetooth = cursor.getInt(cursor.getColumnIndexOrThrow("show_bluetooth")) == 1,
                        use24HourClock = cursor.getInt(cursor.getColumnIndexOrThrow("use_24_hour_clock")) == 1,
                        suppressSystemBars = cursor.getInt(cursor.getColumnIndexOrThrow("suppress_system_bars")) == 1,
                        clockStyle = cursor.getInt(cursor.getColumnIndexOrThrow("clock_style"))
                    )
                } else null
            } ?: SystemUiSettings(true, true, true, true, 0)
        } catch (e: Exception) {
            SystemUiSettings(true, true, true, true, 0)
        }
    }

    fun setShowBattery(context: Context, value: Boolean) = update(context, "show_battery", if (value) 1 else 0)
    fun setShowBluetooth(context: Context, value: Boolean) = update(context, "show_bluetooth", if (value) 1 else 0)
    fun setUse24HourClock(context: Context, value: Boolean) = update(context, "use_24_hour_clock", if (value) 1 else 0)
    fun setSuppressSystemBars(context: Context, value: Boolean) = update(context, "suppress_system_bars", if (value) 1 else 0)
    fun setClockStyle(context: Context, value: Int) = update(context, "clock_style", value)

    private fun update(context: Context, key: String, value: Int) {
        try {
            val values = ContentValues().apply { put(key, value) }
            context.contentResolver.update(URI, values, null, null)
        } catch (e: Exception) {}
    }
}
