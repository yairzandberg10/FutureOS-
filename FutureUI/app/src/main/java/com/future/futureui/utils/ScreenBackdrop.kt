package com.future.futureui.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import java.util.concurrent.Executors

/**
 * הרקע המטושטש של מרכז הבקרה ומרכז ההתראות: צילום של מה שעל המסך ברגע
 * הפתיחה, מוקטן, ש-Compose מטשטש (Modifier.blur - RenderEffect, בתוך החלון
 * שלנו) מתחת לפאנל.
 *
 * במכשיר הזה טשטוש בין-חלונות כבוי (dumpsys window: mBlurEnabled=false), כך
 * ש-FLAG_BLUR_BEHIND לא עושה כלום והפאנלים נראו כמשטחים שחורים. את הצילום
 * לוקחים דרך שירות שורת המצב - רק לו יש canTakeScreenshot, ושלושת השירותים
 * רצים באותו תהליך. בלי השירות, או כשהצילום נכשל/איטי, חוזרים לטפט.
 */
object ScreenBackdrop {
    @Volatile var screenshotService: AccessibilityService? = null

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** מצלם עכשיו (לפני שהפאנל נוסף למסך!). [onResult] נקרא פעם אחת על ה-main thread. */
    fun capture(onResult: (Bitmap?) -> Unit) {
        val service = screenshotService ?: return onResult(null)
        var delivered = false
        fun deliver(bitmap: Bitmap?) {
            main.post {
                if (!delivered) {
                    delivered = true
                    onResult(bitmap)
                }
            }
        }
        // צילום שלא חזר תוך זמן קצר לא מעכב את פתיחת הפאנל.
        main.postDelayed({ deliver(null) }, TIMEOUT_MS)
        try {
            service.takeScreenshot(Display.DEFAULT_DISPLAY, io, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val small = runCatching {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        buffer.close()
                        hw?.let {
                            val soft = it.copy(Bitmap.Config.ARGB_8888, false)
                            it.recycle()
                            Bitmap.createScaledBitmap(soft, soft.width / SCALE, soft.height / SCALE, true)
                                .also { out -> if (out !== soft) soft.recycle() }
                        }
                    }.onFailure { Log.w(TAG, "backdrop convert failed", it) }.getOrNull()
                    deliver(small)
                }

                override fun onFailure(errorCode: Int) { deliver(null) }
            })
        } catch (e: Exception) {
            Log.w(TAG, "backdrop screenshot failed", e)
            deliver(null)
        }
    }

    private const val TAG = "FutureUI"
    private const val SCALE = 4
    private const val TIMEOUT_MS = 350L
}
