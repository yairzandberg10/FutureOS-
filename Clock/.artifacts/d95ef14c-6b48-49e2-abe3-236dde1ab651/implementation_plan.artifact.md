# הפיכת האפליקציה לאפליקציית שעון מלאה

האפליקציה כרגע היא רשימת כלים פשוטה ("שעון עצר" ו"טיימר"). המטרה היא להפוך אותה לאפליקציית שעון מודרנית ומלאה הכוללת תצוגת זמן מרכזית, שעונים מעוררים, שעון עולמי, וניווט נוח בין כל התכונות.

## User Review Required

> [!IMPORTANT]
> האפליקציה מיועדת למכשירי D-pad/מקלדת ללא מסך מגע. הניווט יותאם למקשי חיצים ואישור (Center).

> [!NOTE]
> כל הממשק יישאר בעברית כפי שהתחיל, וישמור על שפת העיצוב של FutureOS.

## Proposed Changes

### [UI & Navigation]

נעבור ממבנה של רשימת כלים למבנה של "טאבים" (Tabs) או תפריט ניווט תחתון/עליון המאפשר מעבר מהיר בין:
1. **שעון**: תצוגת זמן גדולה ומעוצבת.
2. **מעורר**: ניהול שעונים מעוררים.
3. **שעון עולמי**: זמן בערים שונות.
4. **עצר וטיימר**: הכלים הקיימים.

#### [MODIFY] [ClockRoute.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/ui/ClockRoute.kt)
הוספת נתיבים חדשים: `Alarm`, `WorldClock`.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/MainActivity.kt)
עדכון הלוגיקה של החלפת המסכים והוספת רכיב ניווט מרכזי.

#### [MODIFY] [ClockHomeScreen.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/ui/ClockHomeScreen.kt)
עיצוב מחדש של מסך הבית להצגת שעון דיגיטלי גדול ומרשים.

### [Features]

#### [NEW] [AlarmScreen.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/ui/AlarmScreen.kt)
מסך להצגת רשימת שעונים מעוררים, הוספה, עריכה ומחיקה.

#### [NEW] [AlarmLogic.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/logic/AlarmLogic.kt)
לוגיקה לניהול ה-AlarmManager, שמירת נתונים ב-SharedPreferences/Room, וטיפול ב-BroadcastReceiver כשהשעון מצלצל.

#### [NEW] [WorldClockScreen.kt](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/java/com/future/clock/ui/WorldClockScreen.kt)
מסך להצגת זמנים בערים נבחרות בעולם.

### [Manifest & Resources]

#### [MODIFY] [AndroidManifest.xml](file:///C:/Users/yairz/Downloads/FutureOS-master/FutureOS-master/Clock/app/src/main/AndroidManifest.xml)
הוספת הרשאות `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` והצהרה על ה-Receiver של השעון המעורר.

## Verification Plan

### Automated Tests
- בדיקת לוגיקת חישוב הזמן לשעון המעורר.
- בדיקת פרסוּם (persistence) של שעונים מעוררים.

### Manual Verification
- פריסה למכשיר ובדיקה שהשעון המעורר מצלצל בזמן (גם כשהאפליקציה סגורה).
- בדיקת נוחות הניווט באמצעות ה-D-pad בין הטאבים השונים.
- אימות שהשעון הבינלאומי מציג הפרשי שעות נכונים.
