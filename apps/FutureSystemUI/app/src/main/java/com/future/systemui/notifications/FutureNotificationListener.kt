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
            if (NotificationRepository.listener === this) NotificationRepository.listener = null
        } catch (t: Throwable) {
            Log.e(TAG, "onListenerDisconnected failed", t)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        try {
            if (sbn == null) return
            val wasKnown = NotificationRepository.notifications.value.any { it.key == sbn.key }
            NotificationRepository.refresh(this)
            var importance = IMPORTANCE_DEFAULT_FALLBACK
            if (rankingMap != null) {
                val r = Ranking()
                if (rankingMap.getRanking(sbn.key, r)) importance = r.importance
            }
            NotificationRepository.onPosted?.invoke(sbn, !wasKnown, importance)
        } catch (t: Throwable) {
            Log.e(TAG, "onNotificationPosted failed", t)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?, rankingMap: RankingMap?) {
        try {
            NotificationRepository.refresh(this)
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

    private companion object {
        /** android.app.NotificationManager.IMPORTANCE_DEFAULT */
        const val IMPORTANCE_DEFAULT_FALLBACK = 3
    }
}
