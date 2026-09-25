package com.future.settings.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

/** טפט מובנה אחד - אותם 100 צילומים של אפליקציית הטפטים (Unsplash דרך picsum.photos). */
data class WallpaperPhoto(val id: Int, val title: String) {
    val url get() = "https://picsum.photos/id/$id/640/960"
    val thumbUrl get() = "https://picsum.photos/id/$id/200/300"
}

/** הקטלוג המובנה של אפליקציית Wallpapers (WallpaperCatalog.builtIn) - אותם מזהים ואותן קטגוריות. */
object WallpaperPhotos {
    private const val NATURE = "טבע"
    private const val BEACH = "חופים"
    private const val CITY = "עירוני"
    private const val MOUNTAINS = "הרים"

    private val ids: List<Pair<Int, String>> = listOf(
        10 to NATURE, 11 to NATURE, 12 to BEACH, 13 to NATURE, 14 to BEACH, 15 to NATURE, 16 to NATURE, 17 to NATURE,
        18 to NATURE, 19 to NATURE, 20 to CITY, 21 to CITY, 22 to CITY, 23 to CITY, 24 to CITY,
        25 to NATURE, 26 to CITY, 27 to NATURE, 28 to NATURE, 29 to MOUNTAINS, 30 to CITY, 31 to CITY,
        32 to CITY, 33 to NATURE, 34 to CITY, 35 to NATURE, 36 to CITY, 37 to CITY, 38 to CITY,
        39 to NATURE, 40 to CITY, 41 to CITY, 42 to CITY, 43 to CITY, 44 to NATURE, 45 to CITY,
        46 to CITY, 47 to NATURE, 48 to CITY, 49 to CITY, 50 to NATURE, 51 to CITY, 52 to MOUNTAINS,
        53 to NATURE, 54 to MOUNTAINS, 55 to CITY, 56 to CITY, 57 to CITY, 58 to BEACH, 59 to CITY,
        60 to CITY, 61 to CITY, 62 to CITY, 63 to CITY, 64 to CITY, 65 to CITY,
        66 to CITY, 67 to NATURE, 68 to CITY, 69 to CITY, 70 to NATURE, 71 to BEACH, 72 to NATURE,
        73 to CITY, 74 to NATURE, 75 to CITY, 76 to CITY, 77 to CITY, 78 to CITY, 79 to CITY,
        80 to CITY, 81 to CITY, 82 to CITY, 83 to NATURE, 84 to NATURE, 85 to NATURE,
        100 to BEACH, 101 to CITY, 102 to NATURE, 103 to BEACH, 104 to NATURE, 106 to NATURE, 107 to CITY,
        108 to NATURE, 109 to NATURE, 110 to NATURE, 111 to CITY, 112 to NATURE, 113 to CITY, 114 to NATURE,
        115 to NATURE, 116 to NATURE, 117 to BEACH, 118 to NATURE, 119 to CITY, 120 to NATURE,
        121 to NATURE, 122 to CITY, 123 to NATURE, 124 to BEACH, 125 to NATURE,
    )

    val all: List<WallpaperPhoto> = ids.mapIndexed { index, (id, category) -> WallpaperPhoto(id, "$category ${index + 1}") }

    private val memory = object : LruCache<String, Bitmap>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    fun peek(url: String): Bitmap? = memory.get(url)

    /** מוריד (או לוקח מהמטמון בדיסק) ומפענח. חוסם - לקרוא מחוץ ל-Main. null כשאין רשת. */
    fun load(context: Context, url: String, maxPx: Int): Bitmap? {
        memory.get(url)?.let { return it }
        val file = File(File(context.cacheDir, "wallpapers").apply { mkdirs() }, sha1(url))
        if (!file.exists()) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.connectTimeout = 10_000
                connection.readTimeout = 20_000
                connection.instanceFollowRedirects = true
                connection.inputStream.use { input -> file.outputStream().use { input.copyTo(it) } }
                connection.disconnect()
            } catch (e: Exception) {
                file.delete()
                return null
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxPx) sample *= 2
        val bitmap = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample }) ?: run {
            file.delete()
            return null
        }
        memory.put(url, bitmap)
        return bitmap
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
}
