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

    suspend fun runCommand(command: String): String {
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
            proc.waitFor()
            (out + err)
        } catch (e: Exception) {
            "שגיאה: ${e.message}"
        } finally {
            currentProcess = null
        }
    }

    /** מבטל את הפקודה הרצה כרגע (אם יש), למשל פקודה תקועה/ללא מענה. */
    fun cancelCurrent() {
        currentProcess?.destroyForcibly()
        currentProcess = null
    }
}
