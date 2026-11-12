# יומנים, דוחות קריסה, אבחון ומדדים (סעיפים 939-941, 961, 963-966, 970-974, 921, 922, 959)

## 1. יומן מערכת מרכזי (963-966)
- **logd** (`logcat`) הוא היומן המרכזי. הוא ring buffer: כשהמאגר מלא הרשומות הישנות נדרסות, וזה סבב היומנים (964). הגודל: `ro.logd.size=1M` (`futureos_logging.mk`, ברירת מחדל 256K).
- **חיפוש וסינון (965):** `logcat -b all -s TAG:W`, `logcat --pid=<pid>`, `logcat -e <regex>`, `logcat -v threadtime`. אין צורך בכלי נוסף, ו-Terminal כבר מריץ אותם.
- **רמות חומרה (966):** V/D/I/W/E/F, מובנה ב-`Log`. כלל לאפליקציות: אין `Log.d` שמדפיס מידע אישי (השרת מסנן, אבל לא מסתמכים על זה).
- **ב-user build אין שמירה לדיסק:** `logpersistd` עובד רק ב-eng/userdebug. לכן עקבות אחרי קריסה נשמרות ב-DropBox, ב-`/data/tombstones`, ב-ANR traces וב-`bugreportz`. אם הקרנל כולל ramoops/pstore, `last_kmsg` שורד אתחול (לבדוק ב-M1: `ls /sys/fs/pstore`).

## 2. דוחות קריסה (939)

| מקור | מיקום | מה בפנים |
|---|---|---|
| קריסת Java | DropBox: `data_app_crash`, `system_server_crash` | stack trace |
| קריסה native | `/data/tombstones` | tombstone מלא, מוגבל ל-20 (`tombstoned.max_tombstone_count`) |
| ANR | DropBox: `data_app_anr` | traces |
| הכול יחד | `adb bugreport` או "שלח משוב" | zip |

DropBox: ההגדרות הן `Settings.Global` שנקבעות בהפעלה הראשונה על ידי FutureUI (`WRITE_SECURE_SETTINGS`): `dropbox_max_files=500`, `dropbox_age_seconds=1209600` (14 יום), `dropbox_quota_kb=10240`. הן לא ב-overlay כי אין להן ברירת מחדל ב-SettingsProvider. לאמת את שמות המפתחות מול `Settings.Global` בעץ.

שליחה לשרת: רק אחרי אישור המשתמש ועם סינון (מספרי טלפון, מייל, IMEI, קואורדינטות) לפי `update-feedback-design.md` סעיף 3.

## 3. ניתוח Crash Dumps (940)
- חובה לשמור `symbols.zip` לכל גרסה שיצאה (`make_release.sh` עושה זאת). בלי זה אי אפשר לפענח.
- native: `ndk-stack -sym <symbols>/system/lib64 -dump tombstone_00`.
- Java: ה-stack קריא. עם R8 (אפליקציות release) צריך את `mapping.txt` של אותה גרסה: `retrace mapping.txt trace.txt`. לשמור `mapping.txt` יחד עם ה-APK.

## 4. אבחון חומרה (941)
אין אפליקציית אבחון. בדיקות ידניות (מקשים, תאורה, מצלמה, חיישנים) נעשות לפי `device-qa`. פקודות מהירות:
- מקשים: `adb shell getevent -l` (מקלדת `mtk-kpd`).
- סוללה: `dumpsys battery`. חיישנים: `dumpsys sensorservice`. מצלמה: `dumpsys media.camera`.
- מצב הנדסי של MediaTek (קודי `*#*#...#*#*`) לא נבדק ולא מובטח.

## 5. מדדים ומוניטור ביצועים (961, 970-974)
`overlay/vendor/futureos/tools/perf_snapshot.sh` אוסף: CPU (`top`, `dumpsys cpuinfo`), זיכרון (`dumpsys meminfo`, `/proc/meminfo`), דיסק (`df`, `dumpsys diskstats`), רשת (`dumpsys netstats`, `connectivity`), GPU (`dumpsys SurfaceFlinger`, `gfxinfo` לכל אפליקציה). לצפייה מתמשכת: `perfetto` (פעיל כברירת מחדל). אין מסך מוניטור במכשיר; אם רוצים, זה פיצ'ר באפליקציית Tools ולא ב-overlay.

**GPU (974):** ל-Mali של MT6768 אין API אחיד בלי root. מדווחים FPS ו-jank לכל אפליקציה (`gfxinfo`) ולא עומס GPU כללי.

## 6. יציבות לטווח ארוך (921, 922)
- בדיקת soak: להריץ `perf_snapshot.sh` בתחילה, אחרי 24 שעות ואחרי 7 ימים (תנאי M6: 7 ימי שימוש בלי קריסת רדיו או SystemUI). בודקים שה-PSS של `system_server`, FutureUI ו-FutureLauncher לא גדל בעקביות. גידול של יותר מ-20% בלי עלייה בעומס הוא חשד לדליפה.
- דליפה בתהליך native: `dumpsys meminfo --unreachable <pid>`.
- אין אתחול לילי מתוכנן. אם ה-soak יראה דליפה שאי אפשר לתקן, מחליטים אז.

## 7. התראה מוקדמת (959)
**סיכון חשוב:** SystemUI של AOSP הוא שמציג אזהרות "אחסון מלא", "סוללה חלשה" ו"התחממות". FutureUI מחליף אותו, ולכן אם הוא לא מאזין ל-`ACTION_DEVICE_STORAGE_LOW`, ל-`ACTION_BATTERY_LOW` ול-ThermalService (`IThermalEventListener`), האזהרות ייעלמו בשקט. לאמת במכשיר ב-M3 שכל שלוש מופיעות. סף האחסון נקבע ב-`Settings.Global.sys_storage_threshold_percentage` ו-`sys_storage_threshold_max_bytes`.
