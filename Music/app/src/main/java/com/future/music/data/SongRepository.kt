package com.future.music.data

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore

/** גישה אמיתית ל-MediaStore.Audio של אנדרואיד - שירים אמיתיים מהמכשיר, בלי
 * DB מקומי (עקבי עם Gallery/data/MediaRepository.kt). אמנים/אלבומים נגזרים
 * ב-client מרשימת השירים השטוחה - אין צורך בטבלאות נפרדות. */
class SongRepository(private val context: Context) {

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
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
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
                            artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "אמן לא ידוע",
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
