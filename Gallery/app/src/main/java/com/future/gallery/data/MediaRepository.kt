package com.future.gallery.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val displayName: String,
    val size: Long,
    val bucketId: String,
    val bucketName: String,
    // תיקיה יחסית (למשל DCIM/Camera/) - מבדילה בין אלבומים שנקראים באותו שם
    val relativePath: String = "",
)

data class Album(
    val bucketId: String,
    val bucketName: String,
    val coverUri: Uri,
    val count: Int,
    val path: String = "",
)

enum class SortOption { DATE_NEWEST, DATE_OLDEST, NAME_AZ, NAME_ZA, SIZE_LARGEST, SIZE_SMALLEST }

fun List<MediaItem>.sortedBy(option: SortOption): List<MediaItem> = when (option) {
    SortOption.DATE_NEWEST -> sortedByDescending { it.dateAdded }
    SortOption.DATE_OLDEST -> sortedBy { it.dateAdded }
    SortOption.NAME_AZ -> sortedWith { a, b -> naturalCompare(a.displayName, b.displayName) }
    SortOption.NAME_ZA -> sortedWith { a, b -> naturalCompare(b.displayName, a.displayName) }
    SortOption.SIZE_LARGEST -> sortedByDescending { it.size }
    SortOption.SIZE_SMALLEST -> sortedBy { it.size }
}

/**
 * השוואת שמות "טבעית": רצפי ספרות מושוונים כמספרים, כך ש-IMG_2 לפני IMG_10.
 * בלי זה המיון הלקסיקוגרפי שם 10 לפני 2.
 */
internal fun naturalCompare(a: String, b: String): Int {
    var i = 0
    var j = 0
    while (i < a.length && j < b.length) {
        if (a[i].isDigit() && b[j].isDigit()) {
            val startA = i
            while (i < a.length && a[i].isDigit()) i++
            val startB = j
            while (j < b.length && b[j].isDigit()) j++
            val numA = a.substring(startA, i).trimStart('0')
            val numB = b.substring(startB, j).trimStart('0')
            if (numA.length != numB.length) return numA.length.compareTo(numB.length)
            val byValue = numA.compareTo(numB)
            if (byValue != 0) return byValue
        } else {
            val charA = a[i].lowercaseChar()
            val charB = b[j].lowercaseChar()
            if (charA != charB) return charA.compareTo(charB)
            i++
            j++
        }
    }
    return (a.length - i).compareTo(b.length - j)
}

/** גישה אמיתית ל-MediaStore של אנדרואיד - התמונות שבמכשיר. */
class MediaRepository(private val context: Context) {

    fun getAllMedia(): List<MediaItem> {
        // Gallery היא לתמונות בלבד - סרטונים נפתחים מ-Files (נגן הווידאו שם).
        // השאילתה כבר ממוינת DATE_ADDED DESC (ר' queryImages) - בלי מיון שני.
        return queryImages()
    }

    private fun queryImages(): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH
        )
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
                val nameCol = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Images.Media.SIZE)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathCol = cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    result.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else "",
                            size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else "",
                            bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "" else "",
                            relativePath = if (pathCol >= 0) cursor.getString(pathCol) ?: "" else "",
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Error loading images", e)
        }
        return result
    }

    fun hasMediaPermission(): Boolean {
        val perm = if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return context.checkSelfPermission(perm) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun requiredPermission(): String {
        return if (android.os.Build.VERSION.SDK_INT >= 33) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}
