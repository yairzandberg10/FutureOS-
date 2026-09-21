package com.future.bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * עטיפה סביב BluetoothAdapter הקלאסי (סריקה + זיווג) - לא BLE/GATT (זה כבר
 * קיים ב-Fitness/HeartRateMonitor לחיישני דופק ייעודיים). מטרת האפליקציה
 * הזו היא ניהול כללי של מכשירי בלוטוס - בדיוק כמו מסך "בלוטוס" בהגדרות של
 * טלפון רגיל: הפעלה, סריקה למכשירים זמינים, זיווג/ביטול זיווג.
 */
@SuppressLint("MissingPermission") // הקוד הקורא (BluetoothScreen/MainActivity) אחראי לבקש BLUETOOTH_SCAN/BLUETOOTH_CONNECT לפני שימוש בכל פונקציה כאן
class BluetoothController(private val context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun isSupported(): Boolean = adapter != null

    var isEnabled by mutableStateOf(adapter?.isEnabled == true)
        private set
    var isScanning by mutableStateOf(false)
        private set
    val pairedDevices = mutableStateListOf<BluetoothDeviceInfo>()
    val discoveredDevices = mutableStateListOf<BluetoothDeviceInfo>()

    private var receiver: BroadcastReceiver? = null

    /** נקרא אחרי שהרשאות אושרו, אחרי חזרה מ-ACTION_REQUEST_ENABLE, ובכל ON_RESUME -
     * מרענן גם את מצב ההפעלה וגם את רשימת המכשירים המזווגים (השניים יכולים
     * להשתנות מחוץ לאפליקציה, למשל דרך הגדרות המערכת). */
    fun refreshState() {
        isEnabled = adapter?.isEnabled == true
        refreshPairedDevices()
    }

    fun refreshPairedDevices() {
        pairedDevices.clear()
        adapter?.bondedDevices?.forEach { device -> pairedDevices.add(device.toInfo()) }
    }

    fun startDiscovery() {
        val a = adapter ?: return
        if (!a.isEnabled) return
        discoveredDevices.clear()
        if (a.isDiscovering) a.cancelDiscovery()
        isScanning = a.startDiscovery()
    }

    fun stopDiscovery() {
        adapter?.let { if (it.isDiscovering) it.cancelDiscovery() }
        isScanning = false
    }

    fun pair(address: String) {
        val device = remoteDevice(address) ?: return
        device.createBond()
    }

    /** אין API ציבורי לביטול זיווג באנדרואיד - removeBond חשוף רק דרך
     * reflection. דפוס מוכר וידוע (נמצא כך גם באפליקציות מערכת בלוטוס
     * אחרות); נכשל בשקט (מחזיר false) אם ה-OEM חוסם את הקריאה. */
    fun unpair(address: String): Boolean {
        val device = remoteDevice(address) ?: return false
        return try {
            val method = device.javaClass.getMethod("removeBond")
            method.invoke(device) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }
    }

    fun requestEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun register() {
        if (receiver != null) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        val r = object : BroadcastReceiver() {
            @Suppress("DEPRECATION")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        val info = device?.toInfo()
                        if (info != null && !info.isBonded && discoveredDevices.none { it.address == info.address }) {
                            discoveredDevices.add(info)
                        }
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> isScanning = true
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> isScanning = false
                    BluetoothAdapter.ACTION_STATE_CHANGED -> isEnabled = adapter?.isEnabled == true
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> refreshPairedDevices()
                }
            }
        }
        receiver = r
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(r, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(r, filter)
        }
    }

    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // כבר לא רשום - לא קרה כלום
            }
        }
        receiver = null
    }

    private fun remoteDevice(address: String): BluetoothDevice? =
        try {
            adapter?.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun BluetoothDevice.toInfo(): BluetoothDeviceInfo {
        val deviceName = try {
            name
        } catch (e: SecurityException) {
            null
        } ?: address
        return BluetoothDeviceInfo(deviceName, address, bondState == BluetoothDevice.BOND_BONDED)
    }
}
