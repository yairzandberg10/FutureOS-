package com.future.navigation.data.gtfs

import android.content.Context

/**
 * זוכר את התיבה הגיאוגרפית שיובאה לאחרונה בפועל, כדי שחיפוש תחבורה ציבורית
 * חוזר לאותו אזור (או לאזור שכבר מכוסה) לא יוריד שוב את קובץ ה-GTFS הארצי
 * (~130MB) בכל פעם - רק כשהיעד החדש נופל מחוץ לתיבה שכבר יובאה.
 */
object GtfsImportState {
    private const val PREFS_NAME = "gtfs_import_state"
    private const val KEY_MIN_LAT = "min_lat"
    private const val KEY_MAX_LAT = "max_lat"
    private const val KEY_MIN_LON = "min_lon"
    private const val KEY_MAX_LON = "max_lon"

    // F49: הערכים נשמרים כ-Double מדויק (סיביות כ-Long). Float עיגל אותם ושבר את בדיקת הכיסוי.
    private const val KEY_EXACT_PREFIX = "exact_"

    fun lastImportedBoundingBox(context: Context): BoundingBox? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(KEY_EXACT_PREFIX + KEY_MIN_LAT)) {
            fun exact(key: String) = java.lang.Double.longBitsToDouble(prefs.getLong(KEY_EXACT_PREFIX + key, 0L))
            return BoundingBox(
                minLat = exact(KEY_MIN_LAT),
                maxLat = exact(KEY_MAX_LAT),
                minLon = exact(KEY_MIN_LON),
                maxLon = exact(KEY_MAX_LON)
            )
        }
        // ערכים ישנים שנשמרו כ-Float (לפני התיקון).
        if (!prefs.contains(KEY_MIN_LAT)) return null
        return BoundingBox(
            minLat = prefs.getFloat(KEY_MIN_LAT, 0f).toDouble(),
            maxLat = prefs.getFloat(KEY_MAX_LAT, 0f).toDouble(),
            minLon = prefs.getFloat(KEY_MIN_LON, 0f).toDouble(),
            maxLon = prefs.getFloat(KEY_MAX_LON, 0f).toDouble()
        )
    }

    /** מנקה את האזור שנרשם - קוראים לפני ייבוא שמוחק את הטבלאות. */
    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun recordImportedBoundingBox(context: Context, bbox: BoundingBox) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(KEY_EXACT_PREFIX + KEY_MIN_LAT, java.lang.Double.doubleToRawLongBits(bbox.minLat))
            .putLong(KEY_EXACT_PREFIX + KEY_MAX_LAT, java.lang.Double.doubleToRawLongBits(bbox.maxLat))
            .putLong(KEY_EXACT_PREFIX + KEY_MIN_LON, java.lang.Double.doubleToRawLongBits(bbox.minLon))
            .putLong(KEY_EXACT_PREFIX + KEY_MAX_LON, java.lang.Double.doubleToRawLongBits(bbox.maxLon))
            .putFloat(KEY_MIN_LAT, bbox.minLat.toFloat())
            .putFloat(KEY_MAX_LAT, bbox.maxLat.toFloat())
            .putFloat(KEY_MIN_LON, bbox.minLon.toFloat())
            .putFloat(KEY_MAX_LON, bbox.maxLon.toFloat())
            .apply()
    }
}
