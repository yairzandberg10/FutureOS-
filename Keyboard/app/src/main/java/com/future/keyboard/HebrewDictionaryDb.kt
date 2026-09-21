package com.future.keyboard

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File
import java.io.IOException

/**
 * מילון עברי מלא (כ-8.6 מיליון מילים - כל הקורפוס cc100, כל בסיס המילון
 * התקני של Hspell בכל הטיה, וכל צירופי התחיליות/כינויי המושא החבורים)
 * גדול מכדי לחיות ב-List/Map בזיכרון על מכשיר חלש, ולכן חי כאן כקובץ
 * SQLite חתום מראש (assets/dict_he.db, כ-8.6 מיליון שורות (digits, rank,
 * word) עם מפתח (digits, rank) - כל שאילתה היא חיפוש מדד אחד, לא סריקה).
 *
 * ה-DB המוטבע ב-APK הוא asset בלבד (לא ניתן לפתוח ישירות ב-SQLite כי הוא
 * עשוי להיות דחוס בתוך ה-APK) - בפתיחה הראשונה הוא מועתק פעם אחת לאחסון
 * הפנימי של האפליקציה, ומשם נפתח לקריאה בלבד.
 */
class HebrewDictionaryDb(context: Context) {

    private val db: SQLiteDatabase

    init {
        val dbFile = context.getDatabasePath(DB_NAME)
        if (!dbFile.exists()) {
            dbFile.parentFile?.mkdirs()
            // מעתיקים לקובץ זמני ומשנים שם רק אחרי שההעתקה הושלמה:
            // הקובץ שוקל כ-345MB, והעתקה שנקטעת באמצע (דיסק מלא,
            // התהליך נהרג) הייתה משאירה קובץ חלקי שנראה תקין - ומאז ואילך
            // כל פתיחה נכשלת בלי שום ניסיון להעתיק שוב.
            val tempFile = File(dbFile.parentFile, "$DB_NAME.tmp")
            try {
                context.assets.open(DB_NAME).use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                if (!tempFile.renameTo(dbFile)) {
                    throw IOException("Failed to move dictionary into place: ${dbFile.path}")
                }
            } catch (e: Throwable) {
                tempFile.delete()
                dbFile.delete()
                throw e
            }
        }
        db = SQLiteDatabase.openDatabase(dbFile.path, null, SQLiteDatabase.OPEN_READONLY)
    }

    /** כל המילים שתואמות לרצף ספרות נתון, ממוינות לפי rank (=סדר עדיפות/תדירות מקורי). */
    fun candidatesFor(digits: String): List<String> {
        val words = mutableListOf<String>()
        db.rawQuery(
            "SELECT word FROM words WHERE digits = ? ORDER BY rank LIMIT $MAX_CANDIDATES",
            arrayOf(digits),
        ).use { cursor ->
            while (cursor.moveToNext()) words.add(cursor.getString(0))
        }
        return words
    }

    companion object {
        private const val DB_NAME = "dict_he.db"

        // תקרה על מספר המועמדים שמוחזרים לרצף ספרות בודד - חלק מהרצפים
        // הקצרים תואמים עשרות אלפי מילים במילון בגודל הזה, ואין תועלת
        // למשתמש בגלילה דרך יותר מכמה עשרות מועמדים.
        private const val MAX_CANDIDATES = 50
    }
}
