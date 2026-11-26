package com.future.systemui.power

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.future.sharednav.i18n.tr
import com.future.sharednav.root.RootShell
import com.future.sharednav.theme.FutureTypography
import com.future.systemui.CommandQueue
import com.future.systemui.Feature
import com.future.systemui.controlcenter.SYSTEM_DIALOG_TYPES
import com.future.systemui.fui.controlcenter.logic.ControlManager
import com.future.systemui.fui.controlcenter.ui.PowerMenuScreen
import com.future.systemui.fui.ui.theme.FutureUITheme
import com.future.systemui.fui.utils.FutureUIState

/**
 * Power menu (global actions) and shutdown screen for FutureSystemUI.
 *
 * The menu is FutureUI's PowerMenuScreen (focus starts on "ביטול", power off / restart ask for a
 * confirmation that also starts on "ביטול"), in a focusable full-screen window driven by the keypad.
 */
class PowerMenuFeature : Feature, CommandQueue.Callbacks {
    private var ctx: Context? = null
    private val main = Handler(Looper.getMainLooper())
    private var menuOverlay: ComposeOverlay? = null
    private var shutdownOverlay: ComposeOverlay? = null
    private var controlManager: ControlManager? = null

    private val airplaneState = mutableStateOf(false)
    private val silentState = mutableStateOf(false)

    override fun start(context: Context, commandQueue: CommandQueue) {
        ctx = context
        instance = this
        commandQueue.addCallback(this)
    }

    override fun showGlobalActionsMenu() {
        main.post { showMenu() }
    }

    private fun showMenu() {
        val c = ctx ?: return
        if (menuOverlay != null) return
        try {
            val audio = c.getSystemService(AudioManager::class.java)
            airplaneState.value = isAirplaneOn(c)
            silentState.value = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT
            val cm = ControlManager(c).also { controlManager = it }

            val overlay = ComposeOverlay(c, fullScreen = true, onKey = { onMenuKey(it) })
            overlay.setContent {
                FutureUITheme {
                    PowerMenuScreen(
                        onPowerOff = {
                            hideMenu()
                            powerOff(c)
                        },
                        onRestart = {
                            hideMenu()
                            restart(c)
                        },
                        onCancel = { hideMenu() },
                        airplaneOn = airplaneState.value,
                        onAirplane = onAirplane@{
                            // Airplane mode on a locked device lets a thief cut the phone off the network.
                            if (FutureUIState.isSecured) return@onAirplane
                            val next = !airplaneState.value
                            airplaneState.value = next
                            cm.isAirplaneOn = next
                            cm.toggleAirplane()
                        },
                        silentOn = silentState.value,
                        onSilent = {
                            val next = !silentState.value
                            val mode = if (next) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                            if (runCatching { audio?.ringerMode = mode }.isSuccess) silentState.value = next
                        },
                        onScreenshot = {
                            hideMenu()
                            // After the menu left the screen, otherwise it ends up in the picture.
                            main.postDelayed({ takeScreenshot() }, 600)
                        },
                    )
                }
            }
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = overlay.show(wm, SYSTEM_DIALOG_TYPES) { type ->
                // Focusable on purpose: the window must receive the D-pad and BACK keys.
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT,
                ).apply { title = "FuturePowerMenu" }
            }
            if (shown) {
                menuOverlay = overlay
                FutureUIState.powerMenuOpen.value = true
            } else {
                controlManager?.dispose()
                controlManager = null
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "showGlobalActionsMenu failed", t)
            menuOverlay = null
        }
    }

    override fun showShutdownUi(isReboot: Boolean, reason: String?) {
        main.post { showShutdown(isReboot) }
    }

    private fun showShutdown(isReboot: Boolean) {
        val c = ctx ?: return
        try {
            hideMenu()
            if (shutdownOverlay != null) return
            val text = if (isReboot) tr("מפעיל מחדש...") else tr("מכבה...")
            val overlay = ComposeOverlay(c, fullScreen = true, onKey = { true })
            overlay.setContent {
                FutureUITheme { ShutdownContent(text) }
            }
            val wm = c.getSystemService(WindowManager::class.java)
            val shown = overlay.show(wm, SYSTEM_DIALOG_TYPES) { type ->
                WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    type,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.OPAQUE,
                ).apply { title = "FutureShutdown" }
            }
            if (shown) shutdownOverlay = overlay
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "showShutdownUi failed", t)
        }
    }

    /** BACK closes the whole menu (as FutureUI did); everything else goes to the screen's focus handling. */
    private fun onMenuKey(event: KeyEvent): Boolean {
        try {
            if (event.keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) main.post { hideMenu() }
                return true
            }
        } catch (t: Throwable) {
            Log.e(LOG_TAG, "power menu key failed", t)
        }
        return false
    }

    private fun hideMenu() {
        val overlay = menuOverlay ?: return
        menuOverlay = null
        try {
            overlay.dismiss()
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "dismissing the power menu failed", t)
        }
        controlManager?.dispose()
        controlManager = null
        FutureUIState.powerMenuOpen.value = false
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

    /** screencap straight from this (system) process; root only as a fallback. */
    private fun takeScreenshot() {
        Thread {
            val cmd = "d=/sdcard/Pictures/Screenshots; mkdir -p \$d; " +
                "f=\$d/Screenshot_\$(date +%Y%m%d-%H%M%S).png; screencap -p \$f && " +
                "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://\$f"
            try {
                val code = ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start().waitFor()
                if (code != 0) RootShell.run(cmd)
            } catch (t: Throwable) {
                Log.w(LOG_TAG, "screenshot failed", t)
                runCatching { RootShell.run(cmd) }
            }
        }.start()
    }

    private fun isAirplaneOn(c: Context): Boolean = try {
        Settings.Global.getInt(c.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    } catch (t: Throwable) {
        Log.w(LOG_TAG, "could not read airplane mode", t)
        false
    }

    companion object {
        @Volatile
        var instance: PowerMenuFeature? = null
            private set
    }
}

/** Full-screen black screen shown while the device powers off or restarts, in the FutureUI type scale. */
@Composable
internal fun ShutdownContent(text: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = FutureTypography.title,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
        )
    }
}
