package com.future.systemui.notifications

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.content.ContextCompat
import com.future.sharednav.actions.FutureUIActions
import com.future.sharednav.i18n.Tr
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

private const val TAG = "FutureSystemUI"

/**
 * The notification shade (FutureUI's NotificationCenterScreen) and the heads-up banner (HeadsUpNotificationScreen).
 *
 * Opened by the framework through [CommandQueue.Callbacks] (animateExpandNotificationsPanel / togglePanel) or by the
 * package-private broadcast ACTION_SHOW_NOTIFICATION_CENTER (the control center uses it to switch panels).
 * Closed by BACK, animateCollapsePanels or disable(DISABLE_EXPAND).
 *
 * FutureUI opens it with a long press on '#'. That key is not delivered to SystemUI: whoever intercepts keys
 * (ROM PhoneWindowManager, or the status bar agent) must call StatusBarManager togglePanel / send the broadcast above.
 */
class NotificationShadeFeature : Feature, CommandQueue.Callbacks {

    private lateinit var context: Context
    private var shade: ShadeController? = null
    private var headsUp: HeadsUpController? = null
    private val main = Handler(Looper.getMainLooper())

    override fun start(context: Context, commandQueue: CommandQueue) {
        this.context = context.applicationContext ?: context
        try { Tr.init(this.context) } catch (t: Throwable) { Log.w(TAG, "Tr.init failed", t) }
        try { shade = ShadeController(this.context) } catch (t: Throwable) { Log.e(TAG, "shade controller failed", t) }
        try { headsUp = HeadsUpController(this.context) } catch (t: Throwable) { Log.e(TAG, "heads-up controller failed", t) }
        try { ensureListenerEnabled() } catch (t: Throwable) { Log.e(TAG, "enabling the notification listener failed", t) }

        NotificationRepository.onPosted = { sbn ->
            try {
                headsUp?.suppressed = (shade?.isVisible == true) || (shade?.expandDisabled == true)
                headsUp?.onPosted(sbn)
            } catch (t: Throwable) {
                Log.e(TAG, "heads-up failed", t)
            }
        }
        NotificationRepository.onRemoved = { sbn ->
            try { headsUp?.onRemoved(sbn) } catch (t: Throwable) { Log.e(TAG, "heads-up remove failed", t) }
        }

        try {
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(c: Context?, intent: Intent?) {
                    try {
                        if (intent?.action == FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER) {
                            open(intent.getIntExtra(FutureUIActions.EXTRA_FROM_DIRECTION, 0))
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "show broadcast failed", t)
                    }
                }
            }
            // only SystemUI itself (e.g. the control center switching panels) may open the shade
            ContextCompat.registerReceiver(
                this.context, receiver, IntentFilter(FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "registering the show receiver failed", t)
        }

        commandQueue.addCallback(this)
    }

    // ---- listener enabling (self-enable through Settings.Secure, then rebind) ----

    private fun ensureListenerEnabled() {
        val component = ComponentName(context.packageName, FutureNotificationListener::class.java.name)
        val flat = component.flattenToString()
        val cr = context.contentResolver
        val current = Settings.Secure.getString(cr, ENABLED_LISTENERS).orEmpty()
        val enabled = current.split(':').any { it == flat || ComponentName.unflattenFromString(it) == component }
        if (!enabled) {
            val updated = if (current.isEmpty()) flat else "$current:$flat"
            Settings.Secure.putString(cr, ENABLED_LISTENERS, updated)
            Log.i(TAG, "enabled notification listener $flat")
        }
        NotificationListenerService.requestRebind(component)
    }

    // ---- CommandQueue.Callbacks ----
    // animateExpandSettingsPanel is the control center's (quick settings), not ours.

    override fun animateExpandNotificationsPanel() { open(0) }

    override fun togglePanel() {
        try {
            val s = shade ?: return
            if (s.isOpen) s.hide() else open(0)
        } catch (t: Throwable) {
            Log.e(TAG, "togglePanel failed", t)
        }
    }

    override fun animateCollapsePanels() {
        try { shade?.hide() } catch (t: Throwable) { Log.e(TAG, "collapse failed", t) }
    }

    override fun disable(p0: Int, p1: Int, p2: Int) {
        try {
            if (p0 != 0) return // default display only
            val disabled = (p1 and DISABLE_EXPAND) != 0
            shade?.expandDisabled = disabled
            if (disabled) {
                shade?.hide()
                headsUp?.hide()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "disable failed", t)
        }
    }

    private fun open(fromDirection: Int) {
        try {
            val s = shade ?: return
            if (s.expandDisabled) return
            if (NotificationPrivacy.isLocked(context)) return // never over the lock screen
            headsUp?.hide()
            s.show(fromDirection)
        } catch (t: Throwable) {
            Log.e(TAG, "open shade failed", t)
        }
    }

    private companion object {
        const val ENABLED_LISTENERS = "enabled_notification_listeners"

        /** StatusBarManager.DISABLE_EXPAND */
        const val DISABLE_EXPAND = 0x00010000
    }
}
