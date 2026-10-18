package com.future.recorder.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.os.Environment
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/** הקלטה שמורה - קובץ m4a בתיקיית ההקלטות של האפליקציה. */
data class Recording(
    val file: File,
    val name: String,
    val modified: Long,
    val durationMs: Long,
    val sizeBytes: Long,
)

/**
 * ההקלטות נשמרות ב-Android/data/com.future.recorder/files/Recordings - תיקייה
 * של האפליקציה שלא דורשת הרשאת אחסון, ושנמחקת יחד איתה.
 */
object Recordings {
    const val EXTENSION = "m4a"
    private const val BASE_NAME = "הקלטה"

    /** אורך כל קובץ נשמר לפי נתיב+זמן שינוי - MediaMetadataRetriever איטי. */
    private val durations = ConcurrentHashMap<String, Long>()

    fun dir(context: Context): File =
        (context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS) ?: File(context.filesDir, "Recordings"))
            .apply { mkdirs() }

    /** כל ההקלטות, החדשה ראשונה. קורא קבצים - לא לקרוא מה-main thread. */
    fun list(context: Context): List<Recording> =
        dir(context).listFiles { f -> f.isFile && f.extension.equals(EXTENSION, true) && f.length() > 0 }
            .orEmpty()
            .sortedByDescending { it.lastModified() }
            .map { f ->
                Recording(
                    file = f,
                    name = f.nameWithoutExtension,
                    modified = f.lastModified(),
                    durationMs = durationOf(f),
                    sizeBytes = f.length(),
                )
            }

    /** כשל בקריאה לא נשמר במטמון - ניסיון חוזר בקריאה הבאה במקום 0 לנצח. */
    fun durationOf(file: File): Long {
        val key = "${file.path}@${file.lastModified()}"
        durations[key]?.let { return it }
        val duration = runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(file.path)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            }
        }.getOrNull() ?: return 0L
        durations[key] = duration
        return duration
    }

    /** "הקלטה 1", "הקלטה 2"... - המספר הפנוי הבא. */
    fun newFile(context: Context): File {
        val dir = dir(context)
        val used = dir.listFiles().orEmpty().mapNotNull {
            it.nameWithoutExtension.removePrefix("$BASE_NAME ").toIntOrNull()
        }.toSet()
        var n = 1
        while (n in used) n++
        return File(dir, "$BASE_NAME $n.$EXTENSION")
    }

    /** מחזיר את הקובץ החדש, או null אם השם לא חוקי או תפוס. */
    fun rename(file: File, newName: String): File? {
        val clean = newName.trim().replace(Regex("[\\\\/:*?\"<>|]"), "_")
        if (clean.isEmpty()) return null
        val target = File(file.parentFile, "$clean.$EXTENSION")
        if (target.exists() && target != file) return null
        return if (file.renameTo(target)) target else null
    }

    fun delete(file: File): Boolean = file.delete()

    /**
     * R-36: קבצים חלקיים (.part) מהקלטה שנקטעה (התהליך נהרג באמצע). אין בהם סוף
     * קובץ ולכן הם לא ניתנים להשמעה - נמחקים, ומוחזר כמה היו כדי להודיע למשתמש.
     * לקרוא רק כשאין הקלטה פעילה, ולא מה-main thread.
     */
    fun discardInterrupted(context: Context): Int {
        val parts = dir(context).listFiles { f -> f.isFile && f.name.endsWith(".part") }.orEmpty()
        parts.forEach { it.delete() }
        return parts.size
    }
}

/**
 * R-44: שם הקלטה מבודד לתצוגה (FSI...PDI), כך ששם שמערבב אנגלית ועברית לא מתהפך
 * ולא "מושך" איתו גרשיים וסימני פיסוק שמסביבו.
 */
fun isolated(name: String): String = "${Char(0x2068)}$name${Char(0x2069)}"

/** mm:ss, או h:mm:ss מעל שעה. */
fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
