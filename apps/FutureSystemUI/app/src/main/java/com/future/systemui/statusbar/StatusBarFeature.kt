package com.future.systemui.statusbar

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.internal.statusbar.StatusBarIcon
import com.future.sharednav.i18n.Tr
import com.future.systemui.CommandQueue
import com.future.systemui.Feature

/**
 * The real status bar window (TYPE_STATUS_BAR), driven by the IStatusBar callbacks of [CommandQueue].
 * Every callback is wrapped so a failure is logged instead of crashing the system UI process.
 */
class StatusBarFeature : Feature, CommandQueue.Callbacks {

    private var appContext: Context? = null
    private var windowManager: WindowManager? = null
    private var barView: ComposeView? = null
    private var monitor: DeviceStatusMonitor? = null

    // Lifecycle owners for Compose (a window added from a Service has none of its own).
    private val owner = BarOwner()

    private val slotIcons = LinkedHashMap<String, StatusBarSlotIcon>()

    private var disable1 = 0
    private var topAppHidesBar = false
    private var transientShown = false

    override fun start(context: Context, commandQueue: CommandQueue) {
        try {
            val app = context.applicationContext ?: context
            appContext = app
            Tr.init(app)
            windowManager = app.getSystemService(WindowManager::class.java)
            commandQueue.addCallback(this)
            monitor = DeviceStatusMonitor(app).also { it.start() }
            addBarWindow(app)
        } catch (t: Throwable) {
            Log.e(TAG, "StatusBarFeature.start failed", t)
        }
    }

    // ---------------------------------------------------------------- window

    private fun barHeightPx(context: Context): Int {
        val res = context.resources
        val ours = (BAR_HEIGHT_DP * res.displayMetrics.density).toInt()
        val id = res.getIdentifier("status_bar_height", "dimen", "android")
        val system = if (id > 0) res.getDimensionPixelSize(id) else 0
        return maxOf(ours, system)
    }

    private fun addBarWindow(context: Context) {
        val wm = windowManager ?: return
        val mon = monitor ?: return
        owner.create()
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setContent { StatusBarContent(mon) }
        }
        barView = view
        owner.resume()

        val height = barHeightPx(context)
        val types = intArrayOf(TYPE_STATUS_BAR, TYPE_STATUS_BAR_ADDITIONAL, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
        for (type in types) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    height,
                    type,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT,
                ).apply {
                    gravity = Gravity.TOP
                    title = "FutureStatusBar"
                    layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
                    setFitInsetsTypes(0)
                }
                wm.addView(view, params)
                Log.i(TAG, "status bar window added, type=$type height=$height")
                applyVisibility()
                return
            } catch (t: Throwable) {
                Log.w(TAG, "addView failed for window type $type", t)
            }
        }
        barView = null
        Log.e(TAG, "status bar window could not be added with any type")
    }

    /** Hides the whole window when the bar must not be seen; the content itself reacts to the flow states. */
    private fun applyVisibility() {
        try {
            val hide = hideWholeBar()
            barView?.visibility = if (hide) View.GONE else View.VISIBLE
        } catch (t: Throwable) {
            Log.e(TAG, "applyVisibility failed", t)
        }
    }

    private fun hideWholeBar(): Boolean {
        val systemInfo = disable1 and DISABLE_SYSTEM_INFO != 0
        val notifIcons = disable1 and DISABLE_NOTIFICATION_ICONS != 0
        val disabledAll = systemInfo && notifIcons
        // A top app that hides the bar (immersive) only shows it again while a transient reveal is active.
        val appHides = topAppHidesBar && !transientShown
        return disabledAll || appHides
    }

    // ---------------------------------------------------------------- IStatusBar callbacks

    override fun disable(p0: Int, p1: Int, p2: Int) {
        try {
            if (p0 != DEFAULT_DISPLAY) return
            disable1 = p1
            StatusBarState.clockHidden.value = p1 and DISABLE_CLOCK != 0
            StatusBarState.notificationIconsHidden.value = p1 and DISABLE_NOTIFICATION_ICONS != 0
            StatusBarState.systemInfoHidden.value = p1 and DISABLE_SYSTEM_INFO != 0
            applyVisibility()
        } catch (t: Throwable) {
            Log.e(TAG, "disable failed", t)
        }
    }

    override fun setTopAppHidesStatusBar(p0: Boolean) {
        try {
            topAppHidesBar = p0
            if (!p0) transientShown = false
            applyVisibility()
        } catch (t: Throwable) {
            Log.e(TAG, "setTopAppHidesStatusBar failed", t)
        }
    }

    override fun showTransient(p0: Int, p1: IntArray?) {
        try {
            if (p0 != DEFAULT_DISPLAY || p1 == null || !p1.contains(ITYPE_STATUS_BAR)) return
            transientShown = true
            applyVisibility()
        } catch (t: Throwable) {
            Log.e(TAG, "showTransient failed", t)
        }
    }

    override fun abortTransient(p0: Int, p1: IntArray?) {
        try {
            if (p0 != DEFAULT_DISPLAY || p1 == null || !p1.contains(ITYPE_STATUS_BAR)) return
            transientShown = false
            applyVisibility()
        } catch (t: Throwable) {
            Log.e(TAG, "abortTransient failed", t)
        }
    }

    override fun setIcon(p0: String?, p1: StatusBarIcon?) {
        try {
            val slot = p0 ?: return
            if (p1 == null) { removeIcon(slot); return }
            if (slot in OWN_SLOTS) return // drawn by the bar itself
            val visible = readField(p1, "visible") as? Boolean ?: true
            val icon = readField(p1, "icon") as? android.graphics.drawable.Icon
            if (!visible || icon == null) {
                removeIcon(slot)
                return
            }
            val ctx = appContext ?: return
            val bitmap = try {
                icon.loadDrawable(ctx)?.toBitmap(ICON_PX, ICON_PX)?.asImageBitmap()
            } catch (t: Throwable) {
                Log.w(TAG, "icon load failed for $slot", t)
                null
            }
            slotIcons[slot] = StatusBarSlotIcon(
                slot = slot,
                bitmap = bitmap,
                description = readField(p1, "contentDescription") as? CharSequence,
                number = readField(p1, "number") as? Int ?: 0,
            )
            publishIcons()
        } catch (t: Throwable) {
            Log.e(TAG, "setIcon failed", t)
        }
    }

    override fun removeIcon(p0: String?) {
        try {
            if (p0 != null && slotIcons.remove(p0) != null) publishIcons()
        } catch (t: Throwable) {
            Log.e(TAG, "removeIcon failed", t)
        }
    }

    private fun publishIcons() {
        StatusBarState.icons.value = slotIcons.values.toList()
    }

    /** StatusBarIcon is a hidden class (empty compile-time stub); its fields are read reflectively. */
    private fun readField(target: Any, name: String): Any? = try {
        target.javaClass.getField(name).get(target)
    } catch (_: Throwable) {
        try {
            target.javaClass.getDeclaredField(name).apply { isAccessible = true }.get(target)
        } catch (t: Throwable) {
            Log.w(TAG, "StatusBarIcon.$name not readable", t)
            null
        }
    }

    // ---------------------------------------------------------------- lifecycle owner

    private class BarOwner : LifecycleOwner, SavedStateRegistryOwner {
        private val registry = LifecycleRegistry(this)
        private val savedState = SavedStateRegistryController.create(this)

        override val lifecycle: Lifecycle get() = registry
        override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

        fun create() {
            savedState.performRestore(null)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        }

        fun resume() {
            registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }
    }

    private companion object {
        const val TAG = "FutureSystemUI"

        // WindowManager.LayoutParams.TYPE_STATUS_BAR / TYPE_STATUS_BAR_ADDITIONAL (system-only types).
        const val TYPE_STATUS_BAR = 2000
        const val TYPE_STATUS_BAR_ADDITIONAL = 2041

        // StatusBarManager.DISABLE_* flags (state1).
        const val DISABLE_NOTIFICATION_ICONS = 0x00020000
        const val DISABLE_SYSTEM_INFO = 0x00100000
        const val DISABLE_CLOCK = 0x00800000

        const val DEFAULT_DISPLAY = 0
        const val ITYPE_STATUS_BAR = 0 // InsetsState.ITYPE_STATUS_BAR on Android 12

        /** Same as FutureUI's StatusBarLayoutManager.HEIGHT_DP. */
        const val BAR_HEIGHT_DP = 28
        const val ICON_PX = 48

        /** Slots the bar draws itself (battery, clock, wifi, bluetooth, cellular) - skip the framework copies. */
        val OWN_SLOTS = setOf("clock", "battery", "wifi", "bluetooth", "mobile", "no_sim")
    }
}
