package com.future.navigation.data.payment

import android.net.Uri
import com.future.navigation.data.backend.RemoteKeys

/** אפליקציית תשלום מורשית של משרד התחבורה, לפי שם החבילה שלה ב-Google Play. */
data class PaymentApp(val packageName: String, val title: String, val summary: String)

/**
 * כתובות ושמות החבילות של התשלום בתחבורה הציבורית.
 *
 * אין API ציבורי לחיוב נסיעה: רק גורמים שקיבלו רישיון ממשרד התחבורה מחייבים
 * בפועל (רב-קו אונליין, ואפליקציות התיקוף מהנייד). לכן האפליקציה לא מחייבת
 * בעצמה - היא מעבירה את המשתמש לגורם המורשה, עם הפרטים שלו כבר ממולאים.
 *
 * כל הערכים כאן אומתו מול מקורות אמיתיים ב-1.10.2026 (דפי Google Play של
 * כל אפליקציה, ודף הזמנת החוזה של רב-קו אונליין) - לא נוחשו.
 */
object PaymentConfig {

    /**
     * "הזמנת חוזה" ברב-קו אונליין: מספר כרטיס הרב-קו, פרופיל, חוזה (ערך צבור,
     * נסיעה בודדת, חופשי יומי/חודשי) ותשלום בכרטיס אשראי. החוזה נטען לכרטיס
     * אחר כך בעמדת טעינה או במכשיר תומך, והחיוב מתבצע רק אחרי טעינה מוצלחת.
     * ההזמנה תקפה 7 ימים. מ-Remote Config כשיש Firebase, כדי שאפשר יהיה לתקן
     * כתובת שהשתנתה בלי בנייה מחדש.
     */
    const val DEFAULT_WEB_URL = "https://ravkavonline.co.il/en/ravkav-contract-reservation"
    val webUrl: String get() = RemoteKeys.paymentWebUrl.takeIf { it.startsWith("https://") } ?: DEFAULT_WEB_URL

    /**
     * מילוי אוטומטי של הפרטים רק בעמודים של המארחים האלה, ורק ב-HTTPS. אם
     * המשתמש לוחץ על קישור שיוצא לאתר אחר, תעודת הזהות והטלפון שלו לא
     * נשפכים לשם.
     */
    private val AUTOFILL_HOSTS = listOf("ravkavonline.co.il")

    fun isAutofillAllowed(url: String?): Boolean {
        val uri = url?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return false
        if (uri.scheme != "https") return false
        val host = uri.host?.lowercase() ?: return false
        return AUTOFILL_HOSTS.any { host == it || host.endsWith(".$it") }
    }

    /** אפליקציות התשלום המורשות - מוצגות רק אם הן מותקנות במכשיר. */
    val APPS = listOf(
        PaymentApp("co.hopon.client", "רב-פס (הופאון)", "סריקת QR באוטובוס, חיוב בסוף החודש"),
        PaymentApp("com.tranzmate", "מוביט", "תשלום ותיקוף מהנייד, בשיתוף פנגו"),
        PaymentApp("com.isracard.payments", "ANYWAY (ישראכרט)", "תשלום ותיקוף מהנייד"),
        PaymentApp("com.pcentra.ravkavonlinemobile", "רב-קו אונליין", "טעינת כרטיס הרב-קו ב-NFC"),
    )
}
