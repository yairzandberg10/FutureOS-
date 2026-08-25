package com.future.futureui.statusbar.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.recents.logic.RecentAppInfo
import com.future.futureui.recents.logic.RecentAppsManager
import com.future.futureui.recents.ui.RecentAppsScreen
import com.future.futureui.statusbar.logic.StatusBarLayoutManager
import com.future.futureui.statusbar.ui.StatusBarScreen
import com.future.futureui.statusbar.ui.VolumeOverlay
import com.future.futureui.theme.ThemeProvider
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIActions

/**
 * שירות "עמוד השדרה" של FutureOS: מציג שורת מצב קבועה (לא רק לפי דרישה, כמו שאר
 * חלקי FutureUI), מיירט את מקשי הווליום ומציג במקומם חלונית ווליום משלנו, ומבקש
 * (דרך root) להסתיר את שורת המצב/ניווט המקורית של אנדרואיד כדי שרק שלנו תוצג.
 */
class StatusBarAccessibilityService : AccessibilityService(), LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {

    private lateinit var windowManager: WindowManager
    private var statusBarView: ComposeView? = null
    private var volumeOverlayView: ComposeView? = null
    private var controlManager: ControlManager? = null
    private var layoutManager: StatusBarLayoutManager? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val volumeLevelState = mutableFloatStateOf(0.5f)
    private var hideVolumeRunnable: Runnable? = null
    private var statusBarParams: WindowManager.LayoutParams? = null
    private var recentsView: ComposeView? = null
    private var recentsVisible = false
    private val recentAppsList = androidx.compose.runtime.mutableStateListOf<RecentAppInfo>()
    private val recentAppsManager by lazy { RecentAppsManager(this) }
    private var recentAppsTriggered = false
    private val recentAppsRunnable = Runnable {
        recentAppsTriggered = true
        showRecentApps()
    }

    // צבע ההדגשה/פוקוס המשותף בין כל אפליקציות FutureOS (ThemeProvider). כאן
    // ניגשים ל-SharedPreferences ישירות במקום דרך ContentResolver כי השירות
    // רץ באותו תהליך/UID בדיוק כמו ה-ContentProvider עצמו (שניהם בתוך FutureUI).
    private val themePrefs by lazy { getSharedPreferences("shared_theme_prefs", Context.MODE_PRIVATE) }
    private val recentsAccentColor = mutableStateOf(Color.White)
    private val themePrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
        if (key == ThemeProvider.COL_PRIMARY_COLOR) {
            recentsAccentColor.value = Color(prefs.getInt(ThemeProvider.COL_PRIMARY_COLOR, android.graphics.Color.WHITE))
        }
    }

    private val bringToFrontReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT) {
                bringStatusBarToFront()
            }
        }
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    override fun onCreate() {
        super.onCreate()
        try {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            controlManager = ControlManager(this)
            layoutManager = StatusBarLayoutManager(this)

            recentsAccentColor.value = Color(themePrefs.getInt(ThemeProvider.COL_PRIMARY_COLOR, android.graphics.Color.WHITE))
            themePrefs.registerOnSharedPreferenceChangeListener(themePrefsListener)

            val filter = IntentFilter(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bringToFrontReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(bringToFrontReceiver, filter)
            }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error in StatusBar onCreate", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        controlManager?.startRootShell()
        if (layoutManager?.getSuppressSystemBars() == true) {
            suppressSystemBars()
        }
        showStatusBar()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                adjustVolume(if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) 1 else -1)
            }
            // צורכים תמיד את אירוע המקש כדי שחלונית הווליום המקורית של אנדרואיד לא תופיע
            return true
        }

        // כשמסך "אפליקציות אחרונות" שלנו גלוי, BACK סוגר אותו וכל שאר המקשים
        // (חצים, אישור, Menu לסגירת פריט) עוברים ישירות לחלון שלו כדי שהניווט
        // בפוקוס יעבוד רגיל.
        if (recentsVisible) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) hideRecentApps()
                return true
            }
            return false
        }

        // לחיצה ארוכה על מקש Options/Menu פותחת "אפליקציות אחרונות" מכל מקום
        // בטלפון - חוץ ממסך הנעילה, ששם לאותו מקש כבר יש משמעות (מצב עריכה).
        // כשמסך הנעילה גלוי לא נוגעים במקש בכלל, כדי שהוא יגיע אליו כרגיל.
        // לחיצה קצרה (משוחררת לפני שהריצה הארוכה הספיקה לרוץ) לא עושה כלום
        // בעצמה - במקום זאת משודרת ב-ACTION_OPTIONS_SHORT_PRESS כדי שהאפליקציה
        // שבחזית תוכל להגיב (למשל לפתוח תפריט משלה), כי המקש עצמו תמיד נחסם
        // כאן ולא יכול להגיע לאף אפליקציה בשום צורה אחרת.
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
            if (com.future.futureui.utils.FutureUIState.isLockScreenVisible) {
                return super.onKeyEvent(event)
            }
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    recentAppsTriggered = false
                    mainHandler.removeCallbacks(recentAppsRunnable)
                    mainHandler.postDelayed(recentAppsRunnable, 600)
                }
            } else if (event.action == KeyEvent.ACTION_UP) {
                mainHandler.removeCallbacks(recentAppsRunnable)
                if (!recentAppsTriggered) {
                    sendOptionsShortPressBroadcast()
                }
            }
            return true
        }

        return super.onKeyEvent(event)
    }

    /** לחיצה קצרה על Options/Menu - לא סקופ לחבילה שלנו בכוונה (בניגוד ל-
     * bringFrontIntent), כדי שכל אפליקציה בחזית תוכל להאזין ולהגיב. */
    private fun sendOptionsShortPressBroadcast() {
        try {
            sendBroadcast(Intent(FutureUIActions.ACTION_OPTIONS_SHORT_PRESS))
        } catch (e: Exception) {
            Log.e("FutureUI", "Error sending options short-press broadcast", e)
        }
    }

    private fun adjustVolume(direction: Int) {
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val next = (current + direction).coerceIn(0, max)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, next, 0)
            val fraction = if (max > 0) next.toFloat() / max else 0f
            volumeLevelState.floatValue = fraction
            showVolumeOverlay()
        } catch (e: Exception) {
            Log.e("FutureUI", "Error adjusting volume", e)
        }
    }

    private fun showStatusBar() {
        if (statusBarView != null) return
        try {
            val density = resources.displayMetrics.density
            val heightPx = (StatusBarLayoutManager.HEIGHT_DP * density).toInt()

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                heightPx,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP
            }
            statusBarParams = params

            statusBarView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StatusBarAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@StatusBarAccessibilityService)
                setViewTreeViewModelStoreOwner(this@StatusBarAccessibilityService)
                setContent {
                    FutureUITheme {
                        StatusBarScreen(
                            modifier = Modifier.fillMaxSize(),
                            controlManager = controlManager,
                            layoutManager = layoutManager,
                            accentColor = recentsAccentColor.value
                        )
                    }
                }
            }

            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            windowManager.addView(statusBarView, params)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error showing status bar", e)
        }
    }

    /**
     * חלונות overlay חדשים (מרכז בקרה, מרכז התראות, מסך נעילה) נוספים מעל שורת
     * המצב שלנו כי הם נוצרים אחריה. מסירים ומוסיפים מחדש את חלון שורת המצב כדי
     * שהיא תישאר תמיד העליונה ביותר, ממש כמו שורת מצב אמיתית.
     */
    private fun bringStatusBarToFront() {
        val view = statusBarView ?: return
        val params = statusBarParams ?: return
        try {
            windowManager.removeView(view)
            windowManager.addView(view, params)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error bringing status bar to front", e)
        }
    }

    private fun showVolumeOverlay() {
        hideVolumeRunnable?.let { mainHandler.removeCallbacks(it) }

        if (volumeOverlayView == null) {
            try {
                val params = WindowManager.LayoutParams(
                    WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP
                }

                volumeOverlayView = ComposeView(this).apply {
                    setViewTreeLifecycleOwner(this@StatusBarAccessibilityService)
                    setViewTreeSavedStateRegistryOwner(this@StatusBarAccessibilityService)
                    setViewTreeViewModelStoreOwner(this@StatusBarAccessibilityService)
                    setContent {
                        FutureUITheme {
                            VolumeOverlay(
                                level = volumeLevelState.floatValue,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                windowManager.addView(volumeOverlayView, params)
            } catch (e: Exception) {
                Log.e("FutureUI", "Error showing volume overlay", e)
            }
        }

        hideVolumeRunnable = Runnable { hideVolumeOverlay() }
        mainHandler.postDelayed(hideVolumeRunnable!!, 1500)
    }

    private fun hideVolumeOverlay() {
        try {
            volumeOverlayView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error hiding volume overlay", e)
        }
        volumeOverlayView = null
    }

    /** מסך "אפליקציות אחרונות" עצמאי מבוסס UsageStatsManager, במקום GLOBAL_ACTION_RECENTS המכוער. */
    private fun showRecentApps() {
        if (recentsVisible) return
        try {
            if (!recentAppsManager.hasUsageAccess()) {
                controlManager?.runRootCommand("appops set $packageName GET_USAGE_STATS allow")
            }
            recentAppsList.clear()
            recentAppsList.addAll(recentAppsManager.getRecentApps())

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )

            recentsView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StatusBarAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@StatusBarAccessibilityService)
                setViewTreeViewModelStoreOwner(this@StatusBarAccessibilityService)
                setContent {
                    FutureUITheme {
                        RecentAppsScreen(
                            apps = recentAppsList,
                            accentColor = recentsAccentColor.value,
                            onLaunch = { app ->
                                try {
                                    val intent = packageManager.getLaunchIntentForPackage(app.packageName)
                                    intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (intent != null) startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("FutureUI", "Error launching recent app", e)
                                }
                                hideRecentApps()
                            },
                            onClose = { app ->
                                controlManager?.runRootCommand("am force-stop ${app.packageName}")
                                recentAppsList.remove(app)
                            },
                            onDismiss = { hideRecentApps() }
                        )
                    }
                }
            }

            windowManager.addView(recentsView, params)
            recentsVisible = true

            val bringFrontIntent = Intent(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT)
            bringFrontIntent.setPackage(packageName)
            sendBroadcast(bringFrontIntent)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error showing recent apps", e)
        }
    }

    private fun hideRecentApps() {
        try {
            recentsView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error hiding recent apps", e)
        }
        recentsView = null
        recentsVisible = false
    }

    /**
     * מבקש מהמערכת (דרך root) להיכנס למצב אימרסיבי מלא כברירת מחדל, כדי שהשורה
     * המקורית של אנדרואיד לא תוצג מתחת לשלנו. לא נבדק על מכשיר אמיתי - כדאי
     * לוודא שהפקודה עובדת על ה-ROM הספציפי (יאיר: תבדוק על המכשיר בפועל).
     */
    private fun suppressSystemBars() {
        controlManager?.runRootCommand("settings put global policy_control immersive.full=*")
    }

    private fun restoreSystemBars() {
        controlManager?.runRootCommand("settings put global policy_control immersive.none=*")
    }

    override fun onDestroy() {
        try {
            hideVolumeOverlay()
            hideRecentApps()
            statusBarView?.let { windowManager.removeView(it) }
            statusBarView = null
        } catch (e: Exception) {
            Log.e("FutureUI", "Error tearing down status bar", e)
        }
        restoreSystemBars()
        controlManager?.stopRootShell()
        controlManager?.dispose()
        try {
            unregisterReceiver(bringToFrontReceiver)
        } catch (e: Exception) {}
        try {
            themePrefs.unregisterOnSharedPreferenceChangeListener(themePrefsListener)
        } catch (e: Exception) {}
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }
}
