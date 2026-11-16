package com.future.navigation.data.routing

import android.content.Context

/**
 * העדפות נהיגה (בסגנון Waze) שנשמרות בין נסיעות. כרגע: הימנעות מכבישי אגרה
 * (כביש 6 וכו'). נקרא גם בחישוב מחדש באמצע ניווט, כדי שהמסלול החדש לא יחזור לאגרה.
 */
object DrivingPrefs {
    private const val PREFS = "driving_prefs"
    private const val KEY_AVOID_TOLLS = "avoid_tolls"

    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    private val prefs get() = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var avoidTolls: Boolean
        get() = prefs?.getBoolean(KEY_AVOID_TOLLS, false) ?: false
        set(value) {
            prefs?.edit()?.putBoolean(KEY_AVOID_TOLLS, value)?.apply()
        }
}
