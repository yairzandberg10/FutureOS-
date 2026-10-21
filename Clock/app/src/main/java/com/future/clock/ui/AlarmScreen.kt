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
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.nav.onOptionsKeyPress
import androidx.compose.ui.focus.onFocusChanged
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
import com.future.clock.logic.SNOOZE_LIMIT_UNLIMITED
import com.future.clock.logic.typeDigit
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.focus.focusScale
import com.future.sharednav.focus.animateFocusColor

/** מזהה מיוחד ל-restoreFocusTo: הפוקוס חוזר לכפתור "+" ולא לשורה. */
private const val RESTORE_TO_ADD = -2

@Composable
fun AlarmScreen(
    theme: FutureTheme,
    onBack: () -> Unit,
    onOverlayChange: (Boolean) -> Unit = {},
    /** מקשים 1-4 (מחוץ לבורר ולתפריטים) מחליפים לשונית: 0 = מעוררים, 1 = עולמי, 2 = עצר, 3 = טיימר. */
    onTabDigit: (Int) -> Unit = {},
) {
    val context = LocalContext.current
    // CLK-21: הטעינה הראשונה סינכרונית, כדי שלא יבהב מצב ריק לפני שהמעוררים מופיעים.
    val alarms = remember { mutableStateListOf<Alarm>().apply { addAll(AlarmLogic.loadAlarms(context)) } }
    // הרשימה במסך היא עותק של ההעדפות בלבד: נטענת מחדש בכניסה, בחזרה למסך
    // (ON_RESUME) ואחרי כל שמירה או מחיקה. טעינה חוזרת מנקה לפני ההוספה, כך
    // שלא נוצרים כפילויות.
    fun reloadAlarms() {
        alarms.clear()
        alarms.addAll(AlarmLogic.loadAlarms(context))
    }
    // בלי הרשאת מעורר מדויק המערכת יכולה לאחר אזעקה - מציגים באנר שפותח את ההרשאה.
    var exactAlarmsAllowed by remember { mutableStateOf(AlarmLogic.canScheduleExact(context)) }
    var snoozeLimit by remember { mutableIntStateOf(AlarmLogic.snoozeLimit(context)) }
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
    // הכיבוי והמחיקה יושבים בתפריט האפשרויות של המעורר הממוקד (כמו בעיצוב).
    // קודם היו מתג ופח בתוך השורה, אבל אי אפשר היה להגיע אליהם בחצים, והמתג
    // לא הגיב בכלל - מעורר שנוצר נשאר לתמיד.
    var focusedAlarmId by remember { mutableStateOf<Int?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var snoozePickerOpen by remember { mutableStateOf(false) }
    // CK8: אחרי סגירת הבורר או התפריט הפוקוס חוזר לשורת המעורר (שנוסף, נערך או
    // היה ממוקד), ואם אין כזו - ל-"+". קודם שום פריט לא היה ממוקד, ו-OK לא עשה כלום.
    val rowRequesters = remember { mutableMapOf<Int, FocusRequester>() }
    val addFocus = remember { FocusRequester() }
    // CLK-D20: בכניסה למסך הפוקוס מתחיל על "+", כדי שיהיה פוקוס גלוי לפני הלחיצה הראשונה.
    var restoreFocusTo by remember { mutableStateOf<Int?>(RESTORE_TO_ADD) }
    LaunchedEffect(restoreFocusTo, editingAlarm, menuOpen, snoozePickerOpen) {
        val target = restoreFocusTo ?: return@LaunchedEffect
        if (editingAlarm != null || menuOpen || snoozePickerOpen) return@LaunchedEffect
        repeat(6) {
            androidx.compose.runtime.withFrameNanos { }
            val requester = if (target == RESTORE_TO_ADD) addFocus
                else rowRequesters[target] ?: alarms.firstOrNull()?.let { rowRequesters[it.id] } ?: addFocus
            if (runCatching { requester.requestFocus() }.isSuccess) {
                restoreFocusTo = null
                return@LaunchedEffect
            }
        }
        restoreFocusTo = null
    }
    onOptionsKeyPress { if (!overlayOpen && pendingDelete == null && !snoozePickerOpen) menuOpen = true }

    // צלצול רץ ברקע (מסך נפרד) ומשנה את האזעקה - בחזרה למסך טוענים מחדש מההעדפות, כמו
    // בכניסה למסך, כדי שלא יוצג "מופעל" לאזעקה שכבר כבויה.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reloadAlarms()
                exactAlarmsAllowed = AlarmLogic.canScheduleExact(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // השינוי מוחל על הרשימה העדכנית בהעדפות (לא על העותק שבמסך) - צלצול שרץ ברקע
    // כבר כיבה אזעקה חד-פעמית, ושמירה של העותק הישן הייתה מחזירה אותה להפעלה.
    // אחרי השמירה הרשימה במסך נטענת מההעדפות שוב.
    fun updateAlarms(change: (MutableList<Alarm>) -> Unit) {
        val fresh = AlarmLogic.getAlarms(context).toMutableList()
        change(fresh)
        AlarmLogic.saveAlarms(context, fresh)
        reloadAlarms()
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) {
        updateAlarms { list ->
            val at = list.indexOfFirst { it.id == alarm.id }
            if (at != -1) {
                list[at] = list[at].copy(isEnabled = enabled)
            }
        }
    }

    if (menuOpen) {
        val focused = alarms.firstOrNull { it.id == focusedAlarmId }
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { menuOpen = false; restoreFocusTo = focusedAlarmId ?: RESTORE_TO_ADD },
            header = focused?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "שעונים מעוררים",
        ) {
            if (focused != null) {
                FutureMenuRow(if (focused.isEnabled) "כבה" else "הפעל", FutureIcons.Schedule, theme, {
                    menuOpen = false
                    setEnabled(focused, !focused.isEnabled)
                })
                FutureMenuRow("ערוך שעה", FutureIcons.Edit, theme, {
                    menuOpen = false
                    editingAlarm = focused
                })
            } else {
                // CLK-D11: הנודניק הוא הגדרה לכל המעוררים. השורה בתפריט הכללי לא משנה
                // ערך בלחיצה - היא פותחת בורר מפורש עם כותרת "לכל המעוררים", והערך
                // הנוכחי מסומן בו.
                FutureMenuRow(
                    "נודניק לכל המעוררים: " + snoozeLabel(snoozeLimit),
                    FutureIcons.Schedule, theme, {
                        menuOpen = false
                        snoozePickerOpen = true
                    },
                )
            }
            FutureMenuRow("מעורר חדש", FutureIcons.Add, theme, {
                menuOpen = false
                val newId = AlarmLogic.newAlarmId(context)
                // CK7: מעורר חדש נכנס לרשימה רק ב"שמור". קודם הוא נשמר ותוזמן עוד
                // לפני שהבורר נפתח, ו"ביטול" השאיר מעורר 07:00 מופעל.
                editingAlarm = Alarm(newId, 7, 0, emptySet())
            })
            if (focused != null) {
                FutureMenuRow("מחק", FutureIcons.Delete, theme, {
                    menuOpen = false
                    pendingDelete = focused
                }, destructive = true)
            }
        }
    }

    if (snoozePickerOpen) {
        FutureOptionsMenu(
            theme = theme,
            onDismissRequest = { snoozePickerOpen = false; restoreFocusTo = RESTORE_TO_ADD },
            header = "נודניק - לכל המעוררים",
        ) {
            listOf(SNOOZE_LIMIT_UNLIMITED, 3, 5, 10).forEach { option ->
                FutureMenuRow(
                    snoozeLabel(option),
                    if (option == snoozeLimit) FutureIcons.Check else null,
                    theme,
                    {
                        AlarmLogic.setSnoozeLimit(context, option)
                        snoozeLimit = option
                        snoozePickerOpen = false
                        restoreFocusTo = RESTORE_TO_ADD
                    },
                )
            }
        }
    }

    pendingDelete?.let { alarm ->
        ConfirmDialog(
            message = "למחוק את השעון %02d:%02d?".format(alarm.hour, alarm.minute),
            surfaceColor = theme.surfaceColor,
            textColor = theme.textColor,
            dangerColor = theme.dangerColor,
            onCancel = { restoreFocusTo = alarm.id; pendingDelete = null },
            onConfirm = {
                val index = alarms.indexOfFirst { it.id == alarm.id }
                updateAlarms { list -> list.removeAll { it.id == alarm.id } }
                // הפוקוס עובר לשכן של המחוק (הבא, ואם אין - הקודם), ואם הרשימה ריקה - ל"+".
                restoreFocusTo = alarms.getOrNull(index)?.id ?: alarms.getOrNull(index - 1)?.id ?: RESTORE_TO_ADD
                pendingDelete = null
            },
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(theme.backgroundColor)
                // מהרשימה אין מסלול מקשים ללשוניות התחתונות (הסרגל לא ממוקד). לכן מקשים
                // 1-4 מחליפים לשונית. בבורר, בתפריטים ובדיאלוג מחיקה המקשים לא עוברים לכאן.
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || overlayOpen || menuOpen || snoozePickerOpen || pendingDelete != null) return@onKeyEvent false
                    val index = com.future.sharednav.nav.digitForKey(event.key)?.toInt()?.minus(1) ?: return@onKeyEvent false
                    if (index !in 0..3) return@onKeyEvent false
                    onTabDigit(index)
                    true
                }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!overlayOpen && !exactAlarmsAllowed) {
                    FutureButton(
                        text = "אין הרשאה למעורר מדויק - OK לפתיחת ההגדרות",
                        theme = theme,
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                                )
                            }
                        },
                        variant = FutureButtonVariant.Secondary,
                        fillMaxWidth = true,
                        modifier = Modifier.padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingSm),
                    )
                }
                if (!overlayOpen) ToolsHeader(
                    title = "שעונים מעוררים",
                    theme = theme,
                    onBack = onBack,
                    trailing = {
                        if (editingAlarm == null) {
                            Box(modifier = Modifier.onFocusChanged { if (it.hasFocus) focusedAlarmId = null }) {
                                ToolsIconButton(FutureIcons.Add, "הוסף שעון", theme, focusRequester = addFocus) {
                                    val newId = AlarmLogic.newAlarmId(context)
                                    // CK7: מעורר חדש נכנס לרשימה רק ב"שמור". קודם הוא נשמר ותוזמן עוד
                                    // לפני שהבורר נפתח, ו"ביטול" השאיר מעורר 07:00 מופעל.
                                    editingAlarm = Alarm(newId, 7, 0, emptySet())
                                }
                            }
                        }
                    }
                )

                if (editingAlarm != null) {
                    TimePickerOverlay(editingAlarm!!, theme,
                        isNew = alarms.none { it.id == editingAlarm!!.id },
                        onSave = { updated ->
                            updateAlarms { list ->
                                val index = list.indexOfFirst { it.id == updated.id }
                                if (index != -1) {
                                    list[index] = updated
                                } else {
                                    list.add(updated)
                                }
                            }
                            restoreFocusTo = updated.id
                            editingAlarm = null
                        },
                        onCancel = {
                            restoreFocusTo = editingAlarm?.id?.takeIf { id -> alarms.any { it.id == id } } ?: RESTORE_TO_ADD
                            editingAlarm = null
                        }
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
                                        onClick = { editingAlarm = alarm },
                                        focusRequester = rowRequesters.getOrPut(alarm.id) { FocusRequester() },
                                        modifier = Modifier.onFocusChanged { if (it.hasFocus) focusedAlarmId = alarm.id },
                                    )
                                }
                            }
                            Text(
                                "OK פותח את בורר השעה. מקש האפשרויות: הפעלה, כיבוי, עריכת שעה, מעורר חדש ומחיקה. מקשים 1 עד 4 מחליפים לשוניות.",
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
 * שורת מעורר, כמו בעיצוב: [FutureSettingItem] עם מתג בלבד בסוף השורה. המתג
 * מציג את המצב; ההפעלה, הכיבוי והמחיקה בתפריט האפשרויות של השורה הממוקדת.
 */
@Composable
fun AlarmRow(alarm: Alarm, theme: FutureTheme, onClick: () -> Unit, modifier: Modifier = Modifier, focusRequester: FocusRequester? = null) {
    FutureSettingItem(
        title = "%02d:%02d".format(alarm.hour, alarm.minute),
        summary = recurrenceSummary(alarm),
        icon = FutureIcons.Schedule,
        theme = theme,
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
        trailing = {
            FutureSwitch(checked = alarm.isEnabled, theme = theme)
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
fun TimePickerOverlay(alarm: Alarm, theme: FutureTheme, isNew: Boolean = false, onSave: (Alarm) -> Unit, onCancel: () -> Unit) {
    var hour by remember { mutableIntStateOf(alarm.hour) }
    var minute by remember { mutableIntStateOf(alarm.minute) }
    var days by remember { mutableStateOf(alarm.days) }
    val hourFocus = remember { FocusRequester() }
    val minuteFocus = remember { FocusRequester() }
    // OK בגלגל עובר ליום הראשון (א׳). קודם moveFocus(Down) בחר את היום הקרוב
    // גיאומטרית, ומגלגל הדקות (בצד שמאל) נחת על ו׳.
    val firstDayFocus = remember { FocusRequester() }
    // BACK סוגר את הבורר בלי לשמור, כמו ביטול - ולא יוצא מהאפליקציה.
    androidx.activity.compose.BackHandler(onBack = onCancel)
    // בכניסה השנייה הבקשה הראשונה נבלעה (הגלגל עוד לא היה מצויר) והפוקוס נחת
    // על "ביטול" - מנסים שוב בכמה הפריימים הראשונים.
    LaunchedEffect(Unit) {
        repeat(6) {
            androidx.compose.runtime.withFrameNanos { }
            if (runCatching { hourFocus.requestFocus() }.isSuccess) return@LaunchedEffect
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = FutureDimens.screenPadding, vertical = FutureDimens.spacingLg),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(if (isNew) "מעורר חדש" else "ערוך שעה", color = theme.textColor, fontSize = FutureTypography.screenTitle, fontWeight = FutureTypography.weightBold)
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
                onOk = { runCatching { minuteFocus.requestFocus() } },
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
                onOk = { runCatching { firstDayFocus.requestFocus() } },
            )
        }
        Spacer(modifier = Modifier.height(FutureDimens.spacingSm))
        Text("↑↓ שינוי · ספרות הקלדה · OK לשדה הבא (בימים: OK מסמן יום)", color = theme.textAlpha(60), fontSize = FutureTypography.summary)

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
                    modifier = if (dayValue == DAY_LABELS.first().first) Modifier.focusRequester(firstDayFocus) else Modifier,
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
            FutureButton("שמור", theme, { onSave(alarm.copy(hour = hour, minute = minute, days = days)) }, modifier = Modifier.weight(1f))
        }
    }
}

/**
 * עיגול של יום בשבוע. מאציל ל-[FutureDayChip] המשותף - העיצוב שהיה כאן
 * צבע את המצב הנבחר בהדגשה הגולמית, שנעלמת במצב בהיר עם הדגשה לבנה.
 */
@Composable
fun DayToggleChip(label: String, selected: Boolean, theme: FutureTheme, modifier: Modifier = Modifier, onToggle: () -> Unit) {
    FutureDayChip(text = label, theme = theme, modifier = modifier, selected = selected, onClick = onToggle)
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
    onOk: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    var typed by remember { mutableStateOf("") }
    // CLK-D04/D16: שתי ספרות מהירות מגיעות לפני הרכבה מחדש, ו-typed שנקרא מה-state היה ישן
    // (4 ואז 5 נתן 05). המאגר ללוגיקה נשמר מחוץ ל-state, ו-typed רק מציג את ההדגשה.
    val typedBuffer = remember { arrayOf("") }
    LaunchedEffect(isFocused) { if (!isFocused) { typed = ""; typedBuffer[0] = "" } }
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
            .focusScale(isFocused, focusedScale = 1.05f)
            .clip(shape)
            .background(animateFocusColor(isFocused, accent.copy(alpha = 0.14f), theme.textAlpha(8)).value)
            .border(FutureDimens.focusBorderControl, animateFocusColor(isFocused, accent, Color.Transparent).value, shape)
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val digit = com.future.sharednav.nav.digitForKey(event.key)
                when {
                    event.key == Key.DirectionUp -> { typed = ""; typedBuffer[0] = ""; onChange(wrap(value + 1)); true }
                    event.key == Key.DirectionDown -> { typed = ""; typedBuffer[0] = ""; onChange(wrap(value - 1)); true }
                    // ↑/↓ שייכים לגלגל, אז OK הוא היציאה ממנו - לימי החזרה ולכפתורים.
                    event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter -> {
                        typed = ""
                        typedBuffer[0] = ""
                        onOk()
                        true
                    }
                    digit != null -> {
                        val result = typeDigit(typedBuffer[0], digit.toInt(), range.last)
                        typedBuffer[0] = result.buffer
                        typed = result.buffer
                        onChange(result.value)
                        if (result.complete) onTypedComplete()
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

/** תווית מגבלת הנודניק: "ללא הגבלה" או "N פעמים". */
private fun snoozeLabel(limit: Int): String =
    if (limit == SNOOZE_LIMIT_UNLIMITED) "ללא הגבלה" else "$limit פעמים"
