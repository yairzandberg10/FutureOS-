package com.future.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.WorkoutHistoryEntry
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.components.StatTile
import com.future.fitness.ui.theme.FutureTheme
import java.util.Calendar

private data class DayBar(val label: String, val minutes: Int, val isToday: Boolean)

@Composable
fun ProgressScreen(
    history: List<WorkoutHistoryEntry>,
    theme: FutureTheme,
    onBack: () -> Unit,
) {
    val weekBars = remember(history) { buildWeekBars(history) }
    val maxMinutes = (weekBars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    val calNow = Calendar.getInstance()
    val monthEntries = history.filter {
        val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
        c.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) && c.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
    }
    val monthWorkouts = monthEntries.size
    val monthMinutes = monthEntries.sumOf { it.minutes }
    val monthAvg = if (monthWorkouts > 0) monthMinutes / monthWorkouts else 0

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "התקדמות", theme = theme, onBack = onBack)

        Column(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .background(theme.surfaceColor, RoundedCornerShape(16.dp))
                    .padding(18.dp),
            ) {
                Text("פעילות השבוע (דקות)", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    weekBars.forEach { bar ->
                        Column(modifier = Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                                val heightFraction = (bar.minutes.toFloat() / maxMinutes.toFloat()).coerceAtLeast(0.04f)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .fillMaxHeight(heightFraction)
                                        .background(
                                            if (bar.isToday) theme.accentColor else theme.accentColor.copy(alpha = 0.3f),
                                            RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                                        ),
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(bar.minutes.toString(), color = theme.textColor.copy(alpha = 0.5f), fontSize = 9.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(bar.label, color = theme.textColor.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatTile(Icons.Rounded.FitnessCenter, monthWorkouts.toString(), "אימונים החודש", theme, Modifier.weight(1f))
                StatTile(Icons.Rounded.Timer, monthMinutes.toString(), "דקות סה״כ", theme, Modifier.weight(1f))
                StatTile(Icons.AutoMirrored.Rounded.TrendingUp, monthAvg.toString(), "ממוצע לאימון", theme, Modifier.weight(1f))
            }
        }
    }
}

/** מרכיב 7 עמודות (א'-ש') מסך היום אחורה, עם סך הדקות שנצברו בהיסטוריה
 * בכל יום - נתונים אמיתיים מההיסטוריה השמורה, לא מספרים בדויים. */
private fun buildWeekBars(history: List<WorkoutHistoryEntry>): List<DayBar> {
    val labels = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
    val today = Calendar.getInstance()
    val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK) // 1=Sunday .. 7=Saturday

    val minutesByDayOfWeek = IntArray(8)
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    history.forEach { entry ->
        val c = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
        if (!c.before(sevenDaysAgo)) {
            minutesByDayOfWeek[c.get(Calendar.DAY_OF_WEEK)] += entry.minutes
        }
    }

    return (0..6).map { offset ->
        val dow = ((todayDayOfWeek - 1 - offset + 7) % 7) + 1
        DayBar(label = labels[dow - 1], minutes = minutesByDayOfWeek[dow], isToday = offset == 0)
    }.reversed()
}
