package com.future.dialer.telecom

import android.content.Context
import android.net.Uri
import com.future.sharednav.systemui.SystemUiTarget

/**
 * האם המכשיר נעול במסך הנעילה של FutureUI - גם כשהוא פינה מקום לשיחה. בזמן
 * שיחה במכשיר נעול החייגן מציג רק את מסך השיחה, לא יומן ואנשי קשר.
 * אם FutureUI לא זמין - false (אין מסך נעילה שלנו שיכול להיות פעיל).
 */
object DeviceLock {
    private val URI: Uri = Uri.parse("content://${SystemUiTarget.SETTINGS_AUTHORITY}/settings")

    fun isSecured(context: Context): Boolean = runCatching {
        if (!com.future.sharednav.systemui.TrustedProviders.isTrusted(context, SystemUiTarget.SETTINGS_AUTHORITY)) return@runCatching false
        context.contentResolver.query(URI, arrayOf("device_secured"), null, null, null)?.use { c ->
            val col = c.getColumnIndex("device_secured")
            col >= 0 && c.moveToFirst() && c.getInt(col) == 1
        } ?: false
    }.getOrDefault(false)
}
