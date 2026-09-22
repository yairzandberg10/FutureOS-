package com.future.fitness.ui.screens
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
import com.future.sharednav.theme.FutureShapes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.Workout
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.FocusableItem
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.theme.FutureTheme

private val DIFFICULTIES = listOf("הכל", "קל", "בינוני", "קשה")

@Composable
fun WorkoutsScreen(
    workouts: List<Workout>,
    weightKg: Int,
    theme: FutureTheme,
    onOpenWorkout: (String) -> Unit,
    onOpenBuilder: () -> Unit,
    onDeleteCustom: (String) -> Unit,
    onOpenRun: () -> Unit,
    onOpenActivityTypes: () -> Unit,
) {
    var selectedDifficulty by remember { mutableStateOf("הכל") }
    val filteredWorkouts = remember(workouts, selectedDifficulty) {
        if (selectedDifficulty == "הכל") workouts else workouts.filter { it.difficulty == selectedDifficulty }
    }

    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    // מחיקת אימון מותאם היא בלתי הפיכה, וכפתור המחיקה יושב באותה שורה
    // שעליה המשתמש מנווט - בלי אישור, לחיצת OK אחת בשוגג מוחקת אותו.
    var pendingDelete by remember { mutableStateOf<Workout?>(null) }
    pendingDelete?.let { workout ->
        ConfirmDialog(
            message = "למחוק את האימון \"${workout.name}\"?",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { pendingDelete = null },
            onConfirm = {
                onDeleteCustom(workout.id)
                pendingDelete = null
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            Text("ספריית התוכן שלך", color = theme.sectionHeaderColor, fontSize = FutureTypography.label, fontWeight = FontWeight.Bold)
            Text("אימונים", color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
        }

        LazyRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(DIFFICULTIES) { difficulty ->
                val selected = difficulty == selectedDifficulty
                FocusableItem(onClick = { selectedDifficulty = difficulty }, theme = theme) { isFocused ->
                    Box(
                        modifier = Modifier
                            .background(
                                if (selected) theme.readableAccentColor else theme.surfaceColor,
                                FutureShapes.pill,
                            )
                            .padding(horizontal = 18.dp, vertical = 9.dp),
                    ) {
                        Text(
                            difficulty,
                            color = if (selected) theme.backgroundColor else theme.textColor.copy(alpha = 0.7f),
                            fontSize = FutureTypography.summary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            // ריפוד עליון קטן - בלעדיו הפריט הראשון לא מצטייר בקומפוזיציה
            // הראשונה תחת enableEdgeToEdge (ר' ההסבר המלא ב-HomeScreen.kt).
            contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                FocusableItem(
                    onClick = onOpenBuilder,
                    theme = theme,
                    modifier = Modifier.fillMaxWidth(),
                    focusRequester = firstRowFocusRequester,
                ) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isFocused) theme.focusFillChipColor else theme.idleFieldColor, FutureShapes.pill)
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("אימון חדש", color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                QuickActionCard(
                    icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                    title = "ריצה חופשית",
                    subtitle = "מעקב GPS חי - מרחק וקצב",
                    theme = theme,
                    onClick = onOpenRun,
                )
            }

            item {
                QuickActionCard(
                    icon = Icons.Rounded.GridView,
                    title = "כל סוגי הפעילות",
                    subtitle = "כל סוגי הפעילות, כמו בשעון חכם",
                    theme = theme,
                    onClick = onOpenActivityTypes,
                )
            }

            items(filteredWorkouts.size, key = { index -> filteredWorkouts[index].id }) { index ->
                val workout = filteredWorkouts[index]
                WorkoutCard(
                    workout = workout,
                    weightKg = weightKg,
                    theme = theme,
                    onClick = { onOpenWorkout(workout.id) },
                    onDelete = if (workout.isCustom) ({ pendingDelete = workout }) else null,
                )
            }
        }
    }
}

@Composable
private fun QuickActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, theme: FutureTheme, onClick: () -> Unit) {
    FocusableItem(onClick = onClick, theme = theme, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.surfaceColor, FutureShapes.xl)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(52.dp).background(theme.idleFieldColor, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(subtitle, color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label, maxLines = 1)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f))
        }
    }
}

@Composable
private fun WorkoutCard(workout: Workout, weightKg: Int, theme: FutureTheme, onClick: () -> Unit, onDelete: (() -> Unit)?) {
    val calories = remember(workout, weightKg) { WorkoutStore.estimateCalories(workout.met, weightKg, workout.durationMin) }
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        FocusableItem(onClick = onClick, theme = theme, modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surfaceColor, FutureShapes.xl)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(52.dp).background(theme.idleFieldColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(workout.name, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(workout.difficulty, color = theme.sectionHeaderColor, fontSize = FutureTypography.label, fontWeight = FontWeight.SemiBold)
                        Text(" · ", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.label)
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(13.dp))
                        Text(" ${workout.durationMin} דק׳", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
                        Text(" · ", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.label)
                        Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = theme.dangerColor, modifier = Modifier.size(13.dp))
                        Text(" $calories קק״ל", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
                    }
                    if (workout.isCustom) {
                        Text("מותאם אישית", color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.caption, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Box(
                    modifier = Modifier.size(36.dp).background(theme.readableAccentColor, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = theme.backgroundColor, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (onDelete != null) {
            Spacer(Modifier.width(8.dp))
            FocusableItem(onClick = onDelete, theme = theme, modifier = Modifier.size(44.dp)) { isFocused ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(theme.textColor.copy(alpha = if (isFocused) 0.16f else 0.06f), FutureShapes.md),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "מחק אימון", tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
