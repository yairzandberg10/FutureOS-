package com.future.futureui.statusbar.logic

import android.content.Context
import android.content.SharedPreferences

class StatusBarLayoutManager(context: Context) {
    private val appContext = context.applicationContext
    companion object {
        // גובה שורת המצב הקבועה, בדפ"י - צריך להיות זהה בין השירות (שמגדיר את גובה
        // חלון ה-overlay) לבין כל מסך אחר שצריך padding עליון כדי לא להיחסם על ידה.
        const val HEIGHT_DP = 28

        /**
         * גובה חלון שורת המצב בפיקסלים: לפחות HEIGHT_DP, ולפחות גובה שורת המערכת
         * עצמה (ב-Android 12 policy_control לא מסתיר אותה). אם החלון שלנו נמוך ממנה
         * נשאר פס שלה גלוי מתחתינו - שעה ואייקונים כפולים (N-01).
         */
        fun heightPx(context: Context): Int {
            val res = context.resources
            val ours = (HEIGHT_DP * res.displayMetrics.density).toInt()
            val id = res.getIdentifier("status_bar_height", "dimen", "android")
            val system = if (id > 0) res.getDimensionPixelSize(id) else 0
            return maxOf(ours, system)
        }

        // ארבעת סגנונות שורת המצב שאפשר לבחור בהגדרות (ר' עיצוב "FutureOS Status Bar").
        const val STYLE_CLASSIC = "classic"
        const val STYLE_QUIET = "quiet"
        const val STYLE_CAPSULES = "capsules"
        const val STYLE_CENTERED = "centered"
        val STYLES = listOf(STYLE_CLASSIC, STYLE_QUIET, STYLE_CAPSULES, STYLE_CENTERED)
        const val KEY_BAR_STYLE = "bar_style"
        private const val KEY_PREV_POLICY = "prev_policy_control"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences("status_bar_prefs", Context.MODE_PRIVATE)

    fun getShowBattery(): Boolean = prefs.getBoolean("show_battery", true)
    fun saveShowBattery(value: Boolean) = prefs.edit().putBoolean("show_battery", value).apply()

    fun getShowBluetooth(): Boolean = prefs.getBoolean("show_bluetooth", true)
    fun saveShowBluetooth(value: Boolean) = prefs.edit().putBoolean("show_bluetooth", value).apply()

    // עד שהמשתמש בחר במפורש: הגדרת 12/24 של המערכת, לא "true" קבוע (F18 / N-02)
    fun getUse24HourClock(): Boolean =
        if (prefs.contains("use_24_hour_clock")) prefs.getBoolean("use_24_hour_clock", true)
        else android.text.format.DateFormat.is24HourFormat(appContext)
    fun saveUse24HourClock(value: Boolean) = prefs.edit().putBoolean("use_24_hour_clock", value).apply()

    // האם לבקש מהשירות להסתיר את שורת המצב/ניווט המקורית של אנדרואיד (דורש root)
    fun getSuppressSystemBars(): Boolean = prefs.getBoolean("suppress_system_bars", true)
    fun saveSuppressSystemBars(value: Boolean) = prefs.edit().putBoolean("suppress_system_bars", value).apply()

    // הערך של policy_control לפני ששינינו אותו, כדי לשחזר אותו ולא לדרוס את ערך
    // המשתמש (F17). "" = לא היה ערך. נשמר בקובץ ולכן שורד הריגה של התהליך.
    fun hasSavedPolicyControl(): Boolean = prefs.contains(KEY_PREV_POLICY)
    fun getSavedPolicyControl(): String = prefs.getString(KEY_PREV_POLICY, "").orEmpty()
    fun savePolicyControl(value: String?) = prefs.edit().putString(KEY_PREV_POLICY, value.orEmpty()).apply()
    fun clearSavedPolicyControl() = prefs.edit().remove(KEY_PREV_POLICY).apply()

    fun getBarStyle(): String = prefs.getString(KEY_BAR_STYLE, STYLE_CLASSIC)?.takeIf { it in STYLES } ?: STYLE_CLASSIC
    fun saveBarStyle(value: String) = prefs.edit().putString(KEY_BAR_STYLE, value).apply()

    fun registerListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.registerOnSharedPreferenceChangeListener(listener)
    fun unregisterListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) = prefs.unregisterOnSharedPreferenceChangeListener(listener)

    fun getBarOpacity(): Float = prefs.getFloat("bar_opacity", 0.55f)
    fun saveBarOpacity(value: Float) = prefs.edit().putFloat("bar_opacity", value).apply()
}
