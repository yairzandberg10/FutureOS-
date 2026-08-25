package com.future.fitness.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.Workout
import com.future.fitness.ui.components.FocusableItem
import com.future.fitness.ui.components.IconListRow
import com.future.fitness.ui.components.ScreenTopBar
import com.future.fitness.ui.theme.FutureTheme

@Composable
fun WorkoutsScreen(
    workouts: List<Workout>,
    theme: FutureTheme,
    onBack: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    onOpenBuilder: () -> Unit,
    onDeleteCustom: (String) -> Unit,
) {
    val firstRowFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRowFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "אימונים", theme = theme, onBack = onBack)
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                FocusableItem(onClick = onOpenBuilder, theme = theme, modifier = Modifier.fillMaxWidth()) { isFocused ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.accentColor.copy(alpha = if (isFocused) 0.2f else 0.12f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("אימון חדש", color = theme.accentColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(workouts.size) { index ->
                val workout = workouts[index]
                val subtitle = "${workout.exercises.size} תרגילים · ${workout.durationMin} דק׳ · ${workout.difficulty}" +
                    if (workout.isCustom) " · מותאם אישית" else ""

                if (workout.isCustom) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconListRow(
                            icon = Icons.Rounded.FitnessCenter,
                            title = workout.name,
                            subtitle = subtitle,
                            theme = theme,
                            onClick = { onOpenWorkout(workout.id) },
                            showChevron = true,
                            modifier = Modifier.weight(1f),
                            focusRequester = if (index == 0) firstRowFocusRequester else null,
                        )
                        Spacer(Modifier.width(8.dp))
                        FocusableItem(onClick = { onDeleteCustom(workout.id) }, theme = theme, modifier = Modifier.size(44.dp)) { isFocused ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(theme.textColor.copy(alpha = if (isFocused) 0.16f else 0.06f), RoundedCornerShape(14.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "מחק אימון", tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                } else {
                    IconListRow(
                        icon = Icons.Rounded.FitnessCenter,
                        title = workout.name,
                        subtitle = subtitle,
                        theme = theme,
                        onClick = { onOpenWorkout(workout.id) },
                        showChevron = true,
                        focusRequester = if (index == 0) firstRowFocusRequester else null,
                    )
                }
            }
        }
    }
}
