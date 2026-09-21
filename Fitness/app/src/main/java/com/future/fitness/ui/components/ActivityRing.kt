package com.future.fitness.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class RingSpec(val progress: Float, val color: Color)

/** טבעות פעילות מקוננות (כמו במסך "ראשי" של קינטיק אובסידיאן) - כל טבעת
 * היא מדד יומי אחד (קלוריות/דקות פעילות/אימונים), מצוירת ידנית ב-Canvas
 * במקום ספריית גרפים חיצונית שלא נחוצה לשלוש טבעות פשוטות. */
@Composable
fun ActivityRings(rings: List<RingSpec>, trackColor: Color, modifier: Modifier = Modifier, size: Dp = 128.dp, strokeWidth: Dp = 10.dp, content: @Composable () -> Unit = {}) {
    Box(modifier = modifier.size(size), contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val ringGap = strokeWidth.toPx() * 1.6f
            rings.forEachIndexed { index, ring ->
                val inset = index * ringGap
                val arcSize = Size(this.size.width - inset * 2, this.size.height - inset * 2)
                val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
                drawArc(
                    color = ring.color,
                    startAngle = -90f,
                    sweepAngle = 360f * ring.progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke,
                )
            }
        }
        content()
    }
}
