package com.future.navigation.ui.map

import com.future.navigation.data.common.LatLng as AppLatLng
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng as MapLibreLatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
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
    /** במסך הניווט: חץ שמסתובב לכיוון הנסיעה במקום נקודה, והחלק שכבר נסענו מעומעם. */
    private var navigationMode = false
    private var heading: Float = 0f
    /** האינדקס בקו המסלול שהכי קרוב למיקום - מתקדם רק קדימה, כדי שלולאות לא יבלבלו. */
    private var progressIndex = 0

    fun onStyleLoaded(style: Style) {
        this.style = style
        if (style.getSource(ROUTE_SOURCE) == null) {
            style.addSource(GeoJsonSource(TRAVELED_SOURCE))
            style.addSource(GeoJsonSource(ROUTE_SOURCE))
            style.addSource(GeoJsonSource(DESTINATION_SOURCE))
            // מה שכבר נסענו: אפור, מתחת לכל השאר
            style.addLayer(
                LineLayer(TRAVELED_LAYER, TRAVELED_SOURCE).withProperties(
                    PropertyFactory.lineColor(TRAVELED_COLOR),
                    PropertyFactory.lineWidth(7f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
            // מסגרת כהה מתחת לקו (כמו Waze) - הקו בולט גם על כביש בהיר וגם במפה כהה
            style.addLayer(
                LineLayer(ROUTE_CASING_LAYER, ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineColor(darken(accentColor)),
                    PropertyFactory.lineWidth(13f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
            style.addLayer(
                LineLayer(ROUTE_LAYER, ROUTE_SOURCE).withProperties(
                    PropertyFactory.lineColor(accentColor),
                    PropertyFactory.lineWidth(9f),
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                )
            )
            // היעד: עיגול גדול עם מסגרת לבנה ונקודה לבנה במרכז
            style.addLayer(
                CircleLayer(DESTINATION_LAYER, DESTINATION_SOURCE).withProperties(
                    PropertyFactory.circleColor(darken(accentColor)),
                    PropertyFactory.circleRadius(11f),
                    PropertyFactory.circleStrokeColor(android.graphics.Color.WHITE),
                    PropertyFactory.circleStrokeWidth(4f),
                )
            )
            style.addLayer(
                CircleLayer(DESTINATION_DOT_LAYER, DESTINATION_SOURCE).withProperties(
                    PropertyFactory.circleColor(android.graphics.Color.WHITE),
                    PropertyFactory.circleRadius(4f),
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
            style.addImage(ARROW_IMAGE, arrowBitmap(accentColor))
            style.addLayer(
                SymbolLayer(ARROW_LAYER, POSITION_SOURCE).withProperties(
                    PropertyFactory.iconImage(ARROW_IMAGE),
                    PropertyFactory.iconRotate(heading),
                    PropertyFactory.iconRotationAlignment(Property.ICON_ROTATION_ALIGNMENT_MAP),
                    PropertyFactory.iconPitchAlignment(Property.ICON_PITCH_ALIGNMENT_MAP),
                    PropertyFactory.iconAllowOverlap(true),
                    PropertyFactory.iconIgnorePlacement(true),
                )
            )
        }
        applyPuckVisibility()
        drawRoute()
        drawPosition()
    }

    /** צבע ההדגשה של ערכת הנושא, לקו המסלול ולנקודת המיקום. */
    fun setAccent(color: Int) {
        if (color == accentColor) return
        accentColor = color
        val s = style ?: return
        s.getLayer(ROUTE_LAYER)?.setProperties(PropertyFactory.lineColor(color))
        s.getLayer(ROUTE_CASING_LAYER)?.setProperties(PropertyFactory.lineColor(darken(color)))
        s.getLayer(DESTINATION_LAYER)?.setProperties(PropertyFactory.circleColor(darken(color)))
        s.getLayer(POSITION_LAYER)?.setProperties(PropertyFactory.circleColor(color))
        s.addImage(ARROW_IMAGE, arrowBitmap(color))
    }

    /** מסך הניווט: חץ כיוון במקום נקודה, עמעום החלק שנסענו, וזום ניווט. */
    fun setNavigationMode(on: Boolean) {
        if (on && !navigationMode) currentZoom = NAV_ZOOM
        navigationMode = on
        applyPuckVisibility()
        drawRoute()
    }

    /** זום הניווט מחדש (לחיצה על מרכז אחרי שהזום שונה במקשים). */
    fun resetNavZoom() {
        currentZoom = NAV_ZOOM
    }

    fun setRoute(points: List<AppLatLng>) {
        if (points != routePoints) progressIndex = 0
        routePoints = points
        drawRoute()
    }

    fun setPosition(at: AppLatLng?, bearing: Float? = null) {
        position = at
        if (bearing != null) heading = bearing
        drawPosition()
        if (navigationMode) drawRoute()
    }

    private fun applyPuckVisibility() {
        val s = style ?: return
        val nav = if (navigationMode) Property.VISIBLE else Property.NONE
        val dot = if (navigationMode) Property.NONE else Property.VISIBLE
        s.getLayer(ARROW_LAYER)?.setProperties(PropertyFactory.visibility(nav))
        s.getLayer(TRAVELED_LAYER)?.setProperties(PropertyFactory.visibility(nav))
        s.getLayer(POSITION_LAYER)?.setProperties(PropertyFactory.visibility(dot))
    }

    private fun drawRoute() {
        val points = routePoints?.takeIf { it.size >= 2 } ?: return
        val s = style ?: return
        val at = position
        val split = if (navigationMode && at != null) advanceProgress(points, at) else 0
        // החלק שנשאר: מהמיקום שלי ועד היעד. החלק שנסענו: מההתחלה ועד המיקום שלי.
        val remaining = ArrayList<AppLatLng>()
        if (split > 0 && at != null) remaining.add(at)
        remaining.addAll(points.subList(split, points.size))
        val traveled = ArrayList<AppLatLng>()
        if (split > 0 && at != null) {
            traveled.addAll(points.subList(0, split))
            traveled.add(at)
        }
        s.getSourceAs<GeoJsonSource>(ROUTE_SOURCE)?.setGeoJson(line(remaining))
        val traveledSource = s.getSourceAs<GeoJsonSource>(TRAVELED_SOURCE)
        if (traveled.size >= 2) traveledSource?.setGeoJson(line(traveled))
        else traveledSource?.setGeoJson(org.maplibre.geojson.FeatureCollection.fromFeatures(emptyArray<Feature>()))
        val end = points.last()
        s.getSourceAs<GeoJsonSource>(DESTINATION_SOURCE)?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(end.lon, end.lat)))
    }

    /** האינדקס הראשון בקו שעוד לא עברנו: הנקודה הקרובה למיקום, בחלון קדימה מהמיקום הקודם. */
    private fun advanceProgress(points: List<AppLatLng>, at: AppLatLng): Int {
        val from = progressIndex.coerceIn(0, points.size - 1)
        val to = minOf(points.size - 1, from + PROGRESS_WINDOW)
        var best = from
        var bestD = Double.MAX_VALUE
        for (i in from..to) {
            val d = squaredDistance(points[i], at)
            if (d < bestD) { bestD = d; best = i }
        }
        // רחוקים מהקו (סטייה מהמסלול) - לא מקדמים, כדי לא "לאכול" את המסלול
        if (bestD > MAX_SNAP_SQ_DEG) return progressIndex
        progressIndex = best
        return best
    }

    private fun squaredDistance(a: AppLatLng, b: AppLatLng): Double {
        val dLat = a.lat - b.lat
        val dLon = (a.lon - b.lon) * kotlin.math.cos(Math.toRadians(a.lat))
        return dLat * dLat + dLon * dLon
    }

    private fun line(points: List<AppLatLng>): Feature =
        Feature.fromGeometry(LineString.fromLngLats(points.map { Point.fromLngLat(it.lon, it.lat) }))

    private fun drawPosition() {
        val at = position ?: return
        val s = style ?: return
        s.getSourceAs<GeoJsonSource>(POSITION_SOURCE)?.setGeoJson(Feature.fromGeometry(Point.fromLngLat(at.lon, at.lat)))
        s.getLayer(ARROW_LAYER)?.setProperties(PropertyFactory.iconRotate(heading))
    }

    /** חץ ניווט (כמו ב-Waze): ראש חץ בצבע ההדגשה עם מסגרת לבנה וצל רך. */
    private fun arrowBitmap(color: Int): android.graphics.Bitmap {
        val size = ARROW_PX
        val bmp = android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(bmp)
        val w = size.toFloat()
        val path = android.graphics.Path().apply {
            moveTo(w * 0.5f, w * 0.12f)
            lineTo(w * 0.82f, w * 0.84f)
            lineTo(w * 0.5f, w * 0.67f)
            lineTo(w * 0.18f, w * 0.84f)
            close()
        }
        val shadow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = 0x55000000
            maskFilter = android.graphics.BlurMaskFilter(w * 0.06f, android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        c.save()
        c.translate(0f, w * 0.03f)
        c.drawPath(path, shadow)
        c.restore()
        val stroke = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            style = android.graphics.Paint.Style.STROKE
            strokeWidth = w * 0.09f
            strokeJoin = android.graphics.Paint.Join.ROUND
        }
        c.drawPath(path, stroke)
        val fill = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = android.graphics.Paint.Style.FILL
        }
        c.drawPath(path, fill)
        return bmp
    }

    private fun darken(color: Int): Int {
        val f = 0.55f
        return android.graphics.Color.rgb(
            (android.graphics.Color.red(color) * f).toInt(),
            (android.graphics.Color.green(color) * f).toInt(),
            (android.graphics.Color.blue(color) * f).toInt(),
        )
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

    /**
     * מעקב במצב "הכיוון שלי למעלה" (כמו Waze): המפה מסתובבת לכיוון הנסיעה ומוטה
     * מעט. bearing=null (עומדים/אין כיוון) - נשמר הכיוון הקודם של המפה.
     */
    fun followHeading(on: AppLatLng, bearing: Float?) {
        val m = map ?: return
        val position = CameraPosition.Builder()
            .target(MapLibreLatLng(on.lat, on.lon))
            .zoom(currentZoom)
            .bearing(bearing?.toDouble() ?: m.cameraPosition.bearing)
            .tilt(HEADING_TILT)
            // המיקום שלי בשליש התחתון של המסך, כדי לראות יותר מהדרך שלפנינו
            .padding(0.0, FOLLOW_TOP_PADDING_PX, 0.0, 0.0)
            .build()
        m.easeCamera(CameraUpdateFactory.newCameraPosition(position), 900)
    }

    /** תצוגת מסלול מלא: המצלמה מתאימה את גבולות המסלול כולו (צפון למעלה, בלי הטיה). */
    fun fitRoute(points: List<AppLatLng>) {
        val m = map ?: return
        if (points.size < 2) return
        val reset = CameraPosition.Builder(m.cameraPosition).bearing(0.0).tilt(0.0).padding(0.0, 0.0, 0.0, 0.0).build()
        m.moveCamera(CameraUpdateFactory.newCameraPosition(reset))
        val bounds = LatLngBounds.Builder()
        points.forEach { bounds.include(MapLibreLatLng(it.lat, it.lon)) }
        // שוליים גדולים למעלה ולמטה - שם כרטיס הפנייה והשורה התחתונה
        m.easeCamera(
            CameraUpdateFactory.newLatLngBounds(bounds.build(), OVERVIEW_PADDING_PX, OVERVIEW_TOP_PADDING_PX, OVERVIEW_PADDING_PX, OVERVIEW_BOTTOM_PADDING_PX),
            600
        )
    }

    companion object {
        private const val ROUTE_SOURCE = "future-route"
        private const val ROUTE_LAYER = "future-route-line"
        private const val POSITION_SOURCE = "future-position"
        private const val POSITION_LAYER = "future-position-dot"
        private const val ARROW_LAYER = "future-position-arrow"
        private const val ARROW_IMAGE = "future-nav-arrow"
        private const val ARROW_PX = 72
        private const val ROUTE_CASING_LAYER = "future-route-casing"
        private const val TRAVELED_SOURCE = "future-route-traveled"
        private const val TRAVELED_LAYER = "future-route-traveled-line"
        private const val TRAVELED_COLOR = 0xFF9AA0A6.toInt()
        private const val DESTINATION_SOURCE = "future-destination"
        private const val DESTINATION_LAYER = "future-destination-pin"
        private const val DESTINATION_DOT_LAYER = "future-destination-dot"
        private const val NAV_ZOOM = 17.0
        /** חלון החיפוש קדימה בקו המסלול (נקודות), ומרחק מקסימלי לקו (~60 מ' במעלות, בריבוע). */
        private const val PROGRESS_WINDOW = 400
        private const val MAX_SNAP_SQ_DEG = 0.00055 * 0.00055
        /** ריפוד עליון במעקב: המיקום יורד לשליש התחתון של מסך בגובה 960 פיקסלים. */
        private const val FOLLOW_TOP_PADDING_PX = 420.0
        private const val OVERVIEW_TOP_PADDING_PX = 230
        private const val OVERVIEW_BOTTOM_PADDING_PX = 170
        private const val DEFAULT_ACCENT = 0xFF31C09A.toInt()
        private const val DEFAULT_ZOOM = 15.0
        private const val FOLLOW_ZOOM = 17.0
        private const val MIN_ZOOM = 3.0
        private const val MAX_ZOOM = 19.0
        private const val PAN_STEP_DEGREES_AT_ZOOM0 = 40.0
        /** הטיית המפה במצב כיוון הנסיעה (מעט, בלי לפגוע בקריאות). */
        private const val HEADING_TILT = 55.0
        /** שוליים (פיקסלים) סביב המסלול בתצוגת מסלול מלא, כדי שההתחלה והסוף לא ייחתכו. */
        private const val OVERVIEW_PADDING_PX = 40
    }
}
