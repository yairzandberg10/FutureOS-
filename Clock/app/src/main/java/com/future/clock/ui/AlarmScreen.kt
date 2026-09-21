package com.future.clock.ui
import com.future.sharednav.theme.onAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDayChip
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSettingItem
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.theme.subtleTextColor
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.future.clock.logic.Alarm
import com.future.clock.logic.AlarmLogic
import com.future.sharednav.theme.FutureTheme

@Composable
fun AlarmScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    val alarms = remember { mutableStateListOf<Alarm>() }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    // מחיקה היא בלתי הפיכה, ומקש OK הוא מקש בודד - בלי אישור, לחיצה בשוגג
    // על שורת השעון מוחקת אותו.
    var pendingDelete by remember { mutableStateOf<Alarm?>(null) }
    
    LaunchedEffect(Unit) {
        alarms.addAll(AlarmLogic.getAlarms(context))
    }

    fun updateAlarms() {
        AlarmLogic.saveAlarms(context, alarms.toList())
    }

    pendingDelete?.let { alarm ->
        ConfirmDialog(
            message = "למחוק את השעון %02d:%02d?".format(alarm.hour, alarm.minute),
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { pendingDelete = null },
            onConfirm = {
                alarms.remove(alarm)
                updateAlarms()
                pendingDelete = null
            },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                ToolsHeader(
                    title = "שעונים מעוררים",
                    theme = theme,
                    onBack = if (editingAlarm == null) onBack else null,
                    trailing = {
                        if (editingAlarm == null) {
                            ToolsIconButton(Icons.Rounded.Add, "הוסף שעון", theme) {
                                val newId = (alarms.maxOfOrNull { it.id } ?: 0) + 1
                                val newAlarm = Alarm(newId, 7, 0, emptySet())
                                alarms.add(newAlarm)
                                updateAlarms()
                                editingAlarm = newAlarm
                            }
                        }
                    }
                )

                if (editingAlarm != null) {
                    TimePickerOverlay(editingAlarm!!, theme, 
                        onSave = { updated ->
                            val index = alarms.indexOfFirst { it.id == updated.id }
                            if (index != -1) {
                                alarms[index] = updated
                                updateAlarms()
                            }
                            editingAlarm = null
                        },
                        onCancel = { editingAlarm = null }
                    )
                } else {
                    if (alarms.isEmpty()) {
                        com.future.sharednav.components.EmptyState(
                            icon = Icons.Rounded.AccessTime,
                            title = "אין שעונים מעוררים",
                            subtitle = "נווטו לכפתור ההוספה למעלה ולחצו OK כדי להוסיף אחד",
                            textColor = theme.textColor,
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                        )
                    } else {
                        // רשימת המעוררים היא כרטיס אחד של שורות שקופות
                        // המופרדות בקו שיער, ולא שורות נפרדות עם מרווח
                        // ביניהן (ui_kits/clock).
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                        ) {
                            FutureCard(theme = theme) {
                                alarms.forEachIndexed { index, alarm ->
                                    if (index > 0) FutureDivider(theme = theme)
                                    AlarmRow(alarm, theme,
                                        onToggle = { enabled ->
                                            val at = alarms.indexOf(alarm)
                                            if (at != -1) {
                                                alarms[at] = alarm.copy(isEnabled = enabled)
                                                updateAlarms()
                                            }
                                        },
                                        onDelete = { pendingDelete = alarm },
                                        onClick = { editingAlarm = alarm }
                                    )
                                }
                            }
                            Text(
                                "אישור פותח את בורר השעה. מקש ההוספה למעלה מוסיף מעורר חדש.",
                                color = theme.subtleTextColor,
                                fontSize = FutureTypography.summary,
                                modifier = Modifier.padding(
                                    horizontal = FutureDimens.spacingXl,
                                    vertical = 20.dp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun recurrenceSummary(alarm: Alarm): String {
    val state = if (alarm.isEnabled) "מופעל" else "כבוי"
    if (alarm.days.isEmpty()) return "$state · חד-פעמית"
    val days = alarm.days.sorted().joinToString(",") { day ->
        DAY_LABELS.firstOrNull { it.first == day }?.second ?: "?"
    }
    return "$state · $days"
}

/**
 * שורת מעורר. בעיצוב היא [FutureSettingItem] עם מתג בלבד בסוף השורה;
 * כפתור המחיקה נשאר כאן כתוספת, כי הוא הדרך היחידה למחוק מעורר - בעיצוב
 * המחיקה יושבת בתפריט האפשרויות, שאינו קיים במסך הזה.
 */
@Composable
fun AlarmRow(alarm: Alarm, theme: FutureTheme, onToggle: (Boolean) -> Unit, onDelete: () -> Unit, onClick: () -> Unit) {
    FutureSettingItem(
        title = "%02d:%02d".format(alarm.hour, alarm.minute),
        summary = recurrenceSummary(alarm),
        icon = Icons.Rounded.AccessTime,
        theme = theme,
        onClick = onClick,
        trailing = {
            FutureSwitch(checked = alarm.isEnabled, theme = theme)
            Spacer(modifier = Modifier.width(FutureDimens.spacingSm))
            ToolsIconButton(Icons.Rounded.Delete, "מחק", theme, tint = theme.dangerColor, onClick = onDelete)
        },
    )
}

// Calendar.DAY_OF_WEEK: 1=ראשון...7=שבת. בלי בורר הימים הזה, alarm.days היה
// תמיד ריק בפועל - אף מסך בעולם לא איפשר להגדיר אזעקה חוזרת, למרות שהשדה
// עצמו קיים ב-data class ומטופל נכון עכשיו ב-AlarmLogic.nextTriggerMillis.
private val DAY_LABELS = listOf(1 to "א", 2 to "ב", 3 to "ג", 4 to "ד", 5 to "ה", 6 to "ו", 7 to "ש")

@Composable
fun TimePickerOverlay(alarm: Alarm, theme: FutureTheme, onSave: (Alarm) -> Unit, onCancel: () -> Unit) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var days by remember { mutableStateOf(alarm.days) }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ערוך שעה", color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TimeUnitPicker("שעות", hour, 0..23, theme) { hour = it }
            Text(":", color = theme.textColor, fontSize = FutureTypography.hero, fontFamily = FutureTypography.monoFamily, modifier = Modifier.padding(horizontal = 16.dp))
            TimeUnitPicker("דקות", minute, 0..59, theme) { minute = it }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            if (days.isEmpty()) "חד-פעמית" else "חוזרת",
            color = theme.textColor.copy(alpha = 0.6f),
            fontSize = FutureTypography.summary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            DAY_LABELS.forEach { (dayValue, label) ->
                DayToggleChip(
                    label = label,
                    selected = dayValue in days,
                    theme = theme,
                    onToggle = {
                        days = if (dayValue in days) days - dayValue else days + dayValue
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Button(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.textColor.copy(alpha = 0.1f), contentColor = theme.textColor)
            ) {
                Text("ביטול")
            }
            Button(
                onClick = { onSave(alarm.copy(hour = hour, minute = minute, days = days, isEnabled = true)) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = theme.accentColor, contentColor = Color.Black)
            ) {
                Text("שמור")
            }
        }
    }
}

/**
 * עיגול של יום בשבוע. מאציל ל-[FutureDayChip] המשותף - העיצוב שהיה כאן
 * צבע את המצב הנבחר בהדגשה הגולמית, שנעלמת במצב בהיר עם הדגשה לבנה.
 */
@Composable
fun DayToggleChip(label: String, selected: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    FutureDayChip(text = label, theme = theme, selected = selected, onClick = onToggle)
}

@Composable
fun TimeUnitPicker(label: String, value: Int, range: IntRange, theme: FutureTheme, onValueChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        ToolsIconButton(Icons.Rounded.KeyboardArrowUp, "למעלה", theme) {
            onValueChange(if (value == range.last) range.first else value + 1)
        }
        Text(
            "%02d".format(value),
            color = theme.textColor,
            fontSize = FutureTypography.hero,
            fontWeight = FontWeight.Light,
            fontFamily = FutureTypography.monoFamily,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        ToolsIconButton(Icons.Rounded.KeyboardArrowDown, "למטה", theme) {
            onValueChange(if (value == range.first) range.last else value - 1)
        }
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = FutureTypography.label)
    }
}
