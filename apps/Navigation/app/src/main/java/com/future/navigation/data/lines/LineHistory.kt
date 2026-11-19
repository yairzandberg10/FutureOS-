package com.future.navigation.data.lines

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * קו שנפתח או נשמר. לא שומרים מזהה נסיעה (הוא משתנה מיום ליום), אלא הקו, הכיוון,
 * התחנה ומיקומה - כדי למצוא נסיעה עדכנית בכל פתיחה ([LineTimes.resolveTrip]).
 */
data class SavedLine(
    val line: String,
    val headsign: String,
    val agency: String?,
    val stopName: String,
    val stopId: String?,
    val lat: Double,
    val lon: Double,
) {
    /** זהות הקו: מספר וכיוון (התחנה לא משנה את הקו). */
    val key: String get() = "$line|${LineLogic.headsignKey(headsign)}"
}

fun LineDeparture.toSaved(): SavedLine =
    SavedLine(line, LineLogic.headsignKey(headsign), agency, stopName, stopId, location.lat, location.lon)

/**
 * קידוד ושינוי רשימות של קווים, בלי Android (נבדק ביחידה). שורה לכל קו, שבעה שדות
 * מופרדים בטאב, ובלי טאב או ירידת שורה בתוך שדה.
 */
internal object LineHistoryCodec {
    const val MAX_RECENT = 10
    private const val FIELDS = 7

    fun encode(lines: List<SavedLine>): String = lines.joinToString("\n") { line ->
        listOf(
            line.line,
            line.headsign,
            line.agency.orEmpty(),
            line.stopName,
            line.stopId.orEmpty(),
            line.lat.toString(),
            line.lon.toString(),
        ).joinToString("\t") { it.replace('\t', ' ').replace('\n', ' ') }
    }

    fun decode(text: String): List<SavedLine> = text.lineSequence().mapNotNull { row ->
        val parts = row.split('\t')
        if (parts.size != FIELDS || parts[0].isBlank()) return@mapNotNull null
        val lat = parts[5].toDoubleOrNull() ?: return@mapNotNull null
        val lon = parts[6].toDoubleOrNull() ?: return@mapNotNull null
        SavedLine(
            line = parts[0],
            headsign = parts[1],
            agency = parts[2].ifBlank { null },
            stopName = parts[3],
            stopId = parts[4].ifBlank { null },
            lat = lat,
            lon = lon,
        )
    }.toList()

    /** מוסיף לראש הרשימה בלי כפילות של אותו קו, ושומר רק [max] אחרונים. */
    fun pushRecent(list: List<SavedLine>, item: SavedLine, max: Int = MAX_RECENT): List<SavedLine> =
        (listOf(item) + list.filterNot { it.key == item.key }).take(max)

    /** מוסיף את הקו אם לא שמור, ומסיר אותו אם שמור. */
    fun toggle(list: List<SavedLine>, item: SavedLine): List<SavedLine> =
        if (list.any { it.key == item.key }) list.filterNot { it.key == item.key } else list + item
}

/**
 * "לאחרונה" ו"מועדפים" של קווים, נשמרים ב-SharedPreferences ונשארים אחרי סגירת
 * האפליקציה. מקור יחיד לכל המסכים: [get]. הרשימות מתעדכנות ב-StateFlow.
 */
class LineHistory private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val recentState = MutableStateFlow(load(KEY_RECENT))
    private val favoritesState = MutableStateFlow(load(KEY_FAVORITES))

    /** קווים שנפתחו לאחרונה, החדש ראשון, עד 10. */
    val recent: StateFlow<List<SavedLine>> = recentState.asStateFlow()

    /** קווים מועדפים שהמשתמש סימן. */
    val favorites: StateFlow<List<SavedLine>> = favoritesState.asStateFlow()

    fun isFavorite(line: SavedLine): Boolean = favoritesState.value.any { it.key == line.key }

    /** מוסיף למועדפים או מסיר. מחזיר true אם הקו נשמר עכשיו. */
    fun toggleFavorite(line: SavedLine): Boolean {
        val next = LineHistoryCodec.toggle(favoritesState.value, line)
        favoritesState.value = next
        persist(KEY_FAVORITES, next)
        return next.any { it.key == line.key }
    }

    /** רושם שהקו נפתח עכשיו (בראש "לאחרונה"). */
    fun recordOpened(line: SavedLine) {
        val next = LineHistoryCodec.pushRecent(recentState.value, line)
        recentState.value = next
        persist(KEY_RECENT, next)
    }

    fun removeRecent(line: SavedLine) {
        val next = recentState.value.filterNot { it.key == line.key }
        recentState.value = next
        persist(KEY_RECENT, next)
    }

    private fun load(key: String): List<SavedLine> = try {
        LineHistoryCodec.decode(prefs.getString(key, null).orEmpty())
    } catch (e: Exception) {
        emptyList()
    }

    private fun persist(key: String, lines: List<SavedLine>) {
        try {
            prefs.edit().putString(key, LineHistoryCodec.encode(lines)).apply()
        } catch (e: Exception) {
            // שמירה נכשלה - נשארים עם הרשימה בזיכרון לסשן הנוכחי
        }
    }

    companion object {
        private const val PREFS = "transit_lines"
        private const val KEY_RECENT = "recent"
        private const val KEY_FAVORITES = "favorites"

        @Volatile private var instance: LineHistory? = null

        fun get(context: Context): LineHistory =
            instance ?: synchronized(this) {
                instance ?: LineHistory(context).also { instance = it }
            }
    }
}
