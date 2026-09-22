package com.future.camera.ui
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.readableAccentColor
import androidx.compose.material.icons.rounded.Check

import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Timer10
import androidx.compose.material.icons.rounded.Timer3
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.future.camera.data.PhotoStorage
import com.future.camera.data.VideoStorage
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

private enum class CaptureMode { PHOTO, VIDEO }

/** מסך המצלמה היחיד באפליקציה - תצוגה חיה במסך מלא, ללא מגע (כל הפעולות
 * דרך כפתורי OK/D-pad על שורות הבקרות למעלה ולמטה).
 *
 * מעבר לצילום תמונה בסיסי, כולל: וידאו (מעבר מצב + כפתור הקלטה אדום +
 * טיימר הקלטה), זום (כפתורי +/− עם אחוז נוכחי), טיימר עצמי (כיבוי/3/10
 * שניות עם ספירה לאחור על המסך), וקווי רשת (כלל השלישים) - כל אלה חסרו
 * לגמרי במסך הבסיסי הקודם. */
@Composable
fun CameraScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val hasCameraPermission by rememberRuntimePermission(Manifest.permission.CAMERA)
    val (hasAudioPermission, requestAudioPermission) = rememberLazyRuntimePermission(Manifest.permission.RECORD_AUDIO)

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lastMediaUri by remember { mutableStateOf<Uri?>(null) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var flashFeedback by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var maxZoomRatio by remember { mutableStateOf(1f) }
    var captureMode by remember { mutableStateOf(CaptureMode.PHOTO) }
    var showGrid by remember { mutableStateOf(false) }
    var timerSeconds by remember { mutableStateOf(0) }
    var countdownRemaining by remember { mutableStateOf<Int?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableStateOf(0) }
    var currentRecording by remember { mutableStateOf<Recording?>(null) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val recorder = remember { Recorder.Builder().setQualitySelector(QualitySelector.from(Quality.HD)).build() }
    val videoCapture = remember { VideoCapture.withOutput(recorder) }
    val shutterFocus = remember { FocusRequester() }

    LaunchedEffect(flashMode) { imageCapture.flashMode = flashMode }
    LaunchedEffect(zoomRatio, camera) { camera?.cameraControl?.setZoomRatio(zoomRatio) }

    // מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לאפליקציה -
    // כאן פותח את תפריט הגדרות הצילום (רשת/טיימר), בדיוק כמו בכל שאר האפליקציות.
    var showSettingsMenu by remember { mutableStateOf(false) }
    com.future.sharednav.nav.onOptionsKeyPress { showSettingsMenu = true }

    fun startCapture() {
        if (!hasCameraPermission || !cameraAvailable) return
        when (captureMode) {
            CaptureMode.PHOTO -> {
                PhotoStorage.capture(
                    context = context,
                    imageCapture = imageCapture,
                    onSaved = { uri ->
                        lastMediaUri = uri
                        flashFeedback = true
                        Toast.makeText(context, "התמונה נשמרה", Toast.LENGTH_SHORT).show()
                    },
                    onError = { Toast.makeText(context, "הצילום נכשל, נסה שוב", Toast.LENGTH_SHORT).show() }
                )
            }
            CaptureMode.VIDEO -> {
                if (isRecording) {
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
                            Toast.makeText(context, "הווידאו נשמר", Toast.LENGTH_SHORT).show()
                        },
                        onError = {
                            isRecording = false
                            recordingSeconds = 0
                            Toast.makeText(context, "ההקלטה נכשלה", Toast.LENGTH_SHORT).show()
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
    }

    // כפתור הצילום עצמו: אם יש טיימר עצמי פעיל ולא באמצע ספירה כבר, מתחיל
    // ספירה לאחור במקום לצלם מיד - הצילום/ההקלטה בפועל קורים כשהספירה מגיעה ל-0.
    fun onShutterPressed() {
        if (captureMode == CaptureMode.VIDEO && isRecording) {
            startCapture()
            return
        }
        if (timerSeconds > 0 && countdownRemaining == null) {
            countdownRemaining = timerSeconds
        } else if (countdownRemaining == null) {
            startCapture()
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

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (!hasCameraPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "נדרשת הרשאת מצלמה כדי לצלם",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = FutureTypography.body,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else if (!cameraAvailable) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "אין מצלמה זמינה במכשיר הזה",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = FutureTypography.body,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                CameraPreview(
                    lensFacing = lensFacing,
                    captureMode = captureMode,
                    imageCapture = imageCapture,
                    videoCapture = videoCapture,
                    onBindFailed = { cameraAvailable = false },
                    onCameraReady = { boundCamera ->
                        camera = boundCamera
                        maxZoomRatio = boundCamera.cameraInfo.zoomState.value?.maxZoomRatio ?: 1f
                        zoomRatio = 1f
                    },
                )
                if (showGrid) GridOverlay()
            }

            // בלי מעברי צבע בשולי המסך: "No protection gradients; a scrim or a solid
            // capsule does that job" (README של הדיזיין סיסטם). כל פקד כאן כבר
            // יושב על עיגול/קפסולה בהכהיה של המערכת.

            // שורה עליונה: חזרה, מד זמן הקלטה (במצב וידאו פעיל), רשת/טיימר/הבזק.
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "חזור",
                    accentColor = theme.accentColor,
                    size = 40.dp,
                    iconSize = 20.dp,
                    onClick = onExit
                )

                if (isRecording) {
                    RecordingIndicator(seconds = recordingSeconds)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CameraIconButton(
                        icon = when (timerSeconds) {
                            3 -> Icons.Rounded.Timer3
                            10 -> Icons.Rounded.Timer10
                            else -> Icons.Rounded.Timer
                        },
                        contentDescription = "טיימר עצמי",
                        accentColor = theme.accentColor,
                        size = 40.dp,
                        iconSize = 18.dp,
                        tint = if (timerSeconds > 0) theme.accentColor else Color.White,
                        onClick = { timerSeconds = when (timerSeconds) { 0 -> 3; 3 -> 10; else -> 0 } }
                    )
                    CameraIconButton(
                        icon = Icons.Rounded.GridOn,
                        contentDescription = "קווי רשת",
                        accentColor = theme.accentColor,
                        size = 40.dp,
                        iconSize = 18.dp,
                        tint = if (showGrid) theme.accentColor else Color.White,
                        onClick = { showGrid = !showGrid }
                    )
                    CameraIconButton(
                        icon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                            else -> Icons.Rounded.FlashOff
                        },
                        contentDescription = "הבזק",
                        accentColor = theme.accentColor,
                        size = 40.dp,
                        iconSize = 18.dp,
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        }
                    )
                }
            }

            countdownRemaining?.let { remaining ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        remaining.toString(),
                        color = Color.White,
                        fontSize = 96.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            // עמודת פקדים תחתונה: זום, בורר מצב (תמונה/וידאו), ואז שורת הצילום עצמה.
            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (maxZoomRatio > 1.05f) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        CameraIconButton(
                            icon = Icons.Rounded.Remove,
                            contentDescription = "התרחק",
                            accentColor = theme.accentColor,
                            size = 36.dp,
                            iconSize = 16.dp,
                            onClick = { zoomRatio = (zoomRatio - 0.5f).coerceAtLeast(1f) }
                        )
                        Text(
                            "%.1fx".format(zoomRatio),
                            color = Color.White,
                            fontSize = FutureTypography.summary,
                            fontWeight = FontWeight.Medium,
                        )
                        CameraIconButton(
                            icon = Icons.Rounded.Add,
                            contentDescription = "התקרב",
                            accentColor = theme.accentColor,
                            size = 36.dp,
                            iconSize = 16.dp,
                            onClick = { zoomRatio = (zoomRatio + 0.5f).coerceAtMost(maxZoomRatio) }
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (!isRecording) {
                    CaptureModeSelector(
                        selected = captureMode,
                        theme = theme,
                        onSelect = { captureMode = it },
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CameraIconButton(
                        icon = Icons.Rounded.PhotoLibrary,
                        contentDescription = "המדיה האחרונה",
                        accentColor = theme.accentColor,
                        onClick = {
                            val uri = lastMediaUri
                            if (uri != null) {
                                try {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "אין אפליקציה שיכולה להציג את זה", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "עדיין לא צולם כלום", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    CameraIconButton(
                        icon = if (captureMode == CaptureMode.VIDEO) {
                            if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Videocam
                        } else Icons.Rounded.Camera,
                        contentDescription = if (captureMode == CaptureMode.VIDEO) "הקלט וידאו" else "צלם",
                        accentColor = if (isRecording) CameraDanger else theme.accentColor,
                        size = 72.dp,
                        iconSize = 32.dp,
                        focusRequester = shutterFocus,
                        onClick = ::onShutterPressed
                    )

                    CameraIconButton(
                        icon = Icons.Rounded.Cameraswitch,
                        contentDescription = "החלף מצלמה",
                        accentColor = theme.accentColor,
                        onClick = {
                            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_BACK) {
                                CameraSelector.LENS_FACING_FRONT
                            } else {
                                CameraSelector.LENS_FACING_BACK
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = flashFeedback,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.25f)))
            }
            if (flashFeedback) {
                LaunchedEffect(Unit) {
                    delay(120)
                    flashFeedback = false
                }
            }

            if (showSettingsMenu) {
                CameraSettingsMenu(
                    theme = theme,
                    showGrid = showGrid,
                    timerSeconds = timerSeconds,
                    onToggleGrid = { showGrid = !showGrid },
                    onSelectTimer = { timerSeconds = it },
                    onDismiss = { showSettingsMenu = false },
                )
            }

            LaunchedEffect(hasCameraPermission, cameraAvailable) {
                if (hasCameraPermission && cameraAvailable) shutterFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun RecordingIndicator(seconds: Int) {
    val minutes = seconds / 60
    val secs = seconds % 60
    Row(
        modifier = Modifier
            .clip(FutureShapes.pill)
            .background(ScrimOverPreview)
            .padding(horizontal = FutureDimens.spacingMd, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).background(CameraDanger, CircleShape))
        Text("%d:%02d".format(minutes, secs), color = Color.White, fontSize = FutureTypography.summary, fontWeight = FontWeight.Medium)
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

/** בורר בין צילום תמונה לוידאו - שני פלחים קטנים מעל שורת הצילום. */
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
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    // צ'יפ מעל תמונה חיה: נבחר = מילוי בהדגשה עם הדיו שלה; ממוקד = 18% לבן.
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 18.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isSelected) FutureContrast.onColor(theme.accentColor) else Color.White, fontSize = FutureTypography.summary, fontWeight = FutureTypography.weightMedium)
    }
}

@Composable
private fun CameraSettingsMenu(
    theme: FutureTheme,
    showGrid: Boolean,
    timerSeconds: Int,
    onToggleGrid: () -> Unit,
    onSelectTimer: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = "מצלמה") {
        CameraSettingsRow("קווי רשת", theme = theme, isOn = showGrid, onClick = onToggleGrid)
        CameraSettingsRow("טיימר עצמי כבוי", theme = theme, isOn = timerSeconds == 0, onClick = { onSelectTimer(0) })
        CameraSettingsRow("טיימר עצמי 3 שניות", theme = theme, isOn = timerSeconds == 3, onClick = { onSelectTimer(3) })
        CameraSettingsRow("טיימר עצמי 10 שניות", theme = theme, isOn = timerSeconds == 10, onClick = { onSelectTimer(10) })
    }
}

/** שורת תפריט; האפשרות הפעילה מסומנת בסימן V בהדגשה בסוף השורה. */
@Composable
private fun CameraSettingsRow(label: String, theme: FutureTheme, isOn: Boolean, onClick: () -> Unit) {
    FutureMenuRow(
        label = label,
        icon = null,
        theme = theme,
        onClick = onClick,
        trailing = if (isOn) {
            { Icon(Icons.Rounded.Check, contentDescription = null, tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconMenuRow)) }
        } else null,
    )
}

@Composable
private fun CameraPreview(
    lensFacing: Int,
    captureMode: CaptureMode,
    imageCapture: ImageCapture,
    videoCapture: VideoCapture<Recorder>,
    onBindFailed: () -> Unit,
    onCameraReady: (Camera) -> Unit,
) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentOnBindFailed by rememberUpdatedState(onBindFailed)
    val currentOnCameraReady by rememberUpdatedState(onCameraReady)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx -> PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } },
        update = { previewView ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                    val selector = CameraSelector.Builder().requireLensFacing(lensFacing).build()
                    cameraProvider.unbindAll()
                    // חומרת המצלמה כאן ברמת Camera2 "LIMITED" (ר' Camera2CameraInfo בלוגים) -
                    // לא תומכת בקישור בו-זמנית של תצוגה חיה + צילום תמונה + הקלטת וידאו
                    // (3 זרמים) יחד; הניסיון הראשוני לקשר את כולם תמיד גרם למסך שחור לגמרי
                    // תקוע (משא-ומתן על session שלעולם לא מצליח). קושרים רק את מה שהמצב
                    // הנוכחי צריך בפועל - זה כן עובד, במחיר rebind קצר במעבר תמונה/וידאו.
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
