package com.future.futureui.notificationcenter.service

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
import com.future.futureui.notificationcenter.ui.NotificationCenterScreen
import com.future.futureui.notificationcenter.logic.NotificationCenterManager
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIActions

class NotificationCenterAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var isVisible = false
    private var longPressPending = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var notificationManager: NotificationCenterManager? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER) {
                showNotificationCenter()
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
        Log.d("FutureUI", "NC Long Press Triggered")
        longPressPending = false
        toggleNotificationCenter()
    }

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            
            val filter = IntentFilter(FutureUIActions.ACTION_SHOW_NOTIFICATION_CENTER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(receiver, filter)
            }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error in NC onCreate", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Toast.makeText(this, "Notification Center מוכן: לחיצה ארוכה על #", Toast.LENGTH_SHORT).show()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action

        if (keyCode == KeyEvent.KEYCODE_POUND) {
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
                    mainHandler.removeCallbacks(longPressRunnable)
                    longPressPending = false
                }
                return true
            }
            return true
        }

        if (isVisible) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (action == KeyEvent.ACTION_UP) hideNotificationCenter()
                return true
            }
            return false
        }

        return super.onKeyEvent(event)
    }

    private fun toggleNotificationCenter() {
        mainHandler.post {
            if (isVisible) hideNotificationCenter() else showNotificationCenter()
        }
    }

    private fun showNotificationCenter() {
        if (isVisible) return
        try {
            if (notificationManager == null) {
                notificationManager = NotificationCenterManager(this)
            }
            notificationManager?.controlManager?.startRootShell()

            val wallpaperManager = WallpaperManager.getInstance(this)
            val wallpaperDrawable = wallpaperManager.drawable
            val wallpaperBitmap = wallpaperDrawable?.let { drawableToBitmap(it) }

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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    blurBehindRadius = 50
                    flags = flags or WindowManager.LayoutParams.FLAG_BLUR_BEHIND
                }
            }

            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@NotificationCenterAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@NotificationCenterAccessibilityService)
                setViewTreeViewModelStoreOwner(this@NotificationCenterAccessibilityService)
                
                setContent {
                    FutureUITheme {
                        NotificationCenterScreen(
                            modifier = Modifier.fillMaxSize(),
                            wallpaper = wallpaperBitmap?.asImageBitmap(),
                            manager = notificationManager,
                            onSwitchToControlCenter = {
                                val intent = Intent(FutureUIActions.ACTION_SHOW_CONTROL_CENTER)
                                intent.setPackage(packageName)
                                sendBroadcast(intent)
                                mainHandler.postDelayed({ hideNotificationCenter() }, 50)
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
            isVisible = true

            val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            bringFrontIntent.setPackage(packageName)
            sendBroadcast(bringFrontIntent)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error showing NC overlay", e)
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

    private fun hideNotificationCenter() {
        if (!isVisible) return
        try {
            notificationManager?.controlManager?.stopRootShell()
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            windowManager.removeView(composeView)
            composeView = null
            isVisible = false
        } catch (e: Exception) {
            Log.e("FutureUI", "Error hiding NC overlay", e)
        }
    }

    override fun onDestroy() {
        hideNotificationCenter()
        try {
            unregisterReceiver(receiver)
        } catch (e: Exception) {}
        notificationManager?.dispose()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
