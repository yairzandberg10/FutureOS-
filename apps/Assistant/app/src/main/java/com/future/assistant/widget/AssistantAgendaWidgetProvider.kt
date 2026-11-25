package com.future.assistant.widget

import android.content.Context
import com.future.assistant.data.CalendarReader
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מספר האירועים של היום ביומן, והאירוע הבא בשורת המשנה. */
class AssistantAgendaWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val result = CalendarReader.eventsForDay(context, 0)
        if (!result.hasPermission) return WidgetContent(value = "אין הרשאה ליומן", subtitle = "אשר גישה ליומן בעוזר")
        if (result.failed) return WidgetContent(value = "היומן לא זמין כרגע")
        if (result.events.isEmpty()) return WidgetContent(value = "אין אירועים היום")
        val count = result.events.size
        val value = if (count == 1) "אירוע אחד היום" else "$count אירועים היום"
        return WidgetContent(value = value, subtitle = result.events.first())
    }
}
