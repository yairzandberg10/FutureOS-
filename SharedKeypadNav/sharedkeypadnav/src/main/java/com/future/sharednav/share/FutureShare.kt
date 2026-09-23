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
        send.clipData?.let { window.clipData = it }
        try {
            context.startActivity(window)
        } catch (e: ActivityNotFoundException) {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: SecurityException) {
            context.startActivity(Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    /** שיתוף טקסט בלבד. */
    fun text(context: Context, text: String, title: String = "שיתוף") {
        open(context, Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), title)
    }
}
