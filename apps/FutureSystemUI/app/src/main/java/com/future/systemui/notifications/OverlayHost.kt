package com.future.systemui.notifications

import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.widget.FrameLayout
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

/**
 * Lifecycle / saved-state / view-model owners that a ComposeView needs when it is added straight to
 * WindowManager (there is no Activity or Service here). One instance per window; call [destroy] after removing the view.
 */
class OverlayHost : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    init {
        savedStateController.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun attachTo(view: View) {
        view.setViewTreeLifecycleOwner(this)
        view.setViewTreeSavedStateRegistryOwner(this)
        view.setViewTreeViewModelStoreOwner(this)
    }

    fun destroy() {
        try {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        } catch (_: Throwable) {
        }
    }
}

/**
 * Root of an overlay window. FutureUI's screens ran in an accessibility overlay, which gets no system-bar insets,
 * and they add their own status-bar offset (StatusBarLayoutManager.HEIGHT_DP). A real SystemUI window does get
 * insets, so they are consumed here to keep the layout identical to FutureUI.
 * [onBack] (shade only): BACK never reaches Compose, so it is turned into a close request here.
 */
class OverlayRoot(context: Context, private val onBack: (() -> Unit)? = null) : FrameLayout(context) {
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val back = onBack
        if (back != null && event.keyCode == KeyEvent.KEYCODE_BACK) {
            if (event.action == KeyEvent.ACTION_UP) {
                try { back() } catch (t: Throwable) { android.util.Log.e("FutureSystemUI", "onBack failed", t) }
            }
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets = WindowInsets.CONSUMED
}
