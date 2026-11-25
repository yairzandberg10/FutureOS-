package com.future.remote

import android.content.Context
import com.future.remote.data.DeviceCategory
import com.future.remote.data.RemoteRepository
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** סך הכפתורים המוגדרים בכל השלטים, ומספר מזגנים. */
class RemoteButtonsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val devices = RemoteRepository(context).loadDevices()
        val buttons = devices.sumOf { it.buttons.size }
        if (buttons == 0) return WidgetContent(value = "אין כפתורים", subtitle = "הגדירו כפתור בשלט")
        val acs = devices.count { it.category == DeviceCategory.AC }
        return WidgetContent(
            value = if (buttons == 1) "כפתור אחד" else "$buttons כפתורים",
            subtitle = if (acs == 0) "בכל המכשירים" else "מזגנים: $acs",
        )
    }
}
