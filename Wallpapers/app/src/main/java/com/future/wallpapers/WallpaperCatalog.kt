package com.future.wallpapers

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** רקע אחד בקטלוג. */
data class Wallpaper(
    val id: String,
    val title: String,
    val category: String,
    /** התמונה המלאה (בגודל המסך). */
    val url: String,
    /** תמונה קטנה לרשת. */
    val thumbUrl: String,
    val order: Int = 0,
)

/**
 * הקטלוג של הרקעים.
 *
 * המקור הוא Firebase: אוסף "wallpapers" ב-Cloud Firestore (נקרא ב-REST, בלי
 * SDK), וכל מסמך בו הוא רקע - title, category, url, thumb, order. הוספת רקע
 * היא הוספת מסמך באוסף (והתמונה ב-Firebase Storage או כל כתובת אחרת), בלי
 * עדכון לאפליקציה. פרטי הפרויקט נקראים מ-assets/firebase.json:
 * {"projectId": "...", "apiKey": "..."}.
 *
 * עד שהקובץ קיים, ובכל פעם שאין רשת, מוצג הקטלוג המובנה - 100 צילומים אמיתיים
 * (Unsplash דרך picsum.photos, ברישיון חופשי) - ואחרון שנטען מ-Firebase נשמר.
 */
object WallpaperCatalog {
    private const val PREFS = "wallpaper_catalog"
    private const val KEY_CACHE = "firestore"

    /** 100 צילומים מובנים, בגודל המסך (640x960). */
    val builtIn: List<Wallpaper> = BUILT_IN_IDS.mapIndexed { index, (pid, category) ->
        Wallpaper(
            id = "picsum-$pid",
            title = "$category ${index + 1}",
            category = category,
            url = "https://picsum.photos/id/$pid/640/960",
            thumbUrl = "https://picsum.photos/id/$pid/200/300",
            order = index,
        )
    }

    private fun config(context: Context): Pair<String, String>? = try {
        val json = JSONObject(context.assets.open("firebase.json").bufferedReader().use { it.readText() })
        json.getString("projectId") to json.getString("apiKey")
    } catch (e: Exception) {
        null
    }

    fun isFirebaseConfigured(context: Context): Boolean = config(context) != null

    fun cached(context: Context): List<Wallpaper> =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_CACHE, null)?.let(::parseFirestore).orEmpty()

    /** מושך את האוסף מ-Firebase. זורק חריגה כשאין רשת או הגדרה. */
    fun fetch(context: Context): List<Wallpaper> {
        val (projectId, apiKey) = config(context) ?: error("firebase.json missing")
        val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/wallpapers?pageSize=300&key=$apiKey"
        val body = (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = 10_000
            readTimeout = 20_000
            inputStream.bufferedReader().use { it.readText() }.also { disconnect() }
        }
        val list = parseFirestore(body)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CACHE, body).apply()
        return list
    }

    private fun parseFirestore(json: String): List<Wallpaper> = try {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        (0 until docs.length()).mapNotNull { i ->
            val doc = docs.getJSONObject(i)
            val f = doc.optJSONObject("fields") ?: return@mapNotNull null
            fun str(name: String) = f.optJSONObject(name)?.optString("stringValue").orEmpty()
            val url = str("url").ifBlank { return@mapNotNull null }
            Wallpaper(
                id = doc.getString("name").substringAfterLast('/'),
                title = str("title"),
                category = str("category").ifBlank { "כללי" },
                url = url,
                thumbUrl = str("thumb").ifBlank { url },
                order = f.optJSONObject("order")?.optString("integerValue")?.toIntOrNull() ?: 0,
            )
        }.sortedBy { it.order }
    } catch (e: Exception) {
        emptyList()
    }
}

/** קטגוריה לכל צילום מובנה (לפי מה שבתמונה). */
private val BUILT_IN_IDS: List<Pair<Int, String>> = listOf(
    10 to "טבע", 11 to "טבע", 12 to "חופים", 13 to "טבע", 14 to "חופים", 15 to "טבע", 16 to "טבע", 17 to "טבע",
    18 to "טבע", 19 to "טבע", 20 to "עירוני", 21 to "עירוני", 22 to "עירוני", 23 to "עירוני", 24 to "עירוני",
    25 to "טבע", 26 to "עירוני", 27 to "טבע", 28 to "טבע", 29 to "הרים", 30 to "עירוני", 31 to "עירוני",
    32 to "עירוני", 33 to "טבע", 34 to "עירוני", 35 to "טבע", 36 to "עירוני", 37 to "עירוני", 38 to "עירוני",
    39 to "טבע", 40 to "עירוני", 41 to "עירוני", 42 to "עירוני", 43 to "עירוני", 44 to "טבע", 45 to "עירוני",
    46 to "עירוני", 47 to "טבע", 48 to "עירוני", 49 to "עירוני", 50 to "טבע", 51 to "עירוני", 52 to "הרים",
    53 to "טבע", 54 to "הרים", 55 to "עירוני", 56 to "עירוני", 57 to "עירוני", 58 to "חופים", 59 to "עירוני",
    60 to "עירוני", 61 to "עירוני", 62 to "עירוני", 63 to "עירוני", 64 to "עירוני", 65 to "עירוני",
    66 to "עירוני", 67 to "טבע", 68 to "עירוני", 69 to "עירוני", 70 to "טבע", 71 to "חופים", 72 to "טבע",
    73 to "עירוני", 74 to "טבע", 75 to "עירוני", 76 to "עירוני", 77 to "עירוני", 78 to "עירוני", 79 to "עירוני",
    80 to "עירוני", 81 to "עירוני", 82 to "עירוני", 83 to "טבע", 84 to "טבע", 85 to "טבע",
    100 to "חופים", 101 to "עירוני", 102 to "טבע", 103 to "חופים", 104 to "טבע", 106 to "טבע", 107 to "עירוני",
    108 to "טבע", 109 to "טבע", 110 to "טבע", 111 to "עירוני", 112 to "טבע", 113 to "עירוני", 114 to "טבע",
    115 to "טבע", 116 to "טבע", 117 to "חופים", 118 to "טבע", 119 to "עירוני", 120 to "טבע",
    121 to "טבע", 122 to "עירוני", 123 to "טבע", 124 to "חופים", 125 to "טבע",
)

/**
 * טעינת תמונות מהרשת: מטמון בזיכרון לתמונות שעל המסך, ומטמון בדיסק כדי
 * שרקע שכבר נצפה לא יירד שוב.
 */
object ImageLoader {
    private val memory = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(url: String): Bitmap? = memory.get(url)

    fun load(context: Context, url: String, maxPx: Int): Bitmap? {
        memory.get(url)?.let { return it }
        val file = File(File(context.cacheDir, "images").apply { mkdirs() }, sha1(url))
        if (!file.exists()) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                connection.disconnect()
            } catch (e: Exception) {
                file.delete()
                return null
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: run {
            file.delete()
            return null
        }
        memory.put(url, bitmap)
        return bitmap
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
