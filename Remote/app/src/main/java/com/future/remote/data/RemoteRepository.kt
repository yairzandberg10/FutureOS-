package com.future.remote.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** שמירת רשימת מכשירי השלט מקומית (SharedPreferences + Gson) - אין צורך
 * בשרת, כל הנתונים אישיים למכשיר הזה. */
class RemoteRepository(context: Context) {
    private val prefs = context.getSharedPreferences("remote_devices", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadDevices(): List<RemoteDevice> {
        val json = prefs.getString(KEY_DEVICES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<RemoteDevice>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveDevices(devices: List<RemoteDevice>) {
        prefs.edit().putString(KEY_DEVICES, gson.toJson(devices)).apply()
    }

    fun addDevice(device: RemoteDevice) {
        saveDevices(loadDevices() + device)
    }

    fun deleteDevice(deviceId: String) {
        saveDevices(loadDevices().filterNot { it.id == deviceId })
    }

    fun addButton(deviceId: String, button: RemoteButton) {
        saveDevices(loadDevices().map { device ->
            if (device.id == deviceId) device.copy(buttons = device.buttons + button) else device
        })
    }

    fun deleteButton(deviceId: String, buttonId: String) {
        saveDevices(loadDevices().map { device ->
            if (device.id == deviceId) device.copy(buttons = device.buttons.filterNot { it.id == buttonId }) else device
        })
    }

    companion object {
        private const val KEY_DEVICES = "devices_json"
    }
}
