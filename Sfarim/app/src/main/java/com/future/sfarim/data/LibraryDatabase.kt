package com.future.sfarim.data

import android.content.Context
import android.util.Log
import org.sqlite.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * פותח את sefaria.db שנוצר מראש (build_library.py, ראה Sfarim/tools) ונדחף למכשיר
 * דרך adb push - לא Room: ה-DB נבנה בפייתון חיצוני, ולוולידציית הסכמה של Room
 * (השוואת hash מול room_master_table) אין דרך אמינה להתאים בלי לייצר את ה-DB
 * דרך Room עצמו. SQLiteDatabase גולמי נמנע מהבעיה הזו לגמרי.
 *
 * ה-SQLiteDatabase כאן הוא *לא* android.database.sqlite אלא org.sqlite.database
 * (SQLite Android Bindings הרשמיים, ר' התלות ב-app/build.gradle.kts). ה-SQLite
 * המובנה של אנדרואיד נבנה בלי מודול FTS5, ולכן כל שאילתת חיפוש נכשלה על
 * המכשיר עם "no such module: fts5" - האינדקסים search_fts/segments_fts שבקובץ
 * פשוט לא היו נגישים. ה-API זהה לחלוטין (rawQuery/execSQL/android.database.Cursor),
 * אז שאר הקוד לא מרגיש בהבדל.
 */
object LibraryDatabase {
    const val DB_FILE_NAME = "sefaria.db"

    private const val TAG = "LibraryDatabase"

    /**
     * ה-Bindings לא טוענים את הספרייה הנייטיבית לבד (בניגוד למחלקה של
     * המערכת) - חייבים לעשות את זה פעם אחת לפני הפתיחה הראשונה.
     */
    private val nativeLibraryLoaded: Boolean by lazy {
        try {
            System.loadLibrary("sqliteX")
            true
        } catch (e: Throwable) {
            Log.e(TAG, "could not load the bundled SQLite (libsqliteX)", e)
            false
        }
    }

    fun expectedPath(context: Context): File =
        File(context.getExternalFilesDir(null), DB_FILE_NAME)

    fun isInstalled(context: Context): Boolean = expectedPath(context).exists()

    /** null אם הקובץ לא נמצא (המשתמש עדיין לא ביצע adb push) - הקורא צריך להציג מסך התקנה. */
    fun openOrNull(context: Context): SQLiteDatabase? {
        val file = expectedPath(context)
        if (!file.exists()) return null
        if (!nativeLibraryLoaded) return null
        return try {
            SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READWRITE)
        } catch (e: Exception) {
            Log.e(TAG, "could not open ${file.path}", e)
            null
        }
    }
}
