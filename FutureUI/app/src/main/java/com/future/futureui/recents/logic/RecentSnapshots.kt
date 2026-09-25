package com.future.futureui.recents.logic

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Display
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.Executors

/**
 * צילומי המסך של האפליקציות האחרונות - מה שמוצג בכרטיסים של מסך "אחרונות".
 *
 * אין לנו גישה לצילומי המשימות של המערכת (ActivityTaskManager דורש הרשאת
 * מערכת), ולכן שירות הנגישות מצלם בעצמו את האפליקציה שבחזית
 * (AccessibilityService.takeScreenshot): זמן קצר אחרי שחלון עולה, שוב אחרי
 * שהמשתמש מפסיק ללחוץ על מקשים, ומיד לפני שמסך האחרונות נפתח. כך הכרטיס מראה
 * פחות או יותר את המקום שבו עזבנו את האפליקציה.
 *
 * הצילום מוקטן לשליש ושורת המצב שלנו נחתכת ממנו. הוא נשמר גם בקובץ
 * (cacheDir/recents) כדי לשרוד הפעלה מחדש של השירות.
 */
class RecentSnapshots(
    private val service: AccessibilityService,
    /** גובה שורת המצב בפיקסלים - נחתך מראש הצילום. */
    private val topCropPx: Int,
) {
    /** מצב Compose - הכרטיסים מתעדכנים לבד כשצילום חדש מגיע. */
    val images = mutableStateMapOf<String, ImageBitmap>()

    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val dir = File(service.cacheDir, "recents").apply { mkdirs() }
    private var pending: Runnable? = null
    private var lastShotAt = 0L

    init {
        io.execute {
            dir.listFiles()?.forEach { file ->
                val pkg = file.nameWithoutExtension
                runCatching { android.graphics.BitmapFactory.decodeFile(file.path) }.getOrNull()?.let { bmp ->
                    main.post { if (pkg !in images) images[pkg] = bmp.asImageBitmap() }
                }
            }
        }
    }

    /** מתזמן צילום של [pkg] בעוד [delayMs], אם היא עדיין בחזית אז (ר' [isCapturable]). */
    fun schedule(pkg: String, delayMs: Long, isCapturable: (String) -> Boolean) {
        pending?.let(main::removeCallbacks)
        val task = Runnable { if (isCapturable(pkg)) capture(pkg) }
        pending = task
        main.postDelayed(task, delayMs)
    }

    fun cancelPending() {
        pending?.let(main::removeCallbacks)
        pending = null
    }

    /** מצלם עכשיו; [onDone] נקרא על ה-main thread בכל מקרה (גם בכישלון). */
    fun capture(pkg: String, onDone: (() -> Unit)? = null) {
        val now = android.os.SystemClock.uptimeMillis()
        // המערכת מגבילה את takeScreenshot לפעם בשליש שנייה בערך
        if (now - lastShotAt < 400) { onDone?.invoke(); return }
        lastShotAt = now
        try {
            service.takeScreenshot(Display.DEFAULT_DISPLAY, io, object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    val scaled = runCatching {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        buffer.close()
                        hw?.let { shrink(it).also { _ -> it.recycle() } }
                    }.onFailure { Log.w("FutureUI", "snapshot convert failed", it) }.getOrNull()
                    if (scaled != null) {
                        runCatching {
                            File(dir, "$pkg.webp").outputStream().use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, it) }
                        }
                    }
                    main.post {
                        if (scaled != null) images[pkg] = scaled.asImageBitmap()
                        onDone?.invoke()
                    }
                }

                override fun onFailure(errorCode: Int) {
                    // חלון מאובטח (FLAG_SECURE) או הגבלת קצב - נשארים עם הצילום הקודם
                    main.post { onDone?.invoke() }
                }
            })
        } catch (e: Exception) {
            Log.w("FutureUI", "takeScreenshot failed", e)
            onDone?.invoke()
        }
    }

    fun remove(pkg: String) {
        images.remove(pkg)
        io.execute { File(dir, "$pkg.webp").delete() }
    }

    fun clear() {
        images.clear()
        io.execute { dir.listFiles()?.forEach { it.delete() } }
    }

    private fun shrink(hardware: Bitmap): Bitmap {
        val soft = hardware.copy(Bitmap.Config.ARGB_8888, false)
        val top = topCropPx.coerceIn(0, soft.height / 4)
        val w = soft.width / SCALE
        val h = (soft.height - top) / SCALE
        val cropped = Bitmap.createBitmap(soft, 0, top, soft.width, soft.height - top)
        val out = Bitmap.createScaledBitmap(cropped, w, h, true)
        if (cropped !== out) cropped.recycle()
        soft.recycle()
        return out
    }

    private companion object {
        const val SCALE = 3
    }
}
