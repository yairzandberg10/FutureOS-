package com.future.camera.widget

import android.content.Context
import android.provider.MediaStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר הסרטונים שהוקלטו, ומתי הוקלט האחרון. */
class CameraVideosWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent = cameraMediaSummary(
        context = context,
        collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        legacyDir = "Movies/Camera",
        title = "סרטונים שהוקלטו",
        singular = "סרטון",
        plural = "סרטונים",
        empty = "אין סרטונים",
    )
}
