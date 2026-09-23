package com.future.calendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.future.calendar.data.CalendarEvent
import com.future.calendar.data.CalendarRepository
import com.future.calendar.data.CalendarSettings
import com.future.calendar.data.CalendarKind
import com.future.calendar.data.CalendarMonths
import com.future.calendar.data.LocationHelper
import com.future.sharednav.components.ConfirmDialog
import com.future.sharednav.theme.ThemeClient
import com.future.calendar.ui.CalendarHomeScreen
import com.future.calendar.ui.CalendarSettingsScreen
import com.future.calendar.ui.CalendarLocationScreen
import com.future.calendar.ui.CalendarViewMode
import com.future.calendar.ui.EventEditDialog
import com.future.sharednav.theme.FutureTheme
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId

class MainActivity : ComponentActivity() {
    // המכשיר האמיתי הוא מקלדת T9 בלבד בלי מסך מגע - מבטלים קלט מגע לגמרי כדי
    // שההתנהגות תישאר תואמת לחומרה האמיתית. לא פוגע בניווט/הפעלה במקשים -
    // dispatchKeyEvent הוא נתיב נפרד לגמרי מ-dispatchTouchEvent.
    override fun dispatchTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val repository = remember { CalendarRepository(this) }
            val today = remember { LocalDate.now() }

            var hasPermission by remember { mutableStateOf(repository.hasCalendarPermission()) }
            var viewMode by remember { mutableStateOf(CalendarViewMode.MONTH) }
            var currentMonth by remember { mutableStateOf(YearMonth.from(today)) }
            var selectedDate by remember { mutableStateOf(today) }
            var events by remember { mutableStateOf<List<CalendarEvent>>(emptyList()) }
            var editorState by remember { mutableStateOf<Pair<LocalDate, CalendarEvent?>?>(null) }
            var pendingDelete by remember { mutableStateOf<CalendarEvent?>(null) }
            // 0 - לוח השנה, 1 - הגדרות, 2 - הגדרות > מיקום.
            var settingsPage by remember { mutableStateOf(0) }

            var useGps by remember { mutableStateOf(CalendarSettings.getUseGps(this@MainActivity)) }
            var region by remember { mutableStateOf(CalendarSettings.getRegion(this@MainActivity)) }
            var showWeather by remember { mutableStateOf(CalendarSettings.getShowWeather(this@MainActivity)) }
            var calendarKind by remember { mutableStateOf(CalendarSettings.getCalendarKind(this@MainActivity)) }
            var hasLocationPermission by remember { mutableStateOf(LocationHelper.hasPermission(this@MainActivity)) }

            val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                hasLocationPermission = result.values.any { it }
            }

            val gpsLatLon = remember(useGps, hasLocationPermission, region) {
                if (useGps && hasLocationPermission) LocationHelper.lastKnownLatLon(this@MainActivity) else null
            }
            val resolvedLatLon = gpsLatLon ?: (region.lat to region.lon)
            // כאשר המשתמש ביקש GPS ויש הרשאה אבל לא הצלחנו לקבל מיקום עדכני
            // (או שהמיקום השמור היה ישן מדי - ראה LocationHelper) - נופלים
            // חזרה לאזור ברירת המחדל ומציגים על כך חיווי קטן למשתמש כדי
            // שיידע שהזמנים המוצגים לא בהכרח תואמים את מיקומו האמיתי.
            val usingFallbackLocation = useGps && hasLocationPermission && gpsLatLon == null

            var theme by remember {
                mutableStateOf(
                    ThemeClient.getTheme(this@MainActivity).let {
                        FutureTheme(isDarkMode = it.isDarkMode, accentColor = Color(it.primaryColor))
                    }
                )
            }

            val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
                hasPermission = result.values.all { it }
            }

            // הטווח שנטען: שנה (עברית או לועזית) בתצוגת שנה, אחרת שלושה חודשים
            // סביב החודש הנוכחי - חודש עברי חוצה שני חודשים לועזיים.
            val eventRange = remember(viewMode, currentMonth, calendarKind, if (viewMode == CalendarViewMode.YEAR && calendarKind == CalendarKind.HEBREW) CalendarMonths.hebrewYearLabel(selectedDate) else "") {
                if (viewMode == CalendarViewMode.YEAR) {
                    if (calendarKind == CalendarKind.HEBREW) {
                        val months = CalendarMonths.hebrewYear(selectedDate)
                        months.first().first to months.last().last
                    } else {
                        YearMonth.of(currentMonth.year, 1).atDay(1) to YearMonth.of(currentMonth.year, 12).atEndOfMonth()
                    }
                } else {
                    currentMonth.minusMonths(1).atDay(1) to currentMonth.plusMonths(1).atEndOfMonth()
                }
            }

            fun refreshEvents() {
                if (!hasPermission) return
                val (rangeStart, rangeEnd) = eventRange
                val startMillis = rangeStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = rangeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                events = repository.getEventsInRange(startMillis, endMillis)
            }

            LaunchedEffect(hasPermission, eventRange) { refreshEvents() }

            val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                    if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                        hasPermission = repository.hasCalendarPermission()
                        val shared = ThemeClient.getTheme(this@MainActivity)
                        theme = FutureTheme(isDarkMode = shared.isDarkMode, accentColor = Color(shared.primaryColor))
                        refreshEvents()
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            LaunchedEffect(Unit) {
                if (!hasPermission) {
                    permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
                }
            }

            val eventsByDate = remember(events) { events.groupBy { it.startDate } }

            BackHandler(enabled = editorState != null || settingsPage != 0 || viewMode != CalendarViewMode.MONTH) {
                when {
                    editorState != null -> editorState = null
                    settingsPage != 0 -> settingsPage -= 1
                    else -> viewMode = CalendarViewMode.MONTH
                }
            }

            fun goToDate(date: LocalDate) {
                selectedDate = date
                currentMonth = YearMonth.from(date)
            }

            /** חודש קודם/הבא - באותו יום בחודש, לפי סוג הלוח. */
            fun shiftMonth(delta: Int) {
                if (calendarKind == CalendarKind.HEBREW) {
                    val current = CalendarMonths.hebrewContaining(selectedDate)
                    val dayIndex = java.time.temporal.ChronoUnit.DAYS.between(current.first, selectedDate)
                    val target = if (delta < 0) CalendarMonths.hebrewContaining(current.first.minusDays(1))
                    else CalendarMonths.hebrewContaining(current.last.plusDays(1))
                    goToDate(target.first.plusDays(dayIndex.coerceAtMost((target.length - 1).toLong())))
                } else {
                    val base = if (YearMonth.from(selectedDate) == currentMonth) selectedDate else currentMonth.atDay(1)
                    goToDate(base.plusMonths(delta.toLong()))
                }
            }

            fun shiftYear(delta: Int) {
                if (calendarKind == CalendarKind.HEBREW) {
                    val months = CalendarMonths.hebrewYear(selectedDate)
                    goToDate(if (delta < 0) months.first().first.minusDays(1) else months.last().last.plusDays(1))
                } else {
                    goToDate(selectedDate.plusYears(delta.toLong()))
                }
            }

            fun saveEvent(date: LocalDate, editing: CalendarEvent?, title: String, description: String, location: String, startHour: Int, startMinute: Int, endHour: Int, endMinute: Int, allDay: Boolean) {
                if (title.isBlank()) { editorState = null; return }
                val startMillis: Long
                val endMillis: Long
                if (allDay) {
                    startMillis = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                    endMillis = date.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
                } else {
                    startMillis = date.atTime(LocalTime.of(startHour, startMinute)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    var endLocal = date.atTime(LocalTime.of(endHour, endMinute))
                    if (!endLocal.isAfter(date.atTime(LocalTime.of(startHour, startMinute)))) endLocal = endLocal.plusDays(1)
                    endMillis = endLocal.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                }

                if (editing != null) {
                    repository.updateEvent(editing.id, title, description, location, startMillis, endMillis, allDay)
                } else {
                    val calendarId = repository.getDefaultWritableCalendarId()
                    if (calendarId != null) {
                        repository.addEvent(calendarId, title, description, location, startMillis, endMillis, allDay)
                    } else {
                        android.widget.Toast.makeText(this@MainActivity, "לא נמצא לוח שנה זמין לשמירה - האירוע לא נשמר", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                editorState = null
                refreshEvents()
            }

            Surface(modifier = Modifier.fillMaxSize(), color = theme.backgroundColor) {
                // הגדרות לוח השנה עמוקות ממסך הבית - החלקה פנימה/החוצה.
                com.future.sharednav.components.AnimatedScreenHost(
                    targetState = settingsPage,
                    depthOf = { it },
                ) { page ->
                if (page == 1) {
                    CalendarSettingsScreen(
                        kind = calendarKind,
                        locationSummary = if (useGps) "GPS" else region.displayName,
                        showWeather = showWeather,
                        theme = theme,
                        onSelectKind = {
                            calendarKind = it
                            CalendarSettings.setCalendarKind(this@MainActivity, it)
                        },
                        onOpenLocation = { settingsPage = 2 },
                        onToggleShowWeather = {
                            val newValue = !showWeather
                            showWeather = newValue
                            CalendarSettings.setShowWeather(this@MainActivity, newValue)
                        },
                    )
                } else if (page == 2) {
                    CalendarLocationScreen(
                        useGps = useGps,
                        hasLocationPermission = hasLocationPermission,
                        currentRegion = region,
                        theme = theme,
                        onToggleGps = {
                            val newValue = !useGps
                            useGps = newValue
                            CalendarSettings.setUseGps(this@MainActivity, newValue)
                            if (newValue && !hasLocationPermission) {
                                locationPermissionLauncher.launch(arrayOf(android.Manifest.permission.ACCESS_COARSE_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION))
                            }
                        },
                        onSelectRegion = {
                            region = it
                            CalendarSettings.setRegionId(this@MainActivity, it.id)
                        },
                    )
                } else {
                    CalendarHomeScreen(
                        viewMode = viewMode,
                        month = currentMonth,
                        selectedDate = selectedDate,
                        today = today,
                        eventsByDate = eventsByDate,
                        theme = theme,
                        hasPermission = hasPermission,
                        onRequestPermission = {
                            permissionLauncher.launch(arrayOf(android.Manifest.permission.READ_CALENDAR, android.Manifest.permission.WRITE_CALENDAR))
                        },
                        onChangeViewMode = { viewMode = it },
                        onPrevMonth = { shiftMonth(-1) },
                        onNextMonth = { shiftMonth(1) },
                        onPrevDay = { goToDate(selectedDate.minusDays(1)) },
                        onNextDay = { goToDate(selectedDate.plusDays(1)) },
                        onPrevWeek = { goToDate(selectedDate.minusWeeks(1)) },
                        onNextWeek = { goToDate(selectedDate.plusWeeks(1)) },
                        onPrevYear = { shiftYear(-1) },
                        onNextYear = { shiftYear(1) },
                        onSelectDate = { goToDate(it) },
                        onOpenDay = { goToDate(it); viewMode = CalendarViewMode.DAY },
                        onOpenMonth = { m ->
                            goToDate(if (selectedDate in m) selectedDate else if (today in m) today else m.first)
                            viewMode = CalendarViewMode.MONTH
                        },
                        onAddEvent = { editorState = selectedDate to null },
                        onEditEvent = { editorState = selectedDate to it },
                        onDeleteEvent = { pendingDelete = it },
                        onGoToday = {
                            currentMonth = YearMonth.from(today)
                            selectedDate = today
                            viewMode = CalendarViewMode.DAY
                        },
                        onOpenSettings = { settingsPage = 1 },
                        resolvedLat = resolvedLatLon.first,
                        resolvedLon = resolvedLatLon.second,
                        showWeather = showWeather,
                        kind = calendarKind,
                        usingFallbackLocation = usingFallbackLocation
                    )
                }
                }
            }

            // מחיקת אירוע היא בלתי הפיכה, ומגיעה משני מסלולים (תפריט האפשרויות
            // של אירוע ודיאלוג העריכה) - שניהם עוברים דרך אישור אחד כאן.
            pendingDelete?.let { event ->
                ConfirmDialog(
                    message = "למחוק את האירוע \"${event.title}\"?",
                    surfaceColor = theme.surfaceColor,
                    textColor = theme.textColor,
                    dangerColor = theme.dangerColor,
                    onCancel = { pendingDelete = null },
                    onConfirm = {
                        repository.deleteEvent(event.id)
                        pendingDelete = null
                        editorState = null
                        refreshEvents()
                    },
                )
            }

            editorState?.let { (date, editing) ->
                EventEditDialog(
                    initialDate = date,
                    editingEvent = editing,
                    theme = theme,
                    onDismiss = { editorState = null },
                    onSave = { title, description, location, startHour, startMinute, endHour, endMinute, allDay ->
                        saveEvent(date, editing, title, description, location, startHour, startMinute, endHour, endMinute, allDay)
                    },
                    onDelete = if (editing != null) {
                        { pendingDelete = editing }
                    } else null
                )
            }
        }
    }
}
