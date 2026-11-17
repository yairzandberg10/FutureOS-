# CLAUDE.md - הנחיות לפרויקט אנדרואיד (ADB)

## שפת תקשורת (Language)
- ענה לי אך ורק בשפה העברית.
- אל תשתמש בשפות אחרות בתשובות ובדיאלוג איתי.

## חוקי עיצוב ופיתוח האפליקציות (App Design Rules)
- נאמנות מלאה לדיזיין סיסטם (Design System): השתמש אך ורק ברכיבים, בצבעים ובסגנונות המוגדרים במערכת העיצוב של הפרויקט. אל תמציא עיצובים חדשים.
- אל תשנה, אל תחליף ואל תעדכן את אייקוני האפליקציות (App Icons). יש להשאירם כפי שהם.
- הכן והתאם את כל האפליקציות והמסכים לרזולוציית מסך מדויקת של 640×960 פיקסלים בלבד.
- ממשק המשתמש מבוסס על מקשים בלבד (Keypad / Buttons). השתמש ב-Key Events (כמו `KeyEvent.KEYCODE_DPAD_CENTER`, `KEYCODE_1` וכו'). אין להשתמש בנגיעה במסך (Touch).
אל תעשה בדיקות ממשק תגיד לי מה לבדוק ואני יכתוב לך את התוצאות

## בנייה ובדיקות: רק בגיט (GitHub Actions), לא במחשב
- אסור להריץ במחשב המקומי בנייה, בדיקות או כל דבר כבד: לא `./gradlew` (שום משימה), לא `npm test`, לא שרת פיתוח, לא אמולטור.
- כל הבנייה והבדיקות רצות רק ב-GitHub Actions (`.github/workflows/build.yml`), אוטומטית על כל push ל-`main`: בדיקת האבטחה, בנייה ובדיקות יחידה של כל אפליקציה, ובדיקות השרת (`server/feedback`).
- בסיום כל שינוי קוד: commit עם הסבר מה חדש, ו-push ל-`main` בריפו https://github.com/yairzandberg10/FutureOS-.git. התוצאה נבדקת ב-Actions, ואם נכשל - מתקנים ודוחפים שוב.
- התקנה בטלפון (רק כשהמשתמש מבקש): ה-workflow `release-apks.yml` בונה release לכל אפליקציה שהשתנתה, חותם במפתח של המחשב (סוד `DEBUG_KEYSTORE_BASE64`) ומעלה ל-GitHub Release בשם `apks`. מורידים את `https://github.com/yairzandberg10/FutureOS-/releases/download/apks/<App>-release.apk`, ואז `adb install -r <קובץ>` ו-`adb shell cmd package compile -m speed -f <PACKAGE_NAME>`. ההתקנה היא מעל הגרסה הקיימת, בלי למחוק נתונים.
- מותר במחשב רק פעולות קלות: קריאה ועריכה של קבצים, git, וחיפוש.

## ענף אחד בלבד: main (Git)
- בריפו יש ענף אחד בלבד: `main`. כל commit וכל push הולכים אליו (`git push origin main`).
- אסור לפתוח ענפים חדשים (feature, fix, wip, qa וכו') ואסור לדחוף ענפים אחרים ל-GitHub.
- התיקייה לעבודה היא `FutureOS--main` (שם `main` פתוח). לפני שמתחילים: `git pull --rebase origin main`, כדי לא לדרוס עבודה של צ'אט אחר.
- אם הסשן נפתח בכל זאת על ענף אחר או ב-worktree, בסוף המשימה ממזגים אותו ל-`main`, דוחפים, ומוחקים את הענף, גם מקומית וגם ב-GitHub.
- צריך לשמור משהו בצד? commit זמני על `main` שאחר כך מתקנים, לא ענף ולא stash.

## מבנה הריפו
- כל האפליקציות (וגם המודול המשותף `SharedKeypadNav`) נמצאות ב-`apps/<App>/`, למשל `apps/Settings`, `apps/Notes`, `apps/Dialer`. בונים מתוך התיקייה של האפליקציה: `cd apps/<App> && ./gradlew installRelease`.
- תיעוד ב-`docs/` (DEVICE_SETUP, AI-GUIDELINES, QA-REPORT), שיווק ב-`marketing/`, בדיקת האבטחה ב-`security/security-check.py` (מריצים מהשורש: `python security/security-check.py`).
- בשורש נשארים רק README, CLAUDE, DESIGN, PRODUCT, CHANGELOG ו-build-all.sh. לא ליצור תיקיות אפליקציה חדשות בשורש, ולא שמות בעברית בתוך הריפו (Gradle נכשל בנתיב לא-ASCII ב-Windows).
