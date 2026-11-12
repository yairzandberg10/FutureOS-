package com.future.calendar.data

import com.future.sharednav.i18n.tr
import android.content.Context

data class Region(val id: String, val displayName: String, val lat: Double, val lon: Double)

val KNOWN_REGIONS = listOf(
    Region("jerusalem", tr("ירושלים"), 31.7683, 35.2137),
    Region("tel_aviv", tr("תל אביב"), 32.0853, 34.7818),
    Region("haifa", tr("חיפה"), 32.7940, 34.9896),
    Region("beer_sheva", tr("באר שבע"), 31.2518, 34.7913),
    Region("eilat", tr("אילת"), 29.5581, 34.9482),
    Region("tzfat", tr("צפת"), 32.9646, 35.4960),
    Region("tiberias", tr("טבריה"), 32.7922, 35.5312),
    Region("ashdod", tr("אשדוד"), 31.8014, 34.6435),
    Region("modiin", tr("מודיעין"), 31.8969, 35.0095),
    Region("bnei_brak", tr("בני ברק"), 32.0807, 34.8338)
)

/** הגדרות ספציפיות לאפליקציית לוח השנה - שמורות ב-SharedPreferences מקומי לאפליקציה. */
object CalendarSettings {
    private const val PREFS_NAME = "calendar_settings"
    private const val KEY_USE_GPS = "use_gps"
    private const val KEY_REGION_ID = "region_id"
    private const val KEY_SHOW_WEATHER = "show_weather"
    private const val KEY_USE_HEBREW_CALENDAR = "use_hebrew_calendar"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getUseGps(context: Context): Boolean = prefs(context).getBoolean(KEY_USE_GPS, false)
    fun setUseGps(context: Context, value: Boolean) { prefs(context).edit().putBoolean(KEY_USE_GPS, value).apply() }

    fun getShowWeather(context: Context): Boolean = prefs(context).getBoolean(KEY_SHOW_WEATHER, true)
    fun setShowWeather(context: Context, value: Boolean) { prefs(context).edit().putBoolean(KEY_SHOW_WEATHER, value).apply() }

    /** תצוגת התאריכים העיקרית (מספרי הימים ברשת החודש, כותרת חודש/שנה) בלוח
     * העברי עם ספרור באותיות (ט״ו וכו') במקום מספור לועזי גרגוריאני. */
    fun getUseHebrewCalendar(context: Context): Boolean = prefs(context).getBoolean(KEY_USE_HEBREW_CALENDAR, false)

    private const val KEY_CALENDAR_KIND = "calendar_kind"

    /** עברי / לועזי / משולב. מי שהדליק בעבר "לוח עברי" ממשיך בעברי; ברירת המחדל משולב. */
    fun getCalendarKind(context: Context): CalendarKind {
        val stored = prefs(context).getString(KEY_CALENDAR_KIND, null)
        stored?.let { name -> CalendarKind.entries.firstOrNull { it.name == name }?.let { return it } }
        return if (getUseHebrewCalendar(context)) CalendarKind.HEBREW else CalendarKind.COMBINED
    }

    fun setCalendarKind(context: Context, kind: CalendarKind) {
        prefs(context).edit().putString(KEY_CALENDAR_KIND, kind.name).apply()
    }

    fun getRegion(context: Context): Region {
        val id = prefs(context).getString(KEY_REGION_ID, null)
        return KNOWN_REGIONS.firstOrNull { it.id == id } ?: KNOWN_REGIONS.first()
    }

    fun setRegionId(context: Context, id: String) { prefs(context).edit().putString(KEY_REGION_ID, id).apply() }
}
