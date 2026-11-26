package com.future.systemui.notifications

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ServiceManager
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import com.android.internal.statusbar.IStatusBarService
import com.future.sharednav.actions.FutureUIActions
import com.future.systemui.fui.notificationcenter.logic.NotificationCenterManager
import com.future.systemui.fui.notificationcenter.ui.NotificationCenterScreen
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.fui.utils.FutureUIState
import com.future.systemui.fui.utils.OverlayMotion

/**
 * The window-hosting and BACK handling that FutureUI's NotificationCenterAccessibilityService did, as a plain
 * controller. The content is FutureUI's own [NotificationCenterScreen], unchanged.
 */
private const val TAG = "FutureSystemUI"

class ShadeController(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val main = Handler(Looper.getMainLooper())

    private var root: OverlayRoot? = null
    private var host: OverlayHost? = null
    private var params: WindowManager.LayoutParams? = null
    private var manager: NotificationCenterManager? = null
    private var motion = OverlayMotion()
    private var closing = false

    private var cachedWallpaperId = -1
    private var cachedWallpaper: ImageBitmap? = null

    /** Set by disable(DISABLE_EXPAND). */
    @Volatile
    var expandDisabled = false

    private val forceRemove = Runnable { removeNow() }

    val isVisible: Boolean get() = root != null
    val isOpen: Boolean get() = root != null && !closing

    fun toggle() {
        if (isOpen) hide() else show()
    }

    fun show(fromDirection: Int = 0) {
        try {
            if (root != null) {
                if (closing) {
                    // reopened while the exit animation runs: it turns around from where it is
                    closing = false
                    main.removeCallbacks(forceRemove)
                    setFocusable(true)
                    motion.enter(fromDirection)
                    reportRevealed()
                }
                return
            }
            if (expandDisabled) return
            // like FutureUI: never over the lock screen
            if (NotificationPrivacy.isLocked(context)) return

            val mgr = manager ?: NotificationCenterManager(context).also { manager = it }
            val wallpaper = loadWallpaper()
            motion = OverlayMotion()

            val lp = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                0,
                // Focusable on purpose (no FLAG_NOT_FOCUSABLE): the shade must receive the keypad.
                // No touch (keypad-only device): NOT_TOUCHABLE does not affect key events.
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT,
            )
            lp.title = "FutureNotificationShade"
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                lp.blurBehindRadius = 50
                lp.flags = lp.flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
            }

            val h = OverlayHost()
            val r = OverlayRoot(context) { hide() }
            val compose = ComposeView(context)
            h.attachTo(compose)
            h.attachTo(r)
            val thisMotion = motion
            compose.setContent {
                FutureUITheme {
                    NotificationCenterScreen(
                        modifier = Modifier.fillMaxSize(),
                        wallpaper = wallpaper,
                        manager = mgr,
                        onSwitchToControlCenter = { switchToControlCenter() },
                        motion = thisMotion,
                        onExitFinished = { removeNow() },
                    )
                }
            }
            r.addView(compose)

            thisMotion.enter(fromDirection)
            closing = false
            if (!addOverlayWindow(wm, r, lp)) {
                h.destroy()
                return
            }
            root = r
            host = h
            params = lp
            FutureUIState.notificationCenterOpen.value = true
            // Give the Compose view the Android focus so keypad events reach onKeyEvent handlers.
            compose.requestFocus()
            reportRevealed()
            bringStatusBarFront()
        } catch (t: Throwable) {
            Log.e(TAG, "show shade failed", t)
        }
    }

    /** Starts the exit; the window is removed when it ends ([removeNow]). */
    fun hide(toDirection: Int = 0) {
        try {
            if (root == null || closing) return
            closing = true
            setFocusable(false)
            motion.exit(toDirection)
            main.removeCallbacks(forceRemove)
            main.postDelayed(forceRemove, 900)
            reportHidden()
        } catch (t: Throwable) {
            Log.e(TAG, "hide shade failed", t)
        }
    }

    private fun removeNow() {
        main.removeCallbacks(forceRemove)
        val r = root ?: return
        try { wm.removeViewImmediate(r) } catch (t: Throwable) { Log.w(TAG, "remove shade failed", t) }
        host?.destroy()
        root = null
        host = null
        params = null
        closing = false
        FutureUIState.notificationCenterOpen.value = false
    }

    private fun setFocusable(focusable: Boolean) {
        val r = root ?: return
        val lp = params ?: return
        lp.flags = if (focusable) lp.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else lp.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        try { wm.updateViewLayout(r, lp) } catch (t: Throwable) { Log.w(TAG, "updateViewLayout failed", t) }
    }

    /** Same depth as FutureUI: this one leaves to the right while the control center enters from the left. */
    private fun switchToControlCenter() {
        try {
            hide(toDirection = 1)
            val intent = Intent(FutureUIActions.ACTION_SHOW_CONTROL_CENTER)
            intent.setPackage(context.packageName)
            intent.putExtra(FutureUIActions.EXTRA_FROM_DIRECTION, -1)
            context.sendBroadcast(intent)
        } catch (t: Throwable) {
            Log.e(TAG, "switch to control center failed", t)
        }
    }

    private fun bringStatusBarFront() {
        try {
            context.sendBroadcast(Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT).setPackage(context.packageName))
        } catch (t: Throwable) {
            Log.w(TAG, "bring status bar front failed", t)
        }
    }

    private fun statusBarService(): IStatusBarService? =
        IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))

    private fun reportRevealed() {
        try {
            statusBarService()?.onPanelRevealed(false, NotificationRepository.active.value.size)
        } catch (t: Throwable) {
            Log.w(TAG, "onPanelRevealed failed", t)
        }
    }

    private fun reportHidden() {
        try {
            statusBarService()?.onPanelHidden()
        } catch (t: Throwable) {
            Log.w(TAG, "onPanelHidden failed", t)
        }
    }

    // ---- wallpaper (as in FutureUI) ----

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        // the wallpaper is only a blurred background: at most 1024 on the long side
        val w = drawable.intrinsicWidth.coerceAtLeast(1)
        val h = drawable.intrinsicHeight.coerceAtLeast(1)
        val scale = (1024f / maxOf(w, h)).coerceAtMost(1f)
        val bitmap = Bitmap.createBitmap(
            (w * scale).toInt().coerceAtLeast(1),
            (h * scale).toInt().coerceAtLeast(1),
            Bitmap.Config.ARGB_8888,
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun loadWallpaper(): ImageBitmap? {
        val wmgr = WallpaperManager.getInstance(context)
        val id = runCatching { wmgr.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
        if (id != -1 && id == cachedWallpaperId && cachedWallpaper != null) return cachedWallpaper
        val image = runCatching { wmgr.drawable?.let { drawableToBitmap(it).asImageBitmap() } }.getOrNull()
        cachedWallpaperId = id
        cachedWallpaper = image
        return image
    }

    fun dispose() {
        main.removeCallbacksAndMessages(null)
        removeNow()
        try { manager?.dispose() } catch (t: Throwable) { Log.w(TAG, "manager dispose failed", t) }
        manager = null
    }
}
