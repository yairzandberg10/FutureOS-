package com.future.navigation.data.payment

import com.future.navigation.data.common.distanceMetersTo
import com.future.navigation.data.gtfs.LegType
import com.future.navigation.data.gtfs.TransitItinerary
import com.future.navigation.data.gtfs.TransitLeg

/**
 * טבעות המרחק של רפורמת "דרך שווה" ומחיר נסיעה בודדת בכל אחת, באגורות.
 *
 * המקור: מחירון התעריפים הרשמי שמפרסמת רב-פס (הופאון) מטעם משרד התחבורה -
 * https://s3-eu-west-1.amazonaws.com/static.hopon.co.il/mot/ravPassPrices.html
 * (נטען ונקרא בפועל ב-1.10.2026, לא מהזיכרון). התעריפים זהים בתיקוף ברב-קו
 * ובתיקוף מהנייד בכל אחת מהאפליקציות המורשות. התעריפים מתעדכנים כל שנה
 * ב-25 ביוני - זה המקום היחיד שצריך לשנות.
 *
 * [busAgorot] חל גם על רכבת קלה, מטרונית, כרמלית ורכבלית. [railAgorot] הוא
 * רכבת ישראל - null בטבעת שאין בה מחיר רכבת.
 */
enum class FareRadius(val label: String, val maxKm: Double, val busAgorot: Int, val railAgorot: Int?) {
    YELLOW("צהוב", 15.0, 800, 1150),
    GREEN("ירוק", 40.0, 1450, 2100),
    LIGHT_BLUE("תכלת", 75.0, 1900, 2700),
    BLUE("כחול", 120.0, 1900, 3050),
    PURPLE("סגול", 225.0, 3050, 5250),
    GREY("אפור", Double.MAX_VALUE, 8732, null);

    companion object {
        fun forKm(km: Double): FareRadius = entries.first { km <= it.maxKm }
    }
}

enum class FareMode { BUS, LIGHT_RAIL, RAIL }

/**
 * מחיר קטע נסיעה אחד. [agorot] הוא null כשאין מיקום לתחנות (מסלול ישן מלפני
 * העדכון) ואי אפשר לחשב מרחק. [isFreeTransfer] - נסיעת המשך בתוך 90 הדקות של
 * הטבעת הצהובה, בלי תשלום נוסף.
 */
data class RideFare(
    val leg: TransitLeg,
    val mode: FareMode,
    val distanceKm: Double?,
    val radius: FareRadius?,
    val agorot: Int?,
    val isFreeTransfer: Boolean
)

data class TripFare(val rides: List<RideFare>) {
    val totalAgorot: Int get() = rides.sumOf { it.agorot ?: 0 }
    /** false אם לפחות קטע אחד לא תומחר - הסכום הכולל אז הוא הערכה חסרה. */
    val isComplete: Boolean get() = rides.all { it.agorot != null }
}

/**
 * מחיר הנסיעה לפי כללי "דרך שווה", כפי שהם מתוארים באותו מחירון:
 *  - מחיר נסיעה בודדת נקבע לפי המרחק בקו אווירי מתחנת העלייה לתחנת הירידה.
 *  - נסיעה בטבעת הצהובה (עד 15 ק"מ) כוללת מעבר לקו אחר במשך 90 דקות מהתיקוף
 *    הראשון בלי תשלום נוסף. מעל 15 ק"מ - נסיעה בודדת בלי מעבר חינם.
 *  - ברכבת ישראל אין נסיעת המשך של 90 דקות, והמחיר בפועל נקבע לפי מחירון
 *    הרכבת בין תחנות - כאן זו הערכה לפי אותה טבעת.
 *
 * מחיר לפרופיל רגיל, בלי הנחות (אזרח ותיק, נוער, סטודנט) - ההנחה מחושבת אצל
 * גורם התשלום לפי הפרופיל שמוגדר שם.
 */
object FareCalculator {

    private const val FREE_TRANSFER_WINDOW_SECONDS = 90 * 60

    fun calculate(itinerary: TransitItinerary): TripFare {
        var transferWindowStart: Int? = null
        val rides = itinerary.legs.filter { it.type == LegType.RIDE }.map { leg ->
            val mode = when (leg.routeType) {
                0 -> FareMode.LIGHT_RAIL
                2 -> FareMode.RAIL
                else -> FareMode.BUS
            }
            val from = leg.fromStopLocation
            val to = leg.toStopLocation
            if (from == null || to == null) {
                return@map RideFare(leg, mode, null, null, null, isFreeTransfer = false)
            }
            val km = from.distanceMetersTo(to) / 1000.0
            val radius = FareRadius.forKm(km)
            val departure = leg.departureSeconds

            if (mode != FareMode.RAIL && radius == FareRadius.YELLOW) {
                val windowStart = transferWindowStart
                if (windowStart != null && departure != null && departure - windowStart <= FREE_TRANSFER_WINDOW_SECONDS) {
                    return@map RideFare(leg, mode, km, radius, 0, isFreeTransfer = true)
                }
                transferWindowStart = departure
            }
            val price = if (mode == FareMode.RAIL) radius.railAgorot ?: radius.busAgorot else radius.busAgorot
            RideFare(leg, mode, km, radius, price, isFreeTransfer = false)
        }
        return TripFare(rides)
    }

    /** "₪8", "₪14.5", "₪87.32" - כמו במחירון הרשמי. */
    fun format(agorot: Int): String {
        val shekels = agorot / 100
        val rest = agorot % 100
        return when {
            rest == 0 -> "₪$shekels"
            rest % 10 == 0 -> "₪$shekels.${rest / 10}"
            else -> "₪$shekels.${"%02d".format(rest)}"
        }
    }
}
