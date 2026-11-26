package com.future.systemui.controlcenter

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import com.future.sharednav.root.RootShell
import com.future.systemui.CommandQueue
import com.future.systemui.Feature
import com.future.systemui.fui.recents.logic.RecentAppInfo
import com.future.systemui.fui.recents.logic.RecentAppsManager
import com.future.systemui.fui.recents.logic.RecentSnapshots
import com.future.systemui.fui.recents.ui.RecentAppsScreen
import com.future.systemui.fui.theme.ThemeProvider
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.fui.utils.FutureUIState
import com.future.systemui.power.ComposeOverlay

private const val TAG = "FutureSystemUI"

/**
 * FutureUI's recent apps screen (RecentAppsScreen) hosted by SystemUI. The framework asks for it through
 * showRecentApps / toggleRecentApps / hideRecentApps (the app-switch key); the list comes from the
 * system's own recent tasks and the card pictures from the system task snapshots.
 */
class RecentsFeature : Feature, CommandQueue.Callbacks {

    private var ctx: Context? = null
    private val main = Handler(Looper.getMainLooper())
    private var overlay: ComposeOverlay? = null
    private var showPending: Runnable? = null

    private val apps = mutableStateListOf<RecentAppInfo>()
    private val memory = mutableStateOf<String?>(null)
    private var manager: RecentAppsManager? = null
    private var snapshots: RecentSnapshots? = null

    override fun start(context: Context, commandQueue: CommandQueue) {
        ctx = context
        instance = this
        commandQueue.addCallback(this)
    }

    // ---- CommandQueue.Callbacks ----

    override fun showRecentApps(p0: Boolean) {
        main.post { show() }
    }

    override fun toggleRecentApps() {
        main.post { if (overlay != null) hide() else show() }
    }

    override fun hideRecentApps(p0: Boolean, p1: Boolean) {
        main.post { hide() }
    }

    // ---- public API (key service) ----

    fun toggle() {
        main.post { if (overlay != null) hide() else show() }
    }

    // ---- window ----

    private fun mgr(c: Context): RecentAppsManager = manager ?: RecentAppsManager(c).also { manager = it }

    private fun snaps(c: Context): RecentSnapshots =
        snapshots ?: RecentSnapshots(c, { mgr(c).taskIdOf(it) }, 0).also { snapshots = it }

    private fun show() {
        val c = ctx ?: return
        try {
            if (overlay != null || showPending != null) return
            // Locked (even when the lock screen made room for a call): the recents show pictures of every app.
            if (FutureUIState.isSecured) return
            val m = mgr(c)
            m.syncFromSystem()
            val first = m.getRecentApps().firstOrNull()?.packageName
            if (first == null) {
                showNow(c)
                return
            }
            // A fresh picture of the app in front first, otherwise its card shows where it was left earlier.
            var shown = false
            val go = Runnable {
                if (shown) return@Runnable
                shown = true
                showPending = null
                showNow(c)
            }
            showPending = go
            main.postDelayed(go, 600)
            snaps(c).capture(first) { go.run() }
        } catch (t: Throwable) {
            Log.e(TAG, "showing the recent apps failed", t)
            showPending = null
        }
    }

    private fun showNow(c: Context) {
        try {
            if (overlay != null) return
            val m = mgr(c)
            val s = snaps(c)
            apps.clear()
            apps.addAll(m.getRecentApps())
            memory.value = m.memorySummary()
            val accent = Color(
                c.getSharedPreferences("shared_theme_prefs", Context.MODE_PRIVATE)
                    .getInt(ThemeProvider.COL_PRIMARY_COLOR, android.graphics.Color.WHITE),
            )

            val o = ComposeOverlay(c, fullScreen = true, onKey = { onKey(it) })
            o.setContent {
                FutureUITheme {
                    RecentAppsScreen(
                        apps = apps,
                        snapshots = s.images,
                        accentColor = accent,
                        onLaunch = { app ->
                            try {
                                val intent = c.packageManager.getLaunchIntentForPackage(app.packageName)
                                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                if (intent != null) c.startActivity(intent)
                            } catch (t: Throwable) {
                                Log.e(TAG, "launching a recent app failed", t)
                            }
                            hide()
                        },
                        onClose = { app ->
                            stopApp(m, app.packageName)
                            m.remove(app.packageName)
                            s.remove(app.packageName)
                            apps.remove(app)
                        },
                        onCloseAll = {
                            apps.toList().forEach { stopApp(m, it.packageName) }
                            m.clear()
                            s.clear()
                            apps.clear()
                            hide()
                        },
                        memorySummary = memory.value,
                        onDismiss = { hide() },
                    )
                }
            }
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = o.show(wm, SYSTEM_DIALOG_TYPES) { type ->
                // Focusable: the keypad drives the screen. Not touchable: the device has no touch screen.
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply { title = "FutureRecents" }
            }
            if (shown) {
                overlay = o
                FutureUIState.recentsOpen.value = true
            }
        } catch (t: Throwable) {
            Log.e(TAG, "showing the recent apps failed", t)
            overlay = null
        }
    }

    private fun hide() {
        try {
            showPending?.let { main.removeCallbacks(it) }
            showPending = null
            val o = overlay ?: return
            overlay = null
            o.dismiss()
            FutureUIState.recentsOpen.value = false
        } catch (t: Throwable) {
            Log.e(TAG, "hiding the recent apps failed", t)
        }
    }

    /** BACK closes; every other key goes to the screen's own focus handling. */
    private fun onKey(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) hide()
            return true
        }
        return false
    }

    private fun stopApp(m: RecentAppsManager, pkg: String) {
        try {
            if (!m.forceStop(pkg) && RootShell.isSafeToken(pkg)) {
                Thread { RootShell.run("am force-stop ${RootShell.quote(pkg)}") }.start()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "stopping $pkg failed", t)
        }
    }

    companion object {
        @Volatile
        var instance: RecentsFeature? = null
            private set
    }
}
