package com.future.fitness.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.ConfirmDialog
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
import androidx.compose.ui.text.style.TextOverflow
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

private fun restOverBeep() {
    runCatching {
        val tone = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 90)
        // F38: שחרור גם כשההשמעה נכשלת - אחרת ה-ToneGenerator נשאר פתוח.
        runCatching {
            tone.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 400)
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({ tone.release() }, 800)
        }.onFailure { tone.release() }
    }
}

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
    val restFocusRequester = remember { FocusRequester() }
    // יציאה באמצע אימון (אחרי שהתחיל) דורשת אישור - אחרת כל הסטים והזמן הולכים לאיבוד.
    var confirmExit by remember { mutableStateOf(false) }
    androidx.activity.compose.BackHandler(enabled = state.elapsedSec > 0) { confirmExit = true }
    LaunchedEffect(state.resting) {
        runCatching { if (state.resting) restFocusRequester.requestFocus() else actionButtonFocusRequester.requestFocus() }
    }

    LaunchedEffect(state) {
        // F26: הזמן נמדד לפי השעון ולא לפי מספר הטיקים - delay מתארך תחת עומס.
        var lastTick = android.os.SystemClock.elapsedRealtime()
        var carryMs = 0L
        while (true) {
            delay(1000)
            val now = android.os.SystemClock.elapsedRealtime()
            val passedMs = now - lastTick
            lastTick = now
            // המנוחה היא חלק מהאימון: קודם השעון עצר בכל מנוחה, ומשך האימון
            // והקלוריות שנשמרו יצאו קצרים ממה שבאמת היה.
            if (state.running) {
                carryMs += passedMs
                val seconds = (carryMs / 1000).toInt()
                state.elapsedSec += seconds
                carryMs -= seconds * 1000L
            } else {
                carryMs = 0L
            }
            if (state.resting) {
                if (state.restRemaining <= 1) {
                    state.resting = false
                    state.exerciseIndex = state.pendingExerciseIndex
                    state.setIndex = state.pendingSetIndex
                    // בזמן מנוחה לא מסתכלים על המסך - צפצוף קצר כשצריך לחזור לסט
                    restOverBeep()
                } else {
                    state.restRemaining -= 1
                }
            }
            // דופק נדגם רק בזמן אימון פעיל - לא בהשהיה, שם הוא יורד ומוריד את הממוצע
            if (state.running) heartRateMonitor.currentBpm?.let { state.sampleHr(it) }
        }
    }

    val exercise = workout.exercises[state.exerciseIndex]
    val totalSetsInWorkout = workout.exercises.sumOf { it.sets }
    // F48: נספרים הסטים שסומנו כבוצעו (ולא לפי מיקום הסט הבא), כך שבמנוחה
    // הסרגל כבר כולל את הסט שהושלם.
    val doneSets = state.totalCompletedSets()
    val overallProgress = doneSets.toFloat() / totalSetsInWorkout.toFloat()

    if (confirmExit) {
        ConfirmDialog(
            message = "לצאת מהאימון? הנתונים שנצברו לא יישמרו.",
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { confirmExit = false },
            onConfirm = { confirmExit = false; onBack() },
            cancelLabel = "המשך",
            confirmLabel = "צא",
        )
    }
    // המסך נבנה לגובה 480dp: קודם הזמן, שני אריחי דופק/קלוריות, האווטאר
    // הגדול וכרטיס התרגיל הצטברו לכ-550dp, וכפתורי "סיימתי סט"/השהה נדחקו
    // מתחת לקצה המסך. עכשיו הזמן והמדדים חולקים שורה אחת, והכרטיס קומפקטי.
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = workout.name, theme = theme, onBack = { if (state.elapsedSec > 0) confirmExit = true else onBack() })

        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        formatElapsed(state.elapsedSec),
                        color = theme.textColor,
                        fontSize = FutureTypography.display,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FutureTypography.monoFamily,
                    )
                    Text(if (state.running) "זמן אימון" else "מושהה", color = theme.mutedTextColor, fontSize = FutureTypography.label)
                }
                // F42: אותו עיגול דקות כמו בשמירה, ובלי מינימום דקה - קודם 6 קק״ל
                // הופיעו כבר ב-00:03.
                val liveMinutes = Math.round(state.elapsedSec / 60f)
                val liveCalories = remember(liveMinutes, workout.met, weightKg) {
                    WorkoutStore.estimateCalories(workout.met, weightKg, liveMinutes)
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MetricChip(FutureIcons.Favorite, if (isHrConnected && liveBpm != null) "$liveBpm" else "--", "BPM", theme.dangerColor, theme)
                    MetricChip(FutureIcons.LocalFireDepartment, "$liveCalories", "קק״ל", theme.textColor, theme)
                }
            }

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                Text(
                    "תרגיל ${state.exerciseIndex + 1} מתוך ${workout.exercises.size}",
                    color = theme.mutedTextColor,
                    fontSize = FutureTypography.label,
                    modifier = Modifier.weight(1f),
                )
                Text("${(overallProgress * 100).toInt()}%", color = theme.mutedTextColor, fontSize = FutureTypography.label)
            }
            FutureProgressBar(progress = overallProgress, theme = theme)
            Spacer(Modifier.height(12.dp))

            if (state.resting) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.elevatedSurfaceColor, FutureShapes.xl)
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("מנוחה", color = theme.textColor, fontSize = FutureTypography.summary, fontWeight = FontWeight.Bold)
                    Text(state.restRemaining.toString(), color = theme.textColor, fontSize = FutureTypography.hero, fontWeight = FontWeight.Bold)
                    val next = workout.exercises[state.pendingExerciseIndex]
                    Text("הבא: ${next.name} · סט ${state.pendingSetIndex + 1}", color = theme.mutedTextColor, fontSize = FutureTypography.summary)
                    Spacer(Modifier.height(10.dp))
                    FutureButton("דלג על המנוחה", theme, {
                        state.resting = false
                        state.exerciseIndex = state.pendingExerciseIndex
                        state.setIndex = state.pendingSetIndex
                    }, variant = FutureButtonVariant.Secondary, focusRequester = restFocusRequester)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.surfaceColor, FutureShapes.xl)
                        .padding(vertical = 14.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(exercise.name, color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "סט ${state.setIndex + 1} מתוך ${exercise.sets} · ${exercise.repsLabel}",
                        color = theme.mutedTextColor,
                        fontSize = FutureTypography.summary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in 0 until exercise.sets) {
                            val done = state.isSetDone(state.exerciseIndex, i)
                            val current = i == state.setIndex && !done
                            val borderColor = if (done || current) theme.readableAccentColor else theme.subtleTextColor
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(if (done) theme.readableAccentColor else Color.Transparent, CircleShape)
                                    .border(1.5.dp, borderColor, CircleShape)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            if (!state.resting) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FocusableItem(
                        onClick = { state.running = !state.running },
                        theme = theme,
                        modifier = Modifier.size(52.dp),
                        cornerRadius = FutureShapes.radiusLg,
                    ) { _ ->
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                if (state.running) FutureIcons.Pause else FutureIcons.PlayArrow,
                                contentDescription = if (state.running) "השהה" else "המשך",
                                tint = theme.textColor,
                            )
                        }
                    }
                    FutureButton(
                        text = "סיימתי סט",
                        theme = theme,
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
                        modifier = Modifier.weight(1f),
                        fillMaxWidth = true,
                        focusRequester = actionButtonFocusRequester,
                    )
                }
            }
        }
    }
}

/** מדד חי קטן (דופק, קלוריות) ליד הזמן. */
@Composable
private fun MetricChip(icon: ImageVector, value: String, unit: String, color: Color, theme: FutureTheme) {
    Row(
        modifier = Modifier.background(theme.surfaceColor, FutureShapes.pill).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(value, color = theme.textColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
        Text(" $unit", color = theme.mutedTextColor, fontSize = FutureTypography.caption)
    }
}
