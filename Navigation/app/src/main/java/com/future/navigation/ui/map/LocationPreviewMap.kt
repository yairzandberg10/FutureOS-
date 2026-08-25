package com.future.navigation.ui.map

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.future.navigation.data.common.LatLng
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * תצוגה מקדימה סטטית של המפה סביב המיקום הנוכחי - למסך הבית, כדי שנראה
 * מיד "איפה אני" בפתיחת האפליקציה. בכוונה בלי onKeyEvent ובלי focusable:
 * מקשי D-pad במסך הבית משמשים לניווט בין שדה החיפוש/כפתורים/רשימה, לא
 * להזזת המפה הזו (זה תפקידו היחיד של NavMapView, במסך הניווט החי).
 */
@Composable
fun LocationPreviewMap(location: LatLng?, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    var mapLibreMap: MapLibreMap? by remember { mutableStateOf<MapLibreMap?>(null) }

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
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(location, mapLibreMap) {
        val map = mapLibreMap
        if (map != null && location != null) {
            val target = MapLibreLatLng(location.lat, location.lon)
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, PREVIEW_ZOOM))
            map.clear()
            map.addMarker(MarkerOptions().position(target))
        }
    }

    AndroidView(
        factory = {
            mapView.getMapAsync { map ->
                map.uiSettings.setAllGesturesEnabled(false)
                map.setStyle(MapConfig.STYLE_URL)
                mapLibreMap = map
            }
            mapView
        },
        modifier = modifier.fillMaxSize()
    )
}

private const val PREVIEW_ZOOM = 14.0
