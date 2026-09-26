package com.future.terminal

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * מריץ כל פקודה כתהליך su נפרד (לא פותח pty מתמשך) - פשוט ואמין יותר מניהול
 * stdin/stdout זורמים של תהליך יחיד, במחיר שמצב כמו תיקייה נוכחית (cwd) לא
 * נשמר בין פקודות ברמת המעטפת עצמה - האפליקציה עוקבת אחריו ומזריקה `cd` בעצמה.
 */
class ShellSession {
    // תהליך ה-su הפעיל כרגע (אם יש) - חשוף ברמת המחלקה כדי שאפשר יהיה לבטל
    // פקודה תקועה מבחוץ (cancelCurrent) בלי לחכות שהיא תסתיים מעצמה.
    @Volatile
    var currentProcess: Process? = null
        private set

    // מסומן ע"י cancelCurrent - כך פקודה שבוטלה מדווחת "בוטל" ולא "שגיאה: Stream closed"
    // (הזרמים נסגרים באמצע הקריאה כשהתהליך נהרג).
    @Volatile
    private var cancelled = false

    suspend fun runCommand(command: String): String {
        cancelled = false
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            currentProcess = proc
            // קוראים את stdout ו-stderr בו-זמנית (לא ברצף) - קריאה של הזרם הראשון
            // עד הסוף לפני נגיעה בשני עלולה להיתקע (deadlock) אם שני הצינורות
            // מתמלאים בו-זמנית ברמת מערכת ההפעלה והתהליך נחסם בכתיבה.
            val (out, err) = coroutineScope {
                val outDeferred = async {
                    BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
                }
                val errDeferred = async {
                    BufferedReader(InputStreamReader(proc.errorStream)).use { it.readText() }
                }
                outDeferred.await() to errDeferred.await()
            }
            val exit = proc.waitFor()
            val text = out + err
            when {
                cancelled -> text + "^C (בוטל)"
                // פקודה שנכשלה בלי שום פלט (למשל `test -f x`) נראתה בדיוק כמו פקודה
                // שהצליחה - קוד היציאה הוא המידע היחיד שיש.
                exit != 0 && text.isBlank() -> "[קוד יציאה $exit]"
                else -> text
            }
        } catch (e: Exception) {
            if (cancelled) "^C (בוטל)" else "שגיאה: ${e.message}"
        } finally {
            currentProcess = null
        }
    }

    /** מבטל את הפקודה הרצה כרגע (אם יש), למשל פקודה תקועה/ללא מענה. */
    fun cancelCurrent() {
        val proc = currentProcess ?: return
        cancelled = true
        proc.destroyForcibly()
        currentProcess = null
    }
}

/** מצטט מחרוזת למעטפת בגרשיים בודדים - בתוך "..." נתיב עם $, ` או " היה מתפרש/נשבר. */
internal fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
