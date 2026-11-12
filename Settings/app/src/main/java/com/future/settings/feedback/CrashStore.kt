package com.future.settings.feedback

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * דוחות קריסה שהגיעו מאפליקציות FutureOS (CrashReportReceiver) ועוד לא נשלחו כמשוב.
 * נשמרים מקומית בלבד, אחרי סינון פרטים אישיים, עד 10 אחרונים. נמחקים כשהמשתמש שולח או מוחק.
 */
object CrashStore {
    private const val PREFS = "crash_store"
    private const val KEY = "reports"
    private const val MAX_REPORTS = 10

    data class Crash(
        val id: String,
        val pkg: String,
        val versionName: String,
        val versionCode: Long,
        val at: Long,
        val trace: String,
    ) {
        /** השורה הראשונה של החריגה, בלי "thread: ...". מוצגת ברשימה ובהתראה. */
        val headline: String
            get() = trace.lineSequence().drop(1).firstOrNull { it.isNotBlank() }?.trim()?.take(120) ?: ""
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context): JSONArray =
        runCatching { JSONArray(prefs(context).getString(KEY, "[]")) }.getOrDefault(JSONArray())

    /** מוסיף דוח. אותו id פעמיים (שידור חוזר אחרי עלייה) נשמר פעם אחת. מחזיר false על כפילות. */
    @Synchronized
    fun add(context: Context, crash: Crash): Boolean {
        val arr = read(context)
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("id") == crash.id) return false
        val next = JSONArray().put(toJson(crash))
        for (i in 0 until minOf(arr.length(), MAX_REPORTS - 1)) next.put(arr.get(i))
        prefs(context).edit().putString(KEY, next.toString()).commit()
        return true
    }

    fun all(context: Context): List<Crash> {
        val arr = read(context)
        return (0 until arr.length()).mapNotNull { i -> runCatching { fromJson(arr.getJSONObject(i)) }.getOrNull() }
    }

    fun get(context: Context, id: String): Crash? = all(context).firstOrNull { it.id == id }

    /** כמה קריסות של אותה אפליקציה מחכות. משמש להתראה ("נסגרה 3 פעמים"). */
    fun countFor(context: Context, pkg: String): Int = all(context).count { it.pkg == pkg }

    @Synchronized
    fun remove(context: Context, id: String) {
        val arr = read(context)
        val kept = JSONArray()
        for (i in 0 until arr.length()) if (arr.getJSONObject(i).optString("id") != id) kept.put(arr.get(i))
        prefs(context).edit().putString(KEY, kept.toString()).commit()
    }

    private fun toJson(c: Crash) = JSONObject()
        .put("id", c.id).put("pkg", c.pkg).put("version_name", c.versionName)
        .put("version_code", c.versionCode).put("at", c.at).put("trace", c.trace)

    private fun fromJson(o: JSONObject) = Crash(
        id = o.getString("id"),
        pkg = o.getString("pkg"),
        versionName = o.optString("version_name"),
        versionCode = o.optLong("version_code"),
        at = o.optLong("at"),
        trace = o.optString("trace"),
    )
}
