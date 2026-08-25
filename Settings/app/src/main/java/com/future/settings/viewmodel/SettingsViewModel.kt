package com.future.settings.viewmodel

import android.app.Application
import android.content.Context
import android.media.AudioManager
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.AndroidViewModel
import com.future.settings.theme.ThemeClient
import com.future.settings.ui.theme.ThemeConfig
import com.future.settings.utils.SystemInteractor

import android.widget.Toast
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val prefs = application.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
    private val systemInteractor = SystemInteractor(application)

    private val _themeConfig = mutableStateOf(loadThemeConfig())
    val themeConfig: State<ThemeConfig> = _themeConfig

    private val _isReady = mutableStateOf(true) // Start ready to avoid layout flicker
    val isReady: State<Boolean> = _isReady

    // Observables for system settings
    private val _ringerMode = mutableStateOf(AudioManager.RINGER_MODE_NORMAL)
    val ringerMode: State<Int> = _ringerMode

    private val _brightness = mutableStateOf(0.5f)
    val brightness: State<Float> = _brightness

    private val _mediaVolume = mutableStateOf(0.5f)
    val mediaVolume: State<Float> = _mediaVolume

    private val _systemVolume = mutableStateOf(0.5f)
    val systemVolume: State<Float> = _systemVolume

    private val _airplaneMode = mutableStateOf(false)
    val airplaneMode: State<Boolean> = _airplaneMode

    private val _mobileData = mutableStateOf(false)
    val mobileData: State<Boolean> = _mobileData

    private val _bluetoothEnabled = mutableStateOf(false)
    val bluetoothEnabled: State<Boolean> = _bluetoothEnabled

    private val _batteryPercent = mutableStateOf(100)
    val batteryPercent: State<Int> = _batteryPercent

    private val _isCharging = mutableStateOf(false)
    val isCharging: State<Boolean> = _isCharging

    private val _batterySaver = mutableStateOf(false)
    val batterySaver: State<Boolean> = _batterySaver

    private val _soundEffects = mutableStateOf(true)
    val soundEffects: State<Boolean> = _soundEffects

    private val _hapticFeedback = mutableStateOf(true)
    val hapticFeedback: State<Boolean> = _hapticFeedback

    private val _screenTimeout = mutableStateOf(30000)
    val screenTimeout: State<Int> = _screenTimeout

    private val _use24Hour = mutableStateOf(true)
    val use24Hour: State<Boolean> = _use24Hour

    private val _autoTime = mutableStateOf(true)
    val autoTime: State<Boolean> = _autoTime

    private val _notificationVolume = mutableStateOf(0.5f)
    val notificationVolume: State<Float> = _notificationVolume

    private val _alarmVolume = mutableStateOf(0.5f)
    val alarmVolume: State<Float> = _alarmVolume

    private val _callVolume = mutableStateOf(0.5f)
    val callVolume: State<Float> = _callVolume

    private val _dialPadTones = mutableStateOf(true)
    val dialPadTones: State<Boolean> = _dialPadTones

    private val _lockSound = mutableStateOf(true)
    val lockSound: State<Boolean> = _lockSound

    private val _chargingSound = mutableStateOf(true)
    val chargingSound: State<Boolean> = _chargingSound

    private val _vibrateWhenRinging = mutableStateOf(false)
    val vibrateWhenRinging: State<Boolean> = _vibrateWhenRinging

    private val _adaptiveBrightness = mutableStateOf(false)
    val adaptiveBrightness: State<Boolean> = _adaptiveBrightness

    private val _animationsReduced = mutableStateOf(false)
    val animationsReduced: State<Boolean> = _animationsReduced

    private val _locationEnabled = mutableStateOf(false)
    val locationEnabled: State<Boolean> = _locationEnabled

    private val _dataRoaming = mutableStateOf(false)
    val dataRoaming: State<Boolean> = _dataRoaming

    private val _colorInversion = mutableStateOf(false)
    val colorInversion: State<Boolean> = _colorInversion

    private val _autoTimeZone = mutableStateOf(true)
    val autoTimeZone: State<Boolean> = _autoTimeZone

    private val _batteryDetails = mutableStateOf(SystemInteractor.BatteryDetails(0f, 0f, "לא ידועה"))
    val batteryDetails: State<SystemInteractor.BatteryDetails> = _batteryDetails

    private val _lowBatteryWarningLevel = mutableStateOf(15)
    val lowBatteryWarningLevel: State<Int> = _lowBatteryWarningLevel

    private val _ramInfo = mutableStateOf(SystemInteractor.RamInfo(0L, 1L, false))
    val ramInfo: State<SystemInteractor.RamInfo> = _ramInfo

    private val _screenTimeSummary = mutableStateOf(SystemInteractor.ScreenTimeSummary(0L, emptyList()))
    val screenTimeSummary: State<SystemInteractor.ScreenTimeSummary> = _screenTimeSummary

    private val _defaultSmsPackage = mutableStateOf<String?>(null)
    val defaultSmsPackage: State<String?> = _defaultSmsPackage

    private val _defaultDialerPackage = mutableStateOf<String?>(null)
    val defaultDialerPackage: State<String?> = _defaultDialerPackage

    private val _nfcEnabled = mutableStateOf(false)
    val nfcEnabled: State<Boolean> = _nfcEnabled

    private val _stayAwakeWhileCharging = mutableStateOf(false)
    val stayAwakeWhileCharging: State<Boolean> = _stayAwakeWhileCharging

    private val _sleepModeEnabled = mutableStateOf(false)
    val sleepModeEnabled: State<Boolean> = _sleepModeEnabled

    private val _developerModeEnabled = mutableStateOf(false)
    val developerModeEnabled: State<Boolean> = _developerModeEnabled

    private val _isOptimizing = mutableStateOf(false)
    val isOptimizing: State<Boolean> = _isOptimizing

    private val _isFreeingSpace = mutableStateOf(false)
    val isFreeingSpace: State<Boolean> = _isFreeingSpace

    private val _densityDpi = mutableStateOf(systemInteractor.getDisplayDensityDpi())
    val densityDpi: State<Int> = _densityDpi
    val defaultDensityDpi: Int = systemInteractor.getDefaultDisplayDensityDpi()

    private val _multiWindowForced = mutableStateOf(false)
    val multiWindowForced: State<Boolean> = _multiWindowForced

    private val _simCards = mutableStateOf<List<SystemInteractor.SimEntry>>(emptyList())
    val simCards: State<List<SystemInteractor.SimEntry>> = _simCards

    private val _defaultDataSubId = mutableStateOf(-1)
    val defaultDataSubId: State<Int> = _defaultDataSubId
    private val _defaultVoiceSubId = mutableStateOf(-1)
    val defaultVoiceSubId: State<Int> = _defaultVoiceSubId
    private val _defaultSmsSubId = mutableStateOf(-1)
    val defaultSmsSubId: State<Int> = _defaultSmsSubId

    private val _imei = mutableStateOf<String?>(null)
    val imei: State<String?> = _imei
    private val _phoneNumber = mutableStateOf<String?>(null)
    val phoneNumber: State<String?> = _phoneNumber

    private val _batteryExtended = mutableStateOf(SystemInteractor.BatteryExtendedInfo(null, null, null))
    val batteryExtended: State<SystemInteractor.BatteryExtendedInfo> = _batteryExtended

    private val _appTimers = mutableStateOf<List<SystemInteractor.AppTimer>>(emptyList())
    val appTimers: State<List<SystemInteractor.AppTimer>> = _appTimers

    private val _bluetoothDeviceName = mutableStateOf("")
    val bluetoothDeviceName: State<String> = _bluetoothDeviceName

    private val _pairedBluetoothDevices = mutableStateOf<List<SystemInteractor.BluetoothDeviceEntry>>(emptyList())
    val pairedBluetoothDevices: State<List<SystemInteractor.BluetoothDeviceEntry>> = _pairedBluetoothDevices

    private val _isApplyingWallpaper = mutableStateOf(false)
    val isApplyingWallpaper: State<Boolean> = _isApplyingWallpaper

    private val _emergencyContact = mutableStateOf<Pair<String, String>?>(null)
    val emergencyContact: State<Pair<String, String>?> = _emergencyContact
    private val _sosSending = mutableStateOf(false)
    val sosSending: State<Boolean> = _sosSending

    // --- Easter eggs ---

    /** "מצב חגיגה" הוא זמני בכוונה - 5 דקות מכל הפעלה/הפעלה-מחדש של הקוד הסודי,
     *  לא נעילה קבועה. לא נשמר ב-SharedPreferences בכוונה: זו חגיגה חד-פעמית לכל
     *  הפעלה של האפליקציה, לא הגדרה קבועה. 0 = לא פעיל כרגע. */
    private val _partyModeExpiresAt = mutableStateOf(0L)
    val partyModeExpiresAt: State<Long> = _partyModeExpiresAt

    private val _t9HighScore = mutableStateOf(prefs.getInt("t9_high_score", 0))
    val t9HighScore: State<Int> = _t9HighScore

    private val _cyclingAccentEnabled = mutableStateOf(false)
    val cyclingAccentEnabled: State<Boolean> = _cyclingAccentEnabled

    private var cyclingAccentJob: kotlinx.coroutines.Job? = null

    companion object {
        const val PARTY_MODE_DURATION_MS = 5 * 60_000L
    }

    init {
        // Sync in background after initial render
        viewModelScope.launch(Dispatchers.IO) {
            syncWithSystem()
        }
    }

    // מקור האמת לעיצוב (כהה/בהיר, צבע הדגשה) הוא ThemeProvider של FutureUI -
    // משותף בין כל אפליקציות FutureOS, לא רק תוך-אפליקציה. גודל הטקסט נשאר
    // מקומי כי הוא לא רלוונטי לאפליקציות אחרות.
    private fun loadThemeConfig(): ThemeConfig {
        val shared = ThemeClient.getTheme(app)
        return ThemeConfig(
            primaryColor = Color(shared.primaryColor),
            isDarkMode = shared.isDarkMode,
            fontSizeMultiplier = prefs.getFloat("font_size_multiplier", 1.0f)
        )
    }

    fun toggleDarkMode() {
        val newValue = !_themeConfig.value.isDarkMode
        _themeConfig.value = _themeConfig.value.copy(isDarkMode = newValue)
        ThemeClient.setDarkMode(app, newValue)
    }

    fun syncWithSystem() {
        val rMode = systemInteractor.getRingerMode()
        val bright = systemInteractor.getBrightness() / 255f
        val air = systemInteractor.isAirplaneModeOn()
        val data = systemInteractor.isMobileDataEnabled()
        val bt = systemInteractor.isBluetoothEnabled()
        val battery = systemInteractor.getBatteryStatus()
        val saver = systemInteractor.isBatterySaverOn()
        val mediaMax = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val mediaVol = systemInteractor.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / mediaMax
        val systemMax = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_SYSTEM).coerceAtLeast(1)
        val systemVol = systemInteractor.getStreamVolume(AudioManager.STREAM_SYSTEM).toFloat() / systemMax
        val soundFx = systemInteractor.isSoundEffectsEnabled()
        val haptic = systemInteractor.isHapticFeedbackEnabled()
        val timeout = systemInteractor.getScreenTimeout()
        val format24 = systemInteractor.is24HourFormat()
        val autoTimeOn = systemInteractor.isAutoTimeEnabled()
        val notifMax = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
        val notifVol = systemInteractor.getStreamVolume(AudioManager.STREAM_NOTIFICATION).toFloat() / notifMax
        val alarmMax = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val alarmVol = systemInteractor.getStreamVolume(AudioManager.STREAM_ALARM).toFloat() / alarmMax
        val callMax = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)
        val callVol = systemInteractor.getStreamVolume(AudioManager.STREAM_VOICE_CALL).toFloat() / callMax
        val dialTones = systemInteractor.isDialPadTonesEnabled()
        val lockSnd = systemInteractor.isLockSoundEnabled()
        val chargeSnd = systemInteractor.isChargingSoundEnabled()
        val vibrateRing = systemInteractor.isVibrateWhenRingingEnabled()
        val adaptiveBright = systemInteractor.isAdaptiveBrightnessEnabled()
        val animsReduced = systemInteractor.areAnimationsReduced()
        val location = systemInteractor.isLocationEnabled()
        val roaming = systemInteractor.isDataRoamingEnabled()
        val inversion = systemInteractor.isColorInversionEnabled()
        val autoTz = systemInteractor.isAutoTimeZoneEnabled()
        val battDetails = systemInteractor.getBatteryDetails()
        val lowBattLevel = systemInteractor.getLowBatteryWarningLevel()
        val nfcOn = systemInteractor.isNfcEnabled()
        val stayAwake = systemInteractor.isStayAwakeWhileChargingEnabled()
        val sleepMode = systemInteractor.isSleepModeEnabled()
        val devMode = systemInteractor.isDeveloperModeEnabled()
        val multiWindow = systemInteractor.isMultiWindowForced()

        viewModelScope.launch(Dispatchers.Main) {
            _ringerMode.value = rMode
            _brightness.value = bright
            _airplaneMode.value = air
            _mobileData.value = data
            _bluetoothEnabled.value = bt
            _batteryPercent.value = battery.first
            _isCharging.value = battery.second
            _batterySaver.value = saver
            _mediaVolume.value = mediaVol
            _systemVolume.value = systemVol
            _soundEffects.value = soundFx
            _hapticFeedback.value = haptic
            _screenTimeout.value = timeout
            _use24Hour.value = format24
            _autoTime.value = autoTimeOn
            _notificationVolume.value = notifVol
            _alarmVolume.value = alarmVol
            _callVolume.value = callVol
            _dialPadTones.value = dialTones
            _lockSound.value = lockSnd
            _chargingSound.value = chargeSnd
            _vibrateWhenRinging.value = vibrateRing
            _adaptiveBrightness.value = adaptiveBright
            _animationsReduced.value = animsReduced
            _locationEnabled.value = location
            _dataRoaming.value = roaming
            _colorInversion.value = inversion
            _autoTimeZone.value = autoTz
            _batteryDetails.value = battDetails
            _lowBatteryWarningLevel.value = lowBattLevel
            _nfcEnabled.value = nfcOn
            _stayAwakeWhileCharging.value = stayAwake
            _sleepModeEnabled.value = sleepMode
            _developerModeEnabled.value = devMode
            _multiWindowForced.value = multiWindow
        }
    }

    /** עוזר משותף לכל מתגי ה-root: מריץ את הפקודה ברקע (Dispatchers.IO, כדי לא לחסום
     *  את ה-UI thread - זה בדיוק דפוס ה-ANR שכבר תוקן ב-optimizeNow), ומעדכן את מצב
     *  ה-UI רק אם הפקודה באמת הצליחה. אם היא נכשלה (למשל אין הרשאת root), המתג נשאר
     *  במצבו הקודם והמשתמש מקבל הודעה, במקום שהמתג "ישקר" ויראה מופעל בלי סיבה. */
    private fun applyRootToggle(state: MutableState<Boolean>, setter: (Boolean) -> Boolean) {
        val newValue = !state.value
        viewModelScope.launch(Dispatchers.IO) {
            val success = setter(newValue)
            withContext(Dispatchers.Main) {
                if (success) {
                    state.value = newValue
                } else {
                    showToast("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root")
                }
            }
        }
    }

    fun toggleBatterySaver() = applyRootToggle(_batterySaver, systemInteractor::setBatterySaver)

    fun showToast(message: String) {
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show()
    }

    fun setRingerMode(mode: Int) {
        val modeStr = when(mode) {
            AudioManager.RINGER_MODE_NORMAL -> "צליל"
            AudioManager.RINGER_MODE_VIBRATE -> "רטט"
            else -> "השתק"
        }
        val succeeded = systemInteractor.setRingerMode(mode)
        if (succeeded) {
            _ringerMode.value = mode
            showToast("מצב שמע שונה ל: $modeStr")
        } else {
            showToast("צריך לאשר גישה למדיניות התראות כדי לעבור למצב $modeStr")
            systemInteractor.openNotificationPolicyAccessSettings()
        }
    }

    fun setVolume(stream: Int, fraction: Float) {
        val max = systemInteractor.getStreamMaxVolume(stream).coerceAtLeast(1)
        val level = (fraction * max).toInt()
        systemInteractor.setVolume(stream, level)
        val actualFraction = level.toFloat() / max
        if (stream == AudioManager.STREAM_MUSIC) _mediaVolume.value = actualFraction
        if (stream == AudioManager.STREAM_SYSTEM) _systemVolume.value = actualFraction
    }

    fun setBrightness(value: Float) {
        _brightness.value = value
        systemInteractor.setBrightness((value * 255).toInt())
    }
    
    fun toggleAirplaneMode() = applyRootToggle(_airplaneMode, systemInteractor::setAirplaneMode)

    fun toggleMobileData() = applyRootToggle(_mobileData, systemInteractor::setMobileData)

    fun toggleBluetooth() = applyRootToggle(_bluetoothEnabled, systemInteractor::setBluetoothEnabled)

    // --- Bluetooth ---

    fun loadBluetoothInfo() {
        _bluetoothDeviceName.value = systemInteractor.getBluetoothDeviceName()
        _pairedBluetoothDevices.value = systemInteractor.getPairedBluetoothDevicesList()
    }

    fun renameBluetoothDevice(name: String) {
        systemInteractor.setBluetoothDeviceName(name)
        _bluetoothDeviceName.value = systemInteractor.getBluetoothDeviceName()
    }

    fun forgetBluetoothDevice(address: String, deviceName: String) {
        val success = systemInteractor.forgetBluetoothDevice(address)
        _pairedBluetoothDevices.value = systemInteractor.getPairedBluetoothDevicesList()
        showToast(if (success) "$deviceName הוסר מהמכשירים המשויכים" else "לא ניתן היה להסיר את $deviceName")
    }

    // --- טפט ---

    fun applyWallpaperBitmap(bitmap: android.graphics.Bitmap) {
        _isApplyingWallpaper.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setWallpaperBitmap(bitmap)
            viewModelScope.launch(Dispatchers.Main) {
                _isApplyingWallpaper.value = false
                showToast(if (success) "הטפט הוחל" else "החלת הטפט נכשלה")
            }
        }
    }

    fun applyWallpaperFromUri(uri: android.net.Uri) {
        _isApplyingWallpaper.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setWallpaperFromUri(uri)
            viewModelScope.launch(Dispatchers.Main) {
                _isApplyingWallpaper.value = false
                showToast(if (success) "הטפט הוחל" else "לא ניתן היה להחיל את התמונה שנבחרה")
            }
        }
    }

    fun updatePrimaryColor(color: Color) {
        _themeConfig.value = _themeConfig.value.copy(primaryColor = color)
        ThemeClient.setPrimaryColor(app, color.toArgb())
    }

    fun updateFontSize(multiplier: Float) {
        _themeConfig.value = _themeConfig.value.copy(fontSizeMultiplier = multiplier)
        prefs.edit().putFloat("font_size_multiplier", multiplier).apply()
    }

    fun toggleSoundEffects() {
        val newValue = !_soundEffects.value
        _soundEffects.value = newValue
        systemInteractor.setSoundEffectsEnabled(newValue)
    }

    fun toggleHapticFeedback() {
        val newValue = !_hapticFeedback.value
        _hapticFeedback.value = newValue
        systemInteractor.setHapticFeedbackEnabled(newValue)
    }

    val screenTimeoutPresets = listOf(15000, 30000, 60000, 120000, 300000, 600000)

    fun screenTimeoutLabel(millis: Int): String = when {
        millis < 60000 -> "${millis / 1000} שניות"
        millis < 3600000 -> "${millis / 60000} דקות"
        else -> "${millis / 3600000} שעות"
    }

    fun cycleScreenTimeout() {
        val currentIndex = screenTimeoutPresets.indexOf(_screenTimeout.value).let { if (it < 0) 1 else it }
        val next = screenTimeoutPresets[(currentIndex + 1) % screenTimeoutPresets.size]
        _screenTimeout.value = next
        systemInteractor.setScreenTimeout(next)
    }

    fun toggle24Hour() = applyRootToggle(_use24Hour, systemInteractor::set24HourFormat)

    fun toggleAutoTime() = applyRootToggle(_autoTime, systemInteractor::setAutoTimeEnabled)

    fun setSystemLanguage(languageTag: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setSystemLocale(languageTag)
            withContext(Dispatchers.Main) {
                showToast(
                    if (success) "שפת המערכת עודכנה - ייתכן שיידרש רענון של חלק מהאפליקציות"
                    else "לא ניתן לשנות הגדרה זו - נדרשת הרשאת root"
                )
            }
        }
    }

    fun resetFutureOsSettings() {
        _themeConfig.value = _themeConfig.value.copy(primaryColor = Color.White, isDarkMode = true, fontSizeMultiplier = 1.0f)
        ThemeClient.setDarkMode(app, true)
        ThemeClient.setPrimaryColor(app, Color.White.toArgb())
        prefs.edit().putFloat("font_size_multiplier", 1.0f).apply()
        // מאפס גם את "חלון מרובה" כדי שאיפוס ההגדרות יספק דרך חזרה אמיתית למי שהפעיל
        // אותו ונתקע במסך מפוצל בלי מגע - ר' תיקון: המתג הזה חייב "דלת יציאה".
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setMultiWindowForced(false)
            withContext(Dispatchers.Main) {
                if (success) _multiWindowForced.value = false
                showToast("הגדרות FutureOS אופסו")
            }
        }
    }

    fun setNotificationVolume(fraction: Float) {
        val max = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION).coerceAtLeast(1)
        val level = (fraction * max).toInt()
        systemInteractor.setVolume(AudioManager.STREAM_NOTIFICATION, level)
        _notificationVolume.value = level.toFloat() / max
    }

    fun setAlarmVolume(fraction: Float) {
        val max = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_ALARM).coerceAtLeast(1)
        val level = (fraction * max).toInt()
        systemInteractor.setVolume(AudioManager.STREAM_ALARM, level)
        _alarmVolume.value = level.toFloat() / max
    }

    fun setCallVolume(fraction: Float) {
        val max = systemInteractor.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL).coerceAtLeast(1)
        val level = (fraction * max).toInt()
        systemInteractor.setVolume(AudioManager.STREAM_VOICE_CALL, level)
        _callVolume.value = level.toFloat() / max
    }

    fun toggleDialPadTones() {
        // מקומי בלבד (Settings.System ישיר, לא root) - נשאר סינכרוני.
        val newValue = !_dialPadTones.value
        _dialPadTones.value = newValue
        systemInteractor.setDialPadTonesEnabled(newValue)
    }

    fun toggleLockSound() = applyRootToggle(_lockSound, systemInteractor::setLockSoundEnabled)

    fun toggleChargingSound() = applyRootToggle(_chargingSound, systemInteractor::setChargingSoundEnabled)

    fun toggleVibrateWhenRinging() = applyRootToggle(_vibrateWhenRinging, systemInteractor::setVibrateWhenRingingEnabled)

    fun toggleAdaptiveBrightness() {
        // מקומי בלבד (Settings.System ישיר, לא root) - נשאר סינכרוני.
        val newValue = !_adaptiveBrightness.value
        _adaptiveBrightness.value = newValue
        systemInteractor.setAdaptiveBrightnessEnabled(newValue)
    }

    fun toggleAnimationsReduced() = applyRootToggle(_animationsReduced, systemInteractor::setAnimationsReduced)

    fun toggleLocationEnabled() = applyRootToggle(_locationEnabled, systemInteractor::setLocationEnabled)

    fun toggleDataRoaming() = applyRootToggle(_dataRoaming, systemInteractor::setDataRoamingEnabled)

    fun toggleColorInversion() = applyRootToggle(_colorInversion, systemInteractor::setColorInversionEnabled)

    fun toggleAutoTimeZone() = applyRootToggle(_autoTimeZone, systemInteractor::setAutoTimeZoneEnabled)

    fun setLowBatteryWarningLevel(percent: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setLowBatteryWarningLevel(percent)
            withContext(Dispatchers.Main) {
                if (success) _lowBatteryWarningLevel.value = percent
                else showToast("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root")
            }
        }
    }

    fun freeUpSpace() {
        _isFreeingSpace.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.freeUpSpace()
            viewModelScope.launch(Dispatchers.Main) {
                _isFreeingSpace.value = false
                showToast(if (success) "פינוי מקום הופעל" else "פינוי המקום נכשל")
            }
        }
    }

    fun restartDevice() {
        systemInteractor.restartDevice()
    }

    fun shutdownDevice() {
        systemInteractor.shutdownDevice()
    }

    fun forceStopApp(packageName: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.forceStopApp(packageName)
            viewModelScope.launch(Dispatchers.Main) { showToast("$appName נעצרה") }
        }
    }

    fun clearAppData(packageName: String, appName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.clearAppData(packageName)
            viewModelScope.launch(Dispatchers.Main) { showToast("הנתונים של $appName נמחקו") }
        }
    }

    fun refreshRamInfo() {
        _ramInfo.value = systemInteractor.getRamInfo()
    }

    /** כל הפעולות כאן מריצות פקודות root חוסמות (spawn של su, המתנה לפלט) -
     *  חובה על thread ברקע, אחרת ה-UI thread נחסם וגורם ל-ANR אמיתי (נצפה בפועל
     *  במכשיר: חסימה של יותר מדקה כשזה רץ synchronously). */
    fun optimizeNow() {
        _isOptimizing.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val count = systemInteractor.cleanBackgroundApps()
            systemInteractor.freeUpSpace()
            val ram = systemInteractor.getRamInfo()
            viewModelScope.launch(Dispatchers.Main) {
                _ramInfo.value = ram
                _isOptimizing.value = false
                showToast(if (count > 0) "האופטימיזציה הושלמה - $count אפליקציות נוקו" else "האופטימיזציה הושלמה")
            }
        }
    }

    fun loadScreenTime() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!systemInteractor.hasUsageAccess()) {
                systemInteractor.grantUsageAccess()
            }
            val summary = systemInteractor.getTodayScreenTime()
            viewModelScope.launch(Dispatchers.Main) { _screenTimeSummary.value = summary }
        }
    }

    fun loadDefaultApps() {
        _defaultSmsPackage.value = systemInteractor.getDefaultSmsPackage()
        _defaultDialerPackage.value = systemInteractor.getDefaultDialerPackage()
    }

    fun toggleNfc() = applyRootToggle(_nfcEnabled, systemInteractor::setNfcEnabled)

    fun toggleStayAwakeWhileCharging() = applyRootToggle(_stayAwakeWhileCharging, systemInteractor::setStayAwakeWhileChargingEnabled)

    fun toggleSleepMode() {
        val newValue = !_sleepModeEnabled.value
        _sleepModeEnabled.value = newValue
        systemInteractor.setSleepModeEnabled(newValue)
    }

    fun focusNow(minutes: Int) {
        val succeeded = systemInteractor.scheduleFocusNow(minutes)
        if (succeeded) {
            showToast("מצב מיקוד הופעל ל-$minutes דקות")
        } else {
            showToast("צריך לאשר גישה למדיניות התראות כדי להפעיל מיקוד")
            systemInteractor.openNotificationPolicyAccessSettings()
        }
    }

    fun cancelFocusNow() {
        systemInteractor.cancelFocusNow()
        showToast("מצב מיקוד בוטל")
    }

    fun revokePermission(packageName: String, permission: String, onDone: (List<SystemInteractor.PermissionEntry>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.revokePermission(packageName, permission)
            val updated = systemInteractor.getAppPermissions(packageName)
            viewModelScope.launch(Dispatchers.Main) { onDone(updated) }
        }
    }

    fun enableDeveloperMode() {
        systemInteractor.enableDeveloperMode()
        _developerModeEnabled.value = true
        showToast("מצב מפתחים הופעל")
    }

    /** מפעיל/מאריך את "מצב חגיגה" ל-5 דקות מעכשיו - הזנה חוזרת של הקוד הסודי
     *  תוך כדי שהמצב כבר פעיל פשוט "מטעינה" את הטיימר מחדש. */
    fun activatePartyMode() {
        _partyModeExpiresAt.value = System.currentTimeMillis() + PARTY_MODE_DURATION_MS
    }

    fun submitT9Score(score: Int) {
        if (score > _t9HighScore.value) {
            _t9HighScore.value = score
            prefs.edit().putInt("t9_high_score", score).apply()
        }
    }

    /** מחליף את צבע ההדגשה בכל האפליקציה (ObPreference משותף) פעם בשנייה, לא בכל
     *  פריים - קריאה ל-ContentProvider (IPC) בקצב אנימציה תגרום לגמגום/ANR, אותו
     *  לקח בדיוק כמו התיקון ל-optimizeNow הסינכרוני. */
    fun toggleCyclingAccent() {
        val newValue = !_cyclingAccentEnabled.value
        _cyclingAccentEnabled.value = newValue
        if (newValue) {
            cyclingAccentJob = viewModelScope.launch(Dispatchers.Default) {
                var hue = 0f
                // הלולאה בודקת את תפוגת מצב החגיגה בעצמה בכל טיק - כדי שהצבע
                // המתחלף יפסיק לבד ברגע שחמש הדקות נגמרות, גם אם המתג נשאר דלוק.
                while (_cyclingAccentEnabled.value && System.currentTimeMillis() < _partyModeExpiresAt.value) {
                    val color = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.7f, 1f)))
                    withContext(Dispatchers.IO) {
                        ThemeClient.setPrimaryColor(app, color.toArgb())
                    }
                    viewModelScope.launch(Dispatchers.Main) {
                        _themeConfig.value = _themeConfig.value.copy(primaryColor = color)
                    }
                    hue = (hue + 24f) % 360f
                    kotlinx.coroutines.delay(1000)
                }
                viewModelScope.launch(Dispatchers.Main) { _cyclingAccentEnabled.value = false }
            }
        } else {
            cyclingAccentJob?.cancel()
        }
    }

    // --- זום מסך ---

    val densityPresetsCount = 5 // צעדים סביב הצפיפות המקורית: 88%..112%

    fun setScreenZoomStep(step: Int) {
        val factor = 0.88f + (step.coerceIn(0, densityPresetsCount - 1) * 0.06f)
        val dpi = (defaultDensityDpi * factor).toInt()
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setDisplayDensityDpi(dpi)
            withContext(Dispatchers.Main) {
                if (success) _densityDpi.value = dpi
                else showToast("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root")
            }
        }
    }

    fun resetScreenZoom() {
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.resetDisplayDensity()
            withContext(Dispatchers.Main) {
                if (success) _densityDpi.value = defaultDensityDpi
                else showToast("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root")
            }
        }
    }

    // --- חלון מרובה ---

    fun toggleMultiWindowForced() = applyRootToggle(_multiWindowForced, systemInteractor::setMultiWindowForced)

    // --- השתקת אפליקציה ---

    fun setAppAudioMuted(packageName: String, muted: Boolean, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val success = systemInteractor.setAppAudioMuted(packageName, muted)
            viewModelScope.launch(Dispatchers.Main) {
                if (!success) showToast("לא ניתן לשנות הגדרה זו - נדרשת הרשאת root")
                onDone(muted)
            }
        }
    }

    // --- ניהול כרטיסי SIM ---

    fun loadSimCards() {
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.ensurePhoneStatePermission()
            val sims = systemInteractor.getSimCards()
            val data = systemInteractor.getDefaultDataSubscriptionId()
            val voice = systemInteractor.getDefaultVoiceSubscriptionId()
            val sms = systemInteractor.getDefaultSmsSubscriptionId()
            viewModelScope.launch(Dispatchers.Main) {
                _simCards.value = sims
                _defaultDataSubId.value = data
                _defaultVoiceSubId.value = voice
                _defaultSmsSubId.value = sms
            }
        }
    }

    fun renameSim(subscriptionId: Int, name: String) {
        systemInteractor.setSimDisplayName(subscriptionId, name)
        loadSimCards()
    }

    fun recolorSim(subscriptionId: Int, colorArgb: Int) {
        systemInteractor.setSimColor(subscriptionId, colorArgb)
        loadSimCards()
    }

    fun setDefaultDataSim(subscriptionId: Int) {
        _defaultDataSubId.value = subscriptionId
        systemInteractor.setDefaultDataSim(subscriptionId)
    }

    fun setDefaultVoiceSim(subscriptionId: Int) {
        _defaultVoiceSubId.value = subscriptionId
        systemInteractor.setDefaultVoiceSim(subscriptionId)
    }

    fun setDefaultSmsSim(subscriptionId: Int) {
        _defaultSmsSubId.value = subscriptionId
        systemInteractor.setDefaultSmsSim(subscriptionId)
    }

    // --- אודות הטלפון: IMEI, מספר טלפון, מידע רגולטורי ---

    fun loadPhoneIdentity() {
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.ensurePhoneStatePermission()
            val imeiValue = systemInteractor.getImei()
            val numberValue = systemInteractor.getPrimaryPhoneNumber()
            viewModelScope.launch(Dispatchers.Main) {
                _imei.value = imeiValue
                _phoneNumber.value = numberValue
            }
        }
    }

    fun openRegulatoryInfo() {
        systemInteractor.openRegulatoryInfo()
    }

    // --- סוללה מורחבת ---

    fun loadBatteryExtended() {
        _batteryExtended.value = systemInteractor.getBatteryExtendedInfo()
    }

    // --- טיימרים לאפליקציות ---

    fun loadAppTimers() {
        viewModelScope.launch(Dispatchers.IO) {
            if (!systemInteractor.hasUsageAccess()) systemInteractor.grantUsageAccess()
            val timers = systemInteractor.getAppTimers()
            viewModelScope.launch(Dispatchers.Main) { _appTimers.value = timers }
        }
    }

    fun setAppTimer(packageName: String, dailyLimitMinutes: Int) {
        systemInteractor.setAppTimer(packageName, dailyLimitMinutes)
        systemInteractor.scheduleAppTimerChecks()
        _appTimers.value = systemInteractor.getAppTimers()
    }

    fun removeAppTimer(packageName: String) {
        systemInteractor.removeAppTimer(packageName)
        _appTimers.value = systemInteractor.getAppTimers()
        if (_appTimers.value.isEmpty()) systemInteractor.cancelAppTimerChecks()
    }

    // --- בטיחות וחירום (SOS) ---

    fun loadEmergencyContact() {
        _emergencyContact.value = systemInteractor.getEmergencyContact()
    }

    fun saveEmergencyContact(name: String, phone: String) {
        systemInteractor.setEmergencyContact(name, phone)
        _emergencyContact.value = name to phone
        showToast("איש קשר לחירום נשמר")
    }

    fun clearEmergencyContact() {
        systemInteractor.clearEmergencyContact()
        _emergencyContact.value = null
    }

    fun sendSosNow() {
        val contact = _emergencyContact.value
        if (contact == null) {
            showToast("קודם צריך לשמור איש קשר לחירום")
            return
        }
        _sosSending.value = true
        viewModelScope.launch(Dispatchers.IO) {
            systemInteractor.ensureSosPermissions()
            val location = systemInteractor.getLastKnownLocation()
            val locationText = if (location != null) {
                "מיקום אחרון: https://maps.google.com/?q=${location.latitude},${location.longitude}"
            } else {
                "מיקום לא זמין כרגע"
            }
            val message = "SOS - זקוק/ה לעזרה. $locationText"
            val sent = systemInteractor.sendEmergencySms(contact.second, message)
            viewModelScope.launch(Dispatchers.Main) {
                _sosSending.value = false
                showToast(if (sent) "הודעת חירום נשלחה ל-${contact.first}" else "שליחת הודעת החירום נכשלה")
            }
        }
    }

    // --- אבחון מכשיר ---

    fun testVibration() = systemInteractor.testVibration()
    fun testSpeakerTone() = systemInteractor.testSpeakerTone()

    fun getSystemInteractor(): SystemInteractor = systemInteractor
}
