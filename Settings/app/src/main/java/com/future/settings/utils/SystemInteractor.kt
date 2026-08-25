package com.future.settings.utils

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.telephony.SmsManager
import android.location.Location
import android.location.LocationManager
import android.media.ToneGenerator
import java.io.DataOutputStream
import java.io.BufferedReader
import java.io.InputStreamReader

class SystemInteractor(private val context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val bluetoothAdapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as android.bluetooth.BluetoothManager).adapter
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
    private val contentResolver: ContentResolver = context.contentResolver

    /** מצב שקט/רטט דורש הרשאת "גישה למדיניות התראות" - בלעדיה השינוי פשוט לא קורה בשקט. */
    fun hasNotificationPolicyAccess(): Boolean = notificationManager.isNotificationPolicyAccessGranted

    fun openNotificationPolicyAccessSettings() {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    fun getPairedBluetoothDevices(): Set<android.bluetooth.BluetoothDevice> {
        return try { bluetoothAdapter?.bondedDevices ?: emptySet() } catch (e: SecurityException) { emptySet() }
    }

    /** אחוז סוללה נוכחי + האם בטעינה, לפי ה-sticky broadcast של המערכת (לא דורש הרשאה). */
    fun getBatteryStatus(): Pair<Int, Boolean> {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) (level * 100) / scale else 100
            val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            percent to charging
        } catch (e: Exception) {
            100 to false
        }
    }

    fun isBatterySaverOn(): Boolean = powerManager.isPowerSaveMode

    fun setBatterySaver(enabled: Boolean): Boolean {
        return runRootCommand("settings put global low_power ${if (enabled) 1 else 0}").success
    }

    data class StorageInfo(val usedBytes: Long, val totalBytes: Long)

    fun getStorageInfo(): StorageInfo {
        return try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockCountLong * stat.blockSizeLong
            val free = stat.availableBlocksLong * stat.blockSizeLong
            StorageInfo(usedBytes = total - free, totalBytes = total)
        } catch (e: Exception) {
            StorageInfo(0L, 1L)
        }
    }

    fun initSystemPermissions() {
        val packageName = context.packageName
        val commands = listOf(
            "appops set $packageName WRITE_SETTINGS allow",
            "appops set $packageName ACCESS_NOTIFICATION_POLICY allow",
            "pm grant $packageName android.permission.WRITE_SETTINGS",
            "pm grant $packageName android.permission.MODIFY_AUDIO_SETTINGS",
            "pm grant $packageName android.permission.ACCESS_NOTIFICATION_POLICY",
            "pm grant $packageName android.permission.BLUETOOTH_CONNECT",
            "pm grant $packageName android.permission.BLUETOOTH_SCAN",
            "pm grant $packageName android.permission.BLUETOOTH_ADMIN",
            // המקלדת T9 המותאמת (com.future.keyboard) לא עושה כלום אם היא רק "זמינה" -
            // אנדרואיד דורש גם enable וגם set מפורש כדי שהיא באמת תקבל את לחיצות
            // המקש הפיזיות; בלעדיהם ה-IME המובנה של אנדרואיד מטפל בהקלדה במקומה
            // (בלי חיזוי T9 ובלי המיפוי הנכון של המקלדת שלנו). מריצים את זה בכל
            // הפעלה של ההגדרות כדי שזה "יתקן את עצמו" גם אם המשתמש שינה זאת בטעות.
            "ime enable ${KEYBOARD_IME_ID}",
            "ime set ${KEYBOARD_IME_ID}"
        )
        runRootCommands(commands)
    }

    companion object {
        private const val KEYBOARD_IME_ID = "com.future.keyboard/.KeyboardService"
    }

    /** מחזיר false אם השינוי נחסם (למשל: מצב שקט/רטט בלי הרשאת מדיניות התראות). */
    fun setRingerMode(mode: Int): Boolean {
        if (mode != AudioManager.RINGER_MODE_NORMAL && !hasNotificationPolicyAccess()) {
            return false
        }
        audioManager.ringerMode = mode
        return true
    }

    fun getRingerMode(): Int = audioManager.ringerMode

    fun setVolume(stream: Int, level: Int) {
        audioManager.setStreamVolume(stream, level, 0)
    }

    fun getStreamVolume(stream: Int): Int = audioManager.getStreamVolume(stream)
    fun getStreamMaxVolume(stream: Int): Int = audioManager.getStreamMaxVolume(stream)

    fun setBrightness(value: Int) {
        try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getBrightness(): Int = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)

    /** תוצאה של הרצת פקודת/ות root - הפלט הטקסטואלי (לפקודות קריאה כמו "settings get")
     *  יחד עם דגל success אמיתי, כדי שקוראים שרק מפעילים פקודה (למשל toggles) יוכלו
     *  לדעת אם היא באמת הצליחה במקום להניח שהצלחה תמיד קרתה (ר' תיקון: מתגים שהראו
     *  "פועל" גם כשפקודת ה-root נכשלה בשקט, למשל במכשיר לא-rooted). */
    data class RootCommandResult(val output: String, val success: Boolean)

    fun runRootCommand(command: String): RootCommandResult {
        return runRootCommands(listOf(command))
    }

    fun runRootCommands(commands: List<String>): RootCommandResult {
        var output = ""
        var success = false
        try {
            val process = Runtime.getRuntime().exec("su")
            val os = DataOutputStream(process.outputStream)
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            for (cmd in commands) {
                os.writeBytes(cmd + "\n")
            }
            os.writeBytes("exit\n")
            os.flush()

            output = reader.readText()
            val exitCode = process.waitFor()
            success = exitCode == 0
        } catch (e: Exception) {
            // Fallback for non-root environments or individual failures - if we had to
            // fall back here it means "su" itself couldn't be executed (e.g. non-rooted
            // device), so this can never be reported as a genuine root-command success.
            for (cmd in commands) {
                try {
                    val process = Runtime.getRuntime().exec(cmd)
                    process.waitFor()
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
            }
            success = false
        }
        return RootCommandResult(output, success)
    }

    fun setAirplaneMode(enabled: Boolean): Boolean {
        val state = if (enabled) 1 else 0
        val r1 = runRootCommand("settings put global airplane_mode_on $state")
        val r2 = runRootCommand("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled")
        return r1.success && r2.success
    }

    fun isAirplaneModeOn(): Boolean = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

    fun setMobileData(enabled: Boolean): Boolean {
        val state = if (enabled) "enable" else "disable"
        return runRootCommand("svc data $state").success
    }

    fun isMobileDataEnabled(): Boolean {
        return try {
            val result = runRootCommand("settings get global mobile_data").output.trim()
            result == "1"
        } catch (e: Exception) {
            false
        }
    }

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    fun setBluetoothEnabled(enabled: Boolean): Boolean {
        return runRootCommand("svc bluetooth ${if (enabled) "enable" else "disable"}").success
    }

    // --- Bluetooth: שם מכשיר, מכשירים משויכים, גילוי, התאמה ושיתוף קבצים ---

    data class BluetoothDeviceEntry(
        val address: String,
        val name: String,
        val isConnected: Boolean
    )

    /** שם התצוגה של המכשיר שלנו כפי שמכשירי Bluetooth אחרים רואים אותו. */
    fun getBluetoothDeviceName(): String =
        try { bluetoothAdapter?.name ?: Build.MODEL } catch (e: SecurityException) { Build.MODEL }

    fun setBluetoothDeviceName(name: String) {
        try { bluetoothAdapter?.name = name } catch (e: SecurityException) {}
    }

    /** אין API ציבורי ל"מחובר בפועל" (לעומת "משויך/paired") - BluetoothDevice.isConnected()
     *  הוא Hidden API, אז ניגשים אליו ברפלקציה, באותה טכניקה בדיוק שכבר קיימת ב-
     *  FutureUI/ControlManager.kt לצורך אייקון ה-Bluetooth בשורת המצב. */
    private fun android.bluetooth.BluetoothDevice.isActuallyConnected(): Boolean = try {
        val method = javaClass.getMethod("isConnected")
        method.invoke(this) as? Boolean ?: false
    } catch (e: Exception) {
        false
    }

    fun getPairedBluetoothDevicesList(): List<BluetoothDeviceEntry> {
        return try {
            (bluetoothAdapter?.bondedDevices ?: emptySet())
                .map { device -> BluetoothDeviceEntry(device.address, device.name ?: device.address, device.isActuallyConnected()) }
                .sortedByDescending { it.isConnected }
        } catch (e: SecurityException) {
            emptyList()
        }
    }

    fun isBluetoothDiscovering(): Boolean = try { bluetoothAdapter?.isDiscovering ?: false } catch (e: SecurityException) { false }

    fun startBluetoothDiscovery(): Boolean {
        return try {
            val adapter = bluetoothAdapter ?: return false
            if (adapter.isDiscovering) adapter.cancelDiscovery()
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            false
        }
    }

    fun cancelBluetoothDiscovery() {
        try { bluetoothAdapter?.cancelDiscovery() } catch (e: SecurityException) {}
    }

    fun pairBluetoothDevice(device: android.bluetooth.BluetoothDevice): Boolean =
        try { device.createBond() } catch (e: SecurityException) { false }

    /** מבטל שיוך (unpair) - "שכח מכשיר". removeBond() הוא Hidden API כמו isConnected(). */
    fun forgetBluetoothDevice(address: String): Boolean {
        return try {
            val device = bluetoothAdapter?.bondedDevices?.find { it.address == address } ?: return false
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    // --- טפט: הגדרה בפועל דרך WallpaperManager (מסך בית + מסך נעילה יחד) ---

    private val wallpaperManager by lazy { android.app.WallpaperManager.getInstance(context) }

    fun setWallpaperBitmap(bitmap: android.graphics.Bitmap): Boolean {
        return try {
            wallpaperManager.setBitmap(bitmap, null, true, android.app.WallpaperManager.FLAG_SYSTEM or android.app.WallpaperManager.FLAG_LOCK)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun setWallpaperFromUri(uri: Uri): Boolean {
        return try {
            val bitmap = contentResolver.openInputStream(uri)?.use { android.graphics.BitmapFactory.decodeStream(it) } ?: return false
            setWallpaperBitmap(bitmap)
        } catch (e: Exception) {
            false
        }
    }

    // --- צלילים ורטט ---

    fun isSoundEffectsEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0

    fun setSoundEffectsEnabled(enabled: Boolean) {
        try {
            Settings.System.putInt(contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, if (enabled) 1 else 0)
        } catch (e: Exception) {}
    }

    fun isHapticFeedbackEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0

    fun setHapticFeedbackEnabled(enabled: Boolean) {
        try {
            Settings.System.putInt(contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, if (enabled) 1 else 0)
        } catch (e: Exception) {}
    }

    /** מילישניות עד כיבוי מסך אוטומטי. */
    fun getScreenTimeout(): Int =
        Settings.System.getInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)

    fun setScreenTimeout(millis: Int) {
        try {
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, millis)
        } catch (e: Exception) {}
    }

    // --- רינגטונים/צלילי התראה/אזעקה אמיתיים דרך RingtoneManager - בלי לקפוץ להגדרות אנדרואיד ---
    // אותן פונקציות משרתות שלושה סוגי צליל (TYPE_RINGTONE/TYPE_NOTIFICATION/TYPE_ALARM)
    // לפי פרמטר type, כדי לא לשכפל את הלוגיקה לכל מסך בחירת צליל בנפרד.

    data class RingtoneEntry(val title: String, val uri: Uri)

    fun getAvailableRingtones(type: Int = RingtoneManager.TYPE_RINGTONE): List<RingtoneEntry> {
        return try {
            val manager = RingtoneManager(context)
            manager.setType(type)
            manager.cursor.use { cursor ->
                val entries = mutableListOf<RingtoneEntry>()
                while (cursor.moveToNext()) {
                    val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                    val uri = manager.getRingtoneUri(cursor.position)
                    entries.add(RingtoneEntry(title, uri))
                }
                entries
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getCurrentRingtoneUri(type: Int = RingtoneManager.TYPE_RINGTONE): Uri? =
        RingtoneManager.getActualDefaultRingtoneUri(context, type)

    fun setDefaultRingtone(uri: Uri, type: Int = RingtoneManager.TYPE_RINGTONE) {
        try {
            RingtoneManager.setActualDefaultRingtoneUri(context, type, uri)
        } catch (e: Exception) {}
    }

    // --- צלילי מערכת נוספים ---

    fun isDialPadTonesEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, 1) != 0

    fun setDialPadTonesEnabled(enabled: Boolean) {
        try {
            Settings.System.putInt(contentResolver, Settings.System.DTMF_TONE_WHEN_DIALING, if (enabled) 1 else 0)
        } catch (e: Exception) {}
    }

    fun isLockSoundEnabled(): Boolean =
        Settings.System.getInt(contentResolver, "lockscreen_sounds_enabled", 1) != 0

    fun setLockSoundEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put system lockscreen_sounds_enabled ${if (enabled) 1 else 0}").success
    }

    fun isChargingSoundEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, "charging_sounds_enabled", 1) != 0

    fun setChargingSoundEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put global charging_sounds_enabled ${if (enabled) 1 else 0}").success
    }

    /** "vibrate_when_ringing" הוחלף רשמית ב-API חדשים יותר ב-VIBRATE_ON, אבל המפתח הישן
     *  עדיין נקרא בפועל ע"י מערכת הטלפוניה ברוב גרסאות אנדרואיד 12 - נכתב דרך root ישירות. */
    fun isVibrateWhenRingingEnabled(): Boolean =
        Settings.System.getInt(contentResolver, "vibrate_when_ringing", 0) != 0

    fun setVibrateWhenRingingEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put system vibrate_when_ringing ${if (enabled) 1 else 0}").success
    }

    // --- תצוגה ---

    fun isAdaptiveBrightnessEnabled(): Boolean =
        Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC

    fun setAdaptiveBrightnessEnabled(enabled: Boolean) {
        try {
            val mode = if (enabled) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, mode)
        } catch (e: Exception) {}
    }

    /** מכבה/מדליק את שלושת מקדמי האנימציה של המערכת (חלון/מעבר/אנימטור) יחד -
     *  זו הדרך האמיתית לכבות "אנימציות מערכת" באנדרואיד, לא הגדרה בודדת. */
    fun areAnimationsReduced(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.WINDOW_ANIMATION_SCALE, 1f) == 0f

    fun setAnimationsReduced(reduced: Boolean): Boolean {
        val scale = if (reduced) "0" else "1"
        return runRootCommands(
            listOf(
                "settings put global window_animation_scale $scale",
                "settings put global transition_animation_scale $scale",
                "settings put global animator_duration_scale $scale"
            )
        ).success
    }

    // --- מיקום ורשת סלולרית (בלי Wi-Fi) ---

    private val locationManager by lazy { context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager }

    fun isLocationEnabled(): Boolean = try { locationManager.isLocationEnabled } catch (e: Exception) { false }

    fun setLocationEnabled(enabled: Boolean): Boolean {
        return runRootCommand("cmd location set-location-enabled ${if (enabled) "true" else "false"}").success
    }

    fun isDataRoamingEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, Settings.Global.DATA_ROAMING, 0) != 0

    fun setDataRoamingEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put global data_roaming ${if (enabled) 1 else 0}").success
    }

    // --- נגישות ---

    fun isColorInversionEnabled(): Boolean =
        Settings.Secure.getInt(contentResolver, "accessibility_display_inverted_enabled", 0) != 0

    fun setColorInversionEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put secure accessibility_display_inverted_enabled ${if (enabled) 1 else 0}").success
    }

    // --- סוללה מורחב ---

    data class BatteryDetails(val tempCelsius: Float, val voltageVolts: Float, val healthLabel: String)

    fun getBatteryDetails(): BatteryDetails {
        return try {
            val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val tempTenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
            val voltageMillis = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
            val health = when (intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
                BatteryManager.BATTERY_HEALTH_GOOD -> "תקינה"
                BatteryManager.BATTERY_HEALTH_OVERHEAT -> "התחממות יתר"
                BatteryManager.BATTERY_HEALTH_DEAD -> "פגומה"
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "מתח יתר"
                BatteryManager.BATTERY_HEALTH_COLD -> "קרה מדי"
                else -> "לא ידועה"
            }
            BatteryDetails(tempTenths / 10f, voltageMillis / 1000f, health)
        } catch (e: Exception) {
            BatteryDetails(0f, 0f, "לא ידועה")
        }
    }

    fun getLowBatteryWarningLevel(): Int =
        Settings.Global.getInt(contentResolver, "low_power_mode_trigger_level", 15)

    fun setLowBatteryWarningLevel(percent: Int): Boolean {
        return runRootCommand("settings put global low_power_mode_trigger_level $percent").success
    }

    // --- אחסון מורחב ---

    /** מפעיל ניקוי מטמון אמיתי בכל המערכת - הדרך האמיתית היחידה לפנות מקום
     *  בלי הרשאות אחסון-כללי, כי אין ל-Settings עצמה MANAGE_EXTERNAL_STORAGE. */
    fun freeUpSpace(): Boolean {
        return try {
            runRootCommand("pm trim-caches 999999999999").success
        } catch (e: Exception) {
            false
        }
    }

    // --- הפעלה מחדש / כיבוי ---

    fun restartDevice() {
        runRootCommand("reboot")
    }

    fun shutdownDevice() {
        runRootCommand("reboot -p")
    }

    // --- ניהול אפליקציות FutureOS ---

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getApplicationInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getAppVersionName(packageName: String): String? {
        return try {
            context.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    }

    fun forceStopApp(packageName: String) {
        runRootCommand("am force-stop $packageName")
    }

    /** מוחק את כל הנתונים המקומיים של האפליקציה (כמו "ניקוי נתונים" באנדרואיד המקורי) -
     *  פעולה הרסנית ובלתי הפיכה, חייבת אישור מהמשתמש לפני הקריאה לפונקציה הזו. */
    fun clearAppData(packageName: String) {
        runRootCommand("pm clear $packageName")
    }

    private var previewRingtone: Ringtone? = null

    fun previewRingtone(uri: Uri) {
        try {
            previewRingtone?.stop()
            previewRingtone = RingtoneManager.getRingtone(context, uri)
            previewRingtone?.play()
        } catch (e: Exception) {}
    }

    fun stopRingtonePreview() {
        try {
            previewRingtone?.stop()
        } catch (e: Exception) {}
    }

    // --- תאריך ושעה ---

    fun is24HourFormat(): Boolean = Settings.System.getString(contentResolver, Settings.System.TIME_12_24) == "24"

    fun set24HourFormat(use24: Boolean): Boolean {
        return try {
            Settings.System.putString(contentResolver, Settings.System.TIME_12_24, if (use24) "24" else "12")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isAutoTimeEnabled(): Boolean = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME, 1) != 0

    fun setAutoTimeEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put global auto_time ${if (enabled) 1 else 0}").success
    }

    fun isAutoTimeZoneEnabled(): Boolean = Settings.Global.getInt(contentResolver, Settings.Global.AUTO_TIME_ZONE, 1) != 0

    fun setAutoTimeZoneEnabled(enabled: Boolean): Boolean {
        return runRootCommand("settings put global auto_time_zone ${if (enabled) 1 else 0}").success
    }

    // --- שפת מערכת - שינוי אמיתי דרך פקודת root, בלי לקפוץ להגדרות אנדרואיד ---

    fun setSystemLocale(languageTag: String): Boolean {
        val r1 = runRootCommand("settings put system system_locales $languageTag")
        val r2 = runRootCommand("am broadcast -a android.intent.action.LOCALE_CHANGED")
        return r1.success && r2.success
    }

    fun getCurrentLocaleTag(): String = java.util.Locale.getDefault().toLanguageTag()

    // --- אבטחה ---

    fun isDeviceSecure(): Boolean = keyguardManager.isDeviceSecure

    // --- מידע מערכת אמיתי למסך "אודות" ---

    fun isRooted(): Boolean {
        return try {
            val result = runRootCommand("id").output.trim()
            result.contains("uid=0")
        } catch (e: Exception) {
            false
        }
    }

    fun getUptimeString(): String {
        val totalSeconds = SystemClock.elapsedRealtime() / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        return if (hours > 0) "$hours שעות ו-$minutes דקות" else "$minutes דקות"
    }

    fun getTotalRamString(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val info = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            val gb = info.totalMem / (1024.0 * 1024.0 * 1024.0)
            "%.1f GB".format(gb)
        } catch (e: Exception) {
            "לא ידוע"
        }
    }

    fun getKernelVersion(): String = try { System.getProperty("os.version") ?: "לא ידוע" } catch (e: Exception) { "לא ידוע" }

    // --- ביצועים וזיכרון ---

    data class RamInfo(val availBytes: Long, val totalBytes: Long, val lowMemory: Boolean)

    fun getRamInfo(): RamInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val info = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(info)
            RamInfo(info.availMem, info.totalMem, info.lowMemory)
        } catch (e: Exception) {
            RamInfo(0L, 1L, false)
        }
    }

    fun getCpuInfo(): String {
        val cores = Runtime.getRuntime().availableProcessors()
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "לא ידוע"
        return "$cores ליבות ($abi)"
    }

    /** אפליקציות FutureOS שאסור לעצור בכפייה בניקוי RAM - מערכת ההפעלה עצמה,
     *  ההשקה, ואפליקציה שעשויה לנגן ברקע כרגע. */
    private val ramCleanupProtectedPackages = setOf(
        "com.future.futureui", "com.future.futurelauncher", "com.future.settings", "com.future.music"
    )

    /** מנקה RAM ע"י עצירה בכפייה של אפליקציות FutureOS לא-קריטיות - מחזיר כמה
     *  אפליקציות טופלו (אם היו פועלות ברקע, זה עוצר אותן; אם לא, הפקודה חסרת השפעה). */
    fun cleanBackgroundApps(): Int {
        val targets = FUTURE_OS_PACKAGES.filter { it !in ramCleanupProtectedPackages && isAppInstalled(it) }
        if (targets.isEmpty()) return 0
        runRootCommands(targets.map { "am force-stop $it" })
        return targets.size
    }

    // --- זמן מסך (UsageStatsManager) ---

    data class UsageEntry(val packageName: String, val millis: Long)
    data class ScreenTimeSummary(val totalMillis: Long, val topApps: List<UsageEntry>)

    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
            val mode = appOps.checkOpNoThrow(
                android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == android.app.AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            false
        }
    }

    fun grantUsageAccess() {
        runRootCommand("appops set ${context.packageName} GET_USAGE_STATS allow")
    }

    fun getTodayScreenTime(): ScreenTimeSummary {
        return try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as android.app.usage.UsageStatsManager
            val cal = java.util.Calendar.getInstance()
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
            cal.set(java.util.Calendar.MINUTE, 0)
            cal.set(java.util.Calendar.SECOND, 0)
            cal.set(java.util.Calendar.MILLISECOND, 0)
            val startOfDay = cal.timeInMillis
            val now = System.currentTimeMillis()
            val stats = usm.queryUsageStats(android.app.usage.UsageStatsManager.INTERVAL_DAILY, startOfDay, now)
                ?: emptyList()
            val entries = stats
                .filter { it.totalTimeInForeground > 0 }
                .map { UsageEntry(it.packageName, it.totalTimeInForeground) }
                .sortedByDescending { it.millis }
            ScreenTimeSummary(entries.sumOf { it.millis }, entries.take(5))
        } catch (e: Exception) {
            ScreenTimeSummary(0L, emptyList())
        }
    }

    // --- ברירות מחדל ---
    // RoleManager.getRoleHolders() הוא Hidden API שלא זמין ל-SDK ציבורי - במקום זאת
    // משתמשים ב-Telephony.Sms/TelecomManager, שמיועדים בדיוק לצורך הזה ופומביים לחלוטין.

    fun getDefaultSmsPackage(): String? = try {
        android.provider.Telephony.Sms.getDefaultSmsPackage(context)
    } catch (e: Exception) {
        null
    }

    fun getDefaultDialerPackage(): String? = try {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as android.telecom.TelecomManager
        telecomManager.defaultDialerPackage
    } catch (e: Exception) {
        null
    }

    // --- הרשאות אפליקציה (Privacy) ---

    data class PermissionEntry(val name: String, val shortLabel: String, val granted: Boolean)

    private val knownDangerousPermissions = mapOf(
        "android.permission.READ_CONTACTS" to "אנשי קשר",
        "android.permission.WRITE_CONTACTS" to "אנשי קשר",
        "android.permission.SEND_SMS" to "הודעות SMS",
        "android.permission.READ_SMS" to "הודעות SMS",
        "android.permission.RECEIVE_SMS" to "הודעות SMS",
        "android.permission.CALL_PHONE" to "שיחות",
        "android.permission.READ_PHONE_STATE" to "מצב הטלפון",
        "android.permission.READ_CALL_LOG" to "יומן שיחות",
        "android.permission.WRITE_CALL_LOG" to "יומן שיחות",
        "android.permission.CAMERA" to "מצלמה",
        "android.permission.RECORD_AUDIO" to "מיקרופון",
        "android.permission.ACCESS_FINE_LOCATION" to "מיקום מדויק",
        "android.permission.ACCESS_COARSE_LOCATION" to "מיקום משוער",
        "android.permission.READ_EXTERNAL_STORAGE" to "אחסון",
        "android.permission.MANAGE_EXTERNAL_STORAGE" to "אחסון",
        "android.permission.POST_NOTIFICATIONS" to "התראות"
    )

    fun getAppPermissions(packageName: String): List<PermissionEntry> {
        return try {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getPackageInfo(packageName, android.content.pm.PackageManager.GET_PERMISSIONS)
            val names = info.requestedPermissions ?: return emptyList()
            val flags = info.requestedPermissionsFlags ?: IntArray(names.size)
            names.indices.mapNotNull { i ->
                val shortLabel = knownDangerousPermissions[names[i]] ?: return@mapNotNull null
                val granted = (flags.getOrElse(i) { 0 } and android.content.pm.PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0
                PermissionEntry(names[i], shortLabel, granted)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun revokePermission(packageName: String, permission: String) {
        runRootCommand("pm revoke $packageName $permission")
    }

    // --- NFC ---

    private val nfcAdapter: android.nfc.NfcAdapter? by lazy {
        try { android.nfc.NfcAdapter.getDefaultAdapter(context) } catch (e: Exception) { null }
    }

    fun isNfcAvailable(): Boolean = nfcAdapter != null

    fun isNfcEnabled(): Boolean = try { nfcAdapter?.isEnabled == true } catch (e: Exception) { false }

    fun setNfcEnabled(enabled: Boolean): Boolean {
        return runRootCommand("svc nfc ${if (enabled) "enable" else "disable"}").success
    }

    // --- השארת המסך ער בזמן טעינה (Developer options: Stay awake) ---

    fun isStayAwakeWhileChargingEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, 0) != 0

    fun setStayAwakeWhileChargingEnabled(enabled: Boolean): Boolean {
        val value = if (enabled) (BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_USB or BatteryManager.BATTERY_PLUGGED_WIRELESS) else 0
        return runRootCommand("settings put global stay_on_while_plugged_in $value").success
    }

    // --- מצב מיקוד / שעות שקט (AlarmManager, ללא צורך בהרשאת אזעקה מדויקת) ---

    private val alarmManager get() = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager

    private fun focusPendingIntent(action: String, requestCode: Int): android.app.PendingIntent {
        val intent = Intent(context, com.future.settings.receiver.FocusActionReceiver::class.java).apply { this.action = action }
        return android.app.PendingIntent.getBroadcast(
            context, requestCode, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    /** משתיק צלילים למשך [minutes] דקות, ומתזמן חזרה אוטומטית למצב רגיל - "מיקוד עכשיו". */
    fun scheduleFocusNow(minutes: Int): Boolean {
        val succeeded = setRingerMode(AudioManager.RINGER_MODE_SILENT)
        if (!succeeded) return false
        val triggerAt = System.currentTimeMillis() + minutes * 60_000L
        alarmManager.setAndAllowWhileIdle(
            android.app.AlarmManager.RTC_WAKEUP, triggerAt,
            focusPendingIntent(com.future.settings.receiver.FocusActionReceiver.ACTION_FOCUS_REVERT, 1001)
        )
        return true
    }

    fun cancelFocusNow() {
        alarmManager.cancel(focusPendingIntent(com.future.settings.receiver.FocusActionReceiver.ACTION_FOCUS_REVERT, 1001))
        setRingerMode(AudioManager.RINGER_MODE_NORMAL)
    }

    fun isSleepModeEnabled(): Boolean = prefsBoolean("sleep_mode_enabled", false)

    fun setSleepModeEnabled(enabled: Boolean) {
        setPrefsBoolean("sleep_mode_enabled", enabled)
        val startPi = focusPendingIntent(com.future.settings.receiver.FocusActionReceiver.ACTION_SLEEP_START, 1002)
        val endPi = focusPendingIntent(com.future.settings.receiver.FocusActionReceiver.ACTION_SLEEP_END, 1003)
        if (enabled) {
            alarmManager.setRepeating(android.app.AlarmManager.RTC_WAKEUP, nextTriggerFor(23, 0), android.app.AlarmManager.INTERVAL_DAY, startPi)
            alarmManager.setRepeating(android.app.AlarmManager.RTC_WAKEUP, nextTriggerFor(7, 0), android.app.AlarmManager.INTERVAL_DAY, endPi)
        } else {
            alarmManager.cancel(startPi)
            alarmManager.cancel(endPi)
        }
    }

    private fun nextTriggerFor(hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance()
        cal.set(java.util.Calendar.HOUR_OF_DAY, hour)
        cal.set(java.util.Calendar.MINUTE, minute)
        cal.set(java.util.Calendar.SECOND, 0)
        cal.set(java.util.Calendar.MILLISECOND, 0)
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis
    }

    private fun prefs() = context.getSharedPreferences("system_interactor_prefs", Context.MODE_PRIVATE)
    private fun prefsBoolean(key: String, default: Boolean) = prefs().getBoolean(key, default)
    private fun setPrefsBoolean(key: String, value: Boolean) = prefs().edit().putBoolean(key, value).apply()

    // --- מצב מפתחים (הקשה 7 פעמים על מספר הבנייה, כמו באנדרואיד המקורי) ---

    fun isDeveloperModeEnabled(): Boolean =
        Settings.Global.getInt(contentResolver, Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0) != 0

    fun enableDeveloperMode() {
        runRootCommand("settings put global development_settings_enabled 1")
    }

    // --- זום מסך (צפיפות תצוגה) ---
    // אין API ציבורי לשינוי צפיפות התצוגה בזמן ריצה - "wm density" זו אותה פקודה
    // ש-adb shell משתמש בה, וזמינה לנו כי יש לאפליקציה גישת root ממילא.

    private val defaultDensityDpi: Int by lazy { context.resources.displayMetrics.densityDpi }

    fun getDisplayDensityDpi(): Int = context.resources.displayMetrics.densityDpi

    fun getDefaultDisplayDensityDpi(): Int = defaultDensityDpi

    fun setDisplayDensityDpi(dpi: Int): Boolean {
        return runRootCommand("wm density $dpi").success
    }

    fun resetDisplayDensity(): Boolean {
        return runRootCommand("wm density reset").success
    }

    // --- חלון מרובה (Multi Window) ---
    // force_resizable_activities מכריח את כל האפליקציות להיות ניתנות לשינוי גודל,
    // גם אם לא הוצהרו ככאלה - זו הדרך האמיתית ש-Developer Options עצמו משתמש בה.

    fun isMultiWindowForced(): Boolean =
        Settings.Global.getInt(contentResolver, "force_resizable_activities", 0) != 0

    fun setMultiWindowForced(enabled: Boolean): Boolean {
        return runRootCommand("settings put global force_resizable_activities ${if (enabled) 1 else 0}").success
    }

    // --- השתקת סאונד לפי אפליקציה (App Ops: PLAY_AUDIO) ---
    // אין API רשמי ל"עוצמה נפרדת לכל אפליקציה" באנדרואיד הציבורי - אבל app-op
    // PLAY_AUDIO הוא מנגנון אמיתי שמשתיק לגמרי את הפלט האודיו של אפליקציה ספציפית,
    // וזו בדיוק אותה טכניקה שבה שירותי "השתקת אפליקציה" מבוססי-root משתמשים בפועל.

    fun isAppAudioMuted(packageName: String): Boolean {
        return try {
            val result = runRootCommand("appops get $packageName PLAY_AUDIO").output.trim()
            result.contains("ignore", ignoreCase = true) || result.contains("deny", ignoreCase = true)
        } catch (e: Exception) {
            false
        }
    }

    fun setAppAudioMuted(packageName: String, muted: Boolean): Boolean {
        return runRootCommand("appops set $packageName PLAY_AUDIO ${if (muted) "ignore" else "allow"}").success
    }

    // --- ניהול כרטיסי SIM ---
    // שינוי שם/צבע תצוגה של קו לא נתמך ע"י ה-API הפומבי (זה שדה פנימי בטבלת ה-
    // Telephony provider) - נשמר מקומית באפליקציה לפי subscriptionId. קביעת ברירת
    // המחדל לנתונים/שיחות/SMS משתמשת במפתחות ה-Settings.Global הוותיקים שהטלפוניה
    // עדיין קוראת בפועל במכשירים מבוססי-root, באותו סגנון כמו שאר הפונקציות כאן.

    data class SimEntry(
        val subscriptionId: Int,
        val slotIndex: Int,
        val carrierName: String,
        val phoneNumber: String?,
        val displayName: String,
        val colorArgb: Int
    )

    private val subscriptionManager by lazy {
        context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
    }

    fun getSimCards(): List<SimEntry> {
        return try {
            val list = subscriptionManager.activeSubscriptionInfoList ?: return emptyList()
            list.map { info ->
                val subId = info.subscriptionId
                val savedName = prefs().getString("sim_name_$subId", null)
                val savedColor = prefs().getInt("sim_color_$subId", info.iconTint)
                val number = try {
                    if (Build.VERSION.SDK_INT >= 33) subscriptionManager.getPhoneNumber(subId).ifBlank { null }
                    else @Suppress("DEPRECATION") info.number?.ifBlank { null }
                } catch (e: Exception) { null }
                SimEntry(
                    subscriptionId = subId,
                    slotIndex = info.simSlotIndex,
                    carrierName = info.carrierName?.toString() ?: "לא ידוע",
                    phoneNumber = number,
                    displayName = savedName ?: info.displayName?.toString() ?: "קו ${info.simSlotIndex + 1}",
                    colorArgb = savedColor
                )
            }
        } catch (e: SecurityException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun setSimDisplayName(subscriptionId: Int, name: String) {
        prefs().edit().putString("sim_name_$subscriptionId", name).apply()
    }

    fun setSimColor(subscriptionId: Int, colorArgb: Int) {
        prefs().edit().putInt("sim_color_$subscriptionId", colorArgb).apply()
    }

    fun getDefaultDataSubscriptionId(): Int = SubscriptionManager.getDefaultDataSubscriptionId()
    fun getDefaultVoiceSubscriptionId(): Int = SubscriptionManager.getDefaultVoiceSubscriptionId()
    fun getDefaultSmsSubscriptionId(): Int = SubscriptionManager.getDefaultSmsSubscriptionId()

    fun setDefaultDataSim(subscriptionId: Int) {
        runRootCommand("settings put global multi_sim_data_call $subscriptionId")
    }

    fun setDefaultVoiceSim(subscriptionId: Int) {
        runRootCommand("settings put global multi_sim_voice_call $subscriptionId")
    }

    fun setDefaultSmsSim(subscriptionId: Int) {
        runRootCommand("settings put global multi_sim_sms $subscriptionId")
    }

    // --- IMEI, מספר טלפון ומידע רגולטורי (מסך "אודות") ---
    // IMEI חסום מ-API 29 ומעלה גם עם READ_PHONE_STATE רגיל (צריך READ_PRIVILEGED_
    // PHONE_STATE של אפליקציית מערכת) - כאן זה עוקף את זה ע"י הענקת ההרשאה בעצמנו
    // דרך root לפני הקריאה, באותו דפוס בדיוק שכבר קיים ב-initSystemPermissions().

    fun ensurePhoneStatePermission() {
        val packageName = context.packageName
        runRootCommands(
            listOf(
                "pm grant $packageName android.permission.READ_PHONE_STATE",
                "pm grant $packageName android.permission.READ_PHONE_NUMBERS"
            )
        )
    }

    fun getImei(): String? {
        return try {
            @Suppress("DEPRECATION")
            if (Build.VERSION.SDK_INT >= 26) telephonyManager.imei else telephonyManager.deviceId
        } catch (e: Exception) {
            null
        }
    }

    fun getPrimaryPhoneNumber(): String? {
        return try {
            if (Build.VERSION.SDK_INT >= 33) {
                val defaultSub = SubscriptionManager.getDefaultSubscriptionId()
                subscriptionManager.getPhoneNumber(defaultSub).ifBlank { null }
            } else {
                @Suppress("DEPRECATION") telephonyManager.line1Number?.ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }

    fun openRegulatoryInfo() {
        try {
            val intent = Intent(Settings.ACTION_SHOW_REGULATORY_INFO)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {}
    }

    // --- מידע סוללה מורחב (בריאות ארוכת-טווח) ---
    // מספר מחזורי טעינה זמין רק מ-Android 14 (API 34) ומעלה כ-extra על ה-sticky
    // broadcast; במכשירים ישנים יותר פשוט אין לזה API ציבורי, אז מוצג null בכנות
    // במקום מספר מזויף.

    private val batteryManager by lazy { context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager }

    data class BatteryExtendedInfo(val cycleCount: Int?, val chargeCounterMah: Int?, val currentNowMa: Int?)

    fun getBatteryExtendedInfo(): BatteryExtendedInfo {
        val cycleCount = try {
            if (Build.VERSION.SDK_INT >= 34) {
                val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                val c = intent?.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1) ?: -1
                if (c >= 0) c else null
            } else null
        } catch (e: Exception) { null }
        val chargeCounter = try {
            val uAh = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
            if (uAh > 0) uAh / 1000 else null
        } catch (e: Exception) { null }
        val currentNow = try {
            val uA = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
            if (uA != Int.MIN_VALUE) uA / 1000 else null
        } catch (e: Exception) { null }
        return BatteryExtendedInfo(cycleCount, chargeCounter, currentNow)
    }

    // --- טיימרים לאפליקציות ---
    // אין שירות אכיפה ברקע מלא (WorkManager) בפרויקט הזה - האכיפה קורית בכל בדיקה
    // יזומה (בטעינת מסך הטיימרים) ובנוסף ע"י AppTimerCheckReceiver שמתוזמן חוזר
    // כל 15 דקות דרך AlarmManager, באותו דפוס בדיוק כמו מצב השינה/מיקוד למעלה.

    data class AppTimer(val packageName: String, val dailyLimitMinutes: Int)

    fun getAppTimers(): List<AppTimer> {
        val raw = prefs().getStringSet("app_timers", emptySet()) ?: emptySet()
        return raw.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2) {
                val minutes = parts[1].toIntOrNull() ?: return@mapNotNull null
                AppTimer(parts[0], minutes)
            } else null
        }
    }

    fun setAppTimer(packageName: String, dailyLimitMinutes: Int) {
        val current = getAppTimers().filter { it.packageName != packageName }.toMutableList()
        current.add(AppTimer(packageName, dailyLimitMinutes))
        prefs().edit().putStringSet("app_timers", current.map { "${it.packageName}|${it.dailyLimitMinutes}" }.toSet()).apply()
    }

    fun removeAppTimer(packageName: String) {
        val current = getAppTimers().filter { it.packageName != packageName }
        prefs().edit().putStringSet("app_timers", current.map { "${it.packageName}|${it.dailyLimitMinutes}" }.toSet()).apply()
    }

    /** בודק את כל הטיימרים מול השימוש היומי בפועל ועוצר בכפייה כל אפליקציה שחרגה
     *  ממכסתה - מחזיר את רשימת שמות החבילות שנעצרו כדי שאפשר יהיה להציג הודעה. */
    fun checkAndEnforceAppTimers(): List<String> {
        val timers = getAppTimers()
        if (timers.isEmpty()) return emptyList()
        val usage = getTodayScreenTime().topApps.associateBy { it.packageName }
        val exceeded = timers.filter { timer ->
            val usedMinutes = (usage[timer.packageName]?.millis ?: 0L) / 60000
            usedMinutes >= timer.dailyLimitMinutes
        }
        if (exceeded.isNotEmpty()) {
            runRootCommands(exceeded.map { "am force-stop ${it.packageName}" })
        }
        return exceeded.map { it.packageName }
    }

    fun scheduleAppTimerChecks() {
        val pi = appTimerPendingIntent()
        alarmManager.setInexactRepeating(
            android.app.AlarmManager.RTC, System.currentTimeMillis() + 15 * 60_000L,
            15 * 60_000L, pi
        )
    }

    fun cancelAppTimerChecks() {
        alarmManager.cancel(appTimerPendingIntent())
    }

    private fun appTimerPendingIntent(): android.app.PendingIntent {
        val intent = Intent(context, com.future.settings.receiver.AppTimerCheckReceiver::class.java)
        return android.app.PendingIntent.getBroadcast(
            context, 2001, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
    }

    // --- בטיחות וחירום (SOS) ---
    // ללא Accessibility Service שמאזין ללחיצות פיזיות על כפתור הצד (זה שייך ל-
    // FutureUI, לא לאפליקציית ההגדרות) - הטריגר כאן הוא כפתור בתוך המסך, לא רצף
    // לחיצות חומרה כמו ב-One UI. עדיין שולח מיקום אמיתי ו-SMS אמיתי.

    fun getEmergencyContact(): Pair<String, String>? {
        val name = prefs().getString("sos_contact_name", null) ?: return null
        val phone = prefs().getString("sos_contact_phone", null) ?: return null
        return name to phone
    }

    fun setEmergencyContact(name: String, phone: String) {
        prefs().edit().putString("sos_contact_name", name).putString("sos_contact_phone", phone).apply()
    }

    fun clearEmergencyContact() {
        prefs().edit().remove("sos_contact_name").remove("sos_contact_phone").apply()
    }

    fun ensureSosPermissions() {
        val packageName = context.packageName
        runRootCommands(
            listOf(
                "pm grant $packageName android.permission.SEND_SMS",
                "pm grant $packageName android.permission.ACCESS_FINE_LOCATION",
                "pm grant $packageName android.permission.ACCESS_COARSE_LOCATION"
            )
        )
    }

    private val locationManagerForSos by lazy { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    fun getLastKnownLocation(): Location? {
        return try {
            val providers = locationManagerForSos.getProviders(true)
            providers.mapNotNull { locationManagerForSos.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun sendEmergencySms(phoneNumber: String, message: String): Boolean {
        return try {
            val smsManager = if (Build.VERSION.SDK_INT >= 31) context.getSystemService(SmsManager::class.java)
                else @Suppress("DEPRECATION") SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            true
        } catch (e: Exception) {
            false
        }
    }

    // --- אבחון מכשיר (מגע, רטט, רמקול, חיישנים) ---

    private val vibrator by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        }
    }

    fun testVibration() {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION") vibrator.vibrate(300)
            }
        } catch (e: Exception) {}
    }

    private var toneGenerator: ToneGenerator? = null

    fun testSpeakerTone() {
        try {
            toneGenerator?.release()
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 90)
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 700)
        } catch (e: Exception) {}
    }

    fun getSensorManager(): android.hardware.SensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as android.hardware.SensorManager
}

/** רשימת שמות החבילות של כל אפליקציות FutureOS - מקור אמת יחיד המשותף בין
 *  מסך "אפליקציות" הקיים לבין ניקוי ה-RAM החדש, כדי לא לשכפל את הרשימה. */
val FUTURE_OS_PACKAGES = listOf(
    "com.future.futureui", "com.future.futurelauncher", "com.future.dialer", "com.future.messages",
    "com.future.contact", "com.future.files", "com.future.gallery", "com.future.terminal",
    "com.future.tools", "com.future.music", "com.future.notes", "com.future.gotitdone",
    "com.future.mikdash", "com.future.sfarim"
)
