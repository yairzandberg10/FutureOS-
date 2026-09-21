package com.future.sharednav.systemui

/**
 * נקודת האמת היחידה לזהות איזו חבילה היא "אפליקציית ה-System UI" הפעילה
 * בסוויטה כרגע - ממנה מגיעים שידורי ה-broadcast הגלובליים (FutureUIActions)
 * וה-ContentProvider-ים של עיצוב/הגדרות שורת המצב ומסך הנעילה (ThemeClient,
 * SystemUiSettingsClient ב-Settings).
 *
 * כרגע: FutureUI (com.future.futureui) - האפליקציה הפעילה בפועל על המכשיר.
 * קיים גם פרויקט חלופי, com.android.sistemui (תיקיית SystemUI/ בשורש
 * הריפו) עם כל אותם רכיבים + הרחבות, אבל הוא בנוי כפרויקט Gradle עצמאי
 * לחלוטין - לא "מחובר לחשמל" - כדי לא לגעת ב-FutureUI הפעיל ובשאר
 * האפליקציות בלי בדיקה מלאה קודם.
 *
 * כשמחליטים לעבור בפועל ל-SystemUI החדש: לעדכן את שלושת הערכים למטה
 * (הם לא נגזרים אוטומטית זה מזה, כי שם התיקייה הפנימית של ספק ההגדרות
 * שונה בין שני הפרויקטים - "systemui" אצל FutureUI מול "provider" אצל
 * SystemUI, כדי למנוע כפל-מילה מול שם החבילה sistemui.systemui), להסיר/
 * להשבית את FutureUI, ולבנות+להתקין מחדש את כל 25 האפליקציות
 * (./build-all.sh --install) - בלי לחפש ולהחליף מחרוזות חבילה מפוזרות
 * בקוד של כל אפליקציה בנפרד.
 */
object SystemUiTarget {
    const val PACKAGE = "com.future.futureui"
    const val THEME_AUTHORITY = "com.future.futureui.theme"
    const val SETTINGS_AUTHORITY = "com.future.futureui.systemui"
}
