package com.future.systemui.lockscreen

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import com.future.sharednav.actions.FutureUIActions
import com.future.sharednav.theme.ThemeClient
import com.future.systemui.CommandQueue
import com.future.systemui.Feature
import com.future.systemui.fui.controlcenter.logic.ControlManager
import com.future.systemui.fui.lockscreen.LockScreenController
import java.util.concurrent.Executors

/**
 * FutureOS's own lock layer, running FutureUI's lock screen (same UI, same PIN) as a keyguard-level window.
 * The stock keyguard is not touched. See SECURITY.md for the threat model.
 *
 * - boot / SystemUI restart: [LockScreenController.start] locks before any other feature is usable
 * - screen off: locks at once (controller)
 * - foreground watcher: lets the dialer (call) and the alarm ringing over the lock, takes over again afterwards
 */
class LockScreenFeature : Feature {
    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val accent = mutableStateOf(Color.White)
    private var controller: LockScreenController? = null
    private var appContext: Context? = null
    private var lastTop: String? = null

    override fun start(context: Context, commandQueue: CommandQueue) {
        val ctx = context.applicationContext ?: context
        appContext = ctx
        val wm = context.getSystemService(WindowManager::class.java)
        val owner = LockOwner()
        var controlManager: ControlManager? = null
        val c = LockScreenController(
            service = context,
            owner = owner,
            windowManager = wm,
            controlManager = {
                controlManager ?: runCatching { ControlManager(ctx) }.getOrNull()?.also { controlManager = it }
            },
            accentColor = { accent.value },
            // the status bar window is under the lock window by design
            bringStatusBarFront = {},
        )
        controller = c
        c.start()

        // ---- accent colour (ThemeClient may block: read off the main thread)
        readTheme()
        runCatching {
            ctx.contentResolver.registerContentObserver(ThemeClient.THEME_URI, false, object : ContentObserver(main) {
                override fun onChange(selfChange: Boolean) = readTheme()
            })
        }

        // ---- lock now (only holders of FutureUI's signature|privileged permission can send it)
        ContextCompat.registerReceiver(
            ctx,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    if (intent?.action == ACTION_LOCK_NOW) main.post { controller?.lock() }
                }
            },
            IntentFilter(ACTION_LOCK_NOW),
            FutureUIActions.PERMISSION_SYSTEM,
            null,
            ContextCompat.RECEIVER_EXPORTED,
        )

        // ---- calls from the dialer (same permission), as FutureUI's accessibility service had them
        ContextCompat.registerReceiver(
            ctx,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    when (intent?.action) {
                        FutureUIActions.ACTION_CALL_RINGING -> controller?.onCallRinging()
                        FutureUIActions.ACTION_CALL_ENDED -> controller?.onCallEnded()
                    }
                }
            },
            IntentFilter().apply {
                addAction(FutureUIActions.ACTION_CALL_RINGING)
                addAction(FutureUIActions.ACTION_CALL_ENDED)
            },
            FutureUIActions.PERMISSION_SYSTEM,
            null,
            ContextCompat.RECEIVER_EXPORTED,
        )

        main.postDelayed(watcher, POLL_MS)
        Log.i(TAG, "lock screen feature started")
    }

    private fun readTheme() {
        val ctx = appContext ?: return
        io.execute {
            val color = runCatching { Color(ThemeClient.getTheme(ctx).primaryColor) }.getOrNull() ?: return@execute
            main.post { accent.value = color }
        }
    }

    /**
     * No accessibility service any more, so the foreground app is polled (cheap: one binder call) while the device
     * is locked and the screen is on. Sees the alarm / dialer come up over the lock and the first app that is
     * not allowed while the lock is stepped aside.
     */
    private val watcher = object : Runnable {
        override fun run() {
            val c = controller
            var next = POLL_MS
            if (c != null && c.locked) {
                val interactive = appContext?.getSystemService(PowerManager::class.java)?.isInteractive != false
                if (interactive) {
                    c.checkCall()
                    top()?.let { (pkg, cls) ->
                        val key = "$pkg/$cls"
                        if (c.isSuspended || key != lastTop) c.onForegroundChanged(pkg, cls)
                        lastTop = key
                    }
                    if (c.isSuspended) next = POLL_SUSPENDED_MS
                }
            } else {
                lastTop = null
            }
            main.postDelayed(this, next)
        }
    }

    @Suppress("DEPRECATION")
    private fun top(): Pair<String, String?>? = try {
        val am = appContext?.getSystemService(ActivityManager::class.java)
        val comp = am?.getRunningTasks(1)?.firstOrNull()?.topActivity
        comp?.let { it.packageName to it.className }
    } catch (t: Throwable) {
        null
    }

    companion object {
        private const val TAG = "LockScreenFeature"
        const val ACTION_LOCK_NOW = "com.future.systemui.ACTION_LOCK_NOW"
        private const val POLL_MS = 700L
        private const val POLL_SUSPENDED_MS = 250L
    }
}
