package com.future.navigation.ui.map

import java.util.Calendar

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

    fun styleUrl(): String = if (isNight()) DARK_STYLE_URL else STYLE_URL
}
