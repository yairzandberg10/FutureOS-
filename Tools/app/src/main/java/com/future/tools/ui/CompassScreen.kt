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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.sharednav.theme.FutureTheme
import kotlin.math.roundToInt

private fun cardinalFor(degrees: Float): String {
    val dirs = listOf("צפון", "צפון-מזרח", "מזרח", "דרום-מזרח", "דרום", "דרום-מערב", "מערב", "צפון-מערב")
    val index = (((degrees + 22.5f) / 45f).toInt()) % 8
    return dirs[if (index < 0) index + 8 else index]
}

private data class CompassState(val azimuth: Float, val hasSensors: Boolean)
private data class AltitudeState(val meters: Float?, val hasSensor: Boolean)

/** גובה משוער מעל פני הים לפי חיישן הלחץ הברומטרי, מוחלק בפילטר low-pass -
 * מבוסס על הנחת לחץ ים סטנדרטי, לא כיול GPS, ולכן זה קירוב ולא מדידה מדויקת. */
@Composable
private fun rememberAltitudeState(): AltitudeState {
    val context = LocalContext.current
    var meters by remember { mutableStateOf<Float?>(null) }
    var hasSensor by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)
        hasSensor = pressureSensor != null
        var smoothed: Float? = null

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val altitude = SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, event.values[0])
                smoothed = if (smoothed == null) altitude else smoothed!! + (altitude - smoothed!!) * 0.1f
                meters = smoothed
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (pressureSensor != null) {
            sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return AltitudeState(meters, hasSensor)
}

/**
 * מחזיר את הכיוון הנוכחי (0-360, 0=צפון) על בסיס חיישני תאוצה ומגנטומטר, מוחלק
 * בפילטר low-pass, וכן האם החיישנים הנדרשים בכלל קיימים במכשיר.
 */
@Composable
private fun rememberCompassState(): CompassState {
    val context = LocalContext.current
    var azimuth by remember { mutableFloatStateOf(0f) }
    var hasSensors by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        hasSensors = accelerometer != null && magnetometer != null

        val gravity = FloatArray(3)
        val geomagnetic = FloatArray(3)
        var hasGravity = false
        var hasGeomagnetic = false
        var smoothed = 0f
        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientation = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        hasGravity = true
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, geomagnetic, 0, 3)
                        hasGeomagnetic = true
                    }
                }
                if (hasGravity && hasGeomagnetic) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravity, geomagnetic)) {
                        // המכשיר הזה מוחזק זקוף כמו שלט/פלאפון-כפתורים, לא שטוח על
                        // השולחן כמו שה-API הגולמי מניח - בלי remapCoordinateSystem
                        // כאן, getOrientation() מחזירה כיוון שגוי/קופצני כשהמכשיר
                        // אנכי (בדיוק "המצפן לא עובד"/קופץ שדווח). AXIS_X/AXIS_Z הוא
                        // הרימאפ הסטנדרטי למצפן שנאחז זקוף עם המסך פונה למשתמש.
                        SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)
                        SensorManager.getOrientation(remappedMatrix, orientation)
                        val degrees = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
                        var delta = degrees - smoothed
                        if (delta > 180f) delta -= 360f
                        if (delta < -180f) delta += 360f
                        smoothed = (smoothed + delta * 0.15f + 360f) % 360f
                        azimuth = smoothed
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
        }

        if (hasSensors) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
            sensorManager.registerListener(listener, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        }

        onDispose { sensorManager.unregisterListener(listener) }
    }

    return CompassState(azimuth, hasSensors)
}

@Composable
fun CompassScreen(theme: FutureTheme, onBack: () -> Unit) {
    val compassState = rememberCompassState()
    val azimuth = compassState.azimuth
    val altitudeState = rememberAltitudeState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מצפן וגובה", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!compassState.hasSensors) {
                        Text(
                            "לא נמצא חיישן מצפן (תאוצה/מגנטומטר) במכשיר הזה",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompassRose(azimuth = azimuth, theme = theme)
                            Spacer(modifier = Modifier.height(24.dp))
                            Text("${azimuth.roundToInt()}°", color = theme.textColor, fontSize = 32.sp, fontWeight = FontWeight.Light)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cardinalFor(azimuth), color = theme.textColor.copy(alpha = 0.6f), fontSize = 15.sp)
                            Spacer(modifier = Modifier.height(20.dp))
                            AltitudeRow(altitudeState = altitudeState, theme = theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AltitudeRow(altitudeState: AltitudeState, theme: FutureTheme) {
    if (!altitudeState.hasSensor) {
        Text(
            "אין חיישן לחץ ברומטרי - לא ניתן למדוד גובה",
            color = theme.textColor.copy(alpha = 0.35f),
            fontSize = 12.sp
        )
        return
    }
    val meters = altitudeState.meters
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (meters != null) "${meters.roundToInt()} מ' מעל פני הים" else "מודד גובה...",
            color = theme.textColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("גובה משוער לפי לחץ אטמוספרי", color = theme.textColor.copy(alpha = 0.35f), fontSize = 11.sp)
    }
}

@Composable
private fun CompassRose(azimuth: Float, theme: FutureTheme) {
    val size = 220.dp
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(color = theme.textColor.copy(alpha = 0.08f), radius = radius)
            drawCircle(color = theme.textColor.copy(alpha = 0.25f), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

            // הכיוונים מסתובבים נגד כיוון האזימוט כך שהתו "N" תמיד מצביע צפון אמיתי
            rotate(degrees = -azimuth) {
                val labels = listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f)
                labels.forEach { (label, angle) ->
                    val rad = Math.toRadians((angle - 90).toDouble())
                    val labelRadius = radius * 0.78f
                    val x = center.x + (labelRadius * kotlin.math.cos(rad)).toFloat()
                    val y = center.y + (labelRadius * kotlin.math.sin(rad)).toFloat()
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = if (label == "N") theme.accentColor.toArgb() else theme.textColor.copy(alpha = 0.55f).toArgb()
                            textSize = 15.sp.toPx()
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = label == "N"
                        }
                        drawText(label, x, y + paint.textSize / 3, paint)
                    }
                }
                for (tick in 0 until 360 step 15) {
                    val rad = Math.toRadians((tick - 90).toDouble())
                    val outer = radius * 0.95f
                    val inner = if (tick % 90 == 0) radius * 0.82f else radius * 0.88f
                    val p1 = Offset(center.x + (outer * kotlin.math.cos(rad)).toFloat(), center.y + (outer * kotlin.math.sin(rad)).toFloat())
                    val p2 = Offset(center.x + (inner * kotlin.math.cos(rad)).toFloat(), center.y + (inner * kotlin.math.sin(rad)).toFloat())
                    drawLine(color = theme.textColor.copy(alpha = 0.3f), start = p1, end = p2, strokeWidth = 1.5.dp.toPx())
                }
            }

            // המחט מצביעה תמיד למעלה - כלפי הכיוון שהמכשיר עצמו פונה אליו
            val needleLength = radius * 0.6f
            drawLine(
                color = theme.accentColor,
                start = center,
                end = Offset(center.x, center.y - needleLength),
                strokeWidth = 3.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            drawCircle(color = theme.accentColor, radius = 5.dp.toPx(), center = center)
        }
    }
}
