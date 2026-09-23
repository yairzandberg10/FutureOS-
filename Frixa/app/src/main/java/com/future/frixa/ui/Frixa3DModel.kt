package com.future.frixa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class V3(val x: Float, val y: Float, val z: Float)

private fun V3.rotate(cy: Float, sy: Float, cx: Float, sx: Float): V3 {
    val x1 = x * cy + z * sy
    val z1 = -x * sy + z * cy
    val y2 = y * cx - z1 * sx
    val z2 = y * sx + z1 * cx
    return V3(x1, y2, z2)
}

private fun V3.normalized(): V3 {
    val l = sqrt(x * x + y * y + z * z).takeIf { it > 0f } ?: 1f
    return V3(x / l, y / l, z / l)
}

/** פאה אחת במודל: ארבע פינות, הנורמל שלה והצבע שלה. */
private class Face(val corners: List<V3>, val normal: V3, val color: Color)

// צורת הלחמניה: אליפסואיד מוארך, חצוי לרוחבו - חצי עליון וחצי תחתון עם רווח
// ביניהם, ובתוכו המילוי (חריסה, ביצה). צבעי מזון אמיתיים - זה תוכן, לא ממשק.
private const val RX = 1.35f
private const val RY = 0.5f
private const val RZ = 0.72f
private const val GAP = 0.13f
private const val LAT = 7
private const val LON = 16
private val CRUST = Color(0xFFC98A3D)
private val CRUMB = Color(0xFFF1D7A6)
private val HARISSA = Color(0xFFC0392B)
private val EGG = Color(0xFFF4D03F)
private val LIGHT = V3(0.35f, 0.75f, -0.6f).normalized()

private fun buildModel(): List<Face> {
    val faces = mutableListOf<Face>()
    fun point(lat: Float, lon: Float, sign: Float): V3 {
        // lat 0 בקו החיתוך, PI/2 בקודקוד. sign: חצי עליון (1) או תחתון (-1).
        val r = cos(lat)
        return V3(RX * r * cos(lon), sign * (RY * sin(lat) + GAP), RZ * r * sin(lon))
    }
    for (sign in listOf(1f, -1f)) {
        for (i in 0 until LAT) {
            val a0 = (PI / 2 * i / LAT).toFloat()
            val a1 = (PI / 2 * (i + 1) / LAT).toFloat()
            for (j in 0 until LON) {
                val b0 = (2 * PI * j / LON).toFloat()
                val b1 = (2 * PI * (j + 1) / LON).toFloat()
                val corners = listOf(point(a0, b0, sign), point(a0, b1, sign), point(a1, b1, sign), point(a1, b0, sign))
                val mid = (a0 + a1) / 2
                val midLon = (b0 + b1) / 2
                val normal = V3(cos(mid) * cos(midLon) / RX, sign * sin(mid) / RY, cos(mid) * sin(midLon) / RZ).normalized()
                faces += Face(corners, normal, CRUST)
            }
        }
        // פני החיתוך - הבצק הבהיר מבפנים.
        val ring = (0 until LON).map { j ->
            val b = (2 * PI * j / LON).toFloat()
            V3(RX * cos(b), sign * GAP, RZ * sin(b))
        }
        for (j in 0 until LON) {
            val next = (j + 1) % LON
            faces += Face(listOf(V3(0f, sign * GAP, 0f), ring[j], ring[next], ring[next]), V3(0f, -sign, 0f), CRUMB)
        }
    }
    // המילוי: שכבת חריסה ומעליה ביצה, מעט קטנות מהחיתוך.
    for ((y, scale, color) in listOf(Triple(-0.03f, 0.9f, HARISSA), Triple(0.05f, 0.75f, EGG))) {
        val ring = (0 until LON).map { j ->
            val b = (2 * PI * j / LON).toFloat()
            V3(RX * scale * cos(b), y, RZ * scale * sin(b))
        }
        for (j in 0 until LON) {
            val next = (j + 1) % LON
            faces += Face(listOf(V3(0f, y, 0f), ring[j], ring[next], ring[next]), V3(0f, 1f, 0f), color)
        }
    }
    return faces
}

/**
 * מודל תלת-ממד של פריקסה - לחמנייה חצויה עם מילוי, מסתובבת לאט. בנוי פעם
 * אחת כרשת פאות, ובכל פריים רק מסובבים, ממיינים לפי עומק ומציירים את הפאות
 * שפונות לצופה - קל מספיק למכשיר.
 */
@Composable
fun Frixa3DModel(accentColor: Color, modifier: Modifier = Modifier, size: Dp = 150.dp) {
    val model = remember { buildModel() }
    var angle by remember { mutableFloatStateOf(0.6f) }
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            val now = withFrameNanos { it }
            if (last != 0L) angle += (now - last) / 1_000_000_000f * 0.7f
            last = now
        }
    }

    Canvas(modifier = modifier.size(size)) {
        val cy = cos(angle)
        val sy = sin(angle)
        val tilt = 0.55f
        val cx = cos(tilt)
        val sx = sin(tilt)
        val focal = 4f
        val radius = this.size.minDimension / 3.1f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        fun project(v: V3): Offset {
            val k = focal / (focal + v.z)
            return Offset(center.x + v.x * k * radius, center.y - v.y * k * radius)
        }

        model
            .map { face ->
                val pts = face.corners.map { it.rotate(cy, sy, cx, sx) }
                Triple(face, pts, pts.sumOf { it.z.toDouble() }.toFloat() / pts.size)
            }
            .filter { (face, _, _) -> face.normal.rotate(cy, sy, cx, sx).z < 0.05f }
            .sortedByDescending { it.third }
            .forEach { (face, pts, _) ->
                val n = face.normal.rotate(cy, sy, cx, sx)
                val light = (-(n.x * LIGHT.x + n.y * LIGHT.y + n.z * LIGHT.z)).coerceIn(0f, 1f)
                val color = lerp(Color.Black, face.color, 0.45f + 0.55f * light)
                val path = Path()
                pts.forEachIndexed { i, p ->
                    val o = project(p)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                path.close()
                drawPath(path, color)
            }
    }
}
