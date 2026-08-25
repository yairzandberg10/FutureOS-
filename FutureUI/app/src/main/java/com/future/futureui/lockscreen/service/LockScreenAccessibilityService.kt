package com.future.futureui.lockscreen.service

import android.accessibilityservice.AccessibilityService
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.future.futureui.lockscreen.ui.LockScreenScreen
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIActions

class LockScreenAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var isVisible = false
    private val mainHandler = Handler(Looper.getMainLooper())

    // מוגדר ע"י שידורי ACTION_CALL_RINGING/ACTION_CALL_ENDED מ-CallService של dialer -
    // ראו FutureUIActions.ACTION_CALL_RINGING לפירוט. כל עוד שיחה מצלצלת, מסך הנעילה
    // לא אמור להופיע (או נדרש להיעלם אם כבר גלוי) כדי שמסך השיחה יהיה נגיש.
    @Volatile
    private var suppressForActiveCall = false

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    // Prepare or show lock screen
                }
                Intent.ACTION_SCREEN_ON -> {
                    if (!suppressForActiveCall) showLockScreen()
                }
                FutureUIActions.ACTION_CALL_RINGING -> {
                    suppressForActiveCall = true
                    if (isVisible) hideLockScreen()
                }
                FutureUIActions.ACTION_CALL_ENDED -> {
                    suppressForActiveCall = false
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(FutureUIActions.ACTION_CALL_RINGING)
            addAction(FutureUIActions.ACTION_CALL_ENDED)
        }
        // ACTION_CALL_RINGING/ENDED מגיעים משידור מפורש (setPackage) של אפליקציה אחרת
        // (dialer) - מ-API 33 חובה להצהיר EXPORTED/NOT_EXPORTED במפורש כדי לקבל שידור
        // כזה; ה-overload הזה לא קיים על מכשירים ישנים יותר (מכשיר היעד כאן הוא API 31).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenReceiver, filter)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // For testing, show it immediately or on a key
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (isVisible) {
            // Forward keys to the compose view
            return false 
        }
        
        // Example trigger: double press some key to lock for testing
        return super.onKeyEvent(event)
    }

    private fun showLockScreen() {
        if (isVisible) return
        mainHandler.post {
            try {
                val wallpaperManager = WallpaperManager.getInstance(this)
                val wallpaperDrawable = wallpaperManager.drawable
                val wallpaperBitmap = wallpaperDrawable?.let { drawableToBitmap(it) }

                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = android.view.Gravity.CENTER
                    softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                }

                composeView = ComposeView(this).apply {
                    setViewTreeLifecycleOwner(this@LockScreenAccessibilityService)
                    setViewTreeSavedStateRegistryOwner(this@LockScreenAccessibilityService)
                    setViewTreeViewModelStoreOwner(this@LockScreenAccessibilityService)
                    
                    setContent {
                        FutureUITheme {
                            LockScreenScreen(
                                modifier = Modifier.fillMaxSize(),
                                wallpaper = wallpaperBitmap?.asImageBitmap(),
                                onUnlock = { hideLockScreen() }
                            )
                        }
                    }
                }

                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
                
                windowManager.addView(composeView, params)
                isVisible = true
                com.future.futureui.utils.FutureUIState.isLockScreenVisible = true

                val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
                bringFrontIntent.setPackage(packageName)
                sendBroadcast(bringFrontIntent)
            } catch (e: Exception) {
                Log.e("LockScreen", "Error showing lock screen", e)
            }
        }
    }

    private fun hideLockScreen() {
        if (!isVisible) return
        mainHandler.post {
            try {
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                windowManager.removeView(composeView)
                composeView = null
                isVisible = false
                com.future.futureui.utils.FutureUIState.isLockScreenVisible = false
            } catch (e: Exception) {
                Log.e("LockScreen", "Error hiding lock screen", e)
            }
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

    override fun onDestroy() {
        hideLockScreen()
        unregisterReceiver(screenReceiver)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
