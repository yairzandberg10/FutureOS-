package com.future.fitness.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.bluetooth.HrConnectionState
import com.future.fitness.data.WorkoutActivityType
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.formatElapsed
import com.future.fitness.ui.theme.FutureTheme
import kotlinx.coroutines.delay

/** מסך מעקב חי גנרי לכל סוג פעילות שאין לו זרימה ייעודית (לא ריצה/הליכה/רכיבה
 * בחוץ עם GPS, ולא תוכנית אימון עם תרגילים/סטים) - טיימר עולה, קלוריות
 * לפי met*משקל*שעות, ודופק חי/ממוצע/מקסימלי אם שעון מחובר. מכסה את רוב סוגי
 * הפעילות בקטלוג (יוגה, ספורט קבוצתי, ספורט חורף וכו') באותה זרימה אחת. */
@Composable
fun QuickStartScreen(
    activityType: WorkoutActivityType,
    theme: FutureTheme,
    weightKg: Int,
    heartRateMonitor: HeartRateMonitor,
    onBack: () -> Unit,
    onFinish: (minutes: Int, calories: Int, avgHr: Int?, maxHr: Int?) -> Unit,
) {
    var elapsedSec by remember { mutableIntStateOf(0) }
    var running by remember { mutableStateOf(true) }
    var hrSum by remember { mutableIntStateOf(0) }
    var hrSamples by remember { mutableIntStateOf(0) }
    var maxHr by remember { mutableIntStateOf(0) }

    val isHrConnected = heartRateMonitor.state == HrConnectionState.CONNECTED
    val liveBpm = heartRateMonitor.currentBpm

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            if (running) elapsedSec += 1
            heartRateMonitor.currentBpm?.let { bpm ->
                if (bpm > 0) {
                    hrSum += bpm
                    hrSamples += 1
                    if (bpm > maxHr) maxHr = bpm
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = activityType.displayName, theme = theme, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier.size(64.dp).background(theme.accentColor.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(activityType.icon, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(formatElapsed(elapsedSec), color = theme.accentColor, fontSize = 48.sp, fontWeight = FontWeight.Bold)

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)) {
                Text(if (running) "עוקב אחרי הפעילות" else "מושהה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                if (isHrConnected && liveBpm != null) {
                    Text(" · ", color = theme.textColor.copy(alpha = 0.3f), fontSize = 12.sp)
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("$liveBpm", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            val minutesSoFar = maxOf(0, elapsedSec / 60)
            val caloriesSoFar = WorkoutStore.estimateCalories(activityType.met, weightKg, minutesSoFar)
            Text(
                "~$caloriesSoFar קלוריות עד כה",
                color = theme.textColor.copy(alpha = 0.5f),
                fontSize = 13.sp,
            )
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
                    val minutes = maxOf(1, Math.round(elapsedSec / 60f))
                    val calories = WorkoutStore.estimateCalories(activityType.met, weightKg, minutes)
                    val avgHr = if (hrSamples > 0) hrSum / hrSamples else null
                    onFinish(minutes, calories, avgHr, maxHr.takeIf { it > 0 })
                },
                theme = theme,
                modifier = Modifier.weight(1f).height(56.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.accentColor, RoundedCornerShape(16.dp)),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = theme.backgroundColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("סיום", color = theme.backgroundColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
