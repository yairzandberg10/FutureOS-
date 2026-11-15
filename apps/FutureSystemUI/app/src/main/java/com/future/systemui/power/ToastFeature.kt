package com.future.systemui.power

import android.app.ITransientNotificationCallback
import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Text
import com.future.sharednav.theme.FutureAppTheme
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.LocalFutureTheme
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

/**
 * Text toasts of apps. On Android 12 the framework routes them to SystemUI, which draws a small
 * non-focusable overlay near the bottom of the screen. Touch-free: the overlay never takes focus.
 */
class ToastFeature : Feature, CommandQueue.Callbacks {
    private var ctx: Context? = null
    private val main = Handler(Looper.getMainLooper())
    private var toastOverlay: ComposeOverlay? = null
    private var toastToken: IBinder? = null
    private var toastCallback: ITransientNotificationCallback? = null
    private var hideTask: Runnable? = null

    override fun start(context: Context, commandQueue: CommandQueue) {
        ctx = context
        commandQueue.addCallback(this)
    }

    override fun showToast(
        uid: Int,
        packageName: String?,
        token: IBinder?,
        text: CharSequence?,
        windowToken: IBinder?,
        duration: Int,
        callback: ITransientNotificationCallback?,
    ) {
        val c = ctx ?: return
        try {
            // A new toast replaces the one on screen, as on the framework toast queue.
            dismissToast()

            val message = text?.toString().orEmpty()
            val overlay = ComposeOverlay(c, fullScreen = false, onKey = { false })
            overlay.setContent {
                FutureAppTheme { ToastContent(message) }
            }
            val bottomGap = (BOTTOM_GAP_DP * c.resources.displayMetrics.density).toInt()
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = overlay.show(
                wm,
                intArrayOf(WindowManager.LayoutParams.TYPE_TOAST, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
            ) { type ->
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
                    y = bottomGap
                    // A TYPE_TOAST window must be attached to the app's window token.
                    if (type == WindowManager.LayoutParams.TYPE_TOAST) {
                        this.token = windowToken
                    }
                }
            }
            if (!shown) return

            toastOverlay = overlay
            toastToken = token
            toastCallback = callback
            notifyCallback(callback, "onToastShown")

            val delayMs = if (duration == Toast.LENGTH_SHORT) SHORT_MS else LONG_MS
            val task = Runnable { dismissToast() }
            hideTask = task
            main.postDelayed(task, delayMs)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "showToast failed", t)
        }
    }

    override fun hideToast(packageName: String?, token: IBinder?) {
        if (toastOverlay == null) return
        // Only the app that owns the toast (matching token) may hide it early.
        val owned = toastToken
        if (token != null && owned != null && token != owned) return
        dismissToast()
    }

    private fun dismissToast() {
        hideTask?.let { main.removeCallbacks(it) }
        hideTask = null
        val overlay = toastOverlay ?: return
        val callback = toastCallback
        toastOverlay = null
        toastToken = null
        toastCallback = null
        try {
            overlay.dismiss()
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "dismissing toast failed", t)
        }
        notifyCallback(callback, "onToastHidden")
    }

    /**
     * ITransientNotificationCallback is a hidden framework AIDL interface, so its methods are
     * called through reflection on the interface (the binder proxy class itself is not public).
     */
    private fun notifyCallback(callback: ITransientNotificationCallback?, method: String) {
        if (callback == null) return
        try {
            ITransientNotificationCallback::class.java.getMethod(method).invoke(callback)
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "toast callback $method failed", t)
        }
    }

    private companion object {
        const val SHORT_MS = 2000L
        const val LONG_MS = 3500L
        const val BOTTOM_GAP_DP = 64
    }
}

/** Toast text on the same card surface as the other overlays. */
@Composable
internal fun ToastContent(message: String) {
    val theme = LocalFutureTheme.current
    Box(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(FutureShapes.lg)
            .background(theme.elevatedSurfaceColor)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = theme.textColor,
            fontSize = FutureTypography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
}
