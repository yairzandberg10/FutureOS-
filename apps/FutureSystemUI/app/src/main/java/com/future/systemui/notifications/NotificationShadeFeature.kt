package com.future.systemui.notifications

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.os.ServiceManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.android.internal.statusbar.IStatusBarService
import com.future.sharednav.i18n.Tr
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

private const val TAG = "FutureSystemUI"

/**
 * The notification shade: a full-screen keypad-driven overlay plus a heads-up card.
 * Opened by the framework through [CommandQueue.Callbacks] (expand / toggle), closed by BACK or collapse.
 */
class NotificationShadeFeature : Feature, CommandQueue.Callbacks {

    private lateinit var context: Context
    private lateinit var wm: WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var shadeRoot: ShadeRootView? = null
    private var shadeHost: OverlayHost? = null
    private var shadeState: MutableTransitionState<Boolean>? = null
    private var shadeRemoval: Runnable? = null

    private var headsView: ComposeView? = null
    private var headsHost: OverlayHost? = null
    private var headsState: MutableTransitionState<Boolean>? = null
    private val headsCurrent = mutableStateOf<StatusBarNotification?>(null)
    private var headsHide: Runnable? = null
    private var headsRemoval: Runnable? = null

    private var expandDisabled = false

    private val isOpen: Boolean get() = shadeRoot != null && shadeState?.targetState == true

    override fun start(context: Context, commandQueue: CommandQueue) {
        this.context = context.applicationContext ?: context
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        try { Tr.init(this.context) } catch (t: Throwable) { Log.w(TAG, "Tr.init failed", t) }
        try { ensureListenerEnabled() } catch (t: Throwable) { Log.e(TAG, "enabling the notification listener failed", t) }
        NotificationRepository.onPosted = { sbn, isNew, importance ->
            main.post { try { onPosted(sbn, isNew, importance) } catch (t: Throwable) { Log.e(TAG, "heads-up failed", t) } }
        }
        commandQueue.addCallback(this)
    }

    // ---- listener enabling ----

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

    override fun animateExpandNotificationsPanel() { openShade() }

    override fun animateExpandSettingsPanel(p0: String?) { openShade() }

    override fun togglePanel() { if (isOpen) closeShade() else openShade() }

    override fun animateCollapsePanels() { closeShade() }

    override fun disable(p0: Int, p1: Int, p2: Int) {
        try {
            if (p0 != 0) return // default display only
            expandDisabled = (p1 and DISABLE_EXPAND) != 0
            if (expandDisabled) {
                closeShade()
                hideHeadsUp()
            }
        } catch (t: Throwable) {
            Log.e(TAG, "disable failed", t)
        }
    }

    // ---- shade window ----

    private fun openShade() {
        try {
            if (expandDisabled || isOpen) return
            hideHeadsUp()
            removeShadeNow() // a previous close may still be animating out

            val host = OverlayHost()
            val state = MutableTransitionState(false)
            val root = ShadeRootView(context) { closeShade() }
            val compose = ComposeView(context)
            host.attachTo(compose)
            host.attachTo(root)
            compose.setContent {
                ShadeScreen(
                    visibleState = state,
                    notifications = NotificationRepository.notifications,
                    onOpen = { sbn -> openNotification(sbn) },
                    onClear = { sbn -> NotificationRepository.cancel(sbn.key) },
                    onClearAll = {
                        NotificationRepository.cancelAll()
                        closeShade()
                    },
                )
            }
            root.addView(compose)

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                0,
                // Focusable on purpose (no FLAG_NOT_FOCUSABLE): the shade must receive the keypad.
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT,
            )
            lp.gravity = Gravity.TOP or Gravity.START
            lp.title = "FutureNotificationShade"
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN

            if (!addWithFallback(root, lp)) return
            shadeRoot = root
            shadeHost = host
            shadeState = state
            state.targetState = true
            // Give the Compose view the Android focus so keypad events reach onKeyEvent handlers.
            compose.requestFocus()

            val count = NotificationRepository.notifications.value.size
            try {
                statusBarService()?.onPanelRevealed(false, count)
            } catch (t: Throwable) {
                Log.w(TAG, "onPanelRevealed failed", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "openShade failed", t)
        }
    }

    private fun closeShade() {
        try {
            val state = shadeState ?: return
            if (shadeRoot == null || !state.targetState) return
            state.targetState = false
            val r = Runnable { removeShadeNow() }
            shadeRemoval = r
            main.postDelayed(r, EXIT_MILLIS)
            try {
                statusBarService()?.onPanelHidden()
            } catch (t: Throwable) {
                Log.w(TAG, "onPanelHidden failed", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "closeShade failed", t)
        }
    }

    private fun removeShadeNow() {
        shadeRemoval?.let { main.removeCallbacks(it) }
        shadeRemoval = null
        val root = shadeRoot ?: return
        shadeRoot = null
        shadeState = null
        try { wm.removeViewImmediate(root) } catch (t: Throwable) { Log.w(TAG, "remove shade failed", t) }
        shadeHost?.destroy()
        shadeHost = null
    }

    private fun openNotification(sbn: StatusBarNotification) {
        try {
            val pi: PendingIntent? = sbn.notification.contentIntent
            closeShade()
            if (pi != null) {
                try { pi.send() } catch (e: PendingIntent.CanceledException) { Log.w(TAG, "contentIntent cancelled", e) }
            }
            if ((sbn.notification.flags and android.app.Notification.FLAG_AUTO_CANCEL) != 0 && sbn.isClearable) {
                NotificationRepository.cancel(sbn.key)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "openNotification failed", t)
        }
    }

    // ---- heads-up ----

    private fun onPosted(sbn: StatusBarNotification, isNew: Boolean, importance: Int) {
        if (!isNew || expandDisabled || isOpen) return
        if (importance < IMPORTANCE_HIGH) return
        val flags = sbn.notification.flags
        if ((flags and android.app.Notification.FLAG_GROUP_SUMMARY) != 0) return
        if ((flags and android.app.Notification.FLAG_ONGOING_EVENT) != 0) return
        showHeadsUp(sbn)
    }

    private fun showHeadsUp(sbn: StatusBarNotification) {
        try {
            headsCurrent.value = sbn
            headsHide?.let { main.removeCallbacks(it) }
            headsRemoval?.let { main.removeCallbacks(it) }
            headsRemoval = null
            if (headsView == null) {
                val host = OverlayHost()
                val state = MutableTransitionState(false)
                val view = ComposeView(context)
                host.attachTo(view)
                view.setContent { HeadsUpCard(state, headsCurrent.value) }
                val lp = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    0,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    PixelFormat.TRANSLUCENT,
                )
                lp.gravity = Gravity.TOP or Gravity.START
                // Below the 28dp status bar.
                lp.y = (28 * context.resources.displayMetrics.density).toInt()
                lp.title = "FutureHeadsUp"
                if (!addWithFallback(view, lp)) {
                    host.destroy()
                    return
                }
                headsView = view
                headsHost = host
                headsState = state
            }
            headsState?.targetState = true
            val hide = Runnable { hideHeadsUp() }
            headsHide = hide
            main.postDelayed(hide, HEADS_UP_MILLIS)
        } catch (t: Throwable) {
            Log.e(TAG, "showHeadsUp failed", t)
        }
    }

    private fun hideHeadsUp() {
        try {
            headsHide?.let { main.removeCallbacks(it) }
            headsHide = null
            val view = headsView ?: return
            headsState?.targetState = false
            headsRemoval?.let { main.removeCallbacks(it) }
            val r = Runnable {
                try { wm.removeViewImmediate(view) } catch (t: Throwable) { Log.w(TAG, "remove heads-up failed", t) }
                headsHost?.destroy()
                headsHost = null
                headsView = null
                headsState = null
                headsRemoval = null
            }
            headsRemoval = r
            main.postDelayed(r, EXIT_MILLIS)
        } catch (t: Throwable) {
            Log.e(TAG, "hideHeadsUp failed", t)
        }
    }

    // ---- helpers ----

    /** Tries TYPE_NOTIFICATION_SHADE, then TYPE_STATUS_BAR_SUB_PANEL, then TYPE_APPLICATION_OVERLAY. */
    private fun addWithFallback(view: View, lp: WindowManager.LayoutParams): Boolean {
        for (type in WINDOW_TYPES) {
            try {
                lp.type = type
                wm.addView(view, lp)
                return true
            } catch (t: Throwable) {
                Log.w(TAG, "addView with window type $type failed: $t")
            }
        }
        Log.e(TAG, "no window type worked for ${lp.title}")
        return false
    }

    private fun statusBarService(): IStatusBarService? =
        IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))

    private companion object {
        const val ENABLED_LISTENERS = "enabled_notification_listeners"

        /** StatusBarManager.DISABLE_EXPAND */
        const val DISABLE_EXPAND = 0x00010000

        /** NotificationManager.IMPORTANCE_HIGH */
        const val IMPORTANCE_HIGH = 4

        const val HEADS_UP_MILLIS = 4000L

        /** A bit longer than the exit transition (FutureMotion.DurationFast). */
        const val EXIT_MILLIS = 260L

        /** TYPE_NOTIFICATION_SHADE (2040), TYPE_STATUS_BAR_SUB_PANEL (2017), TYPE_APPLICATION_OVERLAY (2038). */
        val WINDOW_TYPES = intArrayOf(2040, 2017, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
    }
}
