package com.future.wallpapers

import com.future.sharednav.i18n.tr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

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
 * עד שהקובץ קיים, ובכל פעם שאין רשת, מוצג הקטלוג המובנה - 91 צילומים אמיתיים
 * (Unsplash דרך picsum.photos, ברישיון חופשי) - ואחרון שנטען מ-Firebase נשמר.
 */
object WallpaperCatalog {
    private const val PREFS = "wallpaper_catalog"
    private const val KEY_CACHE = "firestore"
    private const val TAG = "WallpaperCatalog"
    /** תקרה לעמודי Firestore - 300 מסמכים לעמוד, כלומר עד 3000 רקעים. */
    private const val MaxPages = 10

    /** 91 צילומים מובנים, בגודל המסך (640x960). */
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

    /**
     * מושך את האוסף מ-Firebase, עמוד אחר עמוד (N03). זורק חריגה כשאין רשת או
     * הגדרה. קטלוג ריק לא דורס את המטמון השמור.
     */
    fun fetch(context: Context): List<Wallpaper> {
        val (projectId, apiKey) = config(context) ?: error("firebase.json missing")
        val docs = JSONArray()
        var token: String? = null
        var pages = 0
        do {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/wallpapers?pageSize=300" +
                token?.let { "&pageToken=${URLEncoder.encode(it, "UTF-8")}" }.orEmpty()
            val page = JSONObject(get(url, apiKey))
            page.optJSONArray("documents")?.let { arr -> for (i in 0 until arr.length()) docs.put(arr.get(i)) }
            token = page.optString("nextPageToken").ifBlank { null }
        } while (token != null && ++pages < MaxPages)
        val body = JSONObject().put("documents", docs).toString()
        val list = parseFirestore(body)
        if (list.isNotEmpty()) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_CACHE, body).apply()
        }
        return list
    }

    /**
     * GET ל-Firestore. המפתח נשלח בכותרת ולא בכתובת (W15), דף שגיאה לא נקרא
     * כקטלוג (W16), והחיבור נסגר תמיד.
     */
    private fun get(url: String, apiKey: String): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("x-goog-api-key", apiKey)
            check(connection.responseCode in 200..299) { "Firestore HTTP ${connection.responseCode}" }
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseFirestore(json: String): List<Wallpaper> = try {
        val docs = JSONObject(json).optJSONArray("documents") ?: return emptyList()
        (0 until docs.length()).mapNotNull { i ->
            // W14: מסמך שבור מדולג עם לוג, ולא מפיל את כל הקטלוג
            try {
                parseDoc(docs.getJSONObject(i))
            } catch (e: Exception) {
                Log.w(TAG, "מסמך קטלוג מספר $i דולג", e)
                null
            }
        }.sortedBy { it.order }
    } catch (e: Exception) {
        emptyList()
    }

    private fun parseDoc(doc: JSONObject): Wallpaper? {
        val f = doc.optJSONObject("fields") ?: return null
        fun str(name: String) = f.optJSONObject(name)?.optString("stringValue").orEmpty()
        val url = str("url").ifBlank { return null }
        return Wallpaper(
            id = doc.getString("name").substringAfterLast('/'),
            title = str("title"),
            category = str("category").ifBlank { tr("כללי") },
            url = url,
            thumbUrl = str("thumb").ifBlank { url },
            order = f.optJSONObject("order")?.optString("integerValue")?.toIntOrNull() ?: 0,
        )
    }
}

/** קטגוריה לכל צילום מובנה (לפי מה שבתמונה). בלי צילומים של אנשים - 22, 31, 42,
 * 44, 54, 64, 65, 100, 103 ו-117 הוסרו (אנשים, פנים, ידיים, רגליים, קהל). */
private val BUILT_IN_IDS: List<Pair<Int, String>> = listOf(
    10 to tr("טבע"), 11 to tr("טבע"), 12 to tr("חופים"), 13 to tr("טבע"), 14 to tr("חופים"), 15 to tr("טבע"), 16 to tr("טבע"), 17 to tr("טבע"),
    18 to tr("טבע"), 19 to tr("טבע"), 20 to tr("עירוני"), 21 to tr("עירוני"), 23 to tr("עירוני"), 24 to tr("עירוני"),
    25 to tr("טבע"), 26 to tr("עירוני"), 27 to tr("טבע"), 28 to tr("טבע"), 29 to tr("הרים"), 30 to tr("עירוני"), 
    32 to tr("עירוני"), 33 to tr("טבע"), 34 to tr("עירוני"), 35 to tr("טבע"), 36 to tr("עירוני"), 37 to tr("עירוני"), 38 to tr("עירוני"),
    39 to tr("טבע"), 40 to tr("עירוני"), 41 to tr("עירוני"), 43 to tr("עירוני"), 45 to tr("עירוני"),
    46 to tr("עירוני"), 47 to tr("טבע"), 48 to tr("עירוני"), 49 to tr("עירוני"), 50 to tr("טבע"), 51 to tr("עירוני"), 52 to tr("הרים"),
    53 to tr("טבע"), 55 to tr("עירוני"), 56 to tr("עירוני"), 57 to tr("עירוני"), 58 to tr("חופים"), 59 to tr("עירוני"),
    60 to tr("עירוני"), 61 to tr("עירוני"), 62 to tr("עירוני"), 63 to tr("עירוני"), 
    66 to tr("עירוני"), 67 to tr("טבע"), 68 to tr("עירוני"), 69 to tr("עירוני"), 70 to tr("טבע"), 71 to tr("חופים"), 72 to tr("טבע"),
    73 to tr("עירוני"), 74 to tr("טבע"), 75 to tr("עירוני"), 76 to tr("עירוני"), 77 to tr("עירוני"), 78 to tr("עירוני"), 79 to tr("עירוני"),
    80 to tr("עירוני"), 81 to tr("עירוני"), 82 to tr("עירוני"), 83 to tr("טבע"), 84 to tr("טבע"), 85 to tr("טבע"),
    101 to tr("עירוני"), 102 to tr("טבע"), 104 to tr("טבע"), 106 to tr("טבע"), 107 to tr("עירוני"),
    108 to tr("טבע"), 109 to tr("טבע"), 110 to tr("טבע"), 111 to tr("עירוני"), 112 to tr("טבע"), 113 to tr("עירוני"), 114 to tr("טבע"),
    115 to tr("טבע"), 116 to tr("טבע"), 118 to tr("טבע"), 119 to tr("עירוני"), 120 to tr("טבע"),
    121 to tr("טבע"), 122 to tr("עירוני"), 123 to tr("טבע"), 124 to tr("חופים"), 125 to tr("טבע"),
)

/**
 * טעינת תמונות מהרשת: מטמון בזיכרון לתמונות שעל המסך, ומטמון בדיסק כדי
 * שרקע שכבר נצפה לא יירד שוב.
 */
object ImageLoader {
    private const val MAX_IMAGE_BYTES = 30L * 1024 * 1024

    /** מגבלת הורדות במקביל: גלילה מהירה בין 91 תמונות לא פותחת עשרות חיבורים (W11). */
    private val gate = Semaphore(4)

    /** מנעול לכל כתובת: שתי טעינות של אותה תמונה (רשת + תצוגה מלאה) מחכות להורדה אחת. */
    private val locks = ConcurrentHashMap<String, Any>()

    private val memory = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    /** המפתח כולל את הגודל: אותה כתובת בגודל אחר היא תמונה אחרת (W06). */
    private fun memoryKey(url: String, maxPx: Int) = "$url@$maxPx"

    fun peek(url: String, maxPx: Int): Bitmap? = memory.get(memoryKey(url, maxPx))

    /**
     * מוריד (אם צריך) ומפענח. מתבטל יחד עם ה-coroutine - גלילה מהירה לא משאירה
     * הורדות תלויות עד הזמן הקצוב.
     */
    suspend fun load(context: Context, url: String, maxPx: Int): Bitmap? {
        memory.get(memoryKey(url, maxPx))?.let { return it }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                // הקובץ יכול להימחק בין ההורדה לפענוח (ניקוי מטמון במקביל): מורידים שוב פעם אחת
                repeat(2) {
                    val file = fetchFile(context, url) { !isActive } ?: return@withContext null
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.path, bounds)
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
                    val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
                    if (bitmap != null) {
                        memory.put(memoryKey(url, maxPx), bitmap)
                        return@withContext bitmap
                    }
                    file.delete()
                }
                null
            }
        }
    }

    /**
     * הקובץ במטמון הדיסק - מוריד אם צריך. חוסם (לקרוא מ-IO). null כשאין רשת,
     * או כש-[cancelled] מחזיר true באמצע ההורדה.
     */
    fun fetchFile(context: Context, url: String, cancelled: () -> Boolean = { false }): File? {
        val file = File(File(context.cacheDir, "images").apply { mkdirs() }, sha1(url))
        return synchronized(locks.computeIfAbsent(url) { Any() }) {
            if (file.exists()) {
                // W09: שימוש מעדכן את הזמן - ניקוי המטמון מסיר לפי שימוש אחרון, לא לפי הורדה
                file.setLastModified(System.currentTimeMillis())
                return file
            }
            // הורדה לקובץ זמני ייחודי ואז rename: הורדה שנקטעה (תהליך נהרג, רשת נפלה)
            // השאירה קודם קובץ חתוך בשם הסופי.
            val tmp = File(file.parentFile, "${file.name}.${Thread.currentThread().id}.part")
            try {
                // הכתובות מגיעות מהאוסף ברשת: רק https, ועד גודל סביר - אחרת רשומה אחת
                // (או הפניה) יכלה למלא את כל האחסון של המכשיר.
                if (!url.startsWith("https://")) return null
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 20_000
                    connection.instanceFollowRedirects = true
                    // דף שגיאה (404/500) לא נשמר במטמון כאילו היה תמונה
                    if (connection.responseCode !in 200..299) return null
                    if (connection.url.protocol != "https") return null
                    connection.inputStream.use { input ->
                        tmp.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var total = 0L
                            while (true) {
                                if (cancelled()) return null
                                val n = input.read(buf)
                                if (n < 0) break
                                total += n
                                if (total > MAX_IMAGE_BYTES) return null
                                out.write(buf, 0, n)
                            }
                        }
                    }
                } finally {
                    connection.disconnect()
                }
                if (!tmp.renameTo(file) && !file.exists()) return null
                trimDiskCache(file.parentFile!!)
            } catch (e: Exception) {
                return null
            } finally {
                tmp.delete()
            }
            file
        }
    }

    /** מטמון הדיסק גדל בלי גבול (כל רקע שנצפה); שומרים את ה-[DISK_LIMIT] האחרונים. */
    private fun trimDiskCache(dir: File) {
        val files = dir.listFiles { f -> !f.name.endsWith(".part") } ?: return
        var total = files.sumOf { it.length() }
        if (total <= DISK_LIMIT) return
        for (f in files.sortedBy { it.lastModified() }) {
            if (total <= DISK_LIMIT) break
            total -= f.length()
            f.delete()
        }
    }

    private const val DISK_LIMIT = 60L * 1024 * 1024

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
