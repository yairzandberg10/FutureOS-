package com.future.camera.widget

import android.content.Context
import android.provider.MediaStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר התמונות שצולמו, ומתי צולמה האחרונה. */
class CameraPhotosWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent = cameraMediaSummary(
        context = context,
        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        legacyDir = "Pictures/Camera",
        title = "תמונות שצולמו",
        singular = "תמונה",
        plural = "תמונות",
        empty = "אין תמונות",
    )
}
