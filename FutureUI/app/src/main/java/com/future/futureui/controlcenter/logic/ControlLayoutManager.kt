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
        ControlInfo("security", "אבטחה", Icons.Rounded.Security),
        ControlInfo("predictive_text", "ניבוי טקסט", Icons.Rounded.Spellcheck)
    )

    private val defaultLayout = listOf(
        "wifi", "bluetooth", "flashlight", "airplane", "data", "dnd", "location",
        "rotation", "battery", "night", "settings", "camera",
        "search", "music", "account", "calendar", "security", "predictive_text"
    )

    private val defaultSectionOrder = listOf("pills", "media", "grid", "sliders")

    fun getActiveLayout(): List<String> {
        val saved = prefs.getString("layout_ids", null)
        return saved?.split(",") ?: defaultLayout
    }

    fun saveLayout(ids: List<String>) {
        prefs.edit().putString("layout_ids", ids.joinToString(",")).apply()
    }

    /** סדר שנשמר לפני הגלולות החדשות מכיל "toggles"/"bottom_toggles" (שתי שורות
     *  הגלולות הקבועות שהוסרו) - הראשונה הופכת ל-"pills" והשנייה נמחקת. */
    fun getSectionOrder(): List<String> {
        val saved = prefs.getString("section_order", null)?.split(",") ?: return defaultSectionOrder
        val migrated = saved
            .filter { it != "bottom_toggles" }
            .map { if (it == "toggles") "pills" else it }
            .distinct()
        return if ("pills" in migrated) migrated else listOf("pills") + migrated
    }

    fun saveSectionOrder(order: List<String>) {
        prefs.edit().putString("section_order", order.joinToString(",")).apply()
    }

    /** גלולות שהמשתמש הוסיף (עד PillCatalog.MAX_PILLS) - ריק כברירת מחדל. */
    fun getPillIds(): List<String> {
        val saved = prefs.getString("pill_ids", null) ?: return emptyList()
        return saved.split(",").filter { PillCatalog.get(it) != null }.take(PillCatalog.MAX_PILLS)
    }

    fun savePillIds(ids: List<String>) {
        prefs.edit()
            .putString("pill_ids", ids.joinToString(","))
            .remove("top_toggle_ids")
            .remove("bottom_toggle_ids")
            .apply()
    }

    fun getControlById(id: String): ControlInfo? {
        return allAvailableControls.find { it.id == id }
    }
}
