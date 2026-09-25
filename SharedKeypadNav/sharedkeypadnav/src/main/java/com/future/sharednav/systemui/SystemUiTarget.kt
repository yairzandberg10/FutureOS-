package com.future.sharednav.systemui

/**
 * נקודת האמת היחידה לזהות איזו חבילה היא "אפליקציית ה-System UI" הפעילה
 * בסוויטה כרגע - ממנה מגיעים שידורי ה-broadcast הגלובליים (FutureUIActions)
 * וה-ContentProvider-ים של עיצוב/הגדרות שורת המצב ומסך הנעילה (ThemeClient,
 * SystemUiSettingsClient ב-Settings).
 *
 * כרגע: FutureUI (com.future.futureui) - האפליקציה הפעילה בפועל על המכשיר.
 * קיים גם פרויקט חלופי, com.android.sistemui (תיקיית SystemUI/ בשורש
 * הריפו), בנוי כפרויקט Gradle עצמאי - לא "מחובר לחשמל". מאז 2026-09-25
 * הוא מראה מדויקת של FutureUI: SystemUI/sync-from-futureui.sh מעתיק אליו
 * את כל הקוד ומחליף רק את שם החבילה.
 *
 * כשמחליטים לעבור בפועל ל-SystemUI: לעדכן את שלושת הערכים למטה
 * (com.android.sistemui, com.android.sistemui.theme ו-
 * com.android.sistemui.systemui), להסיר/
 * להשבית את FutureUI, ולבנות+להתקין מחדש את כל 25 האפליקציות
 * (./build-all.sh --install) - בלי לחפש ולהחליף מחרוזות חבילה מפוזרות
 * בקוד של כל אפליקציה בנפרד.
 */
object SystemUiTarget {
    const val PACKAGE = "com.future.futureui"
    const val THEME_AUTHORITY = "com.future.futureui.theme"
    const val SETTINGS_AUTHORITY = "com.future.futureui.systemui"
}
