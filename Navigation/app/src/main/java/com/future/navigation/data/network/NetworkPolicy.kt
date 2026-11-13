package com.future.navigation.data.network

import android.content.Context
import android.net.ConnectivityManager

/** בודק אם הרשת הפעילה מוגבלת (סלולרית) - הורדה גדולה של נתוני תחבורה מחכה לאישור המשתמש. */
object NetworkPolicy {
    fun isMetered(context: Context): Boolean = runCatching {
        context.getSystemService(ConnectivityManager::class.java).isActiveNetworkMetered
    }.getOrDefault(false)
}
