package com.android.sistemui.statusbar.logic

import android.content.Context
import android.content.SharedPreferences

class StatusBarLayoutManager(context: Context) {
    private val appContext = context.applicationContext
    companion object {
        // גובה שורת המצב הקבועה, בדפ"י - צריך להיות זהה בין השירות (שמגדיר את גובה
        // חלון ה-overlay) לבין כל מסך אחר שצריך padding עליון כדי לא להיחסם על ידה.
        const val HEIGHT_DP = 28
        private const val KEY_PREV_POLICY = "prev_policy_control"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)

    fun getShowBattery(): Boolean = prefs.getBoolean("show_battery", true)
    fun saveShowBattery(value: Boolean) = prefs.edit().putBoolean("show_battery", value).apply()

    fun getShowBluetooth(): Boolean = prefs.getBoolean("show_bluetooth", true)
    fun saveShowBluetooth(value: Boolean) = prefs.edit().putBoolean("show_bluetooth", value).apply()

    // עד שהמשתמש בחר במפורש: הגדרת 12/24 של המערכת, לא "true" קבוע
    fun getUse24HourClock(): Boolean =
        if (prefs.contains("use_24_hour_clock")) prefs.getBoolean("use_24_hour_clock", true)
        else android.text.format.DateFormat.is24HourFormat(appContext)
    fun saveUse24HourClock(value: Boolean) = prefs.edit().putBoolean("use_24_hour_clock", value).apply()

    // האם לבקש מהשירות להסתיר את שורת המצב/ניווט המקורית של אנדרואיד (דורש root)
    fun getSuppressSystemBars(): Boolean = prefs.getBoolean("suppress_system_bars", true)
    fun saveSuppressSystemBars(value: Boolean) = prefs.edit().putBoolean("suppress_system_bars", value).apply()

    // הערך של policy_control לפני ששינינו אותו, כדי לשחזר אותו ולא לדרוס את ערך
    // המשתמש (SU-04). "" = לא היה ערך. נשמר בקובץ ולכן שורד הריגה של התהליך.
    fun hasSavedPolicyControl(): Boolean = prefs.contains(KEY_PREV_POLICY)
    fun getSavedPolicyControl(): String = prefs.getString(KEY_PREV_POLICY, "").orEmpty()
    fun savePolicyControl(value: String?) = prefs.edit().putString(KEY_PREV_POLICY, value.orEmpty()).apply()
    fun clearSavedPolicyControl() = prefs.edit().remove(KEY_PREV_POLICY).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun getBarOpacity(): Float = prefs.getFloat("bar_opacity", 0.55f)
    fun saveBarOpacity(value: Float) = prefs.edit().putFloat("bar_opacity", value).apply()
}
