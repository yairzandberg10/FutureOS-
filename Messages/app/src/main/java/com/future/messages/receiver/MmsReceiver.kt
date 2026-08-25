package com.future.messages.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * מקבל WAP_PUSH_DELIVER_ACTION - נדרש כדי שהמערכת תסכים לראות באפליקציה הזו
 * ברירת מחדל תקינה למסרונים. פענוח MMS מלא לא ממומש כרגע (רק SMS) - הטלפון
 * הזה מוגדר לתקשורת נתונים סלולרית בלבד ולא צפוי לקבל הרבה MMS, אבל ההצהרה
 * הזו עדיין נדרשת כדי שהאפליקציה תיחשב כשירה להיות ברירת מחדל.
 */
class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // לא ממומש - ר' הערה למעלה.
    }
}
