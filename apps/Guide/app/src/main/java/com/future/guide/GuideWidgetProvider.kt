package com.future.guide

import com.future.guide.data.GUIDE_APPS
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** טיפ היום - טיפ אחר בכל יום, מתוך הטיפים של כל האפליקציות במדריך. */
class GuideWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val tips = GUIDE_APPS.flatMap { app -> app.tips.map { app.name to it } }
        if (tips.isEmpty()) return WidgetContent(value = "מדריך למשתמש")
        val day = (System.currentTimeMillis() / 86_400_000L).toInt()
        val (app, tip) = tips[day % tips.size]
        return WidgetContent(value = app, subtitle = tip, title = "טיפ היום")
    }
}
