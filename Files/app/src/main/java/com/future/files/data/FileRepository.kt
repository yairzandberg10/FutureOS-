package com.future.files.data

import java.io.File

data class FileEntry(val file: File, val isDirectory: Boolean, val sizeBytes: Long)

enum class FileCategory { TEXT, IMAGE, AUDIO, PDF, VIDEO, APK, OTHER }

private val TEXT_EXTENSIONS = setOf(
    "txt", "md", "json", "xml", "csv", "log", "kt", "kts", "java", "py", "js", "ts",
    "html", "htm", "css", "yml", "yaml", "ini", "conf", "cfg", "sh", "c", "cpp", "h",
    "hpp", "gradle", "properties", "toml", "sql", "gitignore", "env"
)
private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "ogg", "m4a", "flac", "aac", "opus")
private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "3gp", "avi", "mov", "m4v")

/** תיקיות שהמשתמש לעולם לא צריך לגעת בהן - נתוני אפליקציות פנימיים, תיקיות
 * גיבוי של המערכת, וכל תיקייה עם נקודה בתחילת השם (מוסתרת מוסכמתית). */
private val ROOT_HIDDEN_NAMES = setOf(
    "Android", "LOST.DIR", "System Volume Information", ".thumbnails", ".trashed", ".stfolder"
)

/** שמות התיקיות הסטנדרטיות שאנדרואיד יוצר באחסון החיצוני, מוצגות למשתמש
 * בעברית. השם האמיתי במערכת הקבצים לא משתנה (רק התצוגה) - כך שאפליקציות
 * אחרות שכותבות/קוראות מהתיקיות האלה לפי השם האנגלי הסטנדרטי (למשל מצלמה
 * שכותבת ל-DCIM) ימשיכו לעבוד כרגיל. */
private val ROOT_FOLDER_DISPLAY_NAMES = mapOf(
    "Download" to "הורדות",
    "Downloads" to "הורדות",
    "DCIM" to "מצלמה",
    "Pictures" to "תמונות",
    "Documents" to "מסמכים",
    "Music" to "מוזיקה",
    "Movies" to "סרטונים",
    "Alarms" to "התראות שעון",
    "Notifications" to "צלילי התראה",
    "Podcasts" to "פודקאסטים",
    "Ringtones" to "רינגטונים",
    "Screenshots" to "צילומי מסך",
    "Audiobooks" to "ספרי שמע",
    "Recordings" to "הקלטות",
    "WhatsApp" to "וואטסאפ",
    "Telegram" to "טלגרם",
    "Bluetooth" to "בלוטות'"
)

/** שם התצוגה של תיקייה: מתורגם לעברית אם זו תיקיית-שורש מוכרת (isTopLevel),
 * אחרת השם המקורי כפי שהוא - תיקיות/קבצים שהמשתמש יצר לא מתורגמים. */
fun File.displayName(isTopLevel: Boolean): String {
    if (isTopLevel) ROOT_FOLDER_DISPLAY_NAMES[name]?.let { return it }
    return name
}

fun categorize(file: File): FileCategory {
    val ext = file.extension.lowercase()
    return when {
        ext == "apk" -> FileCategory.APK
        ext == "pdf" -> FileCategory.PDF
        ext in IMAGE_EXTENSIONS -> FileCategory.IMAGE
        ext in AUDIO_EXTENSIONS -> FileCategory.AUDIO
        ext in VIDEO_EXTENSIONS -> FileCategory.VIDEO
        ext in TEXT_EXTENSIONS || ext.isEmpty() -> FileCategory.TEXT
        else -> FileCategory.OTHER
    }
}

/** גישה אמיתית למערכת הקבצים של המכשיר - בלי נתונים מדומים. */
class FileRepository {

    fun listDirectory(dir: File, isRoot: Boolean): List<FileEntry> {
        return try {
            (dir.listFiles() ?: emptyArray())
                .filter { entry ->
                    val name = entry.name
                    if (name.startsWith(".")) return@filter false
                    if (isRoot && name in ROOT_HIDDEN_NAMES) return@filter false
                    true
                }
                .map { FileEntry(it, it.isDirectory, if (it.isFile) it.length() else 0L) }
                .sortedWith(compareByDescending<FileEntry> { it.isDirectory }.thenBy { it.file.name.lowercase() })
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun rootDirectory(): File = android.os.Environment.getExternalStorageDirectory()

    /** מאנדרואיד 11 ומעלה, גישה לכל הקבצים דורשת הרשאת "ניהול כל הקבצים" ייעודית. */
    fun hasAllFilesAccess(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= 30) {
            android.os.Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun deleteEntry(file: File): Boolean {
        return try {
            file.deleteRecursively()
        } catch (e: Exception) {
            false
        }
    }

    fun renameEntry(file: File, newName: String): Boolean {
        if (!isValidEntryName(newName)) return false
        return try {
            val target = File(file.parentFile, newName)
            if (target.exists()) false else file.renameTo(target)
        } catch (e: Exception) {
            false
        }
    }

    /** מוודא ששם קובץ/תיקייה חדש לא יכול להזיז את הקובץ לנתיב אחר (לא מכיל
     * מפריד תיקיות) ולא שם רזרבי כמו "." או "..". */
    private fun isValidEntryName(name: String): Boolean {
        if (name.isBlank()) return false
        if (name == "." || name == "..") return false
        if (name.contains("/") || name.contains("\\")) return false
        return true
    }

    fun copyEntry(source: File, destDir: File): Boolean {
        return try {
            val target = File(destDir, source.name)
            if (target.exists() || target.absolutePath.startsWith(source.absolutePath + File.separator)) return false
            if (source.isDirectory) source.copyRecursively(target, overwrite = false) else source.copyTo(target, overwrite = false)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun moveEntry(source: File, destDir: File): Boolean {
        return try {
            val target = File(destDir, source.name)
            if (target.exists()) return false
            if (source.renameTo(target)) return true
            if (copyEntry(source, destDir)) { source.deleteRecursively(); true } else false
        } catch (e: Exception) {
            false
        }
    }

    fun createFolder(parent: File, name: String): Boolean {
        return try {
            val target = File(parent, name)
            if (target.exists()) false else target.mkdirs()
        } catch (e: Exception) {
            false
        }
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        var value = bytes.toDouble()
        var unitIndex = 0
        while (value >= 1024 && unitIndex < units.size - 1) {
            value /= 1024
            unitIndex++
        }
        return "%.1f %s".format(value, units[unitIndex])
    }
}
