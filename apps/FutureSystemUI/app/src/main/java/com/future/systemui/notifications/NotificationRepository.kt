package com.future.systemui.notifications

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.compose.runtime.mutableIntStateOf
import com.future.systemui.fui.utils.FutureUIState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

private const val TAG = "FutureSystemUI"

/**
 * In-process store of the active notifications. [FutureNotificationListener] writes to it; the FutureUI notification
 * center (NotificationCenterManager) and the heads-up read it. Same process, so a plain object is enough.
 * All writes happen on the main thread (listener callbacks).
 *
 * Replaces FutureUI's MediaControlService.instance?.activeNotifications.
 */
object NotificationRepository {
    private val _active = MutableStateFlow<List<StatusBarNotification>>(emptyList())

    /** Every active notification, in the listener's order (exactly what FutureUI's notification center used). */
    val active: StateFlow<List<StatusBarNotification>> = _active

    /** Rises on every add/remove/update; the notification center refreshes from it at once (FutureUI's changeTick). */
    val changeTick = mutableIntStateOf(0)

    /** The bound listener (null while not connected). */
    @Volatile
    var listener: NotificationListenerService? = null

    /** Main thread: a notification was posted or updated. */
    @Volatile
    var onPosted: ((StatusBarNotification) -> Unit)? = null

    /** Main thread: a notification was removed. */
    @Volatile
    var onRemoved: ((StatusBarNotification) -> Unit)? = null

    fun refresh(l: NotificationListenerService) {
        try {
            _active.value = (l.activeNotifications ?: emptyArray()).toList()
        } catch (t: Throwable) {
            Log.e(TAG, "NotificationRepository.refresh failed", t)
        }
        bump()
    }

    fun clear() {
        _active.value = emptyList()
        bump()
    }

    private fun bump() {
        try {
            changeTick.intValue++
            // The status bar icons refresh from this (FutureUI's MediaControlService.bumpTick).
            FutureUIState.notificationTick.intValue++
        } catch (t: Throwable) {
            Log.e(TAG, "bump failed", t)
        }
    }

    fun cancel(key: String) {
        try { listener?.cancelNotification(key) } catch (t: Throwable) { Log.e(TAG, "cancel failed", t) }
    }

    fun cancelAll() {
        try { listener?.cancelAllNotifications() } catch (t: Throwable) { Log.e(TAG, "cancelAll failed", t) }
    }

    fun requestRebind(context: Context) {
        try {
            NotificationListenerService.requestRebind(ComponentName(context.packageName, FutureNotificationListener::class.java.name))
        } catch (t: Throwable) {
            Log.w(TAG, "requestRebind failed", t)
        }
    }
}
