# שרת המשוב של FutureOS

Cloudflare Worker אחד שמקבל משוב מהטלפונים, ממיין אותו עם Gemini, פותח issue ב-GitHub למה שנבחר לטיפול, ומחזיר לטלפון הודעות "בטיפול" ו"תוקן בגרסה X". הוא גם מחזיק את מפתחות HERE ו-SIRI של הניווט, כך שהם לא יושבים בתוך ה-APK.

מסמך העיצוב המלא: [`docs/update-feedback-design.md`](../../docs/update-feedback-design.md).

**מצב נוכחי:** הקוד רץ מקומית ונבדק. שום דבר לא פרוס, לא נפתח שום חשבון ולא הוצא כסף.

## מה יש כאן

| קובץ | מה הוא עושה |
|-|-|
| `src/index.js` | ניתוב הבקשות, ו-Cron שמריץ את המיון פעם בשעה |
| `src/devices.js` | רישום מכשיר, אימות טוקן, קבלת משוב, תיבת הודעות, "מחק את המידע שלי" |
| `src/nav.js` | פרוקסי ניווט: `POST /v1/nav/route` (HERE) ו-`POST /v1/nav/siri/stop` (SIRI) |
| `src/triage.js` | מיון: סיווג, קיבוץ כפילויות, בחירה לטיפול, issue ב-GitHub, הודעות לטלפון |
| `src/gemini.js` | הלקוח של Gemini, ו-mock כשאין מפתח |
| `src/admin.js` | API לדשבורד, מוגן ב-Cloudflare Access (רק המייל שלך) |
| `src/redact.js` | הסרת מספרי טלפון, מיילים, IMEI, מיקום וסודות מהטקסט והלוג |
| `schema.sql` | הטבלאות ב-D1 |
| `github/claude-fix.yml` | תבנית ל-GitHub Action שמפעיל את Claude על issue שנבחר |

## הרצה מקומית (בלי חשבונות ובלי `npm install`)

צריך Node 22.5 ומעלה (`node -v`).

```bash
node --test
```

```bash
node scripts/triage-local.mjs --demo
```

```bash
node scripts/dev-server.mjs
```

שרת הפיתוח מאזין ב-`http://127.0.0.1:8787/v1`, ושומר את הנתונים בתיקייה `.local/`. בלי `.dev.vars` המיון רץ על mock והניווט מחזיר 503.

כדי לחבר אליו טלפון: `adb reverse tcp:8787 tcp:8787`, ובבניית בדיקה של Settings לשים `future.api.base=http://127.0.0.1:8787/v1` ב-`local.properties`. HTTP רגיל מותר ב-Settings רק לכתובת 127.0.0.1 (`res/xml/network_security_config.xml`). כל כתובת אחרת חייבת HTTPS.

## API

| נתיב | מי | מה |
|-|-|-|
| `POST /v1/register` | מכשיר | מחזיר `{device_id, token}`. בשרת נשמר רק hash של הטוקן |
| `POST /v1/feedback` | מכשיר | `{client_id, type, text, app_pkg, app_version?, build?, log?}`. אותו `client_id` פעמיים מחזיר את הרשומה הקיימת. עד 10 ביום למכשיר |
| `GET /v1/feedback` | מכשיר | המשובים של המכשיר והסטטוס שלהם |
| `GET /v1/inbox?ack=<id,...>` | מכשיר | הודעות שעוד לא נמסרו. `ack` מסמן הודעות כנמסרו |
| `DELETE /v1/me` | מכשיר | מוחק את המכשיר, את המשובים, את הלוגים ואת ההודעות |
| `POST /v1/nav/route` | מכשיר | JSON של HERE כמו שהוא. 503 כשאין מפתח, 429 מעל 60 בשעה |
| `POST /v1/nav/siri/stop` | מכשיר | XML של SIRI כמו שהוא. cache של 20 שניות |
| `GET /admin/clusters` | אתה | אשכולות המשוב |
| `POST /admin/clusters/<id>/decision` | אתה | `{"state":"deep"}` פותח issue ושולח "בטיפול". `{"state":"wontfix"}` סוגר |
| `POST /admin/clusters/<id>/fixed` | אתה | `{"version":"..."}` שולח "תוקן בגרסה X" לכל מי שדיווח |
| `POST /admin/triage` | אתה | מריץ סבב מיון עכשיו |

כל בקשה של מכשיר שולחת `Authorization: Bearer <token>` ו-`X-Device-Id`.

## איך המיון עובד

1. פעם בשעה, עד 20 משובים חדשים בכל סבב.
2. Gemini מסווג כל משוב: קטגוריה, חומרה 1-4, ואחת מארבע החלטות (`deep`, `needs_info`, `wontfix`, `duplicate`). כל ערך מחוץ לרשימה הזו הופך ל-`needs_info`, כדי שפלט שבור לא יפעיל את Claude.
3. embedding של כל משוב מושווה לאשכולות הפתוחים של אותה אפליקציה. משוב דומה מצטרף לאשכול קיים.
4. אשכול עובר לטיפול כש-Gemini החליט `deep`, כשהחומרה היא 1, או כש-3 מכשירים שונים דיווחו.
5. כשאשכול עובר לטיפול: נפתח issue עם ה-label `triage:accepted`, וכל מי שדיווח מקבל "בטיפול".
6. כשהתיקון יוצא, לוחצים `fixed` בדשבורד, וכל מי שדיווח מקבל "תוקן בגרסה X". בשלב 4 (OTA) זה יקרה אוטומטית אחרי שהטלפון מדווח שעלה תקין על הגרסה.

**הגנה מפני הוראות שמוסתרות בתוך משוב (prompt injection):** טקסט המשוב נשלח ל-Gemini בתוך סימון "קלט לא אמין". ל-Gemini אין גישה לכלים, והפלט מוגבל לרשימה סגורה. ב-issue הטקסט מופיע בבלוק קוד, בלי אזכורי `@` ובלי הפניות `#`. טקסט משתמש ולוגים נכנסים ל-issue רק אם `GITHUB_REPO_PRIVATE=true`.

## שער האישור: מה Claude לא יכול לעשות

- Claude פותח PR ולא ממזג. צריך branch protection על `main` עם review חובה.
- שום בנייה ושום פרסום לא קורים מהשרת. הוא רק מפיץ את מה שאתה מעלה.
- "תוקן בגרסה X" נשלח רק כשאתה לוחץ.

## מה צריך לעשות כדי לפרוס (אתה וההורה)

כל השירותים כאן חינמיים בהיקף הזה, חוץ מ-Claude ב-GitHub Actions ו-Gemini עם חיוב. **כל חשבון בתשלום, וכל הסכמה לתנאי שימוש, נעשים על ידי ההורה, על שמו ועם אמצעי התשלום שלו.**

### 1. ההורה פותח חשבונות
1. **Cloudflare** (חינמי): ב-dash.cloudflare.com. אין צורך בכרטיס אשראי בשביל Workers, D1 ו-R2 בשכבה החינמית. R2 עשוי לבקש אמצעי תשלום גם בשכבה החינמית.
2. **מפתח Gemini API**: ב-Google AI Studio. **מומלץ חשבון עם חיוב פעיל**, כי בשכבה החינמית גוגל רשאית להשתמש בתוכן שנשלח לשיפור המוצרים שלה. בהיקף של כמה מאות משובים בחודש העלות צפויה להיות פחות מדולר. כדאי להגדיר תקרת הוצאה בחשבון.
3. **מפתח Anthropic API** (ל-Claude ב-GitHub Actions): ב-console.anthropic.com, עם תקרת הוצאה חודשית.

### 2. GitHub (אתה)
1. ריפו **פרטי** חדש `FutureOS-feedback`, בשביל ה-issues.
2. Fine-grained PAT לשרת, עם הרשאת Issues: Read and write על הריפו הזה בלבד.
3. Fine-grained PAT ל-Action (`CODE_REPO_TOKEN`), עם Contents ו-Pull requests: write על `FutureOS-` בלבד.
4. ב-`FutureOS-`: Settings ← Branches ← כלל ל-`main` עם "Require a pull request" ו-"Require approvals". זה שער האישור.
5. להעתיק את `github/claude-fix.yml` ל-`.github/workflows/` בריפו `FutureOS-feedback`, ולהוסיף secrets `ANTHROPIC_API_KEY` ו-`CODE_REPO_TOKEN`. לפני ההפעלה כדאי לבדוק מול התיעוד של `anthropics/claude-code-action` שהפרמטרים לא השתנו.

### 3. פריסה (אתה, אחרי שהחשבונות קיימים)
```bash
npx wrangler login
```
```bash
npx wrangler d1 create futureos
```
להעתיק את ה-`database_id` שמודפס ל-`wrangler.toml`.
```bash
npx wrangler d1 execute futureos --remote --file=schema.sql
```
```bash
npx wrangler r2 bucket create futureos-feedback
```
סודות, כל אחד בפקודה נפרדת. הערך מוקלד כשמתבקש, לא בשורת הפקודה:
```bash
npx wrangler secret put GEMINI_API_KEY
```
```bash
npx wrangler secret put GITHUB_TOKEN
```
```bash
npx wrangler secret put HERE_API_KEY
```
```bash
npx wrangler secret put SIRI_API_KEY
```
```bash
npx wrangler secret put SIRI_BASE_URL
```
ב-`wrangler.toml`: `GITHUB_REPO`, `GITHUB_REPO_PRIVATE = "true"`, `ADMIN_EMAIL`.
```bash
npx wrangler deploy
```

### 4. דשבורד מוגן
ב-Cloudflare Zero Trust ← Access ← Applications: אפליקציה על הנתיב `/admin/*` של ה-Worker, עם מדיניות שמתירה רק את המייל שלך. להעתיק את ה-AUD ואת דומיין הצוות ל-`ACCESS_AUD` ול-`ACCESS_TEAM_DOMAIN` ב-`wrangler.toml`, ולפרוס שוב. בלי שלושת הערכים האלה `/admin` מחזיר 503.

### 5. חיבור הטלפון
בבנייה של Settings ו-Navigation: `future.api.base=https://futureos-feedback.<subdomain>.workers.dev/v1` ב-`local.properties`. ב-AOSP זה יגיע מ-`ro.futureos.api_base`. מאותו רגע הרישום, המשוב והניווט עובדים בלי שינוי קוד.

### 6. לפני שמכשירים של אחרים שולחים משוב
מדיניות פרטיות בעברית שההורה מאשר: מה נשלח, כמה זמן נשמר (לוגים 90 יום, טקסט שנה), ואיך מוחקים. ראו סעיף 8 במסמך העיצוב.

## עלות משוערת

עד 500 מכשירים: Cloudflare בערך $0, ו-Gemini פחות מ-$1 לחודש. Claude עולה בערך $0.5-3 לכל issue שהוא מתקן, וזו העלות היחידה שגדלה עם הכמות. אלה הערכות, וכדאי לאמת בדפי התמחור לפני הפתיחה.
