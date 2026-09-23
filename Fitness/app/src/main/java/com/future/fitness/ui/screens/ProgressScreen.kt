package com.future.fitness.ui.screens
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Route

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.fitness.ui.components.FitnessTextField

import com.future.sharednav.theme.FutureTypography
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.future.fitness.data.DailyStats
import com.future.fitness.data.WorkoutHistoryEntry
import com.future.sharednav.theme.FutureTheme
import java.util.Calendar

private data class DayBar(val label: String, val minutes: Int, val isToday: Boolean)
private data class PersonalRecord(val icon: androidx.compose.ui.graphics.vector.ImageVector, val title: String, val subtitle: String, val value: String, val unit: String, val color: androidx.compose.ui.graphics.Color)

@Composable
fun ProgressScreen(
    history: List<WorkoutHistoryEntry>,
    stats: DailyStats,
    theme: FutureTheme,
) {
    val weekBars = remember(history) { buildWeekBars(history) }
    val maxMinutes = (weekBars.maxOfOrNull { it.minutes } ?: 0).coerceAtLeast(1)

    val calNow = Calendar.getInstance()
    val monthEntries = remember(history) {
        history.filter {
            val c = Calendar.getInstance().apply { timeInMillis = it.dateMillis }
            c.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) && c.get(Calendar.MONTH) == calNow.get(Calendar.MONTH)
        }
    }
    val monthWorkouts = monthEntries.size
    val monthMinutes = monthEntries.sumOf { it.minutes }
    val totalCalories = remember(history) { history.sumOf { it.calories } }

    val records = remember(history) { buildPersonalRecords(history, theme) }
    val nextMilestone = remember(history) { WORKOUT_MILESTONES.firstOrNull { it > history.size } ?: (WORKOUT_MILESTONES.last() * 2) }

    // המסך הזה תצוגה בלבד (אין כרטיס לחיץ), אז בלי טיפול מפורש ב-D-pad
    // למעלה/למטה, DOWN היה פשוט מזיז את הפוקוס האמיתי לכפתור "אימון חי"
    // בסרגל התחתון במקום לגלול - אותו דפוס בדיוק כמו RecipeDetailScreen
    // בפריקסה עבור מסך גלילה-בלבד.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("סקירה ואנליטיקה", color = theme.sectionHeaderColor, fontSize = FutureTypography.label, fontWeight = FontWeight.Bold)
            Text("התקדמות", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { scope.launch { listState.animateScrollBy(220f) }; true }
                        Key.DirectionUp -> { scope.launch { listState.animateScrollBy(-220f) }; true }
                        else -> false
                    }
                },
            // ריפוד עליון קטן - ר' ההסבר המלא ב-HomeScreen.kt (בלעדיו הפריט
            // הראשון לא מצטייר בקומפוזיציה הראשונה תחת enableEdgeToEdge).
            contentPadding = PaddingValues(top = 4.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                // כרטיס סיכום בנטו - 3 מדדים אמיתיים מהחודש הנוכחי
                Row(
                    modifier = Modifier.fillMaxWidth().background(theme.surfaceColor, FutureShapes.xl).padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    SummaryStat(monthWorkouts.toString(), "אימונים החודש", theme)
                    SummaryStat("$totalCalories", "קק״ל סה״כ", theme)
                    SummaryStat(stats.streakDays.toString(), "ימי רצף", theme)
                }
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceColor, FutureShapes.xl)
                        .padding(18.dp),
                ) {
                    Text("עומס שבועי (דקות)", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold)
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
                                                if (bar.isToday) theme.readableAccentColor else theme.readableAccentColor.copy(alpha = 0.3f),
                                                RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp, bottomStart = 2.dp, bottomEnd = 2.dp),
                                            ),
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(bar.minutes.toString(), color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                                Spacer(Modifier.height(4.dp))
                                Text(bar.label, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.caption)
                            }
                        }
                    }
                }
            }

            if (records.isNotEmpty()) {
                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Row {
                            Icon(Icons.Rounded.EmojiEvents, contentDescription = null, tint = theme.dangerColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("שיאים אישיים", color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                        Text("${records.size} נקודות ציון", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
                    }
                }
                items(records.size) { index ->
                    val record = records[index]
                    Row(
                        modifier = Modifier.fillMaxWidth().background(theme.surfaceColor, FutureShapes.xl).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).background(record.color.copy(alpha = 0.16f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(record.icon, contentDescription = null, tint = record.color, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(record.title, color = theme.textColor, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold)
                            Text(record.subtitle, color = theme.textColor.copy(alpha = 0.55f), fontSize = FutureTypography.caption)
                        }
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(record.value, color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.ExtraBold)
                            Text(" ${record.unit}", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().background(theme.surfaceColor, FutureShapes.xl).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).background(theme.successColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = theme.successColor, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("יעד הבא: $nextMilestone אימונים", color = theme.textColor, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold)
                        Text("השלמת ${history.size} אימונים עד כה", color = theme.textColor.copy(alpha = 0.55f), fontSize = FutureTypography.caption)
                    }
                    Text(
                        "${((history.size.toFloat() / nextMilestone) * 100).toInt()}%",
                        color = theme.successColor,
                        fontSize = FutureTypography.bodyLarge,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, theme: FutureTheme) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.ExtraBold)
        Text(label, color = theme.textColor.copy(alpha = 0.55f), fontSize = FutureTypography.caption)
    }
}

private val WORKOUT_MILESTONES = listOf(5, 10, 25, 50, 100, 250)

private fun buildPersonalRecords(history: List<WorkoutHistoryEntry>, theme: FutureTheme): List<PersonalRecord> {
    if (history.isEmpty()) return emptyList()
    val records = mutableListOf<PersonalRecord>()

    val longest = history.maxByOrNull { it.minutes }
    if (longest != null) {
        records.add(PersonalRecord(FutureIcons.Timer, "האימון הארוך ביותר", longest.name, longest.minutes.toString(), "דקות", theme.readableAccentColor))
    }

    val mostCalories = history.maxByOrNull { it.calories }
    if (mostCalories != null) {
        records.add(PersonalRecord(Icons.Rounded.LocalFireDepartment, "שריפת הקלוריות הגדולה ביותר", mostCalories.name, mostCalories.calories.toString(), "קק״ל", theme.dangerColor))
    }

    val longestRun = history.filter { it.distanceKm != null }.maxByOrNull { it.distanceKm!! }
    if (longestRun?.distanceKm != null) {
        records.add(PersonalRecord(Icons.Rounded.Route, "המרחק הארוך ביותר", longestRun.name, "%.2f".format(longestRun.distanceKm), "ק״מ", theme.successColor))
    }

    return records
}

private fun buildWeekBars(history: List<WorkoutHistoryEntry>): List<DayBar> {
    val labels = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
    val today = Calendar.getInstance()
    val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

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
