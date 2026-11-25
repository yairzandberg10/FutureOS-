package com.future.bluetooth.widget

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** מצב הבלוטוס (מופעל/כבוי) ומספר המכשירים המותאמים. */
class BluetoothStatusWidgetProvider : FutureContentWidget() {
    @SuppressLint("MissingPermission")
    override fun content(context: Context): WidgetContent {
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
            ?: return WidgetContent(value = "אין בלוטוס במכשיר")
        if (!adapter.isEnabled) return WidgetContent(value = "בלוטוס כבוי", subtitle = "הפעל כדי לחבר מכשירים")
        if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return WidgetContent(value = "בלוטוס מופעל", subtitle = "אין הרשאה לרשימת המכשירים")
        }
        val paired = runCatching { adapter.bondedDevices.orEmpty().size }.getOrElse { return WidgetContent(value = "בלוטוס מופעל") }
        val subtitle = when (paired) {
            0 -> "אין מכשירים מותאמים"
            1 -> "מכשיר אחד מותאם"
            else -> "$paired מכשירים מותאמים"
        }
        return WidgetContent(value = "בלוטוס מופעל", subtitle = subtitle)
    }
}
