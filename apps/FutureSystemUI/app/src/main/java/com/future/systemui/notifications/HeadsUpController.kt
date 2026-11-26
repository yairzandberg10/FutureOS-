package com.future.systemui.notifications

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import com.future.sharednav.actions.FutureUIActions
import com.future.systemui.fui.notificationcenter.ui.HeadsUpNotificationScreen
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.fui.utils.FrostedBackdrop
import com.future.systemui.fui.utils.safeText

private const val TAG = "FutureSystemUI"

/**
 * FutureUI's HeadsUpNotificationService (+ the filter of MediaControlService.shouldShowHeadsUp) as a plain controller.
 * One banner at a time: a notification arriving while one shows only swaps its content ([generation]).
 * The content is FutureUI's own [HeadsUpNotificationScreen].
 */
class HeadsUpController(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())

    private val current = mutableStateOf<StatusBarNotification?>(null)
    private val generation = mutableIntStateOf(0)
    private val frost = mutableStateOf<FrostedBackdrop.Frost?>(null)
    private val redacted = mutableStateOf(false)

    private var view: ComposeView? = null
    private var host: OverlayHost? = null
    private var pendingShow: Runnable? = null
    private var shownKey: String? = null

    // keys that already got a banner: a silent update (FLAG_ONLY_ALERT_ONCE) must not pop up again
    private val bannerShown = HashSet<String>()

    /** Set while the shade is open or expand is disabled. */
    @Volatile
    var suppressed = false

    fun onPosted(sbn: StatusBarNotification) {
        try {
            if (!shouldShow(sbn)) return
            bannerShown.add(sbn.key)
            show(sbn)
        } catch (t: Throwable) {
            Log.e(TAG, "heads-up onPosted failed", t)
        }
    }

    /** An answered/rejected call removes its ringing notification: the banner closes at once. */
    fun onRemoved(sbn: StatusBarNotification) {
        try {
            bannerShown.remove(sbn.key)
            if (shownKey == sbn.key) hide()
        } catch (t: Throwable) {
            Log.e(TAG, "heads-up onRemoved failed", t)
        }
    }

    private fun shouldShow(sbn: StatusBarNotification): Boolean {
        if (suppressed) return false
        if (sbn.packageName == context.packageName) return false
        val n = sbn.notification
        if ((n.flags and Notification.FLAG_ONLY_ALERT_ONCE) != 0 && sbn.key in bannerShown) return false
        // The lock screen lists notifications itself; only a ringing call may pop up over a locked device.
        val locked = NotificationPrivacy.isLocked(context)
        val isCall = n.category == Notification.CATEGORY_CALL
        if (locked && !isCall) return false
        val isRingingCall = isCall && n.fullScreenIntent != null
        if (sbn.isOngoing && !isRingingCall) return false
        val title = n.safeText(Notification.EXTRA_TITLE)
        val text = n.safeText(Notification.EXTRA_TEXT)
        if (title.isNullOrBlank() && text.isNullOrBlank()) return false
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm != null && nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return false
        return true
    }

    private fun show(sbn: StatusBarNotification) {
        current.value = sbn
        redacted.value = NotificationPrivacy.isLocked(context) && NotificationPrivacy.isSensitive(sbn)
        when {
            view != null -> {
                generation.intValue++
                shownKey = sbn.key
            }
            pendingShow != null -> Unit // the capture is on its way; the banner shows the latest notification
            else -> {
                generation.intValue = 0
                frost.value = null
                var shown = false
                val showNow = Runnable {
                    if (!shown) {
                        shown = true
                        pendingShow = null
                        showBanner()
                    }
                }
                pendingShow = showNow
                main.postDelayed(showNow, FROST_WAIT_MILLIS)
                val regionPx = (FROST_REGION_DP * context.resources.displayMetrics.density).toInt()
                try {
                    FrostedBackdrop.capture(regionPx) { result ->
                        if (!shown) {
                            frost.value = result
                            main.removeCallbacks(showNow)
                            showNow.run()
                        }
                    }
                } catch (t: Throwable) {
                    Log.w(TAG, "frost capture failed", t)
                }
            }
        }
    }

    private fun showBanner() {
        try {
            removeBanner()
            val sbn = current.value ?: return
            // the device locked between the post and now: no content over the lock screen (a call is the exception)
            if (NotificationPrivacy.isLocked(context) && sbn.notification.category != Notification.CATEGORY_CALL) return
            if (suppressed) return

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                0,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            )
            lp.gravity = Gravity.TOP
            lp.title = "FutureHeadsUp"

            val h = OverlayHost()
            val root = OverlayRoot(context)
            val compose = ComposeView(context)
            h.attachTo(compose)
            h.attachTo(root)
            compose.setContent {
                FutureUITheme {
                    val s = current.value ?: return@FutureUITheme
                    // A ringing call stays far longer than a normal banner (FutureUI).
                    val autoDismissMillis = if (s.notification.category == Notification.CATEGORY_CALL) 30000L else 4500L
                    HeadsUpNotificationScreen(
                        sbn = s,
                        generation = generation.intValue,
                        frost = frost.value,
                        autoDismissMillis = autoDismissMillis,
                        redacted = redacted.value,
                        onDismissed = { hide() },
                    )
                }
            }
            root.addView(compose)
            if (!addOverlayWindow(wm, root, lp)) {
                h.destroy()
                return
            }
            view = compose
            rootView = root
            host = h
            shownKey = sbn.key
            try {
                context.sendBroadcast(Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT).setPackage(context.packageName))
            } catch (t: Throwable) {
                Log.w(TAG, "bring status bar front failed", t)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "showBanner failed", t)
        }
    }

    private var rootView: OverlayRoot? = null

    fun hide() {
        try {
            pendingShow?.let { main.removeCallbacks(it) }
            pendingShow = null
            removeBanner()
        } catch (t: Throwable) {
            Log.e(TAG, "hide heads-up failed", t)
        }
    }

    private fun removeBanner() {
        shownKey = null
        val root = rootView
        rootView = null
        view = null
        if (root != null) {
            try { wm.removeViewImmediate(root) } catch (t: Throwable) { Log.w(TAG, "remove banner failed", t) }
        }
        host?.destroy()
        host = null
    }

    fun dispose() {
        hide()
        main.removeCallbacksAndMessages(null)
    }

    private companion object {
        /** How long to wait for the glass capture before showing without it. */
        const val FROST_WAIT_MILLIS = 180L

        /** Height of the captured strip: status bar, spacing and the banner. */
        const val FROST_REGION_DP = 170
    }
}
