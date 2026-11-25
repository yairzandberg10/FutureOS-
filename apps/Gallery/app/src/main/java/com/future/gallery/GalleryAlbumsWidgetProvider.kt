package com.future.gallery

import android.content.Context
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה אלבומים (תיקיות תמונות) יש במכשיר, ואיזה אלבום הכי גדול. */
class GalleryAlbumsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != PackageManager.PERMISSION_GRANTED
        ) return WidgetContent(value = "אלבומים", subtitle = "פתח כדי לאשר גישה")
        val counts = HashMap<String, Int>()
        val names = HashMap<String, String>()
        try {
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Images.Media.BUCKET_ID, MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
                null, null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0) ?: continue
                    counts[id] = (counts[id] ?: 0) + 1
                    if (!names.containsKey(id)) names[id] = c.getString(1).orEmpty().ifBlank { "ללא שם" }
                }
            }
        } catch (e: SecurityException) {
            // ההרשאה יכולה להתבטל בין הבדיקה לשאילתה - לא מפילים את הווידג'ט
            return WidgetContent(value = "אלבומים", subtitle = "פתח כדי לאשר גישה")
        }
        if (counts.isEmpty()) return WidgetContent(value = "אין אלבומים", subtitle = null)
        val biggest = counts.maxByOrNull { it.value }!!
        return WidgetContent(
            value = if (counts.size == 1) "אלבום אחד" else "${counts.size} אלבומים",
            subtitle = "הגדול: ${names[biggest.key]} · ${biggest.value} תמונות",
        )
    }
}
