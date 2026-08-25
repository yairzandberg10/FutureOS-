package com.future.calendar.ui

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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.future.calendar.data.CalendarEvent
import com.future.calendar.data.DafYomi
import com.future.calendar.data.DayZmanim
import com.future.calendar.data.HebrewDateFormatter
import com.future.calendar.data.HebrewNumerals
import com.future.calendar.ui.theme.FutureTheme
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
                        fontSize = 11.sp,
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
                label = "tabBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bgColor)
                    .clickable(interactionSource = interactionSource, indication = null, onClick = { onSelect(mode) })
                    .focusable(interactionSource = interactionSource)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) Color.Black else theme.textColor
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
        Text("כדי להציג את לוח השנה צריך לאשר הרשאה", color = theme.textColor.copy(alpha = 0.7f), fontSize = 15.sp, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        val interactionSource = remember { MutableInteractionSource() }
        val isFocused by interactionSource.collectIsFocusedAsState()
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(if (isFocused) theme.accentColor else theme.accentColor.copy(alpha = 0.7f))
                .clickable(interactionSource = interactionSource, indication = null, onClick = onRequestPermission)
                .focusable(interactionSource = interactionSource)
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("אשר הרשאה", color = Color.Black, fontWeight = FontWeight.Bold)
        }
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
            Text(primaryLabel, textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
            if (secondaryLabel.isNotBlank()) {
                Text(secondaryLabel, textAlign = TextAlign.Center, fontSize = 11.sp, color = theme.textColor.copy(alpha = 0.5f))
            }
        }
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextMonth)
    }
}

@Composable
private fun WeekDayHeaderRow(theme: FutureTheme) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
        HEBREW_WEEKDAY_LABELS.forEach { label ->
            Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 12.sp, color = theme.textColor.copy(alpha = 0.5f))
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
            .focusable()
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val current = focusedIndex
                when (event.key) {
                    Key.DirectionRight -> {
                        val next = current - 1
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true } else false
                    }
                    Key.DirectionLeft -> {
                        val next = current + 1
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true } else false
                    }
                    Key.DirectionDown -> {
                        val next = current + 7
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true } else false
                    }
                    Key.DirectionUp -> {
                        val next = current - 7
                        if (next in cells.indices) { focusedIndex = next; cells[next]?.let(onSelectDate); true } else false
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
    val shape = RoundedCornerShape(14.dp)
    // ספרור עברי (ט״ו וכו') במקום מספר גרגוריאני לועזי - זה בדיוק ה"אותיות
    // עבריות" שהמשתמש ביקש, לא רק שם חודש עברי בתת-כותרת כמו שהיה קודם.
    val dayLabel = if (useHebrewCalendar) {
        HebrewNumerals.toHebrew(HebrewDateFormatter.hebrewDayOfMonth(date))
    } else {
        date.dayOfMonth.toString()
    }
    val bgColor by animateColorAsState(
        if (isSelected && !isFocused) theme.textColor.copy(alpha = 0.08f) else Color.Transparent,
        label = "dayBg"
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(26.dp).clip(CircleShape).then(if (isToday) Modifier.background(theme.accentColor) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    dayLabel,
                    fontSize = if (useHebrewCalendar) 11.sp else 14.sp,
                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                    color = if (isToday) Color.Black else theme.textColor
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .size(width = 12.dp, height = 3.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(if (hasEvents) theme.accentColor.copy(alpha = 0.85f) else Color.Transparent)
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

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevWeek)
        Text(
            "${start.dayOfMonth} ${start.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)} – ${days.last().dayOfMonth} ${days.last().month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE)}",
            modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor
        )
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextWeek)
    }

    LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        items(days, key = { it.toEpochDay() }) { date ->
            WeekDayRow(date = date, isToday = date == today, events = eventsByDate[date] ?: emptyList(), theme = theme, onClick = { onOpenDay(date) })
        }
    }
}

@Composable
private fun WeekDayRow(date: LocalDate, isToday: Boolean, events: List<CalendarEvent>, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.16f) else theme.textColor.copy(alpha = 0.06f), label = "weekRowBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(38.dp).clip(CircleShape).then(if (isToday) Modifier.background(theme.accentColor) else Modifier.background(theme.textColor.copy(alpha = 0.08f))),
            contentAlignment = Alignment.Center
        ) {
            Text(date.dayOfMonth.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isToday) Color.Black else theme.textColor)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
            if (events.isEmpty()) {
                Text("אין אירועים", fontSize = 11.sp, color = theme.textColor.copy(alpha = 0.4f))
            } else {
                val shown = events.take(2)
                shown.forEach { Text("• ${it.title}", fontSize = 11.sp, color = theme.textColor.copy(alpha = 0.7f)) }
                if (events.size > shown.size) {
                    Text("+${events.size - shown.size} נוספים", fontSize = 11.sp, color = theme.textColor.copy(alpha = 0.4f))
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
    val dafYomi = remember(date) { DafYomi.forDate(date) }

    var forecast by remember(lat, lon) { mutableStateOf<Map<LocalDate, com.future.calendar.data.DailyWeather>?>(null) }
    LaunchedEffect(lat, lon, showWeather) {
        if (showWeather) {
            forecast = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                com.future.calendar.data.WeatherService.fetchForecast(lat, lon)
            }
        }
    }
    val todayWeather = forecast?.get(date)

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevDay)
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(date.dayOfWeek.getDisplayName(JavaTextStyle.FULL, HE_LOCALE), fontSize = 12.sp, color = theme.textColor.copy(alpha = 0.5f))
                Text(
                    "${date.dayOfMonth} ב${date.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)}",
                    fontSize = 16.sp, fontWeight = FontWeight.Bold, color = theme.textColor
                )
                if (hebrewDate.isNotBlank()) Text(hebrewDate, fontSize = 11.sp, color = theme.accentColor)
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
                        Text("אין אירועים ביום זה", color = theme.textColor.copy(alpha = 0.5f), fontSize = 14.sp)
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    EventRow(event = event, theme = theme, onClick = { onEditEvent(event) }, onMenu = { menuFor = event })
                }
            }
        }
    }
}

@Composable
private fun ZmanimPanel(zmanim: DayZmanim, theme: FutureTheme) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.textColor.copy(alpha = 0.06f)).padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WbTwilight, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("זמני היום", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor.copy(alpha = 0.7f))
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
                        Text(label, fontSize = 10.sp, color = theme.textColor.copy(alpha = 0.5f))
                        Text("%02d:%02d".format(time.hour, time.minute), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
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
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.accentColor.copy(alpha = 0.12f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.MenuBook, contentDescription = null, tint = theme.accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text("דף יומי", fontSize = 10.sp, color = theme.textColor.copy(alpha = 0.5f))
            Text("$masechet דף ${com.future.calendar.data.HebrewNumerals.toHebrew(daf)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
        }
    }
}

@Composable
private fun WeatherPanel(weather: com.future.calendar.data.DailyWeather, theme: FutureTheme) {
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.textColor.copy(alpha = 0.06f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(weather.emoji, fontSize = 26.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(weather.description, fontSize = 12.sp, color = theme.textColor.copy(alpha = 0.6f))
            val tempLabel = if (weather.currentTemp != null) {
                "${weather.currentTemp.toInt()}° (מקסימום ${weather.maxTemp.toInt()}° · מינימום ${weather.minTemp.toInt()}°)"
            } else {
                "מקסימום ${weather.maxTemp.toInt()}° · מינימום ${weather.minTemp.toInt()}°"
            }
            Text(tempLabel, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
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

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        FocusableIconButton(icon = Icons.Rounded.ChevronRight, theme = theme, onClick = onPrevYear)
        Text(year.toString(), modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        FocusableIconButton(icon = Icons.Rounded.ChevronLeft, theme = theme, onClick = onNextYear)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp)
            .onFocusChanged { isGridFocused = it.isFocused }
            .focusable()
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
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.06f))
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(month.month.getDisplayName(JavaTextStyle.SHORT, HE_LOCALE), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor)
        if (eventDayCount > 0) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("$eventDayCount ימים עם אירועים", fontSize = 9.sp, color = theme.textColor.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, theme: FutureTheme, onClick: () -> Unit, onMenu: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.16f) else theme.textColor.copy(alpha = 0.06f), label = "eventRowBg")
    val timeLabel = if (event.allDay) "כל היום" else {
        val start = java.time.Instant.ofEpochMilli(event.startMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        val end = java.time.Instant.ofEpochMilli(event.endMillis).atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        "%02d:%02d–%02d:%02d".format(start.hour, start.minute, end.hour, end.minute)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event2 ->
                if (isFocused && event2.type == KeyEventType.KeyUp && (event2.key == Key.Menu || event2.key == Key.Settings)) {
                    onMenu(); true
                } else false
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val stripeColor = if (event.color != 0) Color(event.color) else theme.accentColor
        Box(modifier = Modifier.width(4.dp).height(36.dp).clip(RoundedCornerShape(2.dp)).background(stripeColor))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, color = theme.textColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(timeLabel, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp)
            if (event.location.isNotBlank()) {
                Text(event.location, color = theme.textColor.copy(alpha = 0.4f), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun EventOptionsMenu(event: CalendarEvent, theme: FutureTheme, onDismiss: () -> Unit, onEdit: () -> Unit, onDelete: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(theme.surfaceColor).padding(vertical = 8.dp)) {
            Text(event.title, color = theme.textColor.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
            MenuOptionRow("ערוך אירוע", Icons.Rounded.Edit, theme = theme, onClick = onEdit)
            MenuOptionRow("מחק אירוע", Icons.Rounded.Delete, theme = theme, onClick = onDelete, isDestructive = true)
        }
    }
}

@Composable
private fun MenuOptionRow(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, onClick: () -> Unit, isDestructive: Boolean = false) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.12f) else Color.Transparent, label = "menuRowBg")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Text(label, color = if (isDestructive) Color(0xFFFF6B6B) else theme.textColor, fontSize = 15.sp)
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

    Dialog(onDismissRequest = onDismiss) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(theme.surfaceColor)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(if (editingEvent != null) "עריכת אירוע" else "אירוע חדש", color = theme.textColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${initialDate.dayOfMonth} ב${initialDate.month.getDisplayName(JavaTextStyle.FULL, HE_LOCALE)}",
                    color = theme.textColor.copy(alpha = 0.5f),
                    fontSize = 12.sp
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

                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditDialogButton("ביטול", theme.textColor.copy(alpha = 0.15f), theme.textColor, onDismiss)
                    EditDialogButton(if (editingEvent != null) "שמור" else "צור", theme.accentColor, Color.Black) {
                        onSave(title, description, location, startHour, startMinute, endHour, endMinute, allDay)
                    }
                    if (onDelete != null) {
                        EditDialogButton("מחק", Color(0xFFFF6B6B).copy(alpha = 0.2f), Color(0xFFFF6B6B), onDelete)
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
            .clip(RoundedCornerShape(12.dp))
            .background(if (isFocused) theme.textColor.copy(alpha = 0.1f) else Color.Transparent)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("כל היום", color = theme.textColor, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (allDay) theme.accentColor else theme.textColor.copy(alpha = 0.2f)),
            contentAlignment = if (allDay) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.padding(3.dp).size(18.dp).clip(CircleShape).background(if (allDay) Color.Black else theme.textColor))
        }
    }
}

@Composable
private fun TimeStepperField(label: String, hour: Int, minute: Int, theme: FutureTheme, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .clip(shape)
            .background(theme.textColor.copy(alpha = 0.06f))
            .then(if (isFocused) Modifier.background(theme.accentColor.copy(alpha = 0.12f)) else Modifier)
            .focusable(interactionSource = interactionSource)
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> { onMinuteChange(15); true }
                    Key.DirectionDown -> { onMinuteChange(-15); true }
                    Key.DirectionRight -> { onHourChange(-1); true }
                    Key.DirectionLeft -> { onHourChange(1); true }
                    else -> false
                }
            }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
        Text("%02d:%02d".format(hour, minute), color = theme.textColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun EditField(label: String, value: String, theme: FutureTheme, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(label, color = theme.textColor.copy(alpha = 0.5f), fontSize = 11.sp)
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = theme.textColor, fontSize = 14.sp),
            cursorBrush = SolidColor(theme.accentColor),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.textColor.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
                .padding(10.dp)
        )
    }
}

@Composable
private fun EditDialogButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 20.dp, vertical = 10.dp)
    ) {
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun FocusableIconButton(icon: androidx.compose.ui.graphics.vector.ImageVector, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(if (isFocused) 1.15f else 1f, label = "iconScale")
    val bgColor by animateColorAsState(
        if (isFocused) theme.accentColor.copy(alpha = 0.3f) else theme.textColor.copy(alpha = 0.08f),
        label = "iconBg"
    )

    Box(
        modifier = Modifier
            .size(40.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor, modifier = Modifier.size(20.dp))
    }
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
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                FocusableIconButton(icon = Icons.AutoMirrored.Rounded.ArrowForward, theme = theme, onClick = onBack)
                Text("הגדרות לוח שנה", modifier = Modifier.padding(start = 8.dp), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = theme.textColor)
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
    Text(text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = theme.textColor.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp))
}

@Composable
private fun SettingsToggleRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, sublabel: String, checked: Boolean, theme: FutureTheme, onToggle: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    val bgColor by animateColorAsState(if (isFocused) theme.textColor.copy(alpha = 0.16f) else theme.textColor.copy(alpha = 0.06f), label = "settingsRowBg")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onToggle)
            .focusable(interactionSource = interactionSource)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = theme.textColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, color = theme.textColor)
            Text(sublabel, fontSize = 11.sp, color = theme.textColor.copy(alpha = 0.5f))
        }
        Box(
            modifier = Modifier
                .size(width = 44.dp, height = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (checked) theme.accentColor else theme.textColor.copy(alpha = 0.2f)),
            contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(modifier = Modifier.padding(3.dp).size(18.dp).clip(CircleShape).background(if (checked) Color.Black else theme.textColor))
        }
    }
}

@Composable
private fun RegionRow(region: com.future.calendar.data.Region, isSelected: Boolean, theme: FutureTheme, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val bgColor by animateColorAsState(
        when {
            isFocused -> theme.textColor.copy(alpha = 0.16f)
            isSelected -> theme.accentColor.copy(alpha = 0.15f)
            else -> theme.textColor.copy(alpha = 0.06f)
        },
        label = "regionRowBg"
    )
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bgColor)
            .then(if (isFocused) Modifier.border(width = 2.dp, color = theme.accentColor, shape = shape) else Modifier)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .focusable(interactionSource = interactionSource)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = if (isSelected) theme.accentColor else theme.textColor.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(region.displayName, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = theme.textColor)
    }
}
