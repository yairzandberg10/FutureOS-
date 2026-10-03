package com.future.navigation.ui.scan

import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.atomic.AtomicBoolean

/**
 * מצלמה אחורית חיה שמחפשת קוד QR ומחזירה את התוכן הראשון שנמצא. אותו צינור
 * של סורק הקודים באפליקציית הכלים (CameraX + ML Kit בגרסה המובנית, שלא
 * צריכה שירותי Google Play), עם שני הבדלים: רק QR, והמצלמה משתחררת כשיוצאים
 * מהמסך - בלי זה היא נשארת קשורה ל-Activity וממשיכה לנתח פריימים ברקע.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
fun BusQrCamera(modifier: Modifier = Modifier, onQr: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnQr by rememberUpdatedState(onQr)
    val scanner = remember {
        BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build())
    }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }
    val busy = remember { AtomicBoolean(false) }

    DisposableEffect(Unit) {
        onDispose {
            if (providerFuture.isDone) runCatching { providerFuture.get().unbindAll() }
            scanner.close()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            providerFuture.addListener({
                val provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also { imageAnalysis ->
                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                            val mediaImage = proxy.image
                            if (mediaImage == null || !busy.compareAndSet(false, true)) {
                                proxy.close()
                                return@setAnalyzer
                            }
                            scanner.process(InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees))
                                .addOnSuccessListener { barcodes ->
                                    barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue?.let { currentOnQr(it) }
                                }
                                .addOnCompleteListener {
                                    busy.set(false)
                                    proxy.close()
                                }
                        }
                    }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                } catch (e: Exception) {
                    // אין מצלמה אחורית זמינה - התצוגה נשארת ריקה, ונשאר "בחר קו בלי סריקה".
                }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}
