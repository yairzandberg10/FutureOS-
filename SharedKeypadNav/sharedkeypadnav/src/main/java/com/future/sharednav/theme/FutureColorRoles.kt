package com.future.sharednav.theme

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * תפקידי הצבע שחסרו ב-[FutureTheme]. עד עכשיו כל מסך גזר אותם בעצמו
 * בשורת קוד חופשית: "טקסט משני" נכתב כ-copy(alpha=0.5) בקובץ אחד,
 * 0.55 בשני, 0.6 בשלישי ו-0.7 ברביעי; קו מפריד נכתב פעם
 * Color.LightGray.copy(0.4f) ופעם textColor.copy(0.12f); ו"משטח מוגבה"
 * הופיע כ-hex ידני (0xFF2C2C2E) ב-11 מקומות.
 *
 * מאז הדיזיין סיסטם (design/futureos-ds) סולם השקיפויות הוא טבלה סגורה
 * ולא בחירה לכל מסך - tokens/colors.css מגדיר בדיוק שלוש-עשרה דרגות,
 * ו-guidelines/colors-alpha-ladder.html קובע לכל דרגה את התפקיד שלה.
 * הדרגות מופיעות כאן כ-[textAlpha], והתפקידים כ-properties בשם.
 *
 * הם extension properties ולא שדות ב-FutureTheme כדי לא לשבור את
 * ה-constructor שכל 28 האפליקציות קוראות לו.
 */

/**
 * דרגה בסולם השקיפויות של צבע הטקסט. זה מקור האמת לעומק ולהפרדה
 * במערכת - אין גוונים חדשים, רק הטקסט בשקיפות. הדרגות הקיימות:
 * 70, 60, 55, 50, 40, 30, 20, 18, 15, 12, 10, 8, 6.
 */
fun FutureTheme.textAlpha(percent: Int): Color = textColor.copy(alpha = percent / 100f)

/** טקסט משני - שורת ההסבר מתחת לכותרת, תווית של מחוון. */
val FutureTheme.mutedTextColor: Color
    get() = textAlpha(60)

/** כותרת של מצב ריק, כפתור משני בדיאלוג. */
val FutureTheme.secondaryTextColor: Color
    get() = textAlpha(70)

/** כותרת קטע ("תצוגה", "צליל") - יחד עם ריווח אותיות של 1sp. */
val FutureTheme.sectionHeaderColor: Color
    get() = textAlpha(55)

/** טקסט/אייקון רמז - שורה שנייה של מצב ריק, האייקון של מצב ריק. */
val FutureTheme.subtleTextColor: Color
    get() = textAlpha(40)

/** חץ הכניסה בסוף שורה. */
val FutureTheme.chevronColor: Color
    get() = textAlpha(30)

/**
 * קו מפריד בין שורות - 12% במצב כהה, 10% במצב בהיר.
 * (היה הפוך: 10% בכהה ו-12% בבהיר.)
 */
val FutureTheme.dividerColor: Color
    get() = textAlpha(if (isDarkMode) 12 else 10)

/** משטח מוגבה מעל הכרטיס - צ'יפ, מקש, שדה קלט ("זכוכית"). */
val FutureTheme.elevatedSurfaceColor: Color
    get() = if (isDarkMode) Color(0xFF2C2C2E) else Color(0xFFEDEDF2)

/** דרגה נוספת מעל [elevatedSurfaceColor] - כפתור מודגש בתוך צ'יפ. */
val FutureTheme.raisedSurfaceColor: Color
    get() = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFD1D1D6)

/**
 * מילוי העיגול של אווטאר (--fos-avatar-fill). צבע משלו ולא "זכוכית": אווטאר
 * יושב גם על רקע המסך וגם על משטח/כרטיס, ו[elevatedSurfaceColor] כמעט
 * נבלע במשטח במצב בהיר.
 */
val FutureTheme.avatarFillColor: Color
    get() = if (isDarkMode) Color(0xFF3A3A3C) else Color(0xFFD3D3DC)

// ---- רקעים של פוקוס ומנוחה (tokens/focus.css) ----
// לכל רכיב יש בדיוק רקע אחד, ולא "תבנית פוקוס" כללית: שורת רשימה
// נצבעת בהדגשה, שורת הגדרה בטקסט, ושורת תפריט בדרגה אחרת לגמרי.

/** מילוי הרקע של שורת רשימה ממוקדת - 14% מצבע ההדגשה. */
val FutureTheme.focusFillColor: Color
    get() = accentColor.copy(alpha = 0.14f)

/** רקע של כפתור אייקון ממוקד בשורה העליונה - 30% מצבע ההדגשה. */
val FutureTheme.focusFillIconColor: Color
    get() = accentColor.copy(alpha = 0.30f)

/** רקע של שורת הגדרה ממוקדת. */
val FutureTheme.focusFillSettingColor: Color
    get() = textAlpha(6)

/** רקע של שורת תפריט ממוקדת - הרכיב היחיד בלי מסגרת פוקוס. */
val FutureTheme.focusFillMenuColor: Color
    get() = textAlpha(12)

/** רקע של צ'יפ או שורת מחוון ממוקדים. */
val FutureTheme.focusFillChipColor: Color
    get() = textAlpha(18)

/** רקע של צ'יפ במנוחה. */
val FutureTheme.idleChipColor: Color
    get() = textAlpha(6)

/** רקע של שדה קלט או כפתור אייקון במנוחה. */
val FutureTheme.idleFieldColor: Color
    get() = textAlpha(8)

/**
 * צבע ההדגשה כפי שמותר לצייר אותו על הרקע/המשטח של המסך. ברירת המחדל של
 * צבע ההדגשה היא לבן, ובמצב בהיר המשטחים בהירים בעצמם - בלי הבדיקה הזו
 * פריט מודגש נעלם לגמרי.
 */
val FutureTheme.readableAccentColor: Color
    get() = FutureContrast.readableAccent(accentColor, surfaceColor, textColor)

/** צבע הטקסט/האייקון שמונח על גבי צבע ההדגשה עצמו. */
val FutureTheme.onAccentColor: Color
    get() = FutureContrast.onColor(accentColor)

/**
 * צבע הטקסט/האייקון שמונח על גבי [readableAccentColor]. שני הזוגות לא
 * ניתנים להחלפה: במצב בהיר עם הדגשה לבנה, readableAccentColor הוא שחור -
 * ו-[onAccentColor] (שנגזר מהלבן) היה נותן דיו כהה על שחור.
 */
val FutureTheme.onReadableAccentColor: Color
    get() = FutureContrast.onColor(readableAccentColor)

/** צבע טקסט על גבי אחד מצבעי הסטטוס (הצלחה/סכנה/אזהרה). */
fun FutureTheme.onStatusColor(status: Color): Color = FutureContrast.onColor(status)

/**
 * הכהיית הרקע מאחורי דיאלוג/תפריט. 60% שחור בשני המצבים - הדיזיין סיסטם
 * מגדיר את זה כערך קבוע שאינו תלוי במצב כהה/בהיר (--fos-scrim).
 */
val FutureTheme.scrimColor: Color
    get() = Color.Black.copy(alpha = 0.60f)

/**
 * רקע ההתראה הצפה. תמיד כהה, גם במצב בהיר - ההתראה מרחפת מעל כל מסך
 * ולכן היא לא יורשת את המשטח שמתחתיה.
 */
val FutureTheme.headsUpSurfaceColor: Color
    get() = Color(0xFF1C1C1E).copy(alpha = 0.90f)

/** קו השיער סביב ההתראה הצפה - 15% לבן, גם הוא קבוע. */
val FutureTheme.headsUpBorderColor: Color
    get() = Color.White.copy(alpha = 0.15f)

/** מסילת מתג כבוי - קבועה, לא נגזרת מהטקסט. */
val FutureTheme.switchTrackOffColor: Color
    get() = Color(0xFFC8C8CC)

/**
 * צבע ההדגשה שכבר עבר תיקון ניגודיות למסך הנוכחי, כפי ש-[ScreenScaffold]
 * מספק אותו. null פירושו "אף אחד לא סיפק" - ואז הרכיב משתמש בצבע שקיבל
 * כפרמטר, בדיוק כמו קודם.
 *
 * קיים כי ברירת המחדל של צבע ההדגשה היא לבן: רכיב שמצייר את ההדגשה הגולמית
 * על משטח בהיר מצייר לבן על לבן, וסימון הפוקוס - המשוב היחיד שיש במכשיר
 * בלי מגע - נעלם. אותו תיקון נעשה כבר ב-TopBarIconButton דרך
 * [FutureContrast.accentForText]; כאן הוא מחושב פעם אחת למסך במקום בכל
 * שורה, כדי שרשימה ארוכה לא תחשב אותו מחדש לכל פריט.
 */
val LocalFutureAccent = compositionLocalOf<Color?> { null }
