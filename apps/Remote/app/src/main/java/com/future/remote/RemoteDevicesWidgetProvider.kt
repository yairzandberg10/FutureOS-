package com.future.remote

import android.content.Context
import com.future.remote.data.RemoteRepository
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר מכשירי השלט השמורים, ושמות שלושה ראשונים. */
class RemoteDevicesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val devices = RemoteRepository(context).loadDevices()
        if (devices.isEmpty()) return WidgetContent(value = "אין מכשירים", subtitle = "הוסיפו מכשיר במסך הראשי")
        return WidgetContent(
            value = if (devices.size == 1) "מכשיר אחד" else "${devices.size} מכשירים",
            subtitle = devices.take(3).joinToString(", ") { it.name },
        )
    }
}
