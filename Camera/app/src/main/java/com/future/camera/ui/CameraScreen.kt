package com.future.camera.ui

import android.Manifest
import android.net.Uri
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Camera
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashAuto
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.future.camera.data.PhotoStorage
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

/** מסך המצלמה היחיד באפליקציה - תצוגה חיה במסך מלא, ללא מגע (כל הפעולות
 * דרך כפתורי OK/D-pad על שורת הבקרות בתחתית). */
@Composable
fun CameraScreen(theme: FutureTheme, onExit: () -> Unit) {
    val context = LocalContext.current
    val hasPermission by rememberRuntimePermission(Manifest.permission.CAMERA)

    var lensFacing by remember { mutableStateOf(CameraSelector.LENS_FACING_BACK) }
    var flashMode by remember { mutableStateOf(ImageCapture.FLASH_MODE_OFF) }
    var lastPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraAvailable by remember { mutableStateOf(true) }
    var flashFeedback by remember { mutableStateOf(false) }

    val imageCapture = remember { ImageCapture.Builder().build() }
    val shutterFocus = remember { FocusRequester() }

    LaunchedEffect(flashMode) { imageCapture.flashMode = flashMode }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (!hasPermission) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "נדרשת הרשאת מצלמה כדי לצלם",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else if (!cameraAvailable) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "אין מצלמה זמינה במכשיר הזה",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                CameraPreview(
                    lensFacing = lensFacing,
                    imageCapture = imageCapture,
                    onBindFailed = { cameraAvailable = false }
                )
            }

            // רקע שקוף בשולי המסך כדי שהכפתורים הצפים יישארו קריאים מעל התצוגה החיה.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.TopCenter)
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.6f))))
            )

            // שורה עליונה: חזרה + מיתוג מצב הבזק נוכחי
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp).align(Alignment.TopCenter),
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
            }

            // שורה תחתונה: תמונה אחרונה, כפתור צילום מרכזי, החלפת מצלמה, הבזק
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp).align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CameraIconButton(
                    icon = Icons.Rounded.PhotoLibrary,
                    contentDescription = "התמונה האחרונה",
                    accentColor = theme.accentColor,
                    onClick = {
                        val uri = lastPhotoUri
                        if (uri != null) {
                            try {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "אין אפליקציה שיכולה להציג את התמונה", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "עדיין לא צולמה תמונה", Toast.LENGTH_SHORT).show()
                        }
                    }
                )

                CameraIconButton(
                    icon = Icons.Rounded.Camera,
                    contentDescription = "צלם",
                    accentColor = theme.accentColor,
                    size = 72.dp,
                    iconSize = 32.dp,
                    focusRequester = shutterFocus,
                    onClick = {
                        if (hasPermission && cameraAvailable) {
                            PhotoStorage.capture(
                                context = context,
                                imageCapture = imageCapture,
                                onSaved = { uri ->
                                    lastPhotoUri = uri
                                    flashFeedback = true
                                    Toast.makeText(context, "התמונה נשמרה", Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    Toast.makeText(context, "הצילום נכשל, נסה שוב", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CameraIconButton(
                        icon = when (flashMode) {
                            ImageCapture.FLASH_MODE_ON -> Icons.Rounded.FlashOn
                            ImageCapture.FLASH_MODE_AUTO -> Icons.Rounded.FlashAuto
                            else -> Icons.Rounded.FlashOff
                        },
                        contentDescription = "הבזק",
                        accentColor = theme.accentColor,
                        onClick = {
                            flashMode = when (flashMode) {
                                ImageCapture.FLASH_MODE_OFF -> ImageCapture.FLASH_MODE_AUTO
                                ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
                                else -> ImageCapture.FLASH_MODE_OFF
                            }
                        }
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

            LaunchedEffect(hasPermission, cameraAvailable) {
                if (hasPermission && cameraAvailable) shutterFocus.requestFocus()
            }
        }
    }
}

@Composable
private fun CameraPreview(lensFacing: Int, imageCapture: ImageCapture, onBindFailed: () -> Unit) {
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val context = LocalContext.current
    val currentOnBindFailed by rememberUpdatedState(onBindFailed)

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
                    cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                } catch (e: Exception) {
                    currentOnBindFailed()
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}
