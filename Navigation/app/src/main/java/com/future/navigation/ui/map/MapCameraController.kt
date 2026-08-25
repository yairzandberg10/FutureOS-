package com.future.navigation.ui.map

import com.future.navigation.data.common.LatLng as AppLatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.maps.MapLibreMap

/**
 * עוטף את ה-MapLibreMap ומזיז את המצלמה אך ורק פרוגרמטית - אף פעם לא דרך
 * מחוות מגע (הן מבוטלות לגמרי ב-NavMapView, כי אין מסך מגע על המכשיר האמיתי).
 * כל תזוזה במסך הניווט מגיעה מלחיצות D-pad דרך המסך שמחזיק ב-instance הזה.
 */
class MapCameraController {
    private var map: MapLibreMap? = null
    private var currentZoom: Double = DEFAULT_ZOOM

    fun attach(map: MapLibreMap) {
        this.map = map
        map.uiSettings.setAllGesturesEnabled(false)
        currentZoom = map.cameraPosition.zoom.takeIf { it > 0 } ?: DEFAULT_ZOOM
    }

    fun detach() {
        map = null
    }

    fun animateTo(target: AppLatLng, zoom: Double = currentZoom, durationMs: Int = 400) {
        currentZoom = zoom
        val update = CameraUpdateFactory.newLatLngZoom(MapLibreLatLng(target.lat, target.lon), zoom)
        // easeCamera דורש משך אנימציה חיובי - עבור מיצוב מיידי (למשל המיקום
        // הראשוני כשהמפה נטענת) חייבים moveCamera, אחרת MapLibre זורק
        // IllegalArgumentException("Null duration passed into easeCamera").
        if (durationMs > 0) {
            map?.easeCamera(update, durationMs)
        } else {
            map?.moveCamera(update)
        }
    }

    /** הזזה יחסית לפי חלק ממסך המסך הנוכחי - בלי לגעת בזום. */
    fun pan(dLatFraction: Double, dLonFraction: Double) {
        val current = map?.cameraPosition?.target ?: return
        val zoom = map?.cameraPosition?.zoom ?: currentZoom
        // ככל שמתקרבים (זום גבוה) הצעד במעלות קטן יותר, כדי שההזזה בפיקסלים תרגיש עקבית.
        val degreesPerStep = PAN_STEP_DEGREES_AT_ZOOM0 / Math.pow(2.0, zoom)
        val newTarget = MapLibreLatLng(
            current.latitude + dLatFraction * degreesPerStep,
            current.longitude + dLonFraction * degreesPerStep
        )
        map?.moveCamera(CameraUpdateFactory.newLatLng(newTarget))
    }

    fun zoomIn() {
        currentZoom = (currentZoom + 1).coerceAtMost(MAX_ZOOM)
        map?.easeCamera(CameraUpdateFactory.zoomTo(currentZoom), 200)
    }

    fun zoomOut() {
        currentZoom = (currentZoom - 1).coerceAtLeast(MIN_ZOOM)
        map?.easeCamera(CameraUpdateFactory.zoomTo(currentZoom), 200)
    }

    fun recenter(on: AppLatLng, zoom: Double = FOLLOW_ZOOM) {
        animateTo(on, zoom, durationMs = 600)
    }

    companion object {
        private const val DEFAULT_ZOOM = 15.0
        private const val FOLLOW_ZOOM = 17.0
        private const val MIN_ZOOM = 3.0
        private const val MAX_ZOOM = 19.0
        private const val PAN_STEP_DEGREES_AT_ZOOM0 = 40.0
    }
}
