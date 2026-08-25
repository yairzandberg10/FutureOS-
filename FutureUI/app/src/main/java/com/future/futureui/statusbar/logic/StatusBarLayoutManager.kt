package com.future.futureui.statusbar.logic

import android.content.Context
import android.content.SharedPreferences

class StatusBarLayoutManager(context: Context) {
    companion object {
        // גובה שורת המצב הקבועה, בדפ"י - צריך להיות זהה בין השירות (שמגדיר את גובה
        // חלון ה-overlay) לבין כל מסך אחר שצריך padding עליון כדי לא להיחסם על ידה.
        const val HEIGHT_DP = 28
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)

    fun getShowBattery(): Boolean = prefs.getBoolean("show_battery", true)
    fun saveShowBattery(value: Boolean) = prefs.edit().putBoolean("show_battery", value).apply()

    fun getShowBluetooth(): Boolean = prefs.getBoolean("show_bluetooth", true)
    fun saveShowBluetooth(value: Boolean) = prefs.edit().putBoolean("show_bluetooth", value).apply()

    fun getUse24HourClock(): Boolean = prefs.getBoolean("use_24_hour_clock", true)
    fun saveUse24HourClock(value: Boolean) = prefs.edit().putBoolean("use_24_hour_clock", value).apply()

    // האם לבקש מהשירות להסתיר את שורת המצב/ניווט המקורית של אנדרואיד (דורש root)
    fun getSuppressSystemBars(): Boolean = prefs.getBoolean("suppress_system_bars", true)
    fun saveSuppressSystemBars(value: Boolean) = prefs.edit().putBoolean("suppress_system_bars", value).apply()

    fun getBarOpacity(): Float = prefs.getFloat("bar_opacity", 0.55f)
    fun saveBarOpacity(value: Float) = prefs.edit().putFloat("bar_opacity", value).apply()
}
