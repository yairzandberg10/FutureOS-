package com.future.navigation.ui.map

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.future.navigation.data.common.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.maps.MapView

/**
 * מפה אמיתית (MapLibre + אריחי OpenFreeMap), עם מצלמה שזזה אך ורק בלחיצות
 * D-pad - בדיוק כמו כל מסך אחר ב-FutureOS (ר' Calendar's day-grid onKeyEvent
 * לתבנית הקוד). כל מחוות המגע של MapLibre מבוטלות ב-MapCameraController.attach.
 *
 * מיפוי מקשים (זהה בכל מקום שבו המפה הזו מוצגת):
 * - חצי D-pad: הזזת המפה (pan) בכיוון הנלחץ
 * - מרכז/Enter: חזרה למיקום שלי (recenter) וחידוש מצב מעקב
 * - * (KEYCODE_STAR): התרחקות (zoom out); # (KEYCODE_POUND): התקרבות (zoom in)
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun NavMapView(
    modifier: Modifier = Modifier,
    cameraController: MapCameraController,
    onRecenterRequested: () -> Unit,
    onManualPan: () -> Unit = {},
    initialCenter: LatLng?
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusRequester = remember { FocusRequester() }

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            cameraController.detach()
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { cameraController.pan(1.0, 0.0); onManualPan(); true }
                    Key.DirectionDown -> { cameraController.pan(-1.0, 0.0); onManualPan(); true }
                    Key.DirectionLeft -> { cameraController.pan(0.0, -1.0); onManualPan(); true }
                    Key.DirectionRight -> { cameraController.pan(0.0, 1.0); onManualPan(); true }
                    Key.DirectionCenter, Key.Enter -> { onRecenterRequested(); true }
                    Key.Pound -> { cameraController.zoomIn(); true }
                    // KEYCODE_STAR נחשף ב-Compose בשם Key.Multiply (לא Key.Star).
                    Key.Multiply -> { cameraController.zoomOut(); true }
                    else -> false
                }
            }
    ) {
        AndroidView(
            factory = {
                mapView.getMapAsync { map ->
                    map.setStyle(MapConfig.STYLE_URL)
                    cameraController.attach(map)
                    if (initialCenter != null) {
                        cameraController.animateTo(initialCenter, durationMs = 0)
                    }
                }
                mapView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
