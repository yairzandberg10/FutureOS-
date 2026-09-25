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

    fun durationOf(file: File): Long = durations.getOrPut("${file.path}@${file.lastModified()}") {
        runCatching {
            MediaMetadataRetriever().use { r ->
                r.setDataSource(file.path)
                r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            }
        }.getOrDefault(0L)
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
}

/** mm:ss, או h:mm:ss מעל שעה. */
fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
