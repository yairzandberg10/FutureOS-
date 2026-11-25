package com.future.flashlight.data

import android.content.Context
import android.util.Log
import com.future.flashlight.widget.FlashlightStateWidgetProvider
import com.future.flashlight.widget.FlashlightTimeWidgetProvider
import com.future.sharednav.widget.FutureContentWidget
import java.util.Calendar

/** מצב הפנס האחרון וסטטיסטיקת היום (זמן דלוק והדלקות), לווידג'טים. */
data class TorchSnapshot(
    val on: Boolean,
    val changedAt: Long,
    val minutesToday: Long,
    val sessionsToday: Int,
)

/**
 * שומר ב-SharedPreferences את מצב הפנס בכל מעבר (מהאפליקציה, מהשירות או
 * ממרכז הבקרה כשהמאזין פעיל). ווידג'ט לא יכול לשאול את Camera2 על המצב,
 * ולכן הוא קורא מכאן. המעבר נספר פעם אחת בלבד: רק כששווה לשינוי מהמצב השמור.
 */
object TorchStateStore {
    private const val PREFS = "flashlight_state"
    private const val KEY_ON = "on"
    private const val KEY_CHANGED = "changed"
    private const val KEY_SINCE = "since"
    private const val KEY_DAY = "day"
    private const val KEY_MS_TODAY = "ms_today"
    private const val KEY_SESSIONS = "sessions_today"
    private const val TAG = "TorchStateStore"

    fun record(context: Context, on: Boolean, now: Long = System.currentTimeMillis()) {
        try {
            val app = context.applicationContext
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val wasOn = prefs.getBoolean(KEY_ON, false)
            val day = dayKey(now)
            val sameDay = prefs.getString(KEY_DAY, null) == day
            var msToday = if (sameDay) prefs.getLong(KEY_MS_TODAY, 0L) else 0L
            var sessions = if (sameDay) prefs.getInt(KEY_SESSIONS, 0) else 0
            val editor = prefs.edit()
            if (on && !wasOn) {
                editor.putLong(KEY_SINCE, now)
                sessions += 1
            } else if (!on && wasOn) {
                val since = prefs.getLong(KEY_SINCE, now)
                msToday += (now - maxOf(since, startOfDay(now))).coerceAtLeast(0L)
            }
            editor.putBoolean(KEY_ON, on)
                .putLong(KEY_CHANGED, now)
                .putString(KEY_DAY, day)
                .putLong(KEY_MS_TODAY, msToday)
                .putInt(KEY_SESSIONS, sessions)
                .apply()
            if (on != wasOn || !sameDay) {
                FutureContentWidget.refresh(app, FlashlightStateWidgetProvider::class.java)
                FutureContentWidget.refresh(app, FlashlightTimeWidgetProvider::class.java)
            }
        } catch (e: Exception) {
            Log.w(TAG, "שמירת מצב הפנס נכשלה", e)
        }
    }

    fun read(context: Context, now: Long = System.currentTimeMillis()): TorchSnapshot {
        val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val sameDay = prefs.getString(KEY_DAY, null) == dayKey(now)
        val on = prefs.getBoolean(KEY_ON, false)
        var msToday = if (sameDay) prefs.getLong(KEY_MS_TODAY, 0L) else 0L
        // פנס שדלוק עכשיו - מוסיפים גם את הזמן הרץ עד עכשיו
        if (on && sameDay) {
            msToday += (now - maxOf(prefs.getLong(KEY_SINCE, now), startOfDay(now))).coerceAtLeast(0L)
        }
        return TorchSnapshot(
            on = on,
            changedAt = prefs.getLong(KEY_CHANGED, 0L),
            minutesToday = msToday / 60_000L,
            sessionsToday = if (sameDay) prefs.getInt(KEY_SESSIONS, 0) else 0,
        )
    }

    private fun dayKey(millis: Long): String {
        val c = Calendar.getInstance().apply { timeInMillis = millis }
        return "%04d-%02d-%02d".format(c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH))
    }

    private fun startOfDay(millis: Long): Long = Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
