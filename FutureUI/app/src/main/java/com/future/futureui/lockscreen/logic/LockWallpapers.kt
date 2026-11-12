package com.future.futureui.lockscreen.logic

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import java.io.File

/** רקע אחד מהקטלוג המובנה של אפליקציית הטפטים. */
data class LockWallpaper(val id: String, val category: String)

/**
 * הרקעים של מסך הנעילה. FutureUI בכוונה בלי גישה לרשת, אז הקטלוג והתמונות
 * מגיעים מ-WallpaperProvider של אפליקציית הטפטים (מוגן בהרשאת החתימה שלנו).
 * הרקע שנבחר נשמר כקובץ פרטי - לא נוגעים בטפט של המערכת.
 *
 * כל הפונקציות כאן חוסמות - לקרוא מחוץ ל-Main.
 */
object LockWallpapers {
    private const val AUTHORITY = "content://com.future.wallpapers.catalog"
    private const val FILE = "lock_wallpaper.jpg"
    private const val PENDING = "lock_wallpaper_pending.jpg"

    private val thumbs = object : LruCache<String, Bitmap>(4 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun catalog(context: Context): List<LockWallpaper> = runCatching {
        context.contentResolver.query(Uri.parse("$AUTHORITY/list"), null, null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) add(LockWallpaper(c.getString(0), c.getString(1))) }
        }
    }.getOrNull().orEmpty()

    fun peekThumb(id: String): Bitmap? = thumbs.get(id)

    fun thumb(context: Context, id: String): Bitmap? {
        thumbs.get(id)?.let { return it }
        val bmp = decode(context, Uri.parse("$AUTHORITY/thumb/$id"), Bitmap.Config.RGB_565) ?: return null
        thumbs.put(id, bmp)
        return bmp
    }

    /** מוריד את הרקע בגודל מלא לקובץ ממתין. נשמר באמת רק ב-[commit]. */
    fun fetchPending(context: Context, id: String): Bitmap? = runCatching {
        val target = File(context.filesDir, PENDING)
        context.contentResolver.openInputStream(Uri.parse("$AUTHORITY/full/$id"))?.use { input ->
            target.outputStream().use { input.copyTo(it) }
        } ?: return null
        BitmapFactory.decodeFile(target.path)
    }.getOrNull()

    /** סיום עריכה: הרקע הממתין הופך לרקע של מסך הנעילה ("" = הטפט של המכשיר). */
    fun commit(context: Context, id: String) {
        val file = File(context.filesDir, FILE)
        val pending = File(context.filesDir, PENDING)
        if (id.isEmpty()) {
            file.delete()
        } else if (pending.exists()) {
            file.delete()
            pending.renameTo(file)
        }
        pending.delete()
    }

    fun discardPending(context: Context) {
        File(context.filesDir, PENDING).delete()
    }

    /** הרקע של מסך הנעילה: הקובץ שנבחר, אחרת טפט מסך הנעילה של המכשיר, אחרת הטפט הראשי. */
    fun load(context: Context, id: String): Bitmap? {
        if (id.isNotEmpty()) {
            File(context.filesDir, FILE).takeIf { it.exists() }?.let { f -> BitmapFactory.decodeFile(f.path)?.let { return it } }
        }
        return device(context)
    }

    fun device(context: Context): Bitmap? = runCatching {
        val wm = WallpaperManager.getInstance(context)
        wm.getWallpaperFile(WallpaperManager.FLAG_LOCK)?.use { pfd ->
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor)
        } ?: wm.drawable?.let { d ->
            val w = d.intrinsicWidth.coerceAtLeast(1)
            val h = d.intrinsicHeight.coerceAtLeast(1)
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888).also { b ->
                val c = android.graphics.Canvas(b)
                d.setBounds(0, 0, w, h)
                d.draw(c)
            }
        }
    }.getOrNull()

    private fun decode(context: Context, uri: Uri, config: Bitmap.Config): Bitmap? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inPreferredConfig = config })
        }
    }.getOrNull()
}
