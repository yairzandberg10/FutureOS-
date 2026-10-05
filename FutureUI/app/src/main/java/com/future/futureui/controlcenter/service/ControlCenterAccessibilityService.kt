package com.future.futureui.controlcenter.service

import android.accessibilityservice.AccessibilityService
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.future.futureui.controlcenter.ui.ControlCenterScreen
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIActions

class ControlCenterAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var isVisible = false
    private var longPressPending = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var controlManager: ControlManager? = null
    private val powerMenuVisible = androidx.compose.runtime.mutableStateOf(false)

    // פתיחה וסגירה מונפשות: הסגירה רק מחליפה את היעד, והחלון מוסר כשהיציאה
    // נגמרת (ר' OverlayMotion). בזמן היציאה החלון לא לוקח מקשים.
    private val motion = com.future.futureui.utils.OverlayMotion()
    private var closing = false
    private var windowParams: WindowManager.LayoutParams? = null
    private val forceRemove = Runnable { removeControlCenterNow() }

    // תמונת הרקע נשמרת לפי מזהה הרקע. קודם היא הומרה ל-Bitmap בכל פתיחה, על
    // ה-main thread, בדיוק בפריים שבו אנימציית הכניסה מתחילה.
    private var cachedWallpaperId = -1
    private var cachedWallpaper: androidx.compose.ui.graphics.ImageBitmap? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FutureUIActions.ACTION_SHOW_CONTROL_CENTER) {
                showControlCenter(intent.getIntExtra(FutureUIActions.EXTRA_FROM_DIRECTION, 0))
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private val longPressRunnable = Runnable {
        Log.d("FutureUI", "Long Press Triggered via Runnable")
        longPressPending = false
        toggleControlCenter()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val filter = IntentFilter(FutureUIActions.ACTION_SHOW_CONTROL_CENTER)
            // נשלח רק מתוך FutureUI עצמו - אפליקציה זרה לא צריכה לפתוח את המסך הזה.
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error in onCreate", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "FutureUI מוכן: לחיצה ארוכה על כוכבית", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // We don't want to auto-hide on window state changes because opening the overlay 
        // itself triggers these events and causes a self-closing loop.
    }
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // מסך הנעילה מוצג - הוא מטפל בכל המקשים
        // גם כשהמכשיר נעול ומסך הנעילה פינה מקום לשיחה/מעורר: לא מתגים (טיסה, רשת...)
        if (com.future.futureui.utils.FutureUIState.isLocked || com.future.futureui.utils.FutureUIState.isSecured) return false
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_STAR) {
            if (action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    longPressPending = true
                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.postDelayed(longPressRunnable, 600)
                }
                // Every repeat DOWN while the long-press timer is pending belongs to this
                // gesture and must be swallowed so it never leaks to the foreground IME.
                return true
            } else if (action == KeyEvent.ACTION_UP) {
                if (longPressPending) {
                    // Timer never fired: this was a short press. Cancel the pending
                    // long-press toggle and hand the press on to whoever is in front.
                    mainHandler.removeCallbacks(longPressRunnable)
                    longPressPending = false
                    // המקש נצרך כאן לגמרי (ה-return true למטה חוסם אותו גם בלחיצה קצרה),
                    // ולכן מקלדת ה-T9 לא ראתה מעולם לחיצה על * ותפריט הפיסוק שלה פשוט לא
                    // נפתח. הלחיצה הקצרה משודרת גלובלית, בדיוק כמו מקש Options (ר'
                    // FutureUIActions), כדי שהאפליקציה שבחזית תוכל להגיב - בלי לגעת בהחזקה הארוכה.
                    try {
                        sendBroadcast(Intent(FutureUIActions.ACTION_STAR_SHORT_PRESS), FutureUIActions.PERMISSION_SYSTEM)
                    } catch (e: Exception) {
                        Log.w("FutureUI", "star short-press broadcast failed", e)
                    }
                }
                return true
            }

            return true
        }

        if (isVisible && !closing) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) {
                    // BACK מתפריט הכיבוי חוזר ל-Control Center במקום לסגור הכל.
                    if (powerMenuVisible.value) powerMenuVisible.value = false else hideControlCenter()
                }
                return true
            }
            // Let other keys pass to the focused overlay window
            return false
        }

        return super.onKeyEvent(event)
    }

    private fun toggleControlCenter() {
        mainHandler.post {
            if (isVisible && !closing) hideControlCenter() else showControlCenter()
        }
    }

    private fun showControlCenter(fromDirection: Int = 0) {
        if (isVisible) {
            // פתיחה מחדש באמצע היציאה - האנימציה מתהפכת מהנקודה שבה היא נמצאת
            if (closing) {
                closing = false
                mainHandler.removeCallbacks(forceRemove)
                setWindowFocusable(true)
                motion.enter(fromDirection)
            }
            return
        }
        // כל דרך פתיחה (מקש, שידור פנימי, מעבר מהפאנל השני) - לא כשהמכשיר נעול
        if (com.future.futureui.utils.FutureUIState.isLocked || com.future.futureui.utils.FutureUIState.isSecured) return
        try {
            if (controlManager == null) {
                controlManager = ControlManager(this)
            }

            val wallpaperImage = loadWallpaper()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
            ).apply {
                flags = flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
                // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - חוסמים מגע גם בחלון
                // הזה. FLAG_NOT_TOUCHABLE לא משפיע על אירועי מקש, רק על מגע.
                flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE

                // Android 12+ Blur Behind
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = 50
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                }
            }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@ControlCenterAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@ControlCenterAccessibilityService)
                setViewTreeViewModelStoreOwner(this@ControlCenterAccessibilityService)
                
                setContent {
                    FutureUITheme {
                        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
                            ControlCenterScreen(
                                modifier = Modifier.fillMaxSize(),
                                isVisibleByDefault = true,
                                wallpaper = wallpaperImage,
                                controlManager = controlManager,
                                // תפריט כיבוי אחד לכל המערכת - של שירות שורת המצב (גם החזקת
                                // מקש ההפעלה פותחת אותו). מרכז הבקרה נסגר קודם.
                                onPowerClick = {
                                    hideControlCenter()
                                    sendBroadcast(
                                        Intent(com.future.futureui.statusbar.service.StatusBarAccessibilityService.ACTION_SHOW_POWER_MENU)
                                            .setPackage(packageName)
                                    )
                                },
                                onSettingsClick = {
                                    controlManager?.openMainSettings()
                                    hideControlCenter()
                                },
                                onSwitchToNotificationCenter = {
                                    // שתי השכבות באותו עומק: מרכז הבקרה יוצא שמאלה ומרכז
                                    // ההתראות נכנס מימין, באותה תנועה. קודם CC נסגר אחרי
                                    // postDelayed(50) בלי אנימציה, וזה יצר הבזק.
                                    hideControlCenter(toDirection = -1)
                                    val intent = Intent(FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER)
                                    intent.setPackage(packageName)
                                    intent.putExtra(FutureUIActions.EXTRA_FROM_DIRECTION, 1)
                                    sendBroadcast(intent)
                                },
                                onRequestClose = { hideControlCenter() },
                                motion = motion,
                                onExitFinished = { removeControlCenterNow() }
                            )

                        }
                    }
                }
            }

            if (lifecycleRegistry.currentState == Lifecycle.State.CREATED) {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            }
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            
            motion.enter(fromDirection)
            closing = false
            windowParams = params
            windowManager.addView(composeView, params)
            isVisible = true

            val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            bringFrontIntent.setPackage(packageName)
            sendBroadcast(bringFrontIntent)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error showing overlay", e)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) return drawable.bitmap
        
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun loadWallpaper(): androidx.compose.ui.graphics.ImageBitmap? {
        val wm = WallpaperManager.getInstance(this)
        val id = runCatching { wm.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)
        if (id != -1 && id == cachedWallpaperId && cachedWallpaper != null) return cachedWallpaper
        val image = runCatching { wm.drawable?.let { drawableToBitmap(it).asImageBitmap() } }.getOrNull()
        cachedWallpaperId = id
        cachedWallpaper = image
        return image
    }

    private fun setWindowFocusable(focusable: Boolean) {
        val view = composeView ?: return
        val params = windowParams ?: return
        params.flags = if (focusable) params.flags and WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE.inv()
        else params.flags or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        runCatching { windowManager.updateViewLayout(view, params) }
    }

    /** מתחיל את היציאה; החלון מוסר כשהיא נגמרת ([removeControlCenterNow]). */
    private fun hideControlCenter(toDirection: Int = 0) {
        if (!isVisible || closing) return
        closing = true
        powerMenuVisible.value = false
        // המקשים חוזרים לאפליקציה שמתחת מיד, לא רק אחרי שהאנימציה נגמרת
        setWindowFocusable(false)
        motion.exit(toDirection)
        // רשת ביטחון: אם המסך לא דיווח על סוף היציאה (למשל לא צויר), מסירים בכל זאת
        mainHandler.removeCallbacks(forceRemove)
        mainHandler.postDelayed(forceRemove, 900)
    }

    private fun removeControlCenterNow() {
        mainHandler.removeCallbacks(forceRemove)
        if (!isVisible) return
        try {
            powerMenuVisible.value = false
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            windowManager.removeView(composeView)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error hiding overlay", e)
        }
        composeView = null
        windowParams = null
        isVisible = false
        closing = false
    }

    override fun onDestroy() {
        removeControlCenterNow()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {
            android.util.Log.w("ControlCenterAccessibil", "onDestroy failed", e)
        }
        controlManager?.dispose()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
