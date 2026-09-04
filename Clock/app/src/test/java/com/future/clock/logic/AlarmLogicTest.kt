package com.future.clock.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar

/**
 * בדיקות ל-nextTriggerMillis - התיקון המרכזי לבאג שבו alarm.days הוגדר
 * ב-data class אבל מעולם לא נקרא בפועל בזמן התזמון (כל אזעקה, כולל
 * "חוזרת", תוזמנה בפועל כחד-פעמית ליום הקרוב הבא). ר' AlarmLogic.kt
 * ו-CHANGELOG.md לפירוט המלא של הבאג המקורי.
 */
class AlarmLogicTest {

    /** יום שישי, 10:00 - נקודת ייחוס קבועה לכל הבדיקות כדי שלא יהיו תלויות בתאריך הריצה בפועל. */
    private fun friday10am(): Calendar = Calendar.getInstance().apply {
        set(2026, Calendar.SEPTEMBER, 4, 10, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun Calendar.dayOfWeek() = get(Calendar.DAY_OF_WEEK)
    private fun Calendar.hour() = get(Calendar.HOUR_OF_DAY)
    private fun Calendar.minute() = get(Calendar.MINUTE)

    @Test
    fun `one-time alarm later today schedules for today`() {
        val now = friday10am()
        val alarm = Alarm(id = 1, hour = 18, minute = 0, days = emptySet())
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(now.get(Calendar.DAY_OF_YEAR), trigger.get(Calendar.DAY_OF_YEAR))
        assertEquals(18, trigger.hour())
        assertEquals(0, trigger.minute())
    }

    @Test
    fun `one-time alarm earlier today schedules for tomorrow`() {
        val now = friday10am()
        val alarm = Alarm(id = 1, hour = 7, minute = 50, days = emptySet())
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(now.get(Calendar.DAY_OF_YEAR) + 1, trigger.get(Calendar.DAY_OF_YEAR))
        assertEquals(7, trigger.hour())
        assertEquals(50, trigger.minute())
    }

    @Test
    fun `recurring alarm picks nearer of two days later this week`() {
        // מהיום (שישי) - חמישי הבא רחוק (6 ימים), שבת קרובה (יום אחד) - צריך לבחור בשבת.
        val now = friday10am()
        val thursday = Calendar.THURSDAY
        val saturday = Calendar.SATURDAY
        val alarm = Alarm(id = 2, hour = 7, minute = 0, days = setOf(thursday, saturday))
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(saturday, trigger.dayOfWeek())
        assertEquals(now.get(Calendar.DAY_OF_YEAR) + 1, trigger.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `recurring alarm on today's weekday, later today, fires today`() {
        val now = friday10am()
        val alarm = Alarm(id = 3, hour = 20, minute = 0, days = setOf(now.dayOfWeek()))
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(now.get(Calendar.DAY_OF_YEAR), trigger.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun `recurring alarm on today's weekday, already passed, wraps to next week`() {
        val now = friday10am()
        val alarm = Alarm(id = 4, hour = 6, minute = 0, days = setOf(now.dayOfWeek()))
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(now.get(Calendar.DAY_OF_YEAR) + 7, trigger.get(Calendar.DAY_OF_YEAR))
        assertEquals(now.dayOfWeek(), trigger.dayOfWeek())
    }

    @Test
    fun `trigger time always has zero seconds and milliseconds`() {
        // רגרסיה: SECOND אופס אבל לא MILLISECOND בקוד המקורי.
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 4, 10, 30, 45)
            set(Calendar.MILLISECOND, 999)
        }
        val alarm = Alarm(id = 5, hour = 18, minute = 0, days = emptySet())
        val trigger = Calendar.getInstance().apply { timeInMillis = AlarmLogic.nextTriggerMillis(alarm, now) }

        assertEquals(0, trigger.get(Calendar.SECOND))
        assertEquals(0, trigger.get(Calendar.MILLISECOND))
    }
}
