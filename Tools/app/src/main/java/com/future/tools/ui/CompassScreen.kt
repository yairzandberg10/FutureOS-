package com.future.tools.ui
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.mutedTextColor

import com.future.sharednav.theme.FutureTypography
import android.Manifest
import android.annotation.SuppressLint
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
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

private data class LocationFix(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val accuracy: Float,
    val declination: Float,
    /** כיוון התנועה לפי ה-GPS - רק כשזזים מספיק מהר כדי שיהיה אמין */
    val course: Float?,
)

private const val JERUSALEM_LAT = 31.7781
private const val JERUSALEM_LON = 35.2354

/** כיוון (0-360 מצפון אמיתי) מהנקודה הנוכחית לנקודת היעד, לפי מסלול מעגל גדול. */
private fun bearingTo(lat: Double, lon: Double, toLat: Double, toLon: Double): Float {
    val p1 = Math.toRadians(lat); val p2 = Math.toRadians(toLat)
    val dl = Math.toRadians(toLon - lon)
    val y = kotlin.math.sin(dl) * kotlin.math.cos(p2)
    val x = kotlin.math.cos(p1) * kotlin.math.sin(p2) - kotlin.math.sin(p1) * kotlin.math.cos(p2) * kotlin.math.cos(dl)
    return ((Math.toDegrees(kotlin.math.atan2(y, x)) + 360.0) % 360.0).toFloat()
}

/**
 * מיקום נוכחי מ-GPS/רשת. ממנו נגזרת סטיית המגנטיות (GeomagneticField) כדי
 * להפוך את הכיוון המגנטי של החיישן לצפון אמיתי, ובתנועה גם כיוון ה-GPS עצמו.
 */
@SuppressLint("MissingPermission")
@Composable
private fun rememberLocationFix(enabled: Boolean): LocationFix? {
    val context = LocalContext.current
    var fix by remember { mutableStateOf<LocationFix?>(null) }

    DisposableEffect(enabled) {
        if (!enabled) return@DisposableEffect onDispose {}
        val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
        fun publish(location: Location) {
            val field = GeomagneticField(
                location.latitude.toFloat(), location.longitude.toFloat(),
                location.altitude.toFloat(), System.currentTimeMillis()
            )
            fix = LocationFix(
                latitude = location.latitude,
                longitude = location.longitude,
                altitude = if (location.hasAltitude()) location.altitude else null,
                accuracy = location.accuracy,
                declination = field.declination,
                course = if (location.hasBearing() && location.hasSpeed() && location.speed > 1.5f) location.bearing else null,
            )
        }
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) = publish(location)
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
        }
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .filter { runCatching { lm.isProviderEnabled(it) }.getOrDefault(false) }
        // מיקום אחרון ידוע - כדי שהצפון האמיתי יופיע מיד, לפני שה-GPS ננעל
        providers.mapNotNull { runCatching { lm.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }?.let(::publish)
        providers.forEach {
            runCatching { lm.requestLocationUpdates(it, 1000L, 0f, listener, android.os.Looper.getMainLooper()) }
        }
        onDispose { lm.removeUpdates(listener) }
    }
    return fix
}

private fun formatCoordinate(value: Double, positive: String, negative: String): String {
    val abs = kotlin.math.abs(value)
    val degrees = abs.toInt()
    val minutesFull = (abs - degrees) * 60
    val minutes = minutesFull.toInt()
    val seconds = (minutesFull - minutes) * 60
    return "%d°%02d'%04.1f\" %s".format(degrees, minutes, seconds, if (value >= 0) positive else negative)
}

@Composable
fun CompassScreen(theme: FutureTheme, onBack: () -> Unit) {
    val compassState = rememberCompassState()
    val hasLocationPermission by rememberRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION)
    val fix = rememberLocationFix(hasLocationPermission)
    // עם מיקום - צפון אמיתי (מגנטי + סטייה מקומית); בתנועה מהירה כיוון ה-GPS מדויק יותר מהמגנטומטר
    val azimuth = when {
        fix?.course != null -> fix.course
        fix != null -> (compassState.azimuth + fix.declination + 360f) % 360f
        else -> compassState.azimuth
    }
    val jerusalemBearing = fix?.let { bearingTo(it.latitude, it.longitude, JERUSALEM_LAT, JERUSALEM_LON) }
    val altitudeState = rememberAltitudeState()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(title = "מצפן", theme = theme, onBack = onBack)

                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    if (!compassState.hasSensors) {
                        Text(
                            "לא נמצא חיישן מצפן (תאוצה/מגנטומטר) במכשיר הזה",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.body,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CompassRose(azimuth = azimuth, targetBearing = jerusalemBearing, theme = theme)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("${azimuth.roundToInt() % 360}°", color = theme.textColor, fontSize = FutureTypography.display, fontWeight = FontWeight.Light)
                            Text(cardinalFor(azimuth), color = theme.mutedTextColor, fontSize = FutureTypography.bodyLarge)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                when {
                                    fix?.course != null -> "כיוון תנועה לפי GPS"
                                    fix != null -> "צפון אמיתי · סטייה מגנטית ${"%.1f".format(fix.declination)}°"
                                    !hasLocationPermission -> "צפון מגנטי · אין הרשאת מיקום"
                                    else -> "צפון מגנטי · מחפש מיקום"
                                },
                                color = theme.subtleTextColor,
                                fontSize = FutureTypography.caption
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            if (fix != null) {
                                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                                    Text(
                                        "${formatCoordinate(fix.latitude, "N", "S")}   ${formatCoordinate(fix.longitude, "E", "W")}",
                                        color = theme.textColor,
                                        fontSize = FutureTypography.body,
                                        fontFamily = FutureTypography.monoFamily
                                    )
                                }
                                Text(
                                    "דיוק ±${fix.accuracy.roundToInt()} מ'" + (jerusalemBearing?.let { " · ירושלים ${it.roundToInt()}°" } ?: ""),
                                    color = theme.subtleTextColor,
                                    fontSize = FutureTypography.caption
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                            AltitudeRow(altitudeState = altitudeState, gpsAltitude = fix?.altitude, theme = theme)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AltitudeRow(altitudeState: AltitudeState, gpsAltitude: Double?, theme: FutureTheme) {
    if (!altitudeState.hasSensor && gpsAltitude != null) {
        Text("${gpsAltitude.roundToInt()} מ' מעל פני הים (GPS)", color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Medium)
        return
    }
    if (!altitudeState.hasSensor) {
        Text(
            "אין חיישן לחץ ברומטרי - לא ניתן למדוד גובה",
            color = theme.subtleTextColor,
            fontSize = FutureTypography.label
        )
        return
    }
    val meters = altitudeState.meters
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (meters != null) "${meters.roundToInt()} מ' מעל פני הים" else "מודד גובה",
            color = theme.textColor,
            fontSize = FutureTypography.title,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text("גובה משוער לפי לחץ אטמוספרי", color = theme.subtleTextColor, fontSize = FutureTypography.caption)
    }
}

@Composable
private fun CompassRose(azimuth: Float, targetBearing: Float?, theme: FutureTheme) {
    // המסך הוא 320x480dp - וורד קטן יותר משאיר מקום לקואורדינטות ולגובה
    val size = 160.dp
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = this.size.minDimension / 2f
            drawCircle(color = theme.textColor.copy(alpha = 0.08f), radius = radius)
            drawCircle(color = theme.textAlpha(30), radius = radius, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx()))

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
                            textSize = FutureTypography.dialog.toPx()
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

            // סמן כיוון ירושלים על היקף הוורד - זז יחד עם הכיוונים
            if (targetBearing != null) {
                val rad = Math.toRadians((targetBearing - azimuth - 90).toDouble())
                val r = radius * 0.62f
                drawCircle(
                    color = theme.successColor,
                    radius = 6.dp.toPx(),
                    center = Offset(center.x + (r * kotlin.math.cos(rad)).toFloat(), center.y + (r * kotlin.math.sin(rad)).toFloat())
                )
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
