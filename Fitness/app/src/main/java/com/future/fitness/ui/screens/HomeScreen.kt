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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.DailyStats
import com.future.fitness.data.Workout
import com.future.fitness.ui.components.IconListRow
import com.future.fitness.ui.components.StatTile
import com.future.fitness.ui.digitForKey
import com.future.fitness.ui.theme.FutureTheme

private data class MenuItem(val digit: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val subtitle: String, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    theme: FutureTheme,
    stats: DailyStats,
    nextWorkout: Workout,
    onOpenWorkouts: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenRun: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenNextWorkoutDetail: () -> Unit,
    onOpenActivityTypes: () -> Unit,
) {
    val items = listOf(
        MenuItem("1", Icons.Rounded.FitnessCenter, "אימונים", "תוכניות האימון שלך", onOpenWorkouts),
        MenuItem("2", Icons.AutoMirrored.Rounded.DirectionsRun, "ריצה", "מעקב GPS חי - מרחק וקצב", onOpenRun),
        MenuItem("3", Icons.Rounded.History, "היסטוריה", "אימונים קודמים", onOpenHistory),
        MenuItem("4", Icons.AutoMirrored.Rounded.TrendingUp, "התקדמות", "גרפים וסטטיסטיקה", onOpenProgress),
        MenuItem("5", Icons.Rounded.FavoriteBorder, "בריאות", "הסברים ואזורי דופק", onOpenHealth),
        MenuItem("6", Icons.Rounded.Settings, "הגדרות", "עיצוב, פרופיל ושעון חכם", onOpenSettings),
        MenuItem("7", Icons.Rounded.EmojiEvents, "כל סוגי האימונים", "כל סוגי הפעילות, כמו בשעון חכם", onOpenActivityTypes),
    )

    val firstItemFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstItemFocusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = digitForKey(event.key) ?: return@onKeyEvent false
                val match = items.firstOrNull { it.digit == digit } ?: return@onKeyEvent false
                match.onClick()
                true
            }
    ) {
        Text(
            "כושר",
            color = theme.textColor,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp),
        )

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    StatTile(Icons.Rounded.LocalFireDepartment, stats.caloriesToday.toString(), "קלוריות היום", theme, Modifier.weight(1f))
                    StatTile(Icons.Rounded.Timer, stats.activeMinutesToday.toString(), "דקות פעילות", theme, Modifier.weight(1f))
                    StatTile(Icons.Rounded.LocalFireDepartment, stats.streakDays.toString(), "ימי רצף", theme, Modifier.weight(1f))
                }
                Spacer(Modifier.height(18.dp))
            }

            item {
                com.future.fitness.ui.components.FocusableItem(
                    onClick = onOpenNextWorkoutDetail,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.surfaceColor, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier.size(48.dp).background(theme.accentColor.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("האימון הבא", color = theme.accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(nextWorkout.name, color = theme.textColor, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "${nextWorkout.exercises.size} תרגילים · ${nextWorkout.durationMin} דקות",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = null,
                            tint = theme.textColor.copy(alpha = 0.35f),
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
            }

            items(items) { menuItem ->
                IconListRow(
                    icon = menuItem.icon,
                    title = menuItem.label,
                    subtitle = menuItem.subtitle,
                    theme = theme,
                    onClick = menuItem.onClick,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 5.dp),
                    digit = menuItem.digit,
                    focusRequester = if (menuItem.digit == "1") firstItemFocusRequester else null,
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}
