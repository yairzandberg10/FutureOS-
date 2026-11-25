package com.future.settings

import android.content.Context
import com.future.settings.update.UpdateRepository
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר עדכוני המערכת שזמינים להתקנה, ומתי נבדקו בפעם האחרונה. */
class SettingsUpdatesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val count = UpdateRepository.offers(context).size
        val last = UpdateRepository.lastCheck(context)
        val checked = if (last > 0L) "נבדק " + FutureContentWidget.ago(last) else "עוד לא נבדק"
        val value = if (count == 0) "אין עדכונים" else "$count עדכונים זמינים"
        return WidgetContent(value = value, subtitle = checked, title = "עדכונים")
    }
}
