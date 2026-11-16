package com.future.systemui

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.ServiceManager
import android.util.Log
import com.android.internal.statusbar.IStatusBarService

/**
 * Started by system_server at boot (config_systemUIServiceComponent -> com.future.systemui/.SystemUIService).
 * Registers [CommandQueue] as THE status bar with StatusBarManagerService and starts every feature.
 * Each feature is isolated: one feature crashing on start must not take the others down.
 */
class SystemUIService : Service() {
    override fun onCreate() {
        super.onCreate()
        val app = application as SystemUIApp
        if (app.started) return
        app.started = true
        try {
            val bar = IStatusBarService.Stub.asInterface(ServiceManager.getService("statusbar"))
            // The result holds the state the framework already has (icons, disable flags,
            // appearance); StatusBarFeature applies it when it starts.
            app.registerResult = bar.registerStatusBar(app.commandQueue)
            Log.i(TAG, "registered as the status bar")
        } catch (t: Throwable) {
            Log.e(TAG, "registerStatusBar failed", t)
        }
        for (f in app.features) {
            try { f.start(this, app.commandQueue) } catch (t: Throwable) { Log.e(TAG, "feature ${f.javaClass.simpleName} failed", t) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    companion object { const val TAG = "FutureSystemUI" }
}
