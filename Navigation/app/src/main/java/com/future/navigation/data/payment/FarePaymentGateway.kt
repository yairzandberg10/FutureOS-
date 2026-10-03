package com.future.navigation.data.payment

/** כל מה שגורם תשלום מורשה צריך כדי לחייב תיקוף של נסיעה אחת. */
data class FarePaymentRequest(
    /** תוכן הברקוד כפי שנסרק - גורם התשלום מפענח אותו בעצמו. */
    val qrRaw: String,
    val vehicleNumber: String?,
    /** route_id של GTFS (= LineRef ב-SIRI). */
    val routeId: String,
    val lineName: String,
    val tripId: String,
    val boardingStopId: String,
    val alightingStopId: String,
    val distanceKm: Double,
    val agorot: Int,
)

sealed interface FarePaymentResult {
    /** חויב, והכרטיס הונפק אצל גורם התשלום. [confirmation] - מה שמציגים לביקורת. */
    data class Approved(val confirmation: String) : FarePaymentResult
    data class Declined(val reason: String) : FarePaymentResult
    /** אין חיבור לגורם מורשה - לא חויב כלום ולא הונפק כרטיס. */
    data object NotConnected : FarePaymentResult
}

/**
 * נקודת החיבור היחידה בין מסך הסריקה לגורם שמחייב בפועל.
 *
 * חיוב נסיעה עובר דרך מערכת הסליקה של משרד התחבורה, ורק גופים מורשים
 * מחוברים אליה (פנגו - דרכה מוביט גובה, הופאון רב-פס, סלופארק, ישראכרט,
 * רב-קו אונליין). אין API ציבורי. כשיהיה הסכם עם אחד מהם, כותבים מימוש
 * של הממשק הזה לפי ה-API שלו ומחליפים את [current] - שום מסך לא משתנה.
 *
 * עד אז [NotConnectedGateway]: לא מחייב, לא מנפיק "כרטיס" שנראה אמיתי,
 * ואומר את זה במפורש.
 */
fun interface FarePaymentGateway {
    suspend fun pay(request: FarePaymentRequest): FarePaymentResult

    companion object {
        val current: FarePaymentGateway = NotConnectedGateway
    }
}

object NotConnectedGateway : FarePaymentGateway {
    override suspend fun pay(request: FarePaymentRequest): FarePaymentResult = FarePaymentResult.NotConnected
}
