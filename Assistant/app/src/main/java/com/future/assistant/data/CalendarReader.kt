package com.future.assistant.data

import android.content.Context
import android.provider.CalendarContract
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** קריאה אמיתית של אירועי היומן (לא רק פתיחת האפליקציה) - שאילתה ישירה
 * ל-CalendarContract, בלי אינטרנט, עבור טווח יום שלם. */
object CalendarReader {
    data class EventsResult(val events: List<String>, val hasPermission: Boolean)

    fun eventsForDay(context: Context, daysFromToday: Int): EventsResult {
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromToday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = startCal.timeInMillis
        val end = start + 24L * 60 * 60 * 1000

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()
        val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN)
        val events = mutableListOf<String>()

        return try {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = cursor.getLong(1)
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(begin))
                    events.add("$title בשעה $time")
                }
            }
            EventsResult(events, hasPermission = true)
        } catch (e: SecurityException) {
            EventsResult(emptyList(), hasPermission = false)
        } catch (e: Exception) {
            EventsResult(emptyList(), hasPermission = true)
        }
    }
}
