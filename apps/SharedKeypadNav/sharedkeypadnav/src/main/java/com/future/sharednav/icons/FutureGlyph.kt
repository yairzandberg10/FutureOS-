package com.future.sharednav.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

// מידות ה-FosIcon: רשת 24, קו 1.6, נקודות ברדיוס 1.2 (FosIcon.jsx).
private const val GRID = 24f
private const val STROKE = 1.6f
private const val DOT_RADIUS = 1.2f

/**
 * בונה גליף אחד מהמפרט של FosIcon: כל חלק הוא נתיב SVG שמצויר בקו, או
 * "d:x y" - נקודה מלאה. הצבע השחור כאן הוא רק placeholder: Icon() צובע את
 * כל הווקטור ב-tint, כמו כל אייקון Material.
 */
internal fun futureGlyph(
    name: String,
    parts: List<String>,
    autoMirror: Boolean = false,
    solid: Boolean = false,
): ImageVector {
    val builder = ImageVector.Builder(
        name = "FutureIcons.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = GRID,
        viewportHeight = GRID,
        autoMirror = autoMirror,
    )
    val ink = SolidColor(Color.Black)
    for (part in parts) {
        if (part.startsWith("d:")) {
            val (cx, cy) = part.removePrefix("d:").trim().split(" ").map { it.toFloat() }
            val r = DOT_RADIUS
            builder.addPath(
                pathData = addPathNodes("M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0Z"),
                fill = ink,
            )
        } else {
            val closed = part.trim().endsWith("Z")
            builder.addPath(
                pathData = addPathNodes(part),
                fill = if (solid && closed) ink else null,
                stroke = ink,
                strokeLineWidth = STROKE,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }
    return builder.build()
}
