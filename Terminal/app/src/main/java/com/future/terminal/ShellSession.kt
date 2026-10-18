package com.future.terminal

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** תקרה לפלט של פקודה אחת - מעבר לה הפלט נחתך והתהליך נעצר (למשל logcat, yes). */
private const val MAX_OUTPUT_CHARS = 100_000

/** פקודה שלא מסתיימת בזמן הזה נהרגת, כדי שלא תרוץ ברקע עם root בלי הגבלה. */
private const val COMMAND_TIMEOUT_MS = 60_000L

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
            // TRM-05: אין קלט מהמשתמש - stdin נסגר, כך שפקודה שקוראת קלט מקבלת EOF ולא נתקעת.
            runCatching { proc.outputStream.close() }
            currentProcess = proc
            var timedOut = false
            // קוראים את stdout ו-stderr בו-זמנית (לא ברצף) - קריאה של הזרם הראשון
            // עד הסוף לפני נגיעה בשני עלולה להיתקע (deadlock) אם שני הצינורות
            // מתמלאים בו-זמנית ברמת מערכת ההפעלה והתהליך נחסם בכתיבה.
            val (out, err) = coroutineScope {
                // שומר זמן: אחרי COMMAND_TIMEOUT_MS התהליך נהרג, והזרמים נסגרים
                val watchdog = launch {
                    delay(COMMAND_TIMEOUT_MS)
                    if (proc.isAlive) {
                        timedOut = true
                        proc.destroyForcibly()
                    }
                }
                val outDeferred = async { readCapped(proc, proc.inputStream) }
                val errDeferred = async { readCapped(proc, proc.errorStream) }
                val pair = outDeferred.await() to errDeferred.await()
                watchdog.cancel()
                pair
            }
            val exit = proc.waitFor()
            val text = out + err
            when {
                cancelled -> text + "^C (בוטל)"
                timedOut -> text + "\n[הפקודה נעצרה: עברו יותר מ-${COMMAND_TIMEOUT_MS / 1000} שניות]"
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

    /** קורא זרם עד הסוף, אבל לא מעל MAX_OUTPUT_CHARS - מעבר לזה הפלט נחתך והתהליך נהרג. */
    private fun readCapped(proc: Process, stream: java.io.InputStream): String {
        val sb = StringBuilder()
        BufferedReader(InputStreamReader(stream)).use { reader ->
            val buf = CharArray(8192)
            while (true) {
                val n = reader.read(buf)
                if (n < 0) break
                val room = MAX_OUTPUT_CHARS - sb.length
                if (n > room) {
                    sb.append(buf, 0, room)
                    sb.append("\n[הפלט נחתך]")
                    proc.destroyForcibly()
                    break
                }
                sb.append(buf, 0, n)
            }
        }
        return sb.toString()
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
