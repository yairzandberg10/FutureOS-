package com.future.tools.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class RulerMode(val label: String) { RULER("סרגל"), ANGLE("זווית") }

@Composable
private fun rememberTiltAngle(): Float {
    val context = LocalContext.current
    var angle by remember { mutableFloatStateOf(0f) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smoothed = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val y = event.values[1]
                val z = event.values[2]
                val roll = Math.toDegrees(kotlin.math.atan2(y.toDouble(), z.toDouble())).toFloat()
                smoothed += (roll - smoothed) * 0.15f
                angle = smoothed
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return angle
}

@Composable
fun AngleRulerScreen(theme: FutureTheme, onBack: () -> Unit) {
    var mode by remember { mutableStateOf(RulerMode.RULER) }
    val tiltAngle = rememberTiltAngle()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "סרגל וזווית", theme = theme, onBack = onBack)

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RulerMode.entries.forEach { m ->
                        ModeChip(m.label, isSelected = m == mode, theme = theme, modifier = Modifier.weight(1f)) { mode = m }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    when (mode) {
                        RulerMode.RULER -> RulerView(theme = theme)
                        RulerMode.ANGLE -> AngleView(angle = tiltAngle, theme = theme)
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeChip(label: String, isSelected: Boolean, theme: FutureTheme, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val bgColor = when {
        isSelected -> theme.accentColor
        isFocused -> theme.textColor.copy(alpha = 0.18f)
        else -> theme.textColor.copy(alpha = 0.06f)
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (isSelected) Color.Black else theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

/** סרגל מדויק על גבי המסך - מכויל לפי צפיפות המסך האמיתית (xdpi/ydpi) כך
 * שסימוני הס"מ/האינץ' תואמים למידה פיזית אמיתית כשמניחים חפץ צמוד למסך. */
@Composable
private fun RulerView(theme: FutureTheme) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val metrics = context.resources.displayMetrics
    val pxPerCm = metrics.ydpi / 2.54f
    val heightDp = with(density) { (metrics.heightPixels * 0.75f).toDp() }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.width(140.dp).height(heightDp)) {
            val totalCm = (this.size.height / pxPerCm).toInt()
            for (cm in 0..totalCm) {
                val y = cm * pxPerCm
                val isMajor = cm % 5 == 0
                val lineLength = if (isMajor) 60.dp.toPx() else 34.dp.toPx()
                drawLine(
                    color = theme.textColor.copy(alpha = if (isMajor) 0.8f else 0.4f),
                    start = Offset(0f, y),
                    end = Offset(lineLength, y),
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()
                )
                if (isMajor) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = theme.textColor.copy(alpha = 0.7f).toArgb()
                            textSize = 12.sp.toPx()
                        }
                        drawText("$cm", lineLength + 8.dp.toPx(), y + paint.textSize / 3, paint)
                    }
                }
                for (mm in 1..4) {
                    val my = y + mm * (pxPerCm / 5f)
                    if (my > this.size.height) continue
                    drawLine(
                        color = theme.textColor.copy(alpha = 0.2f),
                        start = Offset(0f, my),
                        end = Offset(18.dp.toPx(), my),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "הצמד חפץ לקצה המסך ומדוד לפי הסימונים (ס\"מ)",
            color = theme.textColor.copy(alpha = 0.4f),
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
private fun AngleView(angle: Float, theme: FutureTheme) {
    val displayAngle = abs(angle)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = this.size.minDimension / 2f
                drawArc(
                    color = theme.textColor.copy(alpha = 0.1f),
                    startAngle = 180f, sweepAngle = 180f, useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    style = Stroke(width = 3.dp.toPx())
                )
                for (tick in 0..180 step 10) {
                    val rad = Math.toRadians((180 - tick).toDouble())
                    val outer = radius
                    val inner = if (tick % 90 == 0) radius * 0.82f else radius * 0.9f
                    val p1 = Offset(center.x + (outer * kotlin.math.cos(rad)).toFloat(), center.y - (outer * kotlin.math.sin(rad)).toFloat())
                    val p2 = Offset(center.x + (inner * kotlin.math.cos(rad)).toFloat(), center.y - (inner * kotlin.math.sin(rad)).toFloat())
                    drawLine(theme.textColor.copy(alpha = 0.35f), p1, p2, strokeWidth = 1.5.dp.toPx())
                }
                val needleAngleDeg = 180 - (angle + 90f).coerceIn(0f, 180f)
                val rad = Math.toRadians(needleAngleDeg.toDouble())
                val needleLength = radius * 0.75f
                drawLine(
                    color = theme.accentColor,
                    start = center,
                    end = Offset(center.x + (needleLength * kotlin.math.cos(rad)).toFloat(), center.y - (needleLength * kotlin.math.sin(rad)).toFloat()),
                    strokeWidth = 3.dp.toPx(),
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawCircle(color = theme.accentColor, radius = 5.dp.toPx(), center = center)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text("${displayAngle.roundToInt()}°", color = theme.textColor, fontSize = 40.sp, fontWeight = FontWeight.Light)
        Spacer(modifier = Modifier.height(4.dp))
        Text("הטה את המכשיר לאורך המשטח הנמדד", color = theme.textColor.copy(alpha = 0.4f), fontSize = 12.sp)
    }
}
