package com.future.navigation.ui.settings

import android.content.Context
import com.future.navigation.ui.map.MapTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * בחירת תצוגת המפה (אוטומטי / בהירה / כהה), נשמרת ב-SharedPreferences. מקור יחיד
 * לכל האפליקציה דרך [get]. ערך שנשמר נטען פעם אחת לזיכרון.
 */
class MapThemeStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    val themeFlow: StateFlow<MapTheme> = state.asStateFlow()

    val theme: MapTheme get() = state.value

    fun set(theme: MapTheme) {
        state.value = theme
        try {
            prefs.edit().putString(KEY, theme.key).apply()
        } catch (e: Exception) {
            // שמירה נכשלה - הבחירה נשארת בזיכרון לסשן הנוכחי
        }
    }

    private fun load(): MapTheme = try {
        MapTheme.fromKey(prefs.getString(KEY, null))
    } catch (e: Exception) {
        MapTheme.AUTO
    }

    companion object {
        private const val PREFS = "map_settings"
        private const val KEY = "map_theme"

        @Volatile private var instance: MapThemeStore? = null

        fun get(context: Context): MapThemeStore =
            instance ?: synchronized(this) {
                instance ?: MapThemeStore(context).also { instance = it }
            }

        /** הבחירה הנוכחית אם ה-store כבר נטען, אחרת AUTO (ברירת המחדל). */
        fun currentTheme(): MapTheme = instance?.theme ?: MapTheme.AUTO
    }
}
