package com.future.fitness.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.bluetooth.HrConnectionState
import com.future.fitness.location.RunTracker
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.formatElapsed
import com.future.fitness.ui.theme.FutureTheme
import kotlinx.coroutines.delay

/** מעקב GPS חי לכל סוג פעילות שנעה בחוץ - מרחק, זמן, וקצב (דקות לק"מ), ודופק
 * חי אם שעון/רצועה מחוברים. דורש ACCESS_FINE_LOCATION - מבוקש כשנכנסים למסך.
 * title/met/finishLabel פרמטריים כדי שאותו מסך ישרת גם את "ריצה חופשית" מהבית
 * וגם את סוגי הפעילות עם usesGps בקטלוג (הליכה/רכיבה/טיולי שטח בחוץ). */
@Composable
fun RunScreen(
    theme: FutureTheme,
    heartRateMonitor: HeartRateMonitor,
    weightKg: Int,
    title: String = "ריצה חופשית",
    met: Double = 9.0,
    finishLabel: String = "סיום ריצה",
    onBack: () -> Unit,
    onFinish: (minutes: Int, distanceKm: Double, calories: Int) -> Unit,
) {
    val context = LocalContext.current
    val tracker = remember { RunTracker(context) }
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) tracker.start()
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) tracker.start() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(tracker) {
        while (true) {
            delay(1000)
            if (tracker.isTracking) elapsedSec += 1
        }
    }

    val bpm = heartRateMonitor.currentBpm
    val isHrConnected = heartRateMonitor.state == HrConnectionState.CONNECTED

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = title, theme = theme, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (!hasLocationPermission) {
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                Text(
                    "כדי למדוד מרחק וקצב צריך הרשאת מיקום",
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
                ) { Text("אפשר הרשאת מיקום") }
                Spacer(Modifier.weight(1f))
            } else {
                Spacer(Modifier.height(24.dp))
                Text(formatElapsed(elapsedSec), color = theme.accentColor, fontSize = 48.sp, fontWeight = FontWeight.Bold)
                Text(if (tracker.hasFix) "עוקב אחרי המיקום" else "מחפש GPS...", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    RunStat(String.format("%.2f", tracker.distanceKm()), "ק״מ", theme)
                    RunStat(tracker.paceMinPerKm(elapsedSec)?.let { formatPace(it) } ?: "--:--", "קצב / ק״מ", theme)
                    if (isHrConnected && bpm != null) {
                        RunStat(bpm.toString(), "דופק", theme, icon = Icons.Rounded.Favorite)
                    }
                }

                Spacer(Modifier.weight(1f))

                Button(
                    onClick = {
                        tracker.stop()
                        val minutes = maxOf(1, Math.round(elapsedSec / 60f))
                        val distanceKm = tracker.distanceKm()
                        val calories = com.future.fitness.data.WorkoutStore.estimateCalories(met, weightKg, minutes)
                        onFinish(minutes, distanceKm, calories)
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(bottom = 20.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.DirectionsRun, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(finishLabel, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RunStat(value: String, label: String, theme: FutureTheme, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp).padding(bottom = 4.dp))
        }
        Text(value, color = theme.textColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
    }
}

private fun formatPace(minPerKm: Double): String {
    val m = minPerKm.toInt()
    val s = ((minPerKm - m) * 60).toInt()
    return "%d:%02d".format(m, s)
}
