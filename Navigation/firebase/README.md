# שכבת השרת (Firebase) של אפליקציית הניווט

כל עוד `Navigation/app/google-services.json` **לא** קיים, כל מה שכתוב כאן לא
משפיע על כלום: תוסף `google-services` לא מוחל בכלל (ר' `app/build.gradle.kts`),
`BuildConfig.FIREBASE_CONFIGURED` הוא `false`, ו‑`FirebaseBackend.init()` יוצא
מיד. האפליקציה ממשיכה לדבר ישירות מול Nominatim/HERE/SIRI ולייבא את קובץ
ה‑GTFS בעצמה, בדיוק כמו קודם. ברגע שהקובץ מתווסף והאפליקציה נבנית מחדש —
אותו קוד עובר לעבוד מול השרת, בלי שינוי נוסף.

## מה עובר לשרת ולמה

| מנגנון | לפני | אחרי |
| --- | --- | --- |
| חיפוש כתובות | כל מכשיר פונה לשרת ההדגמה הציבורי של Nominatim, שמדיניותו מגבילה לבקשה אחת בשנייה | פונקציה `geocodeSearch` עם מטמון משותף ב‑Firestore; הקצב נאכף בצד אחד מרוכז |
| ניתוב נהיגה | מפתח HERE יושב ב‑APK (`BuildConfig.HERE_API_KEY`) | הפונקציה `drivingRoute` מחזיקה את המפתח כסוד בשרת; המכשיר לא רואה אותו |
| תחבורה בזמן אמת | כתובת ומפתח SIRI ב‑APK | הפונקציה `siriStopMonitoring` מחזיקה אותם כסוד |
| נתוני תחבורה ציבורית | כל מכשיר מוריד ZIP ארצי של ~130MB ומפרק CSV של מיליוני שורות | משימה מתוזמנת בונה פעם בלילה חבילה מסוננת לאזור (NDJSON ב‑gzip) ב‑Cloud Storage; המכשיר מוריד רק אותה |

שתי הערות חשובות:

* **אין שבירה כשהשרת נופל.** כל קריאה דרך ה‑proxy שנכשלת חוזרת לנתיב הישיר
  שהיה קודם, וייבוא חבילה שנכשל חוזר לייבוא ה‑GTFS המקומי. השרת הוא שיפור,
  לא תלות קשיחה.
* **המפתחות ב‑Remote Config הם נתיב הגיבוי בלבד.** בנתיב הראשי המכשיר לא
  מקבל מפתחות כלל. `RemoteKeys` קיים כדי שגם בנתיב הישיר אפשר יהיה להחליף
  מפתח מרחוק בלי לבנות ולהתקין גרסה חדשה.

## הקמה — מה צריך לעשות בקונסולה

1. **פרויקט:** [console.firebase.google.com](https://console.firebase.google.com)
   → "Add project". השם לא משנה.
2. **תוכנית Blaze.** הכרחי: פונקציות בתוכנית החינמית לא יכולות לפנות לרשת
   חיצונית (HERE, Nominatim, משרד התחבורה), וזה בדיוק מה שהן עושות כאן. יש
   מסגרת חינמית נדיבה; אפשר וכדאי להגדיר התראת תקציב.
3. **אפליקציית אנדרואיד:** Project settings → "Add app" → Android, עם
   package name **`com.future.navigation`** בדיוק. מורידים את
   `google-services.json` ושמים אותו ב‑`Navigation/app/google-services.json`.
   (הקובץ ב‑`.gitignore` — הוא לא אמור להגיע לגיט.)
4. **Authentication** → Sign‑in method → מפעילים **Anonymous**. בלי זה כל
   הקריאות יחזרו `unauthenticated` (והאפליקציה תיפול חזרה לנתיב הישיר).
5. **Firestore Database** → Create database (אותו אזור של הפונקציות).
6. **Storage** → Get started.

## פריסה מהמחשב

```bash
npm install -g firebase-tools
firebase login
cd Navigation/firebase
cp .firebaserc.example .firebaserc     # ואז לערוך: project id אמיתי
cd functions && npm install && cd ..
```

הסודות (נשמרים ב‑Secret Manager, לא בקוד ולא בגיט):

```bash
firebase functions:secrets:set HERE_API_KEY
firebase functions:secrets:set SIRI_BASE_URL
firebase functions:secrets:set SIRI_API_KEY
firebase functions:secrets:set ADMIN_TOKEN
```

`ADMIN_TOKEN` הוא מחרוזת אקראית שממציאים כאן — היא שומרת על נקודת הקצה
הידנית של בניית החבילה.

פריסה:

```bash
firebase deploy --only functions,firestore:rules,storage
```

בנייה ראשונה של חבילת התחבורה (אחרת מחכים עד 03:30 בלילה):

```bash
curl "https://europe-west1-<PROJECT-ID>.cloudfunctions.net/refreshTransitBundleNow?token=<ADMIN_TOKEN>"
```

היא מורידה ~130MB, מסננת ומעלה — סדר גודל של כמה דקות. התשובה היא סיכום עם
מספר התחנות, הנסיעות וגודל החבילה.

## אזור (region)

הפונקציות נפרסות ל‑`europe-west1`, וזה חייב להיות זהה ל‑
`FIREBASE_FUNCTIONS_REGION` ב‑`app/build.gradle.kts` (ברירת המחדל שם זהה).
אזור אחר → כל קריאה מהמכשיר חוזרת `NOT_FOUND`. לשינוי: `setGlobalOptions`
ב‑`functions/src/index.ts` + `FIREBASE_FUNCTIONS_REGION=...` ב‑
`local.properties`.

## מבנה הנתונים ב‑Firestore

| אוסף | תוכן |
| --- | --- |
| `transit_bundles/{regionId}` | `storagePath`, `version`, `minLat/maxLat/minLon/maxLon`, ספירות, `updatedAt` |
| `geocode_cache/{queryId}` | תוצאות חיפוש כתובת, 30 יום |
| `service_state/nominatim` | חותמת "מתי מותר לפנות שוב" — אכיפת הקצב של Nominatim |

חוקי הגישה (`firestore.rules`, `storage.rules`): הלקוח **קורא בלבד**, ורק את
שני האוספים שהוא באמת צריך. כל הכתיבות הן של הפונקציות דרך ה‑Admin SDK,
שעוקף את החוקים.

## להוסיף אזור נוסף

`REGIONS` ב‑`functions/src/gtfs.ts` — מוסיפים תיבה ופורסים מחדש. האפליקציה
בוחרת לבד את החבילה שמכסה את התיבה שהיא מבקשת, ואם אין כזו היא חוזרת לייבוא
המקומי.

## כיבוי מרחוק

ב‑Remote Config אפשר להגדיר:

* `use_functions_proxy = false` — כל הקריאות חוזרות לנתיב הישיר.
* `transit_data_from_server = false` — חזרה לייבוא GTFS מקומי.

שימושי אם נגמרת המכסה או אם משהו בשרת נשבר, בלי להתקין גרסה חדשה במכשיר.
