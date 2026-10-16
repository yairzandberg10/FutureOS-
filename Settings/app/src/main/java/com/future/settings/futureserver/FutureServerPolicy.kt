package com.future.settings.futureserver

/**
 * לוגיקה טהורה של רישום השרת (בלי ספריות Android): חלון ה-15 דקות ובחירת כתובת הבסיס.
 * מופרדת כדי שאפשר לבדוק אותה בבדיקת יחידה רגילה על ה-JVM.
 */
object FutureServerPolicy {
    /** מינימום זמן בין שני ניסיונות רישום, גם אם הניסיון הקודם נכשל. */
    const val MIN_ATTEMPT_INTERVAL_MS: Long = 15L * 60L * 1000L

    /**
     * האם מותר לנסות רישום עכשיו. lastAttemptMs = 0 אומר שמעולם לא נוסה.
     * אם השעון הוחזר אחורה (now < last) מתירים ניסיון, כדי שהחלון לא ייתקע לנצח.
     */
    fun canAttempt(nowMs: Long, lastAttemptMs: Long): Boolean {
        if (lastAttemptMs <= 0L) return true
        if (nowMs < lastAttemptMs) return true
        return nowMs - lastAttemptMs >= MIN_ATTEMPT_INTERVAL_MS
    }

    /**
     * בוחר כתובת בסיס: קודם ro.futureos.api_base, ואם ריקה - BuildConfig.FUTURE_API_BASE.
     * מחזיר null כששתיהן ריקות (אז לא נשלח כלום). סלש בסוף מוסר, כדי שלא ייווצר "//register".
     */
    fun resolveBaseUrl(systemProp: String?, buildConfigBase: String?): String? {
        val picked = systemProp.cleaned() ?: buildConfigBase.cleaned() ?: return null
        return picked.trimEnd('/').takeIf { it.isNotEmpty() }
    }

    private fun String?.cleaned(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
