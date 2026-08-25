package com.future.fitness.ui.screens

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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import com.future.fitness.ui.theme.FutureTheme
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
                color = theme.accentColor,
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 18.dp)) {
                Text("זמן אימון כולל", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
                if (isHrConnected && liveBpm != null) {
                    Text(" · ", color = theme.textColor.copy(alpha = 0.3f), fontSize = 12.sp)
                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("$liveBpm", color = theme.accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    "תרגיל ${state.exerciseIndex + 1} מתוך ${workout.exercises.size}",
                    color = theme.textColor.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    modifier = Modifier.weight(1f),
                )
                Text("${(overallProgress * 100).toInt()}%", color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            LinearProgressIndicator(
                progress = { overallProgress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = theme.accentColor,
                trackColor = theme.textColor.copy(alpha = 0.1f),
            )
            Spacer(Modifier.height(18.dp))

            if (state.resting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.accentColor.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("מנוחה", color = theme.accentColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Text(state.restRemaining.toString(), color = theme.textColor, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                    TextButton(onClick = {
                        state.resting = false
                        state.exerciseIndex = state.pendingExerciseIndex
                        state.setIndex = state.pendingSetIndex
                    }) {
                        Text("דלג על המנוחה", color = theme.textColor.copy(alpha = 0.6f), fontSize = 13.sp)
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceColor, RoundedCornerShape(20.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier.size(56.dp).background(theme.accentColor.copy(alpha = 0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.FitnessCenter, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(exercise.name, color = theme.textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(
                        "סט ${state.setIndex + 1} מתוך ${exercise.sets} · ${exercise.repsLabel}",
                        color = theme.textColor.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until exercise.sets) {
                            val done = state.isSetDone(state.exerciseIndex, i)
                            val current = i == state.setIndex && !done
                            val borderColor = if (done || current) theme.accentColor else theme.textColor.copy(alpha = 0.35f)
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .background(if (done) theme.accentColor else androidx.compose.ui.graphics.Color.Transparent, CircleShape)
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
                    ) { isFocused ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (isFocused) theme.accentColor.copy(alpha = 0.22f) else theme.textColor.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
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
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(theme.accentColor, RoundedCornerShape(16.dp))
                                .then(if (isFocused) Modifier.border(3.dp, theme.textColor, RoundedCornerShape(16.dp)) else Modifier),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.Check, contentDescription = null, tint = theme.backgroundColor, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("סיימתי סט", color = theme.backgroundColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
