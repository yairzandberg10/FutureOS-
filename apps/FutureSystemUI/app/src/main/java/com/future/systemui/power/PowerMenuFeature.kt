package com.future.systemui.power

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.FutureAppTheme
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

/**
 * Power menu (global actions) and shutdown screen for FutureSystemUI.
 *
 * The menu is a focusable full-screen window; the D-pad moves the focus, OK/CENTER activates,
 * BACK closes. It opens with the focus on "ביטול" so a stray OK does not power the phone off.
 */
class PowerMenuFeature : Feature, CommandQueue.Callbacks {
    private var ctx: Context? = null
    private var menuOverlay: ComposeOverlay? = null
    private var menuState: PowerMenuState? = null
    private var shutdownOverlay: ComposeOverlay? = null

    override fun start(context: Context, commandQueue: CommandQueue) {
        ctx = context
        commandQueue.addCallback(this)
    }

    override fun showGlobalActionsMenu() {
        val c = ctx ?: return
        if (menuOverlay != null) return
        try {
            val state = PowerMenuState(
                initialFocus = PowerAction.CANCEL.ordinal,
                airplaneOn = isAirplaneOn(c),
            )
            val overlay = ComposeOverlay(c, fullScreen = true, onKey = { onMenuKey(it) })
            overlay.setContent {
                FutureAppTheme { PowerMenuContent(state) }
            }
            menuState = state
            menuOverlay = overlay
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = overlay.show(
                wm,
                intArrayOf(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
            ) { type ->
                // Focusable on purpose: the window must receive the D-pad and BACK keys.
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                )
            }
            if (!shown) {
                menuState = null
                menuOverlay = null
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "showGlobalActionsMenu failed", t)
            menuState = null
            menuOverlay = null
        }
    }

    override fun showShutdownUi(isReboot: Boolean, reason: String?) {
        val c = ctx ?: return
        try {
            dismissMenu()
            if (shutdownOverlay != null) return
            val text = if (isReboot) tr("מפעיל מחדש...") else tr("מכבה...")
            val overlay = ComposeOverlay(c, fullScreen = true, onKey = { true })
            overlay.setContent {
                FutureAppTheme { ShutdownContent(text) }
            }
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = overlay.show(
                wm,
                intArrayOf(WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY),
            ) { type ->
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.OPAQUE,
                )
            }
            if (shown) shutdownOverlay = overlay
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "showShutdownUi failed", t)
        }
    }

    /** Returns true if the key was handled by the menu (so nothing underneath gets it). */
    private fun onMenuKey(event: KeyEvent): Boolean {
        val state = menuState ?: return false
        when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_BACK -> Unit
            else -> return false
        }
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> state.focus = (state.focus - 1).coerceAtLeast(0)
                KeyEvent.KEYCODE_DPAD_DOWN -> state.focus = (state.focus + 1).coerceAtMost(PowerAction.values().lastIndex)
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (event.repeatCount == 0) activate(PowerAction.values()[state.focus])
                }
                KeyEvent.KEYCODE_BACK -> dismissMenu()
            }
        }
        // Consume key-up too, so the app underneath does not see half a key press.
        return true
    }

    private fun activate(action: PowerAction) {
        val c = ctx ?: return
        when (action) {
            PowerAction.POWER_OFF -> {
                dismissMenu()
                powerOff(c)
            }
            PowerAction.RESTART -> {
                dismissMenu()
                restart(c)
            }
            PowerAction.AIRPLANE -> toggleAirplane(c)
            PowerAction.CANCEL -> dismissMenu()
        }
    }

    private fun dismissMenu() {
        val overlay = menuOverlay ?: return
        menuOverlay = null
        menuState = null
        overlay.dismiss()
    }

    /** Hidden PowerManager.shutdown(confirm, reason, wait) - no confirmation dialog, does not wait. */
    private fun powerOff(c: Context) {
        try {
            val pm = c.getSystemService(PowerManager::class.java)
            val method = PowerManager::class.java.getMethod(
                "shutdown",
                Boolean::class.javaPrimitiveType!!,
                String::class.java,
                Boolean::class.javaPrimitiveType!!,
            )
            method.invoke(pm, false, "userrequested", false)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "power off failed", t)
        }
    }

    private fun restart(c: Context) {
        try {
            c.getSystemService(PowerManager::class.java).reboot(null)
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "restart failed", t)
        }
    }

    /** Needs WRITE_SECURE_SETTINGS. The menu stays open and shows the new state. */
    private fun toggleAirplane(c: Context) {
        val state = menuState ?: return
        val next = !state.airplaneOn
        try {
            Settings.Global.putInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, if (next) 1 else 0)
            c.sendBroadcast(Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED).putExtra("state", next))
            state.airplaneOn = next
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "airplane mode toggle failed", t)
        }
    }

    private fun isAirplaneOn(c: Context): Boolean = try {
        Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    } catch (t: Throwable) {
        Log.w(LOG_TAG, "could not read airplane mode", t)
        false
    }
}
