# תהליך release: חתימה, OTA, דלתא, rollback (סעיפים 810, 815, 816, 817, 820, 825, 935, 940)

מבוסס על `docs/aosp-port-plan.md` סעיף 4 ו-`docs/update-feedback-design.md` סעיף 2. אין כאן בנייה, רק הכללים והפקודות.

## 1. חתימה (810)
- מפתחות: platform, shared, media, networkstack, releasekey, bluetooth (דרך `development/tools/make_key`) ו-`avb.pem` (RSA 4096). נוצרים פעם אחת במחשב לא מחובר, גיבוי מוצפן בשני מקומות, לעולם לא בגיט.
- `tools/make_release.sh` מריץ `sign_target_files_apks` ואחר כך `ota_from_target_files`. החבילה מאומתת במכשיר מול `/system/etc/security/otacerts.zip`. השרת לא מחזיק מפתחות.
- ה-manifest מהשרת חתום בנפרד ב-Ed25519 (הוראה "איזו חבילה להתקין"), ואינו מחליף את חתימת ה-OTA.

## 2. אטומיות וחזרה (815, 816, 935)

| מצב | התנהגות | מי קובע |
|---|---|---|
| Virtual A/B | התקנה ל-slot השני. אם האתחול לא מסומן כתקין (`update_verifier`), חוזרים אוטומטית. יש snapshot של המחיצות | `FUTUREOS_OTA_MODE=vab` |
| A/B רגיל | כנ"ל, בלי snapshot וירטואלי | `ab` |
| non-A/B | התקנה דרך recovery, בלי חזרה אוטומטית | `none` |

- **חזרה לגרסה קודמת יזומה:** לא downgrade. בונים גרסה חדשה עם התוכן הישן (roll-forward), כי `update_engine` דוחה `ro.build.date.utc` ישן ו-AVB rollback index חוסם חזרה.
- **Rollback index:** עולה רק בגרסת תיקון אבטחה. ההחלטה: המשתמש. ברירת המחדל 0 (`BoardConfigUpdates.mk`).
- **Snapshot לפני עדכון (935):** ב-VAB הוא חלק מהמנגנון. ב-non-A/B אין. בשני המקרים FutureUpdater אמור להציע גיבוי הגדרות לפני התקנה (ר' backup-design.md).

## 3. הורדה ברקע ודלתא (817, 820)
- הורדה ברקע: FutureUpdater מזרים payload ישירות ל-`update_engine` (אין עותק מלא בדיסק). דורש את האפליקציה, שמחוץ ל-overlay.
- דלתא: `ota_from_target_files -i <previous_signed_target_files.zip>`. חובה לשמור את ה-target-files החתום של כל גרסה שיצאה. מי שדילג על גרסה מקבל חבילה מלאה.
- בדיקה לפני פרסום: להתקין incremental על מכשיר שרץ על הגרסה הקודמת, בלי מחיקת נתונים (תנאי המעבר של M5).

## 4. סדר פעולות לכל גרסה
1. `m dist` ב-user build, ואז `make_release.sh <out> <target_files.zip> <prev_signed_tf.zip>`.
2. לבדוק `SHA256SUMS`, לשמור `signed-target_files.zip` ו-`symbols.zip` (940).
3. להשלים Release Notes (תבנית `docs/public/RELEASE_NOTES_TEMPLATE.md`) ו-`CHANGELOG.md`.
4. פרסום ל-dev, אחר כך 5%, 25%, 100%. כל העלאה היא אישור של המשתמש (שער ידני).
5. כלל versionCode: ה-OTA הבא חייב להכיל versionCode גבוה או שווה לכל APK שהופץ בנפרד (update-feedback-design סעיף 2.5).
