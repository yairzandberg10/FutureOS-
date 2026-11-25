package com.future.clock

import android.content.Context
import com.future.clock.ui.CityClock
import com.future.clock.ui.WORLD_CITIES
import com.future.sharednav.i18n.tr
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** השעון העולמי: השעה בעיר הראשונה ברשימה השמורה, ובשתיים הבאות בשורת המשנה. */
class WorldClockWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        // אותו מפתח וברירות מחדל כמו WorldClockStore במסך השעון העולמי
        val stored = context.getSharedPreferences("world_clock", Context.MODE_PRIVATE).getString("cities", null)
        val names = stored?.split('|')?.filter { it.isNotBlank() }
            ?: listOf(tr("ירושלים"), tr("לונדון"), tr("ניו יורק"), tr("טוקיו"))
        val cities = names.mapNotNull { name -> WORLD_CITIES.firstOrNull { it.name == name } }
        if (cities.isEmpty()) {
            return WidgetContent(value = "אין ערים", subtitle = "הוסף עיר בשעון העולמי", title = "שעון עולמי")
        }
        val now = Instant.now()
        val first = cities.first()
        val others = cities.drop(1).take(2).joinToString(" · ") { "${it.name} ${timeIn(it, now)}" }
        return WidgetContent(
            value = "${first.name} ${timeIn(first, now)}",
            subtitle = others.ifBlank { null },
            title = "שעון עולמי",
        )
    }

    private fun timeIn(city: CityClock, now: Instant): String =
        DateTimeFormatter.ofPattern("HH:mm", Locale.US).format(now.atZone(ZoneId.of(city.timezone)))
}
