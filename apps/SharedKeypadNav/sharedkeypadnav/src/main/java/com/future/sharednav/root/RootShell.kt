package com.future.sharednav.root

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * מימוש root shell אחד לכל המערכת. לפני זה היו שניים, ושניהם שגויים בדרך
 * אחרת:
 *
 * - `FutureUI/ControlManager.executeRoot` כתב לצינור של shell מתמשך והחזיר
 *   `true` תמיד - בלי להמתין, בלי exit code, ובלי לקרוא את stdout/stderr
 *   כלל, כך שהצינור יכול היה להתמלא ולתקוע את התהליך. מתג שנכשל הראה
 *   "פועל".
 * - `Settings/SystemInteractor.runRootCommands` דווקא החזיר success אמיתי,
 *   אבל מסלול ה-fallback שלו הריץ `Runtime.exec(cmd)` ישירות - בלי shell,
 *   מה שמפצל את הפקודה על רווחים ומשנה את משמעותה (ציטוטים, `|`, `&&`
 *   ומשתנים פשוט אובדים).
 *
 * שני הפרטים ש-`Terminal/ShellSession` כבר עשה נכון נשמרים כאן: stdout
 * ו-stderr נקראים בשני threads מקבילים (אחרת שני הצינורות מתמלאים ונוצר
 * deadlock), ויש timeout שמוודא שפקודה תקועה לא משאירה תהליך su חי לנצח.
 *
 * כל הקריאות חוסמות - להריץ מ-Dispatchers.IO, לא מה-main thread.
 */
object RootShell {

    private const val TAG = "RootShell"

    /** ברירת מחדל נדיבה: פקודות `settings`/`svc` מסתיימות במאות אלפיות שנייה,
     *  אבל `pm`/`am` על מכשיר עמוס יכולות לקחת כמה שניות. */
    const val DEFAULT_TIMEOUT_MS = 10_000L

    /** קוד יציאה סינתטי כש-`su` עצמו לא ניתן להרצה (מכשיר לא rooted, ההרשאה
     *  נדחתה) - מובחן מכישלון אמיתי של הפקודה. */
    const val EXIT_NO_SHELL = -1

    /** קוד יציאה סינתטי כשהפקודה לא הסתיימה בתוך ה-timeout והתהליך נהרג. */
    const val EXIT_TIMEOUT = -2

    data class Result(
        val output: String,
        val error: String,
        val exitCode: Int,
    ) {
        val success: Boolean get() = exitCode == 0
        val isShellUnavailable: Boolean get() = exitCode == EXIT_NO_SHELL
    }

    /**
     * מצטט ארגומנט לשורת פקודה של shell כך שלא יפורש כתחביר: כל ערך שמגיע
     * מבחוץ (שם חבילה, הרשאה, תג שפה) חייב לעבור כאן לפני שהוא משורשר לפקודת
     * root. `pkg; reboot` הופך ל-`'pkg; reboot'` - ארגומנט אחד ולא שתי פקודות.
     */
    fun quote(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"

    private val SAFE_TOKEN = Regex("^[A-Za-z0-9._:/@+=-]{1,255}$")

    /**
     * בדיקה קשיחה לשמות חבילה/הרשאה/תגי שפה: אם יש בערך משהו מחוץ לתווים
     * האלה, זה לא שם חוקי - הפקודה לא תרוץ בכלל (עדיף מלהסתמך רק על ציטוט).
     */
    fun isSafeToken(value: String): Boolean = SAFE_TOKEN.matches(value)

    /** מריץ פקודה אחת כ-root. */
    fun run(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Result =
        run(listOf(command), timeoutMs)

    /**
     * מריץ רצף פקודות ב-su session אחד. ה-exit code הוא של ה-shell, כלומר
     * של הפקודה האחרונה - מי שצריך לדעת על כל פקודה בנפרד יקרא לכאן בנפרד.
     */
    fun run(commands: List<String>, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Result =
        exec(listOf("su"), commands, timeoutMs)

    /**
     * מריץ פקודה דרך `sh -c` בלי root - מסלול ה-fallback הנכון כש-su לא
     * זמין. `sh -c` שומר על משמעות הפקודה (ציטוטים, צינורות, הפניות)
     * בניגוד ל-`Runtime.exec(cmd)` שמפצל אותה על רווחים.
     */
    fun runWithoutRoot(command: String, timeoutMs: Long = DEFAULT_TIMEOUT_MS): Result =
        exec(listOf("sh"), listOf(command), timeoutMs)

    private fun exec(shell: List<String>, commands: List<String>, timeoutMs: Long): Result {
        if (commands.isEmpty()) return Result("", "", 0)
        var process: Process? = null
        try {
            process = ProcessBuilder(shell).start()
            val out = StringBuilder()
            val err = StringBuilder()
            // שני הצינורות נקראים במקביל: אם קוראים רק את אחד מהם, השני
            // מתמלא והתהליך נחסם בכתיבה - ה-deadlock הקלאסי של Process.
            val outReader = drain(process.inputStream, out, "$TAG-out")
            val errReader = drain(process.errorStream, err, "$TAG-err")

            try {
                process.outputStream.bufferedWriter().use { writer ->
                    for (command in commands) {
                        writer.write(command)
                        writer.write("\n")
                    }
                    writer.write("exit\n")
                    writer.flush()
                }
            } catch (e: IOException) {
                // ה-shell מת לפני שהספקנו לכתוב - נופלים לקריאת exit code למטה.
                Log.w(TAG, "Failed writing to shell", e)
            }

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroy()
                outReader.join(READER_JOIN_MS)
                errReader.join(READER_JOIN_MS)
                Log.w(TAG, "Command timed out after ${timeoutMs}ms: ${describe(commands)}")
                return Result(out.toString(), "timeout after ${timeoutMs}ms", EXIT_TIMEOUT)
            }
            outReader.join(READER_JOIN_MS)
            errReader.join(READER_JOIN_MS)
            return Result(out.toString(), err.toString(), process.exitValue())
        } catch (e: Exception) {
            // ה-shell עצמו לא ניתן להרצה (למשל מכשיר לא rooted) - זה לעולם
            // לא יכול להידווח כהצלחה.
            Log.w(TAG, "Shell unavailable for: ${describe(commands)}", e)
            process?.destroy()
            return Result("", e.message ?: "shell unavailable", EXIT_NO_SHELL)
        }
    }

    private const val READER_JOIN_MS = 2_000L

    /**
     * לוג בלי הארגומנטים: רק שם התוכנית של הפקודה הראשונה ("settings") ומספר
     * הפקודות. פקודה מלאה יכולה להכיל ערכים רגישים (שם רשת, DNS פרטי, שם
     * מכשיר), והלוג קריא לכל מי שמחובר ב-adb.
     */
    private fun describe(commands: List<String>): String {
        val first = commands.firstOrNull()?.trim()?.substringBefore(' ').orEmpty()
        return if (commands.size > 1) "$first (+${commands.size - 1} more)" else first
    }

    private fun drain(stream: InputStream, into: StringBuilder, name: String): Thread =
        Thread({
            try {
                stream.bufferedReader().forEachLine { line ->
                    synchronized(into) {
                        into.append(line).append('\n')
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error draining $name", e)
            }
        }, name).apply {
            isDaemon = true
            start()
        }
}
