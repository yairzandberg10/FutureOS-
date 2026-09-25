package com.future.calendar.ui
import com.future.sharednav.systemui.StatusBarInset

import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.components.FutureSwitch
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureDialogButtons
import com.future.sharednav.components.FutureButtonVariant
import com.future.sharednav.components.FutureFormField
import com.future.sharednav.theme.idleFieldColor
import com.future.sharednav.theme.mutedTextColor
import com.future.sharednav.theme.FutureMotion
import com.future.sharednav.theme.readableAccentColor
import com.future.sharednav.theme.idleChipColor
import com.future.sharednav.theme.FutureDimens
import com.future.sharednav.focus.focusMotion
import com.future.sharednav.components.FutureButton
import com.future.sharednav.theme.onReadableAccentColor
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.focus.bringIntoViewOnFocus

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.components.AppDialog
import com.future.calendar.data.CalendarEvent
import com.future.calendar.data.CalMonth
import com.future.calendar.data.CalendarKind
import com.future.calendar.data.CalendarMonths
import com.future.sharednav.components.FutureCard
import com.future.sharednav.components.FutureDivider
import com.future.sharednav.components.FutureSectionHeader
import com.future.sharednav.components.FutureSettingItem
import com.future.calendar.data.DafYomi
import com.future.calendar.data.DayZmanim
import com.future.calendar.data.HebrewDateFormatter
import com.future.calendar.data.HebrewNumerals
import com.future.sharednav.focus.escapeTextFieldFocusTrap
import com.future.sharednav.theme.FutureTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

enum class CalendarViewMode { DAY, WEEK, MONTH, YEAR }

private val HEBREW_WEEKDAY_LABELS = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
private val HE_LOCALE = Locale.forLanguageTag("he")

private fun LocalDate.sundayStartColumn(): Int = dayOfWeek.value % 7
private fun LocalDate.weekStart(): LocalDate = this.minusDays(sundayStartColumn().toLong())

@Composable
fun CalendarHomeScreen(
    viewMode: CalendarViewMode,
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    hasPermission: Boolean,
    onRequestPermission: () -> Unit,
    onChangeViewMode: (CalendarViewMode) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenMonth: (CalMonth) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onGoToday: () -> Unit,
    onOpenSettings: () -> Unit,
    resolvedLat: Double,
    resolvedLon: Double,
    showWeather: Boolean,
    kind: CalendarKind = CalendarKind.COMBINED,
    usingFallbackLocation: Boolean = false
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp, 20.dp, 20.dp, 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FocusableIconButton(icon = FutureIcons.Settings, theme = theme, onClick = onOpenSettings)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FocusableIconButton(icon = FutureIcons.CalendarToday, theme = theme, onClick = onGoToday)
                        FocusableIconButton(icon = FutureIcons.Add, theme = theme, onClick = onAddEvent)
                    }
                }

                if (usingFallbackLocation) {
                    Text(
                        "אין מיקום GPS זמין - הזמנים מוצגים לפי עיר ברירת המחדל",
                        color = theme.textColor.copy(alpha = 0.55f),
                        fontSize = FutureTypography.caption,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                }

                val modes = listOf(CalendarViewMode.DAY, CalendarViewMode.WEEK, CalendarViewMode.MONTH, CalendarViewMode.YEAR)
                com.future.sharednav.components.FutureTabRow(
                    items = listOf("יום", "שבוע", "חודש", "שנה"),
                    selectedIndex = modes.indexOf(viewMode),
                    theme = theme,
                    onSelect = { onChangeViewMode(modes[it]) },
                )

                if (!hasPermission) {
                    PermissionRequiredMessage(theme = theme, onRequestPermission = onRequestPermission)
                    return@Column
                }

                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        // בלוח עברי החודש נגזר מהתאריך הנבחר (חודש עברי שלם), אחרת מהחודש הלועזי.
                        val calMonth = if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewContaining(selectedDate)
                        else CalendarMonths.gregorian(month, kind)
                        MonthNavigationRow(month = calMonth, theme = theme, onPrevMonth = onPrevMonth, onNextMonth = onNextMonth)
                        WeekDayHeaderRow(theme = theme)
                        MonthGrid(
                            month = calMonth,
                            selectedDate = selectedDate,
                            today = today,
                            eventsByDate = eventsByDate,
                            theme = theme,
                            kind = kind,
                            onMoveTo = onSelectDate,
                            onOpenDay = onOpenDay
                        )
                    }
                    CalendarViewMode.WEEK -> WeekView(
                        selectedDate = selectedDate,
                        today = today,
                        eventsByDate = eventsByDate,
                        theme = theme,
                        kind = kind,
                        onPrevWeek = onPrevWeek,
                        onNextWeek = onNextWeek,
                        onOpenDay = onOpenDay
                    )
                    CalendarViewMode.DAY -> DayView(
                        date = selectedDate,
                        events = eventsByDate[selectedDate] ?: emptyList(),
                        theme = theme,
                        onPrevDay = onPrevDay,
                        onNextDay = onNextDay,
                        onAddEvent = onAddEvent,
                        onEditEvent = onEditEvent,
                        onDeleteEvent = onDeleteEvent,
                        lat = resolvedLat,
                        lon = resolvedLon,
                        showWeather = showWeather,
                        kind = kind
                    )
                    CalendarViewMode.YEAR -> YearView(
                        title = if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewYearLabel(selectedDate)
                        else if (kind == CalendarKind.COMBINED) "${month.year} · ${CalendarMonths.hebrewYearLabel(month.atDay(1))}"
                        else month.year.toString(),
                        months = if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewYear(selectedDate)
                        else (1..12).map { CalendarMonths.gregorian(YearMonth.of(month.year, it), kind) },
                        selectedDate = selectedDate,
                        today = today,
                        eventsByDate = eventsByDate,
                        theme = theme,
                        kind = kind,
                        onPrevYear = onPrevYear,
                        onNextYear = onNextYear,
                        onOpenMonth = onOpenMonth
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRequiredMessage(theme: FutureTheme, onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("כדי להציג את לוח השנה צריך לאשר הרשאה", color = theme.textColor.copy(alpha = 0.7f), fontSize = FutureTypography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        FutureButton("אשר הרשאה", theme, onRequestPermission)
    }
}

@Composable
private fun MonthNavigationRow(month: CalMonth, theme: FutureTheme, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FocusableIconButton(icon = FutureIcons.ChevronRight, theme = theme, onClick = onPrevMonth)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(month.title, textAlign = TextAlign.Center, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold, color = theme.textColor, maxLines = 1)
            if (month.subtitle.isNotBlank()) {
                Text(month.subtitle, textAlign = TextAlign.Center, fontSize = FutureTypography.caption, color = theme.mutedTextColor, maxLines = 1)
            }
        }
        FocusableIconButton(icon = FutureIcons.ChevronLeft, theme = theme, onClick = onNextMonth)
    }
}

@Composable
private fun WeekDayHeaderRow(theme: FutureTheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp)) {
        HEBREW_WEEKDAY_LABELS.forEach { label ->
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.label, color = theme.mutedTextColor)
        }
    }
}

/**
 * רשת החודש. הפוקוס הוא התאריך הנבחר עצמו ([selectedDate]) ולא אינדקס
 * פנימי: כל תזוזה מחשבת תאריך חדש ומעבירה אותו ל-[onMoveTo], שמעדכן גם את
 * החודש כשהתאריך יוצא ממנו. כך מעבר חודש בחצים ממשיך בדיוק מהיום הסמוך
 * (ולא קופץ ליום הראשון), והפוקוס לא "נגנב" מכפתורי הניווט בכל החלפת חודש.
 *
 * RTL: העמודה הראשונה (ראשון) מימין, ולכן ימינה = יום קודם, שמאלה = יום הבא.
 * למעלה מהשורה הראשונה יוצא מהרשת אל הכפתורים שמעליה.
 */
@Composable
private fun MonthGrid(
    month: CalMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    kind: CalendarKind,
    onMoveTo: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val leadingBlanks = month.first.sundayStartColumn()
    val totalCells = (((leadingBlanks + month.length + 6) / 7) * 7).coerceAtLeast(35)
    val cells: List<LocalDate?> = (0 until totalCells).map { i ->
        val offset = i - leadingBlanks
        if (offset in 0 until month.length) month.first.plusDays(offset.toLong()) else null
    }
    // תאריך נבחר מחוץ לחודש המוצג (אחרי מעבר חודש בכפתורים) - הפוקוס על היום הראשון.
    val focusDate = if (selectedDate in month) selectedDate else month.first
    val focusedIndex = cells.indexOf(focusDate)

    var isGridFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isGridFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionRight -> { onMoveTo(focusDate.minusDays(1)); true }
                    Key.DirectionLeft -> { onMoveTo(focusDate.plusDays(1)); true }
                    Key.DirectionDown -> { onMoveTo(focusDate.plusDays(7)); true }
                    Key.DirectionUp -> {
                        if (focusedIndex >= 7) { onMoveTo(focusDate.minusDays(7)); true } else false
                    }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { onOpenDay(focusDate); true }
                    else -> false
                }
            }
    ) {
        cells.chunked(7).forEachIndexed { rowIndex, rowCells ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowCells.forEachIndexed { colIndex, date ->
                    val cellIndex = rowIndex * 7 + colIndex
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(2.dp)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                isFocused = isGridFocused && cellIndex == focusedIndex,
                                hasEvents = eventsByDate[date]?.isNotEmpty() == true,
                                theme = theme,
                                kind = kind
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * תא יום. עברי - היום באותיות; לועזי - במספר; משולב - המספר הלועזי גדול
 * ומתחתיו היום העברי באותיות קטנות, כך ששני התאריכים נקראים בבת אחת.
 * הפוקוס הוא מילוי 14% ומסגרת בהדגשה, כמו בכל פריט ממוקד ברשת.
 */
@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, isFocused: Boolean, hasEvents: Boolean, theme: FutureTheme, kind: CalendarKind) {
    val shape = FutureShapes.sm
    val accent = theme.readableAccentColor
    val primary = if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewDayLabel(date) else date.dayOfMonth.toString()
    val secondary = if (kind == CalendarKind.COMBINED) CalendarMonths.hebrewDayLabel(date) else null
    val bgColor by animateColorAsState(
        when {
            isFocused -> accent.copy(alpha = 0.14f)
            isSelected -> theme.textColor.copy(alpha = 0.08f)
            else -> Color.Transparent
        },
        FutureMotion.focusColorSpec,
        label = "dayBg"
    )
    val ring by animateColorAsState(if (isFocused) accent else Color.Transparent, FutureMotion.focusColorSpec, label = "dayRing")
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bgColor)
            .border(width = FutureDimens.focusBorderItem, color = ring, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(if (secondary != null) 22.dp else 26.dp)
                    .clip(CircleShape)
                    .then(if (isToday) Modifier.background(accent) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    primary,
                    fontSize = if (kind == CalendarKind.HEBREW) FutureTypography.label else FutureTypography.body,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) theme.onReadableAccentColor else theme.textColor,
                    maxLines = 1
                )
            }
            if (secondary != null) {
                Text(secondary, fontSize = FutureTypography.caption, color = theme.mutedTextColor, maxLines = 1)
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(width = 12.dp, height = 3.dp)
                    .clip(FutureShapes.xs)
                    .background(if (hasEvents) accent else Color.Transparent)
            )
        }
    }
}

@Composable
private fun WeekView(
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    kind: CalendarKind,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val start = selectedDate.weekStart()
    val days = (0..6).map { start.plusDays(it.toLong()) }
    // בלי פוקוס התחלתי מפורש, המעבר לתצוגת שבוע (למשל מהגדרות או מתצוגת חודש)
    // משאיר את המסך בלי שום פריט מודגש ב-D-pad - בדיוק כמו ב-MonthGrid.
    val firstRowFocusRequester = remember(start) { FocusRequester() }
    LaunchedEffect(start) { firstRowFocusRequester.requestFocus() }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusableIconButton(icon = FutureIcons.ChevronRight, theme = theme, onClick = onPrevWeek)
        Text(
            "${start.dayOfMonth} ${start.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)} – ${days.last().dayOfMonth} ${days.last().month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)}",
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.SemiBold, color = theme.textColor
        )
        FocusableIconButton(icon = FutureIcons.ChevronLeft, theme = theme, onClick = onNextWeek)
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(days, key = { it.toEpochDay() }) { date ->
            WeekDayRow(
                date = date,
                isToday = date == today,
                events = eventsByDate[date] ?: emptyList(),
                theme = theme,
                onClick = { onOpenDay(date) },
                focusRequester = if (date == days.first()) firstRowFocusRequester else null,
                kind = kind
            )
        }
    }
}

@Composable
private fun WeekDayRow(date: LocalDate, isToday: Boolean, events: List<CalendarEvent>, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null, kind: CalendarKind = CalendarKind.COMBINED) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.row
    val bgColor by animateColorAsState(if (isFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.idleChipColor, FutureMotion.focusColorSpec, label = "weekRowBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusMotion(interactionSource)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).then(if (isToday) Modifier.background(theme.readableAccentColor) else Modifier.background(theme.textColor.copy(alpha = 0.08f))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewDayLabel(date) else date.dayOfMonth.toString(),
                fontSize = FutureTypography.body, fontWeight = FontWeight.Bold, color = if (isToday) theme.onReadableAccentColor else theme.textColor
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            val weekday = date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)
            val dayTitle = when (kind) {
                CalendarKind.COMBINED -> "$weekday · ${CalendarMonths.hebrewDayLabel(date)} ${CalendarMonths.hebrewMonthName(date)}"
                CalendarKind.HEBREW -> "$weekday · ${date.dayOfMonth}.${date.monthValue}"
                CalendarKind.GREGORIAN -> weekday
            }
            Text(dayTitle, fontSize = FutureTypography.summary, fontWeight = FontWeight.SemiBold, color = theme.textColor, maxLines = 1)
            if (events.isEmpty()) {
                Text("אין אירועים", fontSize = FutureTypography.caption, color = theme.textColor.copy(alpha = 0.4f))
            } else {
                val shown = events.take(2)
                shown.forEach { Text("• ${it.title}", fontSize = FutureTypography.caption, color = theme.textColor.copy(alpha = 0.7f)) }
                if (events.size > shown.size) {
                    Text("+${events.size - shown.size} נוספים", fontSize = FutureTypography.caption, color = theme.textColor.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun DayView(
    date: LocalDate,
    events: List<CalendarEvent>,
    theme: FutureTheme,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    lat: Double,
    lon: Double,
    showWeather: Boolean,
    kind: CalendarKind = CalendarKind.COMBINED
) {
    var menuFor by remember { mutableStateOf<CalendarEvent?>(null) }
    // עוקב אחרי האירוע הממוקד כרגע ברשימת היום, כדי שמקש Options יוכל לפתוח
    // את תפריט העריכה/מחיקה שלו - בלי זה התפריט לא נגיש בכלל במכשיר אמיתי,
    // כי מקש Options הפיזי נחסם ברמת המערכת ולעולם לא מגיע כ-Key.Menu לאפליקציה.
    var focusedEvent by remember { mutableStateOf<CalendarEvent?>(null) }
    com.future.sharednav.nav.onOptionsKeyPress { if (focusedEvent != null) menuFor = focusedEvent }
    menuFor?.let { event ->
        EventOptionsMenu(
            event = event, theme = theme,
            onDismiss = { menuFor = null },
            onEdit = { onEditEvent(event); menuFor = null },
            onDelete = { onDeleteEvent(event); menuFor = null }
        )
    }

    val zmanim = remember(date, lat, lon) { com.future.calendar.data.ZmanimCalculator.calculate(date, lat, lon) }
    val hebrewDate = remember(date) { HebrewDateFormatter.format(date) }
    // הדף היומי מתחלף בצאת הכוכבים ולא בחצות - רק כשמציגים את היום האמיתי
    // הנוכחי (לא יום עבר/עתיד שהמשתמש דפדף אליו) יש טעם להשוות לשעון בפועל.
    val isViewingToday = date == java.time.LocalDate.now()
    val nightfallPassed = isViewingToday && zmanim.sunset?.let {
        java.time.LocalTime.now().isAfter(it.plusMinutes(40))
    } == true
    val dafYomi = remember(date, nightfallPassed) { DafYomi.forDate(date, afterNightfall = nightfallPassed) }

    var forecast by remember(lat, lon) { mutableStateOf<Map<LocalDate, com.future.calendar.data.DailyWeather>?>(null) }
    LaunchedEffect(lat, lon, showWeather) {
        if (showWeather) {
            forecast = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.future.calendar.data.WeatherService.fetchForecast(lat, lon)
            }
        }
    }
    val todayWeather = forecast?.get(date)

    // בלי פוקוס התחלתי מפורש, המסך הזה נשאר בלי שום פריט מודגש ב-D-pad עד
    // שהמשתמש לוחץ כיוון כלשהו - בדיוק כמו ב-MonthGrid.
    val prevDayFocusRequester = remember(date) { FocusRequester() }
    LaunchedEffect(date) { prevDayFocusRequester.requestFocus() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FocusableIconButton(icon = FutureIcons.ChevronRight, theme = theme, onClick = onPrevDay, focusRequester = prevDayFocusRequester)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE), fontSize = FutureTypography.label, color = theme.mutedTextColor)
                val gregorianDate = "${date.dayOfMonth} ב${date.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)}"
                // בלוח עברי התאריך העברי הוא הראשי; בלועזי בלבד - אין תאריך עברי.
                val primary = if (kind == CalendarKind.HEBREW && hebrewDate.isNotBlank()) hebrewDate else gregorianDate
                val secondary = when (kind) {
                    CalendarKind.HEBREW -> gregorianDate
                    CalendarKind.COMBINED -> hebrewDate
                    CalendarKind.GREGORIAN -> ""
                }
                Text(primary, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.textColor, maxLines = 1)
                if (secondary.isNotBlank()) Text(secondary, fontSize = FutureTypography.caption, color = theme.readableAccentColor, maxLines = 1)
            }
            FocusableIconButton(icon = FutureIcons.ChevronLeft, theme = theme, onClick = onNextDay)
        }

        LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (showWeather && todayWeather != null) {
                item { WeatherPanel(weather = todayWeather, theme = theme) }
            }
            item { ZmanimPanel(zmanim = zmanim, theme = theme) }
            if (dafYomi != null) {
                item { DafYomiPanel(masechet = dafYomi.masechet, daf = dafYomi.daf, theme = theme) }
            }
            if (events.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
                        Text("אין אירועים ביום זה", color = theme.mutedTextColor, fontSize = FutureTypography.body)
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventRow(event = event, theme = theme, onClick = { onEditEvent(event) }, onMenu = { menuFor = event }, onFocusChanged = { isFocused -> if (isFocused) focusedEvent = event })
                }
            }
        }
    }
}

@Composable
private fun ZmanimPanel(zmanim: DayZmanim, theme: FutureTheme) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(FutureShapes.lg).background(theme.textColor.copy(alpha = 0.06f)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(FutureIcons.WbTwilight, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("זמני היום", fontSize = FutureTypography.label, fontWeight = FontWeight.SemiBold, color = theme.textColor.copy(alpha = 0.7f))
        }
        Spacer(modifier = Modifier.height(8.dp))
        val rows = buildList {
            zmanim.sunrise?.let { add("נץ החמה" to it) }
            zmanim.chatzot?.let { add("חצות היום" to it) }
            zmanim.sofZmanShema?.let { add("סוף זמן ק״ש" to it) }
            zmanim.sunset?.let { add("שקיעה" to it) }
            zmanim.candleLighting?.let { add("הדלקת נרות" to it) }
            zmanim.motzeiShabbat?.let { add("צאת שבת" to it) }
        }
        rows.chunked(2).forEach { rowPair ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                rowPair.forEach { (label, time) ->
                    Column(modifier = Modifier.weight(1f)) {
                        Text(label, fontSize = FutureTypography.caption, color = theme.mutedTextColor)
                        Text("%02d:%02d".format(time.hour, time.minute), fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold, color = theme.textColor)
                    }
                }
                if (rowPair.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DafYomiPanel(masechet: String, daf: Int, theme: FutureTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(FutureShapes.lg).background(theme.accentColor.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(FutureIcons.MenuBook, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text("דף יומי", fontSize = FutureTypography.caption, color = theme.mutedTextColor)
            Text("$masechet דף ${com.future.calendar.data.HebrewNumerals.toHebrew(daf)}", fontSize = FutureTypography.body, fontWeight = FontWeight.Bold, color = theme.textColor)
        }
    }
}

@Composable
private fun WeatherPanel(weather: com.future.calendar.data.DailyWeather, theme: FutureTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(FutureShapes.lg).background(theme.textColor.copy(alpha = 0.06f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(weather.icon, contentDescription = weather.description, tint = theme.textColor, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(weather.description, fontSize = FutureTypography.label, color = theme.textColor.copy(alpha = 0.6f))
            val tempLabel = if (weather.currentTemp != null) {
                "${weather.currentTemp.toInt()}° (מקסימום ${weather.maxTemp.toInt()}° · מינימום ${weather.minTemp.toInt()}°)"
            } else {
                "מקסימום ${weather.maxTemp.toInt()}° · מינימום ${weather.minTemp.toInt()}°"
            }
            Text(tempLabel, fontSize = FutureTypography.body, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        }
    }
}

/**
 * תצוגת השנה: ארבעה חודשים בשורה בתאים בגובה קבוע, כך ש-12 חודשים (או 13
 * בשנה עברית מעוברת) נכנסים במסך אחד בלי גלילה ובלי שורות בגבהים שונים.
 * קודם היו 3 בשורה, והמשפט "N ימים עם אירועים" נשבר לשתי שורות בתאים הצרים.
 */
@Composable
private fun YearView(
    title: String,
    months: List<CalMonth>,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    kind: CalendarKind,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onOpenMonth: (CalMonth) -> Unit
) {
    val columns = 4
    val initial = months.indexOfFirst { selectedDate in it }.coerceAtLeast(0)
    var focusedIndex by remember(months.firstOrNull()?.first) { mutableIntStateOf(initial) }
    var isGridFocused by remember { mutableStateOf(false) }
    val gridFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { gridFocusRequester.requestFocus() } }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusableIconButton(icon = FutureIcons.ChevronRight, theme = theme, onClick = onPrevYear)
        Text(title, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        FocusableIconButton(icon = FutureIcons.ChevronLeft, theme = theme, onClick = onNextYear)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .focusRequester(gridFocusRequester)
            .onFocusChanged { isGridFocused = it.isFocused }
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val current = focusedIndex.coerceIn(0, months.lastIndex)
                fun go(n: Int): Boolean = if (n in months.indices) { focusedIndex = n; true } else false
                when (event.key) {
                    // RTL: החודש הראשון מימין.
                    Key.DirectionRight -> go(current - 1) || true
                    Key.DirectionLeft -> go(current + 1) || true
                    Key.DirectionDown -> go(current + columns) || true
                    // מהשורה העליונה - החוצה אל כפתורי השנה.
                    Key.DirectionUp -> go(current - columns)
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { onOpenMonth(months[current]); true }
                    else -> false
                }
            },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        months.chunked(columns).forEachIndexed { rowIndex, rowMonths ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                rowMonths.forEachIndexed { colIndex, month ->
                    val idx = rowIndex * columns + colIndex
                    val eventDays = eventsByDate.count { (date, list) -> list.isNotEmpty() && date in month }
                    MonthMiniCard(
                        name = if (kind == CalendarKind.HEBREW) CalendarMonths.hebrewMonthName(month.first)
                        else month.first.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE),
                        // במשולב - גם החודש העברי שבו החודש הלועזי מתחיל.
                        secondary = if (kind == CalendarKind.COMBINED) CalendarMonths.hebrewMonthName(month.first) else null,
                        isCurrent = today in month,
                        eventDays = eventDays,
                        isFocused = isGridFocused && idx == focusedIndex,
                        theme = theme,
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(columns - rowMonths.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun MonthMiniCard(name: String, secondary: String?, isCurrent: Boolean, eventDays: Int, isFocused: Boolean, theme: FutureTheme, modifier: Modifier) {
    val shape = FutureShapes.sm
    val accent = theme.readableAccentColor
    val bg by animateColorAsState(if (isFocused) accent.copy(alpha = 0.14f) else theme.idleChipColor, FutureMotion.focusColorSpec, label = "monthBg")
    val ring by animateColorAsState(if (isFocused) accent else Color.Transparent, FutureMotion.focusColorSpec, label = "monthRing")
    Column(
        modifier = modifier
            .height(58.dp)
            .clip(shape)
            .background(bg)
            .border(width = FutureDimens.focusBorderControl, color = ring, shape = shape)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            name,
            fontSize = FutureTypography.summary,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
            color = if (isCurrent) accent else theme.textColor,
            maxLines = 1
        )
        if (secondary != null) {
            Text(secondary, fontSize = FutureTypography.caption, color = theme.mutedTextColor, maxLines = 1)
        }
        // נקודה + מספר הימים עם אירועים, במקום משפט שלא נכנס בתא.
        if (eventDays > 0) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(accent))
                Spacer(modifier = Modifier.width(3.dp))
                Text("$eventDays", fontSize = FutureTypography.caption, color = theme.mutedTextColor)
            }
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, theme: FutureTheme, onClick: () -> Unit, onMenu: () -> Unit, onFocusChanged: (Boolean) -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { onFocusChanged(isFocused) }
    val shape = FutureShapes.row
    val bgColor by animateColorAsState(if (isFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.idleChipColor, FutureMotion.focusColorSpec, label = "eventRowBg")
    val timeLabel = if (event.allDay) "כל היום" else {
        val start = java.time.Instant.ofEpochMilli(event.startMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        val end = java.time.Instant.ofEpochMilli(event.endMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        "%02d:%02d–%02d:%02d".format(start.hour, start.minute, end.hour, end.minute)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusMotion(interactionSource)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .onKeyEvent { event2 ->
                if (isFocused && event2.type == KeyEventType.KeyUp && (event2.key == Key.Menu || event2.key == Key.Settings)) {
                    onMenu(); true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stripeColor = if (event.color != 0) Color(event.color) else theme.accentColor
        Box(modifier = Modifier.width(4.dp).height(36.dp).clip(FutureShapes.xs).background(stripeColor))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, color = theme.textColor, fontWeight = FontWeight.SemiBold, fontSize = FutureTypography.bodyLarge)
            Text(timeLabel, color = theme.mutedTextColor, fontSize = FutureTypography.label)
            if (event.location.isNotBlank()) {
                Text(event.location, color = theme.textColor.copy(alpha = 0.4f), fontSize = FutureTypography.label)
            }
        }
    }
}

@Composable
private fun EventOptionsMenu(event: CalendarEvent, theme: FutureTheme, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    FutureOptionsMenu(theme = theme, onDismissRequest = onDismiss, header = event.title) {
        FutureMenuRow("ערוך אירוע", FutureIcons.Edit, theme, onEdit)
        FutureMenuRow("מחק אירוע", FutureIcons.Delete, theme, onDelete, destructive = true)
    }
}



@Composable
fun EventEditDialog(
    initialDate: LocalDate,
    editingEvent: CalendarEvent?,
    theme: FutureTheme,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String, location: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, allDay: Boolean) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(editingEvent?.title ?: "") }
    var description by remember { mutableStateOf(editingEvent?.description ?: "") }
    var location by remember { mutableStateOf(editingEvent?.location ?: "") }
    var allDay by remember { mutableStateOf(editingEvent?.allDay ?: false) }

    val initialStart = editingEvent?.let {
        java.time.Instant.ofEpochMilli(it.startMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    } ?: LocalTime.of(9, 0)
    val initialEnd = editingEvent?.let {
        java.time.Instant.ofEpochMilli(it.endMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    } ?: LocalTime.of(10, 0)

    var startHour by remember { mutableIntStateOf(initialStart.hour) }
    var startMinute by remember { mutableIntStateOf(initialStart.minute) }
    var endHour by remember { mutableIntStateOf(initialEnd.hour) }
    var endMinute by remember { mutableIntStateOf(initialEnd.minute) }

    AppDialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .escapeTextFieldFocusTrap()
                    .clip(FutureShapes.dialog)
                    .background(theme.surfaceColor)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if (editingEvent != null) "עריכת אירוע" else "אירוע חדש", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = FutureTypography.dialog)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${initialDate.dayOfMonth} ב${initialDate.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)}",
                    color = theme.mutedTextColor,
                    fontSize = FutureTypography.label
                )
                Spacer(modifier = Modifier.height(12.dp))
                EditField("כותרת", title, theme) { title = it }
                EditField("מיקום", location, theme) { location = it }
                EditField("הערות", description, theme) { description = it }

                Spacer(modifier = Modifier.height(8.dp))
                AllDayToggleRow(allDay = allDay, theme = theme, onToggle = { allDay = !allDay })

                if (!allDay) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeStepperField(
                            label = "התחלה", hour = startHour, minute = startMinute, theme = theme,
                            onHourChange = { startHour = ((startHour + it) + 24) % 24 },
                            onMinuteChange = {
                                var m = startMinute + it; var h = startHour
                                if (m >= 60) { m -= 60; h = (h + 1) % 24 }
                                if (m < 0) { m += 60; h = (h - 1 + 24) % 24 }
                                startMinute = m; startHour = h
                            }
                        )
                        TimeStepperField(
                            label = "סיום", hour = endHour, minute = endMinute, theme = theme,
                            onHourChange = { endHour = ((endHour + it) + 24) % 24 },
                            onMinuteChange = {
                                var m = endMinute + it; var h = endHour
                                if (m >= 60) { m -= 60; h = (h + 1) % 24 }
                                if (m < 0) { m += 60; h = (h - 1 + 24) % 24 }
                                endMinute = m; endHour = h
                            }
                        )
                    }
                }

                FutureDialogButtons {
                    FutureButton("ביטול", theme, onDismiss, variant = FutureButtonVariant.Secondary)
                    FutureButton(if (editingEvent != null) "שמור" else "צור", theme, {
                        onSave(title, description, location, startHour, startMinute, endHour, endMinute, allDay)
                    })
                    if (onDelete != null) {
                        FutureButton("מחק", theme, onDelete, variant = FutureButtonVariant.Destructive)
                    }
                }
            }
        }
    }
}

@Composable
private fun AllDayToggleRow(allDay: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(FutureShapes.md)
            .background(if (isFocused) theme.textColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("כל היום", color = theme.textColor, fontSize = FutureTypography.body)
        FutureSwitch(checked = allDay, theme = theme)
    }
}

@Composable
private fun TimeStepperField(label: String, hour: Int, minute: Int, theme: FutureTheme, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    val shape = FutureShapes.textField
    val ring by animateColorAsState(if (isFocused) theme.readableAccentColor else Color.Transparent, FutureMotion.focusColorSpec, label = "timeFieldRing")
    Column(
        modifier = Modifier
            .clip(shape)
            .background(theme.idleFieldColor)
            .border(FutureDimens.focusBorderControl, ring, shape)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { onMinuteChange(15); true }
                    Key.DirectionDown -> { onMinuteChange(-15); true }
                    Key.DirectionRight -> { onHourChange(-1); true }
                    Key.DirectionLeft -> { onHourChange(1); true }
                    // בלי זה השדה הזה "בולע" את כל ארבעת מקשי הכיוון לצמיתות ואין דרך
                    // במקלדת לזוז הלאה ממנו לשדה הבא/לכפתורי שמור-מחק-ביטול - מקש
                    // האישור המרכזי הוא הדרך המפורשת לצאת קדימה מעריכת השעה.
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        focusManager.moveFocus(FocusDirection.Next)
                        true
                    }
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, color = theme.mutedTextColor, fontSize = FutureTypography.caption)
        Text("%02d:%02d".format(hour, minute), color = theme.textColor, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditField(label: String, value: String, theme: FutureTheme, onValueChange: (String) -> Unit) {
    FutureFormField(label, value, onValueChange, theme, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
}



@Composable
fun FocusableIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    com.future.sharednav.components.TopBarIconButton(icon, "", theme.textColor, theme.accentColor, onClick, focusRequester)
}

/**
 * הגדרות לוח השנה: סוג הלוח, תצוגה, ומיקום כתת-קטגוריה (מסך משלו) - קודם
 * רשימת עשר הערים ישבה באמצע מסך ההגדרות ודחפה את שאר ההגדרות מתחת לקצה.
 * אין כפתור חזור: מקש BACK הפיזי חוזר (MainActivity).
 */
@Composable
fun CalendarSettingsScreen(
    kind: CalendarKind,
    locationSummary: String,
    showWeather: Boolean,
    theme: FutureTheme,
    onSelectKind: (CalendarKind) -> Unit,
    onOpenLocation: () -> Unit,
    onToggleShowWeather: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    com.future.sharednav.components.ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = "הגדרות לוח שנה",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            FutureSectionHeader("סוג לוח", theme)
            FutureCard(theme = theme) {
                CalendarKind.entries.forEachIndexed { index, option ->
                    if (index > 0) FutureDivider(theme = theme)
                    FutureSettingItem(
                        title = "לוח ${option.label}",
                        summary = when (option) {
                            CalendarKind.HEBREW -> "חודשים עבריים, ימים באותיות"
                            CalendarKind.GREGORIAN -> "חודשים לועזיים"
                            CalendarKind.COMBINED -> "לועזי, ובכל יום גם התאריך העברי"
                        },
                        icon = if (option == CalendarKind.HEBREW) FutureIcons.Translate else FutureIcons.CalendarToday,
                        theme = theme,
                        showChevron = false,
                        focusRequester = if (index == 0) first else null,
                        onClick = { onSelectKind(option) },
                        trailing = {
                            if (option == kind) {
                                Icon(FutureIcons.Check, contentDescription = "נבחר", tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconSettingRow))
                            }
                        },
                    )
                }
            }

            FutureSectionHeader("כללי", theme)
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = "מיקום",
                    summary = locationSummary,
                    icon = FutureIcons.LocationOn,
                    theme = theme,
                    onClick = onOpenLocation,
                )
                FutureDivider(theme = theme)
                FutureSettingItem(
                    title = "תחזית מזג אוויר",
                    summary = if (showWeather) "מוצגת בתצוגת היום" else "כבויה",
                    icon = FutureIcons.WbTwilight,
                    theme = theme,
                    showChevron = false,
                    onClick = onToggleShowWeather,
                    trailing = { FutureSwitch(checked = showWeather, theme = theme) },
                )
            }
        }
    }
}

/** תת-קטגוריית המיקום: GPS, או עיר קבועה לזמני היום ולתחזית. */
@Composable
fun CalendarLocationScreen(
    useGps: Boolean,
    hasLocationPermission: Boolean,
    currentRegion: com.future.calendar.data.Region,
    theme: FutureTheme,
    onToggleGps: () -> Unit,
    onSelectRegion: (com.future.calendar.data.Region) -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }

    com.future.sharednav.components.ScreenScaffold(
        modifier = Modifier.padding(top = StatusBarInset.TITLE_GAP_DP.dp),
        backgroundColor = theme.backgroundColor,
        title = "מיקום",
        textColor = theme.textColor,
        accentColor = theme.accentColor,
    ) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            FutureCard(theme = theme) {
                FutureSettingItem(
                    title = "מיקום GPS",
                    summary = when {
                        useGps && !hasLocationPermission -> "צריך לאשר הרשאת מיקום"
                        useGps -> "מיקום המכשיר בפועל"
                        else -> "כבוי"
                    },
                    icon = FutureIcons.MyLocation,
                    theme = theme,
                    showChevron = false,
                    focusRequester = first,
                    onClick = onToggleGps,
                    trailing = { FutureSwitch(checked = useGps, theme = theme) },
                )
            }
            if (!useGps) {
                FutureSectionHeader("עיר", theme)
                FutureCard(theme = theme) {
                    com.future.calendar.data.KNOWN_REGIONS.forEachIndexed { index, region ->
                        if (index > 0) FutureDivider(theme = theme)
                        FutureSettingItem(
                            title = region.displayName,
                            theme = theme,
                            showChevron = false,
                            onClick = { onSelectRegion(region) },
                            trailing = {
                                if (region.id == currentRegion.id) {
                                    Icon(FutureIcons.Check, contentDescription = "נבחר", tint = theme.readableAccentColor, modifier = Modifier.size(FutureDimens.iconSettingRow))
                                }
                            },
                        )
                    }
                }
            }
        }
    }
}
