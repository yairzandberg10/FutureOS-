package com.future.assistant.data

import android.content.Context
import android.provider.CalendarContract
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** קריאה אמיתית של אירועי היומן (לא רק פתיחת האפליקציה) - שאילתה ישירה
 * ל-CalendarContract, בלי אינטרנט, עבור טווח יום שלם. */
object CalendarReader {
    /** [failed] - השאילתה נכשלה (לא "היומן ריק"), כדי שלא נקריא "אין לך אירועים". */
    data class EventsResult(val events: List<String>, val hasPermission: Boolean, val failed: Boolean = false)

    fun eventsForDay(context: Context, daysFromToday: Int): EventsResult {
        val startCal = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, daysFromToday)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = startCal.timeInMillis
        // סוף היום לפי לוח השנה (ולא +24 שעות - ביום של מעבר לשעון קיץ זה שגוי)
        val end = (startCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY,
        )
        val events = mutableListOf<String>()

        return try {
            context.contentResolver.query(uri, projection, null, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { cursor ->
                while (cursor.moveToNext()) {
                    val title = cursor.getString(0)?.takeIf { it.isNotBlank() } ?: continue
                    val begin = cursor.getLong(1)
                    // אירוע כל היום - בלי שעה (ההתחלה שלו היא חצות UTC, לא שעה אמיתית)
                    // הכותרת מבודדת (LRI..PDI) כדי שכותרת באנגלית לא תשבש את המשפט בעברית
                    val isolated = "⁦$title⁩"
                    if (cursor.getInt(2) == 1) {
                        events.add("$isolated כל היום")
                    } else {
                        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(begin))
                        events.add("$isolated בשעה $time")
                    }
                }
            }
            EventsResult(events, hasPermission = true)
        } catch (e: SecurityException) {
            EventsResult(emptyList(), hasPermission = false)
        } catch (e: Exception) {
            Log.w(TAG, "calendar query failed", e)
            EventsResult(emptyList(), hasPermission = true, failed = true)
        }
    }

    private const val TAG = "Assistant/CalendarReader"
}
