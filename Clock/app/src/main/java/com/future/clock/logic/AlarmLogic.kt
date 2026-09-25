package com.future.clock.logic

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.future.clock.AlarmReceiver
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import java.util.Calendar

private const val TAG = "Clock/AlarmLogic"

/** דקות הנודניק כשלוחצים על מקש כלשהו (לא OK) במסך הצלצול. */
const val SNOOZE_MINUTES = 5

/**
 * הפעולה שמסומנת על ה-Intent שהאזעקה האמיתית מצלצלת בו (בניגוד ל-
 * BOOT_COMPLETED שאותו Receiver גם מאזין לו). בלי הפרדה מפורשת הזאת,
 * AlarmReceiver לא יכול להבדיל בין "המכשיר עלה מחדש" לבין "אזעקה
 * מצלצלת" - זה היה הגורם לכך שבכל אתחול מכשיר, השעון היה מרטט ומציג
 * טוסט "מצלצל" בלי שום אזעקה אמיתית קרתה.
 */
const val ACTION_ALARM_FIRED = "com.future.clock.ACTION_ALARM_FIRED"

/** הטיימר הסתיים - מתוזמן ב-AlarmManager כדי שיצלצל גם כשהמסך כבוי או כשיצאו מהמסך. */
const val ACTION_TIMER_FIRED = "com.future.clock.ACTION_TIMER_FIRED"

/** מפתח תוסף ה-Intent שנושא את מזהה האזעקה שצריכה לצלצל/להתנדנד. */
const val EXTRA_ALARM_ID = "ALARM_ID"

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>, // 1 (Sunday) עד 7 (Saturday), כמו Calendar.DAY_OF_WEEK. ריק = חד-פעמי.
    val isEnabled: Boolean = true,
    val label: String = ""
)

object AlarmLogic {
    private const val PREFS_NAME = "alarms_prefs"
    private const val ALARMS_KEY = "alarms_list"
    private val gson = Gson()

    /**
     * אם ה-JSON השמור פגום מכל סיבה (עדכון גרסה לא תואם, כתיבה חלקית וכו')
     * מחזיר רשימה ריקה במקום לקרוס בעלייה - קודם זה זרק JsonSyntaxException
     * לא-מטופל וגם הצהיר על ערך מוחזר לא-nullable מפונקציה ש-Gson יכולה
     * להחזיר null ממנה (JSON = "null").
     */
    fun getAlarms(context: Context): List<Alarm> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(ALARMS_KEY, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<Alarm>>() {}.type
            gson.fromJson<List<Alarm>?>(json, type) ?: emptyList()
        } catch (e: JsonSyntaxException) {
            Log.w(TAG, "alarms_list פגום, מתעלם ומחזיר רשימה ריקה", e)
            emptyList()
        }
    }

    fun saveAlarms(context: Context, alarms: List<Alarm>) {
        val previous = getAlarms(context)
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ALARMS_KEY, gson.toJson(alarms)).apply()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // אזעקה שנמחקה כבר לא ברשימה החדשה, ולכן הלולאה למטה לא ביטלה אותה -
        // ה-PendingIntent שלה (וגם נודניק פתוח) נשאר מתוזמן ב-AlarmManager.
        val keptIds = alarms.mapTo(HashSet()) { it.id }
        previous.filter { it.id !in keptIds }.forEach { removed ->
            cancelAlarm(context, alarmManager, removed)
            cancelRequestCode(context, alarmManager, snoozeRequestCode(removed.id))
        }
        alarms.forEach { alarm ->
            if (alarm.isEnabled) scheduleAlarm(context, alarmManager, alarm) else cancelAlarm(context, alarmManager, alarm)
        }
    }

    /**
     * נקרא מ-AlarmReceiver ברגע שאזעקה מצלצלת בפועל. במקום לתזמן מחדש את
     * כל הרשימה (כפי ש-saveAlarms עושה), מתעדכנת ונשמרת רק האזעקה שצלצלה -
     * כל השאר לא נוגעות ולא מבוטלות/מתוזמנות מחדש לשווא.
     *
     * אם לאזעקה יש ימים חוזרים (days לא ריק) - היא מתוזמנת מחדש להופעה
     * הבאה שלה ונשארת מופעלת. אם היא חד-פעמית (days ריק) - היא מסומנת
     * ככבויה כדי שלא "תיעלם" מהרשימה מבלי שהמשתמש יבין למה היא לא תצלצל
     * שוב (הבאג המקורי: אזעקה חד-פעמית פשוט נעלמה בלי הסבר אחרי צלצול יחיד).
     */
    fun onAlarmFired(context: Context, alarmId: Int): Alarm? {
        val alarms = getAlarms(context)
        val index = alarms.indexOfFirst { it.id == alarmId }
        if (index == -1) return null
        val firedAlarm = alarms[index]

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val updated = if (firedAlarm.days.isEmpty()) {
            firedAlarm.copy(isEnabled = false)
        } else {
            firedAlarm
        }

        val newList = alarms.toMutableList().apply { this[index] = updated }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(ALARMS_KEY, gson.toJson(newList)).apply()

        if (updated.isEnabled) {
            scheduleAlarm(context, alarmManager, updated)
        }
        return firedAlarm
    }

    /**
     * נקרא מ-AlarmReceiver בתגובה ל-BOOT_COMPLETED. אזעקות ב-AlarmManager
     * לא שורדות אתחול מכשיר - בלי הקריאה הזאת, כל האזעקות המופעלות פשוט
     * מפסיקות לצלצל אחרי כל reboot בלי שום סימן למשתמש.
     */
    fun rescheduleAll(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        getAlarms(context).forEach { alarm ->
            if (alarm.isEnabled) scheduleAlarm(context, alarmManager, alarm)
        }
    }

    /** מתזמן אזעקה חד-פעמית נוספת בעוד SNOOZE_MINUTES דקות, בלי לגעת באזעקה המקורית. */
    fun scheduleSnooze(context: Context, originalAlarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeId = snoozeRequestCode(originalAlarmId)
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRED
            putExtra(EXTRA_ALARM_ID, originalAlarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, snoozeId, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = Calendar.getInstance().apply { add(Calendar.MINUTE, SNOOZE_MINUTES) }.timeInMillis
        setExactSafely(context, alarmManager, triggerAt, pendingIntent)
    }

    /**
     * מזהה request code נפרד לנודניק (לא מתנגש עם ה-PendingIntent של האזעקה
     * המקורית) - כדי שהנודניק וההופעה החוזרת הרגילה הבאה יוכלו להתקיים
     * שתיהן מתוזמנות בו-זמנית בלי לדרוס אחת את השנייה.
     */
    private fun snoozeRequestCode(alarmId: Int): Int = alarmId + 1_000_000

    private fun scheduleAlarm(context: Context, alarmManager: AlarmManager, alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_ALARM_FIRED
            putExtra(EXTRA_ALARM_ID, alarm.id)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerAt = nextTriggerMillis(alarm)
        if (alarmManager.canScheduleExactAlarms()) {
            // setAlarmClock ולא setExactAndAllowWhileIdle: זה ה-API של שעון מעורר -
            // לא נדחה במצב Doze עמוק, והמערכת יודעת שיש אזעקה קרובה (getNextAlarmClock).
            val show = PendingIntent.getActivity(
                context, alarm.id, Intent(context, com.future.clock.MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, show), pendingIntent)
        } else {
            setExactSafely(context, alarmManager, triggerAt, pendingIntent)
        }
    }

    private fun setExactSafely(context: Context, alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        // ב-minSdk 31 המשתמש יכול לשלול את ההרשאה SCHEDULE_EXACT_ALARM בכל רגע
        // (הגדרות מערכת) - קריאה ל-setExactAndAllowWhileIdle בלי הבדיקה הזאת
        // זורקת SecurityException ומקריסה את האפליקציה בכל שמירה/עלייה.
        if (alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } else {
            Log.w(TAG, "אין הרשאת SCHEDULE_EXACT_ALARM - מתזמן אזעקה לא-מדויקת (יכולה לאחר)")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun cancelAlarm(context: Context, alarmManager: AlarmManager, alarm: Alarm) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM_FIRED }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarm.id, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) alarmManager.cancel(pendingIntent)
    }

    /**
     * הטיימר רץ בתוך המסך בלבד - delay של קורוטינה לא מתעורר כשהמעבד ישן, וכשיוצאים
     * מהמסך המצב נעלם. לכן סיום הטיימר מתוזמן גם ב-AlarmManager, ושעת הסיום (שעון
     * קיר, שורד אתחול) נשמרת כדי שהמסך ימשיך את הספירה כשחוזרים אליו.
     */
    fun scheduleTimer(context: Context, endWallMillis: Long, totalMillis: Long) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putLong(TIMER_END_KEY, endWallMillis).putLong(TIMER_TOTAL_KEY, totalMillis).apply()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        setExactSafely(context, alarmManager, endWallMillis, timerIntent(context))
    }

    fun cancelTimer(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .remove(TIMER_END_KEY).remove(TIMER_TOTAL_KEY).apply()
        (context.getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(timerIntent(context))
    }

    /** (שעת סיום בשעון קיר, אורך מלא) של טיימר שעדיין רץ, או null. */
    fun runningTimer(context: Context): Pair<Long, Long>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val end = prefs.getLong(TIMER_END_KEY, 0L)
        if (end <= System.currentTimeMillis()) return null
        return end to prefs.getLong(TIMER_TOTAL_KEY, 0L)
    }

    private fun timerIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context, TIMER_REQUEST_CODE,
        Intent(context, AlarmReceiver::class.java).setAction(ACTION_TIMER_FIRED),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private const val TIMER_END_KEY = "timer_end_wall"
    private const val TIMER_TOTAL_KEY = "timer_total"
    private const val TIMER_REQUEST_CODE = 2_000_000

    private fun cancelRequestCode(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM_FIRED }
        PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
            ?.let { alarmManager.cancel(it) }
    }

    /**
     * מחשב את מועד ההפעלה הבא של אזעקה - התיקון המרכזי לבאג שבו alarm.days
     * הוגדר ב-data class אבל מעולם לא נקרא בפועל בזמן התזמון: אזעקה שבועית
     * חוזרת הייתה מתוזמנת תמיד כחד-פעמית ליום הקרוב הבא, בלי שום קשר לימים
     * שנבחרו.
     *
     * days ריק = חד-פעמי: היום אם השעה עוד לא עברה, אחרת מחר.
     * days לא ריק = היום החוזר הקרוב ביותר (כולל היום עצמו אם השעה עוד לא
     * עברה), עם גלגול לשבוע הבא אם אף יום מהנבחרים לא נשאר השבוע.
     */
    internal fun nextTriggerMillis(alarm: Alarm, now: Calendar = Calendar.getInstance()): Long {
        val candidate = (now.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, alarm.hour)
            set(Calendar.MINUTE, alarm.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (alarm.days.isEmpty()) {
            if (!candidate.after(now)) candidate.add(Calendar.DATE, 1)
            return candidate.timeInMillis
        }

        // בודקים את 7 הימים הבאים (כולל היום) ובוחרים את הראשון שנמצא ב-days
        // ושהזמן שלו עדיין לא עבר.
        for (offset in 0..6) {
            val day = (candidate.clone() as Calendar).apply { add(Calendar.DATE, offset) }
            if (day.get(Calendar.DAY_OF_WEEK) in alarm.days && day.after(now)) {
                return day.timeInMillis
            }
        }
        // לא אמור לקרות (days לא ריק => יש תמיד יום תואם תוך 7 ימים), אבל
        // fallback בטוח לשבוע קדימה במקום לזרוק חריגה.
        return (candidate.clone() as Calendar).apply { add(Calendar.DATE, 7) }.timeInMillis
    }
}
