package com.future.navigation.ui.map

import android.content.Context
import com.future.navigation.ui.settings.MapThemeStore
import java.util.Calendar

/** תצוגת המפה שהמשתמש בחר בהגדרות מפה: אוטומטי (לפי השעה), בהירה או כהה. */
enum class MapTheme(val key: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /** ערך לא מוכר (או חסר) נופל ל-AUTO. */
        fun fromKey(key: String?): MapTheme = entries.firstOrNull { it.key == key } ?: AUTO
    }
}

object MapConfig {
    /**
     * אריחי וקטור חינמיים, בלי מפתח, בלי חשבון, מיועדים לשימוש אמיתי (לא
     * "דמו" כמו אריחי ברירת המחדל של MapLibre עצמה) - OpenFreeMap.
     */
    const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"

    /** אותו ספק, סגנון כהה (בלילה). */
    const val DARK_STYLE_URL = "https://tiles.openfreemap.org/styles/dark"

    /**
     * לילה = אחרי 19:00 או לפני 06:00 (שעון המכשיר). זה הגיבוי המוסכם; חישוב
     * שקיעה אמיתי לא נעשה כאן.
     */
    fun isNight(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val hour = Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
        return hour >= 19 || hour < 6
    }

    /** הסגנון לפי בחירה. ב-AUTO: כהה בלילה, בהיר ביום. */
    fun styleUrl(theme: MapTheme, nowMillis: Long = System.currentTimeMillis()): String = when (theme) {
        MapTheme.LIGHT -> STYLE_URL
        MapTheme.DARK -> DARK_STYLE_URL
        MapTheme.AUTO -> if (isNight(nowMillis)) DARK_STYLE_URL else STYLE_URL
    }

    /** הסגנון לפי ההגדרה השמורה (SharedPreferences). מעדיפים את הגרסה הזו כשיש Context. */
    fun styleUrl(context: Context): String = styleUrl(MapThemeStore.get(context).theme)

    /**
     * בלי Context (NavMapView הקיים): משתמש בבחירה שכבר נטענה. הבחירה נטענת כש-
     * MapThemeStore.get נקרא לראשונה (מסך הבית קורא לו), לפני כן - AUTO.
     */
    fun styleUrl(): String = styleUrl(MapThemeStore.currentTheme())
}
