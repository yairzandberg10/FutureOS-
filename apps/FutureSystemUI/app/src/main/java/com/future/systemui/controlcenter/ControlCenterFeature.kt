package com.future.systemui.controlcenter

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import com.future.sharednav.actions.FutureUIActions
import com.future.systemui.CommandQueue
import com.future.systemui.Feature
import com.future.systemui.fui.controlcenter.logic.ControlManager
import com.future.systemui.fui.controlcenter.ui.ControlCenterScreen
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.fui.utils.FutureUIState
import com.future.systemui.fui.utils.OverlayMotion
import com.future.systemui.power.ComposeOverlay
import com.future.systemui.power.PowerMenuFeature

private const val TAG = "FutureSystemUI"

/**
 * FutureUI's control center (ControlCenterScreen) hosted by SystemUI instead of by
 * ControlCenterAccessibilityService. Same window behaviour: focusable full-screen window with blur
 * behind, animated enter/exit through [OverlayMotion], BACK closes, long press on * toggles
 * (when a key source delivers it, see [ControlCenterKeyService]).
 */
class ControlCenterFeature : Feature, CommandQueue.Callbacks {

    private var ctx: Context? = null
    private val main = Handler(Looper.getMainLooper())
    private var overlay: ComposeOverlay? = null
    private var params: WindowManager.LayoutParams? = null
    private var motion: OverlayMotion? = null
    private var closing = false
    private var controlManager: ControlManager? = null

    private var cachedWallpaperId = -1
    private var cachedWallpaper: ImageBitmap? = null

    private val forceRemove = Runnable { removeNow() }
    private val starLongPress = Runnable { toggle() }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            try {
                if (intent?.action == FutureUIActions.ACTION_SHOW_CONTROL_CENTER) {
                    val from = intent.getIntExtra(FutureUIActions.EXTRA_FROM_DIRECTION, 0)
                    main.post { open(from) }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "control center receiver failed", t)
            }
        }
    }

    override fun start(context: Context, commandQueue: CommandQueue) {
        ctx = context
        instance = this
        try {
            ContextCompat.registerReceiver(
                context, receiver, IntentFilter(FutureUIActions.ACTION_SHOW_CONTROL_CENTER),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
        } catch (t: Throwable) {
            Log.e(TAG, "registering the control center receiver failed", t)
        }
        commandQueue.addCallback(this)
    }

    // ---- CommandQueue.Callbacks ----

    override fun animateExpandSettingsPanel(p0: String?) {
        main.post { open(0) }
    }

    override fun animateCollapsePanels() {
        main.post { close() }
    }

    // ---- public API (key service, other features) ----

    fun toggle() {
        main.post { if (overlay != null && !closing) close() else open(0) }
    }

    fun open(fromDirection: Int = 0) {
        val c = ctx ?: return
        try {
            if (overlay != null) {
                // Re-opened in the middle of the exit: the animation reverses from where it is.
                if (closing) {
                    closing = false
                    main.removeCallbacks(forceRemove)
                    setFocusable(true)
                    motion?.enter(fromDirection)
                }
                return
            }
            if (FutureUIState.isLocked || FutureUIState.isSecured) return

            val cm = controlManager ?: ControlManager(c).also { controlManager = it }
            val wallpaper = loadWallpaper(c)
            val m = OverlayMotion()
            motion = m
            m.enter(fromDirection)

            val o = ComposeOverlay(c, fullScreen = true, onKey = { onKey(it) })
            o.setContent {
                FutureUITheme {
                    Box(modifier = Modifier.fillMaxSize()) {
                        ControlCenterScreen(
                            modifier = Modifier.fillMaxSize(),
                            isVisibleByDefault = true,
                            wallpaper = wallpaper,
                            controlManager = cm,
                            onPowerClick = {
                                close()
                                // One power menu for the whole system; it opens as the control center leaves.
                                main.postDelayed({ PowerMenuFeature.instance?.showGlobalActionsMenu() }, 120)
                            },
                            onSettingsClick = {
                                cm.openMainSettings()
                                close()
                            },
                            onSwitchToNotificationCenter = {
                                // Both layers sit at the same depth: this one leaves to the left while
                                // the notification center enters from the right.
                                close(toDirection = -1)
                                try {
                                    c.sendBroadcast(
                                        Intent(FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER)
                                            .setPackage(c.packageName)
                                            .putExtra(FutureUIActions.EXTRA_FROM_DIRECTION, 1),
                                    )
                                } catch (t: Throwable) {
                                    Log.e(TAG, "switching to the notification center failed", t)
                                }
                            },
                            onRequestClose = { close() },
                            motion = m,
                            onExitFinished = { removeNow() },
                        )
                    }
                }
            }
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = o.show(wm, SYSTEM_DIALOG_TYPES) { type ->
                // Focusable on purpose (no FLAG_NOT_FOCUSABLE): the keypad must reach the screen.
                // NOT_TOUCHABLE: the device has no touch screen.
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_BLUR_BEHIND,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) blurBehindRadius = 50
                    title = "FutureControlCenter"
                }
            }
            if (!shown) {
                motion = null
                return
            }
            overlay = o
            params = o.lastParams
            closing = false
            FutureUIState.controlCenterOpen.value = true
        } catch (t: Throwable) {
            Log.e(TAG, "opening the control center failed", t)
            overlay = null
            motion = null
        }
    }

    /** Starts the exit animation; the window is removed when it ends ([removeNow]). */
    fun close(toDirection: Int = 0) {
        try {
            if (overlay == null || closing) return
            closing = true
            // Keys go back to the app underneath immediately, not only after the animation.
            setFocusable(false)
            motion?.exit(toDirection)
            main.removeCallbacks(forceRemove)
            main.postDelayed(forceRemove, 900)
        } catch (t: Throwable) {
            Log.e(TAG, "closing the control center failed", t)
        }
    }

    private fun removeNow() {
        main.removeCallbacks(forceRemove)
        val o = overlay ?: return
        try {
            o.dismiss()
        } catch (t: Throwable) {
            Log.e(TAG, "removing the control center failed", t)
        }
        overlay = null
        params = null
        motion = null
        closing = false
        FutureUIState.controlCenterOpen.value = false
    }

    private fun setFocusable(focusable: Boolean) {
        val o = overlay ?: return
        val p = params ?: return
        p.flags = if (focusable) p.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else p.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        o.updateLayout(p)
    }

    /** Every key of the window. BACK closes; a long press on * toggles (fallback when no key service). */
    private fun onKey(event: KeyEvent): Boolean {
        try {
            if (closing) return false
            when (event.keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (event.action == KeyEvent.ACTION_UP) close()
                    return true
                }
                KeyEvent.KEYCODE_STAR -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            main.removeCallbacks(starLongPress)
                            main.postDelayed(starLongPress, 600)
                        }
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        main.removeCallbacks(starLongPress)
                    }
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "control center key failed", t)
        }
        return false
    }

    // ---- wallpaper (cached by wallpaper id, scaled down: it is only shown blurred) ----

    private fun loadWallpaper(c: Context): ImageBitmap? {
        val wm = WallpaperManager.getInstance(c)
        val id = runCatching { wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
        if (id != -1 && id == cachedWallpaperId && cachedWallpaper != null) return cachedWallpaper
        val image = runCatching { wm.drawable?.let { drawableToBitmap(it).asImageBitmap() } }.getOrNull()
        cachedWallpaperId = id
        cachedWallpaper = image
        return image
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
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

    companion object {
        @Volatile
        var instance: ControlCenterFeature? = null
            private set
    }
}
