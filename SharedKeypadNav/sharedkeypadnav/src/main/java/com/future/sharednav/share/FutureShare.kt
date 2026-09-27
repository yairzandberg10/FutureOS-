package com.future.sharednav.share

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.future.sharednav.systemui.SystemUiTarget

/**
 * נקודת הכניסה היחידה לשיתוף בכל אפליקציות FutureOS. כל "שתף" עובר כאן,
 * כך שחלון השיתוף הוא אחד - של FutureUI - ולא בורר אחר בכל אפליקציה.
 *
 * [send] הוא Intent.ACTION_SEND/ACTION_SEND_MULTIPLE מוכן (סוג, טקסט/קובץ,
 * FLAG_GRANT_READ_URI_PERMISSION). אם חלון השיתוף של FutureUI לא מותקן, נופלים
 * לבורר של המערכת.
 */
object FutureShare {
    const val ACTION_SHARE = "com.future.futureui.ACTION_SHARE"

    fun open(context: Context, send: Intent, title: String = "שיתוף") {
        val window = Intent(ACTION_SHARE)
            .setPackage(SystemUiTarget.PACKAGE)
            .putExtra(Intent.EXTRA_INTENT, send)
            .putExtra(Intent.EXTRA_TITLE, title)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .addFlags(send.flags and (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        // ה-ClipData של ה-Intent הפנימי נושא את הרשאת הקריאה לקובץ דרך חלון השיתוף.
        // חלון השיתוף מעביר הלאה רק קבצים שקיבל עליהם הרשאה כך - אז גם כשהקורא
        // שם רק EXTRA_STREAM, בונים ממנו ClipData.
        (send.clipData ?: streamClip(send))?.let { window.clipData = it }
        // חלון השיתוף מקבל את הקבצים ואת הטקסט - רק אם הוא באמת FutureUI (אותה
        // חתימה), ולא אפליקציה זרה שהותקנה בשם החבילה שלו כשהוא לא מותקן.
        if (!isTrusted(context)) {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        try {
            context.startActivity(window)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: SecurityException) {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun streamClip(send: Intent): android.content.ClipData? {
        @Suppress("DEPRECATION")
        val uris: List<android.net.Uri> = send.getParcelableArrayListExtra<android.net.Uri>(Intent.EXTRA_STREAM)
            ?: listOfNotNull(runCatching { send.getParcelableExtra<android.net.Uri>(Intent.EXTRA_STREAM) }.getOrNull())
        if (uris.isEmpty()) return null
        return android.content.ClipData.newRawUri("", uris.first()).also { clip ->
            uris.drop(1).forEach { clip.addItem(android.content.ClipData.Item(it)) }
        }
    }

    private fun isTrusted(context: Context): Boolean =
        runCatching {
            context.packageManager.checkSignatures(context.packageName, SystemUiTarget.PACKAGE) ==
                android.content.pm.PackageManager.SIGNATURE_MATCH
        }.getOrDefault(false)

    /** שיתוף טקסט בלבד. */
    fun text(context: Context, text: String, title: String = "שיתוף") {
        open(context, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title)
    }
}
