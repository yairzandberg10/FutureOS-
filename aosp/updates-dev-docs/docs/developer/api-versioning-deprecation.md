# גרסאות API ומדיניות Deprecation (סעיף 879)

סטטוס: טיוטה למדיניות. יש לאשר לפני שמחילים.

## מה נחשב API של FutureOS
FutureOS הוא Android 12, כך ש-API של פריימוורק הוא של Android (`Build.VERSION.SDK_INT = 31`). ה-API שלנו הוא ה-surface שבין רכיבי FutureOS:

| ממשק | מי מספק | מי צורך |
|---|---|---|
| ספריית SharedKeypadNav (מחלקות, טוקנים, רכיבים) | `apps/SharedKeypadNav/` | כל האפליקציות |
| ContentProvider ו-Intents בין אפליקציות, כולל ההרשאה `com.future.futureui.permission.SYSTEM_SETTINGS` | FutureUI | Settings, Launcher |
| מאפייני `ro.futureos.*` | ה-overlay | Settings (מסך מידע), FutureUpdater |
| פורמט manifest של ה-OTA | שרת עדכונים | FutureUpdater (update-feedback-design סעיף 2) |

## כללים
1. **גרסה:** `ro.futureos.api_level` (מספר שלם, מתחיל ב-1) עולה רק בשינוי שוברים של ממשק פנימי. שינוי תואם לאחור לא מעלה אותו.
2. **Deprecation:** ממשק מסומן `@Deprecated` עם הערה "מוסר בגרסה X, השתמשו ב-Y", ונשאר לפחות שתי גרסאות minor אחרי ההכרזה. ההכרזה נכנסת ל-Release Notes.
3. **הסרה:** רק בגרסה major, ורק אחרי שאין צרכנים בעץ (חיפוש בכל האפליקציות).
4. **שינויי manifest של OTA:** שדות חדשים אופציונליים בלבד. לקוח ישן חייב להתעלם משדות לא מוכרים.
5. **גרסת פלטפורמה:** שדרוג Android (12 ל-13) הוא החלטה נפרדת עם מסמך הגירה משלו, לא שינוי רגיל.

## מה נשאר פתוח
- `ro.futureos.api_level` עדיין לא מוגדר ב-overlay (אין צרכן לפני שיש מסך מידע). להוסיף ל-`futureos_lifecycle.mk` כשמחליטים על הערך ההתחלתי.
