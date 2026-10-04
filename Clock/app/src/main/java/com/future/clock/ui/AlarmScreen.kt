package com.future.clock.ui
import androidx.compose.material.icons.rounded.AccessTime

import com.future.sharednav.icons.FutureIcons
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import com.future.sharednav.theme.textAlpha
import com.future.sharednav.theme.readableAccentColor
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import com.future.sharednav.components.TopBarIconButton as SharedTopBarIconButton
import com.future.sharednav.components.FutureButton
import com.future.sharednav.components.FutureButtonVariant
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import com.future.sharednav.theme.FutureShapes
import com.future.clock.logic.Alarm
import com.future.clock.logic.AlarmLogic
import com.future.sharednav.theme.FutureTheme

@Composable
fun AlarmScreen(theme: FutureTheme, onBack: () -> Unit, onOverlayChange: (Boolean) -> Unit = {}) {
    val context = LocalContext.current
    val alarms = remember { mutableStateListOf<Alarm>() }
    var editingAlarm by remember { mutableStateOf<Alarm?>(null) }
    // בורר השעה הוא שכבה על כל המסך (TimePicker.jsx) - בלי השורה העליונה
    // ובלי הסרגל התחתון. בתוך 480dp פחות שניהם הוא לא נכנס, וכפתורי
    // ביטול/שמור נחתכו מלמטה.
    val overlayOpen = editingAlarm != null
    LaunchedEffect(overlayOpen) { onOverlayChange(overlayOpen) }
    DisposableEffect(Unit) { onDispose { onOverlayChange(false) } }
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
                if (!overlayOpen) ToolsHeader(
                    title = "שעונים מעוררים",
                    theme = theme,
                    onBack = onBack,
                    trailing = {
                        if (editingAlarm == null) {
                            ToolsIconButton(FutureIcons.Add, "הוסף שעון", theme) {
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
                            icon = FutureIcons.Schedule,
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
                                    vertical = FutureDimens.spacingXl,
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
        icon = FutureIcons.Schedule,
        theme = theme,
        onClick = onClick,
        trailing = {
            FutureSwitch(checked = alarm.isEnabled, theme = theme)
            Spacer(modifier = Modifier.width(FutureDimens.spacingSm))
            ToolsIconButton(FutureIcons.Delete, "מחק", theme, tint = theme.dangerColor, onClick = onDelete)
        },
    )
}

// Calendar.DAY_OF_WEEK: 1=ראשון...7=שבת. בלי בורר הימים הזה, alarm.days היה
// תמיד ריק בפועל - אף מסך בעולם לא איפשר להגדיר אזעקה חוזרת, למרות שהשדה
// עצמו קיים ב-data class ומטופל נכון עכשיו ב-AlarmLogic.nextTriggerMillis.
private val DAY_LABELS = listOf(1 to "א", 2 to "ב", 3 to "ג", 4 to "ד", 5 to "ה", 6 to "ו", 7 to "ש")

/**
 * בורר השעה (components/forms/TimePicker.jsx, הגרסה המעודכנת): שני "גלגלים" -
 * שעות מימין ודקות משמאל. גלגל ממוקד מקבל את טבעת הפוקוס של שדה (2dp
 * הדגשה), ומעליו ומתחתיו הערך הבא והקודם ב-30%, כך שרואים לאן ↑/↓ יזיזו.
 *
 * - ↑/↓ - הערך הבא/הקודם (מקש מוחזק רץ ברצף).
 * - ספרות - הקלדה ישירה: "0","7" = 07. אחרי שתי ספרות של שעה עוברים לדקות.
 * - ←/→ - מעבר בין שעות, דקות, ימי החזרה והכפתורים.
 *
 * קודם היו ארבעה כפתורי חץ נפרדים: כדי לשנות שעה היה צריך לנווט לחץ, ללחוץ
 * OK פעם אחת לכל דקה, ולנווט לחץ אחר - והבורר גם לא נכנס בגובה המסך.
 */
@Composable
fun TimePickerOverlay(alarm: Alarm, theme: FutureTheme, onSave: (Alarm) -> Unit, onCancel: () -> Unit) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var days by remember { mutableStateOf(alarm.days) }
    val hourFocus = remember { FocusRequester() }
    val minuteFocus = remember { FocusRequester() }
    // BACK סוגר את הבורר בלי לשמור, כמו ביטול - ולא יוצא מהאפליקציה.
    androidx.activity.compose.BackHandler(onBack = onCancel)
    LaunchedEffect(Unit) { runCatching { hourFocus.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("ערוך שעה", color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FutureTypography.weightBold)
        Spacer(modifier = Modifier.height(FutureDimens.spacingLg))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingSm)) {
            TimeWheel(
                value = hour,
                range = 0..23,
                label = "שעות",
                theme = theme,
                focusRequester = hourFocus,
                onChange = { hour = it },
                onTypedComplete = { runCatching { minuteFocus.requestFocus() } },
            )
            TimeValue(":", theme)
            TimeWheel(
                value = minute,
                range = 0..59,
                label = "דקות",
                theme = theme,
                focusRequester = minuteFocus,
                onChange = { minute = it },
                onTypedComplete = {},
            )
        }
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        Text("↑↓ שינוי · ספרות הקלדה · OK הבא", color = theme.textAlpha(60), fontSize = FutureTypography.summary)

        Spacer(modifier = Modifier.height(FutureDimens.spacingLg))
        Text(
            if (days.isEmpty()) "חד-פעמית" else "חוזרת",
            color = theme.textAlpha(60),
            fontSize = FutureTypography.summary
        )
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
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

        Spacer(modifier = Modifier.height(FutureDimens.spacingXl))

        // TimePicker.jsx: הביטול כאן הוא הכפתור השקט - זה המקום היחיד במערכת שבו הוא מופיע.
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(FutureDimens.spacingLg)) {
            FutureButton("ביטול", theme, onCancel, modifier = Modifier.weight(1f), variant = FutureButtonVariant.Quiet)
            FutureButton("שמור", theme, { onSave(alarm.copy(hour = hour, minute = minute, days = days, isEnabled = true)) }, modifier = Modifier.weight(1f))
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

/**
 * גלגל ערך אחד: הערך הבא (מעל) והקודם (מתחת) ב-30%, הערך עצמו 48sp מונו,
 * ותווית 12sp. הגלגל כולו הוא יעד פוקוס אחד - מילוי 8% במנוחה, טבעת 2dp
 * בהדגשה בפוקוס (השדה של הדיזיין סיסטם). ספרה שהוקלדה ועוד לא הושלמה
 * מוצגת בהדגשה.
 */
@Composable
private fun TimeWheel(
    value: Int,
    range: IntRange,
    label: String,
    theme: FutureTheme,
    focusRequester: FocusRequester,
    onChange: (Int) -> Unit,
    onTypedComplete: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var typed by remember { mutableStateOf("") }
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    LaunchedEffect(isFocused) { if (!isFocused) typed = "" }
    fun wrap(v: Int) = when {
        v > range.last -> range.first
        v < range.first -> range.last
        else -> v
    }
    val shape = FutureShapes.textField
    val accent = theme.readableAccentColor
    Column(
        modifier = Modifier
            .width(TimeWheelWidth)
            .clip(shape)
            .background(if (isFocused) accent.copy(alpha = 0.14f) else theme.textAlpha(8))
            .border(FutureDimens.focusBorderControl, if (isFocused) accent else Color.Transparent, shape)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = com.future.sharednav.nav.digitForKey(event.key)
                when {
                    event.key == Key.DirectionUp -> { typed = ""; onChange(wrap(value + 1)); true }
                    event.key == Key.DirectionDown -> { typed = ""; onChange(wrap(value - 1)); true }
                    // ↑/↓ שייכים לגלגל, אז OK הוא היציאה ממנו - לימי החזרה ולכפתורים.
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        typed = ""
                        focusManager.moveFocus(androidx.compose.ui.focus.FocusDirection.Down)
                        true
                    }
                    digit != null -> {
                        val next = typed + digit
                        val v = next.toInt()
                        when {
                            next.length >= 2 || v * 10 > range.last -> {
                                onChange(v.coerceIn(range))
                                typed = ""
                                onTypedComplete()
                            }
                            else -> {
                                typed = next
                                onChange(v)
                            }
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusable(interactionSource = interactionSource)
            .padding(vertical = FutureDimens.spacingSm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("%02d".format(wrap(value + 1)), color = theme.textAlpha(30), fontSize = FutureTypography.headline, fontFamily = FutureTypography.monoFamily)
        TimeValue("%02d".format(value), theme, color = if (typed.isNotEmpty()) accent else theme.textColor)
        Text("%02d".format(wrap(value - 1)), color = theme.textAlpha(30), fontSize = FutureTypography.headline, fontFamily = FutureTypography.monoFamily)
        Text(label, color = theme.textAlpha(50), fontSize = FutureTypography.label)
    }
}

/** 48sp/300 מונו, גובה שורה 1 - כך שהמרווח בין הספרות לחצים הוא המרווח של הטבלה בלבד. */
@Composable
private fun TimeValue(text: String, theme: FutureTheme, color: Color = theme.textColor) {
    Text(
        text,
        color = color,
        fontSize = FutureTypography.hero,
        fontWeight = FontWeight.Light,
        fontFamily = FutureTypography.monoFamily,
        style = TextStyle(
            lineHeight = FutureTypography.hero,
            fontFeatureSettings = "tnum",
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        textAlign = TextAlign.Center,
    )
}

/** 104dp - רוחב גלגל: שתי ספרות 48sp מונו עם ריפוד. */
private val TimeWheelWidth = 104.dp
