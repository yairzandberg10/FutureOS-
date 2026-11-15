package com.future.systemui.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "FutureSystemUI"

/**
 * In-process store of the active notifications. The listener service writes to it, the shade UI reads it.
 * Same process, so a plain object + StateFlow is enough (no IPC).
 */
object NotificationRepository {
    private val _notifications = MutableStateFlow<List<StatusBarNotification>>(emptyList())
    val notifications: StateFlow<List<StatusBarNotification>> = _notifications

    /** The bound listener (null while not connected). Used to cancel notifications. */
    @Volatile
    var listener: NotificationListenerService? = null

    /** Called on the main thread for every posted notification: (sbn, isNew, importance). */
    @Volatile
    var onPosted: ((StatusBarNotification, Boolean, Int) -> Unit)? = null

    /** Re-read the active notifications from [l], filter and sort them (importance desc, then newest first). */
    fun refresh(l: NotificationListenerService) {
        try {
            val active: Array<StatusBarNotification> = l.activeNotifications ?: emptyArray()
            val ranking = try { l.currentRanking } catch (t: Throwable) { null }
            val importance = HashMap<String, Int>()
            val r = NotificationListenerService.Ranking()
            for (sbn in active) {
                val imp = if (ranking != null && ranking.getRanking(sbn.key, r)) r.importance else 3
                importance[sbn.key] = imp
            }
            val groupsWithChildren = active
                .filter { (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) == 0 }
                .mapNotNull { it.groupKey }
                .toSet()
            val list = active
                // A group summary next to its children is just noise on a list-style shade.
                .filterNot {
                    (it.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0 && it.groupKey in groupsWithChildren
                }
                .sortedWith(
                    compareByDescending<StatusBarNotification> { importance[it.key] ?: 3 }
                        .thenByDescending { it.postTime },
                )
            _notifications.value = list
        } catch (t: Throwable) {
            Log.e(TAG, "NotificationRepository.refresh failed", t)
        }
    }

    fun cancel(key: String) {
        try { listener?.cancelNotification(key) } catch (t: Throwable) { Log.e(TAG, "cancel failed", t) }
    }

    fun cancelAll() {
        try { listener?.cancelAllNotifications() } catch (t: Throwable) { Log.e(TAG, "cancelAll failed", t) }
    }
}
