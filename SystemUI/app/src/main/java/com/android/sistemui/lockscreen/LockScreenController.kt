package com.android.sistemui.lockscreen

import com.android.sistemui.utils.safeText
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaMetadata
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.android.sistemui.controlcenter.logic.ControlManager
import com.android.sistemui.controlcenter.service.MediaControlService
import com.android.sistemui.lockscreen.face.FaceAuthenticator
import com.android.sistemui.lockscreen.face.FaceStatus
import com.android.sistemui.lockscreen.logic.LockCatalog
import com.android.sistemui.lockscreen.logic.LockSettings
import com.android.sistemui.lockscreen.logic.LockWallpapers
import com.android.sistemui.lockscreen.ui.LockScreenUi
import com.android.sistemui.ui.theme.FutureUITheme
import com.android.sistemui.utils.FutureUIState
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * מסך הנעילה. רץ בתוך StatusBarAccessibilityService (שכבר פעיל תמיד) ולא
 * כשירות נפרד: חלון overlay אטום מעל הכל, וכל המקשים (חוץ מווליום ותפריט
 * הכיבוי) נתפסים כאן ב-[onKey] - החלון עצמו לא מקבל פוקוס, כך שאין שום דרך
 * שמקש "יברח" לאפליקציה שמתחת.
 *
 * מקשים:
 *  - OK: פתיחה (אם הפנים זוהו / אין קוד) או מעבר להזנת קוד
 *  - ספרות: מתחילות להזין קוד ישירות
 *  - חזור / Menu קצר: הקיצור השמאלי / הימני (פנס עובד גם בלי זיהוי)
 *  - Menu ארוך: עריכת מסך הנעילה (אחרי זיהוי)
 *  - מטה/מעלה: מעבר בין ההתראות, OK פותח את ההתראה אחרי זיהוי
 */
class LockScreenController(
    private val service: Context,
    private val owner: Any, // LifecycleOwner + SavedStateRegistryOwner + ViewModelStoreOwner
    private val windowManager: WindowManager,
    private val controlManager: () -> ControlManager?,
    private val accentColor: () -> Color,
    private val bringStatusBarFront: () -> Unit,
) {
    val settings = LockSettings(service)
    private val face = FaceAuthenticator(service, settings)
    private val state = LockUiState()
    private val main = Handler(Looper.getMainLooper())
    /** קטלוג הרקעים והורדת רקע נבחר - מחוץ ל-Main. */
    private val io = java.util.concurrent.Executors.newSingleThreadExecutor()
    private var view: ComposeView? = null
    private var screenOffAt = 0L
    private var pinBuffer = StringBuilder()
    private var pendingAction: (() -> Unit)? = null
    private var faceFailedThisSession = false
    private var menuLongPressFired = false

    /** נעול = המסך מוצג, או מושהה זמנית בשביל שיחה/שעון מעורר. */
    var locked = false
        private set
    private var suspended = false

    private val menuLongPress = Runnable {
        menuLongPressFired = true
        requestAuth { enterEdit() }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (view == null) return
            refreshDynamic()
            main.postDelayed(this, 3_000)
        }
    }

    private val lockoutTicker = object : Runnable {
        override fun run() {
            val ms = settings.pin.remainingLockoutMs()
            val secs = ((ms + 999) / 1000).toInt()
            // תחילת נעילה חדשה: זוכרים את האורך שלה בשביל פס ההתקדמות
            if (state.lockoutSeconds == 0 || secs > state.lockoutTotal) state.lockoutTotal = secs
            state.lockoutSeconds = secs
            if (ms > 0) main.postDelayed(this, 1_000) else state.pinError = false
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
            }
        }
    }

    fun start() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        service.registerReceiver(screenReceiver, filter)
        // אחרי הפעלה (או התקנה מחדש של FutureUI) - נועלים מיד אם יש קוד.
        if (settings.enabled && settings.pin.hasPin()) lock()
    }

    fun dispose() {
        runCatching { service.unregisterReceiver(screenReceiver) }
        face.stop()
        removeWindow()
        main.removeCallbacksAndMessages(null)
        io.shutdownNow()
    }

    // ------------------------------------------------------------------ מסך

    private fun onScreenOff() {
        screenOffAt = SystemClock.elapsedRealtime()
        face.stop()
        if (locked) {
            // שיחה פעילה: המסך נכבה מחיישן הקרבה - נשארים מפונים בשביל מסך השיחה
            if (suspended && isInCall()) return
            // המסך נכבה בזמן שהיה נעול: מתחילים מחדש (בלי זיהוי, בלי קוד חלקי).
            // isLocked חוזר ל-true גם אם היינו מפונים (אחרת המקשים לא הגיעו למסך הנעילה).
            main.removeCallbacks(callWatch)
            suspended = false
            FutureUIState.isLocked = true
            FutureUIState.isSecured = true
            resetSession()
            showWindow()
        } else if (settings.enabled && (settings.autoLockDelayMs == 0L || !settings.pin.hasPin())) {
            lock()
        }
    }

    private fun onScreenOn() {
        if (!locked && settings.enabled) {
            val elapsed = SystemClock.elapsedRealtime() - screenOffAt
            if (screenOffAt != 0L && elapsed >= settings.autoLockDelayMs) lock()
        }
        if (locked && !suspended) {
            refreshDynamic()
            startFaceScan()
        }
    }

    /** להציג עכשיו (למשל מההגדרות, "נעל עכשיו"). */
    fun lock() {
        if (!settings.enabled) return
        locked = true
        suspended = false
        FutureUIState.isLocked = true
        FutureUIState.isSecured = true
        resetSession()
        showWindow()
        val pm = service.getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == true) startFaceScan()
    }

    private fun resetSession() {
        val wasEditing = state.mode == LockMode.EDIT
        state.mode = LockMode.MAIN
        state.editPanel = EditPanel.NONE
        state.wallpaperLoading = null
        state.unlocking = false
        state.authenticated = false
        state.faceStatus = null
        state.focusedNotification = -1
        state.pinReason = null
        faceFailedThisSession = false
        pendingAction = null
        clearPin()
        loadSettings()
        // עריכה שלא הסתיימה (המסך נכבה באמצע) - מבוטלת, כולל רקע שנבחר ולא נשמר
        if (wasEditing) {
            LockWallpapers.discardPending(service)
            if (view != null) loadWallpaper()
        }
    }

    private fun loadSettings() {
        state.hasPin = settings.pin.hasPin()
        state.pinLength = settings.pin.pinLength()
        state.faceEnabled = settings.faceEnabled && face.isAvailable()
        state.clockStyle = settings.clockStyle
        state.clockColor = settings.clockColor
        state.background = settings.background
        state.wallpaperId = settings.wallpaperId
        state.widgets = settings.widgets
        state.leftShortcut = settings.leftShortcut
        state.rightShortcut = settings.rightShortcut
        state.ownerMessage = settings.ownerMessage
        state.notificationPrivacy = settings.notificationPrivacy
    }

    private fun showWindow() {
        if (view != null) { runCatching { refreshDynamic() }; return }
        // תקלה בטפט או בווידג'טים לא יכולה למנוע את החלון עצמו - אחרת המכשיר
        // "נעול" בלי מסך נעילה (פתוח לגמרי).
        runCatching { loadWallpaper() }.onFailure { Log.e(TAG, "wallpaper failed", it) }
        runCatching { refreshDynamic() }.onFailure { Log.e(TAG, "refresh failed", it) }
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    // הקלטת מסך/צילום של אפליקציה אחרת לא תופסת את מסך הנעילה
                    // (התראות, הקלדת הקוד)
                    WindowManager.LayoutParams.FLAG_SECURE,
                PixelFormat.OPAQUE
            )
            val v = ComposeView(service).apply {
                setViewTreeLifecycleOwner(owner as LifecycleOwner)
                setViewTreeSavedStateRegistryOwner(owner as SavedStateRegistryOwner)
                setViewTreeViewModelStoreOwner(owner as ViewModelStoreOwner)
                setContent {
                    FutureUITheme {
                        LockScreenUi(state = state, accent = accentColor())
                    }
                }
            }
            windowManager.addView(v, params)
            view = v
            bringStatusBarFront()
            main.removeCallbacks(ticker)
            main.postDelayed(ticker, 3_000)
        } catch (e: Exception) {
            Log.e(TAG, "Error showing lock screen", e)
            // לא נשארים נעולים-בלי-חלון: מנסים שוב עד שהחלון עולה
            if (locked && !suspended) main.postDelayed({ if (locked && !suspended && view == null) showWindow() }, 500)
        }
    }

    private fun removeWindow() {
        main.removeCallbacks(ticker)
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        state.wallpaper = null
        state.deviceWallpaper = null
        state.wallpaperCatalog.clear()
    }

    private fun unlock(action: (() -> Unit)? = null) {
        face.stop()
        locked = false
        suspended = false
        FutureUIState.isLocked = false
        FutureUIState.isSecured = false
        state.unlocking = true
        clearPin()
        // אנימציית היציאה (המסך עולה ודועך) ואז מסירים את החלון
        main.postDelayed({
            if (!locked) removeWindow()
            action?.invoke()
        }, 300)
    }

    // ---------------------------------------------------------- שיחה/מעורר

    /** שיחה נכנסת בזמן נעילה: מפנים את המסך למסך השיחה, ונועלים שוב כשהיא נגמרת. */
    fun onCallRinging() {
        if (locked && !suspended) suspend()
    }

    /**
     * השיחה נגמרה: אם פינינו את מסך הנעילה בשבילה - הוא חוזר מיד. בלי זה החייגן
     * (יומן, אנשי קשר, חיוג) נשאר פתוח לגמרי בלי קוד אחרי כל שיחה נכנסת.
     */
    fun onCallEnded() {
        // ACTION_CALL_ENDED מגיע כשהצלצול נגמר - גם כשענו. בודקים כל שנייה עד
        // שאין שום שיחה פעילה, ורק אז נועלים.
        main.removeCallbacks(callWatch)
        if (locked && suspended) main.post(callWatch)
    }

    private val callWatch = object : Runnable {
        override fun run() {
            if (!locked || !suspended) return
            if (isInCall()) main.postDelayed(this, 1_000) else resume()
        }
    }

    /**
     * יש שיחה (מצלצלת או פעילה). TelephonyManager.callState דורש READ_PHONE_STATE
     * מאנדרואיד 12 - אם ההרשאה לא ניתנה נופלים למצב השמע, שלא דורש הרשאה.
     */
    @Suppress("DEPRECATION")
    private fun isInCall(): Boolean {
        val telephony = runCatching {
            service.getSystemService(android.telephony.TelephonyManager::class.java)?.callState !=
                android.telephony.TelephonyManager.CALL_STATE_IDLE
        }.getOrDefault(false)
        if (telephony) return true
        val mode = runCatching { service.getSystemService(android.media.AudioManager::class.java)?.mode }.getOrNull()
        return mode == android.media.AudioManager.MODE_IN_CALL ||
            mode == android.media.AudioManager.MODE_IN_COMMUNICATION ||
            mode == android.media.AudioManager.MODE_RINGTONE
    }

    fun onForegroundChanged(pkg: String?, cls: String?) {
        if (!locked || pkg == null) return
        val isAlarm = pkg == CLOCK_PACKAGE && cls?.endsWith("AlarmRingActivity") == true
        if (!suspended && isAlarm) { suspend(); return }
        if (!suspended) return
        // בזמן שיחה/מעורר מותר רק מה שקשור אליהם. שעון: רק מסך הצלצול (לא רשימת
        // השעונים). FutureUI: רק חלונות overlay שלו, לא מסכי ההגדרות. כל חלון אחר
        // (ומסך הבית) מחזיר את הנעילה.
        val allowed = when (pkg) {
            // החייגן רק כל עוד יש שיחה - לא כדרך להגיע ליומן/אנשי הקשר מתוך מעורר
            DIALER_PACKAGE -> isInCall()
            "android", "com.android.systemui" -> true
            CLOCK_PACKAGE -> isAlarm || cls == null || !cls.endsWith("Activity")
            service.packageName -> cls == null || !cls.endsWith("Activity")
            else -> false
        }
        if (!allowed) resume()
    }

    private fun resume() {
        main.removeCallbacks(callWatch)
        suspended = false
        FutureUIState.isLocked = true
        FutureUIState.isSecured = true
        resetSession()
        showWindow()
        startFaceScan()
    }

    private fun suspend() {
        suspended = true
        FutureUIState.isLocked = false
        FutureUIState.isSecured = true
        face.stop()
        removeWindow()
    }

    // ------------------------------------------------------------ זיהוי פנים

    private fun startFaceScan() {
        if (!locked || suspended || state.authenticated) return
        if (!settings.faceEnabled) return
        if (!face.isAvailable()) {
            state.faceEnabled = false
            if (settings.strongAuthRequired() && settings.pin.hasPin()) {
                state.pinReason = strongAuthReason()
            }
            return
        }
        state.faceEnabled = true
        face.start(onStatus = { s ->
            if (!locked) return@start
            state.faceStatus = s
            if (s == FaceStatus.NOT_RECOGNIZED) faceFailedThisSession = true
        }, onSuccess = {
            if (!locked) return@start
            state.authenticated = true
            val action = pendingAction
            pendingAction = null
            // פעולה ממתינה (קיצור, התראה, עריכה) פותחת בעצמה אם צריך
            when {
                action != null -> action()
                state.mode == LockMode.PIN -> unlock()
                !settings.faceStayOnLock -> unlock()
                // אחרת נשארים במסך הנעילה עם מנעול פתוח (כמו אייפון) - OK פותח
            }
        })
    }

    private fun strongAuthReason(): String =
        if (settings.prefs.getInt("face_failures", 0) >= LockSettings.MAX_FACE_FAILURES) "יותר מדי ניסיונות זיהוי - הזן קוד"
        else "נדרש קוד כדי להפעיל את זיהוי הפנים"

    // --------------------------------------------------------------- מקשים

    /** true = המקש נצרך ע"י מסך הנעילה. */
    fun onKey(event: KeyEvent): Boolean {
        if (!locked || suspended) return false
        if (state.unlocking) return true
        val code = event.keyCode
        if (code == KeyEvent.KEYCODE_VOLUME_UP || code == KeyEvent.KEYCODE_VOLUME_DOWN ||
            code == KeyEvent.KEYCODE_POWER
        ) return false

        // Menu: לחיצה ארוכה = עריכה, קצרה = הקיצור הימני
        if (code == KeyEvent.KEYCODE_MENU || code == KeyEvent.KEYCODE_SETTINGS) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                menuLongPressFired = false
                main.removeCallbacks(menuLongPress)
                if (state.mode == LockMode.MAIN) main.postDelayed(menuLongPress, 600)
            } else if (event.action == KeyEvent.ACTION_UP) {
                main.removeCallbacks(menuLongPress)
                if (!menuLongPressFired && state.mode == LockMode.MAIN) useShortcut(state.rightShortcut)
                else if (!menuLongPressFired && state.mode == LockMode.EDIT) finishEdit(save = true)
            }
            return true
        }

        if (event.action != KeyEvent.ACTION_DOWN) return true
        when (state.mode) {
            LockMode.PIN -> onPinKey(code)
            LockMode.EDIT -> onEditKey(code)
            LockMode.MAIN -> onMainKey(code, event.repeatCount)
        }
        return true
    }

    private fun onMainKey(code: Int, repeat: Int) {
        val digit = digitOf(code)
        if (digit != null) {
            if (state.hasPin && !state.authenticated) {
                pendingAction = null
                state.mode = LockMode.PIN
                appendDigit(digit)
            }
            return
        }
        if (repeat > 0) return
        when (code) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                val i = state.focusedNotification
                val n = state.notifications.getOrNull(i)
                if (n != null) requestAuth { openNotification(n.key, n.packageName) } else requestAuth(null)
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (state.notifications.isNotEmpty()) {
                    state.focusedNotification = (state.focusedNotification + 1).coerceAtMost(state.notifications.lastIndex)
                }
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (state.focusedNotification >= 0) state.focusedNotification--
            }
            KeyEvent.KEYCODE_BACK -> {
                if (state.focusedNotification >= 0) state.focusedNotification = -1
                else useShortcut(state.leftShortcut)
            }
            KeyEvent.KEYCODE_CALL -> requestAuth { unlock { launchPackage(DIALER_PACKAGE) } }
        }
    }

    private fun onPinKey(code: Int) {
        val digit = digitOf(code)
        when {
            digit != null -> appendDigit(digit)
            code == KeyEvent.KEYCODE_BACK || code == KeyEvent.KEYCODE_DEL -> {
                if (pinBuffer.isNotEmpty()) {
                    pinBuffer.setLength(pinBuffer.length - 1)
                    state.pinEntered = pinBuffer.length
                } else {
                    state.mode = LockMode.MAIN
                    pendingAction = null
                }
            }
        }
    }

    private fun appendDigit(d: Char) {
        val lockout = settings.pin.remainingLockoutMs()
        if (lockout > 0) {
            state.pinError = true
            main.removeCallbacks(lockoutTicker)
            lockoutTicker.run()
            return
        }
        state.pinError = false
        pinBuffer.append(d)
        state.pinEntered = pinBuffer.length
        if (pinBuffer.length < state.pinLength) return
        val entered = pinBuffer.toString()
        clearPin()
        if (settings.pin.verifyPin(entered)) {
            settings.onStrongAuth()
            if (faceFailedThisSession) face.learnFromLastProbe()
            state.authenticated = true
            state.mode = LockMode.MAIN
            val action = pendingAction
            pendingAction = null
            if (action != null) action() else unlock()
        } else {
            state.pinError = true
            state.pinErrorTick++
            main.removeCallbacks(lockoutTicker)
            lockoutTicker.run()
        }
    }

    private fun clearPin() {
        // מוחקים את הספרות מהזיכרון, לא רק מאפסים את האורך
        for (i in pinBuffer.indices) pinBuffer.setCharAt(i, '0')
        pinBuffer.setLength(0)
        state.pinEntered = 0
    }

    /** מבצע [action] אחרי זיהוי: מיד אם כבר מזוהה / אין קוד, אחרת אחרי קוד או פנים. */
    private fun requestAuth(action: (() -> Unit)?) {
        if (!state.hasPin || state.authenticated) {
            state.mode = LockMode.MAIN
            if (action != null) action() else unlock()
            return
        }
        pendingAction = action
        state.mode = LockMode.PIN
        if (settings.strongAuthRequired()) state.pinReason = strongAuthReason()
        // OK גם מנסה שוב את הפנים, כמו הרמת הטלפון באייפון
        if (state.faceStatus != FaceStatus.SCANNING) startFaceScan()
    }

    // ------------------------------------------------------------- קיצורים

    private fun useShortcut(id: String) {
        when (id) {
            "none" -> Unit
            // הפנס בלי זיהוי - כמו בכל טלפון
            "flashlight" -> controlManager()?.let { cm ->
                cm.isFlashlightOn = !cm.isFlashlightOn
                cm.toggleFlashlight()
                state.flashlightOn = cm.isFlashlightOn
            }
            else -> requestAuth {
                val pkg = LockCatalog.shortcutPackage(id)
                unlock { if (pkg != null) launchPackage(pkg) }
            }
        }
    }

    private fun launchPackage(pkg: String) {
        try {
            val intent = service.packageManager.getLaunchIntentForPackage(pkg) ?: return
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch $pkg", e)
        }
    }

    private fun openNotification(key: String, pkg: String) {
        unlock {
            val sbn = runCatching { MediaControlService.instance?.activeNotifications?.firstOrNull { it.key == key } }.getOrNull()
            val sent = runCatching { sbn?.notification?.contentIntent?.send(); sbn?.notification?.contentIntent != null }.getOrDefault(false)
            if (!sent) launchPackage(pkg)
        }
    }

    // --------------------------------------------------------------- עריכה
    //
    // כמו One UI: המסך נשאר כמו שהוא, כל רכיב במסגרת. חצים בוחרים רכיב, OK
    // פותח את הלוח שלו מלמטה, תפריט = סיום (שמירה), חזור = ביטול. השינויים
    // נראים מיד אבל נשמרים רק בסיום.

    private fun enterEdit() {
        if (!locked) return
        state.mode = LockMode.EDIT
        state.editTarget = EditTarget.CLOCK
        state.editPanel = EditPanel.NONE
        state.focusedNotification = -1
    }

    private fun finishEdit(save: Boolean) {
        if (save) {
            settings.clockStyle = state.clockStyle
            settings.clockColor = state.clockColor
            settings.background = state.background
            settings.widgets = state.widgets
            settings.leftShortcut = state.leftShortcut
            settings.rightShortcut = state.rightShortcut
            if (state.wallpaperId != settings.wallpaperId) {
                LockWallpapers.commit(service, state.wallpaperId)
                settings.wallpaperId = state.wallpaperId
            } else {
                LockWallpapers.discardPending(service)
            }
        } else {
            LockWallpapers.discardPending(service)
            loadSettings()
            loadWallpaper()
        }
        state.editPanel = EditPanel.NONE
        state.wallpaperLoading = null
        state.mode = LockMode.MAIN
        refreshDynamic()
    }

    private fun onEditKey(code: Int) {
        when (state.editPanel) {
            EditPanel.NONE -> onEditOverviewKey(code)
            EditPanel.CLOCK -> onClockPanelKey(code)
            EditPanel.WIDGETS -> onWidgetsPanelKey(code)
            EditPanel.SHORTCUTS -> onShortcutsPanelKey(code)
            EditPanel.WALLPAPER -> onWallpaperPanelKey(code)
        }
    }

    private fun onEditOverviewKey(code: Int) {
        val t = state.editTarget
        val bottom = t == EditTarget.LEFT_SHORTCUT || t == EditTarget.WALLPAPER || t == EditTarget.RIGHT_SHORTCUT
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> state.editTarget = when (t) {
                EditTarget.CLOCK -> EditTarget.WIDGETS
                EditTarget.WIDGETS -> EditTarget.WALLPAPER
                else -> t
            }
            KeyEvent.KEYCODE_DPAD_UP -> state.editTarget = when {
                bottom -> EditTarget.WIDGETS
                t == EditTarget.WIDGETS -> EditTarget.CLOCK
                else -> t
            }
            // השורה התחתונה לפי המיקום הפיזי: שמאלה = הקיצור השמאלי
            KeyEvent.KEYCODE_DPAD_LEFT -> if (bottom) state.editTarget =
                if (t == EditTarget.RIGHT_SHORTCUT) EditTarget.WALLPAPER else EditTarget.LEFT_SHORTCUT
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (bottom) state.editTarget =
                if (t == EditTarget.LEFT_SHORTCUT) EditTarget.WALLPAPER else EditTarget.RIGHT_SHORTCUT
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> openPanel(t)
            KeyEvent.KEYCODE_BACK -> finishEdit(save = false)
        }
    }

    private fun openPanel(t: EditTarget) {
        state.panelRow = 0
        state.panelIndex = 0
        when (t) {
            EditTarget.CLOCK -> state.editPanel = EditPanel.CLOCK
            EditTarget.WIDGETS -> state.editPanel = EditPanel.WIDGETS
            EditTarget.LEFT_SHORTCUT, EditTarget.RIGHT_SHORTCUT -> {
                state.panelRow = 1
                setShortcutSide(if (t == EditTarget.LEFT_SHORTCUT) 0 else 1)
                state.editPanel = EditPanel.SHORTCUTS
            }
            EditTarget.WALLPAPER -> {
                state.wallpaperCategory = 0
                state.panelRow = 1
                state.editPanel = EditPanel.WALLPAPER
                loadWallpaperCatalog()
            }
        }
    }

    private fun closePanel() {
        state.editPanel = EditPanel.NONE
    }

    /** שעון: שורה 0 סגנון, שורה 1 צבע. RTL - שמאלה = הבא. */
    private fun onClockPanelKey(code: Int) {
        when (code) {
            KeyEvent.KEYCODE_DPAD_UP -> state.panelRow = 0
            KeyEvent.KEYCODE_DPAD_DOWN -> state.panelRow = 1
            KeyEvent.KEYCODE_DPAD_LEFT -> stepClock(1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> stepClock(-1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> closePanel()
        }
    }

    private fun stepClock(delta: Int) {
        if (state.panelRow == 0) {
            state.clockStyle = (state.clockStyle + delta).coerceIn(0, LockCatalog.clockStyles.lastIndex)
        } else {
            state.clockColor = (state.clockColor + delta).coerceIn(0, LockCatalog.clockColors.lastIndex)
        }
    }

    /** ווידג'טים: רשימה, OK מוסיף/מסיר. הסדר = סדר הבחירה, עד 3. */
    private fun onWidgetsPanelKey(code: Int) {
        val last = LockCatalog.widgetChoices.lastIndex
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> state.panelIndex = (state.panelIndex + 1).coerceAtMost(last)
            KeyEvent.KEYCODE_DPAD_UP -> state.panelIndex = (state.panelIndex - 1).coerceAtLeast(0)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> toggleWidget(LockCatalog.widgetChoices[state.panelIndex])
            KeyEvent.KEYCODE_BACK -> closePanel()
        }
    }

    private fun toggleWidget(id: String) {
        val selected = state.widgets.filter { it != "none" }.toMutableList()
        when {
            id in selected -> selected.remove(id)
            selected.size < LockSettings.WIDGET_SLOTS -> selected.add(id)
            else -> return
        }
        state.widgets = List(LockSettings.WIDGET_SLOTS) { selected.getOrNull(it) ?: "none" }
    }

    /** קיצורים: שורה 0 בחירת צד, שורה 1 רשת 3x3. המעבר ברשת בוחר מיד. */
    private fun onShortcutsPanelKey(code: Int) {
        val choices = LockCatalog.shortcutChoices
        val i = state.panelIndex
        if (state.panelRow == 0) {
            when (code) {
                // הלשוניות מסודרות לפי המיקום הפיזי: שמאלית = חזור
                KeyEvent.KEYCODE_DPAD_LEFT -> setShortcutSide(0)
                KeyEvent.KEYCODE_DPAD_RIGHT -> setShortcutSide(1)
                KeyEvent.KEYCODE_DPAD_DOWN -> state.panelRow = 1
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> closePanel()
            }
            return
        }
        val next = when (code) {
            KeyEvent.KEYCODE_DPAD_LEFT -> if (i % 3 < 2 && i + 1 <= choices.lastIndex) i + 1 else i
            KeyEvent.KEYCODE_DPAD_RIGHT -> if (i % 3 > 0) i - 1 else i
            KeyEvent.KEYCODE_DPAD_DOWN -> if (i + 3 <= choices.lastIndex) i + 3 else i
            KeyEvent.KEYCODE_DPAD_UP -> if (i >= 3) i - 3 else { state.panelRow = 0; i }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_BACK -> { closePanel(); return }
            else -> return
        }
        if (next != i) {
            state.panelIndex = next
            if (state.shortcutSide == 0) state.leftShortcut = choices[next] else state.rightShortcut = choices[next]
        }
    }

    private fun setShortcutSide(side: Int) {
        state.shortcutSide = side
        val current = if (side == 0) state.leftShortcut else state.rightShortcut
        state.panelIndex = LockCatalog.shortcutChoices.indexOf(current).coerceAtLeast(0)
    }

    /** רקע: שורה 0 קטגוריות, שורה 1 רשת (3 בשורה), שורה 2 טשטוש. */
    private fun onWallpaperPanelKey(code: Int) {
        when (state.panelRow) {
            0 -> when (code) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    val delta = if (code == KeyEvent.KEYCODE_DPAD_LEFT) 1 else -1
                    val c = (state.wallpaperCategory + delta).coerceIn(0, state.wallpaperCategories.lastIndex)
                    if (c != state.wallpaperCategory) { state.wallpaperCategory = c; state.panelIndex = 0 }
                }
                KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> state.panelRow = 1
                KeyEvent.KEYCODE_BACK -> closePanel()
            }
            1 -> {
                val tiles = state.wallpaperTiles
                val i = state.panelIndex.coerceIn(0, tiles.lastIndex)
                when (code) {
                    KeyEvent.KEYCODE_DPAD_LEFT -> if (i % 3 < 2 && i + 1 <= tiles.lastIndex) state.panelIndex = i + 1
                    KeyEvent.KEYCODE_DPAD_RIGHT -> if (i % 3 > 0) state.panelIndex = i - 1
                    KeyEvent.KEYCODE_DPAD_DOWN -> when {
                        i + 3 <= tiles.lastIndex -> state.panelIndex = i + 3
                        i / 3 < tiles.lastIndex / 3 -> state.panelIndex = tiles.lastIndex
                        else -> state.panelRow = 2
                    }
                    KeyEvent.KEYCODE_DPAD_UP -> if (i >= 3) state.panelIndex = i - 3 else state.panelRow = 0
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> selectWallpaper(tiles[i])
                    KeyEvent.KEYCODE_BACK -> closePanel()
                }
            }
            else -> when (code) {
                KeyEvent.KEYCODE_DPAD_UP -> state.panelRow = 1
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> state.background = if (state.background == 1) 0 else 1
                KeyEvent.KEYCODE_BACK -> closePanel()
            }
        }
    }

    private fun loadWallpaperCatalog() {
        if (state.wallpaperCatalog.isNotEmpty()) { focusCurrentWallpaper(); return }
        io.execute {
            val list = LockWallpapers.catalog(service)
            main.post {
                state.wallpaperCatalog.clear()
                state.wallpaperCatalog.addAll(list)
                focusCurrentWallpaper()
            }
        }
    }

    private fun focusCurrentWallpaper() {
        val i = state.wallpaperTiles.indexOf(state.wallpaperId)
        if (i >= 0 && state.editPanel == EditPanel.WALLPAPER) state.panelIndex = i
    }

    private fun selectWallpaper(id: String) {
        if (id.isEmpty()) {
            state.wallpaperLoading = null
            state.wallpaperId = ""
            state.wallpaper = state.deviceWallpaper
            return
        }
        if (id == state.wallpaperId || state.wallpaperLoading != null) return
        state.wallpaperLoading = id
        io.execute {
            val bmp = LockWallpapers.fetchPending(service, id)
            main.post {
                if (state.wallpaperLoading != id) return@post
                state.wallpaperLoading = null
                if (bmp != null && state.mode == LockMode.EDIT) {
                    state.wallpaperId = id
                    state.wallpaper = bmp.asImageBitmap()
                }
            }
        }
    }

    // ---------------------------------------------------------- נתונים חיים

    private fun refreshDynamic() {
        try {
            val bm = service.getSystemService(BatteryManager::class.java)
            state.batteryPercent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
            state.charging = bm?.isCharging == true
            val alarm = service.getSystemService(AlarmManager::class.java)?.nextAlarmClock
            state.nextAlarm = alarm?.let { SimpleDateFormat("EEE HH:mm", Locale("he")).format(it.triggerTime) }
            state.mediaTitle = runCatching {
                MediaControlService.getActiveControllers(service).firstOrNull()?.metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
            }.getOrNull()
            state.flashlightOn = controlManager()?.isFlashlightOn == true
            refreshNotifications()
        } catch (e: Exception) {
            Log.w(TAG, "refresh failed", e)
        }
    }

    private fun refreshNotifications() {
        val active = runCatching { MediaControlService.instance?.activeNotifications }.getOrNull() ?: return
        val pm = service.packageManager
        val list = active
            .filter { it.packageName != service.packageName && !it.isOngoing && it.notification.safeText("android.title") != null }
            // האפליקציה ביקשה שההתראה לא תופיע במסך נעילה בכלל (VISIBILITY_SECRET) -
            // כמו באנדרואיד, גם כשההגדרה היא "הצג הכל"
            .filter { it.notification.visibility != android.app.Notification.VISIBILITY_SECRET }
            .sortedByDescending { it.postTime }
            .take(MAX_NOTIFICATIONS)
            .map { sbn ->
                val appInfo = runCatching { pm.getApplicationInfo(sbn.packageName, 0) }.getOrNull()
                LockNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: sbn.packageName,
                    title = sbn.notification.safeText("android.title")?.toString() ?: "",
                    text = sbn.notification.safeText("android.text")?.toString() ?: "",
                    time = sbn.postTime,
                    icon = iconCache.getOrPut(sbn.packageName) {
                        appInfo?.let { runCatching { drawableToBitmap(pm.getApplicationIcon(it), 64).asImageBitmap() }.getOrNull() }
                    },
                )
            }
        if (list.map { it.key } != state.notifications.map { it.key }) {
            state.notifications.clear()
            state.notifications.addAll(list)
            if (state.focusedNotification > list.lastIndex) state.focusedNotification = list.lastIndex
        }
    }

    private val iconCache = HashMap<String, androidx.compose.ui.graphics.ImageBitmap?>()

    private fun loadWallpaper() {
        val device = LockWallpapers.device(service)
        state.deviceWallpaper = device?.asImageBitmap()
        state.wallpaper =
            if (state.wallpaperId.isEmpty()) state.deviceWallpaper
            else LockWallpapers.load(service, state.wallpaperId)?.asImageBitmap()
    }

    private fun drawableToBitmap(d: Drawable, size: Int): Bitmap {
        if (d is BitmapDrawable && d.bitmap != null && size == 0) return d.bitmap
        val w = if (size > 0) size else d.intrinsicWidth.coerceAtLeast(1)
        val h = if (size > 0) size else d.intrinsicHeight.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        d.setBounds(0, 0, w, h)
        d.draw(c)
        return bmp
    }

    private fun digitOf(code: Int): Char? = when (code) {
        in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> '0' + (code - KeyEvent.KEYCODE_0)
        in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> '0' + (code - KeyEvent.KEYCODE_NUMPAD_0)
        else -> null
    }

    companion object {
        private const val TAG = "LockScreen"
        private const val DIALER_PACKAGE = "com.future.dialer"
        private const val CLOCK_PACKAGE = "com.future.clock"
        private const val MAX_NOTIFICATIONS = 4
    }
}
