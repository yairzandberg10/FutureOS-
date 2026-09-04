package com.future.futureui.notificationcenter.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.notificationcenter.ui.HeadsUpNotificationScreen
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIActions

/**
 * שירות קל-משקל שמציג באנר קופץ (heads-up) חד-פעמי כשמגיעה התראה חדשה, בלי
 * להתערב בקלט מקשים של האפליקציה שבחזית (המכשיר ללא מסך מגע, ואין סיבה
 * לגזול פוקוס מקלדת עבור באנר חולף) - ראו הערה בקובץ ה-UI המשויך.
 */
class HeadsUpNotificationService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val EXTRA_KEY = "notification_key"

        fun show(context: Context, notificationKey: String) {
            val intent = Intent(context, HeadsUpNotificationService::class.java)
            intent.putExtra(EXTRA_KEY, notificationKey)
            context.startService(intent)
        }
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val key = intent?.getStringExtra(EXTRA_KEY)
        val sbn = key?.let { k -> MediaControlService.instance?.activeNotifications?.firstOrNull { it.key == k } }
        if (sbn == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        showBanner(sbn, startId)
        return START_NOT_STICKY
    }

    private fun showBanner(sbn: android.service.notification.StatusBarNotification, startId: Int) {
        try {
            removeBanner()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = android.view.Gravity.TOP
            }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@HeadsUpNotificationService)
                setViewTreeSavedStateRegistryOwner(this@HeadsUpNotificationService)
                setViewTreeViewModelStoreOwner(this@HeadsUpNotificationService)
                setContent {
                    FutureUITheme {
                        // התראת שיחה נכנסת (CATEGORY_CALL) נשארת על המסך הרבה יותר זמן מבאנר
                        // רגיל - שיחה ממשיכה לצלצל עשרות שניות, ובאנר שנעלם אחרי 4.5 שניות
                        // בזמן שהיא עדיין מצלצלת נראה כאילו השיחה נגמרה.
                        val autoDismissMillis = if (sbn.notification.category == android.app.Notification.CATEGORY_CALL) 30000L else 4500L
                        HeadsUpNotificationScreen(
                            sbn = sbn,
                            autoDismissMillis = autoDismissMillis,
                            onDismissed = {
                                removeBanner()
                                stopSelf(startId)
                            }
                        )
                    }
                }
            }

            if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            windowManager.addView(composeView, params)

            val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            bringFrontIntent.setPackage(packageName)
            sendBroadcast(bringFrontIntent)
        } catch (e: Exception) {
            Log.e("HeadsUpNotification", "Error showing banner", e)
            stopSelf(startId)
        }
    }

    private fun removeBanner() {
        val view = composeView ?: return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {}
        composeView = null
    }

    override fun onDestroy() {
        removeBanner()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
