package com.future.bluetooth.widget

import android.content.Context
import com.future.bluetooth.data.ReceivedFiles
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר הקבצים שהתקבלו בבלוטות', והשם של הקובץ האחרון. */
class ReceivedFilesWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val files = ReceivedFiles.load(context)
        // בלי "גישה לכל הקבצים" רשומות שאינן מדיה לא נראות - מציינים שהרשימה חלקית.
        val note = if (!ReceivedFiles.hasAccess()) "הרשימה חלקית - נדרשת גישה לכל הקבצים" else null
        if (files.isEmpty()) return WidgetContent(value = "אין קבצים שהתקבלו", subtitle = note)
        val value = if (files.size == 1) "קובץ אחד שהתקבל" else "${files.size} קבצים שהתקבלו"
        return WidgetContent(value = value, subtitle = note ?: "⁦${files.first().name}⁩")
    }
}
