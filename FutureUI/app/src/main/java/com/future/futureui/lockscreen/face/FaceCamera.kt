package com.future.futureui.lockscreen.face

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size

/** פריים אפור (בהירות בלבד), כבר מסובב כך שהפנים זקופות. */
class GrayFrame(val pixels: ByteArray, val width: Int, val height: Int) {
    operator fun get(x: Int, y: Int): Int = pixels[y * width + x].toInt() and 0xFF
}

/**
 * המצלמה הקדמית, בלי תצוגה מקדימה ובלי שום קובץ: רק ערוץ הבהירות (Y) של
 * כל פריים, ברזולוציה נמוכה, נמסר ל-[onFrame] על thread רקע. שום פריים לא
 * נשמר - מה שנשאר אחרי הזיהוי הוא רק וקטור מאפיינים (ר' [FaceEngine]).
 *
 * שירות הנגישות של FutureUI מחזיק ביכולת CAMERA גם ברקע (dumpsys: curCapability=LCMN),
 * ולכן אפשר לפתוח את המצלמה ישירות מתוך מסך הנעילה.
 */
class FaceCamera(private val context: Context) {

    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    @Volatile private var running = false

    @SuppressLint("MissingPermission")
    fun start(onFrame: (GrayFrame) -> Unit, onError: (String) -> Unit) {
        if (running) return
        running = true
        val t = HandlerThread("face-camera").also { it.start() }
        thread = t
        val h = Handler(t.looper)
        handler = h
        h.post {
            try {
                val manager = context.getSystemService(CameraManager::class.java)
                val id = manager.cameraIdList.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                }
                if (id == null) { onError("אין מצלמה קדמית"); stop(); return@post }
                val chars = manager.getCameraCharacteristics(id)
                val sensorOrientation = chars.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val size = chooseSize(map?.getOutputSizes(ImageFormat.YUV_420_888))
                val r = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
                reader = r
                r.setOnImageAvailableListener({ rd ->
                    val image = runCatching { rd.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
                    try {
                        if (!running) return@setOnImageAvailableListener
                        val plane = image.planes[0]
                        val w = image.width
                        val hgt = image.height
                        val buf = plane.buffer
                        val rowStride = plane.rowStride
                        val raw = ByteArray(w * hgt)
                        for (y in 0 until hgt) {
                            buf.position(y * rowStride)
                            buf.get(raw, y * w, w)
                        }
                        val frame = rotate(raw, w, hgt, (sensorOrientation + extraRotation) % 360)
                        onFrame(frame)
                    } catch (e: Exception) {
                        Log.w(TAG, "frame failed", e)
                    } finally {
                        image.close()
                    }
                }, h)

                manager.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        if (!running) { camera.close(); return }
                        device = camera
                        try {
                            @Suppress("DEPRECATION")
                            camera.createCaptureSession(listOf(r.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(s: CameraCaptureSession) {
                                    if (!running) { s.close(); return }
                                    session = s
                                    val req = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                        addTarget(r.surface)
                                        set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO)
                                        set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON)
                                        set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                                    }.build()
                                    runCatching { s.setRepeatingRequest(req, null, h) }
                                        .onFailure { onError("המצלמה לא זמינה"); stop() }
                                }

                                override fun onConfigureFailed(s: CameraCaptureSession) {
                                    onError("המצלמה לא זמינה"); stop()
                                }
                            }, h)
                        } catch (e: Exception) {
                            Log.w(TAG, "session failed", e)
                            onError("המצלמה לא זמינה"); stop()
                        }
                    }

                    override fun onDisconnected(camera: CameraDevice) { camera.close(); device = null }
                    override fun onError(camera: CameraDevice, error: Int) {
                        Log.w(TAG, "camera error $error")
                        camera.close(); device = null
                        onError(if (error == ERROR_CAMERA_IN_USE || error == ERROR_MAX_CAMERAS_IN_USE) "המצלמה בשימוש" else "המצלמה חסומה")
                        stop()
                    }
                }, h)
            } catch (e: SecurityException) {
                onError("אין הרשאת מצלמה"); stop()
            } catch (e: Exception) {
                Log.w(TAG, "open failed", e)
                onError("המצלמה לא זמינה"); stop()
            }
        }
    }

    fun stop() {
        if (!running && thread == null) return
        running = false
        val h = handler
        val t = thread
        handler = null
        thread = null
        val cleanup = Runnable {
            runCatching { session?.close() }
            runCatching { device?.close() }
            runCatching { reader?.close() }
            session = null; device = null; reader = null
            t?.quitSafely()
        }
        if (h != null) h.post(cleanup) else cleanup.run()
    }

    private fun chooseSize(sizes: Array<Size>?): Size {
        if (sizes.isNullOrEmpty()) return Size(640, 480)
        // 640x480 מספיק בהרבה לזיהוי (הפנים ממלאות חלק גדול מהפריים) וזול לעיבוד.
        return sizes.filter { it.width <= 800 && it.width >= 320 }
            .minByOrNull { kotlin.math.abs(it.width * it.height - 640 * 480) }
            ?: sizes.minBy { it.width * it.height }
    }

    companion object {
        private const val TAG = "FaceCamera"

        /**
         * סיבוב נוסף, אם יתגלה שה-ROM מדווח כיוון חיישן לא נכון. [FaceEngine]
         * מנסה את הכיוון ההפוך כשלא נמצאות פנים בכמה פריימים ראשונים, וזוכר מה עבד.
         */
        @Volatile var extraRotation = 0

        /** סיבוב עם כיוון השעון ב-0/90/180/270 מעלות. */
        fun rotate(src: ByteArray, w: Int, h: Int, degrees: Int): GrayFrame = when (degrees) {
            90 -> {
                val out = ByteArray(w * h)
                // (x,y) -> (h-1-y, x), רוחב חדש = h
                for (y in 0 until h) for (x in 0 until w) out[x * h + (h - 1 - y)] = src[y * w + x]
                GrayFrame(out, h, w)
            }
            180 -> {
                val out = ByteArray(w * h)
                for (i in 0 until w * h) out[w * h - 1 - i] = src[i]
                GrayFrame(out, w, h)
            }
            270 -> {
                val out = ByteArray(w * h)
                // (x,y) -> (y, w-1-x), רוחב חדש = h
                for (y in 0 until h) for (x in 0 until w) out[(w - 1 - x) * h + y] = src[y * w + x]
                GrayFrame(out, h, w)
            }
            else -> GrayFrame(src, w, h)
        }
    }
}
