package com.android.sistemui.statusbar.service

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
import android.os.SystemClock
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
import com.android.sistemui.controlcenter.logic.ControlManager
import com.android.sistemui.recents.logic.RecentAppInfo
import com.android.sistemui.recents.logic.RecentAppsManager
import com.android.sistemui.recents.ui.RecentAppsScreen
import com.android.sistemui.statusbar.logic.StatusBarLayoutManager
import com.android.sistemui.statusbar.ui.StatusBarScreen
import com.android.sistemui.statusbar.ui.VolumeOverlay
import com.android.sistemui.theme.ThemeProvider
import com.android.sistemui.ui.theme.FutureUITheme
import com.android.sistemui.utils.FutureUIActions
import com.android.sistemui.utils.FutureUIState

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
    private var powerMenuView: ComposeView? = null
    private var recentsVisible = false
    private val recentAppsList = androidx.compose.runtime.mutableStateListOf<RecentAppInfo>()
    private val recentAppsManager by lazy { RecentAppsManager(this) }
    private val recentsMemory = mutableStateOf<String?>(null)
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
            when (intent?.action) {
                FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT -> bringStatusBarToFront()
                // כפתור הכיבוי במרכז הבקרה פותח את אותו תפריט כיבוי אחד.
                ACTION_SHOW_POWER_MENU -> mainHandler.post { showPowerMenu() }
            }
        }
    }

    // מוגדר ע"י שידורי ACTION_CALL_RINGING/ACTION_CALL_ENDED מ-CallService של dialer -
    // ראו FutureUIActions.ACTION_CALL_RINGING לפירוט. כל עוד שיחה מצלצלת, מקשי
    // CALL/ENDCALL עונים/דוחים אותה מכל מסך, ודאבל-קליק על OK לא פותח את העוזר.
    @Volatile
    private var suppressForActiveCall = false

    private val callReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                FutureUIActions.ACTION_CALL_RINGING -> {
                    suppressForActiveCall = true
                    // ה-fullScreenIntent הרגיל של ההתראה מופעל אוטומטית ע"י המערכת רק
                    // כשהמסך כבוי - כשמסך הבית עצמו הוא האפליקציה בחזית צריך לפתוח את
                    // מסך השיחה במפורש כדי שגם שם השיחה תתקבל במסך מלא, לא רק כהתראה.
                    if (FutureUIState.foregroundPackage == HOME_PACKAGE) {
                        sendBroadcast(Intent(FutureUIActions.ACTION_LAUNCH_CALL_UI).setPackage(DIALER_PACKAGE))
                    }
                }
                FutureUIActions.ACTION_CALL_ENDED -> suppressForActiveCall = false
            }
        }
    }

    // דאבל-קליק גלובלי על OK (בכל מסך במערכת) פותח את העוזר הקולי. הלחיצה
    // הראשונה תמיד עוברת הלאה מיד בלי לחכות - רק אם מגיעה לחיצה שנייה בתוך
    // החלון נבלע אותה (כדי שהאפליקציה מתחת לא תפעיל את הפעולה שלה פעמיים).
    private var lastOkDownTime = 0L
    private var swallowNextOkUp = false

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

            val filter = IntentFilter(FutureUIActions.ACTION_BRING_STATUS_BAR_FRONT).apply {
                addAction(ACTION_SHOW_POWER_MENU)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(bringToFrontReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                registerReceiver(bringToFrontReceiver, filter)
            }

            // ACTION_CALL_RINGING/ENDED מגיעים משידור מפורש (setPackage) של dialer -
            // מ-API 33 חובה להצהיר EXPORTED כדי לקבל שידור כזה מאפליקציה אחרת.
            val callFilter = IntentFilter().apply {
                addAction(FutureUIActions.ACTION_CALL_RINGING)
                addAction(FutureUIActions.ACTION_CALL_ENDED)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(callReceiver, callFilter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(callReceiver, callFilter)
            }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error in StatusBar onCreate", e)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        if (layoutManager?.getSuppressSystemBars() == true) {
            suppressSystemBars()
        }
        showStatusBar()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkg = event.packageName?.toString()
            pkg?.let { FutureUIState.foregroundPackage = it }
            // "אחרונות" = רק אפליקציות שהמשתמש באמת פתח (ר' RecentAppsManager.record).
            runCatching { recentAppsManager.record(pkg, event.className?.toString()) }
            maybeReplaceSystemPowerMenu(pkg, event.className?.toString())
        }
    }
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

        // תפריט הכיבוי (החזקת מקש ההפעלה): BACK סוגר, השאר עוברים לחלון שלו.
        if (powerMenuView != null) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) hidePowerMenu()
                return true
            }
            return false
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
        // בטלפון. לחיצה קצרה (משוחררת לפני שהריצה הארוכה הספיקה לרוץ) לא עושה כלום
        // בעצמה - במקום זאת משודרת ב-ACTION_OPTIONS_SHORT_PRESS כדי שהאפליקציה
        // שבחזית תוכל להגיב (למשל לפתוח תפריט משלה), כי המקש עצמו תמיד נחסם
        // כאן ולא יכול להגיע לאף אפליקציה בשום צורה אחרת.
        if (keyCode == KeyEvent.KEYCODE_MENU || keyCode == KeyEvent.KEYCODE_SETTINGS) {
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

        // מקשי CALL/ENDCALL הפיזיים חייבים לענות/לדחות שיחה מצלצלת גם כשה-dialer אינו
        // בחזית. לא נצרכים, כדי לא לשבור את הטיפול הישיר ב-dialer כשהוא כן בחזית -
        // answer/reject אידמפוטנטיים.
        if (suppressForActiveCall && event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
            when (keyCode) {
                KeyEvent.KEYCODE_CALL -> sendBroadcast(Intent(FutureUIActions.ACTION_ANSWER_CALL).setPackage(DIALER_PACKAGE))
                KeyEvent.KEYCODE_ENDCALL -> sendBroadcast(Intent(FutureUIActions.ACTION_REJECT_CALL).setPackage(DIALER_PACKAGE))
            }
        }

        val isOkKey = keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER
        if (isOkKey && !suppressForActiveCall) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                val now = SystemClock.elapsedRealtime()
                if (lastOkDownTime != 0L && now - lastOkDownTime < DOUBLE_CLICK_WINDOW_MS) {
                    lastOkDownTime = 0L
                    swallowNextOkUp = true
                    launchVoiceAssistant()
                    return true
                }
                lastOkDownTime = now
            } else if (event.action == KeyEvent.ACTION_UP && swallowNextOkUp) {
                swallowNextOkUp = false
                return true
            }
        }

        return super.onKeyEvent(event)
    }

    private fun launchVoiceAssistant() {
        try {
            val intent = packageManager.getLaunchIntentForPackage(VOICE_ASSISTANT_PACKAGE) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("FutureUI", "Failed to launch voice assistant", e)
        }
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

    /**
     * החזקת מקש ההפעלה פותחת את חלון הכיבוי של אנדרואיד (GlobalActionsDialog
     * של com.android.systemui) - מקש ההפעלה עצמו לא מגיע לשירותי נגישות, אז
     * מזהים את החלון כשהוא עולה, סוגרים אותו ב-BACK ופותחים את שלנו במקומו.
     */
    private fun maybeReplaceSystemPowerMenu(pkg: String?, cls: String?) {
        if (pkg != "com.android.systemui" || cls == null) return
        if (!cls.contains("globalactions", ignoreCase = true)) return
        performGlobalAction(GLOBAL_ACTION_BACK)
        mainHandler.postDelayed({ showPowerMenu() }, 120)
    }

    private val powerAirplaneState = mutableStateOf(false)
    private val powerSilentState = mutableStateOf(false)

    private fun showPowerMenu() {
        if (powerMenuView != null) return
        try {
            val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            powerAirplaneState.value = android.provider.Settings.Global.getInt(contentResolver, android.provider.Settings.Global.AIRPLANE_MODE_ON, 0) != 0
            powerSilentState.value = audio?.ringerMode == AudioManager.RINGER_MODE_SILENT

            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
            )
            powerMenuView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(this@StatusBarAccessibilityService)
                setViewTreeSavedStateRegistryOwner(this@StatusBarAccessibilityService)
                setViewTreeViewModelStoreOwner(this@StatusBarAccessibilityService)
                setContent {
                    FutureUITheme {
                        com.android.sistemui.controlcenter.ui.PowerMenuScreen(
                            onPowerOff = {
                                hidePowerMenu()
                                android.widget.Toast.makeText(this@StatusBarAccessibilityService, "מכבה את המכשיר...", android.widget.Toast.LENGTH_SHORT).show()
                                controlManager?.runRootCommandAsync("reboot -p")
                            },
                            onRestart = {
                                hidePowerMenu()
                                android.widget.Toast.makeText(this@StatusBarAccessibilityService, "מפעיל מחדש...", android.widget.Toast.LENGTH_SHORT).show()
                                controlManager?.runRootCommandAsync("reboot")
                            },
                            onCancel = { hidePowerMenu() },
                            airplaneOn = powerAirplaneState.value,
                            onAirplane = {
                                val next = !powerAirplaneState.value
                                powerAirplaneState.value = next
                                controlManager?.let { cm ->
                                    cm.isAirplaneOn = next
                                    cm.toggleAirplane()
                                }
                            },
                            silentOn = powerSilentState.value,
                            onSilent = {
                                val next = !powerSilentState.value
                                val mode = if (next) AudioManager.RINGER_MODE_SILENT else AudioManager.RINGER_MODE_NORMAL
                                val ok = runCatching { audio?.ringerMode = mode }.isSuccess
                                if (ok) {
                                    powerSilentState.value = next
                                } else {
                                    controlManager?.runRootCommandAsync("cmd notification allow_dnd $packageName")
                                }
                            },
                            onScreenshot = {
                                hidePowerMenu()
                                // אחרי שהתפריט ירד מהמסך - אחרת הוא ייכנס לצילום.
                                mainHandler.postDelayed({
                                    controlManager?.runRootCommandAsync(
                                        "d=/sdcard/Pictures/Screenshots; mkdir -p \$d; " +
                                            "f=\$d/Screenshot_\$(date +%Y%m%d-%H%M%S).png; screencap -p \$f; " +
                                            "am broadcast -a android.intent.action.MEDIA_SCANNER_SCAN_FILE -d file://\$f"
                                    )
                                }, 600)
                            },
                        )
                    }
                }
            }
            windowManager.addView(powerMenuView, params)
        } catch (e: Exception) {
            Log.e("FutureUI", "Error showing power menu", e)
            powerMenuView = null
        }
    }

    private fun hidePowerMenu() {
        try {
            powerMenuView?.let { windowManager.removeView(it) }
        } catch (e: Exception) {
            Log.e("FutureUI", "Error hiding power menu", e)
        }
        powerMenuView = null
    }

    /** מסך "אפליקציות אחרונות" עצמאי מבוסס UsageStatsManager, במקום GLOBAL_ACTION_RECENTS המכוער. */
    private fun showRecentApps() {
        if (recentsVisible) return
        try {
            if (!recentAppsManager.hasUsageAccess()) {
                controlManager?.runRootCommandAsync("appops set $packageName GET_USAGE_STATS allow")
            }
            recentAppsList.clear()
            recentAppsList.addAll(recentAppsManager.getRecentApps())
            recentsMemory.value = recentAppsManager.memorySummary()

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
                                controlManager?.runRootCommandAsync("am force-stop ${app.packageName}")
                                recentAppsManager.remove(app.packageName)
                                recentAppsList.remove(app)
                            },
                            onCloseAll = {
                                val all = recentAppsList.toList()
                                if (all.isNotEmpty()) {
                                    controlManager?.runRootCommandAsync(all.joinToString("; ") { "am force-stop ${it.packageName}" })
                                }
                                recentAppsManager.clear()
                                recentAppsList.clear()
                                hideRecentApps()
                            },
                            memorySummary = recentsMemory.value,
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
        controlManager?.runRootCommandAsync("settings put global policy_control immersive.full=*")
    }

    private fun restoreSystemBars() {
        controlManager?.runRootCommandAsync("settings put global policy_control immersive.none=*")
    }

    override fun onDestroy() {
        try {
            hideVolumeOverlay()
            hideRecentApps()
            hidePowerMenu()
            statusBarView?.let { windowManager.removeView(it) }
            statusBarView = null
        } catch (e: Exception) {
            Log.e("FutureUI", "Error tearing down status bar", e)
        }
        restoreSystemBars()
        controlManager?.dispose()
        try {
            unregisterReceiver(bringToFrontReceiver)
            unregisterReceiver(callReceiver)
        } catch (e: Exception) {
            android.util.Log.w("StatusBarAccessibilityS", "onDestroy failed", e)
        }
        try {
            themePrefs.unregisterOnSharedPreferenceChangeListener(themePrefsListener)
        } catch (e: Exception) {
            android.util.Log.w("StatusBarAccessibilityS", "onDestroy failed", e)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
        super.onDestroy()
    }

    companion object {
        private const val VOICE_ASSISTANT_PACKAGE = "com.future.assistant"
        private const val DIALER_PACKAGE = "com.future.dialer"
        private const val HOME_PACKAGE = "com.future.futurelauncher"
        private const val DOUBLE_CLICK_WINDOW_MS = 300L
        const val ACTION_SHOW_POWER_MENU = "com.future.futureui.ACTION_SHOW_POWER_MENU"
    }
}
