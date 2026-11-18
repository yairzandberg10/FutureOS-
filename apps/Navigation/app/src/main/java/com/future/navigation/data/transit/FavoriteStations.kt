package com.future.navigation.data.transit

import android.content.Context
import com.future.navigation.data.common.LatLng
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** תחנה שמורה: שם והמיקום שלה (ללוח היציאות ולניווט אליה). */
data class FavoriteStation(val name: String, val location: LatLng)

/**
 * תחנות מועדפות לתחבורה ציבורית, נשמרות ב-SharedPreferences. מסך הבית יאסוף
 * את [favorites] (StateFlow) להצגה. מקור יחיד לכל האפליקציה: [get].
 */
class FavoriteStations private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val state = MutableStateFlow(load())

    val favorites: StateFlow<List<FavoriteStation>> = state.asStateFlow()

    fun isFavorite(name: String): Boolean = state.value.any { it.name == name }

    /** מוסיף את התחנה אם לא שמורה, ומסיר אם כן. מחזיר true אם נשמרה. */
    fun toggle(station: FavoriteStation): Boolean {
        val current = state.value
        val saved = !isFavorite(station.name)
        val next = if (saved) current + station else current.filterNot { it.name == station.name }
        state.value = next
        persist(next)
        return saved
    }

    private fun load(): List<FavoriteStation> = try {
        prefs.getString(KEY, null).orEmpty().lineSequence()
            .mapNotNull { line ->
                // שורה לכל תחנה: שם, lat, lon - מופרדים בטאב (שם תחנה לא מכיל טאב)
                val parts = line.split('\t')
                if (parts.size != 3) return@mapNotNull null
                val lat = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                val lon = parts[2].toDoubleOrNull() ?: return@mapNotNull null
                FavoriteStation(parts[0], LatLng(lat, lon))
            }
            .toList()
    } catch (e: Exception) {
        emptyList()
    }

    private fun persist(stations: List<FavoriteStation>) {
        try {
            prefs.edit().putString(
                KEY,
                stations.joinToString("\n") { "${it.name.replace('\t', ' ').replace('\n', ' ')}\t${it.location.lat}\t${it.location.lon}" },
            ).apply()
        } catch (e: Exception) {
            // שמירה נכשלה - נשארים עם הרשימה בזיכרון לסשן הנוכחי
        }
    }

    companion object {
        private const val PREFS = "transit_favorites"
        private const val KEY = "stations"

        @Volatile private var instance: FavoriteStations? = null

        fun get(context: Context): FavoriteStations =
            instance ?: synchronized(this) {
                instance ?: FavoriteStations(context).also { instance = it }
            }
    }
}
