package com.future.camera.ui

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
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
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
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
import com.future.camera.data.AspectOption
import com.future.camera.data.CameraMedia
import com.future.camera.data.CameraOptions
import com.future.camera.data.PhotoQualityOption
import com.future.camera.data.PhotoStorage
import com.future.camera.data.SceneOption
import com.future.camera.data.VideoQualityOption
import com.future.camera.data.VideoStorage
import com.future.sharednav.components.FutureSnackbarHost
import com.future.sharednav.components.rememberFutureSnackbarState
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.systemui.StatusBarInset
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class CaptureMode { PHOTO, VIDEO }

/** קפיצת הזום בכל חזרה של מקש מוחזק - בערך שנייה וחצי מ-1x עד הזום המרבי. */
private const val ZOOM_STEP = 0.035f

/**
 * מסך המצלמה - תצוגה חיה במסך מלא, הכול מהמקשים:
 *
 * - OK מפעיל את הפקד הממוקד (כפתור הצילום כברירת מחדל).
 * - Options מחליף בין המצלמה האחורית לקדמית, בלי הודעה.
 * - חץ למעלה/למטה מוחזק - זום פנימה/החוצה; לחיצה קצרה מזיזה את הפוקוס.
 * - 1-9 - נקודת מיקוד לפי מיקום הספרה על המקלדת (1 = למעלה משמאל); 0 - מיקוד אוטומטי.
 * - * - תמונה/וידאו. # - הבזק.
 * - BACK - סוגר את ההגדרות/המציג, ואחרת יוצא.
 */
@Composable
fun CameraScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val hasCameraPermission by rememberRuntimePermission(Manifest.permission.CAMERA)
    val (_, requestAudioPermission) = rememberLazyRuntimePermission(Manifest.permission.RECORD_AUDIO)
    // המצלמה תמיד מעל תמונה חיה - הפקדים והתפריטים בצבעי הערכה הכהה.
    val darkTheme = remember(theme.accentColor) { FutureTheme(isDarkMode = true, accentColor = theme.accentColor) }
    val snackbar = rememberFutureSnackbarState()

    var options by remember { mutableStateOf(CameraOptions.load(context)) }
    fun update(transform: (CameraOptions) -> CameraOptions) {
        options = transform(options)
        options.save(context)
    }

    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var extensionsManager by remember { mutableStateOf<ExtensionsManager?>(null) }
    var availableScenes by remember { mutableStateOf(listOf(SceneOption.NONE)) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var countdownRemaining by remember { mutableStateOf<Int?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }
    var flashFeedback by remember { mutableStateOf(false) }

    var linearZoom by remember { mutableFloatStateOf(0f) }
    var zoomRatio by remember { mutableFloatStateOf(1f) }
    var zoomBadgeUntil by remember { mutableStateOf(0L) }
    var upDownHeld by remember { mutableStateOf(false) }

    // נקודת המיקוד הנבחרת, בשברי רוחב/גובה של התצוגה; null = אוטומטי.
    var focusPoint by remember { mutableStateOf<Offset?>(null) }
    var focusLocked by remember { mutableStateOf(false) }

    var lastMediaUri by remember { mutableStateOf<Uri?>(null) }
    var lastThumb by remember { mutableStateOf<Bitmap?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var showViewer by remember { mutableStateOf(false) }

    val shutterFocus = remember { FocusRequester() }

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

    // המדיה האחרונה - לתצוגה המקדימה ליד כפתור הצילום, גם אחרי פתיחה מחדש.
    LaunchedEffect(Unit) {
        val latest = withContext(Dispatchers.IO) { CameraMedia.recentPhotos(context, limit = 1).firstOrNull() }
        if (latest != null && lastMediaUri == null) lastMediaUri = latest
    }
    LaunchedEffect(lastMediaUri) {
        val uri = lastMediaUri ?: return@LaunchedEffect
        lastThumb = withContext(Dispatchers.IO) { CameraMedia.thumbnail(context, uri, 128) }
    }

    // Options - החלפת מצלמה, בלי Toast. פעיל רק במסך הצילום עצמו.
    com.future.sharednav.nav.onOptionsKeyPress {
        if (!showSettings && !showViewer && !isRecording) {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            focusPoint = null
        }
    }

    BackHandler(enabled = showSettings || showViewer || focusPoint != null) {
        when {
            showViewer -> showViewer = false
            showSettings -> showSettings = false
            else -> {
                focusPoint = null
                camera?.cameraControl?.cancelFocusAndMetering()
            }
        }
    }

    fun startCapture() {
        if (!hasCameraPermission || !cameraAvailable) return
        when (captureMode) {
            CaptureMode.PHOTO -> PhotoStorage.capture(
                context = context,
                imageCapture = imageCapture,
                onSaved = { uri ->
                    lastMediaUri = uri
                    flashFeedback = true
                },
                onError = { snackbar.show("הצילום נכשל") },
            )
            CaptureMode.VIDEO -> if (isRecording) {
                currentRecording?.stop()
                currentRecording = null
                isRecording = false
            } else {
                requestAudioPermission()
                val recording = VideoStorage.startRecording(
                    context = context,
                    recorder = recorder,
                    onFinished = { uri ->
                        isRecording = false
                        recordingSeconds = 0
                        uri?.let { lastMediaUri = it }
                        snackbar.show("הווידאו נשמר")
                    },
                    onError = {
                        isRecording = false
                        recordingSeconds = 0
                        snackbar.show("ההקלטה נכשלה")
                    },
                )
                if (recording != null) {
                    currentRecording = recording
                    isRecording = true
                    recordingSeconds = 0
                }
            }
        }
    }

    fun onShutterPressed() {
        if (captureMode == CaptureMode.VIDEO && isRecording) {
            startCapture()
            return
        }
        if (options.timerSeconds > 0 && countdownRemaining == null) countdownRemaining = options.timerSeconds
        else if (countdownRemaining == null) startCapture()
    }

    fun focusAt(fraction: Offset) {
        val view = previewView ?: return
        val cam = camera ?: return
        focusPoint = fraction
        focusLocked = false
        val point = view.meteringPointFactory.createPoint(fraction.x * view.width, fraction.y * view.height)
        val action = FocusMeteringAction.Builder(point).setAutoCancelDuration(5, java.util.concurrent.TimeUnit.SECONDS).build()
        val future = cam.cameraControl.startFocusAndMetering(action)
        future.addListener({
            focusLocked = runCatching { future.get().isFocusSuccessful }.getOrDefault(false)
        }, ContextCompat.getMainExecutor(context))
    }

    fun openPreview() {
        val uri = lastMediaUri
        if (uri == null) {
            snackbar.show("עדיין לא צולם כלום")
            return
        }
        val isVideo = context.contentResolver.getType(uri)?.startsWith("video") == true
        if (!isVideo) {
            showViewer = true
            return
        }
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                .addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            context.startActivity(intent)
        } catch (e: Exception) {
            snackbar.show("אין נגן שיכול להציג את זה")
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
                        true
                    }
                    AndroidKeyEvent.KEYCODE_STAR -> {
                        if (!isRecording) captureMode = if (captureMode == CaptureMode.PHOTO) CaptureMode.VIDEO else CaptureMode.PHOTO
                        true
                    }
                    AndroidKeyEvent.KEYCODE_POUND -> {
                        update { it.copy(flashMode = nextFlash(it.flashMode)) }
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
                CameraPreview(
                    lensFacing = lensFacing,
                    captureMode = captureMode,
                    aspect = options.aspect,
                    scene = options.scene,
                    extensionsManager = extensionsManager,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture,
                    onPreviewView = { previewView = it },
                    onExtensions = { manager, scenes ->
                        extensionsManager = manager
                        availableScenes = scenes
                        if (options.scene !in scenes) update { it.copy(scene = SceneOption.NONE) }
                    },
                    onBindFailed = { cameraAvailable = false },
                    onCameraReady = { bound ->
                        camera = bound
                        linearZoom = 0f
                        zoomRatio = 1f
                    },
                )
                if (options.showGrid) GridOverlay()
                focusPoint?.let { FocusReticle(it, locked = focusLocked, accent = darkTheme.accentColor) }
            }

            // שורה עליונה: הגדרות, מד הקלטה, הבזק. מתחת לשורת המצב - המסך הזה
            // מצויר עד הקצה (ר' StatusBarInset ב-AndroidManifest).
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(start = 16.dp, end = 16.dp, top = StatusBarInset.HEIGHT_DP.dp + 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraIconButton(
                    icon = FutureIcons.Settings,
                    contentDescription = "הגדרות",
                    accentColor = theme.accentColor,
                    size = 40.dp,
                    iconSize = 20.dp,
                    onClick = { showSettings = true }
                )
                if (isRecording) RecordingIndicator(seconds = recordingSeconds)
                ZoomBadge(zoomRatio = zoomRatio, visibleUntil = zoomBadgeUntil)
                CameraIconButton(
                    icon = flashIcon(options.flashMode),
                    contentDescription = "הבזק",
                    accentColor = theme.accentColor,
                    size = 40.dp,
                    iconSize = 20.dp,
                    tint = if (options.flashMode != ImageCapture.FLASH_MODE_OFF) theme.accentColor else Color.White,
                    onClick = { update { it.copy(flashMode = nextFlash(it.flashMode)) } }
                )
            }

            countdownRemaining?.let { remaining ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(remaining.toString(), color = Color.White, fontSize = 96.sp, fontWeight = FontWeight.Bold)
                }
            }

            // למטה: בורר תמונה/וידאו, ושורת הצילום - תצוגה מקדימה, כפתור צילום.
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (!isRecording) {
                    CaptureModeSelector(selected = captureMode, theme = theme, onSelect = { captureMode = it })
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PreviewThumbButton(thumb = lastThumb, accentColor = theme.accentColor, onClick = ::openPreview)
                    CameraIconButton(
                        icon = if (captureMode == CaptureMode.VIDEO) {
                            if (isRecording) FutureIcons.Stop else FutureIcons.Videocam
                        } else FutureIcons.Camera,
                        contentDescription = if (captureMode == CaptureMode.VIDEO) "הקלט וידאו" else "צלם",
                        accentColor = if (isRecording) CameraDanger else theme.accentColor,
                        size = 72.dp,
                        iconSize = 32.dp,
                        focusRequester = shutterFocus,
                        onClick = ::onShutterPressed
                    )
                    // מאזן את השורה - החלפת המצלמה היא מקש Options, בלי כפתור על המסך.
                    Spacer(modifier = Modifier.size(52.dp))
                }
            }

            AnimatedVisibility(visible = flashFeedback, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.25f)))
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
                PhotoViewer(theme = darkTheme, startUri = lastMediaUri)
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

private fun nextFlash(mode: Int): Int = when (mode) {
    ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
    ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
    else -> ImageCapture.FLASH_MODE_OFF
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
        Text(
            "%.1fx".format(zoomRatio),
            color = Color.White,
            fontSize = FutureTypography.summary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(FutureShapes.pill)
                .background(ScrimOverPreview)
                .padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
        )
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

/** התמונה האחרונה בעיגול במקום אייקון גלריה - לחיצה פותחת את המציג. */
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
            Icon(Icons.Rounded.PhotoLibrary, contentDescription = "התמונה האחרונה", tint = Color.White, modifier = Modifier.size(24.dp))
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

/** בורר בין צילום תמונה לווידאו - שני פלחים קטנים מעל שורת הצילום. */
@Composable
private fun CaptureModeSelector(selected: CaptureMode, theme: FutureTheme, onSelect: (CaptureMode) -> Unit) {
    Row(
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(ScrimOverPreview)
            .padding(3.dp),
    ) {
        CaptureModeSegment("תמונה", isSelected = selected == CaptureMode.PHOTO, theme = theme) { onSelect(CaptureMode.PHOTO) }
        CaptureModeSegment("וידאו", isSelected = selected == CaptureMode.VIDEO, theme = theme) { onSelect(CaptureMode.VIDEO) }
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
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isSelected) FutureContrast.onColor(theme.accentColor) else Color.White, fontSize = FutureTypography.summary, fontWeight = FutureTypography.weightMedium)
    }
}

@Composable
private fun CameraPreview(
    lensFacing: Int,
    captureMode: CaptureMode,
    aspect: AspectOption,
    scene: SceneOption,
    extensionsManager: ExtensionsManager?,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    onPreviewView: (PreviewView) -> Unit,
    onExtensions: (ExtensionsManager, List<SceneOption>) -> Unit,
    onBindFailed: () -> Unit,
    onCameraReady: (Camera) -> Unit,
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentOnBindFailed by rememberUpdatedState(onBindFailed)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)
    val currentOnExtensions by rememberUpdatedState(onExtensions)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }.also(onPreviewView) },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val baseSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    // אילו מצבי צילום מיוחדים החומרה תומכת בהם - נבדק פעם אחת.
                    if (extensionsManager == null) {
                        val future = ExtensionsManager.getInstanceAsync(context, cameraProvider)
                        future.addListener({
                            runCatching { future.get() }.getOrNull()?.let { manager ->
                                val scenes = SceneOption.entries.filter {
                                    it == SceneOption.NONE || runCatching { manager.isExtensionAvailable(baseSelector, it.mode) }.getOrDefault(false)
                                }
                                currentOnExtensions(manager, scenes)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                    val selector = if (scene != SceneOption.NONE && captureMode == CaptureMode.PHOTO && extensionsManager != null &&
                        runCatching { extensionsManager.isExtensionAvailable(baseSelector, scene.mode) }.getOrDefault(false)
                    ) extensionsManager.getExtensionEnabledCameraSelector(baseSelector, scene.mode) else baseSelector

                    val preview = Preview.Builder().setResolutionSelector(resolutionFor(aspect)).build()
                        .also { it.surfaceProvider = previewView.surfaceProvider }
                    cameraProvider.unbindAll()
                    // החומרה כאן ברמת Camera2 "LIMITED" - לא מקשרים תצוגה + תמונה +
                    // וידאו יחד (המסך נשאר שחור). רק מה שהמצב הנוכחי צריך.
                    val boundCamera = if (captureMode == CaptureMode.VIDEO) {
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, videoCapture)
                    } else {
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                    }
                    currentOnCameraReady(boundCamera)
                } catch (e: Exception) {
                    currentOnBindFailed()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
