# Tools (כלים)

`com.future.tools` — אוסף כלים למכשיר מקלדת-בלבד (ללא מסך מגע):

- **מחשבון** — חישובים והיסטוריה, `BigDecimal` למניעת שגיאות עיגול, פריסת מקלדת סטנדרטית של 5 שורות.
- **פנס** — הדלקה/כיבוי מהירים דרך Camera2 (`FlashlightController`).
- **שעון עצר** — מדידת זמן עם הקפות.
- **טיימר** — ספירה לאחור עם התראת רטט.
- **ממיר יחידות** — אורך, משקל, טמפרטורה.
- **מצפן** — כיוון לפי חיישני תאוצה ומגנטומטר (`Sensor.TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD`).
- **פלס** — איזון אופקי לפי חיישן תאוצה, עם בועה חזותית.

כל כלי הוא מסך `@Composable` נפרד תחת `ui/`, מנותב דרך `ToolRoute` (sealed class) ורשום גם ב-`TOOL_ENTRIES`
(`ToolsHomeScreen.kt`) לתצוגה ברשימה הראשית, וגם ב-`when` שב-`MainActivity.kt`. הוספת כלי חדש דורשת שלושה
שינויים: ענף חדש ב-`ToolRoute`, שורה ב-`TOOL_ENTRIES`, וענף ב-`when` של ה-Activity.

## הוספת כלי בודד למסך הבית כאפליקציה עצמאית

ליד כל כלי ברשימה הראשית יש כפתור נעץ (`PushPin`) - לחיצה עליו מפעילה/מכבה activity-alias
ייעודי לאותו כלי (`ToolShortcutCalculator`, `ToolShortcutFlashlight` וכו', מוצהרים ב-
`AndroidManifest.xml` וכבויים כברירת מחדל). ברגע שה-alias מופעל (`ToolShortcuts.setPinnedToHome`,
`data/ToolShortcuts.kt`) הוא נכנס ל-`queryIntentActivities` של המערכת ומופיע בלאנצ'ר כאפליקציה
נפרדת עם התווית שלו (למשל "פנס" בפני עצמו, לא רק בתוך "כלים") - אפשר להוסיף אותו לדף הבית
בדיוק כמו כל אפליקציה אחרת, דרך "הוספת אפליקציה" בלאנצ'ר. בפועל כל ה-alias-ים מריצים את אותה
`MainActivity`, שמזהה איזה alias שימש להפעלה לפי `intent.component` ופותחת ישר במסך המתאים; כשהיא
מופעלת ככה, מקש/כפתור החזרה סוגר את האפליקציה במקום לחזור לרשימת "כלים" - כמו כל אפליקציה עצמאית.

הוספת כלי חדש למנגנון הזה: entry ב-`ALIAS_BY_ROUTE` (`ToolShortcuts.kt`), activity-alias תואם
ב-`AndroidManifest.xml`, ומחרוזת תווית ב-`strings.xml`.

**הערה:** התיקון הנדרש כדי שזה יעבוד נכון היה גם ב-FutureLauncher עצמו - קוד ההפעלה שם השתמש
ב-`PackageManager.getLaunchIntentForPackage(packageName)`, שמחזיר "פעילות הפעלה" אחת לפי שם
חבילה בלבד ומתעלם מזהות הרכיב הספציפי שנבחר בפועל. כשלחבילה יש כמה activity-alias פתוחים
ל-LAUNCHER (בדיוק המצב כאן), זה גרם לכך שלחיצה על כל אחד מהם עלולה לפתוח רכיב שרירותי במקום
את זה שבאמת נבחר. תוקן ב-`LauncherItem.launchIntent()` (`FutureLauncher/ui/LauncherItem.kt`)
שבונה Intent לפי `ComponentName` מדויק מתוך ה-`ResolveInfo` שכבר נפתר.
