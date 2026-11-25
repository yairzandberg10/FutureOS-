package com.future.music

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.MediaStore
import com.future.music.data.PlaylistStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** כמה שירים יש בספרייה, וכמה פלייליסטים ומועדפים שמורים. */
class MusicLibraryWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val permission = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "ספריית המוזיקה", subtitle = "פתח כדי לאשר גישה", title = "ספרייה")
        }
        val songs = context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Media._ID),
            "${MediaStore.Audio.Media.IS_MUSIC} != 0",
            null,
            null,
        )?.use { it.count } ?: 0
        val store = PlaylistStore(context)
        val value = when (songs) {
            0 -> "הספרייה ריקה"
            1 -> "שיר אחד בספרייה"
            else -> "$songs שירים בספרייה"
        }
        return WidgetContent(
            value = value,
            subtitle = "${store.getPlaylists().size} פלייליסטים · ${store.getFavoriteIds().size} במועדפים",
            title = "ספרייה",
        )
    }
}
