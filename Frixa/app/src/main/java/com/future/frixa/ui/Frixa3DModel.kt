package com.future.frixa.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
}

private fun cross(a: Vec3, b: Vec3) = Vec3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)
private fun dot(a: Vec3, b: Vec3) = a.x * b.x + a.y * b.y + a.z * b.z
private fun normalize(v: Vec3): Vec3 {
    val len = sqrt(dot(v, v)).coerceAtLeast(0.0001f)
    return Vec3(v.x / len, v.y / len, v.z / len)
}

/** קודקודי אוקטהדרון יחידה - "מודל תלת מימד" מינימלי לענף המותג של פריקסה,
 * בלי תלות בספריית רינדור 3D (Sceneform/Filament) שכבדה מדי לחומרה של
 * מכשיר פיצ'ר-פון. הטלה פרספקטיבית + מיון עומק (painter's algorithm) +
 * הצללה שטוחה (dot product מול כיוון אור קבוע) - כל זה מצויר ידנית על
 * Canvas רגיל. */
private val OCTAHEDRON_VERTICES = listOf(
    Vec3(1f, 0f, 0f), Vec3(-1f, 0f, 0f),
    Vec3(0f, 1f, 0f), Vec3(0f, -1f, 0f),
    Vec3(0f, 0f, 1f), Vec3(0f, 0f, -1f),
)

private data class Face(val a: Vec3, val b: Vec3, val c: Vec3, val normal: Vec3)

private val OCTAHEDRON_FACES: List<Face> = run {
    val (px, nx, py, ny, pz, nz) = OCTAHEDRON_VERTICES
    listOf(
        Triple(px, py, pz), Triple(py, nx, pz), Triple(nx, ny, pz), Triple(ny, px, pz),
        Triple(py, px, nz), Triple(nx, py, nz), Triple(ny, nx, nz), Triple(px, ny, nz),
    ).map { (a, b, c) ->
        val centroid = Vec3((a.x + b.x + c.x) / 3f, (a.y + b.y + c.y) / 3f, (a.z + b.z + c.z) / 3f)
        var normal = normalize(cross(b - a, c - a))
        // מוודאים שהנורמל פונה החוצה (אותו כיוון כמו הצנטרואיד) - בלי זה
        // הצללה/backface-culling היו תלויים בסדר הקודקודים השרירותי למעלה.
        if (dot(normal, centroid) < 0f) normal = Vec3(-normal.x, -normal.y, -normal.z)
        Face(a, b, c, normal)
    }
}

private operator fun <T> List<T>.component6() = this[5]

private fun rotate(v: Vec3, cosY: Float, sinY: Float, cosX: Float, sinX: Float): Vec3 {
    val x1 = v.x * cosY + v.z * sinY
    val z1 = -v.x * sinY + v.z * cosY
    val y1 = v.y
    val y2 = y1 * cosX - z1 * sinX
    val z2 = y1 * sinX + z1 * cosX
    return Vec3(x1, y2, z2)
}

private val LIGHT_DIR = normalize(Vec3(0.4f, 0.6f, -1f))

/** מודל תלת מימד מסתובב לרוחב - אלמנט המותג של פריקסה במסך הראשי, בצבע
 * ה-accent המשותף (theme) כדי להישאר נאמן לדיזיין סיסטם, לא לפלטה מומצאת. */
@Composable
fun Frixa3DModel(accentColor: Color, modifier: Modifier = Modifier) {
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        var lastNanos = 0L
        while (true) {
            val nanos = withFrameNanos { it }
            if (lastNanos != 0L) {
                val deltaSeconds = (nanos - lastNanos) / 1_000_000_000f
                angle += deltaSeconds * 0.9f
            }
            lastNanos = nanos
        }
    }

    Canvas(modifier = modifier.size(160.dp)) {
        val cosY = cos(angle)
        val sinY = sin(angle)
        val tilt = 0.5f
        val cosX = cos(tilt)
        val sinX = sin(tilt)

        val focal = 3f
        val radius = size.minDimension / 2.2f
        val center = Offset(size.width / 2f, size.height / 2f)

        fun project(v: Vec3): Offset {
            val scale = focal / (focal + v.z)
            return Offset(center.x + v.x * scale * radius, center.y - v.y * scale * radius)
        }

        val rotatedFaces = OCTAHEDRON_FACES.map { face ->
            val ra = rotate(face.a, cosY, sinY, cosX, sinX)
            val rb = rotate(face.b, cosY, sinY, cosX, sinX)
            val rc = rotate(face.c, cosY, sinY, cosX, sinX)
            val rn = rotate(face.normal, cosY, sinY, cosX, sinX)
            Triple(Triple(ra, rb, rc), rn, (ra.z + rb.z + rc.z) / 3f)
        }
            .filter { (_, normal, _) -> normal.z < 0f }
            .sortedByDescending { (_, _, avgZ) -> avgZ }

        rotatedFaces.forEach { (verts, normal, _) ->
            val (ra, rb, rc) = verts
            val brightness = (dot(normal, LIGHT_DIR) * -1f).coerceIn(0.25f, 1f)
            val color = lerp(Color.Black, accentColor, brightness)
            val path = Path().apply {
                val p0 = project(ra)
                val p1 = project(rb)
                val p2 = project(rc)
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                close()
            }
            drawPath(path, color = color)
        }
    }
}
