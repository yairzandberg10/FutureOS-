package com.future.fitness.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.elevatedSurfaceColor
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.focusFillChipColor
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.subtleTextColor
import com.future.sharednav.theme.sectionHeaderColor
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
import com.future.fitness.ui.components.FitnessTextField

import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.Exercise
import com.future.fitness.data.Workout
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.components.SegmentedControl
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme

private class DraftExercise(name: String = "", sets: String = "3", reps: String = "") {
    var name by mutableStateOf(name)
    var sets by mutableStateOf(sets)
    var reps by mutableStateOf(reps)
}

/** בניית אימון מותאם אישית - שם, קושי, ורשימת תרגילים דינמית (הוספה/הסרה).
 * met משוער אוטומטית לפי רמת הקושי הנבחרת (המשתמש לא צריך להבין מהו met). */
@Composable
fun WorkoutBuilderScreen(
    theme: FutureTheme,
    onBack: () -> Unit,
    onSave: (Workout) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var difficulty by remember { mutableStateOf("בינוני") }
    var durationMin by remember { mutableStateOf("30") }
    val exercises = remember { mutableStateListOf(DraftExercise()) }

    val canSave = name.isNotBlank() && exercises.any { it.name.isNotBlank() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אימון חדש", theme = theme, onBack = onBack)

        LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp).escapeTextFieldFocusTrap()) {
            item {
                FitnessTextField(
                        theme = theme,
                    value = name,
                    onValueChange = { name = it },
                    label = "שם האימון",
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp),
                    singleLine = true,
                )
            }

            item {
                Text("רמת קושי", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedControl(
                    options = listOf("קל", "בינוני", "קשה"),
                    selected = difficulty,
                    theme = theme,
                    onSelect = { difficulty = it },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            }

            item {
                FitnessTextField(
                        theme = theme,
                    value = durationMin,
                    onValueChange = { v -> if (v.length <= 3 && v.all { it.isDigit() }) durationMin = v },
                    label = "משך משוער (דקות)",
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 18.dp),
                    singleLine = true,
                )
            }

            item {
                Text("תרגילים", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
            }

            itemsIndexed(exercises) { index, ex ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .background(theme.surfaceColor, FutureShapes.lg)
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FitnessTextField(
                        theme = theme,
                            value = ex.name,
                            onValueChange = { ex.name = it },
                            label = "תרגיל ${index + 1}",
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        FocusableItem(
                            onClick = { if (exercises.size > 1) exercises.removeAt(index) },
                            theme = theme,
                            modifier = Modifier.size(40.dp),
                            cornerRadius = FutureShapes.radiusSm,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(theme.textColor.copy(alpha = 0.08f), FutureShapes.sm)
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(FutureIcons.Close, contentDescription = "הסר תרגיל", tint = theme.textColor.copy(alpha = if (exercises.size > 1) 0.7f else 0.2f))
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row {
                        FitnessTextField(
                        theme = theme,
                            value = ex.sets,
                            onValueChange = { v -> if (v.length <= 2 && v.all { it.isDigit() }) ex.sets = v },
                            label = "סטים",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(90.dp),
                            singleLine = true,
                        )
                        Spacer(Modifier.width(8.dp))
                        FitnessTextField(
                        theme = theme,
                            value = ex.reps,
                            onValueChange = { ex.reps = it },
                            label = "חזרות/משך",
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                        )
                    }
                }
            }

            item {
                FocusableItem(
                    onClick = { exercises.add(DraftExercise()) },
                    theme = theme,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.idleChipColor, FutureShapes.lg)
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(FutureIcons.Add, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("הוסף תרגיל", color = theme.textColor, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            FutureButton(
                "שמור אימון",
                theme,
                {
                    val met = when (difficulty) { "קל" -> 3.5; "קשה" -> 6.5; else -> 5.0 }
                    val workout = Workout(
                        id = "custom_${System.currentTimeMillis()}",
                        name = name.trim(),
                        difficulty = difficulty,
                        durationMin = durationMin.toIntOrNull() ?: 30,
                        met = met,
                        exercises = exercises.filter { it.name.isNotBlank() }.map { ex ->
                            Exercise(ex.name.trim(), ex.sets.toIntOrNull()?.coerceAtLeast(1) ?: 3, ex.reps.ifBlank { "10 חזרות" })
                        },
                        isCustom = true,
                    )
                    onSave(workout)
                },
                fillMaxWidth = true,
                enabled = canSave,
            )
        }
    }
}
