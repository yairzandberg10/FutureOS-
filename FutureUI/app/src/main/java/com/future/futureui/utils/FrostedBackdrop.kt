package com.future.futureui.utils

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.util.concurrent.Executors

/**
 * רקע "זכוכית מטושטשת" אמיתי להתראה הצפה.
 *
 * למה לא טשטוש של חלון: במכשיר (MT6768) ה-SurfaceFlinger לא תומך בטשטוש
 * בין חלונות - `ro.surface_flinger.supports_background_blur` לא מוגדר ו-
 * `dumpsys window` מראה `mBlurEnabled=false` - ולכן FLAG_BLUR_BEHIND ו-
 * setBackgroundBlurRadius לא עושים כלום. גם אילו עבדו, טשטוש חי של
 * SurfaceFlinger בכל פריים יקר מדי ל-Mali-G52.
 *
 * במקום זה: צילום מסך אחד דרך שירות הנגישות (takeScreenshot, כמו בצילומי
 * "אחרונות"), חיתוך הרצועה העליונה שמתחת להתראה, הקטנה פי 8 וטשטוש קופסה
 * על התמונה הקטנה (בערך 80×40 פיקסלים - זניח). ההתראה מציירת אותה מוגדלת
 * עם סינון, וזה נראה כמו זכוכית מטושטשת. העבודה כולה רצה מחוץ ל-main
 * thread, פעם אחת לכל הופעה של התראה - לא בכל פריים.
 *
 * הצילום הוא תמונת מצב: אם התוכן מתחת משתנה בזמן שההתראה מוצגת (4.5
 * שניות), הזכוכית לא מתעדכנת. זה מקובל לבאנר חולף.
 */
object FrostedBackdrop {

    /** שירות שורת המצב, שמורשה לצלם (canTakeScreenshot). null - אין טשטוש, רקע אטום כמו קודם. */
    @Volatile
    var service: AccessibilityService? = null

    class Frost(val image: ImageBitmap, val sourceWidth: Int, val sourceHeight: Int)

    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    /** מצלם את [regionHeightPx] הפיקסלים העליונים; [onResult] נקרא על ה-main thread (null בכישלון). */
    fun capture(regionHeightPx: Int, onResult: (Frost?) -> Unit) {
        val svc = service
        if (svc == null) {
            onResult(null)
            return
        }
        try {
            svc.takeScreenshot(Display.DEFAULT_DISPLAY, io, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val frost = runCatching {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        buffer.close()
                        hw?.let { make(it, regionHeightPx).also { _ -> it.recycle() } }
                    }.onFailure { Log.w("FutureUI", "frost failed", it) }.getOrNull()
                    main.post { onResult(frost) }
                }

                override fun onFailure(errorCode: Int) {
                    // הגבלת קצב (צילום של "אחרונות" ממש לפני) או חלון מאובטח - רקע אטום
                    main.post { onResult(null) }
                }
            })
        } catch (e: Exception) {
            Log.w("FutureUI", "frost takeScreenshot failed", e)
            onResult(null)
        }
    }

    private fun make(hardware: Bitmap, regionHeightPx: Int): Frost {
        val width = hardware.width
        val height = regionHeightPx.coerceIn(1, hardware.height)
        val soft = hardware.copy(Bitmap.Config.ARGB_8888, false)
        val crop = Bitmap.createBitmap(soft, 0, 0, width, height)
        val small = Bitmap.createScaledBitmap(crop, (width / Downscale).coerceAtLeast(1), (height / Downscale).coerceAtLeast(1), true)
        if (crop !== soft) crop.recycle()
        soft.recycle()
        val out = if (small.isMutable) small else small.copy(Bitmap.Config.ARGB_8888, true)
        boxBlur(out, radius = 2, passes = 3)
        return Frost(out.asImageBitmap(), width, height)
    }

    /** טשטוש קופסה אופקי ואנכי, [passes] פעמים - קרוב לגאוסיאני, על תמונה זעירה. */
    private fun boxBlur(bitmap: Bitmap, radius: Int, passes: Int) {
        val w = bitmap.width
        val h = bitmap.height
        val src = IntArray(w * h)
        val tmp = IntArray(w * h)
        bitmap.getPixels(src, 0, w, 0, 0, w, h)
        repeat(passes) {
            blurLine(src, tmp, w, h, radius, horizontal = true)
            blurLine(tmp, src, w, h, radius, horizontal = false)
        }
        bitmap.setPixels(src, 0, w, 0, 0, w, h)
    }

    private fun blurLine(input: IntArray, output: IntArray, w: Int, h: Int, r: Int, horizontal: Boolean) {
        val lines = if (horizontal) h else w
        val len = if (horizontal) w else h
        for (line in 0 until lines) {
            var sa = 0; var sr = 0; var sg = 0; var sb = 0
            fun idx(i: Int) = if (horizontal) line * w + i.coerceIn(0, len - 1) else i.coerceIn(0, len - 1) * w + line
            for (i in -r..r) {
                val c = input[idx(i)]
                sa += c ushr 24; sr += (c shr 16) and 0xFF; sg += (c shr 8) and 0xFF; sb += c and 0xFF
            }
            val div = 2 * r + 1
            for (i in 0 until len) {
                output[idx(i)] = ((sa / div) shl 24) or ((sr / div) shl 16) or ((sg / div) shl 8) or (sb / div)
                val add = input[idx(i + r + 1)]
                val rem = input[idx(i - r)]
                sa += (add ushr 24) - (rem ushr 24)
                sr += ((add shr 16) and 0xFF) - ((rem shr 16) and 0xFF)
                sg += ((add shr 8) and 0xFF) - ((rem shr 8) and 0xFF)
                sb += (add and 0xFF) - (rem and 0xFF)
            }
        }
    }

    private const val Downscale = 8
}
