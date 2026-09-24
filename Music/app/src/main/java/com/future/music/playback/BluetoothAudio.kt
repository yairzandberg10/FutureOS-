package com.future.music.playback

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log

/**
 * Bluetooth לשמע ישירות מהמוזיקה: הפעלה, רשימת האוזניות/הבוקסות המצומדות,
 * חיבור וניתוק לפרופיל המדיה (A2DP) וסוללה. החיבור עצמו עובר דרך אותה
 * מתודה נסתרת של BluetoothA2dp שאפליקציית ה-Bluetooth של FutureOS משתמשת בה.
 */
@SuppressLint("MissingPermission")
class BluetoothAudio(private val context: Context) {

    data class AudioDevice(
        val address: String,
        val name: String,
        val connected: Boolean,
        /** 0..100, או null כשההתקן לא מדווח. */
        val battery: Int?,
        val isHeadphones: Boolean,
    )

    private val adapter: BluetoothAdapter? =
        context.getSystemService(BluetoothManager::class.java)?.adapter
    private var a2dp: BluetoothA2dp? = null
    private var receiver: BroadcastReceiver? = null

    val isSupported get() = adapter != null
    val isEnabled get() = runCatching { adapter?.isEnabled == true }.getOrDefault(false)

    /** [onChange] נקרא בכל שינוי (הפעלה, חיבור, סוללה) - לרענון המסך. */
    fun start(onChange: () -> Unit) {
        adapter?.getProfileProxy(context, object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                a2dp = proxy as? BluetoothA2dp
                onChange()
            }
            override fun onServiceDisconnected(profile: Int) { a2dp = null }
        }, BluetoothProfile.A2DP)
        receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = onChange()
        }.also {
            val filter = IntentFilter().apply {
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
                addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
                addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
                addAction("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")
            }
            context.registerReceiver(it, filter)
        }
    }

    fun stop() {
        receiver?.let { runCatching { context.unregisterReceiver(it) } }
        receiver = null
        a2dp?.let { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        a2dp = null
    }

    /** targetSdk 31 - enable()/disable() עדיין עובדים ישירות, בלי מסך אישור. */
    @Suppress("DEPRECATION")
    fun setEnabled(on: Boolean): Boolean = runCatching {
        if (on) adapter?.enable() == true else adapter?.disable() == true
    }.getOrDefault(false)

    fun devices(): List<AudioDevice> {
        val bonded = runCatching { adapter?.bondedDevices.orEmpty() }.getOrDefault(emptySet())
        val connected = runCatching { a2dp?.connectedDevices.orEmpty() }.getOrDefault(emptyList()).map { it.address }.toSet()
        return bonded.filter { it.isAudio() }.map { d ->
            AudioDevice(
                address = d.address,
                name = runCatching { d.alias ?: d.name }.getOrNull() ?: d.address,
                connected = d.address in connected,
                battery = batteryOf(d),
                isHeadphones = d.bluetoothClass?.deviceClass in HEADPHONE_CLASSES,
            )
        }.sortedWith(compareByDescending<AudioDevice> { it.connected }.thenBy { it.name })
    }

    fun connect(address: String): Boolean = callA2dp("connect", address)
    fun disconnect(address: String): Boolean = callA2dp("disconnect", address)

    private fun callA2dp(method: String, address: String): Boolean {
        val proxy = a2dp ?: return false
        val device = runCatching { adapter?.getRemoteDevice(address) }.getOrNull() ?: return false
        return try {
            BluetoothA2dp::class.java.getMethod(method, BluetoothDevice::class.java).invoke(proxy, device) as? Boolean ?: false
        } catch (e: Exception) {
            Log.e(TAG, "$method failed", e)
            false
        }
    }

    private fun batteryOf(device: BluetoothDevice): Int? = try {
        (BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(device) as? Int)?.takeIf { it in 0..100 }
    } catch (e: Exception) {
        null
    }

    private fun BluetoothDevice.isAudio(): Boolean {
        val cls = runCatching { bluetoothClass }.getOrNull() ?: return true
        return cls.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO ||
            cls.hasService(BluetoothClass.Service.AUDIO) || cls.hasService(BluetoothClass.Service.RENDER)
    }

    companion object {
        private const val TAG = "BluetoothAudio"
        private val HEADPHONE_CLASSES = setOf(
            BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES,
            BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET,
            BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE,
        )
    }
}
