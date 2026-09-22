package com.future.calendar.ui
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WbTwilight
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
    onOpenMonth: (YearMonth) -> Unit,
    onAddEvent: () -> Unit,
    onEditEvent: (CalendarEvent) -> Unit,
    onDeleteEvent: (CalendarEvent) -> Unit,
    onGoToday: () -> Unit,
    onOpenSettings: () -> Unit,
    resolvedLat: Double,
    resolvedLon: Double,
    showWeather: Boolean,
    useHebrewCalendar: Boolean = false,
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
                    FocusableIconButton(icon = Icons.Rounded.Settings, theme = theme, onClick = onOpenSettings)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FocusableIconButton(icon = Icons.Rounded.Today, theme = theme, onClick = onGoToday)
                        FocusableIconButton(icon = Icons.Rounded.Add, theme = theme, onClick = onAddEvent)
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

                ViewModeTabRow(current = viewMode, theme = theme, onSelect = onChangeViewMode)

                if (!hasPermission) {
                    PermissionRequiredMessage(theme = theme, onRequestPermission = onRequestPermission)
                    return@Column
                }

                when (viewMode) {
                    CalendarViewMode.MONTH -> {
                        MonthNavigationRow(month = month, theme = theme, useHebrewCalendar = useHebrewCalendar, onPrevMonth = onPrevMonth, onNextMonth = onNextMonth)
                        WeekDayHeaderRow(theme = theme)
                        MonthGrid(
                            month = month,
                            selectedDate = selectedDate,
                            today = today,
                            eventsByDate = eventsByDate,
                            theme = theme,
                            useHebrewCalendar = useHebrewCalendar,
                            onPrevMonth = onPrevMonth,
                            onNextMonth = onNextMonth,
                            onSelectDate = onSelectDate,
                            onOpenDay = onOpenDay
                        )
                    }
                    CalendarViewMode.WEEK -> WeekView(
                        selectedDate = selectedDate,
                        today = today,
                        eventsByDate = eventsByDate,
                        theme = theme,
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
                        showWeather = showWeather
                    )
                    CalendarViewMode.YEAR -> YearView(
                        year = month.year,
                        eventsByDate = eventsByDate,
                        theme = theme,
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
private fun ViewModeTabRow(current: CalendarViewMode, theme: FutureTheme, onSelect: (CalendarViewMode) -> Unit) {
    val tabs = listOf(
        CalendarViewMode.DAY to "יום",
        CalendarViewMode.WEEK to "שבוע",
        CalendarViewMode.MONTH to "חודש",
        CalendarViewMode.YEAR to "שנה"
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        tabs.forEach { (mode, label) ->
            val interactionSource = remember { MutableInteractionSource() }
            val isFocused by interactionSource.collectIsFocusedAsState()
            val isSelected = mode == current
            val bgColor by animateColorAsState(
                when {
                    isSelected -> theme.accentColor
                    isFocused -> theme.textColor.copy(alpha = 0.18f)
                    else -> theme.textColor.copy(alpha = 0.08f)
                },
                FutureMotion.focusColorSpec,
                label = "tabBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(FutureShapes.md)
                    .background(bgColor)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(mode) })
                    .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = FutureTypography.summary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) theme.onReadableAccentColor else theme.textColor
                )
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
private fun MonthNavigationRow(month: YearMonth, theme: FutureTheme, useHebrewCalendar: Boolean = false, onPrevMonth: () -> Unit, onNextMonth: () -> Unit) {
    val gregLabel = "${month.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)} ${month.year}"
    val hebrewLabel = HebrewDateFormatter.formatMonthYear(month.atDay(15))
    // כשהמצב העברי דלוק, התאריך העברי הוא הראשי (גדול/מודגש) והלועזי המשני -
    // הפוך מהמצב הרגיל, כדי שבאמת "לוח עברי" יהיה מה שהעין קולטת קודם.
    val primaryLabel = if (useHebrewCalendar) hebrewLabel else gregLabel
    val secondaryLabel = if (useHebrewCalendar) gregLabel else hebrewLabel
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevMonth)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(primaryLabel, textAlign = TextAlign.Center, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold, color = theme.textColor)
            if (secondaryLabel.isNotBlank()) {
                Text(secondaryLabel, textAlign = TextAlign.Center, fontSize = FutureTypography.caption, color = theme.mutedTextColor)
            }
        }
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextMonth)
    }
}

@Composable
private fun WeekDayHeaderRow(theme: FutureTheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        HEBREW_WEEKDAY_LABELS.forEach { label ->
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.label, color = theme.mutedTextColor)
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    useHebrewCalendar: Boolean = false,
    onPrevMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectDate: (LocalDate) -> Unit,
    onOpenDay: (LocalDate) -> Unit
) {
    val firstOfMonth = month.atDay(1)
    val leadingBlanks = firstOfMonth.sundayStartColumn()
    val daysInMonth = month.lengthOfMonth()
    val totalCells = (((leadingBlanks + daysInMonth + 6) / 7) * 7).coerceAtLeast(35)
    val cells: List<LocalDate?> = (0 until totalCells).map { i ->
        val dayNum = i - leadingBlanks + 1
        if (dayNum in 1..daysInMonth) month.atDay(dayNum) else null
    }

    var focusedIndex by remember(month) {
        mutableIntStateOf(cells.indexOf(selectedDate).let { if (it >= 0) it else leadingBlanks })
    }
    var isGridFocused by remember(month) { mutableStateOf(false) }
    val focusRequester = remember(month) { FocusRequester() }

    LaunchedEffect(month) { focusRequester.requestFocus() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { isGridFocused = it.isFocused }
            .focusable().bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val current = focusedIndex
                when (event.key) {
                    Key.DirectionRight -> {
                        val next = current - 1
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true }
                        else { onNextMonth(); true }
                    }
                    Key.DirectionLeft -> {
                        val next = current + 1
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true }
                        else { onPrevMonth(); true }
                    }
                    Key.DirectionDown -> {
                        val next = current + 7
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true }
                        else { onNextMonth(); true }
                    }
                    Key.DirectionUp -> {
                        val next = current - 7
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true }
                        // בשורה העליונה משאירים את מקש למעלה לצאת מהרשת, כדי שאפשר יהיה להגיע
                        // לכפתורים מעל (הגדרות/היום/הוספה, טאבים, ניווט חודש) - לא "בולעים" אותו כמו שאר הכיוונים
                        else false
                    }
                    Key.DirectionCenter, Key.Enter -> {
                        cells.getOrNull(current)?.let(onOpenDay)
                        true
                    }
                    else -> false
                }
            }
    ) {
        val rows = cells.chunked(7)
        rows.forEachIndexed { rowIndex, rowCells ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowCells.forEachIndexed { colIndex, date ->
                    val cellIndex = rowIndex * 7 + colIndex
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        if (date != null) {
                            DayCell(
                                date = date,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                isFocused = isGridFocused && cellIndex == focusedIndex,
                                hasEvents = eventsByDate[date]?.isNotEmpty() == true,
                                theme = theme,
                                useHebrewCalendar = useHebrewCalendar
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, isToday: Boolean, isSelected: Boolean, isFocused: Boolean, hasEvents: Boolean, theme: FutureTheme, useHebrewCalendar: Boolean = false) {
    val shape = FutureShapes.sm
    // ספרור עברי (ט״ו וכו') במקום מספר גרגוריאני לועזי - זה בדיוק ה"אותיות
    // עבריות" שהמשתמש ביקש, לא רק שם חודש עברי בתת-כותרת כמו שהיה קודם.
    val dayLabel = if (useHebrewCalendar) {
        HebrewNumerals.toHebrew(HebrewDateFormatter.hebrewDayOfMonth(date))
    } else {
        date.dayOfMonth.toString()
    }
    val bgColor by animateColorAsState(
        if (isSelected && !isFocused) theme.textColor.copy(alpha = 0.08f) else Color.Transparent,
        FutureMotion.focusColorSpec,
        label = "dayBg"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).then(if (isToday) Modifier.background(theme.readableAccentColor) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLabel,
                    fontSize = if (useHebrewCalendar) FutureTypography.caption else FutureTypography.body,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) theme.onReadableAccentColor else theme.textColor
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(width = 12.dp, height = 3.dp)
                    .clip(FutureShapes.xs)
                    .background(if (hasEvents) theme.readableAccentColor else Color.Transparent)
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
        FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevWeek)
        Text(
            "${start.dayOfMonth} ${start.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)} – ${days.last().dayOfMonth} ${days.last().month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)}",
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.SemiBold, color = theme.textColor
        )
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextWeek)
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(days, key = { it.toEpochDay() }) { date ->
            WeekDayRow(
                date = date,
                isToday = date == today,
                events = eventsByDate[date] ?: emptyList(),
                theme = theme,
                onClick = { onOpenDay(date) },
                focusRequester = if (date == days.first()) firstRowFocusRequester else null
            )
        }
    }
}

@Composable
private fun WeekDayRow(date: LocalDate, isToday: Boolean, events: List<CalendarEvent>, theme: FutureTheme, onClick: () -> Unit, focusRequester: FocusRequester? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.sm
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
            Text(date.dayOfMonth.toString(), fontSize = FutureTypography.body, fontWeight = FontWeight.Bold, color = if (isToday) theme.onReadableAccentColor else theme.textColor)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE), fontSize = FutureTypography.summary, fontWeight = FontWeight.SemiBold, color = theme.textColor)
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
    showWeather: Boolean
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
            FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevDay, focusRequester = prevDayFocusRequester)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE), fontSize = FutureTypography.label, color = theme.mutedTextColor)
                Text(
                    "${date.dayOfMonth} ב${date.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)}",
                    fontSize = FutureTypography.bodyLarge, fontWeight = FontWeight.Bold, color = theme.textColor
                )
                if (hebrewDate.isNotBlank()) Text(hebrewDate, fontSize = FutureTypography.caption, color = theme.accentColor)
            }
            FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextDay)
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
            Icon(Icons.Rounded.WbTwilight, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
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
        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
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
        Text(weather.emoji, fontSize = FutureTypography.headline)
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

@Composable
private fun YearView(
    year: Int,
    eventsByDate: Map<LocalDate, List<CalendarEvent>>,
    theme: FutureTheme,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onOpenMonth: (YearMonth) -> Unit
) {
    val months = (1..12).map { YearMonth.of(year, it) }
    var focusedIndex by remember(year) { mutableIntStateOf(0) }
    var isGridFocused by remember(year) { mutableStateOf(false) }
    // בלי פוקוס התחלתי מפורש, הרשת הזו נשארת בלי שום פריט מודגש ב-D-pad עד
    // שהמשתמש לוחץ כיוון כלשהו - בדיוק כמו ב-MonthGrid.
    val gridFocusRequester = remember(year) { FocusRequester() }
    LaunchedEffect(year) { gridFocusRequester.requestFocus() }

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevYear)
        Text(year.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = FutureTypography.title, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextYear)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .focusRequester(gridFocusRequester)
            .onFocusChanged { isGridFocused = it.isFocused }
            .focusable().bringIntoViewOnFocus()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val current = focusedIndex
                when (event.key) {
                    Key.DirectionRight -> { val n = current - 1; if (n in months.indices) { focusedIndex = n; true } else false }
                    Key.DirectionLeft -> { val n = current + 1; if (n in months.indices) { focusedIndex = n; true } else false }
                    Key.DirectionDown -> { val n = current + 3; if (n in months.indices) { focusedIndex = n; true } else false }
                    Key.DirectionUp -> { val n = current - 3; if (n in months.indices) { focusedIndex = n; true } else false }
                    Key.DirectionCenter, Key.Enter -> { onOpenMonth(months[current]); true }
                    else -> false
                }
            }
    ) {
        months.chunked(3).forEachIndexed { rowIndex, rowMonths ->
            Row(modifier = Modifier.fillMaxWidth()) {
                rowMonths.forEachIndexed { colIndex, ym ->
                    val idx = rowIndex * 3 + colIndex
                    val count = eventsByDate.entries.count { it.key.year == ym.year && it.key.monthValue == ym.monthValue && it.value.isNotEmpty() }
                    MonthMiniCard(
                        month = ym,
                        eventDayCount = count,
                        isFocused = isGridFocused && idx == focusedIndex,
                        theme = theme,
                        modifier = Modifier.weight(1f).padding(4.dp),
                        onClick = { onOpenMonth(ym) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthMiniCard(month: YearMonth, eventDayCount: Int, isFocused: Boolean, theme: FutureTheme, modifier: Modifier, onClick: () -> Unit) {
    val shape = FutureShapes.sm
    Column(
        modifier = modifier
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.06f))
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(month.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE), fontSize = FutureTypography.summary, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        if (eventDayCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("$eventDayCount ימים עם אירועים", fontSize = FutureTypography.caption, color = theme.mutedTextColor)
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, theme: FutureTheme, onClick: () -> Unit, onMenu: () -> Unit, onFocusChanged: (Boolean) -> Unit = {}) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    LaunchedEffect(isFocused) { onFocusChanged(isFocused) }
    val shape = FutureShapes.sm
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
        FutureMenuRow("ערוך אירוע", Icons.Rounded.Edit, theme, onEdit)
        FutureMenuRow("מחק אירוע", Icons.Rounded.Delete, theme, onDelete, destructive = true)
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

@Composable
fun CalendarSettingsScreen(
    useGps: Boolean,
    hasLocationPermission: Boolean,
    currentRegion: com.future.calendar.data.Region,
    showWeather: Boolean,
    useHebrewCalendar: Boolean,
    theme: FutureTheme,
    onBack: () -> Unit,
    onToggleGps: () -> Unit,
    onSelectRegion: (com.future.calendar.data.Region) -> Unit,
    onToggleShowWeather: () -> Unit,
    onToggleHebrewCalendar: () -> Unit
) {
    // בלי פוקוס התחלתי מפורש, מסך ההגדרות נשאר בלי שום פריט מודגש ב-D-pad -
    // בשונה מ-MonthGrid שכן קובע פוקוס אוטומטי.
    val backFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { backFocusRequester.requestFocus() }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                // Icons.AutoMirrored.Rounded.ArrowBack (לא ArrowForward!) - תחת
                // LayoutDirection.Rtl הכפוי, AutoMirrored הופך אותו לחץ ימינה כראוי לכפתור "חזור".
                FocusableIconButton(icon = Icons.AutoMirrored.Rounded.ArrowBack, theme = theme, onClick = onBack, focusRequester = backFocusRequester)
                Text("הגדרות לוח שנה", modifier = Modifier.padding(start = 8.dp), fontSize = FutureTypography.title, fontWeight = FontWeight.Bold, color = theme.textColor)
            }

            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item { SettingsSectionLabel("מיקום לזמני היום ותחזית", theme) }
                item {
                    SettingsToggleRow(
                        icon = Icons.Rounded.MyLocation,
                        label = "שימוש במיקום GPS",
                        sublabel = if (useGps && !hasLocationPermission) "צריך לאשר הרשאת מיקום" else "מיקום המכשיר בפועל, במקום עיר קבועה",
                        checked = useGps,
                        theme = theme,
                        onToggle = onToggleGps
                    )
                }
                if (!useGps) {
                    items(com.future.calendar.data.KNOWN_REGIONS) { region ->
                        RegionRow(region = region, isSelected = region.id == currentRegion.id, theme = theme, onClick = { onSelectRegion(region) })
                    }
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }
                item { SettingsSectionLabel("תצוגה", theme) }
                item {
                    SettingsToggleRow(
                        icon = Icons.Rounded.WbTwilight,
                        label = "הצג תחזית מזג אוויר",
                        sublabel = "במסך תצוגת היום",
                        checked = showWeather,
                        theme = theme,
                        onToggle = onToggleShowWeather
                    )
                }
                item {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Translate,
                        label = "לוח עברי באותיות עבריות",
                        sublabel = "מספרי הימים בלוח (למשל ט״ו) לפי החודש העברי, במקום תאריך לועזי",
                        checked = useHebrewCalendar,
                        theme = theme,
                        onToggle = onToggleHebrewCalendar
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionLabel(text: String, theme: FutureTheme) {
    Text(text, fontSize = FutureTypography.label, fontWeight = FontWeight.SemiBold, color = theme.mutedTextColor, modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp))
}

@Composable
private fun SettingsToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sublabel: String, checked: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = FutureShapes.sm
    val bgColor by animateColorAsState(if (isFocused) theme.readableAccentColor.copy(alpha = 0.14f) else theme.idleChipColor, FutureMotion.focusColorSpec, label = "settingsRowBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusMotion(interactionSource)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = FutureTypography.body, color = theme.textColor)
            Text(sublabel, fontSize = FutureTypography.caption, color = theme.mutedTextColor)
        }
        FutureSwitch(checked = checked, theme = theme)
    }
}

@Composable
private fun RegionRow(region: com.future.calendar.data.Region, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            isFocused -> theme.readableAccentColor.copy(alpha = 0.14f)
            isSelected -> theme.readableAccentColor.copy(alpha = 0.15f)
            else -> theme.textColor.copy(alpha = 0.06f)
        },
        FutureMotion.focusColorSpec,
        label = "regionRowBg"
    )
    val shape = FutureShapes.sm
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .focusMotion(interactionSource)
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = FutureDimens.focusBorderItem, color = theme.readableAccentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource).bringIntoViewOnFocus()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = if (isSelected) theme.readableAccentColor else theme.mutedTextColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(region.displayName, fontSize = FutureTypography.body, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = theme.textColor)
    }
}
