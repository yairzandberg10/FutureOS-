package com.future.sharednav.i18n

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.compose.ui.unit.LayoutDirection
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * תרגום ממשק של כל הסוויטה. הטקסטים בקוד נשארים בעברית (עברית היא שפת המקור),
 * ו-tr() מחליף אותם בזמן ריצה לפי שפת המערכת, מתוך assets/i18n/<lang>.json
 * של המודול הזה (נכנס לכל APK צורך). מפתח = הטקסט העברי עצמו; משתנים
 * בתבנית מסומנים {0}, {1}... כשאין תרגום מוחזר המקור - כך שטקסט חסר
 * מופיע בעברית ולא שובר כלום.
 */
object Tr {
    /** שפות שיש להן טבלה ב-assets/i18n. עברית היא המקור ואין לה טבלה. */
    val SUPPORTED = setOf("en", "ar", "ru", "fr", "es", "de", "it", "pt", "uk", "tr", "pl", "zh", "ja")
    private val RTL = setOf("he", "iw", "ar", "fa", "ur", "yi")

    @Volatile private var appContext: Context? = null
    private val tables = ConcurrentHashMap<String, Map<String, String>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** קוד השפה הנוכחי ("iw" הישן מנורמל ל-"he"). */
    val language: String
        get() = Locale.getDefault().language.let { if (it == "iw") "he" else it }

    /** כיוון הפריסה של השפה הנוכחית - מחליף את ה-Rtl הכפוי שהיה בכל מסך. */
    val layoutDirection: LayoutDirection
        get() = if (language in RTL) LayoutDirection.Rtl else LayoutDirection.Ltr

    fun t(he: String): String {
        val lang = language
        if (lang !in SUPPORTED) return he
        return table(lang)[he] ?: he
    }

    private fun table(lang: String): Map<String, String> = tables.getOrPut(lang) {
        val ctx = appContext ?: return emptyMap()
        runCatching {
            val json = JSONObject(ctx.assets.open("i18n/$lang.json").bufferedReader().use { it.readText() })
            HashMap<String, String>(json.length() * 2).apply {
                json.keys().forEach { k -> put(k, json.getString(k)) }
            }
        }.getOrDefault(emptyMap())
    }
}

/** הטקסט [he] בשפת המערכת. */
fun tr(he: String): String = Tr.t(he)

/** כמו tr(), עם משתנים: tr("נשארו {0} דקות", n). */
fun tr(he: String, vararg args: Any?): String {
    var out = Tr.t(he)
    args.forEachIndexed { i, a -> out = out.replace("{$i}", a.toString()) }
    return out
}

/** טוען את Tr עם ה-Context של האפליקציה, כמו StatusBarInsetInstaller. */
class TrInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        context?.let { Tr.init(it) }
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
