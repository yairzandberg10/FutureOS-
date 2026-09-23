package com.future.sharednav.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * סקאלת מרווחים/רדיוסים אחת למערכת - עד כה הטוקנים האלה היו קיימים רק
 * בתוך ThemeConfig של אפליקציית ההגדרות (itemSpacing/borderRadius), וכל
 * אפליקציה אחרת קבעה ריפוד/רדיוס ידנית וללא עקביות. הערכים כאן זהים
 * לערכי ברירת המחדל שהיו כבר ב-ThemeConfig, ולא ערכים חדשים.
 */
object FutureDimens {
    val itemSpacing: Dp = 12.dp

    // ---- סקאלת מרווחים (כפולות של 4dp) ----
    // הריפודים בקוד היו ערכים חופשיים לחלוטין (2,4,5,6,8,10,12,14,16,18,
    // 20,24,32...). הדרגות כאן הן אותה משפחה, בשמות.
    val spacingXxs: Dp = 2.dp
    val spacingXs: Dp = 4.dp
    val spacingSm: Dp = 8.dp
    val spacingMd: Dp = 12.dp
    val spacingLg: Dp = 16.dp
    val spacingXl: Dp = 24.dp
    val spacingXxl: Dp = 32.dp

    val screenPadding: Dp = spacingLg

    // ---- רדיוסים ----
    // מקור האמת הוא FutureShapes; השמות כאן נשארים כדי שקוד קיים שקורא
    // ל-FutureDimens.cardCornerRadius ימשיך לעבוד, ולא יתקיימו שתי
    // סקאלות מתחרות.
    /** שורת רשימה - --fos-radius-row. */
    val itemCornerRadius: Dp get() = FutureShapes.radiusRow
    val cardCornerRadius: Dp get() = FutureShapes.radiusLg
    val borderRadius: Dp get() = FutureShapes.radiusXl
    val glassColorRadius: Dp get() = FutureShapes.radiusXl

    // ---- עובי מסגרת הפוקוס ----
    // שני העוביים אינם תאונה: guidelines/focus-spec.html מפריד ביניהם
    // במפורש - שורת רשימה מקבלת 1.5dp, ופקד (דיאלוג, שדה, צ'יפ, שורת
    // הגדרה) מקבל 2dp. שורת תפריט היא החריג היחיד בלי מסגרת כלל.

    /** שורות רשימה. */
    val focusBorderItem: Dp = 1.5.dp

    /** פקדים - דיאלוג, שדה קלט, צ'יפ, שורת הגדרה. */
    val focusBorderControl: Dp = 2.dp

    /** ברירת המחדל ההיסטורית; מצביעה על עובי הפקד. */
    val focusBorderWidth: Dp get() = focusBorderControl

    /** הגדלת פריט ממוקד - שורות רשימה ודיאלוגים בלבד. */
    const val focusScale: Float = 1.02f

    // ---- גבהי שורה ממוקדת ----
    // אין מגע, ולכן כלל 48dp של Material לא חל. מה שכן חל: השורה
    // הממוקדת חייבת להיות חד-משמעית במבט אחד, ומכאן הגבהים הגדולים
    // (tokens/spacing.css).

    /** FocusableItem - שורת רשימה (130px ב---fos-row-list). */
    val rowHeightList: Dp = 65.dp

    /** SettingItem - שורת הגדרה. */
    val rowHeightSetting: Dp = 54.dp

    /** MenuRow - שורה בתפריט האפשרויות. */
    val rowHeightMenu: Dp = 50.dp

    /** ConfirmDialog - כפתור בדיאלוג. */
    val rowHeightDialogButton: Dp = 44.dp

    /** TopBarIconButton - כפתור אייקון בשורה העליונה. */
    val rowHeightTopBarButton: Dp = 36.dp

    // ---- גדלי אייקונים ----
    // האייקונים במערכת קטנים וקבועים, ולכל תפקיד גודל אחד (README של
    // הדיזיין סיסטם, פרק Iconography). אייקון לא נושא צבע מותג משלו:
    // בשורת הגדרה הוא בצבע ההדגשה, בשורה העליונה ובתפריט בצבע הטקסט,
    // ובשורת תפריט הרסנית בצבע הסכנה.

    /** שורה עליונה, וחץ הכניסה בסוף שורה. */
    val iconTopBar: Dp = 18.dp

    /** שורה בתפריט האפשרויות. */
    val iconMenuRow: Dp = 20.dp

    /** שורת הגדרה. */
    val iconSettingRow: Dp = 22.dp

    /** אייקון האפליקציה בתוך התראה צפה. */
    val iconNotificationApp: Dp = 34.dp

    /** האייקון הגדול של מצב ריק. */
    val iconEmptyState: Dp = 56.dp

    /**
     * האייקון בתא של ActionGrid (פקדי שיחה, פעולות איש קשר, שורת פעולות
     * מתחת לתרגום): 44% מגובה התא, לא פחות מ-20dp ולא יותר מ-40dp -
     * Math.max(40, Math.min(80, height * 0.44)) בפיקסלים ב-ActionGrid.jsx.
     */
    fun iconActionCell(cellHeight: Dp): Dp = (cellHeight * 0.44f).coerceIn(20.dp, 40.dp)

    // ---- מידות המסך ----
    // מסך אחד, לנצח. אין breakpoints ואין כללים רספונסיביים.

    /** רוחב המסך ב-dp (640 פיקסלים בצפיפות 2.0). */
    val screenWidth: Dp = 320.dp

    /** גובה המסך ב-dp (960 פיקסלים בצפיפות 2.0). */
    val screenHeight: Dp = 480.dp

    /** קו מפריד בין שורות בכרטיס. */
    val dividerThickness: Dp = 0.8.dp
}
