package com.future.systemui.statusbar

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import android.hardware.HardwareBuffer
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.future.systemui.fui.statusbar.logic.StatusBarLayoutManager
import java.util.concurrent.Executors

/**
 * Same job as FutureUI's sampleBarTone(): measure the strip right under the bar and tell the bar whether to
 * use light or dark content and which colour to continue. FutureUI used AccessibilityService.takeScreenshot;
 * SystemUI (platform-signed) uses the hidden SurfaceControl.captureDisplay through reflection.
 * When that is not possible [supported] turns false and the caller falls back to the system-bar appearance flags.
 */
internal class BarToneSampler(
    private val context: Context,
    private val onSample: (luma: Float, dominant: Int?) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val executor by lazy { Executors.newSingleThreadExecutor() }
    private var lastShotAt = 0L
    private var failures = 0
    private var everWorked = false

    @Volatile
    var supported = true
        private set

    private val run = Runnable { sample() }

    fun schedule(delayMs: Long) {
        if (!supported) return
        main.removeCallbacks(run)
        main.postDelayed(run, delayMs)
    }

    private fun sample() {
        if (!supported) return
        val now = SystemClock.uptimeMillis()
        if (now - lastShotAt < 500) { schedule(550); return }
        lastShotAt = now
        executor.execute {
            try {
                val strip = capture()
                if (strip == null) {
                    failures++
                    if (!everWorked && failures >= 3) {
                        supported = false
                        Log.w(TAG, "tone sampling unavailable, using appearance flags")
                    } else {
                        main.post { schedule(900) }
                    }
                    return@execute
                }
                everWorked = true
                failures = 0
                val result = try { measure(strip) } finally { strip.recycle() }
                main.post {
                    try { onSample(result.first, result.second) } catch (t: Throwable) { Log.e(TAG, "tone callback failed", t) }
                }
            } catch (t: Throwable) {
                supported = false
                Log.w(TAG, "tone sampling disabled", t)
            }
        }
    }

    /** The strip (24dp) directly under the bar, as a software bitmap; null on a transient failure. */
    private fun capture(): Bitmap? {
        val dm = context.resources.displayMetrics
        val barPx = StatusBarLayoutManager.heightPx(context)
        val stripH = (24 * dm.density).toInt().coerceAtLeast(1)
        val crop = Rect(0, barPx, dm.widthPixels, (barPx + stripH).coerceAtMost(dm.heightPixels))
        if (crop.width() <= 0 || crop.height() <= 0) return null

        val sc = Class.forName("android.view.SurfaceControl")
        val token = sc.getMethod("getInternalDisplayToken").invoke(null) as? IBinder ?: return null
        val builderCls = Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs\$Builder")
        val builder = builderCls.getConstructor(IBinder::class.java).newInstance(token)
        builderCls.getMethod("setSourceCrop", Rect::class.java).invoke(builder, crop)
        builderCls.getMethod("setSize", Int::class.javaPrimitiveType, Int::class.javaPrimitiveType)
            .invoke(builder, crop.width(), crop.height())
        val args = builderCls.getMethod("build").invoke(builder)
        val argsCls = Class.forName("android.view.SurfaceControl\$DisplayCaptureArgs")
        val shot = sc.getMethod("captureDisplay", argsCls).invoke(null, args) ?: return null

        val hw: Bitmap? = try {
            shot.javaClass.getMethod("asBitmap").invoke(shot) as? Bitmap
        } catch (_: NoSuchMethodException) {
            val buffer = shot.javaClass.getMethod("getHardwareBuffer").invoke(shot) as? HardwareBuffer ?: return null
            val space = shot.javaClass.getMethod("getColorSpace").invoke(shot) as? ColorSpace
            val b = Bitmap.wrapHardwareBuffer(buffer, space)
            buffer.close()
            b
        }
        hw ?: return null
        val soft = hw.copy(Bitmap.Config.ARGB_8888, false)
        hw.recycle()
        return soft
    }

    /** Average luma and the dominant colour (a 4-bit-per-channel bucket covering >= 55% of the strip), as in FutureUI. */
    private fun measure(soft: Bitmap): Pair<Float, Int?> {
        var sum = 0.0
        var count = 0
        val hits = IntArray(4096)
        val sumR = LongArray(4096)
        val sumG = LongArray(4096)
        val sumB = LongArray(4096)
        var y = 0
        while (y < soft.height) {
            var x = 0
            while (x < soft.width) {
                val c = soft.getPixel(x, y)
                val r = (c shr 16) and 0xFF
                val g = (c shr 8) and 0xFF
                val b = c and 0xFF
                sum += (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
                count++
                val bucket = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
                hits[bucket]++
                sumR[bucket] += r.toLong()
                sumG[bucket] += g.toLong()
                sumB[bucket] += b.toLong()
                x += 8
            }
            y += 4
        }
        if (count == 0) return 0f to null
        var best = 0
        for (i in 1 until hits.size) if (hits[i] > hits[best]) best = i
        val n = hits[best]
        val dominant = if (n >= count * 0.55f) {
            val r = (sumR[best] / n).toInt()
            val g = (sumG[best] / n).toInt()
            val b = (sumB[best] / n).toInt()
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        } else null
        return (sum / count).toFloat() to dominant
    }

    private companion object { const val TAG = "FutureSystemUI" }
}
