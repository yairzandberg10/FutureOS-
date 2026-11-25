package com.future.files

import android.content.Context
import android.os.Environment
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.io.File

/** מספר הקבצים בתיקיית ההורדות, והקובץ שנוסף/עודכן אחרון. */
class FilesDownloadsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val root = Environment.getExternalStorageDirectory()
        val dir = listOf("Download", "Downloads").map { File(root, it) }.firstOrNull { it.isDirectory }
            ?: return WidgetContent(value = "אין הורדות", subtitle = "תיקיית ההורדות לא נמצאה")
        val files = dir.listFiles()?.filter { it.isFile && !it.name.startsWith(".") }.orEmpty()
        val latest = files.maxByOrNull { it.lastModified() }
            ?: return WidgetContent(value = "אין הורדות", subtitle = "התיקייה ריקה")
        return WidgetContent(
            value = if (files.size == 1) "קובץ אחד בהורדות" else "${files.size} קבצים בהורדות",
            subtitle = "אחרון: ${latest.name} · ${ago(latest.lastModified())}",
        )
    }
}
