package com.future.navigation.data.common

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lon: Double)

/** מרחק הייברסיין במטרים - משמש לחיפוש תחנות קרובות ולזיהוי סטייה מהמסלול. */
fun LatLng.distanceMetersTo(other: LatLng): Double {
    val earthRadius = 6_371_000.0
    val dLat = Math.toRadians(other.lat - lat)
    val dLon = Math.toRadians(other.lon - lon)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return earthRadius * c
}

/**
 * המרחק (במטרים) מנקודה לקטע הישר הקרוב ביותר בפוליליין - משמש לזיהוי סטייה
 * מהמסלול במסך הניווט החי. קירוב פשוט במרחב מעלות (מספיק על מרחקים קצרים
 * של עיר/כביש; לא פרויקציה גיאודזית מלאה).
 */
fun nearestDistanceMetersToPolyline(point: LatLng, polyline: List<LatLng>): Double {
    if (polyline.isEmpty()) return Double.MAX_VALUE
    if (polyline.size == 1) return point.distanceMetersTo(polyline[0])

    var best = Double.MAX_VALUE
    for (i in 0 until polyline.size - 1) {
        val a = polyline[i]
        val b = polyline[i + 1]
        val d = distanceMetersToSegment(point, a, b)
        if (d < best) best = d
    }
    return best
}

private fun distanceMetersToSegment(p: LatLng, a: LatLng, b: LatLng): Double {
    // עובד במרחב מעלות (עם תיקון קירוב לקו רוחב) ואז ממיר בחזרה למטרים -
    // מדויק מספיק למרחקי סטייה של עשרות/מאות מטרים.
    val latScale = 111_320.0
    val lonScale = 111_320.0 * cos(Math.toRadians(a.lat))

    val px = (p.lon - a.lon) * lonScale
    val py = (p.lat - a.lat) * latScale
    val bx = (b.lon - a.lon) * lonScale
    val by = (b.lat - a.lat) * latScale

    val lenSq = bx * bx + by * by
    val t = if (lenSq == 0.0) 0.0 else ((px * bx + py * by) / lenSq).coerceIn(0.0, 1.0)

    val projX = t * bx
    val projY = t * by

    val dx = px - projX
    val dy = py - projY
    return sqrt(dx * dx + dy * dy)
}
