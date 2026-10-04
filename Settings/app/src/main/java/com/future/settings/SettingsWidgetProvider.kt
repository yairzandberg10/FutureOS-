package com.future.settings

import android.bluetooth.BluetoothManager
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** סוללה (אחוז וטעינה), ומצב ה-Wi-Fi והבלוטות'. */
class SettingsWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.let { b ->
            val l = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && s > 0) l * 100 / s else null
        }
        val charging = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let {
            it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
        }
        val wifi = runCatching { context.applicationContext.getSystemService(WifiManager::class.java)?.isWifiEnabled }.getOrNull()
        val bt = runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull()
        val value = if (level != null) "סוללה $level%" + if (charging) " · בטעינה" else "" else "הגדרות"
        val states = listOfNotNull(
            wifi?.let { "Wi-Fi " + if (it) "מופעל" else "כבוי" },
            bt?.let { "בלוטות' " + if (it) "מופעל" else "כבוי" },
        ).joinToString(" · ")
        return WidgetContent(value = value, subtitle = states)
    }
}
