package com.future.gallery.data

import android.content.Context
import android.net.Uri
import android.provider.MediaStore

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val dateAdded: Long,
    val isVideo: Boolean,
    val displayName: String,
    val size: Long,
    val bucketId: String,
    val bucketName: String
)

data class Album(val bucketId: String, val bucketName: String, val coverUri: Uri, val count: Int)

enum class SortOption { DATE_NEWEST, DATE_OLDEST, NAME_AZ, NAME_ZA, SIZE_LARGEST, SIZE_SMALLEST }

fun List<MediaItem>.sortedBy(option: SortOption): List<MediaItem> = when (option) {
    SortOption.DATE_NEWEST -> sortedByDescending { it.dateAdded }
    SortOption.DATE_OLDEST -> sortedBy { it.dateAdded }
    SortOption.NAME_AZ -> sortedBy { it.displayName.lowercase() }
    SortOption.NAME_ZA -> sortedByDescending { it.displayName.lowercase() }
    SortOption.SIZE_LARGEST -> sortedByDescending { it.size }
    SortOption.SIZE_SMALLEST -> sortedBy { it.size }
}

/** גישה אמיתית ל-MediaStore של אנדרואיד - תמונות וסרטונים אמיתיים מהמכשיר. */
class MediaRepository(private val context: Context) {

    fun getAllMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        items.addAll(queryImages())
        items.addAll(queryVideos())
        return items.sortedByDescending { it.dateAdded }
    }

    fun getAlbums(): List<Album> {
        return getAllMedia()
            .groupBy { it.bucketId }
            .mapNotNull { (bucketId, items) ->
                if (bucketId.isBlank() || items.isEmpty()) null
                else Album(
                    bucketId = bucketId,
                    bucketName = items.first().bucketName,
                    coverUri = items.first().uri,
                    count = items.size
                )
            }
            .sortedByDescending { album -> album.count }
    }

    private fun queryImages(): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
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
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id.toString())
                    result.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            isVideo = false,
                            displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else "",
                            size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else "",
                            bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "" else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Error loading images", e)
        }
        return result
    }

    private fun queryVideos(): List<MediaItem> {
        val result = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.BUCKET_ID,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME
        )
        try {
            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val dateCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val nameCol = cursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME)
                val sizeCol = cursor.getColumnIndex(MediaStore.Video.Media.SIZE)
                val bucketIdCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_ID)
                val bucketNameCol = cursor.getColumnIndex(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val uri = Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                    result.add(
                        MediaItem(
                            id = id,
                            uri = uri,
                            dateAdded = if (dateCol >= 0) cursor.getLong(dateCol) else 0L,
                            isVideo = true,
                            displayName = if (nameCol >= 0) cursor.getString(nameCol) ?: "" else "",
                            size = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L,
                            bucketId = if (bucketIdCol >= 0) cursor.getString(bucketIdCol) ?: "" else "",
                            bucketName = if (bucketNameCol >= 0) cursor.getString(bucketNameCol) ?: "" else ""
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MediaRepository", "Error loading videos", e)
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
