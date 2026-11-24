package com.future.futurelauncher.ui

import android.Manifest
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.icu.text.DateFormat
import android.icu.util.ULocale
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.CalendarContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.future.sharednav.i18n.tr
import com.future.sharednav.icons.FutureIcons
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
 * מתעדכנים חי (שעון, סוללה) ונשמרים בפריסה כ-LauncherItem.Widget עם [key]
 * בשדה builtin ו-widgetId = -1.
 *
 * המראה הוא תבנית הווידג'ט של הדיזיין סיסטם (Widget.prompt.md /
 * FutureContentWidget): משטח ההתראה הצפה (#1C1C1E ב-90%, קו שיער 0.5dp לבן
 * 15%), רדיוס 22dp, ריפוד 16/10, אייקון האפליקציה שמזינה אותו 18dp + כותרת
 * 12sp ב-60%, ערך מודגש 20sp (28sp לשעון), שורת משנה 13sp ב-60%. תמיד כהה -
 * הוא יושב על הטפט. אין לו מצב פוקוס משלו (הסמן של הרשת), ו-OK פותח את
 * האפליקציה שמזינה אותו.
 *
 * [minSpanX] - מתחת לשתי עמודות אין מקום לטקסט, אז ווידג'ט מובנה לא מצטמצם
 * לתא בודד (לא בהוספה ולא בשינוי גודל).
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
) {
    CLOCK("clock", "שעון ותאריך", "שעה, תאריך עברי ולועזי ומעורר הבא", 4, 2, "com.future.clock", FutureIcons.Alarm),
    AGENDA("agenda", "אירועים קרובים", "האירועים הבאים מהיומן", 4, 1, "com.future.calendar", FutureIcons.CalendarToday),
    DEVICE("device", "סוללה ומכשיר", "סוללה, מצב צלצול ואחסון פנוי", 2, 1, "com.future.settings", FutureIcons.BatteryFull);

    val minSpanX: Int get() = 2

    companion object {
        fun fromKey(key: String?): BuiltinWidget? = entries.firstOrNull { it.key == key }
    }
}

/**
 * אות רענון לכל הווידג'טים המובנים - עולה כשהמשתמש אישר גישה ליומן, כדי
 * שווידג'ט האירועים יקרא שוב מיד ולא רק בדקה הבאה.
 */
object BuiltinWidgetSignals {
    var refresh by mutableIntStateOf(0)
}

/** 28sp - ערך השעון בווידג'ט (--fos-size-widget-clock). */
private val WidgetClockSize: TextUnit = 28.sp
/** הלבן של המעטפת ו-60% שלו (--fos-shell-ink / --fos-shell-ink-60). */
private val ShellInk = Color.White
private val ShellInk60 = Color.White.copy(alpha = 0.6f)
/** גובה שורה 1.3 של הדיזיין סיסטם (--fos-line-height) - בלי זה Text יורש 24sp מ-Material ושורת 12sp תופסת 24dp. */
private fun ws(size: TextUnit, weight: FontWeight = FontWeight.Normal, tabular: Boolean = false) = TextStyle(
    fontSize = size,
    lineHeight = 1.3.em,
    fontWeight = weight,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

@Composable
fun BuiltinWidgetContent(kind: BuiltinWidget, spanX: Int, spanY: Int, theme: FutureTheme) {
    val tick = rememberWidgetTick()
    // ווידג'ט "גדול" - לפחות שתי שורות ושלוש עמודות: הערך עובר לגודל hero
    // (48sp, ערך יחיד שהוא כל התוכן) ויש מקום לשורת משנה לכל עובדה.
    val large = spanY >= 2 && spanX >= 3
    val tall = spanY >= 2
    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(FutureShapes.xl)
            .background(theme.headsUpSurfaceColor)
            .border(0.5.dp, theme.headsUpBorderColor, FutureShapes.xl)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        when (kind) {
            BuiltinWidget.CLOCK -> ClockWidget(kind, tick, large, tall)
            BuiltinWidget.AGENDA -> AgendaWidget(kind, tick, tall)
            BuiltinWidget.DEVICE -> DeviceWidget(kind, tick, large, tall)
        }
    }
}

// ---------------------------------------------------------------- שעון ותאריך

@Composable
private fun ClockWidget(kind: BuiltinWidget, tick: Int, large: Boolean, tall: Boolean) {
    val context = LocalContext.current
    val now = remember(tick) { Date() }
    val is24 = android.text.format.DateFormat.is24HourFormat(context)
    val time = remember(now, is24) { SimpleDateFormat(if (is24) "HH:mm" else "h:mm", Locale.US).format(now) }
    val gregorian = remember(now) { SimpleDateFormat(tr("EEEE, d בMMMM"), Locale("iw", "IL")).format(now) }
    val hebrew = remember(now) { hebrewDate(now) }
    val alarm = remember(tick) { nextAlarm(context) }

    WidgetHeader(kind, if (large) gregorian else tr("שעון"))
    Text(
        time,
        color = ShellInk,
        style = if (large) ws(FutureTypography.hero, FontWeight.Light, tabular = true) else ws(WidgetClockSize, FontWeight.Bold, tabular = true),
        maxLines = 1,
        modifier = Modifier.padding(top = 2.dp),
    )
    if (large) {
        WidgetSubtitle(hebrew)
        alarm?.let { WidgetSubtitle(it) }
    } else if (tall) {
        WidgetSubtitle(gregorian)
        WidgetSubtitle(listOfNotNull(hebrew, alarm).joinToString(" · "), maxLines = 2)
    } else {
        WidgetSubtitle(listOfNotNull(gregorian, alarm).joinToString(" · "), maxLines = 1)
    }
}

/** "ג׳ בכסלו תשפ״ז" - אותו פורמט כמו ווידג'ט היומן. */
private fun hebrewDate(date: Date): String = runCatching {
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
private fun AgendaWidget(kind: BuiltinWidget, tick: Int, tall: Boolean) {
    val context = LocalContext.current
    val agenda by produceState<Agenda?>(null, tick) {
        value = withContext(Dispatchers.IO) { loadAgenda(context) }
    }
    WidgetHeader(kind, tr("אירועים קרובים"))
    when (val a = agenda) {
        null -> WidgetValue(tr("יומן"))
        Agenda.NoPermission -> {
            WidgetValue(tr("יומן"))
            WidgetSubtitle(tr("פתח כדי לאשר גישה"))
        }
        Agenda.Unavailable -> {
            WidgetValue(tr("יומן"))
            WidgetSubtitle(tr("לא ניתן לקרוא את היומן"))
        }
        is Agenda.Events -> if (a.items.isEmpty()) {
            WidgetValue(tr("אין אירועים"))
            WidgetSubtitle(tr("בשבוע הקרוב"))
        } else {
            val (title, whenLabel) = a.items.first()
            WidgetValue(title)
            WidgetSubtitle(
                if (a.items.size > 1 && !tall) whenLabel + " · " + tr("ועוד {0}", a.items.size - 1) else whenLabel,
                maxLines = 1,
            )
            if (tall) {
                a.items.drop(1).take(2).forEach { (t, w) ->
                    Spacer(Modifier.height(4.dp))
                    Text(t, color = ShellInk, style = ws(FutureTypography.body, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    WidgetSubtitle(w, maxLines = 1)
                }
            }
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
        while (c.moveToNext() && items.size < 3) {
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

private data class DeviceInfo(val level: Int?, val charging: Boolean, val ringer: Int?, val freeBytes: Long?)

@Composable
private fun DeviceWidget(kind: BuiltinWidget, tick: Int, large: Boolean, tall: Boolean) {
    val context = LocalContext.current
    var battery by remember { mutableStateOf<Intent?>(null) }
    var ringerTick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_BATTERY_CHANGED) battery = intent else ringerTick++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
        }
        // השידור של הסוללה "דביק" - הערך הנוכחי חוזר מיד מהרישום
        battery = runCatching { context.registerReceiver(receiver, filter) }.getOrNull()
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val freeBytes by produceState<Long?>(null, tick) {
        value = withContext(Dispatchers.IO) {
            runCatching { StatFs(Environment.getDataDirectory().path).availableBytes }.getOrNull()
        }
    }
    val ringer = remember(tick, ringerTick) {
        runCatching { context.getSystemService(AudioManager::class.java)?.ringerMode }.getOrNull()
    }
    val info = DeviceInfo(
        level = battery?.let { b ->
            val l = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val s = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            if (l >= 0 && s > 0) l * 100 / s else null
        },
        charging = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1).let {
            it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL
        },
        ringer = ringer,
        freeBytes = freeBytes,
    )

    val ringerLabel = when (info.ringer) {
        AudioManager.RINGER_MODE_SILENT -> tr("שקט")
        AudioManager.RINGER_MODE_VIBRATE -> tr("רטט")
        AudioManager.RINGER_MODE_NORMAL -> tr("צלצול")
        else -> null
    }
    val storageLabel = info.freeBytes?.let { tr("{0} פנויים", android.text.format.Formatter.formatShortFileSize(context, it)) }
    val chargingLabel = if (info.charging) tr("בטעינה") else null

    WidgetHeader(kind, tr("סוללה"))
    Text(
        info.level?.let { "$it%" } ?: "--",
        color = ShellInk,
        style = if (large) ws(FutureTypography.hero, FontWeight.Light, tabular = true) else ws(FutureTypography.screenTitle, FontWeight.Bold, tabular = true),
        maxLines = 1,
        modifier = Modifier.padding(top = 4.dp),
    )
    if (tall) {
        listOfNotNull(chargingLabel, ringerLabel?.let { tr("מצב: {0}", it) }, storageLabel).forEach { WidgetSubtitle(it, maxLines = 1) }
    } else {
        WidgetSubtitle(listOfNotNull(chargingLabel, ringerLabel, storageLabel).joinToString(" · "), maxLines = 1)
    }
}

// ---------------------------------------------------------------- תבנית משותפת

/** אייקון האפליקציה שמזינה (18dp, כפי שהוא - בלי צביעה) ושם 12sp ב-60%. */
@Composable
private fun WidgetHeader(kind: BuiltinWidget, title: String) {
    val context = LocalContext.current
    val icon by produceState<ImageBitmap?>(null, kind.appPackage) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(kind.appPackage).toBitmap(48, 48).asImageBitmap() }.getOrNull()
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
            val bmp = icon
            if (bmp != null) {
                Image(bmp, contentDescription = null, modifier = Modifier.size(18.dp).clip(RoundedCornerShape(percent = 28)))
            } else {
                Icon(kind.fallbackIcon, contentDescription = null, tint = ShellInk60, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Text(title, color = ShellInk60, style = ws(FutureTypography.label), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun WidgetValue(text: String) {
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
private fun WidgetSubtitle(text: String, maxLines: Int = 2) {
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
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) { tick++ }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
        }
        runCatching { context.registerReceiver(receiver, filter) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) tick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return tick + BuiltinWidgetSignals.refresh * 100_000
}
