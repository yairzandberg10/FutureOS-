# אמינות והתאוששות (סעיפים 933, 938, 944, 947, 928, 946, 921)

## מצב בטוח (933)
AOSP נכנס למצב בטוח כשמחזיקים מקש בזמן האתחול (`WindowManagerService.detectSafeMode`): MENU, S, DPAD_CENTER או עוצמה למטה. בטלפון מקשים: **להחזיק DPAD_CENTER (אמצע ה-D-pad) או מקש התפריט מרגע שאנימציית האתחול מתחילה.**
- ה-overlay מפעיל זאת במפורש (`config_safeModeDisabled=false`).
- במצב בטוח אפליקציות צד שלישי מושבתות וה-ROM עצמו ממשיך (אפליקציות `com.future.*` הן אפליקציות מערכת ולכן ממשיכות לרוץ).
- **לבדוק:** שהמקש הפיזי שמסומן כ"תפריט" ממופה ל-`KEYCODE_MENU` ב-`.kl` של `mtk-kpd` (אחרת נשאר DPAD_CENTER). ושה-FutureUI מציג חיווי "מצב בטוח" (`sys.safemode=1`), כי SystemUI האמיתי הוא שמציג אותו ב-AOSP.
- `GlobalActions` (כיבוי) של AOSP מציע "אתחל למצב בטוח" בלחיצה ארוכה על "כבה". FutureUI מחליף אותו ולכן זה פיצ'ר שצריך לממש בו (`PowerManager.reboot("safemode")` / `SafeMode` דרך `RECOVERY`). לא ב-overlay.

## שירותים שמתקנים את עצמם (938)
- init מפעיל מחדש שירותי native שקרסו (ברירת מחדל, חוץ מ-`oneshot`). `system_server` שקורס גורם להפעלה מחדש של ה-zygote והממשק, בלי אתחול מלא.
- אפליקציות מערכת קריטיות (FutureUI, FutureLauncher) מסומנות `android:persistent="true"` ב-manifest, כך שהמערכת מפעילה אותן מחדש אם הן קרסו. **שימו לב:** persistent דורש `privileged` ופועל רק לאפליקציה שמותקנת ב-/system.
- אין שירות native משלנו, ולכן אין rc ב-overlay.

## Journaling ומערכת קבצים (944)
userdata הוא ext4 (עם journal) או f2fs (עם checkpoint), לפי ה-fstab של היצרן. אין שינוי. Virtual A/B דורש checkpoint ב-fstab. אימות: `mount | grep /data` (נכלל ב-`verify_updates_dev.sh`).

## התאוששות מקריסת GPU (947)
HWUI ו-SurfaceFlinger מתאוששים מאובדן הקשר GPU. אם SurfaceFlinger קורס, init מפעיל אותו מחדש והאפליקציות מציירות מחדש. בדיקה ב-userdebug בלבד: `adb shell stop surfaceflinger; start surfaceflinger`. אם הממשק לא חוזר, זו בעיה של ה-vendor HAL (blobs של MediaTek) ולא של ה-overlay.

## התאוששות מערכת מלאה (928)
סדר פעולות, מהקל לקשה:
1. אתחול למצב בטוח (למעלה) ואז הסרת האפליקציה שגרמה לבעיה.
2. **recovery > wipe data:** מוחק נתוני משתמש, שומר את המערכת. (בלי גיבוי, הנתונים אובדים.)
3. **התקנת OTA מלא מחדש** (sideload של `ota-full.zip` מ-recovery) כשהמערכת עצמה פגומה. לא מוחק נתונים.
4. **צריבה מחדש של כל המחיצות** מ-dump M0: רק כשהכול נכשל. **לפני כל צריבה:** גיבוי `nvram`, `nvdata`, `nvcfg`, `persist`, `proinfo`, `md1img` (שם ה-IMEI והכיול). בלעדיהם אין רשת. ר' `aosp/tools/README.md`.
סטטוס: הנהלים האלה לא נבדקו על המכשיר.

## שחזור אפליקציות וחלונות אחרי אתחול (946)
ר' `sdk-guide.md` סעיף 3: האחריות על האפליקציות. אין שינוי ב-overlay.

## Soak (921)
ר' `logging-diagnostics.md` סעיף 6.
