package com.future.systemui.power

import android.content.Context
import android.util.Log
import android.widget.FrameLayout
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.Composable
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

internal const val LOG_TAG = "FutureSystemUI"

/**
 * Lifecycle, saved-state and view-model owner for a Compose view that lives in a
 * WindowManager window (there is no Activity in SystemUI).
 */
internal class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    init {
        savedStateController.performRestore(null)
    }

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun start() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun stop() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

/** Root view that sees every key event before its children, so the overlay can drive the D-pad itself. */
private class KeyRootLayout(
    context: Context,
    private val onKey: (KeyEvent) -> Boolean,
) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (onKey(event)) return true
        return super.dispatchKeyEvent(event)
    }
}

/**
 * A Compose UI hosted directly in a WindowManager window. Create a new instance for every
 * show: the lifecycle is terminal once it is destroyed.
 *
 * @param fullScreen true for a window that covers the screen, false for a wrap-content window (toast).
 * @param onKey receives every key event of the window; return true to consume it.
 */
internal class ComposeOverlay(
    context: Context,
    fullScreen: Boolean,
    onKey: (KeyEvent) -> Boolean,
) {
    private val owner = OverlayOwner()
    private val root = KeyRootLayout(context, onKey)
    private val compose = ComposeView(context)
    private var windowManager: WindowManager? = null
    /** The params the window was added with (the type that worked). */
    var lastParams: WindowManager.LayoutParams? = null
        private set

    init {
        val size = if (fullScreen) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        root.addView(compose, FrameLayout.LayoutParams(size, size))
        root.setViewTreeLifecycleOwner(owner)
        root.setViewTreeViewModelStoreOwner(owner)
        root.setViewTreeSavedStateRegistryOwner(owner)
    }

    fun setContent(content: @Composable () -> Unit) {
        compose.setContent(content)
    }

    /**
     * Adds the window using the first type in [types] that the WindowManager accepts.
     * [params] builds the layout params for a given window type. Returns false if no type worked.
     */
    fun show(wm: WindowManager, types: IntArray, params: (Int) -> WindowManager.LayoutParams): Boolean {
        for (type in types) {
            try {
                val lp = params(type)
                wm.addView(root, lp)
                lastParams = lp
                windowManager = wm
                owner.start()
                return true
            } catch (t: Throwable) {
                Log.w(LOG_TAG, "addView failed for window type $type", t)
            }
        }
        Log.e(LOG_TAG, "could not add overlay window")
        return false
    }

    /** Updates the layout params of the live window (for example to toggle focusability). */
    fun updateLayout(params: WindowManager.LayoutParams) {
        val wm = windowManager ?: return
        try {
            wm.updateViewLayout(root, params)
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "updateViewLayout failed", t)
        }
    }

    fun dismiss() {
        val wm = windowManager ?: return
        windowManager = null
        try {
            wm.removeView(root)
        } catch (t: Throwable) {
            Log.w(LOG_TAG, "removeView failed", t)
        }
        owner.stop()
    }
}
