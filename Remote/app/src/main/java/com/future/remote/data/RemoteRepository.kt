package com.future.remote.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** שמירת רשימת מכשירי השלט מקומית (SharedPreferences + Gson) - אין צורך
 * בשרת, כל הנתונים אישיים למכשיר הזה. */
class RemoteRepository(context: Context) {
    private val prefs = context.getSharedPreferences("remote_devices", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadDevices(): List<RemoteDevice> = readDevicesOrNull() ?: emptyList()

    /** null = השמירה הקיימת לא נקראת. כותבים רק כשהקריאה הצליחה - אחרת שמירה של מכשיר חדש דורסת את כולם. */
    private fun readDevicesOrNull(): List<RemoteDevice>? {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RemoteDevice>>() {}.type
            gson.fromJson<List<RemoteDevice>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            null
        }
    }

    private fun saveDevices(devices: List<RemoteDevice>) {
        prefs.edit().putString(KEY_DEVICES, gson.toJson(devices)).apply()
    }

    fun addDevice(device: RemoteDevice) {
        saveDevices((readDevicesOrNull() ?: return) + device)
    }

    fun deleteDevice(deviceId: String) {
        saveDevices((readDevicesOrNull() ?: return).filterNot { it.id == deviceId })
    }

    fun addButton(deviceId: String, button: RemoteButton) {
        saveDevices((readDevicesOrNull() ?: return).map { device ->
            if (device.id == deviceId) device.copy(buttons = device.buttons + button) else device
        })
    }

    fun deleteButton(deviceId: String, buttonId: String) {
        saveDevices((readDevicesOrNull() ?: return).map { device ->
            if (device.id == deviceId) device.copy(buttons = device.buttons.filterNot { it.id == buttonId }) else device
        })
    }

    fun updateDevice(deviceId: String, transform: (RemoteDevice) -> RemoteDevice) {
        saveDevices((readDevicesOrNull() ?: return).map { if (it.id == deviceId) transform(it) else it })
    }

    companion object {
        private const val KEY_DEVICES = "devices_json"
    }
}
