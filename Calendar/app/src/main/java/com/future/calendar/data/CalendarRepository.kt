package com.future.calendar.data

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class UserCalendar(
    val id: Long,
    val displayName: String,
    val accountName: String,
    val color: Int,
    val isVisible: Boolean,
    val isWritable: Boolean
)

data class CalendarEvent(
    val id: Long,
    val calendarId: Long,
    val title: String,
    val description: String,
    val location: String,
    val startMillis: Long,
    val endMillis: Long,
    val allDay: Boolean,
    val color: Int
) {
    val startDate: LocalDate get() = Instant.ofEpochMilli(startMillis).atZone(if (allDay) ZoneId.of("UTC") else ZoneId.systemDefault()).toLocalDate()
    val endDate: LocalDate get() = Instant.ofEpochMilli(endMillis).atZone(if (allDay) ZoneId.of("UTC") else ZoneId.systemDefault()).toLocalDate()
}

/** גישה אמיתית לספק לוח השנה של אנדרואיד (CalendarContract) - בלי נתונים מדומים. */
class CalendarRepository(private val context: Context) {

    fun hasCalendarPermission(): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.READ_CALENDAR) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun getCalendars(): List<UserCalendar> {
        val result = mutableListOf<UserCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.VISIBLE,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        try {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null)?.use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(
                        UserCalendar(
                            id = cursor.getLong(0),
                            displayName = cursor.getString(1) ?: "",
                            accountName = cursor.getString(2) ?: "",
                            color = cursor.getInt(3),
                            isVisible = cursor.getInt(4) == 1,
                            isWritable = cursor.getInt(5) >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "Error loading calendars", e)
        }
        return result
    }

    /** לוח השנה הראשי שאירועים חדשים נוצרים בו כברירת מחדל - הראשון הניתן לכתיבה. */
    fun getDefaultWritableCalendarId(): Long? {
        return getCalendars().firstOrNull { it.isWritable }?.id ?: getCalendars().firstOrNull()?.id
    }

    /** שולף אירועים בטווח נתון תוך הרחבת אירועים חוזרים, דרך טבלת Instances -
     * אותה טבלה שאפליקציית לוח השנה של אנדרואיד עצמה משתמשת בה. */
    fun getEventsInRange(startMillis: Long, endMillis: Long): List<CalendarEvent> {
        val result = mutableListOf<CalendarEvent>()
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.DISPLAY_COLOR
        )
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, startMillis)
        ContentUris.appendId(builder, endMillis)
        try {
            context.contentResolver.query(builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    result.add(
                        CalendarEvent(
                            id = cursor.getLong(0),
                            calendarId = cursor.getLong(1),
                            title = cursor.getString(2) ?: "(ללא כותרת)",
                            description = cursor.getString(3) ?: "",
                            location = cursor.getString(4) ?: "",
                            startMillis = cursor.getLong(5),
                            endMillis = cursor.getLong(6),
                            allDay = cursor.getInt(7) == 1,
                            color = cursor.getInt(8)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "Error loading events", e)
        }
        return result
    }

    fun addEvent(
        calendarId: Long,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean
    ): Long? {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.CALENDAR_ID, calendarId)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, java.util.TimeZone.getDefault().id)
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "Error adding event", e)
            null
        }
    }

    fun updateEvent(
        eventId: Long,
        title: String,
        description: String,
        location: String,
        startMillis: Long,
        endMillis: Long,
        allDay: Boolean
    ): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.DESCRIPTION, description)
                put(CalendarContract.Events.EVENT_LOCATION, location)
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
            }
            context.contentResolver.update(uri, values, null, null) > 0
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "Error updating event", e)
            false
        }
    }

    fun deleteEvent(eventId: Long): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(uri, null, null) > 0
        } catch (e: Exception) {
            android.util.Log.e("CalendarRepository", "Error deleting event", e)
            false
        }
    }
}
