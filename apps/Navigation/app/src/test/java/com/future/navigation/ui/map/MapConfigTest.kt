package com.future.navigation.ui.map

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/** בחירת תצוגת המפה: בהיר / כהה קבועים, ואוטומטי לפי השעה. */
class MapConfigTest {

    private fun atHour(hour: Int): Long = Calendar.getInstance().apply {
        set(2026, Calendar.NOVEMBER, 17, hour, 30, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Test
    fun lightAndDarkAreFixedRegardlessOfTime() {
        assertEquals(MapConfig.STYLE_URL, MapConfig.styleUrl(MapTheme.LIGHT, atHour(23)))
        assertEquals(MapConfig.STYLE_URL, MapConfig.styleUrl(MapTheme.LIGHT, atHour(12)))
        assertEquals(MapConfig.DARK_STYLE_URL, MapConfig.styleUrl(MapTheme.DARK, atHour(12)))
        assertEquals(MapConfig.DARK_STYLE_URL, MapConfig.styleUrl(MapTheme.DARK, atHour(23)))
    }

    @Test
    fun autoFollowsTheTimeOfDay() {
        assertEquals(MapConfig.DARK_STYLE_URL, MapConfig.styleUrl(MapTheme.AUTO, atHour(20)))
        assertEquals(MapConfig.DARK_STYLE_URL, MapConfig.styleUrl(MapTheme.AUTO, atHour(3)))
        assertEquals(MapConfig.STYLE_URL, MapConfig.styleUrl(MapTheme.AUTO, atHour(12)))
    }

    @Test
    fun unknownStoredValueFallsBackToAuto() {
        assertEquals(MapTheme.AUTO, MapTheme.fromKey(null))
        assertEquals(MapTheme.AUTO, MapTheme.fromKey("bogus"))
        assertEquals(MapTheme.DARK, MapTheme.fromKey("dark"))
        assertEquals(MapTheme.LIGHT, MapTheme.fromKey("light"))
    }
}
