package com.future.futureui.notificationcenter.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.service.notification.StatusBarNotification
import android.util.Log
import android.view.WindowManager
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
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
import com.future.futureui.utils.FrostedBackdrop
import com.future.futureui.utils.FutureUIActions

/**
 * שירות קל-משקל שמציג באנר קופץ (heads-up) כשמגיעה התראה חדשה, בלי
 * להתערב בקלט מקשים של האפליקציה שבחזית (המכשיר ללא מסך מגע, ואין סיבה
 * לגזול פוקוס מקלדת עבור באנר חולף) - ראו הערה בקובץ ה-UI המשויך.
 *
 * באנר אחד בכל רגע: התראה שמגיעה בזמן שבאנר מוצג מחליפה רק את התוכן שלו
 * ([generation]) - קודם הבאנר נמחק ונבנה מחדש, וזה נראה כמו חיתוך חד.
 *
 * לפני שהבאנר מוצג מצלמים את רצועת המסך העליונה בשביל רקע הזכוכית
 * (FrostedBackdrop) - חייבים לצלם לפני שהחלון שלנו מכסה אותה. אם הצילום
 * לא חוזר תוך [FrostWaitMillis] הבאנר מוצג בלי זכוכית, כדי שהתראה לא תתעכב.
 */
class HeadsUpNotificationService : Service(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    companion object {
        private const val EXTRA_KEY = "notification_key"

        /** כמה מחכים לצילום הזכוכית לפני שמציגים בלעדיה. */
        private const val FrostWaitMillis = 180L

        /** גובה הרצועה שמצולמת - שורת המצב, הריווח והבאנר עצמו. */
        private const val FrostRegionDp = 170

        fun show(context: Context, notificationKey: String) {
            val intent = Intent(context, HeadsUpNotificationService::class.java)
            intent.putExtra(EXTRA_KEY, notificationKey)
            context.startService(intent)
        }

        private const val EXTRA_DISMISS_KEY = "dismiss_key"

        /** המפתח של הבאנר שמוצג כרגע - כדי לא להעיר את השירות על כל התראה שנמחקת. */
        @Volatile
        private var shownKey: String? = null

        fun dismiss(context: Context, notificationKey: String) {
            if (shownKey != notificationKey) return
            runCatching {
                context.startService(Intent(context, HeadsUpNotificationService::class.java).putExtra(EXTRA_DISMISS_KEY, notificationKey))
            }
        }
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val current = mutableStateOf<StatusBarNotification?>(null)
    private val generation = mutableIntStateOf(0)
    private val frost = mutableStateOf<FrostedBackdrop.Frost?>(null)
    private var pendingShow: Runnable? = null
    private var latestStartId = 0

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
        val dismissKey = intent?.getStringExtra(EXTRA_DISMISS_KEY)
        if (dismissKey != null) {
            if (dismissKey == shownKey) removeBanner()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        latestStartId = startId
        val key = intent?.getStringExtra(EXTRA_KEY)
        val sbn = key?.let { k -> MediaControlService.instance?.activeNotifications?.firstOrNull { it.key == k } }
        if (sbn == null) {
            if (composeView == null && pendingShow == null) stopSelf(startId)
            return START_NOT_STICKY
        }

        current.value = sbn
        when {
            // באנר כבר מוצג: רק התוכן מתחלף והטיימר מתאפס
            composeView != null -> {
                generation.intValue++
                shownKey = sbn.key
            }
            // הצילום עוד בדרך - הבאנר יוצג עם ההתראה העדכנית
            pendingShow != null -> Unit
            else -> {
                generation.intValue = 0
                frost.value = null
                var shown = false
                val showNow = Runnable {
                    if (!shown) {
                        shown = true
                        pendingShow = null
                        showBanner()
                    }
                }
                pendingShow = showNow
                mainHandler.postDelayed(showNow, FrostWaitMillis)
                val regionPx = (FrostRegionDp * resources.displayMetrics.density).toInt()
                FrostedBackdrop.capture(regionPx) { result ->
                    if (!shown) {
                        frost.value = result
                        mainHandler.removeCallbacks(showNow)
                        showNow.run()
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun showBanner() {
        try {
            removeBanner()
            // המכשיר ננעל בין פרסום ההתראה להצגה - לא מציגים תוכן מעל מסך הנעילה
            // (שיחה נכנסת היא החריג: היא חייבת להופיע)
            val st = com.future.futureui.utils.FutureUIState
            val sbn = current.value ?: return
            if ((st.isLocked || st.isSecured) && sbn.notification.category != android.app.Notification.CATEGORY_CALL) {
                stopSelf(latestStartId)
                return
            }

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
                        val sbn = current.value ?: return@FutureUITheme
                        // התראת שיחה נכנסת (CATEGORY_CALL) נשארת על המסך הרבה יותר זמן מבאנר
                        // רגיל - שיחה ממשיכה לצלצל עשרות שניות, ובאנר שנעלם אחרי 4.5 שניות
                        // בזמן שהיא עדיין מצלצלת נראה כאילו השיחה נגמרה.
                        val autoDismissMillis = if (sbn.notification.category == android.app.Notification.CATEGORY_CALL) 30000L else 4500L
                        HeadsUpNotificationScreen(
                            sbn = sbn,
                            generation = generation.intValue,
                            frost = frost.value,
                            autoDismissMillis = autoDismissMillis,
                            onDismissed = {
                                removeBanner()
                                stopSelf(latestStartId)
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
            shownKey = current.value?.key

            val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            bringFrontIntent.setPackage(packageName)
            sendBroadcast(bringFrontIntent)
        } catch (e: Exception) {
            Log.e("HeadsUpNotification", "Error showing banner", e)
            stopSelf(latestStartId)
        }
    }

    private fun removeBanner() {
        shownKey = null
        val view = composeView ?: return
        try {
            windowManager.removeView(view)
        } catch (e: Exception) {
            android.util.Log.w("HeadsUpNotificationServ", "removeBanner failed", e)
        }
        composeView = null
    }

    override fun onDestroy() {
        pendingShow?.let(mainHandler::removeCallbacks)
        pendingShow = null
        removeBanner()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
