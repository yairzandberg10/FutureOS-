package com.future.clock.ui

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.future.sharednav.components.AnimatedScreenHost
import com.future.sharednav.components.EmptyState
import com.future.sharednav.components.FutureListItem
import com.future.sharednav.components.FutureMenuRow
import com.future.sharednav.components.FutureOptionsMenu
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.nav.onOptionsKeyPress
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.readableAccentColor
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class CityClock(val name: String, val timezone: String)

/** הערים שאפשר להוסיף. */
val WORLD_CITIES = listOf(
    CityClock("ירושלים", "Asia/Jerusalem"),
    CityClock("לונדון", "Europe/London"),
    CityClock("פריז", "Europe/Paris"),
    CityClock("ברלין", "Europe/Berlin"),
    CityClock("רומא", "Europe/Rome"),
    CityClock("מדריד", "Europe/Madrid"),
    CityClock("אמסטרדם", "Europe/Amsterdam"),
    CityClock("אתונה", "Europe/Athens"),
    CityClock("איסטנבול", "Europe/Istanbul"),
    CityClock("מוסקבה", "Europe/Moscow"),
    CityClock("קייב", "Europe/Kiev"),
    CityClock("קהיר", "Africa/Cairo"),
    CityClock("יוהנסבורג", "Africa/Johannesburg"),
    CityClock("דובאי", "Asia/Dubai"),
    CityClock("מומבאי", "Asia/Kolkata"),
    CityClock("בנגקוק", "Asia/Bangkok"),
    CityClock("סינגפור", "Asia/Singapore"),
    CityClock("הונג קונג", "Asia/Hong_Kong"),
    CityClock("בייג'ינג", "Asia/Shanghai"),
    CityClock("סיאול", "Asia/Seoul"),
    CityClock("טוקיו", "Asia/Tokyo"),
    CityClock("סידני", "Australia/Sydney"),
    CityClock("מלבורן", "Australia/Melbourne"),
    CityClock("אוקלנד", "Pacific/Auckland"),
    CityClock("הונולולו", "Pacific/Honolulu"),
    CityClock("לוס אנג'לס", "America/Los_Angeles"),
    CityClock("דנוור", "America/Denver"),
    CityClock("שיקגו", "America/Chicago"),
    CityClock("ניו יורק", "America/New_York"),
    CityClock("מיאמי", "America/New_York"),
    CityClock("טורונטו", "America/Toronto"),
    CityClock("מקסיקו סיטי", "America/Mexico_City"),
    CityClock("בואנוס איירס", "America/Argentina/Buenos_Aires"),
    CityClock("סאו פאולו", "America/Sao_Paulo"),
)

private val DEFAULT_CITY_NAMES = listOf("ירושלים", "לונדון", "ניו יורק", "טוקיו")

/** הערים שבחר המשתמש, בסדר שלו. */
private object WorldClockStore {
    private const val PREFS = "world_clock"
    private const val KEY = "cities"

    fun load(context: Context): List<CityClock> {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
        val names = stored?.split('|')?.filter { it.isNotBlank() } ?: DEFAULT_CITY_NAMES
        return names.mapNotNull { name -> WORLD_CITIES.firstOrNull { it.name == name } }
    }

    fun save(context: Context, cities: List<CityClock>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY, cities.joinToString("|") { it.name }).apply()
    }
}

/**
 * שעון עולמי: רשימה של ערים ששומרת את עצמה. חצים עוברים בין הערים (והרשימה
 * גוללת), מקש התפריט - הוסף עיר / מחק את העיר הממוקדת. ההוספה היא רשימת
 * ערים משלה; BACK חוזר ממנה.
 */
@Composable
fun WorldClockScreen(theme: FutureTheme, onBack: () -> Unit) {
    val context = LocalContext.current
    var cities by remember { mutableStateOf(WorldClockStore.load(context)) }
    var focusedIndex by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val firstFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000 - now % 1000)
        }
    }

    fun persist(list: List<CityClock>) {
        cities = list
        WorldClockStore.save(context, list)
    }

    onOptionsKeyPress { if (!picking) menuOpen = !menuOpen }
    BackHandler(enabled = picking) { picking = false }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AnimatedScreenHost(targetState = picking, depthOf = { if (it) 1 else 0 }) { showPicker ->
            if (showPicker) {
                CityPicker(
                    theme = theme,
                    exclude = cities,
                    onPick = { city ->
                        persist(cities + city)
                        focusedIndex = cities.lastIndex
                        picking = false
                    },
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        ToolsHeader(title = "שעון עולמי", theme = theme)
                        if (cities.isEmpty()) {
                            EmptyState(
                                icon = FutureIcons.Public,
                                title = "אין שעונים",
                                subtitle = "לחץ על מקש התפריט כדי להוסיף",
                                textColor = theme.textColor,
                            )
                        } else {
                            val listState = rememberLazyListState()
                            val requesters = remember(cities.size) { List(cities.size) { FocusRequester() } }
                            LaunchedEffect(cities.size) {
                                val target = focusedIndex.coerceIn(0, cities.lastIndex)
                                runCatching { requesters[target].requestFocus() }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            ) {
                                itemsIndexed(cities, key = { _, c -> c.name }) { index, city ->
                                    CityClockRow(
                                        city = city,
                                        now = now,
                                        theme = theme,
                                        focusRequester = requesters[index],
                                        modifier = Modifier.onFocusChanged { if (it.isFocused) focusedIndex = index },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (menuOpen) {
        FutureOptionsMenu(theme = theme, onDismissRequest = { menuOpen = false }, header = "שעון עולמי") {
            FutureMenuRow("הוסף עיר", FutureIcons.Add, theme, {
                menuOpen = false
                picking = true
            })
            cities.getOrNull(focusedIndex)?.let { city ->
                FutureMenuRow("מחק את ${city.name}", FutureIcons.Delete, theme, {
                    menuOpen = false
                    persist(cities - city)
                    focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
                }, destructive = true)
            }
        }
    }
}

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

/** "היום · +7 שע׳" - היום ביחס למקומי, והפרש השעות. */
private fun relativeLabel(zone: ZoneId, now: Long): String {
    val instant = Instant.ofEpochMilli(now)
    val here = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    val there = ZonedDateTime.ofInstant(instant, zone)
    val day = when (there.toLocalDate().compareTo(here.toLocalDate())) {
        0 -> "היום"
        in 1..Int.MAX_VALUE -> "מחר"
        else -> "אתמול"
    }
    val diffMinutes = (there.offset.totalSeconds - here.offset.totalSeconds) / 60
    if (diffMinutes == 0) return "$day · אותה שעה"
    val sign = if (diffMinutes > 0) "+" else "−"
    val abs = kotlin.math.abs(diffMinutes)
    val hours = if (abs % 60 == 0) "${abs / 60}" else "%d:%02d".format(abs / 60, abs % 60)
    return "$day · $sign$hours שע׳"
}

@Composable
private fun CityClockRow(city: CityClock, now: Long, theme: FutureTheme, focusRequester: FocusRequester, modifier: Modifier = Modifier) {
    val zone = remember(city.timezone) { ZoneId.of(city.timezone) }
    FutureListItem(
        title = city.name,
        summary = relativeLabel(zone, now),
        theme = theme,
        onClick = {},
        focusRequester = focusRequester,
        modifier = modifier,
        trailing = {
            Text(
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(now), zone).format(TIME_FORMAT),
                color = theme.readableAccentColor,
                fontSize = FutureTypography.headline,
                fontWeight = FutureTypography.weightLight,
                fontFamily = FutureTypography.monoFamily,
            )
        },
    )
}

@Composable
private fun CityPicker(theme: FutureTheme, exclude: List<CityClock>, onPick: (CityClock) -> Unit) {
    val options = remember(exclude) { WORLD_CITIES.filter { it !in exclude } }
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { first.requestFocus() } }
    val now = remember { System.currentTimeMillis() }
    Column(modifier = Modifier.fillMaxSize().background(theme.backgroundColor)) {
        ToolsHeader(title = "הוסף עיר", theme = theme)
        LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            itemsIndexed(options, key = { _, c -> c.name }) { index, city ->
                FutureListItem(
                    title = city.name,
                    summary = relativeLabel(ZoneId.of(city.timezone), now),
                    theme = theme,
                    onClick = { onPick(city) },
                    focusRequester = if (index == 0) first else null,
                )
            }
        }
    }
}
