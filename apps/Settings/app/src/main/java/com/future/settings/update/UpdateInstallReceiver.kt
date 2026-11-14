package com.future.settings.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * תוצאה של session התקנה. לא חשוף (exported=false): רק ה-PendingIntent של PackageInstaller,
 * שנוצר ב-UpdateRepository.commitSession, מגיע לכאן.
 */
class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != UpdateRepository.ACTION_INSTALL_STATUS) return
        UpdateRepository.onInstallStatus(context.applicationContext, intent)
    }
}
