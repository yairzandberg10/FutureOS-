package com.future.clock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.future.clock.logic.ACTION_ALARM_FIRED
import com.future.clock.logic.ACTION_TIMER_FIRED
import com.future.clock.logic.AlarmLogic
import com.future.clock.logic.EXTRA_ALARM_ID

private const val TAG = "Clock/AlarmReceiver"

/**
 * רשום גם ל-BOOT_COMPLETED (במניפסט) וגם מקבל ה-Intent המפורש ששולח
 * AlarmManager כשאזעקה מצלצלת (ACTION_ALARM_FIRED, ר' AlarmLogic). קודם
 * onReceive לא הבדיל בין השניים בכלל - כל קריאה, כולל אתחול מכשיר,
 * הפעילה את לוגיקת ה"צלצול" (טוסט + רטט), ואף אזעקה לא תוזמנה מחדש אחרי
 * reboot.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // אחרי שינוי שעה/אזור זמן מועד ההפעלה המוחלט (RTC) כבר לא מתאים לשעה
            // המקומית שנבחרה - אזעקת 07:00 הייתה מצלצלת ב-06:00 או ב-08:00.
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "${intent.action} - rescheduling all enabled alarms")
                AlarmLogic.rescheduleAll(context)
            }
            ACTION_ALARM_FIRED -> {
                val alarmId = intent.getIntExtra(EXTRA_ALARM_ID, -1)
                if (alarmId == -1) return
                val alarm = AlarmLogic.onAlarmFired(context, alarmId) ?: return

                // מתחיל את מסך הצלצול ישירות מה-Receiver - הפעלת Activity
                // מ-Receiver שמופעל על ידי AlarmManager (בעוד האפליקציה מחזיקה
                // SCHEDULE_EXACT_ALARM) פטורה ממגבלות "background activity
                // start" בדיוק בשביל השימוש הזה. FLAG_ACTIVITY_NEW_TASK נדרש
                // כי אין כאן Activity-parent שממנו הופעלנו.
                val ringIntent = Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(EXTRA_ALARM_ID, alarm.id)
                    putExtra(AlarmRingActivity.EXTRA_HOUR, alarm.hour)
                    putExtra(AlarmRingActivity.EXTRA_MINUTE, alarm.minute)
                    putExtra(AlarmRingActivity.EXTRA_LABEL, alarm.label)
                }
                context.startActivity(ringIntent)
            }
            ACTION_TIMER_FIRED -> {
                AlarmLogic.cancelTimer(context)
                val now = java.util.Calendar.getInstance()
                context.startActivity(Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(AlarmRingActivity.EXTRA_HOUR, now.get(java.util.Calendar.HOUR_OF_DAY))
                    putExtra(AlarmRingActivity.EXTRA_MINUTE, now.get(java.util.Calendar.MINUTE))
                    putExtra(AlarmRingActivity.EXTRA_LABEL, "הטיימר הסתיים")
                })
            }
            else -> Log.w(TAG, "פעולה לא צפויה התקבלה: ${intent.action}")
        }
    }
}
