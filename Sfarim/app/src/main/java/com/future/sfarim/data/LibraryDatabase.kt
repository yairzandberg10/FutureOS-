package com.future.sfarim.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * פותח את sefaria.db שנוצר מראש (build_library.py, ראה Sfarim/tools) ונדחף למכשיר
 * דרך adb push - לא Room: ה-DB נבנה בפייתון חיצוני, ולוולידציית הסכמה של Room
 * (השוואת hash מול room_master_table) אין דרך אמינה להתאים בלי לייצר את ה-DB
 * דרך Room עצמו. SQLiteDatabase גולמי נמנע מהבעיה הזו לגמרי.
 */
object LibraryDatabase {
    const val DB_FILE_NAME = "sefaria.db"

    fun expectedPath(context: Context): File =
        File(context.getExternalFilesDir(null), DB_FILE_NAME)

    fun isInstalled(context: Context): Boolean = expectedPath(context).exists()

    /** null אם הקובץ לא נמצא (המשתמש עדיין לא ביצע adb push) - הקורא צריך להציג מסך התקנה. */
    fun openOrNull(context: Context): SQLiteDatabase? {
        val file = expectedPath(context)
        if (!file.exists()) return null
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            null
        }
    }
}
