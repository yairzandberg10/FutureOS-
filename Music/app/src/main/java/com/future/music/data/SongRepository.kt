package com.future.music.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** גישה אמיתית ל-MediaStore.Audio של אנדרואיד - שירים אמיתיים מהמכשיר, בלי
 * DB מקומי (עקבי עם Gallery/data/MediaRepository.kt). אמנים/אלבומים נגזרים
 * ב-client מרשימת השירים השטוחה - אין צורך בטבלאות נפרדות. */
class SongRepository(private val context: Context) {

    private val artistPrefs = context.getSharedPreferences("artist_names", Context.MODE_PRIVATE)

    /**
     * שינוי שם אמן - נשמר אצלנו (המקור -> השם החדש) ולא בקבצים עצמם: עריכת
     * תגיות של קובץ ששייך לאפליקציה אחרת דורשת באנדרואיד 12 אישור במסך מגע.
     */
    fun renameArtist(originalName: String, newName: String) {
        val original = originalArtistName(originalName)
        artistPrefs.edit().apply {
            if (newName.isBlank() || newName == original) remove(original) else putString(original, newName.trim())
        }.apply()
    }

    /** השם המקורי שמאחורי שם תצוגה (אם שונה). */
    private fun originalArtistName(displayName: String): String =
        artistPrefs.all.entries.firstOrNull { it.value == displayName }?.key ?: displayName

    private fun displayArtist(raw: String): String = artistPrefs.getString(raw, null) ?: raw

    /**
     * מבקש מ-MediaScanner לסרוק את תיקיות המוזיקה/ההורדות/ההקלטות - קובץ
     * שהורד זה עתה או הועתק לא תמיד נכנס ל-MediaStore מעצמו. השינוי מגיע
     * אחר כך ל-ContentObserver והרשימה מתרעננת.
     */
    fun scanMediaFolders() {
        val root = android.os.Environment.getExternalStorageDirectory()
        val dirs = listOf("Music", "Download", "Recordings", "Podcasts", "Audiobooks", "WhatsApp/Media/WhatsApp Audio")
        val paths = dirs.map { java.io.File(root, it) }.filter { it.isDirectory }.flatMap { dir ->
            dir.walkTopDown().maxDepth(4).filter { it.isFile && it.extension.lowercase() in AUDIO_EXTENSIONS }.map { it.absolutePath }.toList()
        }
        if (paths.isNotEmpty()) {
            android.media.MediaScannerConnection.scanFile(context, paths.toTypedArray(), null, null)
        }
    }

    fun getAllSongs(): List<Song> {
        val result = mutableListOf<Song>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
        )
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                // לא רק IS_MUSIC: הקלטות, פודקאסטים ופורמטים שהסורק לא סימן
                // כמוזיקה (opus/ogg/amr/m4a מהורדות) לא הופיעו בכלל. מסננים
                // רק צלצולים/התראות/שעונים מעורר.
                "${MediaStore.Audio.Media.IS_RINGTONE} = 0 AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0 AND ${MediaStore.Audio.Media.IS_ALARM} = 0 AND ${MediaStore.Audio.Media.DURATION} > 1000",
                null,
                "${MediaStore.Audio.Media.TITLE} ASC",
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    result.add(
                        Song(
                            id = id,
                            uri = uri,
                            title = cursor.getString(titleCol) ?: "לא ידוע",
                            artist = displayArtist(cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "אמן לא ידוע"),
                            album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "אלבום לא ידוע",
                            albumId = cursor.getLong(albumIdCol),
                            durationMs = cursor.getLong(durationCol),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SongRepository", "Error loading songs", e)
        }
        return result
    }

    companion object {
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "ogg", "opus", "flac", "wav", "amr", "3gp", "wma", "mka", "mid", "midi")
    }

    fun hasAudioPermission(): Boolean {
        val perm = requiredPermission()
        return context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_AUDIO
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
