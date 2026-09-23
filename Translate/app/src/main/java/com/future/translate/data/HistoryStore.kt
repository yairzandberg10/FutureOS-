package com.future.translate.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(
    val id: Long,
    val src: String,
    val dst: String,
    val from: String,
    val to: String,
    val saved: Boolean,
    val time: Long,
)

/**
 * היסטוריית התרגומים והשמורים, ב-SharedPreferences כ-JSON - עשרות רשומות
 * קצרות, בלי צורך במסד נתונים. החדשה ביותר ראשונה.
 */
class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("translate_history", Context.MODE_PRIVATE)
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<HistoryEntry>> = _entries.asStateFlow()

    /**
     * רושם תרגום. אותו טקסט באותו זוג שפות מעדכן את הרשומה הקיימת ומעלה
     * אותה לראש, ולא נוסף שוב.
     */
    fun record(src: String, dst: String, from: String, to: String, saved: Boolean = false): HistoryEntry {
        val existing = _entries.value.firstOrNull { it.src == src && it.from == from && it.to == to }
        val entry = HistoryEntry(
            id = existing?.id ?: System.currentTimeMillis(),
            src = src,
            dst = dst,
            from = from,
            to = to,
            saved = saved || existing?.saved == true,
            time = System.currentTimeMillis(),
        )
        val rest = _entries.value.filter { it.id != entry.id }
        // השמורים לא נחתכים; הרגילים נשמרים עד MaxEntries.
        val kept = rest.filter { it.saved } + rest.filter { !it.saved }.take(MaxEntries - 1)
        update(listOf(entry) + kept.sortedByDescending { it.time })
        return entry
    }

    fun find(src: String, from: String, to: String): HistoryEntry? =
        _entries.value.firstOrNull { it.src == src && it.from == from && it.to == to }

    fun setSaved(id: Long, saved: Boolean) {
        update(_entries.value.map { if (it.id == id) it.copy(saved = saved) else it })
    }

    /** "נקה היסטוריה" - השמורים נשארים בלשונית שלהם. */
    fun clearUnsaved() {
        update(_entries.value.filter { it.saved })
    }

    private fun update(list: List<HistoryEntry>) {
        _entries.value = list
        val array = JSONArray()
        list.forEach { e ->
            array.put(
                JSONObject()
                    .put("id", e.id)
                    .put("src", e.src)
                    .put("dst", e.dst)
                    .put("from", e.from)
                    .put("to", e.to)
                    .put("saved", e.saved)
                    .put("time", e.time)
            )
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    private fun load(): List<HistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                HistoryEntry(
                    id = o.getLong("id"),
                    src = o.getString("src"),
                    dst = o.getString("dst"),
                    from = o.getString("from"),
                    to = o.getString("to"),
                    saved = o.optBoolean("saved"),
                    time = o.optLong("time"),
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private companion object {
        const val KEY = "entries"
        const val MaxEntries = 100
    }
}

/** ההעדפות של האפליקציה: זוג השפות האחרון, שפות אחרונות, ומסך ההגדרות. */
class TranslatePrefs(context: Context) {
    private val prefs = context.getSharedPreferences("translate_prefs", Context.MODE_PRIVATE)

    var from: String
        get() = prefs.getString("from", "he") ?: "he"
        set(value) = prefs.edit().putString("from", value).apply()

    var to: String
        get() = prefs.getString("to", "en") ?: "en"
        set(value) = prefs.edit().putString("to", value).apply()

    /** "שמירת היסטוריה" - כבוי: תרגומים לא נרשמים, רק מה שנשמר במפורש. */
    var saveHistory: Boolean
        get() = prefs.getBoolean("save_history", true)
        set(value) = prefs.edit().putBoolean("save_history", value).apply()

    /** "הורדה ב-Wi-Fi בלבד" - מודלי השפה גדולים לחבילת גלישה. */
    var wifiOnly: Boolean
        get() = prefs.getBoolean("wifi_only", false)
        set(value) = prefs.edit().putBoolean("wifi_only", value).apply()

    /** שלוש השפות האחרונות שנבחרו, החדשה ראשונה - הכרטיס "אחרונות" בבורר. */
    var recentLanguages: List<String>
        get() = prefs.getString("recent_langs", "en,ar,ru")!!.split(',').filter { Languages.isSupported(it) }
        set(value) = prefs.edit().putString("recent_langs", value.joinToString(",")).apply()

    fun rememberLanguage(code: String) {
        if (!Languages.isSupported(code)) return
        recentLanguages = (listOf(code) + recentLanguages.filter { it != code }).take(3)
    }
}
