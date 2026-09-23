package com.future.camera.data

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.provider.MediaStore
import android.util.Size

/**
 * התמונות שהמצלמה שמרה (Pictures/Camera) - לתצוגה המקדימה ליד כפתור הצילום
 * ולמציג התמונות האחרונות בתוך האפליקציה. קבצים שהאפליקציה יצרה נקראים
 * בלי הרשאת אחסון (Scoped Storage).
 */
object CameraMedia {
    fun recentPhotos(context: Context, limit: Int = 60): List<Uri> {
        val result = mutableListOf<Uri>()
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media._ID),
                "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?",
                arrayOf("Pictures/Camera%"),
                "${MediaStore.MediaColumns.DATE_ADDED} DESC",
            )?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                while (c.moveToNext() && result.size < limit) {
                    result += ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, c.getLong(idCol))
                }
            }
        } catch (e: Exception) {
            android.util.Log.w("CameraMedia", "recentPhotos failed", e)
        }
        return result
    }

    fun thumbnail(context: Context, uri: Uri, sizePx: Int): Bitmap? = try {
        context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
    } catch (e: Exception) {
        null
    }

    /** התמונה בגודל המסך - לא ברזולוציית החיישן המלאה, שלא תיכנס לזיכרון. */
    fun screenImage(context: Context, uri: Uri, maxPx: Int): Bitmap? = try {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            val w = info.size.width
            val h = info.size.height
            val scale = maxOf(1, maxOf(w, h) / maxPx)
            decoder.setTargetSize(w / scale, h / scale)
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
    } catch (e: Exception) {
        null
    }
}
