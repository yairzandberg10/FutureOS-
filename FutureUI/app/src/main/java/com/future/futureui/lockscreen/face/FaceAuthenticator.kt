package com.future.futureui.lockscreen.face

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.future.futureui.lockscreen.logic.LockSettings

/** מצב סריקה, להצגה במסך הנעילה / ברישום. */
enum class FaceStatus { SCANNING, NO_FACE, TOO_FAR, TOO_DARK, NOT_RECOGNIZED, RECOGNIZED, UNAVAILABLE }

/**
 * סריקה אחת לפתיחת נעילה: עד [TIMEOUT_MS] של פריימים מהמצלמה הקדמית, וצריך
 * [REQUIRED_MATCHES] פריימים רצופים שתואמים - פריים בודד "בר מזל" לא מספיק.
 * כל הקריאות חוזרות ל-main thread.
 */
class FaceAuthenticator(context: Context, private val settings: LockSettings) {
    private val appContext = context.applicationContext
    private val camera = FaceCamera(appContext)
    private val store = FaceTemplateStore(appContext)
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var active = false
    private var startedAt = 0L
    private var matches = 0
    private var framesWithoutFace = 0
    private var sawFace = false

    /** הדגימה האחרונה שנסרקה - אם הסריקה נכשלה והקוד הוזן נכון, נלמד ממנה. */
    @Volatile var lastProbe: FloatArray? = null
        private set

    fun isAvailable(): Boolean = settings.faceEnabled && store.isEnrolled() && !settings.strongAuthRequired()

    fun start(onStatus: (FaceStatus) -> Unit, onSuccess: () -> Unit) {
        if (active) return
        val data = store.load()
        if (data == null || data.enrolled.isEmpty()) { onStatus(FaceStatus.UNAVAILABLE); return }
        active = true
        startedAt = SystemClock.elapsedRealtime()
        matches = 0
        framesWithoutFace = 0
        sawFace = false
        lastProbe = null
        FaceCamera.extraRotation = settings.prefs.getInt(KEY_ROTATION, 0)
        val threshold = data.calibration * FaceEngine.thresholdFactor(settings.faceSensitivity)
        val templates = data.all
        post { onStatus(FaceStatus.SCANNING) }

        camera.start(onFrame = frame@{ frame ->
            if (!active) return@frame
            if (SystemClock.elapsedRealtime() - startedAt > TIMEOUT_MS) {
                finish()
                if (sawFace) settings.onFaceFailure()
                post { onStatus(if (sawFace) FaceStatus.NOT_RECOGNIZED else FaceStatus.NO_FACE) }
                return@frame
            }
            val r = FaceEngine.analyze(frame)
            when (r.problem) {
                FaceEngine.Problem.NONE -> {
                    sawFace = true
                    framesWithoutFace = 0
                    val probe = r.features!!
                    lastProbe = probe
                    val d = FaceEngine.bestDistance(probe, templates)
                    Log.i(TAG, "face distance=%.4f threshold=%.4f".format(d, threshold))
                    if (d <= threshold) {
                        if (++matches >= REQUIRED_MATCHES) {
                            finish()
                            settings.onFaceSuccess()
                            post { onStatus(FaceStatus.RECOGNIZED); onSuccess() }
                        }
                    } else {
                        matches = 0
                    }
                }
                FaceEngine.Problem.NO_FACE -> { matches = 0; maybeFixRotation() }
                FaceEngine.Problem.TOO_FAR -> { matches = 0; post { onStatus(FaceStatus.TOO_FAR) } }
                FaceEngine.Problem.TOO_DARK -> { matches = 0; post { onStatus(FaceStatus.TOO_DARK) } }
                FaceEngine.Problem.EDGE -> matches = 0
            }
        }, onError = { msg ->
            Log.w(TAG, "camera: $msg")
            active = false
            post { onStatus(FaceStatus.UNAVAILABLE) }
        })
    }

    /**
     * אם במשך 10 פריימים ברצף לא נמצאו פנים אף פעם (גם לא בתחילת הסריקה),
     * כנראה שהסיבוב של המצלמה שגוי ב-ROM הזה - מנסים את הכיוון הבא ושומרים.
     */
    private fun maybeFixRotation() {
        framesWithoutFace++
        if (sawFace || framesWithoutFace < 10) return
        if (settings.prefs.getBoolean(KEY_ROTATION_CONFIRMED, false)) return
        framesWithoutFace = 0
        val next = (FaceCamera.extraRotation + 180) % 360
        FaceCamera.extraRotation = next
        settings.prefs.edit().putInt(KEY_ROTATION, next).apply()
    }

    fun stop() {
        active = false
        camera.stop()
    }

    /** הקוד הוזן נכון אחרי שהפנים לא זוהו - לומדים מהדגימה האחרונה. */
    fun learnFromLastProbe() {
        val p = lastProbe ?: return
        lastProbe = null
        Thread { store.addAdaptive(p) }.start()
    }

    private fun finish() {
        active = false
        camera.stop()
    }

    private fun post(block: () -> Unit) = main.post(block)

    companion object {
        private const val TAG = "FaceAuth"
        const val TIMEOUT_MS = 5_000L
        private const val REQUIRED_MATCHES = 2
        const val KEY_ROTATION = "face_rotation"
        const val KEY_ROTATION_CONFIRMED = "face_rotation_ok"
    }
}

/**
 * רישום פנים (מתוך ההגדרות): אוסף [TARGET] דגימות שונות זו מזו, בזמן
 * שהמשתמש מזיז לאט את הראש. מציג תצוגה מקדימה קטנה כדי שיהיה קל למקם את הפנים.
 */
class FaceEnroller(context: Context, private val settings: LockSettings) {
    private val appContext = context.applicationContext
    private val camera = FaceCamera(appContext)
    private val main = Handler(Looper.getMainLooper())
    private val samples = ArrayList<FloatArray>()
    private var lastSampleAt = 0L
    private var framesWithoutFace = 0
    private var sawFace = false
    @Volatile private var active = false

    fun start(
        onPreview: (Bitmap) -> Unit,
        onProgress: (count: Int, status: FaceStatus) -> Unit,
        onDone: () -> Unit,
        onError: (String) -> Unit,
    ) {
        if (active) return
        active = true
        samples.clear()
        FaceCamera.extraRotation = settings.prefs.getInt(FaceAuthenticator.KEY_ROTATION, 0)
        var previewAt = 0L
        camera.start(onFrame = frame@{ frame ->
            if (!active) return@frame
            val now = SystemClock.elapsedRealtime()
            if (now - previewAt > 120) {
                previewAt = now
                val bmp = FaceEngine.previewBitmap(frame)
                main.post { onPreview(bmp) }
            }
            val r = FaceEngine.analyze(frame)
            val status = when (r.problem) {
                FaceEngine.Problem.NONE -> FaceStatus.SCANNING
                FaceEngine.Problem.TOO_FAR -> FaceStatus.TOO_FAR
                FaceEngine.Problem.TOO_DARK -> FaceStatus.TOO_DARK
                else -> FaceStatus.NO_FACE
            }
            if (r.problem == FaceEngine.Problem.NO_FACE && !sawFace) {
                if (++framesWithoutFace >= 12) {
                    framesWithoutFace = 0
                    FaceCamera.extraRotation = (FaceCamera.extraRotation + 180) % 360
                }
            }
            val features = r.features
            if (features != null) {
                if (!sawFace) {
                    sawFace = true
                    // הכיוון הזה עובד - שומרים אותו לפתיחת הנעילה
                    settings.prefs.edit()
                        .putInt(FaceAuthenticator.KEY_ROTATION, FaceCamera.extraRotation)
                        .putBoolean(FaceAuthenticator.KEY_ROTATION_CONFIRMED, true)
                        .apply()
                }
                val different = samples.isEmpty() || FaceEngine.bestDistance(features, samples) > MIN_SAMPLE_DISTANCE
                if (now - lastSampleAt > SAMPLE_GAP_MS && different) {
                    lastSampleAt = now
                    samples.add(features)
                }
            }
            val count = samples.size
            main.post { onProgress(count, status) }
            if (count >= TARGET) {
                active = false
                camera.stop()
                val copy = samples.toList()
                val calibration = FaceEngine.calibrate(copy)
                Log.i("FaceEnroll", "enrolled ${copy.size} samples, calibration=%.4f".format(calibration))
                FaceTemplateStore(appContext).saveEnrollment(copy, calibration)
                settings.onFaceSuccess()
                main.post(onDone)
            }
        }, onError = { msg ->
            active = false
            main.post { onError(msg) }
        })
    }

    fun stop() {
        active = false
        camera.stop()
    }

    companion object {
        const val TARGET = 12
        private const val SAMPLE_GAP_MS = 300L
        private const val MIN_SAMPLE_DISTANCE = 0.01f
    }
}
