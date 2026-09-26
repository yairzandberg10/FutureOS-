package com.future.futureui.lockscreen

import android.app.AlarmManager
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
import com.future.futureui.controlcenter.logic.ControlManager
import com.future.futureui.controlcenter.service.MediaControlService
import com.future.futureui.lockscreen.face.FaceAuthenticator
import com.future.futureui.lockscreen.face.FaceStatus
import com.future.futureui.lockscreen.logic.LockCatalog
import com.future.futureui.lockscreen.logic.LockSettings
import com.future.futureui.lockscreen.ui.LockScreenUi
import com.future.futureui.ui.theme.FutureUITheme
import com.future.futureui.utils.FutureUIState
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
            state.lockoutSeconds = ((ms + 999) / 1000).toInt()
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
    }

    // ------------------------------------------------------------------ מסך

    private fun onScreenOff() {
        screenOffAt = SystemClock.elapsedRealtime()
        face.stop()
        if (locked) {
            // המסך נכבה בזמן שהיה נעול: מתחילים מחדש (בלי זיהוי, בלי קוד חלקי)
            suspended = false
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
        resetSession()
        showWindow()
        val pm = service.getSystemService(PowerManager::class.java)
        if (pm?.isInteractive == true) startFaceScan()
    }

    private fun resetSession() {
        state.mode = LockMode.MAIN
        state.unlocking = false
        state.authenticated = false
        state.faceStatus = null
        state.focusedNotification = -1
        state.pinReason = null
        faceFailedThisSession = false
        pendingAction = null
        clearPin()
        loadSettings()
    }

    private fun loadSettings() {
        state.hasPin = settings.pin.hasPin()
        state.pinLength = settings.pin.pinLength()
        state.faceEnabled = settings.faceEnabled && face.isAvailable()
        state.clockStyle = settings.clockStyle
        state.clockColor = settings.clockColor
        state.background = settings.background
        state.widgets = settings.widgets
        state.leftShortcut = settings.leftShortcut
        state.rightShortcut = settings.rightShortcut
        state.ownerMessage = settings.ownerMessage
        state.notificationPrivacy = settings.notificationPrivacy
    }

    private fun showWindow() {
        if (view != null) { refreshDynamic(); return }
        try {
            loadWallpaper()
            refreshDynamic()
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
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
        }
    }

    private fun removeWindow() {
        main.removeCallbacks(ticker)
        view?.let { runCatching { windowManager.removeView(it) } }
        view = null
        state.wallpaper = null
    }

    private fun unlock(action: (() -> Unit)? = null) {
        face.stop()
        locked = false
        suspended = false
        FutureUIState.isLocked = false
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

    fun onForegroundChanged(pkg: String?, cls: String?) {
        if (!locked || pkg == null) return
        val isAlarm = pkg == CLOCK_PACKAGE && cls?.endsWith("AlarmRingActivity") == true
        if (!suspended && isAlarm) { suspend(); return }
        // חזרה מהשיחה/מעורר למשהו אחר - המסך חוזר
        if (suspended && pkg != DIALER_PACKAGE && pkg != CLOCK_PACKAGE && pkg != service.packageName &&
            pkg != "android" && pkg != "com.android.systemui"
        ) {
            suspended = false
            FutureUIState.isLocked = true
            resetSession()
            showWindow()
            startFaceScan()
        }
    }

    private fun suspend() {
        suspended = true
        FutureUIState.isLocked = false
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
                else if (!menuLongPressFired && state.mode == LockMode.EDIT) exitEdit()
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

    private fun enterEdit() {
        if (!locked) return
        state.mode = LockMode.EDIT
        state.editSector = EditSector.CLOCK_STYLE
    }

    private fun exitEdit() {
        state.mode = LockMode.MAIN
    }

    private fun onEditKey(code: Int) {
        val sectors = EditSector.values()
        when (code) {
            KeyEvent.KEYCODE_DPAD_DOWN -> state.editSector = sectors[(state.editSector.ordinal + 1).coerceAtMost(sectors.lastIndex)]
            KeyEvent.KEYCODE_DPAD_UP -> state.editSector = sectors[(state.editSector.ordinal - 1).coerceAtLeast(0)]
            // RTL: שמאלה = הבא
            KeyEvent.KEYCODE_DPAD_LEFT -> changeEditValue(1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> changeEditValue(-1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> changeEditValue(1)
            KeyEvent.KEYCODE_BACK -> exitEdit()
        }
    }

    private fun changeEditValue(delta: Int) {
        when (val s = state.editSector) {
            EditSector.CLOCK_STYLE -> {
                state.clockStyle = LockCatalog.cycleIndex(LockCatalog.clockStyles.size, state.clockStyle, delta)
                settings.clockStyle = state.clockStyle
            }
            EditSector.CLOCK_COLOR -> {
                state.clockColor = LockCatalog.cycleIndex(LockCatalog.clockColors.size, state.clockColor, delta)
                settings.clockColor = state.clockColor
            }
            EditSector.BACKGROUND -> {
                state.background = LockCatalog.cycleIndex(LockCatalog.backgrounds.size, state.background, delta)
                settings.background = state.background
            }
            EditSector.WIDGET_1, EditSector.WIDGET_2, EditSector.WIDGET_3 -> {
                val slot = s.ordinal - EditSector.WIDGET_1.ordinal
                val list = state.widgets.toMutableList()
                list[slot] = LockCatalog.cycle(LockCatalog.widgets, list[slot], delta)
                state.widgets = list
                settings.widgets = list
                refreshDynamic()
            }
            EditSector.LEFT_SHORTCUT -> {
                state.leftShortcut = LockCatalog.cycle(LockCatalog.shortcuts, state.leftShortcut, delta)
                settings.leftShortcut = state.leftShortcut
            }
            EditSector.RIGHT_SHORTCUT -> {
                state.rightShortcut = LockCatalog.cycle(LockCatalog.shortcuts, state.rightShortcut, delta)
                settings.rightShortcut = state.rightShortcut
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
            .filter { it.packageName != service.packageName && !it.isOngoing && it.notification.extras.getCharSequence("android.title") != null }
            .sortedByDescending { it.postTime }
            .take(MAX_NOTIFICATIONS)
            .map { sbn ->
                val appInfo = runCatching { pm.getApplicationInfo(sbn.packageName, 0) }.getOrNull()
                LockNotification(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    appName = appInfo?.let { pm.getApplicationLabel(it).toString() } ?: sbn.packageName,
                    title = sbn.notification.extras.getCharSequence("android.title")?.toString() ?: "",
                    text = sbn.notification.extras.getCharSequence("android.text")?.toString() ?: "",
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
        state.wallpaper = runCatching {
            WallpaperManager.getInstance(service).drawable?.let { drawableToBitmap(it, 0).asImageBitmap() }
        }.getOrNull()
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
