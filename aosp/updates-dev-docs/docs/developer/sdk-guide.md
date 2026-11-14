# מדריך למפתח: לבנות אפליקציה ל-FutureOS (סעיפים 841, 945, 946, 952, 953, 954, 998)

סטטוס: טיוטה. מבוסס על `CLAUDE.md`, `apps/SharedKeypadNav/README.md` ו-`docs/aosp-port-plan.md`. אין "SDK" נפרד: FutureOS הוא Android 12 (SDK 31), ו"ה-SDK" הוא Android SDK רגיל + ספריית `SharedKeypadNav`.

## 1. מה צריך
- Android SDK 31 (compile/target), JDK 17. אפליקציות Fitness, Music, Navigation, notes ו-Tasks דורשות JDK 21 (ר' `docs/DEVICE_SETUP.md`).
- הספרייה `SharedKeypadNav` (`com.future.sharednav`): טוקנים, רכיבים וניווט במקשים. כל האפליקציות תלויות בה, ואסור להמציא צבע, רדיוס, גודל גופן או התנהגות פוקוס שכבר קיימים בה.
- מסך 640x960 בלבד, מקשים בלבד (`KeyEvent.KEYCODE_DPAD_*`, `KEYCODE_0..9`, `*`, `#`). בלי מגע.
- אייקוני אפליקציות לא משתנים.

## 2. מבנה חבילה
- שם חבילה `com.future.<name>`. אפליקציות מערכת: `privileged` ב-`/system_ext/priv-app` או `/product/priv-app`.
- כל הרשאת signature|privileged חייבת להיות רשומה ב-`privapp-permissions-futureos-*.xml` (דוגמה: `overlay/vendor/futureos/permissions/`), אחרת האפליקציה לא תעלה (`ro.control_privapp_permissions=enforce`).
- אפליקציה שלא צריכה הרשאות signature נחתמת במפתח `futureos_apps` ולא ב-`platform` (aosp-port-plan סעיף 4).
- בלי `su` ובלי `Runtime.exec` ב-user build. משתמשים ב-API של המערכת (טבלה ב-aosp-port-plan סעיף 3ד).

## 3. שמירה אוטומטית ושחזור מצב (945, 946)
המערכת לא משחזרת חלונות אחרי אתחול. כל אפליקציה אחראית על המצב שלה:
1. נתונים של המשתמש נכתבים מיד כשהם משתנים (Room, או SharedPreferences עם `apply()`), לא ב-`onDestroy`.
2. טפסים וטיוטות (הודעה לא שנשלחה, פתק פתוח): שמירה ב-`onPause` / `onStop` וגם בטיימר קצר (עד 5 שניות אחרי הקלדה אחרונה).
3. מצב UI זמני: `onSaveInstanceState` + `SavedStateHandle`. נדרש כי התהליך מת כשהזיכרון נגמר, לא רק באתחול.
4. אחרי אתחול: האפליקציה נפתחת על המסך האחרון שנשמר בנתוני האפליקציה (לא מ-Recents). FutureLauncher אחראי רק לפתוח את האפליקציה האחרונה, לא לשחזר חלונות (החלטה: אין שינוי ב-overlay).
5. פעולות ארוכות (העתקה, הורדה) חייבות להיות ניתנות לחידוש.

## 4. רשת, מצב לא מקוון וסנכרון (952, 953, 954)
- כל פעולת רשת נכשלת בשקט וחוזרת מאוחר יותר: `WorkManager` עם `Constraints(NetworkType.CONNECTED)` ו-backoff מעריכי. לא `Thread.sleep` ולא לולאת retry.
- בדיקת חיבור לפי `ConnectivityManager.NetworkCallback` (לא `NetworkInfo` שהוצא משימוש).
- כל מסך חייב להיפתח ללא רשת עם הנתונים האחרונים שנשמרו, ולהציג שורה "לא מקוון" (טוקן `warningColor`), לא שגיאה.
- סנכרון אחרי חזרת חיבור: `WorkManager` עם אילוץ רשת מבצע זאת בלי קוד נוסף. אין Firebase push במערכת (אין GMS): ערוץ ה-push הוא שרת העדכונים והמשוב (aosp-port-plan סעיף 3ו).
- טבלת "מה עובד בלי רשת" לכל אפליקציה: ר' `docs/public/KNOWN_ISSUES.md` (נבדקת ידנית ב-M6).

## 5. גיבוי (923, 955)
- `android:allowBackup="true"` + `android:dataExtractionRules` + `android:fullBackupContent` (תבניות ב-`overlay/vendor/futureos/apps-template/res/xml/`).
- לא מגבים: מטמון, לוגים, ערכי Keystore-bound (PIN של FutureUI, תבניות פנים).

## 6. רמזים והסברים בממשק (998)
- כל פעולה שאינה מובנת מאליה מקבלת שורת רמז בתחתית המסך (מוצגת כשהפוקוס עליה), בטקסט עד 40 תווים. לא Toast, כי אין מגע.
- פעולות הרסניות: דיאלוג אישור עם הסבר מה יקרה ("הפתק יימחק לצמיתות"), כפתור ברירת מחדל = ביטול.
- טקסט עברי תמיד דרך `strings.xml` (RTL), לא מחרוזות בקוד.

## 7. מצב מפתחים (865)
- ב-user build אפשרויות המפתח מוסתרות ומופעלות רק בהקשה על מספר build. `ro.debuggable=0`, `ro.adb.secure=1`.
- אין `su` ולכן Terminal רץ כמשתמש האפליקציה. מצב root של Terminal קיים רק ב-userdebug (aosp-port-plan סעיף 3ד).
