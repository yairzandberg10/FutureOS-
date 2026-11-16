package com.future.systemui.statusbar

import android.app.AlarmManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyDisplayInfo
import android.telephony.TelephonyManager
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Everything the bar shows about the device, kept in Compose state so the bar redraws on change.
 *
 * All of it comes from the real system services (telephony, connectivity, audio, power...) as
 * events - no polling. This process is the platform-signed SystemUI, so the callbacks that a normal
 * app cannot use (signal strength, call state, display network type) are available here.
 */
internal class DeviceStatusMonitor(private val context: Context) {
    var time by mutableStateOf("")
        private set
    var batteryPercent by mutableIntStateOf(100)
        private set
    var charging by mutableStateOf(false)
        private set
    var batterySaver by mutableStateOf(false)
        private set

    /** Wi-Fi is associated (not merely switched on). */
    var wifiConnected by mutableStateOf(false)
        private set
    /** 0..[MAX_LEVEL]. */
    var wifiLevel by mutableIntStateOf(0)
        private set

    var simPresent by mutableStateOf(true)
        private set
    var cellInService by mutableStateOf(false)
        private set
    /** 0..[MAX_LEVEL]. */
    var cellLevel by mutableIntStateOf(0)
        private set
    /** "5G", "4G+", "4G", "3G", "H+", "E"... empty when unknown. */
    var cellType by mutableStateOf("")
        private set
    /** A cellular data network is actually up. */
    var cellularData by mutableStateOf(false)
        private set

    var airplane by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var bluetoothConnected by mutableStateOf(false)
        private set
    var ringerMode by mutableIntStateOf(AudioManager.RINGER_MODE_NORMAL)
        private set
    var dnd by mutableStateOf(false)
        private set
    var alarmSet by mutableStateOf(false)
        private set
    var wiredHeadset by mutableStateOf(false)
        private set

    /** elapsedRealtime when the current call went off-hook, 0 when there is no call. */
    var callStartedAt by mutableLongStateOf(0L)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var receiver: BroadcastReceiver? = null
    private var cellularCallback: ConnectivityManager.NetworkCallback? = null
    private var wifiCallback: ConnectivityManager.NetworkCallback? = null
    private var telephonyCallback: TelephonyCallback? = null
    private var audioCallback: AudioDeviceCallback? = null
    private val cellularNetworks = HashSet<Network>()
    private val wifiNetworks = HashSet<Network>()

    private val telephony get() = context.getSystemService(TelephonyManager::class.java)
    private val wifiManager get() = context.getSystemService(WifiManager::class.java)
    private val audio get() = context.getSystemService(AudioManager::class.java)

    fun start() {
        safe("time") { refreshTime() }
        safe("receiver") { registerReceiver() }
        safe("ringer") { refreshRinger() }
        safe("dnd") { refreshDnd() }
        safe("alarm") { refreshAlarm() }
        safe("airplane") { refreshAirplane() }
        safe("power") { refreshPowerSave() }
        safe("bluetooth") { refreshBluetooth() }
        safe("sim") { refreshSim() }
        safe("wifi") { startWifiWatch() }
        safe("cellular") { startCellularWatch() }
        safe("telephony") { startTelephonyWatch() }
        safe("audio devices") { startAudioDeviceWatch() }
    }

    fun stop() {
        runCatching { receiver?.let { context.unregisterReceiver(it) } }
        receiver = null
        val cm = context.getSystemService(ConnectivityManager::class.java)
        runCatching { cellularCallback?.let { cm?.unregisterNetworkCallback(it) } }
        runCatching { wifiCallback?.let { cm?.unregisterNetworkCallback(it) } }
        cellularCallback = null
        wifiCallback = null
        runCatching { telephonyCallback?.let { telephony?.unregisterTelephonyCallback(it) } }
        telephonyCallback = null
        runCatching { audioCallback?.let { audio?.unregisterAudioDeviceCallback(it) } }
        audioCallback = null
    }

    // ---------------------------------------------------------------- broadcasts

    private fun registerReceiver() {
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                safe("receiver ${intent?.action}") {
                    when (intent?.action) {
                        Intent.ACTION_BATTERY_CHANGED -> applyBattery(intent)
                        Intent.ACTION_TIMEZONE_CHANGED,
                        Intent.ACTION_LOCALE_CHANGED,
                        Intent.ACTION_TIME_TICK,
                        Intent.ACTION_TIME_CHANGED -> refreshTime()
                        PowerManager.ACTION_POWER_SAVE_MODE_CHANGED -> refreshPowerSave()
                        AudioManager.RINGER_MODE_CHANGED_ACTION -> refreshRinger()
                        NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> refreshDnd()
                        AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED -> refreshAlarm()
                        Intent.ACTION_AIRPLANE_MODE_CHANGED -> refreshAirplane()
                        BluetoothAdapter.ACTION_STATE_CHANGED,
                        BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED -> refreshBluetooth()
                        ACTION_SIM_STATE_CHANGED -> refreshSim()
                    }
                }
            }
        }
        receiver = r
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_LOCALE_CHANGED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_SIM_STATE_CHANGED)
        }
        // ACTION_BATTERY_CHANGED is sticky, so the first value arrives immediately.
        applyBattery(context.registerReceiver(r, filter))
    }

    /**
     * The status bar never shows AM/PM (like stock Android): on a 320dp-wide bar "לפנה״צ" takes
     * the room of three notification icons.
     */
    private fun refreshTime() {
        val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
        time = SimpleDateFormat(pattern, Locale.getDefault()).apply { timeZone = TimeZone.getDefault() }.format(Date())
    }

    private fun applyBattery(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) batteryPercent = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun refreshPowerSave() {
        batterySaver = context.getSystemService(PowerManager::class.java)?.isPowerSaveMode == true
    }

    private fun refreshRinger() {
        ringerMode = audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    }

    private fun refreshDnd() {
        val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        dnd = filter != null && filter != NotificationManager.INTERRUPTION_FILTER_ALL &&
            filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
    }

    private fun refreshAlarm() {
        alarmSet = context.getSystemService(AlarmManager::class.java)?.nextAlarmClock != null
    }

    private fun refreshAirplane() {
        airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0
    }

    private fun refreshBluetooth() {
        val adapter = try {
            context.getSystemService(BluetoothManager::class.java)?.adapter
        } catch (_: Throwable) { null }
        bluetoothOn = try { adapter?.isEnabled == true } catch (_: Throwable) { false }
        bluetoothConnected = bluetoothOn && try {
            BT_PROFILES.any { adapter?.getProfileConnectionState(it) == BluetoothProfile.STATE_CONNECTED }
        } catch (_: Throwable) { false }
    }

    private fun refreshSim() {
        val state = try { telephony?.simState } catch (_: Throwable) { null } ?: return
        // UNKNOWN is the boot/transition state - only a definite ABSENT shows the no-SIM icon.
        simPresent = state != TelephonyManager.SIM_STATE_ABSENT
    }

    // ---------------------------------------------------------------- networks

    private fun startWifiWatch() {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        // FLAG_INCLUDE_LOCATION_INFO is not needed: only the RSSI is read, never the SSID.
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                wifiNetworks.add(network)
                wifiConnected = true
                val rssi = caps.signalStrength
                if (rssi != NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) wifiLevel = wifiLevelFor(rssi)
            }

            override fun onLost(network: Network) {
                wifiNetworks.remove(network)
                wifiConnected = wifiNetworks.isNotEmpty()
                if (!wifiConnected) wifiLevel = 0
            }
        }
        wifiCallback = cb
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build(), cb, main,
        )
    }

    private fun wifiLevelFor(rssi: Int): Int {
        val wm = wifiManager ?: return 0
        val max = wm.maxSignalLevel.coerceAtLeast(1)
        return (wm.calculateSignalLevel(rssi) * MAX_LEVEL + max / 2) / max
    }

    private fun startCellularWatch() {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { cellularNetworks.add(network); cellularData = true }
            override fun onLost(network: Network) { cellularNetworks.remove(network); cellularData = cellularNetworks.isNotEmpty() }
        }
        cellularCallback = cb
        cm.registerNetworkCallback(
            NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR).build(), cb, main,
        )
    }

    // ---------------------------------------------------------------- telephony

    private var dataNetworkType = TelephonyManager.NETWORK_TYPE_UNKNOWN
    private var overrideType = TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NONE

    private inner class Telephony :
        TelephonyCallback(),
        TelephonyCallback.SignalStrengthsListener,
        TelephonyCallback.ServiceStateListener,
        TelephonyCallback.DisplayInfoListener,
        TelephonyCallback.CallStateListener {

        override fun onSignalStrengthsChanged(signalStrength: SignalStrength) = safe("signal") {
            cellLevel = signalStrength.level.coerceIn(0, MAX_LEVEL)
        }

        override fun onServiceStateChanged(serviceState: ServiceState) = safe("service") {
            cellInService = serviceState.state == ServiceState.STATE_IN_SERVICE
            refreshSim()
        }

        override fun onDisplayInfoChanged(telephonyDisplayInfo: TelephonyDisplayInfo) = safe("display info") {
            dataNetworkType = telephonyDisplayInfo.networkType
            overrideType = telephonyDisplayInfo.overrideNetworkType
            cellType = typeLabel(dataNetworkType, overrideType)
        }

        override fun onCallStateChanged(state: Int) = safe("call state") {
            callStartedAt = when (state) {
                TelephonyManager.CALL_STATE_OFFHOOK -> callStartedAt.takeIf { it != 0L } ?: SystemClock.elapsedRealtime()
                else -> 0L
            }
        }
    }

    private fun startTelephonyWatch() {
        val tm = telephony ?: return
        val cb = Telephony()
        telephonyCallback = cb
        tm.registerTelephonyCallback(context.mainExecutor, cb)
    }

    private fun typeLabel(network: Int, override: Int): String = when (override) {
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_NSA_MMWAVE,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_NR_ADVANCED -> "5G"
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_ADVANCED_PRO,
        TelephonyDisplayInfo.OVERRIDE_NETWORK_TYPE_LTE_CA -> "4G+"
        else -> when (network) {
            TelephonyManager.NETWORK_TYPE_NR -> "5G"
            TelephonyManager.NETWORK_TYPE_LTE, TelephonyManager.NETWORK_TYPE_IWLAN -> "4G"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "H+"
            TelephonyManager.NETWORK_TYPE_HSDPA, TelephonyManager.NETWORK_TYPE_HSUPA,
            TelephonyManager.NETWORK_TYPE_HSPA -> "H"
            TelephonyManager.NETWORK_TYPE_UMTS, TelephonyManager.NETWORK_TYPE_TD_SCDMA,
            TelephonyManager.NETWORK_TYPE_EVDO_0, TelephonyManager.NETWORK_TYPE_EVDO_A,
            TelephonyManager.NETWORK_TYPE_EVDO_B, TelephonyManager.NETWORK_TYPE_EHRPD -> "3G"
            TelephonyManager.NETWORK_TYPE_EDGE -> "E"
            TelephonyManager.NETWORK_TYPE_GPRS, TelephonyManager.NETWORK_TYPE_GSM,
            TelephonyManager.NETWORK_TYPE_CDMA, TelephonyManager.NETWORK_TYPE_1xRTT -> "G"
            else -> ""
        }
    }

    // ---------------------------------------------------------------- wired headset

    private fun startAudioDeviceWatch() {
        val am = audio ?: return
        val cb = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) = refreshHeadset()
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) = refreshHeadset()
        }
        audioCallback = cb
        am.registerAudioDeviceCallback(cb, main)
        refreshHeadset()
    }

    private fun refreshHeadset() = safe("headset") {
        val outputs = audio?.getDevices(AudioManager.GET_DEVICES_OUTPUTS) ?: emptyArray()
        wiredHeadset = outputs.any { it.type in WIRED_TYPES }
    }

    private fun safe(what: String, block: () -> Unit) {
        try { block() } catch (t: Throwable) { Log.e(TAG, "$what failed", t) }
    }

    companion object {
        /** Signal levels are shown on a 0..4 scale (SignalStrength.getLevel()'s range). */
        const val MAX_LEVEL = 4

        private const val TAG = "FutureSystemUI"
        /** TelephonyIntents.ACTION_SIM_STATE_CHANGED (hidden constant, stable string). */
        private const val ACTION_SIM_STATE_CHANGED = "android.intent.action.SIM_STATE_CHANGED"

        private val BT_PROFILES = intArrayOf(BluetoothProfile.HEADSET, BluetoothProfile.A2DP, BluetoothProfile.HEARING_AID)
        private val WIRED_TYPES = setOf(
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET,
        )
    }
}
