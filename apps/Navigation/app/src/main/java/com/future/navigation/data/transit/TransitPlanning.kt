package com.future.navigation.data.transit

import com.future.navigation.data.common.LatLng
import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** שעון ישראל - כל זמני התכנון והתצוגה. */
val ISRAEL_ZONE: ZoneId = ZoneId.of("Asia/Jerusalem")

/** סוג הזמן שנבחר במסך "מתי": יציאה עכשיו / בזמן / הגעה עד / קווים אחרונים להיום. */
enum class TripWhenKind { NOW, DEPART_AT, ARRIVE_BY, LAST_LINES }

/** הבחירה במסך "מתי" + ההזזה המצטברת של ±10 דק' במסך התוצאות. */
data class TripWhen(
    val kind: TripWhenKind = TripWhenKind.NOW,
    val hour: Int = 0,
    val minute: Int = 0,
    val shiftMinutes: Int = 0,
)

/** מה שנשלח ל-MOTIS: time (null = עכשיו) ו-arriveBy. */
data class PlanTime(val iso: String?, val arriveBy: Boolean)

/** העדפת מסלול (מסך ההעדפות). */
enum class RoutePreference(val label: String) {
    RECOMMENDED("מומלץ"),
    LESS_WALKING("פחות הליכה"),
    FEWER_TRANSFERS("פחות החלפות"),
    WHEELCHAIR("נגישות לכסא גלגלים");

    fun next(): RoutePreference = values()[(ordinal + 1) % values().size]
}

/** העדפות התחבורה שנשמרות ב-SharedPreferences (ר' TransitPrefsStore). */
data class TransitPrefs(
    val preference: RoutePreference = RoutePreference.RECOMMENDED,
    val bus: Boolean = true,
    val rail: Boolean = true,
    val lightRail: Boolean = true,
    /** מוניות שירות/שאטלים: נשמר להעדפה, אבל אין כאן שם מצב MOTIS מאומת - לא נשלח. */
    val shareTaxi: Boolean = true,
)

object TransitPlanning {
    const val LAST_LINES_HOUR = 23
    const val SHIFT_STEP_MINUTES = 10
    private const val DAY_SECONDS = 24 * 3600
    private val ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    /**
     * זמן התכנון ל-MOTIS. בלי בחירה ובלי הזזה - null (MOTIS משתמש ב"עכשיו").
     * הזזה מעבירה תמיד לזמן מפורש. יציאה בזמן שכבר עבר היום - נספרת למחר.
     */
    fun planTime(choice: TripWhen, now: ZonedDateTime): PlanTime {
        val local = now.withZoneSameInstant(ISRAEL_ZONE)
        val base: ZonedDateTime? = when (choice.kind) {
            TripWhenKind.NOW -> null
            TripWhenKind.LAST_LINES -> local.toLocalDate().atTime(LAST_LINES_HOUR, 0).atZone(ISRAEL_ZONE)
            TripWhenKind.DEPART_AT, TripWhenKind.ARRIVE_BY -> {
                val t = local.toLocalDate().atTime(choice.hour, choice.minute).atZone(ISRAEL_ZONE)
                if (choice.kind == TripWhenKind.DEPART_AT && t.isBefore(local)) t.plusDays(1) else t
            }
        }
        val shift = choice.shiftMinutes.toLong()
        val target = when {
            base != null -> base.plusMinutes(shift)
            shift != 0L -> local.plusMinutes(shift)
            else -> return PlanTime(null, false)
        }
        return PlanTime(
            iso = target.withSecond(0).withNano(0).format(ISO),
            arriveBy = choice.kind == TripWhenKind.ARRIVE_BY,
        )
    }

    /** כותרת הבחירה לשורת הרמז במסך התוצאות. */
    fun whenLabel(choice: TripWhen): String {
        val base = when (choice.kind) {
            TripWhenKind.NOW -> "יציאה עכשיו"
            TripWhenKind.DEPART_AT -> "יציאה ב-%02d:%02d".format(choice.hour, choice.minute)
            TripWhenKind.ARRIVE_BY -> "הגעה עד %02d:%02d".format(choice.hour, choice.minute)
            TripWhenKind.LAST_LINES -> "קווים אחרונים להיום"
        }
        val shift = choice.shiftMinutes
        if (shift == 0) return base
        return base + " (" + (if (shift > 0) "+" else "") + shift + " דק׳)"
    }

    /**
     * פרמטרים נוספים ל-/plan לפי ההעדפות. שולחים רק מה שלא ברירת המחדל,
     * כך שהבקשה עם ברירות המחדל זהה למה שנשלח עד עכשיו.
     */
    fun planParams(prefs: TransitPrefs): Map<String, String> {
        val out = linkedMapOf<String, String>()
        when (prefs.preference) {
            RoutePreference.RECOMMENDED -> Unit
            RoutePreference.LESS_WALKING -> {
                out["maxPreTransitTime"] = "600"
                out["maxPostTransitTime"] = "600"
            }
            RoutePreference.FEWER_TRANSFERS -> out["maxTransfers"] = "1"
            RoutePreference.WHEELCHAIR -> out["pedestrianProfile"] = "WHEELCHAIR"
        }
        if (!(prefs.bus && prefs.rail && prefs.lightRail)) {
            val modes = buildList {
                if (prefs.bus) add("BUS")
                if (prefs.rail) add("SUBURBAN")
                if (prefs.lightRail) {
                    add("TRAM")
                    add("SUBWAY")
                }
            }
            if (modes.isNotEmpty()) out["transitModes"] = modes.joinToString(",")
        }
        return out
    }

    /**
     * מחיר נסיעה משוער לפי מרחק (ק"מ), טבלה ארצית פשוטה: עד 15 ק"מ ₪8,
     * 15-40 ק"מ ₪12.5, 40-120 ק"מ ₪19, מעל 120 ק"מ ₪27.5.
     */
    fun estimatedFareShekel(distanceMeters: Double): Double = when {
        distanceMeters <= 15_000.0 -> 8.0
        distanceMeters <= 40_000.0 -> 12.5
        distanceMeters <= 120_000.0 -> 19.0
        else -> 27.5
    }

    /**
     * סכום מרחקי הקווים (ישר בין תחנות עלייה וירידה). רגל הליכה מעדכנת את
     * נקודת ההמשך, כך שהחלפות לא נספרות. קירוב - לא מרחק ברשת הכבישים.
     */
    fun rideDistanceMeters(legs: List<TransitLeg>): Double {
        var prev: LatLng? = null
        var total = 0.0
        for (leg in legs) {
            if (leg.type == LegType.RIDE) {
                val from = leg.fromLocation ?: prev
                val to = leg.toLocation
                if (from != null && to != null) total += from.distanceMetersTo(to)
                if (to != null) prev = to
            } else {
                leg.toLocation?.let { prev = it }
            }
        }
        return total
    }

    fun estimatedFare(itinerary: TransitItinerary): Double =
        estimatedFareShekel(rideDistanceMeters(itinerary.legs))

    /** "₪8" / "₪12.5" - מספר ספרות אחת אחרי הנקודה רק כשצריך. */
    fun formatShekel(value: Double): String =
        if (value % 1.0 == 0.0) "₪${value.toInt()}" else "₪" + String.format(Locale.US, "%.1f", value)

    /** שניות מאז חצות בשעון ישראל. */
    fun secondsOfDay(now: ZonedDateTime): Int =
        now.withZoneSameInstant(ISRAEL_ZONE).toLocalTime().toSecondOfDay()

    /** דקות עד יציאה, עם גלישה דרך חצות. */
    fun minutesUntil(departureSeconds: Int, nowSeconds: Int): Int {
        val diff = ((departureSeconds - nowSeconds) % DAY_SECONDS + DAY_SECONDS) % DAY_SECONDS
        return diff / 60
    }

    fun clock(seconds: Int): String {
        val s = ((seconds % DAY_SECONDS) + DAY_SECONDS) % DAY_SECONDS
        return "%02d:%02d".format(s / 3600, (s % 3600) / 60)
    }

    /** טקסט לשליחה ב-SMS: סיכום המסלול הצעד אחר צעד. */
    fun shareText(itinerary: TransitItinerary): String = buildString {
        appendLine(
            "מסלול תחבורה ציבורית: יציאה ${clock(itinerary.departureSeconds)}, " +
                "הגעה ${clock(itinerary.arrivalSeconds)} (${itinerary.totalDurationSeconds / 60} דק׳)"
        )
        itinerary.legs.forEachIndexed { index, leg ->
            when (leg.type) {
                LegType.WALK -> appendLine("${index + 1}. הליכה אל ${leg.toStopName} (${(leg.walkDurationSeconds ?: 0) / 60} דק׳)")
                LegType.RIDE -> appendLine(
                    "${index + 1}. קו ${leg.routeShortName.orEmpty()} ${leg.routeLongName.orEmpty()}: " +
                        "עלייה ${leg.fromStopName} ${leg.departureSeconds?.let { clock(it) }.orEmpty()}, " +
                        "ירידה ${leg.toStopName} ${leg.arrivalSeconds?.let { clock(it) }.orEmpty()}"
                )
            }
        }
        append("מחיר משוער: ${formatShekel(estimatedFare(itinerary))}")
    }
}
