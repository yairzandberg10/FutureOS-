package com.future.files.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File

/** מטמון תמונות ממוזערות בזיכרון עבור תצוגת ה-preview ברשימת הקבצים - טוען
 * את התמונה בגודל מוקטן (inSampleSize) כדי לא לבזבז זיכרון על bitmap מלא
 * לכל שורה ברשימה. */
object ThumbnailCache {
    private val cache = object : LruCache<String, Bitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    fun get(file: File): Bitmap? = cache.get(cacheKey(file))

    fun decode(file: File, reqSizePx: Int): Bitmap? {
        val key = cacheKey(file)
        cache.get(key)?.let { return it }
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(bounds, reqSizePx, reqSizePx)
            }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, options)
            if (bitmap != null) cache.put(key, bitmap)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun cacheKey(file: File) = "${file.absolutePath}:${file.lastModified()}"

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
