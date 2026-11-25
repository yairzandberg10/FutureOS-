package com.future.futurelauncher.ui

import android.Manifest
import android.app.ActivityManager
import android.app.AlarmManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.icu.util.HebrewCalendar
import android.icu.util.ULocale
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.TelephonyManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.future.sharednav.i18n.tr
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.coroutines.resume
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/*
 * הווידג'טים המובנים הנוספים (ראו BuiltinWidget). כולם נשענים על המונה המשותף
 * של הדקה ועל שידורי מערכת - אין טיימרים משלהם, ושום דבר לא רץ כשמסך הבית
 * לא מוצג.
 */

private val He = Locale("iw", "IL")

// ---------------------------------------------------------------- שעון גדול

@Composable
internal fun BigClockWidget(tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val now = remember(tick) { Date() }
    val time = formatTime(context, now)
    val date = remember(now) { SimpleDateFormat(tr("EEEE, d בMMMM"), He).format(now) }
    val hebrew = remember(now) { hebrewDate(now) }
    // הגודל הגדול ביותר מהסולם שנכנס: hero 48 / display 34 / headline 24
    val size = when {
        box.height >= 84.dp && box.width >= 150.dp -> FutureTypography.hero
        box.height >= 44.dp && box.width >= 90.dp -> FutureTypography.display
        else -> FutureTypography.headline
    }
    val timeH = lineHeightOf(size)
    val subH = lineHeightOf(FutureTypography.summary)
    val wideRow = box.height < timeH + subH && box.width >= 180.dp
    if (wideRow) {
        // שורה אחת רחבה: השעה ולידה התאריך
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(time, color = ShellInk, style = ws(size, FontWeight.Light, tabular = true), maxLines = 1, softWrap = false)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(date, color = ShellInk, style = ws(FutureTypography.summary, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (box.height >= subH * 2) Text(hebrew, color = ShellInk60, style = ws(FutureTypography.summary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    } else {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(time, color = ShellInk, style = ws(size, FontWeight.Light, tabular = true), maxLines = 1, softWrap = false)
            if (box.height >= timeH + subH) {
                Text(date, color = ShellInk60, style = ws(FutureTypography.summary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (box.height >= timeH + subH * 2) {
                Text(hebrew, color = ShellInk60, style = ws(FutureTypography.summary), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ---------------------------------------------------------------- שעון מחוגים

@Composable
internal fun AnalogClockWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox, theme: FutureTheme) {
    val now = remember(tick) { java.util.Calendar.getInstance() }
    val accent = widgetAccent(theme)
    val dial: Dp = minOf(box.height, box.width, 150.dp)
    val showText = box.width - dial >= 70.dp
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (showText) Arrangement.Start else Arrangement.Center) {
        ClockFace(now, accent, Modifier.size(dial))
        if (showText) {
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                val d = now.time
                Text(SimpleDateFormat("EEEE", He).format(d), color = ShellInk, style = ws(FutureTypography.body, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(SimpleDateFormat(tr("d בMMMM"), He).format(d), color = ShellInk60, style = ws(FutureTypography.summary), maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (box.height >= 60.dp) Text(hebrewDate(d), color = ShellInk60, style = ws(FutureTypography.summary), maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** חוגה: 12 שנתות, מחוג שעות ודקות (בלי מחוג שניות - מתעדכן פעם בדקה). */
@Composable
private fun ClockFace(now: java.util.Calendar, accent: Color, modifier: Modifier) {
    val minute = now.get(java.util.Calendar.MINUTE)
    val hour = now.get(java.util.Calendar.HOUR) + minute / 60f
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        drawCircle(Color.White.copy(alpha = 0.06f), r, c)
        drawCircle(ShellInk15, r - 0.5.dp.toPx(), c, style = Stroke(0.5.dp.toPx()))
        for (i in 0 until 12) {
            val a = i / 12f * 2f * PI.toFloat()
            val major = i % 3 == 0
            val outer = r - 4.dp.toPx()
            val inner = outer - (if (major) 6.dp else 3.dp).toPx()
            drawLine(
                if (major) ShellInk else ShellInk60,
                Offset(c.x + inner * sin(a), c.y - inner * cos(a)),
                Offset(c.x + outer * sin(a), c.y - outer * cos(a)),
                strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }
        fun hand(fraction: Float, length: Float, width: Dp, color: Color) {
            val a = fraction * 2f * PI.toFloat()
            drawLine(color, c, Offset(c.x + length * sin(a), c.y - length * cos(a)), strokeWidth = width.toPx(), cap = StrokeCap.Round)
        }
        hand(hour / 12f, r * 0.5f, 3.dp, ShellInk)
        hand(minute / 60f, r * 0.72f, 2.dp, ShellInk)
        drawCircle(accent, 3.dp.toPx(), c)
    }
}

// ---------------------------------------------------------------- שעון עולמי

private val WorldCities = listOf(
    "לונדון" to "Europe/London",
    "ניו יורק" to "America/New_York",
    "טוקיו" to "Asia/Tokyo",
    "לוס אנג׳לס" to "America/Los_Angeles",
)

@Composable
internal fun WorldClockWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val now = remember(tick) { Date() }
    val count = (box.width / 54.dp).toInt().coerceIn(2, WorldCities.size)
    val showHeader = !box.compact
    val showDiff = box.height >= 18.dp + 62.dp
    if (showHeader) WidgetHeader(kind, tr("שעון עולמי"))
    Row(Modifier.fillMaxWidth().padding(top = if (showHeader) 4.dp else 0.dp)) {
        WorldCities.take(count).forEach { (city, zoneId) ->
            val zone = TimeZone.getTimeZone(zoneId)
            Column(Modifier.weight(1f)) {
                if (!box.tiny) Text(tr(city), color = ShellInk60, style = ws(FutureTypography.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    formatTime(context, now, zone),
                    color = ShellInk,
                    style = ws(if (box.width / count < 52.dp) FutureTypography.title else FutureTypography.screenTitle, FontWeight.Bold, tabular = true),
                    maxLines = 1,
                    softWrap = false,
                )
                if (showDiff) Text(zoneDiffLabel(now, zone), color = ShellInk60, style = ws(FutureTypography.caption), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** "‎-2 ש׳" / "מחר, ‎+7 ש׳" - ההפרש מהשעון המקומי. */
private fun zoneDiffLabel(now: Date, zone: TimeZone): String {
    val local = TimeZone.getDefault()
    val diffMin = (zone.getOffset(now.time) - local.getOffset(now.time)) / 60_000
    val h = diffMin / 60
    val m = kotlin.math.abs(diffMin % 60)
    val diff = when {
        diffMin == 0 -> tr("כמו כאן")
        m == 0 -> "‎" + (if (h > 0) "+" else "") + h + " " + tr("ש׳")
        else -> "‎" + (if (diffMin > 0) "+" else "-") + kotlin.math.abs(h) + ":" + "%02d".format(m)
    }
    val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
    val there = fmt.apply { timeZone = zone }.format(now).toInt()
    val here = SimpleDateFormat("yyyyMMdd", Locale.US).apply { timeZone = local }.format(now).toInt()
    val day = when {
        there > here -> tr("מחר") + ", "
        there < here -> tr("אתמול") + ", "
        else -> ""
    }
    return day + diff
}

// ---------------------------------------------------------------- המעורר הבא

@Composable
internal fun NextAlarmWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val next = remember(tick) { runCatching { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock?.triggerTime }.getOrNull() }
    if (next == null) {
        StandardWidget(kind, box, tr("המעורר הבא"), tr("אין מעורר"), listOf(tr("OK לפתיחת השעון")))
        return
    }
    val date = Date(next)
    val nowMs = System.currentTimeMillis()
    val today = LocalDate.now()
    val day = Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault()).toLocalDate()
    val dayLabel = when (ChronoUnit.DAYS.between(today, day)) {
        0L -> tr("היום")
        1L -> tr("מחר")
        else -> SimpleDateFormat("EEEE", He).format(date)
    }
    val left = ((next - nowMs) / 60_000).coerceAtLeast(0)
    val leftLabel = when {
        left < 60 -> tr("בעוד {0} ד׳", left)
        left < 24 * 60 -> tr("בעוד {0} ש׳ {1} ד׳", left / 60, left % 60)
        else -> tr("בעוד {0} ימים", left / (24 * 60))
    }
    StandardWidget(
        kind, box, tr("המעורר הבא"),
        value = formatTime(context, date),
        lines = listOf(dayLabel, leftLabel),
        valueStyle = ws(WidgetClockSize, FontWeight.Bold, tabular = true),
        largeValueStyle = ws(FutureTypography.hero, FontWeight.Light, tabular = true),
        compactTrailing = "$dayLabel · $leftLabel",
    )
}

// ---------------------------------------------------------------- תאריך עברי וחגים

private class Holiday(val name: String, val month: Int, val day: Int, val days: Int = 1, val shift: ((HebrewCalendar) -> Int)? = null)

/** יום העצמאות מוקדם/נדחה כדי לא לגעת בשבת (וביום שני - לא במוצאי שבת). */
private val atzmautShift: (HebrewCalendar) -> Int = { cal ->
    when (cal.get(HebrewCalendar.DAY_OF_WEEK)) {
        java.util.Calendar.FRIDAY -> -1
        java.util.Calendar.SATURDAY -> -2
        java.util.Calendar.MONDAY -> 1
        else -> 0
    }
}

/** החגים והמועדים (מנהג ארץ ישראל). חודשי ICU: אדר = אדר ב׳ בשנה מעוברת. */
private val Holidays = listOf(
    Holiday("ראש השנה", HebrewCalendar.TISHRI, 1, 2),
    Holiday("יום כיפור", HebrewCalendar.TISHRI, 10),
    Holiday("סוכות", HebrewCalendar.TISHRI, 15, 7),
    Holiday("שמחת תורה", HebrewCalendar.TISHRI, 22),
    Holiday("חנוכה", HebrewCalendar.KISLEV, 25, 8),
    Holiday("ט״ו בשבט", HebrewCalendar.SHEVAT, 15),
    Holiday("פורים", HebrewCalendar.ADAR, 14),
    Holiday("פסח", HebrewCalendar.NISAN, 15, 7),
    Holiday("יום השואה", HebrewCalendar.NISAN, 27, 1) { cal ->
        when (cal.get(HebrewCalendar.DAY_OF_WEEK)) {
            java.util.Calendar.FRIDAY -> -1
            java.util.Calendar.SUNDAY -> 1
            else -> 0
        }
    },
    Holiday("יום הזיכרון", HebrewCalendar.IYAR, 5, 1) { cal -> atzmautShift(cal) - 1 },
    Holiday("יום העצמאות", HebrewCalendar.IYAR, 5, 1, atzmautShift),
    Holiday("ל״ג בעומר", HebrewCalendar.IYAR, 18),
    Holiday("יום ירושלים", HebrewCalendar.IYAR, 28),
    Holiday("שבועות", HebrewCalendar.SIVAN, 6),
    Holiday("תשעה באב", HebrewCalendar.AV, 9, 1) { cal ->
        if (cal.get(HebrewCalendar.DAY_OF_WEEK) == java.util.Calendar.SATURDAY) 1 else 0
    },
)

private fun hebrewToLocal(year: Int, h: Holiday): LocalDate? = runCatching {
    val cal = HebrewCalendar(android.icu.util.TimeZone.getTimeZone("UTC"), ULocale("he_IL@calendar=hebrew"))
    cal.clear()
    cal.set(year, h.month, h.day)
    // יום השואה / הזיכרון נבדקים לפי היום בשבוע של היום המקורי (5 באייר וכו')
    val shift = h.shift?.invoke(cal) ?: 0
    Instant.ofEpochMilli(cal.timeInMillis).atZone(ZoneOffset.UTC).toLocalDate().plusDays(shift.toLong())
}.getOrNull()

/** החג שחל היום (וכמה ימים בתוכו), או הבא בתור. */
private data class HolidayInfo(val name: String, val daysUntil: Long, val dayOf: Int)

private fun nextHoliday(today: LocalDate): HolidayInfo? {
    val year = runCatching {
        HebrewCalendar(android.icu.util.TimeZone.getDefault(), ULocale("he_IL@calendar=hebrew")).get(HebrewCalendar.EXTENDED_YEAR)
    }.getOrNull() ?: return null
    var best: HolidayInfo? = null
    for (y in listOf(year, year + 1)) for (h in Holidays) {
        val start = hebrewToLocal(y, h) ?: continue
        val diff = ChronoUnit.DAYS.between(today, start)
        val info = when {
            diff <= 0 && -diff < h.days -> HolidayInfo(h.name, 0, (-diff).toInt() + 1)
            diff > 0 -> HolidayInfo(h.name, diff, 0)
            else -> null
        } ?: continue
        if (best == null || info.daysUntil < best.daysUntil) best = info
    }
    return best
}

private fun holidayLabel(h: HolidayInfo?): String? = when {
    h == null -> null
    h.daysUntil == 0L && h.name == "חנוכה" -> tr("נר {0} של חנוכה", h.dayOf)
    h.daysUntil == 0L -> tr("היום: {0}", tr(h.name))
    h.daysUntil == 1L -> tr("מחר: {0}", tr(h.name))
    else -> tr("{0} בעוד {1} ימים", tr(h.name), h.daysUntil)
}

@Composable
internal fun HebrewDateWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val now = remember(tick) { Date() }
    val day = LocalDate.now()
    val short = remember(day) {
        runCatching { android.icu.text.SimpleDateFormat("d בMMMM", ULocale.forLanguageTag("he-IL-u-ca-hebrew-nu-hebr")).format(now) }.getOrDefault("")
    }
    val year = remember(day) {
        runCatching { android.icu.text.SimpleDateFormat("y", ULocale.forLanguageTag("he-IL-u-ca-hebrew-nu-hebr")).format(now) }.getOrDefault("")
    }
    val holiday = remember(day) { holidayLabel(nextHoliday(day)) }
    val weekday = remember(day) { SimpleDateFormat("EEEE", He).format(now) }
    val gregorian = remember(day) { SimpleDateFormat(tr("d בMMMM yyyy"), He).format(now) }
    StandardWidget(
        kind, box,
        title = weekday,
        value = short.ifBlank { gregorian },
        lines = listOfNotNull(holiday, year.takeIf { it.isNotBlank() }?.let { tr("שנת {0}", it) }, gregorian),
        valueFlexible = true,
    )
}

// ---------------------------------------------------------------- לוח חודשי

@Composable
internal fun MonthCalendarWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox, theme: FutureTheme) {
    val today = remember(tick) { LocalDate.now() }
    val accent = widgetAccent(theme)
    val onAccent = FutureContrast.onColor(accent)
    val letters = listOf("א", "ב", "ג", "ד", "ה", "ו", "ש")
    val first = today.withDayOfMonth(1)
    // ראשון = עמודה 0 (בצד ימין - הרשת RTL)
    val lead = first.dayOfWeek.value % 7
    val weeks = (lead + today.lengthOfMonth() + 6) / 7
    val headerH = 18.dp + 4.dp
    val letterH = lineHeightOf(FutureTypography.caption)
    val rowH = (box.height - headerH - letterH) / weeks
    val title = SimpleDateFormat(tr("MMMM yyyy"), He).format(Date())

    if (rowH < 13.dp) {
        // אין מקום לחודש: רצועת השבוע הנוכחי
        val sunday = today.minusDays((today.dayOfWeek.value % 7).toLong())
        if (!box.tiny) WidgetHeader(kind, title)
        Row(Modifier.fillMaxWidth().padding(top = if (box.tiny) 0.dp else 2.dp)) {
            (0 until 7).forEach { i ->
                val d = sunday.plusDays(i.toLong())
                val isToday = d == today
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(letters[i], color = ShellInk60, style = ws(FutureTypography.caption), maxLines = 1)
                    DayCell(d.dayOfMonth, isToday, accent, onAccent, 20.dp)
                }
            }
        }
        return
    }
    WidgetHeader(kind, title)
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth()) {
        letters.forEach {
            Text(it, color = ShellInk60, style = ws(FutureTypography.caption), textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    val cell = minOf(rowH, 22.dp)
    for (w in 0 until weeks) {
        Row(Modifier.fillMaxWidth().height(rowH), verticalAlignment = Alignment.CenterVertically) {
            for (c in 0 until 7) {
                val dayNum = w * 7 + c - lead + 1
                Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) {
                    if (dayNum in 1..today.lengthOfMonth()) DayCell(dayNum, dayNum == today.dayOfMonth, accent, onAccent, cell)
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, today: Boolean, accent: Color, onAccent: Color, size: Dp) {
    Box(
        Modifier.size(size).clip(FutureShapes.pill).background(if (today) accent else Color.Transparent),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            day.toString(),
            color = if (today) onAccent else ShellInk,
            style = ws(FutureTypography.caption, if (today) FontWeight.Bold else FontWeight.Normal, tabular = true),
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------- זריחה ושקיעה

/** ירושלים - אין הרשאת מיקום, אז החישוב לעיר קבועה (מצוינת בווידג'ט). */
private const val SunLat = 31.778
private const val SunLon = 35.235

/**
 * זמן הזריחה/השקיעה בדקות UTC מחצות - אלגוריתם האלמנך (Almanac for Computers,
 * 1990), דיוק של דקה-שתיים. 90.833° = שפת השמש עם שבירת האור.
 */
private fun sunEventUtcHours(date: LocalDate, rising: Boolean, zenith: Double = 90.833): Double? {
    fun rad(d: Double) = d * PI / 180
    fun deg(r: Double) = r * 180 / PI
    val n = date.dayOfYear
    val lngHour = SunLon / 15
    val t = n + ((if (rising) 6 else 18) - lngHour) / 24
    val m = 0.9856 * t - 3.289
    val l = ((m + 1.916 * sin(rad(m)) + 0.020 * sin(rad(2 * m)) + 282.634) % 360 + 360) % 360
    var ra = (deg(atan(0.91764 * tan(rad(l)))) % 360 + 360) % 360
    ra += floor(l / 90) * 90 - floor(ra / 90) * 90
    ra /= 15
    val sinDec = 0.39782 * sin(rad(l))
    val cosDec = cos(asin(sinDec))
    val cosH = (cos(rad(zenith)) - sinDec * sin(rad(SunLat))) / (cosDec * cos(rad(SunLat)))
    if (cosH > 1 || cosH < -1) return null
    val h = (if (rising) 360 - deg(acos(cosH)) else deg(acos(cosH))) / 15
    val tt = h + ra - 0.06571 * t - 6.622
    return ((tt - lngHour) % 24 + 24) % 24
}

private fun sunEvent(date: LocalDate, rising: Boolean): ZonedDateTime? {
    val ut = sunEventUtcHours(date, rising) ?: return null
    return date.atStartOfDay(ZoneOffset.UTC).plusSeconds((ut * 3600).toLong()).withZoneSameInstant(ZoneId.systemDefault())
}

@Composable
internal fun SunTimesWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val today = remember(tick) { LocalDate.now() }
    val times = remember(today) { sunEvent(today, true) to sunEvent(today, false) }
    val (rise, set) = times
    fun fmt(z: ZonedDateTime?) = z?.let { formatTime(context, Date.from(it.toInstant())) } ?: "--"
    val now = ZonedDateTime.now()
    // האירוע הבא: לפני הזריחה - הזריחה, ביום - השקיעה, בלילה - הזריחה של מחר
    val beforeRise = rise != null && now.isBefore(rise)
    val afterSunset = set != null && now.isAfter(set)
    val tomorrowRise = remember(today, afterSunset) { if (afterSunset) sunEvent(today.plusDays(1), true) else null }
    val (value, label, other) = when {
        afterSunset -> Triple(fmt(tomorrowRise), tr("זריחה מחר"), tr("שקיעה {0}", fmt(set)))
        beforeRise -> Triple(fmt(rise), tr("זריחה"), tr("שקיעה {0}", fmt(set)))
        else -> Triple(fmt(set), tr("שקיעה"), tr("זריחה {0}", fmt(rise)))
    }
    val dayLen = if (rise != null && set != null) ChronoUnit.MINUTES.between(rise, set) else null
    val noon = if (rise != null && set != null) rise.plusMinutes(ChronoUnit.MINUTES.between(rise, set) / 2) else null
    StandardWidget(
        kind, box, tr("זריחה ושקיעה · ירושלים"),
        value = value,
        lines = listOfNotNull(
            "$label · $other",
            dayLen?.let { tr("אורך היום {0}:{1}", it / 60, "%02d".format(it % 60)) },
            noon?.let { tr("חצות היום {0}", fmt(it)) },
        ),
        valueStyle = ws(FutureTypography.screenTitle, FontWeight.Bold, tabular = true),
        compactTrailing = if (box.width >= 150.dp) "$label · $other" else label,
    )
}

// ---------------------------------------------------------------- סוללה בטבעת

@Composable
internal fun BatteryRingWidget(kind: BuiltinWidget, box: WidgetBox, theme: FutureTheme) {
    val context = LocalContext.current
    val battery = rememberStickyBroadcast(Intent.ACTION_BATTERY_CHANGED)
    val level = batteryLevel(battery)
    val charging = batteryCharging(battery)
    val temp = battery?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)?.takeIf { it != Int.MIN_VALUE }
    val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
    val remaining = remember(battery) {
        if (charging) runCatching { context.getSystemService(BatteryManager::class.java)?.computeChargeTimeRemaining() }.getOrNull() else null
    }
    val color = when {
        charging -> WidgetSuccess
        level != null && level <= 15 -> WidgetDanger
        else -> widgetAccent(theme)
    }
    val status = when {
        battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_FULL -> tr("טעונה במלואה")
        charging && plugged == BatteryManager.BATTERY_PLUGGED_USB -> tr("בטעינה (USB)")
        charging -> tr("בטעינה")
        else -> tr("על סוללה")
    }
    val lines = listOfNotNull(
        status,
        remaining?.takeIf { it > 0 }?.let { ms -> val m = ms / 60_000; tr("מלאה בעוד {0}:{1}", m / 60, "%02d".format(m % 60)) },
        temp?.let { tr("טמפרטורה {0}°", "%.1f".format(it / 10f)) },
    )
    val ring = minOf(box.height, box.width, 120.dp)
    val showText = box.width - ring >= 64.dp
    Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = if (showText) Arrangement.Start else Arrangement.Center) {
        Box(Modifier.size(ring), contentAlignment = Alignment.Center) {
            val pct = (level ?: 0) / 100f
            Canvas(Modifier.fillMaxSize()) {
                val stroke = (if (ring >= 80.dp) 7.dp else 5.dp).toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                drawArc(ShellInk15, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                drawArc(color, -90f, 360f * pct, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
            }
            Text(
                level?.let { "$it%" } ?: "--",
                color = ShellInk,
                style = ws(if (ring >= 96.dp) FutureTypography.headline else if (ring >= 56.dp) FutureTypography.body else FutureTypography.label, FontWeight.Bold, tabular = true),
                maxLines = 1,
            )
        }
        if (showText) {
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                if (box.height >= 60.dp) WidgetHeader(kind, tr("סוללה"))
                val room = ((box.height - if (box.height >= 60.dp) 18.dp else 0.dp) / lineHeightOf(FutureTypography.summary)).toInt()
                lines.take(room.coerceAtLeast(1)).forEach { WidgetSubtitle(it, maxLines = 1) }
            }
        }
    }
}

// ---------------------------------------------------------------- אחסון וזיכרון

private data class Usage(val used: Long, val total: Long) {
    val fraction: Float get() = if (total > 0) (used.toFloat() / total).coerceIn(0f, 1f) else 0f
}

@Composable
internal fun StorageWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox, theme: FutureTheme) {
    val context = LocalContext.current
    val data by produceState<Pair<Usage?, Usage?>>(null to null, tick) {
        value = withContext(Dispatchers.IO) {
            val disk = runCatching {
                val s = StatFs(Environment.getDataDirectory().path)
                Usage(s.totalBytes - s.availableBytes, s.totalBytes)
            }.getOrNull()
            val ram = runCatching {
                val info = ActivityManager.MemoryInfo()
                context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(info)
                Usage(info.totalMem - info.availMem, info.totalMem)
            }.getOrNull()
            disk to ram
        }
    }
    fun size(b: Long) = android.text.format.Formatter.formatShortFileSize(context, b)
    val (disk, ram) = data
    val accent = widgetAccent(theme)
    val showHeader = !box.compact
    if (showHeader) {
        WidgetHeader(kind, tr("אחסון וזיכרון"))
        Spacer(Modifier.height(6.dp))
    }
    UsageBar(
        tr("אחסון"),
        disk?.let { tr("{0} פנויים מתוך {1}", size(it.total - it.used), size(it.total)) } ?: "--",
        disk?.fraction ?: 0f,
        if ((disk?.fraction ?: 0f) > 0.9f) WidgetDanger else accent,
    )
    if (!box.tiny) {
        Spacer(Modifier.height(if (showHeader) 8.dp else 4.dp))
        UsageBar(
            tr("זיכרון"),
            ram?.let { tr("{0} בשימוש מתוך {1}", size(it.used), size(it.total)) } ?: "--",
            ram?.fraction ?: 0f,
            accent,
        )
    }
}

@Composable
private fun UsageBar(label: String, detail: String, fraction: Float, color: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = ShellInk, style = ws(FutureTypography.label, FontWeight.Medium), maxLines = 1, modifier = Modifier.alignByBaseline())
        Spacer(Modifier.width(6.dp))
        Text(detail, color = ShellInk60, style = ws(FutureTypography.label), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.alignByBaseline().weight(1f, fill = false))
    }
    Spacer(Modifier.height(3.dp))
    Box(Modifier.fillMaxWidth().height(4.dp).clip(FutureShapes.pill).background(ShellInk15)) {
        Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().clip(FutureShapes.pill).background(color))
    }
}

// ---------------------------------------------------------------- קישוריות

private data class Connectivity(
    val wifi: Boolean,
    val wifiLevel: Int?,
    val cellular: Boolean,
    val operator: String?,
    val cellLevel: Int?,
    val bluetooth: Boolean?,
    val airplane: Boolean,
)

private fun readConnectivity(context: Context): Connectivity {
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val caps = runCatching { cm?.getNetworkCapabilities(cm.activeNetwork) }.getOrNull()
    val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
    val cellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
    val wifiLevel = if (wifi) runCatching {
        val rssi = caps!!.signalStrength
        if (rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED) null
        else context.getSystemService(WifiManager::class.java)?.let { wm -> wm.calculateSignalLevel(rssi) * 4 / wm.maxSignalLevel.coerceAtLeast(1) }
    }.getOrNull() else null
    val tm = context.getSystemService(TelephonyManager::class.java)
    val operator = runCatching { tm?.networkOperatorName }.getOrNull()?.takeIf { it.isNotBlank() }
    val cellLevel = runCatching { tm?.signalStrength?.level }.getOrNull()
    val bt = runCatching { context.getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled }.getOrNull()
    val airplane = runCatching { Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) == 1 }.getOrDefault(false)
    return Connectivity(wifi, wifiLevel, cellular, operator, cellLevel, bt, airplane)
}

private fun bars(level: Int?): String = level?.let { "▂▄▆█".take(it.coerceIn(0, 4)).padEnd(4, '·') } ?: ""

@Composable
internal fun ConnectivityWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val events = rememberBroadcastTick(
        ConnectivityManager.CONNECTIVITY_ACTION,
        Intent.ACTION_AIRPLANE_MODE_CHANGED,
        BluetoothAdapter.ACTION_STATE_CHANGED,
        WifiManager.RSSI_CHANGED_ACTION,
    )
    val c by produceState<Connectivity?>(null, tick, events) {
        value = withContext(Dispatchers.IO) { readConnectivity(context) }
    }
    val s = c
    val main = when {
        s == null -> "--"
        s.airplane && !s.wifi -> tr("מצב טיסה")
        s.wifi -> "Wi-Fi"
        s.cellular -> s.operator ?: tr("סלולר")
        else -> tr("אין חיבור")
    }
    val lines = if (s == null) emptyList() else listOfNotNull(
        if (s.wifi) "Wi-Fi " + bars(s.wifiLevel) else tr("Wi-Fi לא מחובר"),
        (s.operator ?: tr("סלולר")) + if (s.airplane) " · " + tr("כבוי") else " " + bars(s.cellLevel),
        s.bluetooth?.let { tr("בלוטות׳ {0}", if (it) tr("פועל") else tr("כבוי")) },
        if (s.airplane) tr("מצב טיסה פועל") else null,
    )
    val trailing = if (s == null) null else listOfNotNull(
        if (s.wifi) bars(s.wifiLevel) else if (s.cellular) bars(s.cellLevel) else null,
        s.bluetooth?.takeIf { it }?.let { tr("בלוטות׳") },
    ).joinToString(" · ")
    StandardWidget(kind, box, tr("קישוריות"), main, lines, compactTrailing = trailing, valueFlexible = true)
}

// ---------------------------------------------------------------- צליל

@Composable
internal fun SoundWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val events = rememberBroadcastTick(AudioManager.RINGER_MODE_CHANGED_ACTION, "android.media.VOLUME_CHANGED_ACTION")
    val audio = remember(context) { context.getSystemService(AudioManager::class.java) }
    val state = remember(tick, events) {
        runCatching {
            fun pct(stream: Int) = audio!!.getStreamVolume(stream) * 100 / audio.getStreamMaxVolume(stream).coerceAtLeast(1)
            Triple(audio!!.ringerMode, pct(AudioManager.STREAM_RING), pct(AudioManager.STREAM_MUSIC))
        }.getOrNull()
    }
    val mode = ringerLabel(state?.first) ?: "--"
    val ring = state?.let { tr("צלצול {0}%", it.second) }
    val media = state?.let { tr("מדיה {0}%", it.third) }
    StandardWidget(
        kind, box, tr("צליל"),
        value = mode,
        lines = listOfNotNull(ring, media, tr("OK מחליף צלצול/רטט")),
        compactTrailing = listOfNotNull(ring, media).joinToString(" · "),
    )
}

// ---------------------------------------------------------------- צעדים

/**
 * חיישן TYPE_STEP_COUNTER סופר מאז האתחול. "היום" = הערך פחות הבסיס שנשמר
 * בתחילת היום (הקריאה האחרונה של אתמול), ואחרי אתחול (ערך קטן מהבסיס) -
 * מתחילים מאפס. החיישן נרשם לרגע בכל דקה ומבוטל מיד עם הערך הראשון.
 */
private suspend fun readStepCounter(context: Context): Float? {
    val sm = context.getSystemService(SensorManager::class.java) ?: return null
    val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
    return withTimeoutOrNull(3_000) {
        suspendCancellableCoroutine { cont ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    sm.unregisterListener(this)
                    if (cont.isActive) cont.resume(event.values.firstOrNull())
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            cont.invokeOnCancellation { sm.unregisterListener(listener) }
        }
    }
}

private fun stepsToday(context: Context, raw: Float?): Int? {
    val prefs = context.getSharedPreferences("builtin_steps", Context.MODE_PRIVATE)
    val today = LocalDate.now().toEpochDay()
    var base = prefs.getLong("base", -1)
    val day = prefs.getLong("day", -1)
    val last = prefs.getLong("last", -1)
    if (raw == null) {
        // החיישן לא ענה בזמן - הערך האחרון של היום, אם יש
        return if (day == today && last >= 0 && base >= 0) (last - base).toInt().coerceAtLeast(0) else null
    }
    val value = raw.toLong()
    if (day != today || base < 0) base = if (last in 0..value) last else value
    if (value < base) base = 0 // אתחול
    prefs.edit().putLong("day", today).putLong("base", base).putLong("last", value).apply()
    return (value - base).toInt()
}

@Composable
internal fun StepsWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox, theme: FutureTheme) {
    val context = LocalContext.current
    val granted = context.checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
    val steps by produceState<Int?>(null, tick, granted) {
        value = if (!granted) null else {
            val raw = readStepCounter(context)
            withContext(Dispatchers.IO) { stepsToday(context, raw) }
        }
    }
    if (!granted) {
        StandardWidget(kind, box, tr("צעדים היום"), tr("צעדים"), listOf(tr("פתח כדי לאשר גישה")))
        return
    }
    val s = steps
    val km = s?.let { tr("≈ {0} ק״מ", "%.1f".format(it * 0.75 / 1000)) }
    StandardWidget(
        kind, box, tr("צעדים היום"),
        value = s?.let { "%,d".format(Locale.US, it) } ?: "--",
        lines = listOfNotNull(km, tr("מאז חצות")),
        valueStyle = ws(FutureTypography.screenTitle, FontWeight.Bold, tabular = true),
        largeValueStyle = ws(FutureTypography.hero, FontWeight.Light, tabular = true),
    )
}

// ---------------------------------------------------------------- אנשי קשר מועדפים

private sealed class Favorites {
    object NoPermission : Favorites()
    data class Starred(val names: List<String>) : Favorites()
}

private fun loadFavorites(context: Context): Favorites {
    if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return Favorites.NoPermission
    val names = mutableListOf<String>()
    runCatching {
        context.contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY),
            "${ContactsContract.Contacts.STARRED} = 1", null,
            "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} COLLATE LOCALIZED ASC",
        )?.use { c -> while (c.moveToNext() && names.size < 8) c.getString(0)?.takeIf { it.isNotBlank() }?.let { names += it } }
    }
    return Favorites.Starred(names)
}

@Composable
internal fun FavoritesWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val fav by produceState<Favorites?>(null, tick) { value = withContext(Dispatchers.IO) { loadFavorites(context) } }
    val title = tr("אנשי קשר מועדפים")
    when (val f = fav) {
        null -> StandardWidget(kind, box, title, tr("מועדפים"), emptyList())
        Favorites.NoPermission -> StandardWidget(kind, box, title, tr("מועדפים"), listOf(tr("פתח כדי לאשר גישה")))
        is Favorites.Starred -> if (f.names.isEmpty()) {
            StandardWidget(kind, box, title, tr("אין מועדפים"), listOf(tr("סמנו כוכב באנשי הקשר")))
        } else {
            val avatar = when {
                box.height >= 96.dp -> 40.dp
                box.tiny -> 0.dp
                else -> 28.dp
            }
            val showHeader = box.height >= 18.dp + 4.dp + avatar + lineHeightOf(FutureTypography.caption)
            if (showHeader) WidgetHeader(kind, title)
            val count = (box.width / 52.dp).toInt().coerceIn(1, f.names.size)
            Row(Modifier.fillMaxWidth().padding(top = if (showHeader) 4.dp else 0.dp)) {
                f.names.take(count).forEach { name ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (avatar > 0.dp) {
                            Box(Modifier.size(avatar).clip(FutureShapes.pill).background(ShellInk15), contentAlignment = Alignment.Center) {
                                Text(initials(name), color = ShellInk, style = ws(if (avatar >= 40.dp) FutureTypography.body else FutureTypography.label, FontWeight.Bold), maxLines = 1)
                            }
                        }
                        Text(name.substringBefore(' '), color = ShellInk60, style = ws(FutureTypography.caption), maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
                    }
                }
                // שאר העמודות ריקות כשיש פחות מועדפים מהמקום
                repeat(((box.width / 52.dp).toInt().coerceAtLeast(1) - count).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

private fun initials(name: String): String =
    name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2).joinToString("") { it.take(1) }.uppercase()

// ---------------------------------------------------------------- פרקי אבות

/** משניות מפרקי אבות (נחלת הכלל). אחת ליום, לפי מספר היום מאז 1970. */
private val AvotSayings = listOf(
    "עַל שְׁלֹשָׁה דְבָרִים הָעוֹלָם עוֹמֵד: עַל הַתּוֹרָה וְעַל הָעֲבוֹדָה וְעַל גְּמִילוּת חֲסָדִים" to "שמעון הצדיק, א׳ ב׳",
    "הֱוֵי מִתַּלְמִידָיו שֶׁל אַהֲרֹן, אוֹהֵב שָׁלוֹם וְרוֹדֵף שָׁלוֹם" to "הלל, א׳ י״ב",
    "אִם אֵין אֲנִי לִי, מִי לִי? וּכְשֶׁאֲנִי לְעַצְמִי, מָה אֲנִי? וְאִם לֹא עַכְשָׁיו, אֵימָתָי?" to "הלל, א׳ י״ד",
    "אֱמֹר מְעַט וַעֲשֵׂה הַרְבֵּה, וֶהֱוֵי מְקַבֵּל אֶת כָּל הָאָדָם בְּסֵבֶר פָּנִים יָפוֹת" to "שמאי, א׳ ט״ו",
    "עֲשֵׂה לְךָ רַב, וּקְנֵה לְךָ חָבֵר, וֶהֱוֵי דָן אֶת כָּל הָאָדָם לְכַף זְכוּת" to "יהושע בן פרחיה, א׳ ו׳",
    "לֹא הַמִּדְרָשׁ הוּא הָעִקָּר, אֶלָּא הַמַּעֲשֶׂה" to "שמעון בן גמליאל, א׳ י״ז",
    "עַל שְׁלֹשָׁה דְבָרִים הָעוֹלָם קַיָּם: עַל הַדִּין וְעַל הָאֱמֶת וְעַל הַשָּׁלוֹם" to "רבן שמעון בן גמליאל, א׳ י״ח",
    "אַל תִּפְרֹשׁ מִן הַצִּבּוּר, וְאַל תָּדִין אֶת חֲבֵרְךָ עַד שֶׁתַּגִּיעַ לִמְקוֹמוֹ" to "הלל, ב׳ ד׳",
    "בִּמְקוֹם שֶׁאֵין אֲנָשִׁים, הִשְׁתַּדֵּל לִהְיוֹת אִישׁ" to "הלל, ב׳ ה׳",
    "לֹא הַבַּיְשָׁן לָמֵד, וְלֹא הַקַּפְּדָן מְלַמֵּד" to "הלל, ב׳ ה׳",
    "יְהִי כְבוֹד חֲבֵרְךָ חָבִיב עָלֶיךָ כְּשֶׁלְּךָ, וְאַל תְּהִי נוֹחַ לִכְעֹס" to "רבי אליעזר, ב׳ י׳",
    "לֹא עָלֶיךָ הַמְּלָאכָה לִגְמֹר, וְלֹא אַתָּה בֶן חוֹרִין לִבָּטֵל מִמֶּנָּה" to "רבי טרפון, ב׳ ט״ז",
    "הַיּוֹם קָצָר וְהַמְּלָאכָה מְרֻבָּה" to "רבי טרפון, ב׳ ט״ו",
    "אֵיזֶהוּ חָכָם? הַלּוֹמֵד מִכָּל אָדָם" to "בן זומא, ד׳ א׳",
    "אֵיזֶהוּ גִבּוֹר? הַכּוֹבֵשׁ אֶת יִצְרוֹ" to "בן זומא, ד׳ א׳",
    "אֵיזֶהוּ עָשִׁיר? הַשָּׂמֵחַ בְּחֶלְקוֹ" to "בן זומא, ד׳ א׳",
    "אֵיזֶהוּ מְכֻבָּד? הַמְכַבֵּד אֶת הַבְּרִיּוֹת" to "בן זומא, ד׳ א׳",
    "הֱוֵי רָץ לְמִצְוָה קַלָּה, וּבוֹרֵחַ מִן הָעֲבֵרָה" to "בן עזאי, ד׳ ב׳",
    "אַל תְּהִי בָז לְכָל אָדָם, וְאַל תְּהִי מַפְלִיג לְכָל דָּבָר, שֶׁאֵין לְךָ אָדָם שֶׁאֵין לוֹ שָׁעָה" to "בן עזאי, ד׳ ג׳",
    "אַל תִּסְתַּכֵּל בַּקַּנְקַן, אֶלָּא בְמַה שֶּׁיֶּשׁ בּוֹ" to "רבי, ד׳ כ׳",
    "הֱוֵי מַקְדִּים בִּשְׁלוֹם כָּל אָדָם" to "רבי מתיא בן חרש, ד׳ ט״ו",
    "הֱוֵי זָנָב לָאֲרָיוֹת, וְאַל תְּהִי רֹאשׁ לַשּׁוּעָלִים" to "רבי מתיא בן חרש, ד׳ ט״ו",
    "כָּל הַמְכַבֵּד אֶת הַתּוֹרָה, גּוּפוֹ מְכֻבָּד עַל הַבְּרִיּוֹת" to "רבי יוסי, ד׳ ו׳",
    "הֱוֵי שְׁפַל רוּחַ בִּפְנֵי כָל אָדָם" to "רבי מאיר, ד׳ י׳",
    "בֶּן חָמֵשׁ שָׁנִים לַמִּקְרָא, בֶּן עֶשֶׂר לַמִּשְׁנָה" to "יהודה בן תימא, ה׳ כ״א",
    "לְפוּם צַעֲרָא אַגְרָא" to "בן הא הא, ה׳ כ״ג",
    "הֲפֹךְ בָּהּ וַהֲפֹךְ בָּהּ, דְּכֹלָּא בָהּ" to "בן בג בג, ה׳ כ״ב",
    "כָּל מַחֲלֹקֶת שֶׁהִיא לְשֵׁם שָׁמַיִם, סוֹפָהּ לְהִתְקַיֵּם" to "ה׳ י״ז",
    "הֱוֵי עַז כַּנָּמֵר, וְקַל כַּנֶּשֶׁר, רָץ כַּצְּבִי, וְגִבּוֹר כָּאֲרִי" to "יהודה בן תימא, ה׳ כ׳",
    "סְיָג לַחָכְמָה, שְׁתִיקָה" to "רבי עקיבא, ג׳ י״ג",
    "הַכֹּל צָפוּי, וְהָרְשׁוּת נְתוּנָה" to "רבי עקיבא, ג׳ ט״ו",
    "כָּל שֶׁרוּחַ הַבְּרִיּוֹת נוֹחָה הֵימֶנּוּ, רוּחַ הַמָּקוֹם נוֹחָה הֵימֶנּוּ" to "רבי חנינא בן דוסא, ג׳ י׳",
)

/** בלי ניקוד וטעמים - בשורה קטנה הם רק מעמיסים. */
private fun stripNiqqud(s: String) = s.replace(Regex("[֑-ׇ]"), "")

@Composable
internal fun AvotWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val day = remember(tick) { LocalDate.now().toEpochDay() }
    val (text, source) = AvotSayings[(day % AvotSayings.size).toInt()]
    val shown = stripNiqqud(text)
    val bodyH = lineHeightOf(FutureTypography.body)
    val header = !box.tiny
    val room = box.height - (if (header) 18.dp + 4.dp else 0.dp)
    val lines = (room / bodyH).toInt().coerceIn(1, 6)
    if (header) WidgetHeader(kind, tr("פרקי אבות") + " · " + source)
    Text(
        shown,
        color = ShellInk,
        style = ws(FutureTypography.body, FontWeight.Medium),
        maxLines = lines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = if (header) 4.dp else 0.dp),
    )
}
