package com.future.tools.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.tools.ui.theme.FutureTheme
import kotlin.math.roundToInt

private data class LuxState(val lux: Float, val hasSensor: Boolean)

@Composable
private fun rememberLux(): LuxState {
    val context = LocalContext.current
    var lux by remember { mutableFloatStateOf(0f) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        hasSensor = lightSensor != null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                lux += (event.values[0] - lux) * 0.3f
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (lightSensor != null) {
            sensorManager.registerListener(listener, lightSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return LuxState(lux, hasSensor)
}

private fun lightingLabel(lux: Float): String = when {
    lux < 1f -> "חושך מוחלט"
    lux < 50f -> "תאורה עמומה"
    lux < 200f -> "תאורת חדר רגילה"
    lux < 1000f -> "תאורה בהירה"
    lux < 10000f -> "אור יום מקורה"
    else -> "אור שמש ישיר"
}

@Composable
fun LuxMeterScreen(theme: FutureTheme, onBack: () -> Unit) {
    val state = rememberLux()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מד אור", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!state.hasSensor) {
                        Text(
                            "לא נמצא חיישן אור במכשיר הזה",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            LuxGauge(lux = state.lux, theme = theme)
                            Spacer(modifier = Modifier.height(20.dp))
                            Text("${state.lux.roundToInt()} lux", color = theme.textColor, fontSize = 40.sp, fontWeight = FontWeight.Light)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(lightingLabel(state.lux), color = theme.accentColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LuxGauge(lux: Float, theme: FutureTheme) {
    val size = 220.dp
    // סקאלה לוגריתמית כי טווח התאורה עצום (0 עד 100,000+ lux)
    val fraction = (kotlin.math.log10((lux + 1).toDouble()) / 5.0).toFloat().coerceIn(0f, 1f)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            val topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
            drawArc(
                color = theme.textColor.copy(alpha = 0.1f),
                startAngle = 135f, sweepAngle = 270f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFFFFD60A),
                startAngle = 135f, sweepAngle = 270f * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
            )
        }
    }
}
