package com.future.futureui.controlcenter.logic

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*

class ControlLayoutManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("control_center_layout", Context.MODE_PRIVATE)

    val allAvailableControls = listOf(
        ControlInfo("wifi", "Wi-Fi", Icons.Rounded.Wifi),
        ControlInfo("bluetooth", "Bluetooth", Icons.Rounded.Bluetooth),
        ControlInfo("flashlight", "פנס", Icons.Rounded.FlashlightOn),
        ControlInfo("airplane", "מצב טיסה", Icons.Rounded.AirplanemodeActive),
        ControlInfo("data", "נתונים", Icons.Rounded.SignalCellularAlt),
        ControlInfo("dnd", "נא לא להפריע", Icons.Rounded.DoNotDisturbOn),
        ControlInfo("location", "מיקום", Icons.Rounded.LocationOn),
        ControlInfo("rotation", "סיבוב", Icons.Rounded.ScreenRotation),
        ControlInfo("battery", "סוללה", Icons.Rounded.BatterySaver),
        ControlInfo("night", "לילה", Icons.Rounded.Nightlight),
        ControlInfo("settings", "הגדרות", Icons.Rounded.Settings),
        ControlInfo("camera", "מצלמה", Icons.Rounded.CameraAlt),
        ControlInfo("search", "חיפוש", Icons.Rounded.Search),
        ControlInfo("music", "מוזיקה", Icons.Rounded.MusicNote),
        ControlInfo("account", "חשבון", Icons.Rounded.Person),
        ControlInfo("calendar", "יומן", Icons.Rounded.CalendarMonth),
        ControlInfo("security", "אבטחה", Icons.Rounded.Security)
    )

    private val defaultLayout = listOf(
        "wifi", "bluetooth", "flashlight", "airplane", "data", "dnd", "location",
        "rotation", "battery", "night", "settings", "camera",
        "search", "music", "account", "calendar", "security"
    )

    private val defaultSectionOrder = listOf("toggles", "media", "grid", "sliders", "bottom_toggles")

    fun getActiveLayout(): List<String> {
        val saved = prefs.getString("layout_ids", null)
        return saved?.split(",") ?: defaultLayout
    }

    fun saveLayout(ids: List<String>) {
        prefs.edit().putString("layout_ids", ids.joinToString(",")).apply()
    }

    fun getSectionOrder(): List<String> {
        val saved = prefs.getString("section_order", null)
        return saved?.split(",") ?: defaultSectionOrder
    }

    fun saveSectionOrder(order: List<String>) {
        prefs.edit().putString("section_order", order.joinToString(",")).apply()
    }

    fun getTopToggleIds(): List<String> {
        val saved = prefs.getString("top_toggle_ids", "wifi,bluetooth")
        return saved?.split(",") ?: listOf("wifi", "bluetooth")
    }

    fun saveTopToggleIds(ids: List<String>) {
        prefs.edit().putString("top_toggle_ids", ids.joinToString(",")).apply()
    }

    fun getBottomToggleIds(): List<String> {
        val saved = prefs.getString("bottom_toggle_ids", "airplane,dnd")
        return saved?.split(",") ?: listOf("airplane", "dnd")
    }

    fun saveBottomToggleIds(ids: List<String>) {
        prefs.edit().putString("bottom_toggle_ids", ids.joinToString(",")).apply()
    }

    fun getControlById(id: String): ControlInfo? {
        return allAvailableControls.find { it.id == id }
    }
}
