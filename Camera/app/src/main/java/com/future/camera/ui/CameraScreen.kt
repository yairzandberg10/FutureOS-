package com.future.camera.ui

import android.Manifest
import android.graphics.Bitmap
import android.media.MediaActionSound
import android.net.Uri
import android.view.View
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.BackHandler
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.extensions.ExtensionsManager
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.future.camera.data.AspectOption
import com.future.camera.data.CameraFilter
import com.future.camera.data.CameraOptions
import com.future.camera.data.CameraMedia
import com.future.camera.data.PhotoQualityOption
import com.future.camera.data.PhotoStorage
import com.future.camera.data.SceneOption
import com.future.camera.data.VideoQualityOption
import com.future.camera.data.VideoStorage
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/** מצבי הצילום. וידאו הוא היחיד שקושר VideoCapture; השאר מצלמים תמונה. */
private enum class CaptureMode(val label: String) {
    PHOTO("תמונה"),
    VIDEO("וידאו"),
    BURST("רצף"),
    SQUARE("ריבוע"),
    DOCUMENT("מסמך"),
}

/** קפיצת הזום בכל חזרה של מקש מוחזק - בערך שנייה וחצי מ-1x עד הזום המרבי. */
private const val ZOOM_STEP = 0.035f

/** כמה תמונות במצב רצף. */
private const val BURST_COUNT = 5

/**
 * מסך המצלמה - תצוגה חיה במסך מלא, הכול מהמקשים:
 *
 * - OK מצלם (על כפתור הצילום); OK מוחזק - מיקוד במרכז ונעילה.
 * - Options מחליף בין המצלמה האחורית לקדמית.
 * - חץ למעלה/למטה מוחזק - זום פנימה/החוצה; לחיצה קצרה מזיזה את הפוקוס.
 * - 1-9 - נקודת מיקוד לפי מיקום הספרה על המקלדת (1 = למעלה משמאל); 0 - מיקוד אוטומטי.
 * - * - מצב הצילום הבא. # - הבזק.
 * - BACK - סוגר את ההגדרות/המציג/הפילטרים, ואחרת יוצא.
 */
@Composable
fun CameraScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val hasCameraPermission by rememberRuntimePermission(Manifest.permission.CAMERA)
    val (_, requestAudioPermission) = rememberLazyRuntimePermission(Manifest.permission.RECORD_AUDIO)
    // המצלמה תמיד מעל תמונה חיה - הפקדים והתפריטים בצבעי הערכה הכהה.
    val darkTheme = remember(theme.accentColor) { FutureTheme(isDarkMode = true, accentColor = theme.accentColor) }
    val snackbar = rememberFutureSnackbarState()
    val sound = remember { MediaActionSound().apply { load(MediaActionSound.SHUTTER_CLICK) } }
    DisposableEffect(Unit) { onDispose { sound.release() } }

    var options by remember { mutableStateOf(CameraOptions.load(context)) }
    fun update(transform: (CameraOptions) -> CameraOptions) {
        options = transform(options)
        options.save(context)
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var extensionsManager by remember { mutableStateOf<ExtensionsManager?>(null) }
    var availableScenes by remember { mutableStateOf(listOf(SceneOption.NONE)) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var countdownRemaining by remember { mutableStateOf<Int?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var flashFeedback by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var burstShot by remember { mutableStateOf<Int?>(null) }

    var linearZoom by remember { mutableFloatStateOf(0f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var zoomBadgeUntil by remember { mutableStateOf(0L) }
    var upDownHeld by remember { mutableStateOf(false) }
    var okHeld by remember { mutableStateOf(false) }
    var shutterFocused by remember { mutableStateOf(false) }

    // נקודת המיקוד הנבחרת, בשברי רוחב/גובה של התצוגה; null = אוטומטי.
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusLocked by remember { mutableStateOf(false) }

    // מה שצולם בסשן הזה בלבד, החדש ראשון - לתצוגה המקדימה ולמציג.
    val sessionMedia = remember { mutableStateListOf<Uri>() }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showHints by remember { mutableStateOf(true) }

    val shutterFocus = remember { FocusRequester() }
    val isVideo = captureMode == CaptureMode.VIDEO

    val imageCapture = remember(options.aspect, options.photoQuality) {
        ImageCapture.Builder()
            .setResolutionSelector(resolutionFor(options.aspect))
            .setCaptureMode(
                if (options.photoQuality == PhotoQualityOption.QUALITY) ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                else ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
            )
            .build()
    }
    val recorder = remember(options.videoQuality) {
        val quality = when (options.videoQuality) {
            VideoQualityOption.SD -> Quality.SD
            VideoQualityOption.HD -> Quality.HD
            VideoQualityOption.FHD -> Quality.FHD
        }
        Recorder.Builder()
            .setQualitySelector(QualitySelector.from(quality, FallbackStrategy.lowerQualityOrHigherThan(quality)))
            .build()
    }
    val videoCapture = remember(recorder) { VideoCapture.withOutput(recorder) }
    // יציאה מהמסך באמצע הקלטה - סוגרים את ההקלטה כדי שהקובץ ייסגר תקין ולא יישאר Recording פתוח
    DisposableEffect(Unit) { onDispose { currentRecording?.stop() } }

    // ספק המצלמה ו-Extensions - פעם אחת לכל המסך.
    LaunchedEffect(hasCameraPermission) {
        if (!hasCameraPermission) return@LaunchedEffect
        val provider = ProcessCameraProvider.getInstance(context).await() ?: run { cameraAvailable = false; return@LaunchedEffect }
        cameraProvider = provider
        val manager = ExtensionsManager.getInstanceAsync(context, provider).await()
        if (manager != null) {
            val base = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
            availableScenes = SceneOption.entries.filter {
                it == SceneOption.NONE || runCatching { manager.isExtensionAvailable(base, it.mode) }.getOrDefault(false)
            }
            extensionsManager = manager
            if (options.scene !in availableScenes) update { it.copy(scene = SceneOption.NONE) }
        }
    }

    // קישור המצלמה - רק כשמשהו שבאמת משפיע על הקישור משתנה. קודם הקישור ישב
    // ב-update של ה-AndroidView, ורץ מחדש (unbindAll + bind, כמה מאות ms של
    // מסך שחור) בכל recomposition: כל שנייה של הקלטה, כל צעד זום, כל מיקוד.
    val sceneForBind = if (!isVideo && extensionsManager != null) options.scene else SceneOption.NONE
    LaunchedEffect(cameraProvider, previewView, lensFacing, isVideo, options.aspect, sceneForBind, imageCapture, videoCapture) {
        val provider = cameraProvider ?: return@LaunchedEffect
        val view = previewView ?: return@LaunchedEffect
        try {
            val base = CameraSelector.Builder().requireLensFacing(lensFacing).build()
            val manager = extensionsManager
            val selector = if (sceneForBind != SceneOption.NONE && manager != null &&
                runCatching { manager.isExtensionAvailable(base, sceneForBind.mode) }.getOrDefault(false)
            ) manager.getExtensionEnabledCameraSelector(base, sceneForBind.mode) else base
            val preview = Preview.Builder().setResolutionSelector(resolutionFor(options.aspect)).build()
                .also { it.surfaceProvider = view.surfaceProvider }
            provider.unbindAll()
            // החומרה כאן ברמת Camera2 "LIMITED" - לא מקשרים תצוגה + תמונה +
            // וידאו יחד (המסך נשאר שחור). רק מה שהמצב הנוכחי צריך.
            camera = if (isVideo) provider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
            else provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
            linearZoom = 0f
            zoomRatio = 1f
            focusPoint = null
        } catch (e: Exception) {
            cameraAvailable = false
        }
    }

    LaunchedEffect(options.flashMode, imageCapture) { imageCapture.flashMode = options.flashMode }
    LaunchedEffect(linearZoom, camera) {
        val cam = camera ?: return@LaunchedEffect
        val future = cam.cameraControl.setLinearZoom(linearZoom)
        future.addListener({ zoomRatio = cam.cameraInfo.zoomState.value?.zoomRatio ?: 1f }, ContextCompat.getMainExecutor(context))
    }
    // הפנס בווידאו: אין "הבזק" להקלטה, אז "מופעל" מדליק את הפנס בזמן ההקלטה.
    LaunchedEffect(isRecording, options.flashMode, camera) {
        camera?.cameraControl?.enableTorch(isRecording && options.flashMode == ImageCapture.FLASH_MODE_ON)
    }
    // הפילטר על התצוגה החיה: מטריצת הצבע על שכבת ה-PreviewView (TextureView).
    val liveMatrix = if (captureMode == CaptureMode.DOCUMENT) CameraFilter.DOCUMENT else options.filter.matrix()
    LaunchedEffect(previewView, liveMatrix) {
        val view = previewView ?: return@LaunchedEffect
        val paint = liveMatrix?.let { android.graphics.Paint().apply { colorFilter = android.graphics.ColorMatrixColorFilter(it) } }
        view.setLayerType(if (paint != null) View.LAYER_TYPE_HARDWARE else View.LAYER_TYPE_NONE, paint)
    }
    LaunchedEffect(sessionMedia.firstOrNull()) {
        val uri = sessionMedia.firstOrNull() ?: return@LaunchedEffect
        lastThumb = withContext(Dispatchers.IO) { CameraMedia.thumbnail(context, uri, 128) }
    }
    // רמזי המקשים מוצגים בכניסה, ונעלמים אחרי כמה שניות.
    LaunchedEffect(Unit) {
        delay(4000)
        showHints = false
    }

    // Options - החלפת מצלמה. פעיל רק במסך הצילום עצמו.
    com.future.sharednav.nav.onOptionsKeyPress {
        if (!showSettings && !showViewer && !isRecording && !busy) {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            snackbar.show(if (lensFacing == CameraSelector.LENS_FACING_FRONT) "מצלמה קדמית" else "מצלמה אחורית")
        }
    }

    BackHandler(enabled = showSettings || showViewer || showFilters || focusPoint != null) {
        when {
            showViewer -> showViewer = false
            showSettings -> showSettings = false
            showFilters -> showFilters = false
            else -> {
                focusPoint = null
                camera?.cameraControl?.cancelFocusAndMetering()
                snackbar.show("מיקוד אוטומטי")
            }
        }
    }

    fun onSaved(uri: Uri) {
        sessionMedia.add(0, uri)
    }

    fun takePhoto(onDone: (Boolean) -> Unit = {}) {
        val square = captureMode == CaptureMode.SQUARE
        val matrix = if (captureMode == CaptureMode.DOCUMENT) CameraFilter.DOCUMENT else options.filter.matrix()
        // משוב מיידי, ברגע הלחיצה ולא אחרי השמירה: צליל תריס והבהוב.
        sound.play(MediaActionSound.SHUTTER_CLICK)
        flashFeedback = true
        if (!square && matrix == null) {
            PhotoStorage.capture(
                context = context,
                imageCapture = imageCapture,
                onSaved = { onSaved(it); onDone(true) },
                onError = { snackbar.show("הצילום נכשל"); onDone(false) },
            )
        } else {
            PhotoStorage.captureProcessed(
                context = context,
                imageCapture = imageCapture,
                square = square,
                matrix = matrix,
                onSaved = { onSaved(it); onDone(true) },
                onError = { snackbar.show("הצילום נכשל"); onDone(false) },
            )
        }
    }

    fun takeBurst(shot: Int = 1) {
        burstShot = shot
        takePhoto { ok ->
            if (ok && shot < BURST_COUNT) {
                takeBurst(shot + 1)
            } else {
                burstShot = null
                busy = false
                if (ok) snackbar.show("$BURST_COUNT תמונות נשמרו")
            }
        }
    }

    fun startCapture() {
        if (!hasCameraPermission || !cameraAvailable || camera == null) return
        when (captureMode) {
            CaptureMode.VIDEO -> if (isRecording) {
                currentRecording?.stop()
                currentRecording = null
                isRecording = false
                sound.play(MediaActionSound.STOP_VIDEO_RECORDING)
            } else {
                requestAudioPermission()
                val recording = VideoStorage.startRecording(
                    context = context,
                    recorder = recorder,
                    onFinished = { uri ->
                        isRecording = false
                        recordingSeconds = 0
                        uri?.let { onSaved(it) }
                        snackbar.show("הווידאו נשמר")
                    },
                    onError = {
                        isRecording = false
                        recordingSeconds = 0
                        snackbar.show("ההקלטה נכשלה")
                    },
                )
                if (recording != null) {
                    sound.play(MediaActionSound.START_VIDEO_RECORDING)
                    currentRecording = recording
                    isRecording = true
                    recordingSeconds = 0
                }
            }
            CaptureMode.BURST -> {
                busy = true
                takeBurst()
            }
            else -> takePhoto()
        }
    }

    fun onShutterPressed() {
        if (busy) return
        if (isVideo && isRecording) {
            startCapture()
            return
        }
        if (options.timerSeconds > 0 && countdownRemaining == null) countdownRemaining = options.timerSeconds
        else if (countdownRemaining == null) startCapture()
    }

    fun focusAt(fraction: Offset) {
        val view = previewView ?: return
        val cam = camera ?: return
        val point = view.meteringPointFactory.createPoint(fraction.x * view.width, fraction.y * view.height)
        val action = FocusMeteringAction.Builder(point).setAutoCancelDuration(5, java.util.concurrent.TimeUnit.SECONDS).build()
        if (!cam.cameraInfo.isFocusMeteringSupported(action)) {
            snackbar.show("המצלמה הזו לא תומכת במיקוד ידני")
            return
        }
        focusPoint = fraction
        focusLocked = false
        val future = cam.cameraControl.startFocusAndMetering(action)
        future.addListener({
            val ok = runCatching { future.get().isFocusSuccessful }.getOrDefault(false)
            focusLocked = ok
            snackbar.show(if (ok) "המיקוד ננעל" else "לא הצליח להתמקד")
        }, ContextCompat.getMainExecutor(context))
    }

    fun openPreview() {
        if (sessionMedia.isEmpty()) {
            snackbar.show("עדיין לא צולם כלום")
            return
        }
        showViewer = true
    }

    fun nextMode() {
        if (isRecording || busy) return
        captureMode = CaptureMode.entries[(captureMode.ordinal + 1) % CaptureMode.entries.size]
    }

    // * ו-# נצרכים ברמת המערכת ומגיעים לאפליקציה רק כשידור של לחיצה קצרה (ר'
    // onStarKeyPress) - ה-KEYCODE_STAR/POUND ב-cameraKeys נשארים רק למקלדת בלי FutureUI.
    com.future.sharednav.nav.onStarKeyPress {
        if (!showSettings && !showViewer && !showFilters) nextMode()
    }
    com.future.sharednav.nav.onPoundKeyPress {
        if (!showSettings && !showViewer && !showFilters) {
            update { it.copy(flashMode = nextFlash(it.flashMode)) }
            snackbar.show(flashLabel(options.flashMode))
        }
    }

    LaunchedEffect(countdownRemaining) {
        val remaining = countdownRemaining ?: return@LaunchedEffect
        if (remaining <= 0) {
            countdownRemaining = null
            startCapture()
        } else {
            delay(1000)
            countdownRemaining = remaining - 1
        }
    }

    LaunchedEffect(isRecording) {
        while (isRecording) {
            delay(1000)
            recordingSeconds++
        }
    }

    // מקשי המצלמה. onPreviewKeyEvent כדי שמקש מוחזק יגיע לזום לפני שהפוקוס זז.
    val cameraKeys = Modifier.onPreviewKeyEvent { event ->
        if (showSettings || showViewer) return@onPreviewKeyEvent false
        val native = event.nativeKeyEvent
        // בורר הפילטרים פתוח: ימינה/שמאלה בוחרים (התצוגה החיה מתעדכנת), OK סוגר.
        if (showFilters) {
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent event.key in listOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionCenter, Key.Enter)
            val all = CameraFilter.entries
            when (event.key) {
                Key.DirectionLeft -> { update { it.copy(filter = all[(all.indexOf(it.filter) + 1).coerceAtMost(all.lastIndex)]) }; return@onPreviewKeyEvent true }
                Key.DirectionRight -> { update { it.copy(filter = all[(all.indexOf(it.filter) - 1).coerceAtLeast(0)]) }; return@onPreviewKeyEvent true }
                Key.DirectionCenter, Key.Enter -> { showFilters = false; return@onPreviewKeyEvent true }
                else -> {}
            }
        }
        when (event.key) {
            Key.DirectionUp, Key.DirectionDown -> {
                val zoomIn = event.key == Key.DirectionUp
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (native.repeatCount == 0) {
                            upDownHeld = false
                        } else {
                            upDownHeld = true
                            linearZoom = (linearZoom + if (zoomIn) ZOOM_STEP else -ZOOM_STEP).coerceIn(0f, 1f)
                            zoomBadgeUntil = System.currentTimeMillis() + 1500
                        }
                    }
                    KeyEventType.KeyUp -> if (!upDownHeld) {
                        focusManager.moveFocus(if (zoomIn) FocusDirection.Up else FocusDirection.Down)
                    }
                }
                true
            }
            // OK על כפתור הצילום: קצר = צילום, מוחזק = מיקוד במרכז ("חצי לחיצה").
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                if (!shutterFocused) return@onPreviewKeyEvent false
                when (event.type) {
                    KeyEventType.KeyDown -> {
                        if (native.repeatCount == 0) okHeld = false
                        else if (!okHeld) { okHeld = true; focusAt(Offset(0.5f, 0.5f)) }
                    }
                    KeyEventType.KeyUp -> if (!okHeld) onShutterPressed()
                }
                true
            }
            else -> {
                if (event.type != KeyEventType.KeyDown || native.repeatCount > 0) return@onPreviewKeyEvent false
                when (native.keyCode) {
                    in AndroidKeyEvent.KEYCODE_1..AndroidKeyEvent.KEYCODE_9 -> {
                        // המיקום הפיזי של הספרה על המקלדת: 1 2 3 / 4 5 6 / 7 8 9.
                        val index = native.keyCode - AndroidKeyEvent.KEYCODE_1
                        focusAt(Offset((index % 3) / 3f + 1f / 6f, (index / 3) / 3f + 1f / 6f))
                        true
                    }
                    AndroidKeyEvent.KEYCODE_0 -> {
                        focusPoint = null
                        camera?.cameraControl?.cancelFocusAndMetering()
                        snackbar.show("מיקוד אוטומטי")
                        true
                    }
                    AndroidKeyEvent.KEYCODE_STAR -> {
                        nextMode()
                        true
                    }
                    AndroidKeyEvent.KEYCODE_POUND -> {
                        update { it.copy(flashMode = nextFlash(it.flashMode)) }
                        snackbar.show(flashLabel(options.flashMode))
                        true
                    }
                    else -> false
                }
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black).then(cameraKeys)) {
            if (!hasCameraPermission) {
                CenterMessage("נדרשת הרשאת מצלמה כדי לצלם")
            } else if (!cameraAvailable) {
                CenterMessage("אין מצלמה זמינה במכשיר הזה")
            } else {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            // TextureView ולא SurfaceView - כדי שמטריצת הצבע של הפילטר
                            // תחול על התצוגה החיה.
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }.also { previewView = it }
                    },
                )
                if (captureMode == CaptureMode.SQUARE) SquareMask()
                if (options.showGrid) GridOverlay()
                focusPoint?.let { FocusReticle(it, locked = focusLocked, accent = darkTheme.accentColor) }
            }

            // שורה עליונה: הגדרות, פילטר, מד הקלטה, הבזק. מתחת לשורת המצב - המסך
            // הזה מצויר עד הקצה (ר' StatusBarInset ב-AndroidManifest).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    // מתחת לשורת המצב של FutureUI (28dp) - ב-10dp חצי מהעיגולים היה מוסתר.
                    .padding(start = 16.dp, end = 16.dp, top = com.future.sharednav.systemui.StatusBarInset.HEIGHT_DP.dp + 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
                    CameraIconButton(
                        icon = FutureIcons.Settings,
                        contentDescription = "הגדרות",
                        accentColor = theme.accentColor,
                        size = 40.dp,
                        iconSize = 20.dp,
                        onClick = { showSettings = true }
                    )
                    CameraIconButton(
                        icon = FutureIcons.FilterVintage,
                        contentDescription = "פילטרים",
                        accentColor = theme.accentColor,
                        size = 40.dp,
                        iconSize = 20.dp,
                        tint = if (options.filter != CameraFilter.NONE) theme.accentColor else Color.White,
                        onClick = { if (!isVideo) showFilters = !showFilters else snackbar.show("פילטרים זמינים בצילום תמונות") }
                    )
                }
                if (isRecording) RecordingIndicator(seconds = recordingSeconds)
                else if (options.filter != CameraFilter.NONE && !isVideo && !showFilters) OverlayChip(options.filter.label)
                ZoomBadge(zoomRatio = zoomRatio, visibleUntil = zoomBadgeUntil)
                CameraIconButton(
                    icon = flashIcon(options.flashMode),
                    contentDescription = "הבזק",
                    accentColor = theme.accentColor,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = if (options.flashMode != ImageCapture.FLASH_MODE_OFF) theme.accentColor else Color.White,
                    onClick = {
                        update { it.copy(flashMode = nextFlash(it.flashMode)) }
                        snackbar.show(flashLabel(options.flashMode))
                    }
                )
            }

            AnimatedVisibility(
                visible = showHints && hasCameraPermission && cameraAvailable,
                enter = fadeIn(FutureMotion.enter()),
                exit = fadeOut(FutureMotion.exit()),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 96.dp),
            ) {
                OverlayChip("1-9 מיקוד · OK מוחזק מיקוד במרכז · * מצב")
            }

            countdownRemaining?.let { remaining ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(remaining.toString(), color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold)
                }
            }
            burstShot?.let { shot ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OverlayChip("$shot/$BURST_COUNT")
                }
            }

            // למטה: פילטרים (כשפתוחים), בורר מצבים, ושורת הצילום.
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showFilters) {
                    FilterStrip(selected = options.filter, theme = darkTheme)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                if (!isRecording) {
                    CaptureModeSelector(selected = captureMode, theme = theme, onSelect = { if (!busy) captureMode = it })
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewThumbButton(thumb = lastThumb, accentColor = theme.accentColor, onClick = ::openPreview)
                    Box(modifier = Modifier.onFocusChanged { shutterFocused = it.hasFocus }) {
                        CameraIconButton(
                            icon = when {
                                isVideo && isRecording -> FutureIcons.Stop
                                isVideo -> FutureIcons.Videocam
                                captureMode == CaptureMode.DOCUMENT -> FutureIcons.DocumentScanner
                                else -> FutureIcons.Camera
                            },
                            contentDescription = if (isVideo) "הקלט וידאו" else "צלם",
                            accentColor = if (isRecording) CameraDanger else theme.accentColor,
                            size = 72.dp,
                            iconSize = 32.dp,
                            focusRequester = shutterFocus,
                            onClick = ::onShutterPressed
                        )
                    }
                    // מאזן את השורה - החלפת המצלמה היא מקש Options, בלי כפתור על המסך.
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }

            AnimatedVisibility(visible = flashFeedback, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.35f)))
            }
            if (flashFeedback) {
                LaunchedEffect(Unit) {
                    delay(120)
                    flashFeedback = false
                }
            }

            AnimatedVisibility(visible = showSettings, enter = fadeIn(FutureMotion.enter()), exit = fadeOut(FutureMotion.exit())) {
                CameraSettingsScreen(
                    theme = darkTheme,
                    options = options,
                    availableScenes = availableScenes,
                    onChange = { changed -> update { changed } },
                )
            }
            AnimatedVisibility(visible = showViewer, enter = fadeIn(FutureMotion.enter()), exit = fadeOut(FutureMotion.exit())) {
                PhotoViewer(theme = darkTheme, items = sessionMedia.toList())
            }

            FutureSnackbarHost(snackbar, darkTheme)

            LaunchedEffect(hasCameraPermission, cameraAvailable, showSettings, showViewer) {
                if (hasCameraPermission && cameraAvailable && !showSettings && !showViewer) {
                    runCatching { shutterFocus.requestFocus() }
                }
            }
        }
    }
}

/** ListenableFuture -> suspend, בלי תלות נוספת. */
private suspend fun <T> com.google.common.util.concurrent.ListenableFuture<T>.await(): T? =
    suspendCancellableCoroutine { cont ->
        addListener({ cont.resume(runCatching { get() }.getOrNull()) }, Runnable::run)
    }

private fun nextFlash(mode: Int): Int = when (mode) {
    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
    else -> ImageCapture.FLASH_MODE_OFF
}

private fun flashLabel(mode: Int): String = when (mode) {
    ImageCapture.FLASH_MODE_ON -> "הבזק מופעל"
    ImageCapture.FLASH_MODE_AUTO -> "הבזק אוטומטי"
    else -> "הבזק כבוי"
}

private fun flashIcon(mode: Int) = when (mode) {
    ImageCapture.FLASH_MODE_ON -> FutureIcons.FlashOn
    ImageCapture.FLASH_MODE_AUTO -> FutureIcons.FlashAuto
    else -> FutureIcons.FlashOff
}

private fun resolutionFor(aspect: AspectOption): ResolutionSelector = ResolutionSelector.Builder()
    .setAspectRatioStrategy(
        if (aspect == AspectOption.RATIO_16_9) AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        else AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
    )
    .build()

@Composable
private fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = Color.White.copy(alpha = 0.7f), fontSize = FutureTypography.body, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

/** תווית קטנה על הכהיה מעל התצוגה החיה (פילטר פעיל, מונה רצף, רמזי מקשים). */
@Composable
private fun OverlayChip(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = FutureTypography.summary,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(ScrimOverPreview)
            .padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
    )
}

/** "2.4x" ליד הפקדים העליונים, בזמן זום ושנייה וחצי אחריו. */
@Composable
private fun ZoomBadge(zoomRatio: Float, visibleUntil: Long) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(visibleUntil) {
        visible = System.currentTimeMillis() < visibleUntil
        if (visible) {
            delay((visibleUntil - System.currentTimeMillis()).coerceAtLeast(0))
            visible = false
        }
    }
    AnimatedVisibility(visible = visible || zoomRatio > 1.01f, enter = fadeIn(), exit = fadeOut()) {
        OverlayChip("%.1fx".format(zoomRatio))
    }
}

/** מסגרת המיקוד: לבנה בזמן החיפוש, בהדגשה כשהמיקוד ננעל. */
@Composable
private fun FocusReticle(fraction: Offset, locked: Boolean, accent: Color) {
    val scale by animateFloatAsState(if (locked) 1f else 1.25f, FutureMotion.focusScaleSpec, label = "reticleScale")
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val size = 64.dp
        val density = LocalDensity.current
        val x = with(density) { (maxWidth.toPx() * fraction.x - size.toPx() / 2).toInt() }
        val y = with(density) { (maxHeight.toPx() * fraction.y - size.toPx() / 2).toInt() }
        // המיקום כאן מוחלט (שבר מהרוחב מהקצה השמאלי), בלי קשר לכיוון הפריסה.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(x, y) }
                    .size(size)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .border(2.dp, if (locked) accent else Color.White, FutureShapes.sm)
            )
        }
    }
}

/** במצב ריבוע: הכהיה מעל ומתחת לריבוע שיישמר. */
@Composable
private fun SquareMask() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val side = size.width
        val top = (size.height - side) / 2f
        drawRect(ScrimOverPreview, Offset.Zero, Size(size.width, top))
        drawRect(ScrimOverPreview, Offset(0f, top + side), Size(size.width, size.height - top - side))
    }
}

/** המדיה האחרונה מהסשן בעיגול - לחיצה פותחת את המציג. */
@Composable
private fun PreviewThumbButton(thumb: Bitmap?, accentColor: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(ScrimOverPreview)
            .border(FutureDimens.focusBorderControl, if (isFocused) accentColor else Color.White.copy(alpha = 0.3f), CircleShape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        if (thumb != null) {
            Image(thumb.asImageBitmap(), contentDescription = "התמונה האחרונה", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(FutureIcons.Image, contentDescription = "התמונה האחרונה", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun RecordingIndicator(seconds: Int) {
    Row(
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(ScrimOverPreview)
            .padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(CameraDanger, CircleShape))
        Text("%d:%02d".format(seconds / 60, seconds % 60), color = Color.White, fontSize = FutureTypography.summary, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GridOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineColor = Color.White.copy(alpha = 0.5f)
        val thirdW = size.width / 3f
        val thirdH = size.height / 3f
        drawLine(lineColor, Offset(thirdW, 0f), Offset(thirdW, size.height), strokeWidth = 1.dp.toPx())
        drawLine(lineColor, Offset(thirdW * 2, 0f), Offset(thirdW * 2, size.height), strokeWidth = 1.dp.toPx())
        drawLine(lineColor, Offset(0f, thirdH), Offset(size.width, thirdH), strokeWidth = 1.dp.toPx())
        drawLine(lineColor, Offset(0f, thirdH * 2), Offset(size.width, thirdH * 2), strokeWidth = 1.dp.toPx())
    }
}

/** פס הפילטרים: גלולות על ההכהיה, הנבחר במילוי ההדגשה. ימינה/שמאלה בוחרים. */
@Composable
private fun FilterStrip(selected: CameraFilter, theme: FutureTheme) {
    val listState = rememberLazyListState()
    val index = CameraFilter.entries.indexOf(selected)
    LaunchedEffect(index) { listState.animateScrollToItem((index - 1).coerceAtLeast(0)) }
    LazyRow(
        state = listState,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = FutureDimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm),
    ) {
        itemsIndexed(CameraFilter.entries) { _, filter ->
            val isSelected = filter == selected
            Text(
                filter.label,
                color = if (isSelected) FutureContrast.onColor(theme.accentColor) else Color.White,
                fontSize = FutureTypography.summary,
                fontWeight = FutureTypography.weightMedium,
                modifier = Modifier
                    .clip(FutureShapes.pill)
                    .background(if (isSelected) theme.accentColor else ScrimOverPreview)
                    .padding(horizontal = 14.dp, vertical = 7.dp),
            )
        }
    }
}

/** בורר מצבי הצילום - פלחים קטנים מעל שורת הצילום. * עובר למצב הבא. */
@Composable
private fun CaptureModeSelector(selected: CaptureMode, theme: FutureTheme, onSelect: (CaptureMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(ScrimOverPreview)
            .padding(3.dp),
    ) {
        CaptureMode.entries.forEach { mode ->
            CaptureModeSegment(mode.label, isSelected = selected == mode, theme = theme) { onSelect(mode) }
        }
    }
}

@Composable
private fun CaptureModeSegment(label: String, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.pill
    val bgColor by androidx.compose.animation.animateColorAsState(
        if (isSelected) theme.accentColor else if (isFocused) Color.White.copy(alpha = 0.18f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "captureModeSegmentBg",
    )
    Box(
        modifier = Modifier
            .clip(shape)
            .background(bgColor)
            .border(FutureDimens.focusBorderControl, if (isFocused && isSelected) Color.White else Color.Transparent, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isSelected) FutureContrast.onColor(theme.accentColor) else Color.White, fontSize = FutureTypography.summary, fontWeight = FutureTypography.weightMedium)
    }
}
