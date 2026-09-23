package com.future.fitness.ui.screens

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.WorkoutHistoryEntry
import com.future.fitness.ui.components.IconListRow
import com.future.fitness.ui.components.ScreenTopBar
import com.future.sharednav.theme.FutureTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<WorkoutHistoryEntry>,
    theme: FutureTheme,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopBar(title = "היסטוריה", theme = theme, onBack = onBack)

        if (history.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(FutureIcons.History, contentDescription = null, tint = theme.textColor.copy(alpha = 0.3f), modifier = Modifier.padding(bottom = 12.dp))
                Text("עדיין אין אימונים שהושלמו", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.body, fontWeight = FontWeight.Medium)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                // ריפוד עליון קטן - ר' ההסבר המלא ב-HomeScreen.kt (בלעדיו הפריט
                // הראשון לא מצטייר בקומפוזיציה הראשונה תחת enableEdgeToEdge).
                contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(history.size, key = { index -> history[index].dateMillis }) { index ->
                    val entry = history[index]
                    val parts = mutableListOf("${relativeDay(entry.dateMillis)}", "${entry.minutes} דק׳", "${entry.calories} קלוריות")
                    entry.distanceKm?.let { parts.add("%.2f ק״מ".format(it)) }
                    entry.avgHr?.let { parts.add("דופק ממוצע $it") }
                    IconListRow(
                        icon = FutureIcons.Check,
                        title = entry.name,
                        subtitle = parts.joinToString(" · "),
                        theme = theme,
                        // onClick={} ולא null בכוונה: מבטיח שהשורה תהיה focusable כדי
                        // שאפשר יהיה לגלול אליה עם D-pad (ראו IconListRow ב-Rows.kt) -
                        // בלי זה רשומות היסטוריה שגולשות מחוץ למסך היו בלתי-נגישות
                        // לחלוטין במכשיר בלי מסך מגע.
                        onClick = {},
                    )
                }
            }
        }
    }
}

private fun relativeDay(millis: Long): String {
    val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val entryDay = dayFormat.format(millis)
    val today = dayFormat.format(System.currentTimeMillis())
    if (entryDay == today) return "היום"

    val yesterdayCal = Calendar.getInstance()
    yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
    if (entryDay == dayFormat.format(yesterdayCal.time)) return "אתמול"

    val entryCal = Calendar.getInstance().apply { timeInMillis = millis }
    val todayCal = Calendar.getInstance()
    val diffDays = ((todayCal.timeInMillis - entryCal.timeInMillis) / (24 * 60 * 60 * 1000)).toInt()
    return "לפני $diffDays ימים"
}
