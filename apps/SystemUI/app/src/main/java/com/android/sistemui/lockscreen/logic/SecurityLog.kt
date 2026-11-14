package com.android.sistemui.lockscreen.logic

import android.content.Context
import android.content.SharedPreferences

/**
 * יומן ניסיונות פתיחה שגויים.
 *
 * קובץ העדפות נפרד בכוונה: לא נוגעים ב-lockscreen2_prefs ובמפתחות של PinStore,
 * כדי שהיומן לא ישפיע לעולם על בדיקת הקוד או על ההשהיה אחרי כישלונות.
 * נשמרים רק זמנים - לא הספרות שהוקשו.
 *
 * אחרי פתיחה מוצלחת הבעלים רואים כמה ניסיונות שגויים היו מאז הפעם הקודמת
 * (כמו "ניסיונות כניסה כושלים" באייפון/מק), וההגדרות מציגות את סיכום השבוע.
 */
class SecurityLog(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** קוד שגוי הוזן (במסך הנעילה או בכניסה להגדרות הנעילה). */
    fun onFailedPin() {
        val now = System.currentTimeMillis()
        val times = (failureTimes() + now).takeLast(MAX_ENTRIES)
        // commit: כמו המונה ב-PinStore, ניסיון שנרשם חייב להגיע לדיסק גם אם המכשיר נכבה מיד
        prefs.edit()
            .putString(KEY_TIMES, times.joinToString(","))
            .putInt(KEY_UNSEEN, prefs.getInt(KEY_UNSEEN, 0) + 1)
            .commit()
    }

    /** כמה ניסיונות שגויים היו מאז שהבעלים ראו את ההודעה בפעם האחרונה, ומאפס. */
    fun takeUnseen(): Unseen? {
        val count = prefs.getInt(KEY_UNSEEN, 0)
        if (count <= 0) return null
        prefs.edit().putInt(KEY_UNSEEN, 0).apply()
        return Unseen(count, failureTimes().lastOrNull() ?: 0L)
    }

    /** ניסיונות שגויים בשבעת הימים האחרונים. */
    fun failuresLastWeek(): Int {
        val since = System.currentTimeMillis() - WEEK_MS
        return failureTimes().count { it >= since }
    }

    fun lastFailureAt(): Long = failureTimes().lastOrNull() ?: 0L

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun failureTimes(): List<Long> =
        (prefs.getString(KEY_TIMES, "") ?: "").split(",").mapNotNull { it.toLongOrNull() }

    data class Unseen(val count: Int, val lastAt: Long)

    companion object {
        const val PREFS = "security_log"
        private const val KEY_TIMES = "failed_times"
        private const val KEY_UNSEEN = "failed_unseen"
        private const val MAX_ENTRIES = 50
        private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
    }
}
