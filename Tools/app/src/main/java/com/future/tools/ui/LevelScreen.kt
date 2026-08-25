package com.future.tools.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import kotlin.math.abs
import kotlin.math.roundToInt

private const val LEVEL_THRESHOLD_DEG = 2f

/** מחזיר את זווית ההטיה של המכשיר (ציר X ו-Y, ב-90 = שכיבה שטוחה לגמרי) מחיישן התאוצה, מוחלק בפילטר low-pass. */
@Composable
private fun rememberTilt(): Offset {
    val context = LocalContext.current
    var tilt by remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var smoothedX = 0f
        var smoothedY = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val pitch = Math.toDegrees(kotlin.math.atan2(-x.toDouble(), kotlin.math.sqrt((y * y + z * z).toDouble()))).toFloat()
                val roll = Math.toDegrees(kotlin.math.atan2(y.toDouble(), z.toDouble())).toFloat()
                smoothedX += (pitch - smoothedX) * 0.15f
                smoothedY += (roll - smoothedY) * 0.15f
                tilt = Offset(smoothedX, smoothedY)
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return tilt
}

@Composable
fun LevelScreen(theme: FutureTheme, onBack: () -> Unit) {
    val tilt = rememberTilt()
    val isLevel = abs(tilt.x) < LEVEL_THRESHOLD_DEG && abs(tilt.y) < LEVEL_THRESHOLD_DEG
    val bubbleColor by animateColorAsState(
        if (isLevel) androidx.compose.ui.graphics.Color(0xFF32D74B) else theme.accentColor,
        label = "levelBubbleColor"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "פלס", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LevelBubble(tilt = tilt, bubbleColor = bubbleColor, theme = theme)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "%.1f° / %.1f°".format(tilt.x, tilt.y),
                            color = theme.textColor,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Light
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            if (isLevel) "מאוזן" else "הטה עד לאיזון",
                            color = if (isLevel) bubbleColor else theme.textColor.copy(alpha = 0.5f),
                            fontSize = 14.sp,
                            fontWeight = if (isLevel) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelBubble(tilt: Offset, bubbleColor: androidx.compose.ui.graphics.Color, theme: FutureTheme) {
    val boxSize = 220.dp
    Box(modifier = Modifier.size(boxSize), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(color = theme.textColor.copy(alpha = 0.06f), radius = radius)
            drawCircle(color = theme.textColor.copy(alpha = 0.25f), radius = radius, style = Stroke(width = 2.dp.toPx()))
            drawCircle(color = theme.textColor.copy(alpha = 0.4f), radius = radius * 0.25f, style = Stroke(width = 1.5.dp.toPx()))
            drawLine(theme.textColor.copy(alpha = 0.15f), Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), strokeWidth = 1.dp.toPx())
            drawLine(theme.textColor.copy(alpha = 0.15f), Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), strokeWidth = 1.dp.toPx())

            // מגביל את מיקום הבועה לגבולות המעגל כדי שלא תיעלם מהמסך בהטיה חדה
            val maxOffsetPx = radius * 0.85f
            val degToPx = radius / 45f
            val offsetX = (tilt.y * degToPx).coerceIn(-maxOffsetPx, maxOffsetPx)
            val offsetY = (-tilt.x * degToPx).coerceIn(-maxOffsetPx, maxOffsetPx)
            val bubbleCenter = Offset(center.x + offsetX, center.y + offsetY)

            drawCircle(color = bubbleColor.copy(alpha = 0.25f), radius = 22.dp.toPx(), center = bubbleCenter)
            drawCircle(color = bubbleColor, radius = 12.dp.toPx(), center = bubbleCenter)
        }
    }
}
