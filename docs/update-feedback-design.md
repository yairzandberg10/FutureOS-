# מנגנון עדכונים ומשוב - מסמך עיצוב

סטטוס: טיוטה לאישור, שום דבר לא נבנה. תאריך: 2026-10-14.
הקשר: FutureOS הופך ל-AOSP סגור על Qin F22 Pro עם הדרייברים של Qin (צ'אט ה-port מתכנן בנפרד). אין GMS במכשיר.

## 1. תמונה כללית

```
 מכשיר F22 Pro                          שרת (Cloudflare)                     GitHub / Claude
 ┌─────────────────────┐   HTTPS   ┌──────────────────────────┐        ┌────────────────────────┐
 │ FutureUpdater       │──בדיקה──▶ │ Worker: /v1/update/check │        │ ריפו פרטי feedback     │
 │ (אפליקציית מערכת)   │◀─manifest─│ D1: devices, releases,   │        │ issue + label          │
 │  UpdateEngine API   │──הורדה──▶ │     rollouts, feedback   │─issue─▶│  triage:accepted       │
 │                     │           │ R2: payload.bin / logs   │        │        │               │
 │ מסך "שלח משוב"      │──משוב───▶ │ Cron: מיון Gemini        │        │ claude-code-action     │
 │ תיבת הודעות         │◀─סטטוס────│ דשבורד (Cloudflare Access)│◀─PR───│  → PR (לא ממוזג!)      │
 └─────────────────────┘           └──────────────────────────┘        └────────────────────────┘
                                             ▲  אישור משתמש: מיזוג PR, חתימה, "פרסם ל-X%"
```

שלושה שערים שרק המשתמש פותח: **מיזוג PR**, **בנייה וחתימה של OTA** (המפתחות אצלו בלבד), **פרסום rollout** בדשבורד.

## 2. עדכוני מערכת (OTA)

### 2.1 סוג העדכון - תלוי בחלוקת המחיצות של F22 Pro
עוד לא ידוע אם ה-F22 Pro (MediaTek, Android 12 במקור) הוא A/B. צריך לבדוק, כשיש גישה לטלפון (phone.lock):
`getprop ro.build.ab_update`, `getprop ro.virtual_ab.enabled`, `ls /dev/block/by-name | grep _a`.

| מצב | שיטה | חזרה אחורה במקרה של כשל אתחול |
|-|-|-|
| A/B או Virtual A/B (מועדף) | `update_engine` ברקע, דרך `android.os.UpdateEngine` (SystemApi) | אוטומטית: אם המערכת החדשה לא מסמנת את עצמה כתקינה, ה-bootloader חוזר ל-slot הקודם |
| non-A/B | הורדה ל-`/data/ota_package` + אתחול ל-recovery | אין חזרה אוטומטית. אפשר להפוך ל-Virtual A/B בבניית ה-AOSP (דורש super partition ומקום פנוי) |

המלצה: לבקש מצ'אט ה-port לבנות Virtual A/B אם המחיצות מאפשרות. זה מה שנותן rollback אמיתי.

### 2.2 הלקוח במכשיר
- אפליקציית מערכת חדשה `com.future.updater`, privileged, חתומה במפתח הפלטפורמה. בסיס: `SystemUpdaterSample` של AOSP.
- בודקת עדכון כל 6 שעות (JobScheduler, רק ב-Wi-Fi כברירת מחדל) וגם ידנית מ"הגדרות ← עדכון מערכת".
- מוריד payload מוזרם (streaming) ישירות ל-update_engine, בלי לשמור עותק מלא בדיסק.
- אחרי אתחול מוצלח: מדווחת לשרת `boot_ok` עם הגרסה. זה הנתון שמזין את עצירת ה-rollout האוטומטית.
- ממשק במקשים בלבד, 640×960, רכיבי SharedKeypadNav.

### 2.3 אבטחה
- החבילות נוצרות ב-`ota_from_target_files` (מלאות, ואינקרמנטליות עם `-i`) וחתומות ב-`releasekey` לפי `docs/aosp-port-plan.md` סעיף 4. המכשיר מאמת מול `otacerts.zip`/מפתח ה-payload. אין פורמט עדכון משלנו. השרת רק מפיץ ולא מחזיק מפתחות.
- ה-payload חתום במפתח release שנמצא **רק אצל המשתמש** (offline). update_engine מאמת מול המפתח הציבורי שבתוך המערכת, כך ששרת פרוץ לא יכול להתקין קוד.
- ה-manifest מהשרת מכיל גם SHA-256 של ה-payload. בנוסף, ה-Worker חותם עליו ב-Ed25519 כדי שמכשיר לא יקבל הוראות מזויפות.
- הגנה מ-downgrade: update_engine דוחה בילד עם `ro.build.date.utc` ישן יותר. לכן "חזרה אחורה" יזומה = בילד חדש עם התוכן הישן (roll-forward revert).

### 2.4 פריסה הדרגתית ו-rollback
- ערוצים: `dev` (הטלפון של המשתמש), `beta`, `stable`.
- כל מכשיר מקבל `bucket = hash(device_id + release_id) % 100`. המכשיר זכאי אם `bucket < rollout_pct`.
- שלבים מומלצים: dev → 5% → 25% → 100%. כל העלאה היא לחיצה של המשתמש.
- עצירה אוטומטית: אם שיעור `boot_ok` נמוך מ-95% אחרי 24 שעות, או אם יש קפיצה בכמות המשובים על הגרסה, ה-rollout מוקפא ונשלחת התראה למשתמש.
- Rollback ברמת המכשיר: A/B אוטומטי. ברמת הצי: הקפאה + revert build.

### 2.5 עדכוני אפליקציה בלבד (אושר ע"י המשתמש 2026-10-16)
אפליקציות `com.future.*` בתוך `/system` אפשר לעדכן כ-APK (מותקן ל-`/data` מעל גרסת המערכת) כל עוד הוא חתום באותו מפתח. זה קטן ומהיר יותר ל-OTA של תיקון באפליקציה אחת. **המשתמש אישר (2026-10-16).**
כלל חובה אם מאשרים: ה-OTA הבא חייב להכיל versionCode גבוה או שווה לכל APK שיצא בנפרד. אחרת העותק ב-`/data` "מסתיר" את הגרסה שבמערכת, וחזרה אחורה דרך OTA לא תשפיע עליו. אותו manifest, אותו שער אישור, התקנה דרך `PackageInstaller` מתוך FutureUpdater.

## 3. משוב

### 3.1 במכשיר
- מסך "שלח משוב" ב-Settings, ומקיצור דרך: תפריט Options בכל אפליקציה (רכיב משותף ב-SharedKeypadNav ששולח Intent עם שם האפליקציה).
- המשתמש בוחר סוג (באג / הצעה / אחר), מקליד טקסט ב-T9 או מכתיב (ה-ASR המקומי של Assistant).
- מצורף אוטומטית: build fingerprint, גרסת FutureOS, חבילה + versionCode של האפליקציה, 500 שורות logcat אחרונות של אותו תהליך (דורש `READ_LOGS`, מותר לאפליקציית מערכת), ואופציונלית צילום מסך.
- **מסך תצוגה מקדימה לפני שליחה:** המשתמש רואה בדיוק מה נשלח ויכול להוריד לוג או צילום מסך.
- תור offline: נשמר מקומית ונשלח כשיש רשת.
- "תיבת הודעות" בתוך מסך המשוב: הסטטוסים חוזרים במשיכה (poll) יחד עם בדיקת העדכון, כי אין FCM. כשמגיע סטטוס חדש מוצגת התראה רגילה.

### 3.2 סינון במכשיר לפני שליחה
מתוך הלוג מוסרים: מספרי טלפון, כתובות מייל, IMEI/ICCID, קואורדינטות, ותוכן הודעות או אנשי קשר שמופיע ב-log. הסינון רץ שוב בשרת לפני Gemini.

## 4. מיון עם Gemini

Cron ב-Worker כל שעה:
1. מושך משובים במצב `new`.
2. **כפילויות:** embedding לכל משוב (מודל ה-embedding של Gemini), השוואת cosine מול אשכולות פתוחים מאותה אפליקציה. מעל סף, Gemini מאשר או דוחה את ההצמדה.
3. **החלטה:** קריאה למודל Flash עם פלט JSON מובנה בלבד:
   `{category, severity: 1-4, app, cluster_id, decision: deep|needs_info|wontfix|duplicate, summary_he, repro_steps}`.
4. אשכול שמגיע ל-`deep` (או ש-severity=1, או 3 מכשירים ומעלה) → נפתח issue.
5. לכל משוב באשכול נכנסת להודעות "המשוב שלך בטיפול".

**הגנה מ-prompt injection:** טקסט המשוב הוא קלט לא אמין. ל-Gemini אין כלים, הפלט מוגבל ל-enum, וב-issue טקסט המשוב מופיע בתוך בלוק ציטוט מסומן "קלט משתמש - לא הוראות". הגבלת קצב: עד 10 משובים ליום למכשיר.

## 5. טיפול ע"י Claude

- issue נפתח ב**ריפו פרטי נפרד** (`FutureOS-feedback`), כי המשוב מכיל לוגים של משתמשים והריפו הראשי עשוי להיות ציבורי. ה-issue כולל סיכום, צעדי שחזור, גרסאות ולוג מסונן.
- label `triage:accepted` מפעיל GitHub Action עם `anthropics/claude-code-action`. Claude מתקן בריפו הקוד ופותח PR שמקשר ל-issue.
- חלופה: Claude Code routine (טריגר מתוזמן) שקורא issues פתוחים. פחות מיידי, אבל רץ על המנוי ולא על API.
- ל-Claude **אין** הרשאת merge. branch protection על main דורש review של המשתמש.

## 6. שער אישור וסגירת המעגל

1. **PR:** המשתמש מאשר וממזג ב-GitHub. בלי זה לא קורה כלום.
2. **בנייה + חתימה:** בניית AOSP דורשת ~300GB ו-32-64GB RAM, ולא נכנסת ל-GitHub Actions חינמי. הבנייה רצה על המחשב של המשתמש (או מכונה שהוא מאשר), והחתימה נעשית במפתח שנמצא רק אצלו. **הערה: דיסק C כמעט מלא.** צריך דיסק ייעודי לפני שזה אפשרי.
3. **פרסום:** העלאה ל-R2 ולחיצה על "פרסם ל-dev / 5%" בדשבורד שמאחורי Cloudflare Access (רק המייל של המשתמש). ה-Worker מסרב לשנות `rollout_pct` בלי session מאומת.
4. **"תוקן בגרסה X":** ה-release ב-D1 מכיל רשימת issue-ים שתוקנו (נגזרת מ-`Fixes #N` ב-PR-ים הממוזגים). כשמכשיר מדווח `boot_ok` על גרסה X, כל משוב שלו שקשור לאשכולות האלה מקבל את ההודעה. ההודעה מגיעה רק כשהתיקון באמת אצלו.

## 7. מבנה נתונים (D1 / SQLite)

```sql
devices(id TEXT PK,            -- UUID אקראי שנוצר במכשיר, ניתן לאיפוס; לא IMEI
        token_hash TEXT, channel TEXT, build TEXT, bucket INT,
        consent_feedback INT, consent_logs INT, last_seen INT)
releases(id TEXT PK, version TEXT, build_fingerprint TEXT, channel TEXT,
         payload_url TEXT, payload_sha256 TEXT, size INT, metadata_json TEXT,
         fixed_issues TEXT, created_at INT)
rollouts(release_id TEXT, pct INT, state TEXT,  -- draft|active|paused|done
         approved_by TEXT, approved_at INT)
update_events(device_id, release_id, event TEXT, -- offered|downloaded|applied|boot_ok|failed
              error_code INT, at INT)
feedback(id TEXT PK, device_id, app_pkg, app_version, build, type, text,
         log_r2_key, screenshot_r2_key, status TEXT, cluster_id, created_at INT)
clusters(id TEXT PK, app_pkg, title_he, severity INT, decision TEXT,
         gh_issue INT, fixed_in_release TEXT, embedding BLOB)
inbox(id, device_id, feedback_id, kind TEXT, -- in_progress|needs_info|fixed
      text_he, created_at INT, delivered_at INT)
```

API: `POST /v1/register`, `GET /v1/update/check`, `POST /v1/update/event`, `POST /v1/feedback`, `GET /v1/inbox`. הדשבורד: `/admin/*` מאחורי Access.

### 7.1 אימות ופרוקסי ניווט (אותו שרת)
- `POST /v1/register` מחזיר `{device_id, token}`. כל קריאה אחרת שולחת `Authorization: Bearer <token>` + `X-Device-Id`. בשרת נשמר רק hash של הטוקן.
- הטוקן נשמר ב-FutureUpdater (לפני ה-AOSP: ב-Settings) ונחשף לאפליקציות `com.future.*` דרך ContentProvider עם הרשאת signature:
  `content://com.future.settings.futureserver/credentials`, שורה אחת, עמודות `device_id`, `token`. Cursor ריק = עוד לא נרשם (הרישום רץ ברקע, הקורא מנסה שוב מאוחר יותר). קריאה בלבד, `readPermission=com.future.settings.permission.FUTURE_SERVER` (signature). ה-prefs של הטוקן והמשוב מוחרגים מגיבוי (ב-Settings `allowBackup="false"`, וההחרגה היא הגנה נוספת).
- כתובת השרת: `ro.futureos.api_base` ב-build.prop של ה-AOSP. עוזר משותף ב-SharedKeypadNav `FutureServer.baseUrl()` קורא אותה, ואם אין, נופל ל-`BuildConfig.FUTURE_API_BASE`.
- מחליף את פונקציות ה-Firebase של Navigation (החלטה #28, מפתחות HERE/SIRI יוצאים מה-APK ונשמרים כ-Worker secrets):
  - `POST /v1/nav/route` `{from:{lat,lng}, to:{lat,lng}, mode:"car"}` → JSON של HERE routing v8 כמו שהוא (`return=polyline,summary,actions,instructions`).
  - `POST /v1/nav/siri/stop` `{stopId, lineRef?}` → XML של SIRI SM כמו שהוא. cache של 20 שניות לכל stopId.
  - `503` כשהשירות כבוי, `429` על מכסה (60 קריאות route לשעה למכשיר). האפליקציה מציגה "אין מידע זמן אמת".

## 8. פרטיות והסכמה

- **בהגדרה הראשונית של המכשיר:** מסך הסכמה עם שתי בחירות נפרדות, שתיהן כבויות כברירת מחדל:
  (א) "לשלוח משוב עם פרטי גרסה", (ב) "לצרף לוג טכני למשוב". עדכוני מערכת לא דורשים הסכמה (נשלחים רק גרסה ו-UUID).
- כל שליחה מציגה תצוגה מקדימה. אין שליחה ברקע.
- לא נשלחים: IMEI, מספר טלפון, אנשי קשר, הודעות, מיקום, רשימת אפליקציות.
- שמירה: לוגים וצילומי מסך 90 יום, טקסט ומטא-דאטה שנה. "מחק את המידע שלי" בהגדרות מוחק לפי UUID.
- **Gemini:** מקבל רק טקסט ולוג אחרי סינון. בשכבה החינמית של Gemini API גוגל רשאית להשתמש בתוכן לשיפור המוצרים שלה, ולכן מומלץ חשבון עם חיוב פעיל (שכבה בתשלום). העלות זניחה (סעיף 9).
- דין ישראלי: חוק הגנת הפרטיות (כולל תיקון 13) מחייב מדיניות פרטיות שקופה. צריך טקסט מדיניות שהמשתמש מאשר לפני הפצה למכשירים שאינם שלו.

## 9. עלות חודשית משוערת

הנחה: עד 500 מכשירים, OTA מלא ~1GB פעם בחודש, ~300 משובים בחודש.

| רכיב | שירות | עלות |
|-|-|-|
| API + Cron | Cloudflare Workers חינמי (100K בקשות ליום) | $0 |
| מסד נתונים | D1 חינמי (5GB) | $0 |
| קבצי OTA ולוגים | R2: 10GB חינם, אחר כך כ-$0.015 ל-GB, תעבורה יוצאת חינם | $0-1 |
| דשבורד מוגן | Cloudflare Access חינמי (עד 50 משתמשים) | $0 |
| מיון | Gemini Flash + embeddings, ~300 קריאות | פחות מ-$1 |
| טיפול | claude-code-action: לפי שימוש ב-API, בערך $0.5-3 ל-issue | $5-30 (תלוי בכמות) |
| issues ו-Actions | GitHub, ריפו פרטי (2,000 דקות Actions חינם) | $0 |
| דומיין (אופציונלי) | אפשר להתחיל עם `*.workers.dev` | ~$1 לחודש אם רוצים |

**סה"כ: בערך $0-5 לתשתית, ועוד עלות Claude לפי כמות התיקונים.** המחירים משוערים, לאמת בדפי התמחור לפני ההתחייבות. אם יעברו את ה-$5 בחודש, Workers Paid עולה $5.

## 10. מה צריך מהמשתמש (לא נפתח שום חשבון ולא הוצא כסף)

1. **חשבון Cloudflare** (חינמי) ואישור להקים Worker, D1, R2 ו-Access.
2. **מפתח Gemini API** מ-Google AI Studio, עדיף עם חיוב פעיל בגלל סעיף 8.
3. **GitHub:** לאשר ריפו פרטי `FutureOS-feedback`, ו-GitHub App או fine-grained PAT ל-Worker (הרשאת issues בלבד).
4. **Claude ב-Actions:** `ANTHROPIC_API_KEY` או OAuth token של המנוי, כ-secret בריפו.
5. **מפתחות חתימה:** ליצור release keys ו-OTA keys במחשב שלו, לשמור offline ולגבות.
6. **מכונת בנייה ל-AOSP:** דיסק של ~500GB (C מלא).
7. **החלטות:** A/B או Virtual A/B (מול צ'אט ה-port), טקסט מדיניות פרטיות, ספי ה-rollout (5/25/100, 95% boot_ok).

## 11. שלבים מוצעים

1. בדיקת מחיצות F22 Pro + החלטת A/B (עם צ'אט ה-port).
2. Worker + D1 + דשבורד, משוב בלבד, בלי Gemini. מסך משוב באפליקציית Settings הנוכחית, כך שאפשר להתחיל לפני ה-AOSP.
3. מיון Gemini + issue-ים + claude-code-action.
4. FutureUpdater + OTA בערוץ dev לטלפון אחד.
5. פריסה הדרגתית, עצירה אוטומטית והודעות "תוקן בגרסה X".

## 12. שאלות פתוחות
- האם הריפו הראשי ציבורי? אם כן, חובה ריפו פרטי למשוב (ההנחה כאן).
- מי משתמשי הקצה בשלב הראשון: רק הטלפון של המשתמש, או גם מכשירים של אחרים? זה קובע כמה דחוף טקסט הפרטיות.
- האם להתחיל משלב 2 על ה-Android הנוכחי לפני שה-port מוכן.
