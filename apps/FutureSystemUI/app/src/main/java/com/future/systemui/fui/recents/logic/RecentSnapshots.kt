package com.future.systemui.fui.recents.logic

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

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
    context: android.content.Context,
    /** מזהה המשימה של חבילה (RecentAppsManager.taskIdOf), או -1. */
    private val taskIdOf: (String) -> Int,
    /** גובה שורת המצב בפיקסלים - נחתך מראש הצילום. */
    private val topCropPx: Int,
) {
    /** מצב Compose - הכרטיסים מתעדכנים לבד כשצילום חדש מגיע. */
    /** תאימות לקוד FutureUI שנבנה עם שירות נגישות: אין מזהי משימות, ולכן אין צילומים (הכרטיס נשאר בצבע האייקון). */
    constructor(service: android.accessibilityservice.AccessibilityService, topCropPx: Int) : this(service, { -1 }, topCropPx)

    val images = mutableStateMapOf<String, ImageBitmap>()

    private val main = Handler(Looper.getMainLooper())
    // thread אחד שנסגר לבד אחרי 30 שניות בטלה - לא נשאר חי אחרי הפעלה מחדש של השירות
    private val io = ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, LinkedBlockingQueue<Runnable>()).apply { allowCoreThreadTimeOut(true) }
    private val dir = File(context.cacheDir, "recents").apply { mkdirs() }
    private var pending: Runnable? = null
    private var lastShotAt = 0L

    init {
        io.execute {
            dir.listFiles()?.forEach { file ->
                // צילום ישן מדי נמחק (SU-16): צילומי מסך של אפליקציות לא נשארים בדיסק לנצח
                if (System.currentTimeMillis() - file.lastModified() > MAX_AGE_MS) { file.delete(); return@forEach }
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

    /**
     * מצלם עכשיו; [onDone] נקרא על ה-main thread בכל מקרה (גם בכישלון).
     * ב-SystemUI הצילום הוא ה-TaskSnapshot של המערכת (IActivityTaskManager, דורש READ_FRAME_BUFFER),
     * דרך reflection כי ה-API נסתר. למשימה בחזית מבקשים צילום טרי (takeTaskSnapshot), לשאר - השמור.
     */
    fun capture(pkg: String, onDone: (() -> Unit)? = null) {
        val taskId = taskIdOf(pkg)
        val now = android.os.SystemClock.uptimeMillis()
        if (taskId < 0 || now - lastShotAt < 400) { onDone?.invoke(); return }
        lastShotAt = now
        io.execute {
            val scaled = runCatching { snapshotBitmap(taskId)?.let { shrink(it).also { _ -> it.recycle() } } }
                .onFailure { Log.w("FutureSystemUI", "task snapshot failed", it) }.getOrNull()
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
    }

    private fun snapshotBitmap(taskId: Int): Bitmap? {
        val atm = Class.forName("android.app.ActivityTaskManager").getMethod("getService").invoke(null) ?: return null
        var snap: Any? = runCatching {
            atm.javaClass.getMethod("takeTaskSnapshot", Int::class.javaPrimitiveType).invoke(atm, taskId)
        }.getOrNull()
        if (snap == null) {
            snap = atm.javaClass.getMethod("getTaskSnapshot", Int::class.javaPrimitiveType, Boolean::class.javaPrimitiveType)
                .invoke(atm, taskId, false)
        }
        snap ?: return null
        val buffer = snap.javaClass.getMethod("getHardwareBuffer").invoke(snap) as? android.hardware.HardwareBuffer ?: return null
        val colorSpace = runCatching { snap.javaClass.getMethod("getColorSpace").invoke(snap) as? android.graphics.ColorSpace }.getOrNull()
        return try {
            Bitmap.wrapHardwareBuffer(buffer, colorSpace)
        } finally {
            runCatching { buffer.close() }
        }
    }

    /** onDestroy של השירות: מבטל צילום מתוזמן (ה-thread של הדיסק נסגר לבד). */
    fun dispose() {
        cancelPending()
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
        const val MAX_AGE_MS = 24L * 60 * 60 * 1000
    }
}
