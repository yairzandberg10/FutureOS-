package com.future.fitness.ui.screens

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.AvatarListSize
import com.future.sharednav.components.FutureAvatar
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.future.sharednav.nav.digitForKey
import kotlinx.coroutines.launch
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.fitness.data.DailyStats
import com.future.fitness.data.Workout
import com.future.fitness.data.WorkoutHistoryEntry
import com.future.fitness.ui.components.ActivityRings
import com.future.fitness.ui.components.IconListRow
import com.future.fitness.ui.components.RingSpec
import com.future.fitness.ui.components.StatTile

import com.future.sharednav.theme.FutureTheme
import java.util.Calendar

/** יעד דקות פעילות ליום, נגזר מהנחיית ארגון הבריאות העולמי המצוטטת כבר
 * במסך "בריאות" (150 דקות/שבוע ÷ 7) - לא מספר מומצא, וגם לא "יעד אישי"
 * שלא קיים במודל הנתונים (אין שדה יעד בפרופיל המשתמש). */
private const val WHO_DAILY_ACTIVE_MINUTES_TARGET = 21

private data class MenuItem(val digit: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val subtitle: String, val onClick: () -> Unit)

@Composable
fun HomeScreen(
    theme: FutureTheme,
    stats: DailyStats,
    history: List<WorkoutHistoryEntry>,
    nextWorkout: Workout,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHealth: () -> Unit,
    onOpenNextWorkoutDetail: () -> Unit,
) {
    val items = listOf(
        MenuItem("1", FutureIcons.History, "היסטוריה", "אימונים קודמים", onOpenHistory),
        MenuItem("2", FutureIcons.FavoriteBorder, "בריאות", "הסברים ואזורי דופק", onOpenHealth),
        MenuItem("3", FutureIcons.Settings, "הגדרות", "עיצוב, פרופיל ושעון חכם", onOpenSettings),
    )

    val weekDays = remember(history) { buildWeekDots(history) }
    val minutesFraction = (stats.activeMinutesToday.toFloat() / WHO_DAILY_ACTIVE_MINUTES_TARGET).coerceIn(0f, 1f)

    // הפוקוס+onKeyEvent יושבים על ה-LazyColumn עצמו (לא על ה-Column החיצוני
    // שעוטף הכל) - זה גם פותר תקלה פונקציונלית וגם UX: קודם הפוקוס היה על
    // ה-Column החיצוני (כדי שקיצורי הספרות יעבדו בלי תלות בעוגן הפוקוס
    // הגנרי ב-FitnessNavHost, שהוא "אח" של המסך ולא אב-קדמון שלו), אבל אז
    // חיצי מעלה/מטה לא גללו בכלל (חיפוש הפוקוס הדיפולטיבי של Compose מ-
    // container גדול שממוקד ישירות לא זהה לגלילה חלקה). כאן, בדיוק כמו
    // ב-ProgressScreen, DirectionUp/Down מטפלים בגלילה מפורשת עם
    // animateScrollBy, וספרות עדיין מפעילות קיצורי-דרך - הכל על אותו רכיב.
    // קודם ה-LazyColumn כולו היה יעד הפוקוס ובלע את חיצי מעלה/מטה לגלילה -
    // כך שהכרטיס "האימון הבא" ושורות התפריט לא קיבלו פוקוס אף פעם. עכשיו
    // הפוקוס עובר בין הרכיבים עצמם (הרשימה גוללת אליהם), הספרות 1-3 עדיין
    // קיצורי דרך (עולות מהרכיב הממוקד אל ה-Column), והכרטיסים העליונים -
    // מידע בלבד - נחשפים כשחוזרים לרכיב הראשון.
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

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
            greeting(),
            color = theme.textColor,
            fontSize = FutureTypography.headline,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        )
        Text(
            "מוכנים לשבור את השיא היומי?",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = FutureTypography.summary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            // ריפוד עליון קטן חשוב פונקציונלית ולא רק ויזואלית: בלעדיו, תוכן
            // ה-item הראשון לא מצטייר כלל בקומפוזיציה הראשונה של המסך (נראה
            // כתקלת תזמון עם עיבוד ה-WindowInsets תחת enableEdgeToEdge - כל
            // תוכן שממוקם ממש בפיקסלים העליונים של אזור הגלילה "נבלע"). קצת
            // ריפוד למעלה מבטיח שהתוכן האמיתי לעולם לא יושב שם.
            contentPadding = PaddingValues(top = 4.dp),
        ) {
            item {
                val doneCount = weekDays.count { it.done }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .background(theme.surfaceColor, FutureShapes.xl)
                        .padding(vertical = 14.dp, horizontal = 14.dp),
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("רצף אימונים שבועי", color = theme.textColor.copy(alpha = 0.6f), fontSize = FutureTypography.label, fontWeight = FontWeight.Bold)
                        Text("$doneCount מתוך 7 ימים", color = theme.sectionHeaderColor, fontSize = FutureTypography.label, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        weekDays.forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(day.label, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                                Spacer(Modifier.height(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .background(
                                            when {
                                                day.isToday -> theme.focusFillChipColor
                                                day.done -> theme.idleFieldColor
                                                else -> theme.textColor.copy(alpha = 0.06f)
                                            },
                                            CircleShape,
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (day.done) {
                                        Icon(FutureIcons.Check, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).background(theme.surfaceColor, FutureShapes.xl).padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ActivityRings(
                        rings = listOf(RingSpec(minutesFraction, theme.readableAccentColor)),
                        trackColor = theme.textColor.copy(alpha = 0.08f),
                        size = 96.dp,
                        strokeWidth = 9.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${stats.activeMinutesToday}", color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.ExtraBold)
                            Text("דק׳ מ-$WHO_DAILY_ACTIVE_MINUTES_TARGET", color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.caption)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        MiniStatRow(FutureIcons.LocalFireDepartment, "${stats.caloriesToday} קק״ל היום", theme.dangerColor, theme)
                        MiniStatRow(FutureIcons.LocalFireDepartment, "${stats.streakDays} ימי רצף", theme.readableAccentColor, theme)
                        Text(
                            "יעד יומי מומלץ (WHO): $WHO_DAILY_ACTIVE_MINUTES_TARGET דק׳ פעילות",
                            color = theme.mutedTextColor,
                            fontSize = FutureTypography.caption,
                        )
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            item {
                com.future.fitness.ui.components.FocusableItem(
                    onClick = onOpenNextWorkoutDetail,
                    theme = theme,
                    focusRequester = focusRequester,
                    cornerRadius = FutureShapes.radiusXl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        // הרכיב הממוקד הראשון - כשהוא מקבל פוקוס גם כרטיסי המידע שמעליו נראים.
                        .onFocusChanged { if (it.isFocused) scope.launch { listState.animateScrollToItem(0) } },
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.surfaceColor, FutureShapes.xl)
                            .padding(18.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FutureAvatar(theme = theme, icon = FutureIcons.FitnessCenter, size = AvatarListSize)
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("האימון המומלץ הבא", color = theme.sectionHeaderColor, fontSize = FutureTypography.caption, fontWeight = FontWeight.Bold)
                                Text(nextWorkout.name, color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.Bold)
                                Text(
                                    "${nextWorkout.exercises.size} תרגילים · ${nextWorkout.durationMin} דקות · ${nextWorkout.difficulty}",
                                    color = theme.textColor.copy(alpha = 0.6f),
                                    fontSize = FutureTypography.label,
                                )
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(theme.readableAccentColor, FutureShapes.pill),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(FutureIcons.PlayCircle, contentDescription = null, tint = theme.backgroundColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("פתח והתחל אימון", color = theme.backgroundColor, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold)
                        }
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
                )
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

@Composable
private fun MiniStatRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: androidx.compose.ui.graphics.Color, theme: FutureTheme) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = theme.textColor, fontSize = FutureTypography.summary, fontWeight = FontWeight.Medium)
    }
}

private data class DayDot(val label: String, val done: Boolean, val isToday: Boolean)

private fun greeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 5 -> "לילה טוב"
        hour < 12 -> "בוקר טוב"
        hour < 18 -> "צהריים טובים"
        else -> "ערב טוב"
    }
}

private fun buildWeekDots(history: List<WorkoutHistoryEntry>): List<DayDot> {
    val labels = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
    val today = Calendar.getInstance()
    val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)

    val doneByDayOfWeek = BooleanArray(8)
    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }.apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    history.forEach { entry ->
        val c = Calendar.getInstance().apply { timeInMillis = entry.dateMillis }
        if (!c.before(sevenDaysAgo)) doneByDayOfWeek[c.get(Calendar.DAY_OF_WEEK)] = true
    }

    return (0..6).map { offset ->
        val dow = ((todayDayOfWeek - 1 - offset + 7) % 7) + 1
        DayDot(label = labels[dow - 1], done = doneByDayOfWeek[dow], isToday = offset == 0)
    }.reversed()
}
