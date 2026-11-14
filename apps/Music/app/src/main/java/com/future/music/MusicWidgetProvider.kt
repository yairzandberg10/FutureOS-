package com.future.music

import android.content.ContentUris
import android.provider.MediaStore
import org.json.JSONArray
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** השיר האחרון שהושמע (מהתור שהמוזיקה שומרת) - שם ואמן. */
class MusicWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val prefs = context.getSharedPreferences("music_store", Context.MODE_PRIVATE)
        val ids = runCatching {
            val arr = JSONArray(prefs.getString("last_queue", null) ?: "[]")
            (0 until arr.length()).map { arr.getLong(it) }
        }.getOrDefault(emptyList())
        val id = ids.getOrNull(prefs.getInt("last_index", 0)) ?: return WidgetContent(value = "לא הושמע כלום", subtitle = "OK לפתיחת המוזיקה")
        val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
        runCatching {
            context.contentResolver.query(uri, arrayOf(MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val artist = c.getString(1)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                    return WidgetContent(value = c.getString(0).orEmpty(), subtitle = artist ?: "הושמע לאחרונה", title = "הושמע לאחרונה")
                }
            }
        }
        return WidgetContent(value = "לא הושמע כלום", subtitle = "OK לפתיחת המוזיקה")
    }
}
