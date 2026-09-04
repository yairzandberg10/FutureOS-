package com.future.fitness.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.KeyEvent as AndroidKeyEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Terrain
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.bluetooth.HrConnectionState
import com.future.fitness.data.WorkoutActivityType
import com.future.fitness.data.WorkoutStore
import com.future.fitness.data.WorkoutTemplate
import com.future.fitness.data.template
import com.future.fitness.location.RunTracker
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.formatElapsed
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

private enum class WorkoutPage { MAIN, DYNAMICS, POWER_CADENCE, ELEVATION, HR_ZONES, MUSIC }

/**
 * מסך אימון חי מאוחד לפי תבנית (ראו WorkoutTemplate) - מחליף את הפיצול הקודם
 * בין RunScreen (GPS) ל-QuickStartScreen (גנרי). ניתן לדפדף בין מסכי-המשנה
 * עם חץ שמאל/ימין (בלי מגע, כמו בכל FutureOS) - כמו הדפדוף בין מסכי אימון
 * ב-Apple Watch. עמודי המשנה תלויים בתבנית ובחיישנים שבפועל מחוברים דרך
 * heartRateMonitor (דופק תמיד; קצב-צעדים/עוצמת-רכיבה רק אם שעון/חיישן
 * תואם-Bluetooth-SIG מחובר בפועל - ראו HeartRateMonitor).
 */
@Composable
fun WorkoutTemplateScreen(
    activityType: WorkoutActivityType,
    theme: FutureTheme,
    weightKg: Int,
    age: Int?,
    heartRateMonitor: HeartRateMonitor,
    title: String = activityType.displayName,
    finishLabel: String = "סיום",
    onBack: () -> Unit,
    onFinish: (minutes: Int, distanceKm: Double?, calories: Int, avgHr: Int?, maxHr: Int?) -> Unit,
) {
    val context = LocalContext.current
    val template = remember(activityType) { activityType.template() }
    val usesGps = activityType.usesGps

    val tracker = remember { RunTracker(context) }
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) tracker.start()
    }
    LaunchedEffect(Unit) {
        if (usesGps) {
            if (hasLocationPermission) tracker.start() else permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    // בלי זה, יציאה במקש "חזור" (במקום סיום רשמי דרך onFinish) משאירה את
    // ה-LocationListener רשום לצמיתות - GPS ימשיך לפעול ברקע ומנקז סוללה.
    DisposableEffect(Unit) {
        onDispose { if (usesGps) tracker.stop() }
    }

    var elapsedSec by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var hrSum by remember { mutableIntStateOf(0) }
    var hrSamples by remember { mutableIntStateOf(0) }
    var maxHrSeen by remember { mutableIntStateOf(0) }
    // שניות מצטברות בכל אחד מ-5 אזורי הדופק הקלאסיים (50-60% / 60-70% / ... /
    // 90%+ מהדופק המקסימלי המוערך - נוסחת 220-גיל), לתצוגה במסך "אזורי דופק".
    val zoneSeconds = remember { mutableStateListOf(0, 0, 0, 0, 0) }
    val estimatedMaxHr = remember(age) { age?.let { WorkoutStore.estimateMaxHr(it) } }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (running) {
                elapsedSec += 1
                val bpm = heartRateMonitor.currentBpm
                if (bpm != null && bpm > 0) {
                    hrSum += bpm
                    hrSamples += 1
                    if (bpm > maxHrSeen) maxHrSeen = bpm
                    val maxHr = estimatedMaxHr
                    if (maxHr != null && maxHr > 0) {
                        val pct = bpm * 100 / maxHr
                        val zoneIndex = when {
                            pct < 60 -> -1 // מתחת לאזור 1, לא נספר
                            pct < 70 -> 0
                            pct < 80 -> 1
                            pct < 90 -> 2
                            pct < 100 -> 3
                            else -> 4
                        }
                        if (zoneIndex in 0..4) zoneSeconds[zoneIndex] = zoneSeconds[zoneIndex] + 1
                    }
                }
            }
        }
    }

    val pages = remember(template, usesGps, heartRateMonitor.hasRunningCadenceSensor, heartRateMonitor.hasCyclingPowerSensor) {
        buildList {
            add(WorkoutPage.MAIN)
            if (template == WorkoutTemplate.RUN_WALK) add(WorkoutPage.DYNAMICS)
            if (template == WorkoutTemplate.CYCLING) add(WorkoutPage.POWER_CADENCE)
            if (usesGps) add(WorkoutPage.ELEVATION)
            add(WorkoutPage.HR_ZONES)
            add(WorkoutPage.MUSIC)
        }
    }
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val pagerScope = rememberCoroutineScope()
    val pagerFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { pagerFocusRequester.requestFocus() }

    val isHrConnected = heartRateMonitor.state == HrConnectionState.CONNECTED
    val bpm = heartRateMonitor.currentBpm

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = title, theme = theme, onBack = onBack)

        // נקודות עמוד - מציגות באיזה מסך-משנה נמצאים ושכמה יש בסה"כ, בלי מגע.
        if (pages.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                pages.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (index == pagerState.currentPage) 7.dp else 5.dp)
                            .background(
                                if (index == pagerState.currentPage) theme.accentColor else theme.textColor.copy(alpha = 0.25f),
                                CircleShape,
                            ),
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false, // אין מגע במכשיר הזה - הדפדוף אך ורק דרך חצי שמאל/ימין
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .focusRequester(pagerFocusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft -> {
                            if (pagerState.currentPage < pages.lastIndex) {
                                pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            }
                            true
                        }
                        Key.DirectionRight -> {
                            if (pagerState.currentPage > 0) {
                                pagerScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                            true
                        }
                        else -> false
                    }
                },
        ) { pageIndex ->
            when (pages[pageIndex]) {
                WorkoutPage.MAIN -> MainPage(
                    theme = theme,
                    activityType = activityType,
                    usesGps = usesGps,
                    tracker = tracker,
                    hasLocationPermission = hasLocationPermission,
                    onRequestLocationPermission = { permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    elapsedSec = elapsedSec,
                    weightKg = weightKg,
                    isHrConnected = isHrConnected,
                    bpm = bpm,
                )
                WorkoutPage.DYNAMICS -> RunningDynamicsPage(theme = theme, heartRateMonitor = heartRateMonitor)
                WorkoutPage.POWER_CADENCE -> CyclingPowerPage(theme = theme, heartRateMonitor = heartRateMonitor)
                WorkoutPage.ELEVATION -> ElevationPage(theme = theme, tracker = tracker)
                WorkoutPage.HR_ZONES -> HeartRateZonesPage(theme = theme, estimatedMaxHr = estimatedMaxHr, zoneSeconds = zoneSeconds, currentBpm = if (isHrConnected) bpm else null)
                WorkoutPage.MUSIC -> MusicControlPage(theme = theme)
            }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusableItem(onClick = { running = !running }, theme = theme, modifier = Modifier.size(56.dp)) { isFocused ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(if (isFocused) theme.accentColor.copy(alpha = 0.22f) else theme.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (running) "השהה" else "המשך",
                        tint = theme.textColor,
                    )
                }
            }
            FocusableItem(
                onClick = {
                    if (usesGps) tracker.stop()
                    val minutes = maxOf(1, Math.round(elapsedSec / 60f))
                    val distanceKm = if (usesGps) tracker.distanceKm() else null
                    val calories = WorkoutStore.estimateCalories(activityType.met, weightKg, minutes)
                    val avgHr = if (hrSamples > 0) hrSum / hrSamples else null
                    onFinish(minutes, distanceKm, calories, avgHr, maxHrSeen.takeIf { it > 0 })
                },
                theme = theme,
                modifier = Modifier.weight(1f).height(56.dp),
            ) { isFocused ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.accentColor, RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(finishLabel, color = theme.backgroundColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun MainPage(
    theme: FutureTheme,
    activityType: WorkoutActivityType,
    usesGps: Boolean,
    tracker: RunTracker,
    hasLocationPermission: Boolean,
    onRequestLocationPermission: () -> Unit,
    elapsedSec: Int,
    weightKg: Int,
    isHrConnected: Boolean,
    bpm: Int?,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (usesGps && !hasLocationPermission) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
            Text(
                "כדי למדוד מרחק וקצב צריך הרשאת מיקום",
                color = theme.textColor.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 16.dp),
            )
            Button(
                onClick = onRequestLocationPermission,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
            ) { Text("אפשר הרשאת מיקום") }
            return@Column
        }

        Text(formatElapsed(elapsedSec), color = theme.accentColor, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        if (usesGps) {
            Text(if (tracker.hasFix) "עוקב אחרי המיקום" else "מחפש GPS...", color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(bottom = 20.dp))
        } else {
            Spacer(Modifier.height(24.dp))
        }

        val minutesSoFar = maxOf(0, elapsedSec / 60)
        val calories = WorkoutStore.estimateCalories(activityType.met, weightKg, minutesSoFar)

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            if (usesGps) {
                if (activityType.template() == WorkoutTemplate.CYCLING) {
                    StatColumn(String.format("%.1f", tracker.currentSpeedKmh ?: 0.0), "קמ\"ש", theme)
                } else {
                    StatColumn(tracker.paceMinPerKm(elapsedSec)?.let { formatPace(it) } ?: "--:--", "קצב / ק\"מ", theme)
                }
                StatColumn(String.format("%.2f", tracker.distanceKm()), "ק\"מ", theme)
            }
            StatColumn(calories.toString(), "קלוריות", theme)
            if (isHrConnected && bpm != null) {
                StatColumn(bpm.toString(), "דופק", theme, icon = Icons.Rounded.Favorite)
            }
        }
    }
}

@Composable
private fun RunningDynamicsPage(theme: FutureTheme, heartRateMonitor: HeartRateMonitor) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("דינמיקת ריצה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
        if (!heartRateMonitor.hasRunningCadenceSensor) {
            NoSensorMessage(theme, "אין חיישן קצב-צעדים מחובר (Running Speed and Cadence) - התחברו לשעון/חיישן תואם בהגדרות")
            return@Column
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn(heartRateMonitor.runningCadenceSpm?.toString() ?: "--", "צעדים/דקה", theme)
            StatColumn(heartRateMonitor.runningSpeedKmh?.let { String.format("%.1f", it) } ?: "--", "קמ\"ש", theme)
        }
        heartRateMonitor.runningStrideLengthM?.let { stride ->
            Spacer(Modifier.height(16.dp))
            StatColumn(String.format("%.2f", stride), "מ' לצעד", theme)
        }
    }
}

@Composable
private fun CyclingPowerPage(theme: FutureTheme, heartRateMonitor: HeartRateMonitor) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("עוצמה וקצב דיווש", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
        if (!heartRateMonitor.hasCyclingPowerSensor) {
            NoSensorMessage(theme, "אין מד-כוח מחובר (Cycling Power) - התחברו לשעון/מד-כוח תואם בהגדרות")
            return@Column
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn("${heartRateMonitor.cyclingPowerWatts ?: "--"}", "וואט", theme)
            StatColumn(heartRateMonitor.cyclingCadenceRpm?.toString() ?: "--", "סל\"ד", theme)
        }
    }
}

@Composable
private fun ElevationPage(theme: FutureTheme, tracker: RunTracker) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Rounded.Terrain, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(32.dp))
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            StatColumn(tracker.currentAltitudeMeters?.let { "%.0f".format(it) } ?: "--", "מ' גובה", theme)
            StatColumn("%.0f".format(tracker.elevationGainMeters), "מ' עלייה", theme)
        }
        Text(
            "מבוסס על GPS בלבד (בלי חיישן ברומטרי) - ייתכן רעש/אי-דיוק",
            color = theme.textColor.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 16.dp),
        )
    }
}

@Composable
private fun HeartRateZonesPage(theme: FutureTheme, estimatedMaxHr: Int?, zoneSeconds: List<Int>, currentBpm: Int?) {
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("אזורי דופק", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 12.dp))
        if (estimatedMaxHr == null) {
            NoSensorMessage(theme, "הגדירו גיל בהגדרות כדי לחשב אזורי דופק (מבוסס על 220-גיל)")
            return@Column
        }
        if (currentBpm != null) {
            Text("$currentBpm BPM כרגע", color = theme.accentColor, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))
        }
        val zoneLabels = listOf("אזור 1 · חימום", "אזור 2 · קל", "אזור 3 · בינוני", "אזור 4 · קשה", "אזור 5 · מקסימלי")
        val totalSeconds = zoneSeconds.sum().coerceAtLeast(1)
        zoneLabels.forEachIndexed { index, label ->
            val seconds = zoneSeconds.getOrElse(index) { 0 }
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = theme.textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
                Text(formatElapsed(seconds), color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(theme.textColor.copy(alpha = 0.08f), RoundedCornerShape(3.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(seconds.toFloat() / totalSeconds.toFloat())
                        .height(6.dp)
                        .background(theme.accentColor, RoundedCornerShape(3.dp)),
                )
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
private fun MusicControlPage(theme: FutureTheme) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("בקרת מוזיקה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
        Text(
            "שולט על נגן המדיה הפעיל כרגע במערכת",
            color = theme.textColor.copy(alpha = 0.4f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 20.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaButton(theme, Icons.Rounded.SkipPrevious, "הקודם") { sendMediaKey(context, AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS) }
            MediaButton(theme, Icons.Rounded.PlayArrow, "נגן/השהה", primary = true) { sendMediaKey(context, AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) }
            MediaButton(theme, Icons.Rounded.SkipNext, "הבא") { sendMediaKey(context, AndroidKeyEvent.KEYCODE_MEDIA_NEXT) }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            MediaButton(theme, Icons.AutoMirrored.Rounded.VolumeDown, "הנמך") { adjustVolume(context, AudioManager.ADJUST_LOWER) }
            MediaButton(theme, Icons.AutoMirrored.Rounded.VolumeUp, "הגבר") { adjustVolume(context, AudioManager.ADJUST_RAISE) }
        }
    }
}

@Composable
private fun MediaButton(theme: FutureTheme, icon: ImageVector, contentDescription: String, primary: Boolean = false, onClick: () -> Unit) {
    FocusableItem(onClick = onClick, theme = theme, modifier = Modifier.size(56.dp)) { isFocused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (primary) theme.accentColor else if (isFocused) theme.accentColor.copy(alpha = 0.22f) else theme.textColor.copy(alpha = 0.08f),
                    RoundedCornerShape(16.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = if (primary) theme.backgroundColor else theme.textColor)
        }
    }
}

@Composable
private fun NoSensorMessage(theme: FutureTheme, text: String) {
    Text(text, color = theme.textColor.copy(alpha = 0.5f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun StatColumn(value: String, label: String, theme: FutureTheme, icon: ImageVector? = null) {
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

/** שולח אירוע מקש-מדיה סטנדרטי (ACTION_MEDIA_BUTTON) לנגן המדיה הפעיל כרגע
 * במערכת - אותו מנגנון שמשמש כפתורי מדיה על אוזניות/רכב, בלי צורך בהרשאת
 * "גישה להתראות" (NotificationListenerService) לאף אפליקציית מוזיקה ספציפית. */
private fun sendMediaKey(context: Context, keyCode: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    val downEvent = AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, keyCode)
    val upEvent = AndroidKeyEvent(AndroidKeyEvent.ACTION_UP, keyCode)
    audioManager.dispatchMediaKeyEvent(downEvent)
    audioManager.dispatchMediaKeyEvent(upEvent)
}

private fun adjustVolume(context: Context, direction: Int) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI)
}
