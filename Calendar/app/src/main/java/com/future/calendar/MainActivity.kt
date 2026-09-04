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
import com.future.calendar.data.LocationHelper
import com.future.sharednav.theme.ThemeClient
import com.future.calendar.ui.CalendarHomeScreen
import com.future.calendar.ui.CalendarSettingsScreen
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
            var showSettings by remember { mutableStateOf(false) }

            var useGps by remember { mutableStateOf(CalendarSettings.getUseGps(this@MainActivity)) }
            var region by remember { mutableStateOf(CalendarSettings.getRegion(this@MainActivity)) }
            var showWeather by remember { mutableStateOf(CalendarSettings.getShowWeather(this@MainActivity)) }
            var useHebrewCalendar by remember { mutableStateOf(CalendarSettings.getUseHebrewCalendar(this@MainActivity)) }
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

            fun refreshEvents() {
                if (!hasPermission) return
                val rangeStart: LocalDate
                val rangeEnd: LocalDate
                if (viewMode == CalendarViewMode.YEAR) {
                    rangeStart = YearMonth.of(currentMonth.year, 1).atDay(1)
                    rangeEnd = YearMonth.of(currentMonth.year, 12).atEndOfMonth()
                } else {
                    rangeStart = currentMonth.atDay(1).minusDays(7)
                    rangeEnd = currentMonth.atEndOfMonth().plusDays(7)
                }
                val startMillis = rangeStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                val endMillis = rangeEnd.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
                events = repository.getEventsInRange(startMillis, endMillis)
            }

            LaunchedEffect(hasPermission, currentMonth, viewMode) { refreshEvents() }

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

            BackHandler(enabled = editorState != null || showSettings || viewMode != CalendarViewMode.MONTH) {
                when {
                    editorState != null -> editorState = null
                    showSettings -> showSettings = false
                    else -> viewMode = CalendarViewMode.MONTH
                }
            }

            fun goToDate(date: LocalDate) {
                selectedDate = date
                currentMonth = YearMonth.from(date)
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
                if (showSettings) {
                    CalendarSettingsScreen(
                        useGps = useGps,
                        hasLocationPermission = hasLocationPermission,
                        currentRegion = region,
                        showWeather = showWeather,
                        useHebrewCalendar = useHebrewCalendar,
                        theme = theme,
                        onBack = { showSettings = false },
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
                        onToggleShowWeather = {
                            val newValue = !showWeather
                            showWeather = newValue
                            CalendarSettings.setShowWeather(this@MainActivity, newValue)
                        },
                        onToggleHebrewCalendar = {
                            val newValue = !useHebrewCalendar
                            useHebrewCalendar = newValue
                            CalendarSettings.setUseHebrewCalendar(this@MainActivity, newValue)
                        }
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
                        onPrevMonth = { currentMonth = currentMonth.minusMonths(1) },
                        onNextMonth = { currentMonth = currentMonth.plusMonths(1) },
                        onPrevDay = { goToDate(selectedDate.minusDays(1)) },
                        onNextDay = { goToDate(selectedDate.plusDays(1)) },
                        onPrevWeek = { goToDate(selectedDate.minusWeeks(1)) },
                        onNextWeek = { goToDate(selectedDate.plusWeeks(1)) },
                        onPrevYear = { currentMonth = currentMonth.minusYears(1) },
                        onNextYear = { currentMonth = currentMonth.plusYears(1) },
                        onSelectDate = { selectedDate = it },
                        onOpenDay = { goToDate(it); viewMode = CalendarViewMode.DAY },
                        onOpenMonth = { currentMonth = it; viewMode = CalendarViewMode.MONTH },
                        onAddEvent = { editorState = selectedDate to null },
                        onEditEvent = { editorState = selectedDate to it },
                        onDeleteEvent = {
                            repository.deleteEvent(it.id)
                            refreshEvents()
                        },
                        onGoToday = {
                            currentMonth = YearMonth.from(today)
                            selectedDate = today
                            viewMode = CalendarViewMode.DAY
                        },
                        onOpenSettings = { showSettings = true },
                        resolvedLat = resolvedLatLon.first,
                        resolvedLon = resolvedLatLon.second,
                        showWeather = showWeather,
                        useHebrewCalendar = useHebrewCalendar,
                        usingFallbackLocation = usingFallbackLocation
                    )
                }
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
                        {
                            repository.deleteEvent(editing.id)
                            editorState = null
                            refreshEvents()
                        }
                    } else null
                )
            }
        }
    }
}
