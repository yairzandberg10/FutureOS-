package com.future.futurelauncher.ui

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
import com.future.sharednav.theme.FutureContrast
import com.future.sharednav.theme.FutureShapes
import com.future.sharednav.theme.FutureTheme
import com.future.sharednav.theme.FutureTypography
import com.future.sharednav.theme.headsUpBorderColor
import com.future.sharednav.theme.headsUpSurfaceColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * ווידג'טים מובנים של מסך הבית - מצוירים בלאנצ'ר עצמו (Compose), בלי ספק
 * AppWidgetProvider ובלי לקשר מזהה ב-AppWidgetHost. הם עובדים מהרגע הראשון,
 * מתעדכנים חי ונשמרים בפריסה כ-LauncherItem.Widget עם [key] בשדה builtin
 * ו-widgetId = -1. ה-[key] הוא מה שנשמר - אסור לשנות מפתח קיים.
 *
 * המראה בסגנון ווידג'טי iOS (כמו FutureContentWidget): משטח #1C1C1E אטום בלי
 * מסגרת, רדיוס 22dp, ריפוד 16/12, תוכן מיושר למעלה, כותרת וסמל בגוון של
 * הווידג'ט ([tint]), ערך מודגש לבן ושורות משנה באפור המשני של iOS. תמיד כהה -
 * הוא יושב על הטפט. אין לו מצב פוקוס משלו (הסמן של הרשת), ו-OK פותח את
 * האפליקציה שמזינה אותו (או מבצע את הפעולה של הווידג'ט, ראו
 * [performBuiltinWidgetAction]).
 *
 * הפריסה נבחרת לפי הגובה שנמדד בפועל ([WidgetBox]) ולא לפי מספר השורות:
 * שורה אחת היא 80dp (72dp במצב עריכה, ועוד שם מתחת), אז ווידג'ט בשורה אחת
 * מקבל פריסה דחוסה של שתי שורות טקסט לכל היותר.
 *
 * [minSpanX] - מתחת לשתי עמודות אין מקום לטקסט, אז ווידג'ט מובנה לא מצטמצם
 * לתא בודד (לא בהוספה ולא בשינוי גודל).
 *
 * [permission] - הרשאת זמן ריצה שהווידג'ט צריך. מתבקשת רק ב-OK על הווידג'ט,
 * ועד אז הוא מציג "פתח כדי לאשר גישה".
 */
enum class BuiltinWidget(
    val key: String,
    val title: String,
    val description: String,
    val defaultSpanX: Int,
    val defaultSpanY: Int,
    /** האפליקציה שמזינה את הווידג'ט: האייקון שלה בכותרת, ו-OK פותח אותה. */
    val appPackage: String,
    val fallbackIcon: ImageVector,
    val permission: String? = null,
) {
    CLOCK("clock", "שעון ותאריך", "שעה, תאריך עברי ולועזי ומעורר הבא", 4, 2, "com.future.clock", FutureIcons.Alarm),
    BIG_CLOCK("big_clock", "שעון גדול", "השעה בספרות גדולות והתאריך", 4, 1, "com.future.clock", FutureIcons.Schedule),
    ANALOG_CLOCK("analog_clock", "שעון מחוגים", "שעון עגול עם מחוגים", 2, 2, "com.future.clock", FutureIcons.Watch),
    WORLD_CLOCK("world_clock", "שעון עולמי", "השעה בלונדון, ניו יורק, טוקיו ולוס אנג׳לס", 4, 1, "com.future.clock", FutureIcons.Public),
    NEXT_ALARM("next_alarm", "המעורר הבא", "מתי המעורר הבא יצלצל ובעוד כמה זמן", 2, 1, "com.future.clock", FutureIcons.Alarm),
    HEBREW_DATE("hebrew_date", "תאריך עברי וחגים", "התאריך העברי והחג או המועד הקרוב", 2, 2, "com.future.calendar", FutureIcons.CalendarToday),
    MONTH_CALENDAR("month_calendar", "לוח חודשי", "החודש הנוכחי, היום מודגש", 4, 2, "com.future.calendar", FutureIcons.CalendarToday),
    AGENDA("agenda", "אירועים קרובים", "האירועים הבאים מהיומן", 4, 1, "com.future.calendar", FutureIcons.CalendarToday, Manifest.permission.READ_CALENDAR),
    SUN_TIMES("sun_times", "זריחה ושקיעה", "זמני השמש בירושלים, מחושבים במכשיר", 2, 1, "com.future.calendar", FutureIcons.WbTwilight),
    DEVICE("device", "סוללה ומכשיר", "סוללה, מצב צלצול ואחסון פנוי", 2, 1, "com.future.settings", FutureIcons.BatteryFull),
    BATTERY_RING("battery_ring", "סוללה בטבעת", "אחוז סוללה, זמן טעינה וטמפרטורה", 2, 2, "com.future.settings", FutureIcons.BatteryChargingFull),
    STORAGE("storage", "אחסון וזיכרון", "מקום פנוי באחסון וזיכרון בשימוש", 4, 1, "com.future.files", FutureIcons.Storage),
    CONNECTIVITY("connectivity", "קישוריות", "Wi-Fi, סלולר, בלוטות׳ ומצב טיסה", 2, 2, "com.future.settings", FutureIcons.Wifi),
    SOUND("sound", "צליל", "מצב צלצול ועוצמות. OK מחליף בין צלצול לרטט", 2, 1, "com.future.settings", FutureIcons.VolumeUp),
    STEPS("steps", "צעדים היום", "ספירת הצעדים של היום מחיישן המכשיר", 2, 1, "com.future.fitness", FutureIcons.DirectionsWalk, Manifest.permission.ACTIVITY_RECOGNITION),
    FAVORITES("favorites", "אנשי קשר מועדפים", "אנשי הקשר שסומנו בכוכב", 4, 1, "com.future.contact", FutureIcons.Star, Manifest.permission.READ_CONTACTS),
    AVOT("avot", "פרקי אבות", "משנה יומית מפרקי אבות", 4, 1, "com.future.sfarim", FutureIcons.MenuBook);

    val minSpanX: Int get() = 2

    /** הודעה כשהמשתמש סירב להרשאה של הווידג'ט. */
    val permissionDeniedText: String
        get() = when (this) {
            AGENDA -> "אין גישה ליומן"
            STEPS -> "אין גישה לחיישן הצעדים"
            FAVORITES -> "אין גישה לאנשי הקשר"
            else -> "לא ניתנה הרשאה"
        }

    /** האם הווידג'ט יכול לעבוד במכשיר הזה (מוסתר מהבורר אם לא). */
    fun isSupported(context: Context): Boolean = when (this) {
        STEPS -> runCatching {
            context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null
        }.getOrDefault(false)
        else -> true
    }

    companion object {
        fun fromKey(key: String?): BuiltinWidget? = entries.firstOrNull { it.key == key }
    }
}

/**
 * אות רענון לכל הווידג'טים המובנים - עולה כשהמשתמש אישר הרשאה (יומן, אנשי
 * קשר, צעדים) או שינה מצב צלצול מהווידג'ט, כדי שהווידג'ט יקרא שוב מיד ולא
 * רק בדקה הבאה.
 */
object BuiltinWidgetSignals {
    var refresh by mutableIntStateOf(0)
}

/**
 * פעולת OK של ווידג'ט שאינה "פתח את האפליקציה". true אם טופלה.
 * צליל: מחליף בין צלצול לרטט (מצב שקט דורש גישה ל"נא לא להפריע", אז ממנו
 * חוזרים לצלצול, ואם המערכת מסרבת - נופלים לפתיחת ההגדרות).
 */
fun performBuiltinWidgetAction(context: Context, kind: BuiltinWidget): Boolean {
    if (kind != BuiltinWidget.SOUND) return false
    val audio = context.getSystemService(AudioManager::class.java) ?: return false
    val next = when (audio.ringerMode) {
        AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
        else -> AudioManager.RINGER_MODE_NORMAL
    }
    val ok = runCatching { audio.ringerMode = next; audio.ringerMode == next }.getOrDefault(false)
    if (!ok) return false
    BuiltinWidgetSignals.refresh++
    Toast.makeText(
        context,
        if (next == AudioManager.RINGER_MODE_VIBRATE) tr("מצב רטט") else tr("מצב צלצול"),
        Toast.LENGTH_SHORT,
    ).show()
    return true
}

/** 28sp - ערך השעון בווידג'ט (--fos-size-widget-clock). */
internal val WidgetClockSize: TextUnit = 28.sp
/**
 * צבעי iOS במצב כהה: הלבן של הטקסט הראשי, האפור המשני (secondaryLabel,
 * #EBEBF5 ב-60%) ומילוי משני (#767680 ב-24%) לטבעות, פסים ועיגולים.
 */
internal val ShellInk = Color.White
internal val ShellInk60 = Color(0x99EBEBF5)
internal val ShellInk15 = Color(0x3D767680)
/** המשטח של ווידג'ט iOS כהה - אטום, בלי קו מסגרת. */
internal val WidgetSurface = Color(0xFF1C1C1E)
/** צבעי הסטטוס של iOS (systemRed / systemGreen כהים). */
internal val WidgetDanger: Color = Color(0xFFFF453A)
internal val WidgetSuccess: Color = Color(0xFF30D158)

/**
 * הגוון של כל ווידג'ט - כמו ב-iOS, הכותרת וההדגשות (טבעת, היום בלוח, מחוג
 * השניות) בצבע של האפליקציה שמזינה אותו: כתום לשעון, אדום ליומן וכו'.
 */
internal val BuiltinWidget.tint: Color
    get() = when (this) {
        BuiltinWidget.CLOCK, BuiltinWidget.BIG_CLOCK, BuiltinWidget.ANALOG_CLOCK,
        BuiltinWidget.WORLD_CLOCK, BuiltinWidget.NEXT_ALARM -> Color(0xFFFF9F0A)
        BuiltinWidget.HEBREW_DATE, BuiltinWidget.MONTH_CALENDAR, BuiltinWidget.AGENDA -> Color(0xFFFF453A)
        BuiltinWidget.SUN_TIMES -> Color(0xFFFFD60A)
        BuiltinWidget.DEVICE, BuiltinWidget.BATTERY_RING -> Color(0xFF30D158)
        BuiltinWidget.STORAGE, BuiltinWidget.CONNECTIVITY -> Color(0xFF0A84FF)
        BuiltinWidget.SOUND -> Color(0xFFBF5AF2)
        BuiltinWidget.STEPS -> Color(0xFFFF375F)
        BuiltinWidget.FAVORITES -> Color(0xFF64D2FF)
        BuiltinWidget.AVOT -> Color(0xFFD4A373)
    }

/** הגוון של הווידג'ט שמצויר עכשיו (ר' [BuiltinWidgetContent]). */
internal val LocalWidgetTint = androidx.compose.runtime.staticCompositionLocalOf { Color(0xFF0A84FF) }

/** ההדגשה בתוך הווידג'ט - הגוון שלו, כמו ב-iOS (לא ההדגשה הכללית של המשתמש). */
@Composable
internal fun widgetAccent(@Suppress("UNUSED_PARAMETER") theme: FutureTheme): Color = LocalWidgetTint.current

/** גובה שורה 1.3 של הדיזיין סיסטם (--fos-line-height) - בלי זה Text יורש 24sp מ-Material ושורת 12sp תופסת 24dp. */
internal fun ws(size: TextUnit, weight: FontWeight = FontWeight.Normal, tabular: Boolean = false) = TextStyle(
    fontSize = size,
    lineHeight = 1.3.em,
    fontWeight = weight,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/**
 * השטח שנשאר לתוכן אחרי הריפוד. [tiny] - שורת ערך אחת בלבד (מצב עריכה בשורה
 * אחת); [compact] - כותרת + שורת ערך (שורה אחת, ~56dp); אחרת - כותרת, ערך
 * ושורות משנה כמה שנכנסות.
 */
internal class WidgetBox(val width: Dp, val height: Dp) {
    val tiny: Boolean get() = height < 42.dp
    val compact: Boolean get() = height < 66.dp
    val large: Boolean get() = height >= 112.dp && width >= 150.dp
}

/** גובה שורת טקסט בסגנון הזה (גודל × 1.3). */
@Composable
internal fun lineHeightOf(size: TextUnit): Dp = with(LocalDensity.current) { (size * 1.3f).toDp() }

@Composable
fun BuiltinWidgetContent(kind: BuiltinWidget, spanX: Int, spanY: Int, theme: FutureTheme) {
    val tick = rememberWidgetTick()
    androidx.compose.runtime.CompositionLocalProvider(LocalWidgetTint provides kind.tint) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .clip(FutureShapes.xl)
            .background(WidgetSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        val box = WidgetBox(maxWidth, maxHeight)
        // iOS: התוכן מתחיל מלמעלה; בשורה אחת אין מקום לרווח - ממורכז
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = if (box.compact) Arrangement.Center else Arrangement.Top,
        ) {
            when (kind) {
                BuiltinWidget.CLOCK -> ClockWidget(kind, tick, box)
                BuiltinWidget.AGENDA -> AgendaWidget(kind, tick, box)
                BuiltinWidget.DEVICE -> DeviceWidget(kind, tick, box)
                BuiltinWidget.BIG_CLOCK -> BigClockWidget(tick, box)
                BuiltinWidget.ANALOG_CLOCK -> AnalogClockWidget(kind, tick, box, theme)
                BuiltinWidget.WORLD_CLOCK -> WorldClockWidget(kind, tick, box)
                BuiltinWidget.NEXT_ALARM -> NextAlarmWidget(kind, tick, box)
                BuiltinWidget.HEBREW_DATE -> HebrewDateWidget(kind, tick, box)
                BuiltinWidget.MONTH_CALENDAR -> MonthCalendarWidget(kind, tick, box, theme)
                BuiltinWidget.SUN_TIMES -> SunTimesWidget(kind, tick, box)
                BuiltinWidget.BATTERY_RING -> BatteryRingWidget(kind, box, theme)
                BuiltinWidget.STORAGE -> StorageWidget(kind, tick, box, theme)
                BuiltinWidget.CONNECTIVITY -> ConnectivityWidget(kind, tick, box)
                BuiltinWidget.SOUND -> SoundWidget(kind, tick, box)
                BuiltinWidget.STEPS -> StepsWidget(kind, tick, box, theme)
                BuiltinWidget.FAVORITES -> FavoritesWidget(kind, tick, box)
                BuiltinWidget.AVOT -> AvotWidget(kind, tick, box)
            }
        }
    }
    }
}

// ---------------------------------------------------------------- שעון ותאריך

@Composable
private fun ClockWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val now = remember(tick) { Date() }
    val time = formatTime(context, now)
    val gregorian = remember(now) { SimpleDateFormat(tr("EEEE, d בMMMM"), Locale("iw", "IL")).format(now) }
    val hebrew = remember(now) { hebrewDate(now) }
    val alarm = remember(tick) { nextAlarm(context) }
    StandardWidget(
        kind, box,
        title = if (box.large) gregorian else tr("שעון"),
        value = time,
        lines = if (box.large) listOfNotNull(hebrew, alarm) else listOfNotNull(gregorian, hebrew, alarm),
        valueStyle = ws(WidgetClockSize, FontWeight.Bold, tabular = true),
        largeValueStyle = ws(FutureTypography.hero, FontWeight.Light, tabular = true),
        compactTrailing = gregorian,
    )
}

/** "14:05" או "2:05" לפי הגדרת 24 השעות של המערכת. */
internal fun formatTime(context: Context, date: Date, zone: java.util.TimeZone? = null): String {
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    return SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.US).apply { zone?.let { timeZone = it } }.format(date)
}

/** "ג׳ בכסלו תשפ״ז" - אותו פורמט כמו ווידג'ט היומן. */
internal fun hebrewDate(date: Date): String = runCatching {
    DateFormat.getDateInstance(DateFormat.LONG, ULocale.forLanguageTag("he-IL-u-ca-hebrew-nu-hebr")).format(date)
}.getOrDefault("")

/** המעורר הבא של המערכת - לא דורש הרשאה. */
private fun nextAlarm(context: Context): String? {
    val next = runCatching { context.getSystemService(AlarmManager::class.java)?.nextAlarmClock }.getOrNull() ?: return null
    return tr("מעורר") + " " + SimpleDateFormat("EEE HH:mm", Locale("iw", "IL")).format(Date(next.triggerTime))
}

// ---------------------------------------------------------------- אירועים קרובים

private sealed class Agenda {
    object NoPermission : Agenda()
    object Unavailable : Agenda()
    data class Events(val items: List<Pair<String, String>>) : Agenda()
}

@Composable
private fun AgendaWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val agenda by produceState<Agenda?>(null, tick) {
        value = withContext(Dispatchers.IO) { loadAgenda(context) }
    }
    val title = tr("אירועים קרובים")
    when (val a = agenda) {
        null -> StandardWidget(kind, box, title, tr("יומן"), emptyList())
        Agenda.NoPermission -> StandardWidget(kind, box, title, tr("יומן"), listOf(tr("פתח כדי לאשר גישה")))
        Agenda.Unavailable -> StandardWidget(kind, box, title, tr("יומן"), listOf(tr("לא ניתן לקרוא את היומן")))
        is Agenda.Events -> if (a.items.isEmpty()) {
            StandardWidget(kind, box, title, tr("אין אירועים"), listOf(tr("בשבוע הקרוב")))
        } else {
            val (first, whenLabel) = a.items.first()
            val more = a.items.drop(1).map { (t, w) -> "$t · $w" }
            StandardWidget(
                kind, box, title,
                value = first,
                lines = listOf(whenLabel) + more,
                valueFlexible = true,
                compactTrailing = if (a.items.size > 1) whenLabel + " · " + tr("ועוד {0}", a.items.size - 1) else whenLabel,
            )
        }
    }
}

/** האירועים מעכשיו ועד שבעה ימים קדימה (כולל אירוע שכבר התחיל ועוד לא נגמר). */
private fun loadAgenda(context: Context): Agenda {
    if (context.checkSelfPermission(Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return Agenda.NoPermission
    val now = System.currentTimeMillis()
    val end = now + 7L * 24 * 60 * 60 * 1000
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        .appendPath(now.toString()).appendPath(end.toString()).build()
    val cursor = runCatching {
        context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.ALL_DAY),
            "${CalendarContract.Instances.VISIBLE} = 1", null, "${CalendarContract.Instances.BEGIN} ASC",
        )
    }.getOrNull() ?: return Agenda.Unavailable // שאילתה שנכשלה היא לא "אין אירועים"
    val items = mutableListOf<Pair<String, String>>()
    cursor.use { c ->
        while (c.moveToNext() && items.size < 4) {
            val title = c.getString(0).orEmpty().ifBlank { tr("אירוע") }
            items += title to whenLabel(c.getLong(1), c.getInt(2) == 1, now)
        }
    }
    return Agenda.Events(items)
}

/** "היום 14:00" / "מחר 09:30" / "יום ג׳ 10:00", ובאירוע של יום שלם - בלי שעה. */
private fun whenLabel(begin: Long, allDay: Boolean, now: Long): String {
    fun dayIndex(ms: Long, utc: Boolean): Long {
        val cal = Calendar.getInstance(if (utc) java.util.TimeZone.getTimeZone("UTC") else java.util.TimeZone.getDefault())
        cal.timeInMillis = ms
        return cal.get(Calendar.YEAR) * 400L + cal.get(Calendar.DAY_OF_YEAR)
    }
    // אירוע של יום שלם נשמר בחצות UTC
    val today = dayIndex(now, false)
    val day = dayIndex(begin, allDay)
    val he = Locale("iw", "IL")
    val dayLabel = when (day - today) {
        0L -> tr("היום")
        1L -> tr("מחר")
        else -> SimpleDateFormat("EEEE", he).apply { if (allDay) timeZone = java.util.TimeZone.getTimeZone("UTC") }.format(Date(begin))
    }
    return if (allDay) dayLabel + " · " + tr("כל היום") else dayLabel + " " + SimpleDateFormat("HH:mm", Locale.US).format(Date(begin))
}

// ---------------------------------------------------------------- סוללה ומכשיר

/** אחוז הסוללה מתוך שידור ACTION_BATTERY_CHANGED. */
internal fun batteryLevel(battery: Intent?): Int? = battery?.let { b ->
    val l = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val s = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
    if (l >= 0 && s > 0) l * 100 / s else null
}

internal fun batteryCharging(battery: Intent?): Boolean = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let {
    it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
}

internal fun ringerLabel(mode: Int?): String? = when (mode) {
    AudioManager.RINGER_MODE_SILENT -> tr("שקט")
    AudioManager.RINGER_MODE_VIBRATE -> tr("רטט")
    AudioManager.RINGER_MODE_NORMAL -> tr("צלצול")
    else -> null
}

@Composable
private fun DeviceWidget(kind: BuiltinWidget, tick: Int, box: WidgetBox) {
    val context = LocalContext.current
    val battery = rememberStickyBroadcast(Intent.ACTION_BATTERY_CHANGED)
    val ringerTick = rememberBroadcastTick(AudioManager.RINGER_MODE_CHANGED_ACTION)
    val freeBytes by produceState<Long?>(null, tick) {
        value = withContext(Dispatchers.IO) {
            runCatching { StatFs(Environment.getDataDirectory().path).availableBytes }.getOrNull()
        }
    }
    val ringer = remember(tick, ringerTick) {
        runCatching { context.getSystemService(AudioManager::class.java)?.ringerMode }.getOrNull()
    }
    val ringerText = ringerLabel(ringer)
    val storageLabel = freeBytes?.let { tr("{0} פנויים", android.text.format.Formatter.formatShortFileSize(context, it)) }
    val chargingLabel = if (batteryCharging(battery)) tr("בטעינה") else null

    StandardWidget(
        kind, box, tr("סוללה"),
        value = batteryLevel(battery)?.let { "$it%" } ?: "--",
        lines = listOfNotNull(chargingLabel, ringerText?.let { tr("מצב: {0}", it) }, storageLabel),
        valueStyle = ws(FutureTypography.screenTitle, FontWeight.Bold, tabular = true),
        largeValueStyle = ws(FutureTypography.hero, FontWeight.Light, tabular = true),
        compactTrailing = listOfNotNull(chargingLabel, ringerText, storageLabel).joinToString(" · "),
    )
}

// ---------------------------------------------------------------- תבנית משותפת

/**
 * התבנית של רוב הווידג'טים: כותרת, ערך מודגש ושורות משנה - בכמה שנכנס.
 * [compactTrailing] - מה שמופיע ליד הערך כשאין מקום לשורות משנה (ברירת
 * מחדל: השורה הראשונה). [valueFlexible] - ערך טקסטואלי שיכול להיות ארוך
 * (שם אירוע) ומתחלק ברוחב עם הטקסט שלידו; ערך מספרי (שעה, אחוז) לא נחתך.
 */
@Composable
internal fun StandardWidget(
    kind: BuiltinWidget,
    box: WidgetBox,
    title: String,
    value: String,
    lines: List<String>,
    valueStyle: TextStyle = ws(FutureTypography.screenTitle, FontWeight.Bold),
    largeValueStyle: TextStyle? = null,
    compactTrailing: String? = lines.firstOrNull(),
    valueFlexible: Boolean = false,
) {
    when {
        box.tiny -> ValueLine(value, compactTrailing, valueStyle, valueFlexible)
        box.compact -> {
            WidgetHeader(kind, title)
            ValueLine(value, compactTrailing, valueStyle, valueFlexible, Modifier.padding(top = 2.dp))
        }
        else -> {
            val style = if (box.large && largeValueStyle != null) largeValueStyle else valueStyle
            WidgetHeader(kind, title)
            Text(
                value,
                color = ShellInk,
                style = style,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            )
            val used = 18.dp + 2.dp + lineHeightOf(style.fontSize)
            val lineH = lineHeightOf(FutureTypography.summary)
            val room = ((box.height - used) / lineH).toInt().coerceAtLeast(0)
            lines.filter { it.isNotBlank() }.take(room).forEach { WidgetSubtitle(it, maxLines = 1) }
        }
    }
}

/** ערך וטקסט משנה באותה שורה, מיושרים לקו הבסיס. */
@Composable
internal fun ValueLine(
    value: String,
    trailing: String?,
    style: TextStyle,
    valueFlexible: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Text(
            value,
            color = ShellInk,
            style = style,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.alignByBaseline().then(if (valueFlexible) Modifier.weight(1f, fill = false) else Modifier),
        )
        if (!trailing.isNullOrBlank()) {
            Spacer(Modifier.width(8.dp))
            Text(
                trailing,
                color = ShellInk60,
                style = ws(FutureTypography.summary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline().weight(1f, fill = false),
            )
        }
    }
}

/**
 * כותרת בסגנון iOS: סמל קווי בגוון של הווידג'ט ושם מודגש-למחצה באותו גוון
 * (כמו "יום רביעי" האדום של ווידג'ט היומן ב-iOS). בלי אייקון האפליקציה.
 */
@Composable
internal fun WidgetHeader(kind: BuiltinWidget, title: String) {
    val tint = LocalWidgetTint.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(18.dp)) {
        Icon(kind.fallbackIcon, contentDescription = null, tint = tint, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(5.dp))
        Text(title, color = tint, style = ws(FutureTypography.label, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun WidgetValue(text: String) {
    Text(
        text,
        color = ShellInk,
        style = ws(FutureTypography.screenTitle, FontWeight.Bold),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    )
}

@Composable
internal fun WidgetSubtitle(text: String, maxLines: Int = 2) {
    if (text.isBlank()) return
    Text(
        text,
        color = ShellInk60,
        style = ws(FutureTypography.summary),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * מונה שעולה בכל דקה (TIME_TICK), בשינוי שעה/אזור זמן/תאריך, בשינוי המעורר
 * הבא, בכל חזרה למסך הבית ובאות של [BuiltinWidgetSignals]. רישום דינמי בלבד -
 * TIME_TICK לא מגיע לרסיבר מהמניפסט.
 */
@Composable
private fun rememberWidgetTick(): Int {
    val tick = rememberBroadcastTick(
        Intent.ACTION_TIME_TICK,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
        Intent.ACTION_DATE_CHANGED,
        AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
    )
    var resumes by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumes++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return tick + resumes * 10_000 + BuiltinWidgetSignals.refresh * 100_000
}

/**
 * מונה שעולה בכל שידור מהפעולות האלה - לווידג'טים שמתעדכנים מאירוע (צלצול,
 * קישוריות) ולא רק מהדקה. רישום דינמי, מבוטל כשהווידג'ט יוצא מהמסך.
 */
@Composable
internal fun rememberBroadcastTick(vararg actions: String): Int {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    val key = actions.joinToString("|")
    DisposableEffect(context, key) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { tick++ }
        }
        val filter = IntentFilter().apply { actions.forEach { addAction(it) } }
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return tick
}

/** השידור האחרון של פעולה "דביקה" (סוללה) - הערך הנוכחי חוזר מיד מהרישום. */
@Composable
internal fun rememberStickyBroadcast(action: String): Intent? {
    val context = LocalContext.current
    var last by remember { mutableStateOf<Intent?>(null) }
    DisposableEffect(context, action) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { if (intent?.action == action) last = intent }
        }
        last = runCatching { context.registerReceiver(receiver, IntentFilter(action)) }.getOrNull()
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return last
}

/** Spacer קטן בין שורות בפריסות המותאמות. */
@Composable
internal fun Gap(h: Dp) = Spacer(Modifier.height(h))
