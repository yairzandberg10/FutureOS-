package com.future.wallpapers

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileNotFoundException

/**
 * הקטלוג המובנה לקריאה בלבד, בשביל מסך הנעילה של FutureUI (שאין לו גישה
 * לרשת בכוונה). מוגן בהרשאת החתימה של FutureUI.
 *
 *  - content://com.future.wallpapers.catalog/list        -> id, category
 *  - content://com.future.wallpapers.catalog/thumb/<id>  -> JPEG קטן
 *  - content://com.future.wallpapers.catalog/full/<id>   -> JPEG בגודל המסך
 *
 * רק מזהים מהקטלוג המובנה - אי אפשר לבקש מכאן כתובת שרירותית.
 */
class WallpaperProvider : ContentProvider() {

    override fun onCreate() = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        if (uri.pathSegments.firstOrNull() != "list") return null
        val cursor = MatrixCursor(arrayOf("id", "category"))
        WallpaperCatalog.builtIn.forEach { cursor.addRow(arrayOf(it.id, it.category)) }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw SecurityException("read only")
        val segments = uri.pathSegments
        val kind = segments.getOrNull(0)
        val wallpaper = WallpaperCatalog.builtIn.firstOrNull { it.id == segments.getOrNull(1) }
            ?: throw FileNotFoundException(uri.toString())
        val url = when (kind) {
            "thumb" -> wallpaper.thumbUrl
            "full" -> wallpaper.url
            else -> throw FileNotFoundException(uri.toString())
        }
        val file = ImageLoader.fetchFile(context ?: throw FileNotFoundException(), url)
            ?: throw FileNotFoundException("offline")
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun getType(uri: Uri): String? = if (uri.pathSegments.firstOrNull() == "list") null else "image/jpeg"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
}
