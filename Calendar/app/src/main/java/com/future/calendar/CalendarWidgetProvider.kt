package com.future.calendar

import android.content.pm.PackageManager
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** התאריך העברי, התאריך הלועזי, והאירוע הבא של היום (אם יש הרשאה ליומן). */
class CalendarWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val now = Date()
        val hebrew = DateFormat.getDateInstance(DateFormat.LONG, ULocale.forLanguageTag("he-IL-u-ca-hebrew")).format(now)
        val gregorian = SimpleDateFormat("EEEE, d בMMMM", Locale("iw", "IL")).format(now)
        return WidgetContent(value = hebrew, subtitle = listOfNotNull(gregorian, nextEventToday(context)).joinToString(" · "))
    }

    private fun nextEventToday(context: Context): String? {
        if (context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return null
        val start = System.currentTimeMillis()
        val end = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59)
        }.timeInMillis
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString()).appendPath(end.toString()).build()
        context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
            "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { c ->
            if (c.moveToFirst()) {
                val title = c.getString(0).orEmpty().ifBlank { "אירוע" }
                return if (c.getInt(2) == 1) title else SimpleDateFormat("HH:mm", Locale.US).format(Date(c.getLong(1))) + " " + title
            }
        }
        return "אין אירועים היום"
    }
}
