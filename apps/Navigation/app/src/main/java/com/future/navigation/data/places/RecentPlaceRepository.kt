package com.future.navigation.data.places

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * היעדים האחרונים, נשמרים ב-SharedPreferences כ-JSON. לא ב-Room: הטבלאות
 * והמסד נמצאים ב-data/gtfs, ושינוי סכמה שם עלול לקרוס בשדרוג. הנתון קטן
 * (עד 15 שורות) ואינו קריטי - אם לא נשמר, המשתמש פשוט לא רואה את ההיסטוריה.
 */
class RecentPlaceRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _recents = MutableStateFlow(RecentPlaceList.decode(readRaw()))
    val recents: StateFlow<List<RecentPlace>> = _recents.asStateFlow()

    fun add(place: RecentPlace) = update(RecentPlaceList.add(_recents.value, place))

    fun remove(place: RecentPlace) = update(RecentPlaceList.remove(_recents.value, place))

    private fun update(list: List<RecentPlace>) {
        _recents.value = list
        try {
            prefs.edit().putString(KEY_ITEMS, RecentPlaceList.encode(list)).apply()
        } catch (e: Exception) {
            // שמירה שנכשלה: הרשימה בזיכרון עדיין נכונה לסשן הנוכחי
        }
    }

    private fun readRaw(): String? = try {
        prefs.getString(KEY_ITEMS, null)
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val PREFS_NAME = "recent_places"
        const val KEY_ITEMS = "items"
    }
}
