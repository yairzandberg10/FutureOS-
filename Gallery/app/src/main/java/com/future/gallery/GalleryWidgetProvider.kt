package com.future.gallery

import android.content.pm.PackageManager
import android.provider.MediaStore
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה תמונות יש, ומתי צולמה האחרונה. */
class GalleryWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (context.checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
            context.checkSelfPermission("android.permission.READ_MEDIA_IMAGES") != PackageManager.PERMISSION_GRANTED
        ) return WidgetContent(value = "גלריה", subtitle = "פתח כדי לאשר גישה")
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media.DATE_ADDED),
            null, null, "${MediaStore.Images.Media.DATE_ADDED} DESC",
        )?.use { c ->
            if (c.moveToFirst()) {
                return WidgetContent(value = "${c.count} תמונות", subtitle = "האחרונה נוספה ${ago(c.getLong(0) * 1000L)}")
            }
        }
        return WidgetContent(value = "אין תמונות", subtitle = null)
    }
}
