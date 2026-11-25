package com.future.flashlight.widget

import android.content.Context
import com.future.flashlight.data.FlashlightController
import com.future.flashlight.data.TorchStateStore
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מצב הפנס עכשיו (דלוק/כבוי) ומתי השתנה לאחרונה. */
class FlashlightStateWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        if (!FlashlightController(context).hasFlash()) {
            return WidgetContent(value = "אין פנס", subtitle = "לא נמצא פנס במכשיר הזה")
        }
        val state = TorchStateStore.read(context)
        if (state.changedAt == 0L) return WidgetContent(value = "הפנס כבוי", subtitle = "עוד לא הופעל")
        return WidgetContent(
            value = if (state.on) "הפנס דלוק" else "הפנס כבוי",
            subtitle = "עודכן ${ago(state.changedAt)}",
        )
    }
}
