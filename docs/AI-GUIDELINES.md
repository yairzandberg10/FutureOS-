# הנחיות לעבודת AI על FutureOS

מסמך לכל עוזר AI (Claude, Codex, Cursor, Copilot וכו') שעובד על הפרויקט. לקרוא לפני כל שינוי.
המקורות המפורטים: `CLAUDE.md`, `README.md`, `apps/SharedKeypadNav/README.md`, `.claude/coordination/FIXES-LESSONS.md`.

---

## 1. מה זה הפרויקט
- FutureOS היא מערכת מבוססת Android 12 (SDK 31, minSdk 31) לטלפון **מקשים בלבד**: Qin F22 Pro.
- מסך **640×960 פיקסלים**, צפיפות 2.0, עברית ו-RTL.
- כ-30 אפליקציות, כל אחת פרויקט Android Studio עצמאי בתיקייה משלה (`apps/Clock/`, `apps/Dialer/`, `apps/Messages/`...). הטבלה המלאה ב-`README.md`.
- `FutureUI` הוא ה-System UI (שורת מצב, מרכז בקרה, התראות) כ-`AccessibilityService` overlays. `SystemUI` הוא מימוש מקביל שלא מותקן במכשיר - לא להפעיל את שניהם יחד.
- `SharedKeypadNav` היא הספרייה המשותפת שכל האפליקציות תלויות בה: ניווט D-pad, מיפוי T9 וה-design system.

## 2. שפה
- כל תשובה למשתמש **בעברית**, גם כשהמקורות באנגלית.
- הודעות commit בעברית, שמסבירות מה חדש בגרסה.

## 3. חוקי עיצוב (חובה)
1. **מקשים בלבד.** אין Touch. כל פעולה נגישה ב-`KeyEvent` (DPAD, ספרות 0-9, `*`, `#`, Menu, Back). `clickable`/`pointerInput` לא יכולים להיות הדרך היחידה לפעולה.
2. **פוקוס:** לכל אלמנט אינטראקטיבי יש פוקוס, יש פוקוס התחלתי בכל מסך, אין מלכודות פוקוס, Back תמיד יוצא. להשתמש ברכיבי הניווט של SharedKeypadNav ולא לממש לבד.
3. **Design system בלבד.** צבעים, רדיוסים, גדלי פונט, ריווח ומשכי אנימציה רק מה-tokens של `SharedKeypadNav` (`FutureTheme`, `FutureShapes`, roles כמו `mutedTextColor`, `textAlpha(n)`). המקור הקנוני: `design/futureos-ds/`. לא להמציא עיצוב חדש.
4. **לא לצייר `Color.White`/`Color.Black` על צבע ה-accent** - ה-accent בחירה חופשית של המשתמש. להשתמש ב-`onAccentColor` / `readableAccentColor`.
5. **רזולוציה אחת:** 640×960. בלי breakpoints למסכים אחרים.
6. **RTL:** כיוונים לוגיים (start/end). chevron כניסה פונה שמאלה.
7. **אייקוני אפליקציות - לא נוגעים.** אין שינוי ב-`mipmap-*` / `ic_launcher*`.
8. **אנימציות בולטות** - תנועה צריכה להיראות בבירור, לא עדינה. (במחשב של המשתמש מופעל reduced-motion, אז לא לשפוט לפי מה שרואים שם.)
9. אין להציג למשתמש Google / Play / GMS / Firebase בממשק.

## 4. קוד
- לכתוב כמו הקוד שמסביב: אותו סגנון, שמות, צפיפות הערות.
- minSdk=31 ו-lint לא תופס APIs חדשים יותר (למשל `readNBytes(int)` הוא API 33 והפיל את Files). לבדוק API level לכל קריאה חדשה.
- שינוי ב-`SharedKeypadNav` משפיע על **כל** האפליקציות - לא לשנות בלי אישור המשתמש.
- לא להעביר תיקיית אפליקציה או את `SharedKeypadNav` ממקומם (ה-`settings.gradle.kts` מפנה אליהם בנתיב יחסי).
- לא להכניס לגיט: `keystore.properties`, `local.properties`, `google-services.json`, `apps/Sfarim/tools/output/sefaria.db`.
- אחרי שינוי ב-Manifest, receivers, services, root או הצפנה - להריץ `python security/security-check.py`.

## 5. בנייה והתקנה
- **לא לבנות ולא להתקין על הטלפון בלי בקשה מפורשת של המשתמש.** (גובר על ההוראה הישנה ב-`CLAUDE.md` להתקין אוטומטית.)
- כשמבקשים: release בלבד, לא debug:
  ```bash
  ./gradlew assembleRelease --no-daemon --max-workers=1
  ```
- **build אחד בכל המחשב בכל רגע.** לבדוק שאין GradleDaemon של צ'אט אחר לפני שמתחילים.
- **אסור** `gradlew --stop`, `taskkill /IM java.exe`, `pkill java` - זה הורג build של צ'אט אחר. עוצרים רק PID שהתחלת בעצמך.
- דיסק C כמעט מלא: לבדוק מקום פנוי לפני build, ולמחוק `app/build` ו-`build` אחרי כל אפליקציה.
- התקנה: `adb install -r -d <apk>`, ואחריה קומפילציית ART:
  ```bash
  adb shell cmd package compile -m speed -f <package>
  ```
- ADB לא ב-PATH: `C:/Users/yairz/AppData/Local/Android/Sdk/platform-tools/adb.exe`.

## 6. הטלפון (כמה צ'אטים במקביל)
- לפני כל פעולה בטלפון לבדוק `.claude/coordination/phone.lock`. אם קיים - לחכות. אם לא - לכתוב בו `<שם הצ'אט> <זמן> <מה עושים>` ולמחוק בסיום.
- אחרי התקנה לרשום ב-`.claude/coordination/STATUS.md` מה הותקן.
- **לא לחייג ולא לשלוח SMS אמיתיים.** בדיקות של Messages / dialer רק באישור המשתמש.
- אם יש שיחה פעילה (`mCallState≠0`) - לא לגעת בטלפון.
- קוד PIN מקליד רק המשתמש. לא להפעיל את נעילת FutureUI (כבויה בכוונה).
- `su` מ-adb מקפיץ בקשת root ב-Magisk - להימנע.
- `screen_off_timeout`: בסוף סבב להחזיר ל-2 דקות (`120000`). רק הצ'אט האחרון שמשתמש בטלפון.
- התקנת FutureUI נועלת את הטלפון - לקחת בחשבון שהמשתמש יצטרך לפתוח.

## 7. בדיקות ממשק
- **לא לבצע בדיקות UI במקום המשתמש** (אלא אם ביקש). לסיים כל שינוי ברשימה קצרה: מה לבדוק בטלפון ואילו מקשים ללחוץ. המשתמש יחזיר תוצאות.
- לפני `ship` להריץ את סוקר `keypad-reviewer` על שינויי UI.

## 8. גיט
- הריפו: `https://github.com/yairzandberg10/FutureOS-.git`.
- `git add` רק לקבצים של השינוי (לא `-A`) - יש WIP של צ'אטים אחרים בתיקיות.
- worktrees: `FutureOS-fixes` (ענף `qa-install`), `FutureOS-nav-nogms`, `FutureOS-qa-build` (FutureUI נבנה רק משם). לבדוק באיזה עותק השינוי צריך להיות.
- רשומה ב-`CHANGELOG.md` תחת `## Unreleased` לכל אפליקציה ששונתה.

## 9. החלטות שדורשות את המשתמש
- שינויים ב-`SharedKeypadNav`, dialer #8, messages #14.
- כל פעולה חיצונית: push, הודעה לאדם, פרסום.
- לעולם לא לשלוח הודעות להרב יונתן.

## 10. סיום משימה
1. מה השתנה (קבצים ואפליקציות).
2. מה לא נבדק / לא נבנה.
3. מה המשתמש צריך לבדוק בטלפון, צעד אחר צעד במקשים.
