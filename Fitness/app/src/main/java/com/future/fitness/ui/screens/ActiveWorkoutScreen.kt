package com.future.fitness.ui.screens
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureAvatar
import com.future.sharednav.components.FutureProgressBar
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
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.bluetooth.HeartRateMonitor
import com.future.fitness.bluetooth.HrConnectionState
import com.future.fitness.data.Workout
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.formatElapsed
import androidx.compose.ui.graphics.Color
import com.future.sharednav.theme.FutureTheme
import kotlinx.coroutines.delay

private const val REST_SECONDS = 15

private class ActiveWorkoutState(workout: Workout) {
    var exerciseIndex by mutableIntStateOf(0)
    var setIndex by mutableIntStateOf(0)
    var elapsedSec by mutableIntStateOf(0)
    var running by mutableStateOf(true)
    var resting by mutableStateOf(false)
    var restRemaining by mutableIntStateOf(0)
    var pendingExerciseIndex by mutableIntStateOf(0)
    var pendingSetIndex by mutableIntStateOf(0)
    var maxHr by mutableIntStateOf(0)
    var hrSum by mutableIntStateOf(0)
    var hrSamples by mutableIntStateOf(0)

    // רשימה בלתי-ניתנת-לשינוי בתוך mutableStateOf, מוחלפת כולה בכל עדכון -
    // כך ש-Compose עוקב אחרי השינוי כראוי (MutableList גולמי לא היה מפעיל
    // רה-קומפוזיציה).
    var completedSets by mutableStateOf(workout.exercises.map { e -> List(e.sets) { false } })
        private set

    fun isSetDone(exercise: Int, set: Int): Boolean = completedSets[exercise][set]

    fun markSetDone(exercise: Int, set: Int) {
        completedSets = completedSets.mapIndexed { i, row ->
            if (i == exercise) row.mapIndexed { j, v -> if (j == set) true else v } else row
        }
    }

    fun totalCompletedSets(): Int = completedSets.sumOf { row -> row.count { it } }

    fun sampleHr(bpm: Int) {
        if (bpm <= 0) return
        hrSum += bpm
        hrSamples += 1
        if (bpm > maxHr) maxHr = bpm
    }

    fun avgHr(): Int? = if (hrSamples > 0) hrSum / hrSamples else null
}

/** מסך האימון החי - טיימר כולל שרץ, מעבר בין תרגילים/סטים, וטיימר מנוחה
 * אמיתי בין סטים. כפתורי הפעולה עטופים ב-FocusableItem (במקום Button רגיל)
 * כדי שהפוקוס יהיה ברור ובניגוד גבוה - קריטי במכשיר הזה שהוא מקלדת/D-pad
 * בלבד בלי מסך מגע (ראו MainActivity.dispatchTouchEvent), אז המשתמש *חייב*
 * לראות בבירור איזה כפתור ממוקד עכשיו. סוגר ב-onFinish כשהסט האחרון של
 * התרגיל האחרון הושלם, עם קלוריות מדויקות (met*משקל*שעות) ודופק ממוצע/מקסימלי
 * אם שעון מחובר. */
@Composable
fun ActiveWorkoutScreen(
    workout: Workout,
    theme: FutureTheme,
    weightKg: Int,
    heartRateMonitor: HeartRateMonitor,
    onBack: () -> Unit,
    onFinish: (minutes: Int, calories: Int, totalSets: Int, avgHr: Int?, maxHr: Int?) -> Unit,
) {
    val state = remember(workout) { ActiveWorkoutState(workout) }
    val isHrConnected = heartRateMonitor.state == HrConnectionState.CONNECTED
    val liveBpm = heartRateMonitor.currentBpm
    val actionButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(state.resting) {
        if (!state.resting) actionButtonFocusRequester.requestFocus()
    }

    LaunchedEffect(state) {
        while (true) {
            delay(1000)
            if (state.resting) {
                if (state.restRemaining <= 1) {
                    state.resting = false
                    state.exerciseIndex = state.pendingExerciseIndex
                    state.setIndex = state.pendingSetIndex
                } else {
                    state.restRemaining -= 1
                }
            } else if (state.running) {
                state.elapsedSec += 1
            }
            heartRateMonitor.currentBpm?.let { state.sampleHr(it) }
        }
    }

    val exercise = workout.exercises[state.exerciseIndex]
    val totalSetsInWorkout = workout.exercises.sumOf { it.sets }
    val doneSets = (0 until state.exerciseIndex).sumOf { workout.exercises[it].sets } + state.setIndex
    val overallProgress = doneSets.toFloat() / totalSetsInWorkout.toFloat()

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = workout.name, theme = theme, onBack = onBack)

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                formatElapsed(state.elapsedSec),
                color = theme.textColor,
                fontSize = FutureTypography.hero,
                fontWeight = FontWeight.Bold,
            )
            Text("זמן אימון כולל", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label, modifier = Modifier.padding(bottom = 14.dp))

            if (!state.resting) {
                val liveCalories = remember(state.elapsedSec, workout.met, weightKg) {
                    WorkoutStore.estimateCalories(workout.met, weightKg, maxOf(1, state.elapsedSec / 60))
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    BioTile(
                        icon = Icons.Rounded.Favorite,
                        value = if (isHrConnected && liveBpm != null) "$liveBpm" else "--",
                        unit = "BPM",
                        color = theme.dangerColor,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                    )
                    BioTile(
                        icon = Icons.Rounded.LocalFireDepartment,
                        value = "$liveCalories",
                        unit = "קק״ל",
                        color = theme.textColor,
                        theme = theme,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    "תרגיל ${state.exerciseIndex + 1} מתוך ${workout.exercises.size}",
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = FutureTypography.label,
                    modifier = Modifier.weight(1f),
                )
                Text("${(overallProgress * 100).toInt()}%", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label)
            }
            FutureProgressBar(progress = overallProgress, theme = theme)
            Spacer(Modifier.height(18.dp))

            if (state.resting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.elevatedSurfaceColor, FutureShapes.xl)
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("מנוחה", color = theme.textColor, fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold)
                    Text(state.restRemaining.toString(), color = theme.textColor, fontSize = FutureTypography.hero, fontWeight = FontWeight.Bold)
                    FutureButton("דלג על המנוחה", theme, {
                        state.resting = false
                        state.exerciseIndex = state.pendingExerciseIndex
                        state.setIndex = state.pendingSetIndex
                    }, variant = FutureButtonVariant.Quiet)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceColor, FutureShapes.xl)
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FutureAvatar(theme = theme, icon = FutureIcons.FitnessCenter, size = 56.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(exercise.name, color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold)
                    Text(
                        "סט ${state.setIndex + 1} מתוך ${exercise.sets} · ${exercise.repsLabel}",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = FutureTypography.summary,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until exercise.sets) {
                            val done = state.isSetDone(state.exerciseIndex, i)
                            val current = i == state.setIndex && !done
                            val borderColor = if (done || current) theme.readableAccentColor else theme.subtleTextColor
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(if (done) theme.readableAccentColor else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                                    .border(1.5.dp, borderColor, CircleShape)
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableItem(
                        onClick = { state.running = !state.running },
                        theme = theme,
                        modifier = Modifier.size(56.dp),
                        focusRequester = actionButtonFocusRequester,
                        cornerRadius = FutureShapes.radiusLg,
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isFocused) theme.focusFillChipColor else theme.textColor.copy(alpha = 0.08f), FutureShapes.lg),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (state.running) FutureIcons.Pause else FutureIcons.PlayArrow,
                                contentDescription = if (state.running) "השהה" else "המשך",
                                tint = theme.textColor,
                            )
                        }
                    }
                    FocusableItem(
                        onClick = {
                            state.markSetDone(state.exerciseIndex, state.setIndex)

                            val isLastSet = state.setIndex == exercise.sets - 1
                            val isLastExercise = state.exerciseIndex == workout.exercises.lastIndex

                            if (isLastSet && isLastExercise) {
                                val minutes = maxOf(1, Math.round(state.elapsedSec / 60f))
                                val calories = WorkoutStore.estimateCalories(workout.met, weightKg, minutes)
                                onFinish(minutes, calories, state.totalCompletedSets(), state.avgHr(), state.maxHr.takeIf { it > 0 })
                            } else {
                                state.pendingExerciseIndex = if (isLastSet) state.exerciseIndex + 1 else state.exerciseIndex
                                state.pendingSetIndex = if (isLastSet) 0 else state.setIndex + 1
                                state.restRemaining = REST_SECONDS
                                state.resting = true
                            }
                        },
                        theme = theme,
                        modifier = Modifier.weight(1f).height(56.dp),
                        cornerRadius = FutureShapes.radiusLg,
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(theme.readableAccentColor, FutureShapes.lg)
                                .then(if (isFocused) Modifier.border(3.dp, theme.textColor, FutureShapes.lg) else Modifier),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(FutureIcons.Check, contentDescription = null, tint = theme.backgroundColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("סיימתי סט", color = theme.backgroundColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BioTile(icon: ImageVector, value: String, unit: String, color: Color, theme: FutureTheme, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(theme.surfaceColor, FutureShapes.lg)
            .padding(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(value, color = theme.textColor, fontSize = FutureTypography.headline, fontWeight = FontWeight.ExtraBold)
            Text(" $unit", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
        }
    }
}
