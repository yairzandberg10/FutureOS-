package com.future.clock

import android.app.AlarmManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** השעה (TextClock חי), התאריך, והמעורר הבא של המערכת. */
class ClockWidgetProvider : FutureContentWidget() {
    override val liveClock = true

    override fun content(context: Context): WidgetContent {
        val he = Locale("iw", "IL")
        val date = SimpleDateFormat("EEEE, d בMMMM", he).format(Date())
        val next = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock
        val alarm = next?.let { "מעורר " + SimpleDateFormat("EEE HH:mm", he).format(Date(it.triggerTime)) }
        return WidgetContent(value = "", subtitle = listOfNotNull(date, alarm).joinToString(" · "))
    }
}
