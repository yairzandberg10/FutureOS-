package com.future.fitness.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

data class FoundDevice(val name: String, val address: String)

enum class HrConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

/** לקוח BLE GATT סטנדרטי ל-Heart Rate Service (0x180D) / Heart Rate
 * Measurement (0x2A37) - הפרוטוקול הסטנדרטי שכל שעון חכם/רצועת דופק תומכת
 * בו (Bluetooth SIG), לא SDK ספציפי ליצרן. פענוח הערך לפי המפרט: ביט 0
 * בדגלים קובע פורמט UINT8/UINT16. שומר את המכשיר האחרון שהתחבר אליו כדי
 * להתחבר אוטומטית בפעם הבאה (WorkoutStore.getPairedDevice). */
@SuppressLint("MissingPermission") // הקוד הקורא אחראי לבקש BLUETOOTH_SCAN/BLUETOOTH_CONNECT לפני שימוש
class HeartRateMonitor(private val context: Context) {
    var state by mutableStateOf(HrConnectionState.DISCONNECTED)
        private set
    var connectedDeviceName by mutableStateOf<String?>(null)
        private set
    var currentBpm by mutableStateOf<Int?>(null)
        private set
    val foundDevices = mutableStateListOf<FoundDevice>()

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var gatt: BluetoothGatt? = null

    fun isBluetoothAvailable(): Boolean = adapter != null

    fun startScan() {
        val scanner = adapter?.bluetoothLeScanner ?: return
        foundDevices.clear()
        state = HrConnectionState.SCANNING
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(HEART_RATE_SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(listOf(filter), settings, scanCallback)
        } catch (e: SecurityException) {
            state = HrConnectionState.DISCONNECTED
        }
    }

    fun stopScan() {
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // אין הרשאה/Bluetooth כבוי - אין מה לעצור
        }
        if (state == HrConnectionState.SCANNING) state = HrConnectionState.DISCONNECTED
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            val name = try { device.name } catch (e: SecurityException) { null } ?: "מכשיר לא ידוע"
            if (foundDevices.none { it.address == device.address }) {
                foundDevices.add(FoundDevice(name, device.address))
            }
        }
    }

    fun connect(address: String) {
        val device = try { adapter?.getRemoteDevice(address) } catch (e: IllegalArgumentException) { null } ?: return
        stopScan()
        state = HrConnectionState.CONNECTING
        gatt = device.connectGatt(context, false, gattCallback)
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        state = HrConnectionState.DISCONNECTED
        connectedDeviceName = null
        currentBpm = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectedDeviceName = try { g.device.name } catch (e: SecurityException) { null } ?: g.device.address
                    g.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    state = HrConnectionState.DISCONNECTED
                    connectedDeviceName = null
                    currentBpm = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            val service = g.getService(HEART_RATE_SERVICE_UUID) ?: return
            val characteristic = service.getCharacteristic(HEART_RATE_MEASUREMENT_UUID) ?: return
            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID)
            if (descriptor != null) {
                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                g.writeDescriptor(descriptor)
            }
            state = HrConnectionState.CONNECTED
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid != HEART_RATE_MEASUREMENT_UUID) return
            val data = characteristic.value ?: return
            currentBpm = parseHeartRate(data)
        }
    }

    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** פענוח ערך הדופק לפי מפרט ה-Bluetooth SIG ל-Heart Rate Measurement:
         * ביט 0 של הדגלים (בית ראשון) קובע אם הערך הוא UINT8 (בית אחד) או
         * UINT16 little-endian (שני בתים) בבית שאחריו. */
        fun parseHeartRate(data: ByteArray): Int? {
            if (data.isEmpty()) return null
            val flags = data[0].toInt()
            val is16Bit = (flags and 0x01) != 0
            return if (is16Bit) {
                if (data.size < 3) return null
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else {
                if (data.size < 2) return null
                data[1].toInt() and 0xFF
            }
        }
    }
}
