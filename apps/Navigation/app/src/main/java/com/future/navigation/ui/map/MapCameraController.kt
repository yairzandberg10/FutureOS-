package com.future.navigation.ui.map

import com.future.navigation.data.common.LatLng as AppLatLng
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

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
        style = null
    }

    // ---------- קו המסלול והמיקום שלי ----------
    // נשמרים גם לפני שהסגנון נטען, ומצוירים ברגע שהוא מוכן (onStyleLoaded).

    private var style: Style? = null
    private var routePoints: List<AppLatLng>? = null
    private var position: AppLatLng? = null
    private var accentColor: Int = DEFAULT_ACCENT

    fun onStyleLoaded(style: Style) {
        this.style = style
        if (style.getSource(ROUTE_SOURCE) == null) {
            style.addSource(GeoJsonSource(ROUTE_SOURCE))
            style.addLayer(
                LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineColor(accentColor),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
        }
        if (style.getSource(POSITION_SOURCE) == null) {
            style.addSource(GeoJsonSource(POSITION_SOURCE))
            style.addLayer(
                CircleLayer(POSITION_LAYER, POSITION_SOURCE).withProperties(
                    PropertyFactory.circleColor(accentColor),
                    PropertyFactory.circleRadius(9f),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                    PropertyFactory.circleStrokeWidth(3f),
                )
            )
        }
        drawRoute()
        drawPosition()
    }

    /** צבע ההדגשה של ערכת הנושא, לקו המסלול ולנקודת המיקום. */
    fun setAccent(color: Int) {
        if (color == accentColor) return
        accentColor = color
        style?.getLayer(ROUTE_LAYER)?.setProperties(PropertyFactory.lineColor(color))
        style?.getLayer(POSITION_LAYER)?.setProperties(PropertyFactory.circleColor(color))
    }

    fun setRoute(points: List<AppLatLng>) {
        routePoints = points
        drawRoute()
    }

    fun setPosition(at: AppLatLng?) {
        position = at
        drawPosition()
    }

    private fun drawRoute() {
        val points = routePoints?.takeIf { it.size >= 2 } ?: return
        val source = style?.getSourceAs<GeoJsonSource>(ROUTE_SOURCE) ?: return
        source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) })))
    }

    private fun drawPosition() {
        val at = position ?: return
        val source = style?.getSourceAs<GeoJsonSource>(POSITION_SOURCE) ?: return
        source.setGeoJson(Feature.fromGeometry(Point.fromLngLat(at.lon, at.lat)))
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

    /** מעקב אחרי מיקום תוך שמירה על הזום הנוכחי (זום שהמשתמש שינה במקשים לא נדרס). */
    fun follow(on: AppLatLng) {
        animateTo(on, currentZoom, durationMs = 600)
    }

    companion object {
        private const val ROUTE_SOURCE = "future-route"
        private const val ROUTE_LAYER = "future-route-line"
        private const val POSITION_SOURCE = "future-position"
        private const val POSITION_LAYER = "future-position-dot"
        private const val DEFAULT_ACCENT = 0xFF31C09A.toInt()
        private const val DEFAULT_ZOOM = 15.0
        private const val FOLLOW_ZOOM = 17.0
        private const val MIN_ZOOM = 3.0
        private const val MAX_ZOOM = 19.0
        private const val PAN_STEP_DEGREES_AT_ZOOM0 = 40.0
    }
}
