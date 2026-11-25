package com.future.calendar

import android.content.Context
import com.future.calendar.data.DafYomi
import com.future.calendar.data.DafYomiEntry
import com.future.calendar.data.HebrewNumerals
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.time.LocalDate

/** הדף היומי של היום, ושל מחר בשורת המשנה. */
class DafYomiWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val today = LocalDate.now()
        val todayEntry = DafYomi.forDate(today) ?: return WidgetContent(value = "אין דף יומי")
        val tomorrowEntry = DafYomi.forDate(today.plusDays(1))
        return WidgetContent(
            value = describe(todayEntry),
            subtitle = tomorrowEntry?.let { "מחר: ${describe(it)}" },
        )
    }

    private fun describe(entry: DafYomiEntry): String = "${entry.masechet} · דף ${HebrewNumerals.toHebrew(entry.daf)}"
}
