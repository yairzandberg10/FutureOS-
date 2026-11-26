package com.future.systemui.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

private const val TAG = "FutureSystemUI"

/** Feeds [NotificationRepository]. Enabled by [NotificationShadeFeature] through Settings.Secure. */
class FutureNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        try {
            NotificationRepository.listener = this
            NotificationRepository.refresh(this)
            Log.i(TAG, "notification listener connected")
        } catch (t: Throwable) {
            Log.e(TAG, "onListenerConnected failed", t)
        }
    }

    override fun onListenerDisconnected() {
        try {
            if (NotificationRepository.listener === this) {
                NotificationRepository.listener = null
                NotificationRepository.clear()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onListenerDisconnected failed", t)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        try {
            if (sbn == null) return
            NotificationRepository.refresh(this)
            NotificationRepository.onPosted?.invoke(sbn)
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationPosted failed", t)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        try {
            NotificationRepository.refresh(this)
            if (sbn != null) NotificationRepository.onRemoved?.invoke(sbn)
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationRemoved failed", t)
        }
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        try {
            NotificationRepository.refresh(this)
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationRankingUpdate failed", t)
        }
    }
}
