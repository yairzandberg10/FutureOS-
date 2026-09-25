package com.android.sistemui.controlcenter.logic

import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.PowerManager
import android.database.ContentObserver
import android.location.LocationManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.future.sharednav.root.RootShell
import androidx.compose.runtime.getValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import android.graphics.Bitmap
import com.android.sistemui.controlcenter.service.MediaControlService

class ControlManager(private val context: Context) {

    private val audioManager by lazy { try { context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager } catch(t: Throwable) { null } }
    private val cameraManager by lazy { try { context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager } catch(t: Throwable) { null } }
    private val notificationManager by lazy { try { context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager } catch(t: Throwable) { null } }
    private val bluetoothAdapter by lazy { try { (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter } catch(t: Throwable) { null } }
    private val powerManager by lazy { try { context.getSystemService(Context.POWER_SERVICE) as? PowerManager } catch(t: Throwable) { null } }
    private val locationManager by lazy { try { context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager } catch(t: Throwable) { null } }
    private val connectivityManager by lazy { try { context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager } catch(t: Throwable) { null } }
    private val wifiManager by lazy { try { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager } catch(t: Throwable) { null } }

    var volumeLevel by mutableFloatStateOf(0.5f)
    var brightnessLevel by mutableFloatStateOf(0.5f)
    var isFlashlightOn by mutableStateOf(false)
    var isDndOn by mutableStateOf(false)
    var isRotateOn by mutableStateOf(false)
    var isWifiOn by mutableStateOf(false)
    var isBluetoothOn by mutableStateOf(false)
    var isBluetoothDeviceConnected by mutableStateOf(false)
    var isAirplaneOn by mutableStateOf(false)
    var isLocationOn by mutableStateOf(false)
    var isDataOn by mutableStateOf(false)
    var isNightModeOn by mutableStateOf(false)
    var isBatterySaverOn by mutableStateOf(false)
    var isPredictiveTextOn by mutableStateOf(true)

    // Media States
    var isPlaying by mutableStateOf(false)
    var hasActiveMedia by mutableStateOf(false)
    var currentSongTitle by mutableStateOf("אין מוזיקה מנגנת")
    var currentArtist by mutableStateOf("")
    var currentAlbumArt by mutableStateOf<Bitmap?>(null)
    var mediaPosition by mutableLongStateOf(0L)
    var mediaDuration by mutableLongStateOf(0L)
    var isMediaServiceEnabled by mutableStateOf(false)

    private var cameraId: String? = null
    private var activeController: MediaController? = null
    
    private val scope = CoroutineScope(Dispatchers.IO)

    // מוגדרים כאן, לפני init, ולא ליד updateStates שמשתמשת בהם: init קורא ל-
    // updateStates, ו-Kotlin מאתחל שדות לפי סדר הופעתם בקובץ - שדה שמוגדר אחרי
    // init עדיין null כשהוא רץ. אותו דבר ל-lazy: אובייקט ה-delegate עצמו
    // נוצר במקומו בקובץ, וה-thread ברקע ניגש אליו מיד.
    private val mainScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var refreshJob: Job? = null
    private var refreshQueued = false
    private var mediaRefreshJob: Job? = null

    // ה-Method של ה-reflection נמצא פעם אחת ונשמר. קודם getDeclaredMethod רץ
    // מחדש בכל ריענון (כל 500ms במרכז הבקרה), וכשהמתודה חסומה ב-hidden API
    // של אנדרואיד 12 כל ניסיון כזה גם בנה NoSuchMethodException עם stack trace.
    // null פירושו "חיפשנו ואין" - לא מחפשים שוב.
    private val mobileDataMethod: java.lang.reflect.Method? by lazy {
        try {
            connectivityManager?.javaClass?.getDeclaredMethod("getMobileDataEnabled")?.apply { isAccessible = true }
        } catch (t: Throwable) {
            null
        }
    }

    private val bluetoothIsConnectedMethod: java.lang.reflect.Method? by lazy {
        try {
            android.bluetooth.BluetoothDevice::class.java.getMethod("isConnected")
        } catch (t: Throwable) {
            null
        }
    }

    /**
     * מריץ פקודת root ומחזיר אם היא באמת הצליחה.
     *
     * קודם לכן היה כאן shell מתמשך שאליו רק כתבנו: בלי להמתין, בלי
     * לבדוק exit code, ובלי לקרוא את stdout/stderr כלל - כך שכל מתג
     * הראה "פועל" גם כשהפקודה נכשלה, והצינור יכול היה להתמלא
     * ולתקוע את התהליך. עכשיו זה מימוש אחד משותף (RootShell),
     * שגם Settings משתמש בו.
     *
     * חוסם - להריץ מ-Dispatchers.IO, לא מה-main thread.
     */
    fun runRootCommand(command: String): Boolean = RootShell.run(command).success

    /** גרסה לא-חוסמת לקוראים שרצים על ה-main thread (שירותי הנגישות). */
    fun runRootCommandAsync(command: String) {
        scope.launch { RootShell.run(command) }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (this@ControlManager.cameraId == cameraId) {
                isFlashlightOn = enabled
            }
        }
    }

    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            updateStates()
        }
    }

    private val callback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            isPlaying = state?.state == PlaybackState.STATE_PLAYING
            mediaPosition = state?.position ?: 0L
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            currentSongTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "שיר לא ידוע"
            currentArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "אמן לא ידוע"
            currentAlbumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            mediaDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        }
    }

    init {
        try {
            updateStates()
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
            cameraManager?.registerTorchCallback(torchCallback, Handler(Looper.getMainLooper()))
            
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
                false,
                settingsObserver
            )
            context.contentResolver.registerContentObserver(
                Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION),
                false,
                settingsObserver
            )
        } catch (t: Throwable) {
            Log.e("ControlManager", "Error in init", t)
        }
    }

    /**
     * משחררת את ה-ContentObserver וה-TorchCallback שנרשמו ב-init - בלי קריאה
     * מפורשת לזה, כל שירות accessibility שיוצר ControlManager (ואף אחד מהם לא
     * חי לתמיד) משאיר אחריו listener רשום לצמיתות אצל ה-ContentResolver/CameraManager,
     * שממשיך להחזיק הפניה ל-Context של השירות שכבר נהרס (זליגת זיכרון קלאסית).
     * יש לקרוא לזה מתוך onDestroy()/onInterrupt() של השירות שמחזיק את המופע הזה.
     */
    fun dispose() {
        mainScope.cancel()
        try {
            context.contentResolver.unregisterContentObserver(settingsObserver)
        } catch (t: Throwable) {
            Log.e("ControlManager", "Error unregistering settingsObserver", t)
        }
        try {
            cameraManager?.unregisterTorchCallback(torchCallback)
        } catch (t: Throwable) {
            Log.e("ControlManager", "Error unregistering torchCallback", t)
        }
    }

    fun handleControlToggle(id: String) {
        // עדכון אופטימי - חוץ מ-"dnd", שדורש הרשאת מדיניות התראות ועלול להיכשל
        // בשקט; אם מעדכנים אותו כאן מראש בלי קשר לתוצאה האמיתית, הכפתור
        // "משתיק" נראה כאילו הוא עבד גם כשאנדרואיד חסם את השינוי בפועל.
        when (id) {
            "flashlight" -> isFlashlightOn = !isFlashlightOn
            "rotation" -> isRotateOn = !isRotateOn
            "airplane" -> isAirplaneOn = !isAirplaneOn
            "wifi" -> isWifiOn = !isWifiOn
            "bluetooth" -> isBluetoothOn = !isBluetoothOn
            "location" -> isLocationOn = !isLocationOn
            "data" -> isDataOn = !isDataOn
            "night" -> isNightModeOn = !isNightModeOn
            "battery" -> isBatterySaverOn = !isBatterySaverOn
            "predictive_text" -> isPredictiveTextOn = !isPredictiveTextOn
        }

        when (id) {
            "flashlight" -> toggleFlashlight()
            "dnd" -> toggleDnd()
            "rotation" -> toggleRotation()
            "airplane" -> toggleAirplane()
            "wifi" -> toggleWifi()
            "bluetooth" -> toggleBluetooth()
            "settings" -> openMainSettings()
            "data" -> toggleData()
            "location" -> toggleLocation()
            "battery" -> openBatterySettings()
            "night" -> toggleNightMode()
            "camera" -> openCamera()
            "search" -> openSearch()
            "music" -> openMusic()
            "account" -> openAccount()
            "calendar" -> openCalendar()
            "security" -> openSecurity()
            "predictive_text" -> togglePredictiveText()
        }
        // Poll state after a short delay to sync with actual system result
        Handler(Looper.getMainLooper()).postDelayed({ updateStates() }, 800)
    }

    fun getControlState(id: String): Boolean {
        return when (id) {
            "flashlight" -> isFlashlightOn
            "dnd" -> isDndOn
            "rotation" -> isRotateOn
            "wifi" -> isWifiOn
            "bluetooth" -> isBluetoothOn
            "airplane" -> isAirplaneOn
            "location" -> isLocationOn
            "data" -> isDataOn
            "night" -> isNightModeOn
            "battery" -> isBatterySaverOn
            "predictive_text" -> isPredictiveTextOn
            else -> false
        }
    }

    /**
     * מרכז הבקרה קורא לזה כל 500ms. החיפוש עצמו (הגדרת Secure וקריאת binder
     * ל-MediaSessionManager) רץ ברקע, כמו ב-[updateStates]; רק ההחלפה של
     * ה-controller חוזרת ל-main thread, כי registerCallback בלי Handler נרשם
     * על ה-Looper של ה-thread שקורא לו.
     */
    fun updateMediaController() {
        if (mediaRefreshJob?.isActive == true) return
        mediaRefreshJob = mainScope.launch {
            val (enabled, controllers) = withContext(Dispatchers.IO) {
                val enabled = MediaControlService.isEnabled(context)
                enabled to if (enabled) MediaControlService.getActiveControllers(context) else emptyList()
            }
            applyMediaController(enabled, controllers.firstOrNull())
        }
    }

    private fun applyMediaController(enabled: Boolean, newController: MediaController?) {
        isMediaServiceEnabled = enabled
        if (!isMediaServiceEnabled) return

        hasActiveMedia = newController != null

        if (newController?.packageName != activeController?.packageName) {
            activeController?.unregisterCallback(callback)
            activeController = newController
            activeController?.registerCallback(callback)
            
            // Sync initial state
            isPlaying = activeController?.playbackState?.state == PlaybackState.STATE_PLAYING
            mediaPosition = activeController?.playbackState?.position ?: 0L
            val metadata = activeController?.metadata
            currentSongTitle = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: "אין מוזיקה מנגנת"
            currentArtist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
            currentAlbumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            mediaDuration = metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        }
    }

    fun updateProgress() {
        activeController?.let { controller ->
            if (isPlaying) {
                mediaPosition = controller.playbackState?.position ?: 0L
            }
        }
    }

    fun seekTo(position: Long) {
        activeController?.transportControls?.seekTo(position)
    }

    fun togglePlayPause() {
        if (isPlaying) {
            activeController?.transportControls?.pause()
        } else {
            activeController?.transportControls?.play()
        }
    }

    fun nextTrack() {
        activeController?.transportControls?.skipToNext()
    }

    fun previousTrack() {
        activeController?.transportControls?.skipToPrevious()
    }

    fun openNotificationAccessSettings() {
        val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun getCurrentVolume(): Float {
        return try {
            val audio = audioManager ?: return 0.5f
            val current = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            current.toFloat() / max
        } catch(t: Throwable) { 0.5f }
    }

    fun setVolume(fraction: Float) {
        try {
            val audio = audioManager ?: return
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val target = kotlin.math.round(fraction * max).toInt().coerceIn(0, max)
            audio.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
            volumeLevel = fraction
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "setVolume failed", t)
        }
    }

    private fun getCurrentBrightness(): Float {
        return try {
            val brightness = Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            brightness.toFloat() / 255f
        } catch (t: Throwable) {
            0.5f
        }
    }

    fun setBrightness(fraction: Float) {
        try {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            val target = (fraction * 255).toInt()
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, target)
            brightnessLevel = fraction
        } catch (t: Throwable) {
            Log.e("ControlManager", "Error setting brightness", t)
        }
    }

    fun toggleFlashlight() {
        try {
            val camera = cameraManager ?: return
            val id = cameraId ?: return
            camera.setTorchMode(id, isFlashlightOn)
        } catch (t: Throwable) {
            Log.e("ControlManager", "Error toggling flashlight", t)
        }
    }

    fun toggleDnd() {
        try {
            val nm = notificationManager ?: return
            if (!nm.isNotificationPolicyAccessGranted) {
                val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            val newFilter = if (isDndOn) NotificationManager.INTERRUPTION_FILTER_ALL else NotificationManager.INTERRUPTION_FILTER_PRIORITY
            nm.setInterruptionFilter(newFilter)
            isDndOn = !isDndOn
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "toggleDnd failed", t)
        }
    }

    fun togglePredictiveText() {
        try {
            com.future.sharednav.keyboard.KeyboardSettingsClient.setPredictiveEnabled(context, isPredictiveTextOn)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "togglePredictiveText failed", t)
        }
    }

    fun toggleRotation() {
        try {
            if (!Settings.System.canWrite(context)) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            }
            val newValue = if (isRotateOn) 1 else 0
            Settings.System.putInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, newValue)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "toggleRotation failed", t)
        }
    }

    /** מ-API 29 ואילך WifiManager.setWifiEnabled() חסום לאפליקציות רגילות ומחזיר
     * false בשקט - בדיוק כמו bluetooth/data, נופלים ל-root ואז למסך ההגדרות. */
    fun toggleWifi() = scope.launch {
        val newState = isWifiOn
        if (!runRootCommand("svc wifi ${if (newState) "enable" else "disable"}")) {
            try {
                @Suppress("DEPRECATION")
                if (wifiManager?.setWifiEnabled(newState) != true) openWifiSettings()
            } catch (t: Throwable) {
                openWifiSettings()
            }
        }
    }

    private fun openWifiSettings() {
        try {
            val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openWifiSettings failed", t)
        }
    }

    fun toggleBluetooth() = scope.launch {
        val newState = isBluetoothOn
        if (!runRootCommand("svc bluetooth ${if (newState) "enable" else "disable"}")) {
            try {
                val adapter = bluetoothAdapter ?: return@launch
                @Suppress("DEPRECATION")
                if (newState) {
                    adapter.enable()
                } else {
                    adapter.disable()
                }
            } catch(t: Throwable) {
                openBluetoothSettings()
            }
        }
    }

    fun toggleData() = scope.launch {
        val newState = isDataOn
        if (!runRootCommand("svc data ${if (newState) "enable" else "disable"}")) {
            openDataSettings()
        }
    }

    fun toggleLocation() = scope.launch {
        val newState = isLocationOn
        if (!runRootCommand("cmd location set-location-enabled ${if (newState) "true" else "false"}")) {
            try {
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (t: Throwable) {
                android.util.Log.w("ControlManager", "toggleLocation failed", t)
            }
        }
    }

    fun toggleAirplane() = scope.launch {
        val newState = isAirplaneOn
        val value = if (newState) 1 else 0
        val rootOk = runRootCommand("settings put global airplane_mode_on $value") &&
            runRootCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state ${if (newState) "true" else "false"}")
        if (!rootOk) {
            try {
                val intent = Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (t: Throwable) {
                android.util.Log.w("ControlManager", "toggleAirplane failed", t)
            }
        }
    }

    /** ללא רוט אין API ציבורי להפעיל/לכבות נתונים סלולריים ישירות (מוגבל מ-API 26)
     * - פותחים את הפאנל המהיר של המערכת (זמין מ-API 29) או מסך ההגדרות כנפילה חזרה,
     * כדי שהכפתור תמיד יוביל לפעולה אמיתית במקום להיראות "תקוע". */
    private fun openDataSettings() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val intent = Intent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val intent = Intent(Settings.ACTION_DATA_ROAMING_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (t: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (t2: Throwable) {
                android.util.Log.w("ControlManager", "openDataSettings failed", t2)
            }
        }
    }

    fun openBluetoothSettings() {
        try {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openBluetoothSettings failed", t)
        }
    }

    /** Intent סתמי (android.settings.SETTINGS) משאיר לרזולוור של אנדרואיד לבחור
     * מי מטפל בו - לא אמין כשיש כמה מועמדים על המכשיר. פותחים ישירות את
     * אפליקציית ההגדרות של FutureOS לפי שם החבילה/מחלקה המפורש שלה, עם נפילה
     * חזרה ל-Intent הסתמי רק אם היא לא מותקנת בכלל. */
    fun openMainSettings() {
        try {
            val intent = Intent().setClassName("com.future.settings", "com.future.settings.MainActivity")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } catch (t2: Throwable) {
                android.util.Log.w("ControlManager", "openMainSettings failed", t2)
            }
        }
    }

    fun toggleNightMode() {
        try {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "toggleNightMode failed", t)
        }
    }

    fun openBatterySettings() {
        try {
            val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openBatterySettings failed", t)
        }
    }

    fun openCamera() {
        try {
            val intent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openCamera failed", t)
        }
    }

    fun openSearch() {
        try {
            val intent = Intent(Intent.ACTION_WEB_SEARCH)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openSearch failed", t)
        }
    }

    fun openMusic() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_MUSIC)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openMusic failed", t)
        }
    }

    fun openAccount() {
        try {
            val intent = Intent(Settings.ACTION_SYNC_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openAccount failed", t)
        }
    }

    fun openCalendar() {
        try {
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_APP_CALENDAR)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openCalendar failed", t)
        }
    }

    fun openSecurity() {
        try {
            val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (t: Throwable) {
            android.util.Log.w("ControlManager", "openSecurity failed", t)
        }
    }

    /**
     * מרענן את כל מצבי המערכת. לא חוסם: הקריאות עצמן רצות ב-thread רקע,
     * ורק ההשמה לשדות חוזרת ל-main thread.
     *
     * קודם כל הקריאה הזו רצה על ה-main thread - בערך תריסר קריאות binder
     * (אודיו, התראות, Bluetooth, Wi-Fi, מיקום, חשמל), שתי קריאות reflection,
     * ושאילתת ContentProvider לתהליך של המקלדת. מרכז הבקרה קרא לה כל 500ms
     * כל עוד הוא פתוח, ושורת המצב כל 15 שניות תמיד. ה-main thread של FutureUI
     * הוא גם זה שעונה למסנן המקשים של שירותי הנגישות, כך שכל ריענון כזה
     * עיכב את לחיצת המקש הבאה בכל הטלפון, והפוקוס במרכז הבקרה קפץ בגמגום.
     *
     * קריאות שמגיעות בזמן שריענון כבר רץ לא פותחות ריענון מקביל - הן רק
     * מסמנות שצריך עוד סבב אחד כשהנוכחי מסתיים, כדי שהתוצאה האחרונה תמיד
     * תשקף את המצב אחרי הקריאה האחרונה.
     *
     * נקראת רק מה-main thread (LaunchedEffect, ה-ContentObserver שרשום על
     * ה-main looper, ו-init מתוך onCreate של השירות) - לכן refreshJob ו-
     * refreshQueued לא צריכים סנכרון.
     */
    fun updateStates() {
        if (refreshJob?.isActive == true) {
            refreshQueued = true
            return
        }
        refreshJob = mainScope.launch {
            do {
                refreshQueued = false
                val states = withContext(Dispatchers.IO) { readSystemStates() }
                applySystemStates(states)
            } while (refreshQueued)
        }
    }

    /** צילום של כל מצבי המערכת - נבנה ב-thread רקע ומוחל בבת אחת על ה-main thread. */
    private class SystemStates(
        val volume: Float,
        val brightness: Float,
        val dnd: Boolean,
        val rotate: Boolean,
        val predictiveText: Boolean,
        val bluetooth: Boolean,
        val wifi: Boolean,
        val airplane: Boolean,
        val location: Boolean,
        val data: Boolean,
        val nightMode: Boolean,
        val batterySaver: Boolean,
        val bluetoothDeviceConnected: Boolean,
    )

    private fun readSystemStates(): SystemStates {
        val bluetooth = try { bluetoothAdapter?.isEnabled ?: false } catch (t: Throwable) { false }
        return SystemStates(
            volume = getCurrentVolume(),
            brightness = getCurrentBrightness(),
            dnd = try {
                notificationManager?.currentInterruptionFilter?.let { it != NotificationManager.INTERRUPTION_FILTER_ALL } ?: false
            } catch (t: Throwable) { false },
            rotate = try { Settings.System.getInt(context.contentResolver, Settings.System.ACCELEROMETER_ROTATION, 0) == 1 } catch (t: Throwable) { false },
            predictiveText = try { com.future.sharednav.keyboard.KeyboardSettingsClient.isPredictiveEnabled(context) } catch (t: Throwable) { true },
            bluetooth = bluetooth,
            wifi = try { wifiManager?.isWifiEnabled ?: false } catch (t: Throwable) { false },
            airplane = try { Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0 } catch (t: Throwable) { false },
            location = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    locationManager?.isLocationEnabled ?: false
                } else {
                    @Suppress("DEPRECATION")
                    val mode = Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, Settings.Secure.LOCATION_MODE_OFF)
                    mode != Settings.Secure.LOCATION_MODE_OFF
                }
            } catch (t: Throwable) { false },
            data = isMobileDataEnabled(),
            nightMode = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES,
            batterySaver = try { powerManager?.isPowerSaveMode ?: false } catch (t: Throwable) { false },
            bluetoothDeviceConnected = bluetooth && isAnyBluetoothDeviceConnected(),
        )
    }

    private fun applySystemStates(states: SystemStates) {
        volumeLevel = states.volume
        brightnessLevel = states.brightness
        isDndOn = states.dnd
        isRotateOn = states.rotate
        isPredictiveTextOn = states.predictiveText
        isBluetoothOn = states.bluetooth
        isWifiOn = states.wifi
        isAirplaneOn = states.airplane
        isLocationOn = states.location
        isDataOn = states.data
        isNightModeOn = states.nightMode
        isBatterySaverOn = states.batterySaver
        isBluetoothDeviceConnected = states.bluetoothDeviceConnected
    }

    private fun isMobileDataEnabled(): Boolean = try {
        mobileDataMethod?.invoke(connectivityManager) as? Boolean ?: false
    } catch (t: Throwable) {
        false
    }

    /** בודק אם יש מכשיר Bluetooth מחובר בפועל, לא רק מקושר (paired) - אין API
     * ציבורי ישיר לזה, אז משתמשים ב-BluetoothDevice.isConnected() המוסתרת
     * אבל יציבה, בדיוק כמו שהמערכת עצמה עושה בשורת המצב האמיתית שלה. */
    private fun isAnyBluetoothDeviceConnected(): Boolean {
        val method = bluetoothIsConnectedMethod ?: return false
        return try {
            bluetoothAdapter?.bondedDevices?.any { device ->
                method.invoke(device) as? Boolean ?: false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
