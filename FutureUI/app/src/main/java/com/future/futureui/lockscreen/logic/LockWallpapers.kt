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

    /**
     * רץ בתהליך של מסך הנעילה: קריסה כאן (OOM מתמונה ענקית) מעלימה את מסך הנעילה
     * עד שהשירות עולה מחדש. לכן: רק מול אפליקציית הטפטים האמיתית (אותה חתימה),
     * קובץ עד גודל סביר, ופענוח מוקטן לגודל המסך.
     */
    private fun trusted(context: Context) =
        com.future.sharednav.systemui.TrustedProviders.isTrusted(context, "com.future.wallpapers.catalog")

    private const val MAX_FILE_BYTES = 30L * 1024 * 1024
    private const val MAX_SIDE = 1600

    private fun sampleFor(w: Int, h: Int): Int {
        var sample = 1
        while (maxOf(w, h) / (sample * 2) >= MAX_SIDE) sample *= 2
        return sample
    }

    private fun decodeFileBounded(path: String): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) })
    }

    fun catalog(context: Context): List<LockWallpaper> = runCatching {
        if (!trusted(context)) return emptyList()
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
        if (!trusted(context)) return null
        val target = File(context.filesDir, PENDING)
        context.contentResolver.openInputStream(Uri.parse("$AUTHORITY/full/$id"))?.use { input ->
            target.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    total += n
                    if (total > MAX_FILE_BYTES) { target.delete(); return null }
                    out.write(buf, 0, n)
                }
            }
        } ?: return null
        decodeFileBounded(target.path)
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
            File(context.filesDir, FILE).takeIf { it.exists() }?.let { f -> runCatching { decodeFileBounded(f.path) }.getOrNull()?.let { return it } }
        }
        return device(context)
    }

    fun device(context: Context): Bitmap? = runCatching {
        val wm = WallpaperManager.getInstance(context)
        wm.getWallpaperFile(WallpaperManager.FLAG_LOCK)?.use { pfd ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, bounds)
            android.system.Os.lseek(pfd.fileDescriptor, 0, android.system.OsConstants.SEEK_SET)
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null,
                BitmapFactory.Options().apply { inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight) })
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
        if (!trusted(context)) return null
        context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inPreferredConfig = config })
        }
    }.getOrNull()
}
