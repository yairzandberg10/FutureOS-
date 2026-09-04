package com.future.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.Workout
import com.future.fitness.data.WorkoutStore
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme

@Composable
fun WorkoutDetailScreen(
    workout: Workout,
    theme: FutureTheme,
    weightKg: Int,
    onBack: () -> Unit,
    onStart: () -> Unit,
) {
    val estimatedCalories = WorkoutStore.estimateCalories(workout.met, weightKg, workout.durationMin)
    val startButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { startButtonFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = workout.name, theme = theme, onBack = onBack)

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            MetaChip(Icons.Rounded.FitnessCenter, "${workout.exercises.size} תרגילים", theme)
            MetaChip(Icons.Rounded.Timer, "${workout.durationMin} דקות", theme)
            MetaChip(Icons.AutoMirrored.Rounded.TrendingUp, workout.difficulty, theme)
        }
        Text(
            "~$estimatedCalories קלוריות משוער",
            color = theme.textColor.copy(alpha = 0.45f),
            fontSize = 11.sp,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp),
        )

        Text(
            "תרגילים",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 8.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(workout.exercises.size) { index ->
                val exercise = workout.exercises[index]
                // עטוף ב-FocusableItem (onClick={} - השורה מידעית בלבד, לא פעולה)
                // כדי שהיא תהיה focusable ואפשר יהיה לגלול אליה עם D-pad; בלי זה
                // תרגילים שגולשים מחוץ למסך היו בלתי-נגישים במכשיר בלי מסך מגע.
                FocusableItem(onClick = {}, theme = theme, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.textColor.copy(alpha = 0.04f), RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.foundation.layout.Box(
                            modifier = Modifier.size(26.dp).background(theme.textColor.copy(alpha = 0.08f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text((index + 1).toString(), color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(exercise.name, color = theme.textColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                "${exercise.sets} סטים · ${exercise.repsLabel}",
                                color = theme.textColor.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(52.dp).focusRequester(startButtonFocusRequester),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = theme.backgroundColor),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("התחל אימון", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MetaChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, theme: FutureTheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = theme.textColor.copy(alpha = 0.6f), fontSize = 12.sp)
    }
}
