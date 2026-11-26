package com.future.systemui.controlcenter

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.future.sharednav.actions.FutureUIActions
import com.future.systemui.fui.utils.FutureUIState

private const val TAG = "FutureSystemUI"

/**
 * OPTIONAL key source. The framework never hands the * key or a long press on Menu to SystemUI
 * (PhoneWindowManager only forwards system keys such as app-switch/power to the status bar), so the
 * only way to keep FutureUI's gestures without patching the ROM is an accessibility key filter:
 *   long press * -> control center   (short press -> ACTION_STAR_SHORT_PRESS broadcast, for the T9 keyboard)
 *   long press Menu/Settings -> recent apps   (short press -> ACTION_OPTIONS_SHORT_PRESS broadcast)
 * Does nothing else. See the report for the manifest entry and the accessibility config XML.
 */
class ControlCenterKeyService : AccessibilityService() {
    private val main = Handler(Looper.getMainLooper())
    private var starPending = false
    private var menuTriggered = false

    private val starLong = Runnable {
        starPending = false
        ControlCenterFeature.instance?.toggle()
    }
    private val menuLong = Runnable {
        menuTriggered = true
        RecentsFeature.instance?.toggle()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        try {
            when (event.keyCode) {
                KeyEvent.KEYCODE_STAR -> {
                    // Locked: the lock screen handles every key.
                    if (FutureUIState.isLocked || FutureUIState.isSecured) return false
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            starPending = true
                            main.removeCallbacks(starLong)
                            main.postDelayed(starLong, 600)
                        }
                    } else if (event.action == KeyEvent.ACTION_UP && starPending) {
                        main.removeCallbacks(starLong)
                        starPending = false
                        broadcast(FutureUIActions.ACTION_STAR_SHORT_PRESS)
                    }
                    return true
                }
                KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_SETTINGS -> {
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (event.repeatCount == 0) {
                            menuTriggered = false
                            main.removeCallbacks(menuLong)
                            main.postDelayed(menuLong, 600)
                        }
                    } else if (event.action == KeyEvent.ACTION_UP) {
                        main.removeCallbacks(menuLong)
                        if (!menuTriggered) broadcast(FutureUIActions.ACTION_OPTIONS_SHORT_PRESS)
                    }
                    return true
                }
            }
        } catch (t: Throwable) {
            Log.e(TAG, "key service failed", t)
        }
        return false
    }

    private fun broadcast(action: String) {
        try {
            sendBroadcast(Intent(action), FutureUIActions.PERMISSION_SYSTEM)
        } catch (t: Throwable) {
            Log.w(TAG, "broadcast $action failed", t)
        }
    }

    override fun onDestroy() {
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
