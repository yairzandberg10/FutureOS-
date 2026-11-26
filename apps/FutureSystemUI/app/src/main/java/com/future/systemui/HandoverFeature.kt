package com.future.systemui

import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * FutureSystemUI now draws the status bar, shade, control center and lock screen itself, using the same
 * UI code as FutureUI. FutureUI's accessibility-overlay versions of those screens would draw a second copy
 * on top, so while FutureSystemUI runs as the SystemUI they are switched off. FutureUI itself stays
 * installed: its theme, settings and lock settings providers are still used.
 */
class HandoverFeature : Feature {
    override fun start(context: Context, commandQueue: CommandQueue) {
        try {
            val cr = context.contentResolver
            val key = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            val current = Settings.Secure.getString(cr, key).orEmpty()
            val before = current.split(':').filter { it.isNotBlank() }
            // FutureUI's overlays off; our key-only filter on (long-press keys never reach a SystemUI otherwise)
            val kept = before.filter { !it.startsWith("$FUTURE_UI/") && it != KEY_SERVICE } + KEY_SERVICE
            if (kept != before) {
                Settings.Secure.putString(cr, key, kept.joinToString(":"))
                Settings.Secure.putInt(cr, Settings.Secure.ACCESSIBILITY_ENABLED, 1)
                Log.i(SystemUIService.TAG, "FutureUI overlay services switched off, FutureSystemUI key filter on")
            }
        } catch (t: Throwable) {
            Log.e(SystemUIService.TAG, "handover failed", t)
        }
    }

    companion object {
        const val FUTURE_UI = "com.future.futureui"
        const val KEY_SERVICE = "com.future.systemui/com.future.systemui.controlcenter.ControlCenterKeyService"
    }
}
