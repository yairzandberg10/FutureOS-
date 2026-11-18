package com.future.bluetooth.data

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import java.io.File

data class ReceivedFile(val file: File, val size: Long, val modified: Long) {
    val name: String get() = file.name
}

/**
 * קבצים שהתקבלו בבלוטות'. באנדרואיד 12 שירות ה-OPP של המערכת שומר אותם
 * ישר בתיקיית Download (דרך MediaStore, והבעלים של הרשומה הוא
 * com.android.bluetooth) - לא בתת-תיקייה. לכן מחפשים קודם לפי הבעלים ב-
 * MediaStore, ומוסיפים את התיקיות שבהן גרסאות אחרות שומרות
 * (Download/Bluetooth, bluetooth בשורש האחסון).
 *
 * רשומות של אפליקציה אחרת שאינן מדיה גלויות רק עם "גישה לכל הקבצים"
 * ([hasAccess]); בלי זה הרשימה חלקית.
 */
object ReceivedFiles {
    private const val BLUETOOTH_PACKAGE = "com.android.bluetooth"

    fun hasAccess(): Boolean = Environment.isExternalStorageManager()

    fun load(context: Context): List<ReceivedFile> {
        val found = LinkedHashMap<String, File>()
        runCatching {
            @Suppress("DEPRECATION")
            val projection = arrayOf(MediaStore.MediaColumns.DATA)
            context.contentResolver.query(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                projection,
                "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME}=?",
                arrayOf(BLUETOOTH_PACKAGE),
                null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(0) ?: continue
                    found[path] = File(path)
                }
            }
        }
        val root = Environment.getExternalStorageDirectory()
        listOf(
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Bluetooth"),
            File(root, "bluetooth"),
        ).forEach { dir ->
            dir.listFiles()?.forEach { if (it.isFile) found[it.absolutePath] = it }
        }
        return found.values
            .filter { it.isFile && !it.name.startsWith(".") }
            .map { ReceivedFile(it, it.length(), it.lastModified()) }
            .sortedByDescending { it.modified }
    }

    fun delete(file: ReceivedFile, context: Context): Boolean {
        val ok = file.file.delete()
        if (ok) {
            // הרשומה ב-MediaStore נשארת אחרת, וגלריה/קבצים מציגות קובץ שלא קיים.
            runCatching {
                @Suppress("DEPRECATION")
                context.contentResolver.delete(
                    MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL),
                    "${MediaStore.MediaColumns.DATA}=?",
                    arrayOf(file.file.absolutePath),
                )
            }
        }
        return ok
    }
}
