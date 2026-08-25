package com.future.tools.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

/**
 * תצוגת מצלמה חיה (preview) עם ניתוח פריים-אחר-פריים דרך CameraX, משותפת בין
 * סורק הקודים (QR/ברקוד) לסורק הטקסט (OCR) - שני הכלים היחידים שבפועל צריכים
 * גישה חיה למצלמה, לא רק צילום בודד. onFrame אחראי לסגור כל ImageProxy
 * (proxy.close()) כשסיים לעבד אותו, אחרת הניתוח הבא ננעל.
 */
@Composable
fun CameraAnalyzerView(modifier: Modifier = Modifier, onFrame: (ImageProxy) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                            currentOnFrame(proxy)
                        }
                    }
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    // אין מצלמה אחורית זמינה - התצוגה תישאר ריקה
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
