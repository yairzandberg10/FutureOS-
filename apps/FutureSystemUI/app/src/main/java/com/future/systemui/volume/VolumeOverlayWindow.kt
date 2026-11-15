package com.future.systemui.volume

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.future.sharednav.theme.FutureAppTheme

/**
 * חלון ההצגה של הווליום. נוצר בהצגה הראשונה ונשאר מחובר ל-WindowManager;
 * הנראות נשלטת ע"י [MutableTransitionState] כך שהכניסה והיציאה מונפשות.
 * הכול רץ על ה-main thread (מקבל השידורים רץ שם).
 */
class VolumeOverlayWindow(private val context: Context) {

    private val handler = Handler(Looper.getMainLooper())
    private val visibleState = MutableTransitionState(false)
    private var streamLabel by mutableStateOf("")
    private var level by mutableFloatStateOf(0f)

    private var view: ComposeView? = null
    private var owner: OverlayOwner? = null

    private val hideRunnable = Runnable { hide() }
    private val removeRunnable = Runnable { removeView() }

    /** מציג את הערוץ והעוצמה (0..1) ומאפס את טיימר ההסתרה. */
    fun show(label: String, newLevel: Float) {
        try {
            streamLabel = label
            level = newLevel
            handler.removeCallbacks(removeRunnable)
            if (view == null) attach()
            visibleState.targetState = true
            handler.removeCallbacks(hideRunnable)
            handler.postDelayed(hideRunnable, AUTO_HIDE_MS)
        } catch (t: Throwable) {
            Log.e(TAG, "volume overlay show failed", t)
            view = null
        }
    }

    /** מתחיל את היציאה; החלון מוסר אחריה. */
    private fun hide() {
        if (view == null) return
        visibleState.targetState = false
        handler.removeCallbacks(removeRunnable)
        handler.postDelayed(removeRunnable, EXIT_MS)
    }

    private fun attach() {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val lifecycleOwner = OverlayOwner()
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(lifecycleOwner)
            setContent {
                FutureAppTheme {
                    VolumeOverlayContent(
                        streamLabel = streamLabel,
                        level = level,
                        visibleState = visibleState,
                    )
                }
            }
        }
        lifecycleOwner.start()
        try {
            wm.addView(composeView, layoutParams(TYPE_VOLUME_OVERLAY))
        } catch (t: Throwable) {
            Log.w(TAG, "TYPE_VOLUME_OVERLAY refused, falling back to TYPE_APPLICATION_OVERLAY", t)
            wm.addView(composeView, layoutParams(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY))
        }
        view = composeView
        owner = lifecycleOwner
    }

    private fun removeView() {
        val v = view ?: return
        try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            wm.removeViewImmediate(v)
        } catch (t: Throwable) {
            Log.e(TAG, "volume overlay remove failed", t)
        } finally {
            view = null
            owner?.destroy()
            owner = null
        }
    }

    private fun layoutParams(type: Int): WindowManager.LayoutParams =
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
        }

    companion object {
        private const val TAG = "FutureSystemUI"

        /** TYPE_VOLUME_OVERLAY - קבוע מוסתר ב-SDK הציבורי, לכן מוגדר כאן. */
        private const val TYPE_VOLUME_OVERLAY = 2020

        private const val AUTO_HIDE_MS = 2000L

        /** ארוך מעט מאנימציית היציאה (ר' VolumeOverlayContent), כדי שלא נסיר את החלון באמצע. */
        private const val EXIT_MS = 300L
    }
}

/**
 * בעלי מחזור החיים שנדרשים ל-ComposeView מחוץ לActivity. כל הצגה מחדש של החלון
 * יוצרת בעלים חדש, כי מחזור החיים של Lifecycle לא חוזר מ-DESTROYED.
 */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateController.performRestore(null)
    }

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry

    override val viewModelStore: ViewModelStore
        get() = store

    fun start() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}
