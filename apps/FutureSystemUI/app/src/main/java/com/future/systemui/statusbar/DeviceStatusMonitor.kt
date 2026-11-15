package com.future.systemui.statusbar

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Clock, battery, Wi-Fi, Bluetooth and cellular state, kept in Compose state so the bar redraws on change. */
internal class DeviceStatusMonitor(private val context: Context) {
    var time by mutableStateOf("")
        private set
    var batteryPercent by mutableIntStateOf(100)
        private set
    var charging by mutableStateOf(false)
        private set
    var wifiOn by mutableStateOf(false)
        private set
    var bluetoothOn by mutableStateOf(false)
        private set
    var cellular by mutableStateOf(false)
        private set

    private val main = Handler(Looper.getMainLooper())
    private var timeFormat = newFormat()
    private var receiver: BroadcastReceiver? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private val cellularNetworks = HashSet<Network>()

    fun start() {
        try { refreshTime() } catch (t: Throwable) { Log.e(TAG, "time init failed", t) }
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                try {
                    when (intent?.action) {
                        Intent.ACTION_BATTERY_CHANGED -> applyBattery(intent)
                        Intent.ACTION_TIMEZONE_CHANGED,
                        Intent.ACTION_LOCALE_CHANGED,
                        Intent.ACTION_TIME_TICK,
                        Intent.ACTION_TIME_CHANGED -> refreshTime()
                        WifiManager.WIFI_STATE_CHANGED_ACTION -> refreshWifi()
                        BluetoothAdapter.ACTION_STATE_CHANGED -> refreshBluetooth()
                    }
                } catch (t: Throwable) { Log.e(TAG, "receiver failed", t) }
            }
        }
        receiver = r
        try {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_TIME_TICK)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
                addAction(Intent.ACTION_LOCALE_CHANGED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            // ACTION_BATTERY_CHANGED is sticky, so the first value arrives immediately.
            applyBattery(context.registerReceiver(r, filter))
        } catch (t: Throwable) { Log.e(TAG, "registerReceiver failed", t) }
        try { refreshWifi() } catch (t: Throwable) { Log.e(TAG, "wifi init failed", t) }
        try { refreshBluetooth() } catch (t: Throwable) { Log.e(TAG, "bluetooth init failed", t) }
        startCellularWatch()
    }

    fun stop() {
        try { receiver?.let { context.unregisterReceiver(it) } } catch (_: Throwable) {}
        receiver = null
        try {
            networkCallback?.let { context.getSystemService(ConnectivityManager::class.java)?.unregisterNetworkCallback(it) }
        } catch (_: Throwable) {}
        networkCallback = null
    }

    private fun newFormat(): SimpleDateFormat =
        SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", Locale.getDefault())
            .apply { timeZone = TimeZone.getDefault() }

    private fun refreshTime() {
        timeFormat = newFormat()
        time = timeFormat.format(Date())
    }

    private fun applyBattery(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level >= 0 && scale > 0) batteryPercent = (level * 100) / scale
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun refreshWifi() {
        val wm = context.getSystemService(WifiManager::class.java)
        wifiOn = wm?.isWifiEnabled == true
    }

    private fun refreshBluetooth() {
        bluetoothOn = try { BluetoothAdapter.getDefaultAdapter()?.isEnabled == true } catch (_: Throwable) { false }
    }

    private fun startCellularWatch() {
        try {
            val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { cellularNetworks.add(network); cellular = true }
                override fun onLost(network: Network) { cellularNetworks.remove(network); cellular = cellularNetworks.isNotEmpty() }
            }
            networkCallback = cb
            val request = NetworkRequest.Builder()
                .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
                .build()
            cm.registerNetworkCallback(request, cb, main)
        } catch (t: Throwable) { Log.e(TAG, "cellular watch failed", t) }
    }

    private companion object { const val TAG = "FutureSystemUI" }
}
