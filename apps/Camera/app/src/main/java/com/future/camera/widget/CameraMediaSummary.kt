package com.future.camera.widget

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.future.camera.data.CAMERA_DIR
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/**
 * סיכום לקבצים שהמצלמה שמרה ב-MediaStore: כמה יש, ומתי נשמר האחרון.
 * [legacyDir] - התיקייה הישנה (Pictures/Camera או Movies/Camera) שגם היא נספרת.
 * הקבצים נוצרו על ידי האפליקציה עצמה, ולכן השאילתה עובדת בלי הרשאת אחסון.
 */
internal fun cameraMediaSummary(
    context: Context,
    collection: Uri,
    legacyDir: String,
    title: String,
    singular: String,
    plural: String,
    empty: String,
): WidgetContent {
    val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ? OR ${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
    val args = arrayOf("$CAMERA_DIR%", "$legacyDir%")
    val order = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    context.contentResolver.query(collection, arrayOf(MediaStore.MediaColumns.DATE_ADDED), selection, args, order)
        ?.use { c ->
            if (c.count == 0) return WidgetContent(value = empty, title = title)
            c.moveToFirst()
            val savedMillis = c.getLong(0) * 1000L // DATE_ADDED נשמר בשניות
            val count = c.count
            return WidgetContent(
                value = "$count ${if (count == 1) singular else plural}",
                subtitle = "האחרון: ${FutureContentWidget.ago(savedMillis)}",
                title = title,
            )
        }
    return WidgetContent(value = empty, title = title)
}
