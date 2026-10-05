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

private val SCRIPT_EXTENSIONS = setOf("sh", "bash", "py", "bat", "cmd", "ps1", "js", "rb", "pl")

/** קובץ שמכיל קוד להרצה - מוצג כטקסט לקריאה בלבד, אף פעם לא מורץ מהקבצים. */
fun isScript(file: File): Boolean = file.extension.lowercase() in SCRIPT_EXTENSIONS

/**
 * האם [file] בתוך האחסון של המשתמש. תיקיות מערכת (data, system, cache, proc...)
 * ונתוני אפליקציות לא נגישים מהקבצים, גם דרך קישור סמלי שמוביל אליהן.
 */
fun isInsideUserStorage(file: File, root: File): Boolean = try {
    val path = file.canonicalPath
    val rootPath = root.canonicalPath
    (path == rootPath || path.startsWith("$rootPath/")) &&
        !path.startsWith("$rootPath/Android/data") && !path.startsWith("$rootPath/Android/obb")
} catch (e: Exception) {
    false
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

/** סדר הרשימה - נבחר בכפתור המיון שבשורה העליונה. תיקיות תמיד לפני קבצים. */
enum class SortOrder(val label: String) { NAME("שם"), DATE("תאריך"), SIZE("גודל"), TYPE("סוג") }

/** התקדמות של העתקה/העברה ארוכה, וביטול שלה מהמסך. */
class CopyProgress {
    @Volatile var total: Long = 1L
    @Volatile var copied: Long = 0L
    @Volatile var cancelled: Boolean = false
        private set
    val fraction: Float get() = (copied.toFloat() / total.coerceAtLeast(1L)).coerceIn(0f, 1f)
    fun cancel() { cancelled = true }
}

/**
 * מצב מפתח (הגדרות > אפשרויות מפתח). בלעדיו הקבצים מציגים רק את מה ששייך
 * למשתמש: בלי תיקיות של אפליקציות (Android, backups, com.xxx), בלי קבצי
 * מערכת (xml, log, apk) ובלי וידאו, שהקבצים לא תומכים בו.
 */
fun isDeveloperMode(context: android.content.Context): Boolean =
    android.provider.Settings.Global.getInt(context.contentResolver, android.provider.Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) == 1

/** תיקיות טכניות שאפליקציות יוצרות - מוסתרות כשמצב מפתח כבוי. */
private val TECHNICAL_FOLDERS = setOf(
    "android", "lost.dir", "backups", "backup", "baidu", "baiduasr", "apkpure", "tencent", "miui",
    "bugreports", "logs", "log", "mtklog", "debuglogger", "data", "obb", "system", "cache", "temp", "tmp",
    "sharedit", "shareit", "amap", "autonavi", "netease", "sogou", "ucdownloads",
)

/** סיומות של קבצים טכניים - מוסתרות כשמצב מפתח כבוי. */
private val TECHNICAL_EXTENSIONS = setOf(
    "xml", "log", "apk", "apks", "xapk", "dex", "so", "bin", "db", "db-journal", "tmp", "ini", "cfg", "conf",
    "prop", "dat", "lock", "bak", "json", "nomedia",
)

/** "com.apkpure.aegon" - תיקייה בשם של חבילה היא תיקיית נתונים של אפליקציה. */
private val PACKAGE_NAME = Regex("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+$")

private fun isTechnical(entry: File): Boolean {
    val lower = entry.name.lowercase()
    return if (entry.isDirectory) lower in TECHNICAL_FOLDERS || PACKAGE_NAME.matches(lower)
    else entry.extension.lowercase() in TECHNICAL_EXTENSIONS || categorize(entry) == FileCategory.VIDEO
}

/** גישה אמיתית למערכת הקבצים של המכשיר - בלי נתונים מדומים. */
class FileRepository {

    fun listDirectory(dir: File, isRoot: Boolean, sort: SortOrder = SortOrder.NAME, developer: Boolean = false): List<FileEntry> {
        return try {
            val entries = (dir.listFiles() ?: emptyArray())
                .filter { entry ->
                    val name = entry.name
                    if (name.startsWith(".")) return@filter false
                    if (isRoot && name in ROOT_HIDDEN_NAMES) return@filter false
                    if (!developer && isTechnical(entry)) return@filter false
                    // קישור שמוביל אל מחוץ לאחסון (למשל אל /data) לא מוצג בכלל.
                    if (entry.isDirectory && !isInsideUserStorage(entry, rootDirectory())) return@filter false
                    true
                }
                .map { FileEntry(it, it.isDirectory, if (it.isFile) it.length() else 0L) }
            // סדר אלפביתי אמיתי (ICU, עברית): עברית לחוד ואנגלית לחוד, כל אחת לפי
            // הא"ב שלה, בלי תלות באותיות גדולות. קודם השמות התערבבו.
            val collator = java.text.Collator.getInstance(java.util.Locale("he")).apply { strength = java.text.Collator.SECONDARY }
            val byName = Comparator<FileEntry> { a, b -> collator.compare(a.file.name, b.file.name) }
            val bySort: Comparator<FileEntry> = when (sort) {
                SortOrder.NAME -> byName
                SortOrder.DATE -> compareByDescending { it.file.lastModified() }
                SortOrder.SIZE -> compareByDescending<FileEntry> { it.sizeBytes }.then(byName)
                SortOrder.TYPE -> compareBy<FileEntry> { it.file.extension.lowercase() }.then(byName)
            }
            entries.sortedWith(compareByDescending<FileEntry> { it.isDirectory }.then(bySort))
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

    /** העתקה עם התקדמות וביטול. ביטול באמצע מוחק את מה שכבר הועתק. */
    fun copyEntry(source: File, destDir: File, progress: CopyProgress = CopyProgress()): Boolean {
        val target = File(destDir, source.name)
        return try {
            if (target.exists() || target.absolutePath.startsWith(source.absolutePath + File.separator)) return false
            progress.total = source.walkTopDown().filter { it.isFile }.sumOf { it.length() }.coerceAtLeast(1L)
            source.walkTopDown().forEach { from ->
                if (progress.cancelled) throw java.util.concurrent.CancellationException()
                val to = File(target, from.relativeTo(source).path)
                if (from.isDirectory) {
                    to.mkdirs()
                } else {
                    to.parentFile?.mkdirs()
                    from.inputStream().use { input ->
                        to.outputStream().use { output ->
                            val buf = ByteArray(64 * 1024)
                            while (true) {
                                if (progress.cancelled) throw java.util.concurrent.CancellationException()
                                val n = input.read(buf)
                                if (n < 0) break
                                output.write(buf, 0, n)
                                progress.copied += n
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            target.deleteRecursively()
            false
        }
    }

    fun moveEntry(source: File, destDir: File, progress: CopyProgress = CopyProgress()): Boolean {
        return try {
            val target = File(destDir, source.name)
            if (target.exists()) return false
            if (source.renameTo(target)) return true
            if (copyEntry(source, destDir, progress)) { source.deleteRecursively(); true } else false
        } catch (e: Exception) {
            false
        }
    }

    /** שמירת קובץ טקסט/קוד מהעורך. כתיבה לקובץ זמני ואז החלפה - בלי קובץ חצי-כתוב. */
    fun saveText(file: File, text: String): Boolean = try {
        val tmp = File(file.parentFile, ".${file.name}.saving")
        tmp.writeText(text)
        if (!tmp.renameTo(file)) { file.writeText(text); tmp.delete() }
        true
    } catch (e: Exception) {
        false
    }

    fun createFolder(parent: File, name: String): Boolean {
        // אותה בדיקה כמו בשינוי שם - "a/b" יצר קודם שתי תיקיות מקוננות בשקט
        if (!isValidEntryName(name)) return false
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
