# שכבת השרת (Firebase) של אפליקציית הניווט

**המכשיר לא מריץ Google Play Services, ולכן אין באפליקציה את ה-SDK של
Firebase בכלל.** כל הדיבור עם השרת הוא REST רגיל על OkHttp
(`data/backend/FirebaseBackend.kt`): כניסה אנונימית דרך Identity Toolkit,
קריאה לפונקציות ה-callable ב-HTTPS, וקריאת Firestore ו-Storage דרך ה-REST
API שלהם. מ-`google-services.json` הבנייה לוקחת רק שלושה ערכים (מזהה
הפרויקט, מפתח ה-Web API וה-bucket), בלי תוסף `google-services`; אפשר גם
לתת אותם ב-`local.properties` (`FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`,
`FIREBASE_STORAGE_BUCKET`).

כל עוד אין פרויקט בבנייה, כל מה שכתוב כאן לא משפיע על כלום:
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
* **המפתחות ב‑`app_config/navigation` הם נתיב הגיבוי בלבד.** בנתיב הראשי המכשיר לא
  מקבל מפתחות כלל. `RemoteKeys` קורא את המסמך הזה ב-Firestore (במקום Remote Config,
  שה-SDK שלו דורש Google Play Services) כדי שגם בנתיב הישיר אפשר יהיה
  להחליף מפתח מרחוק בלי לבנות ולהתקין גרסה חדשה.

## הקמה — מה צריך לעשות בקונסולה

1. **פרויקט:** [console.firebase.google.com](https://console.firebase.google.com)
   → "Add project". השם לא משנה.
2. **תוכנית Blaze.** הכרחי: פונקציות בתוכנית החינמית לא יכולות לפנות לרשת
   חיצונית (HERE, Nominatim, משרד התחבורה), וזה בדיוק מה שהן עושות כאן. יש
   מסגרת חינמית נדיבה; אפשר וכדאי להגדיר התראת תקציב.
3. **אפליקציית אנדרואיד:** Project settings → "Add app" → Android, עם
   package name **`com.future.navigation`** בדיוק. מורידים את
   `google-services.json` ושמים אותו ב‑`Navigation/app/google-services.json`
   (הבנייה קוראת ממנו רק את מזהה הפרויקט, המפתח וה-bucket).
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
| `app_config/navigation` | מתגים מרחוק ומפתחות לנתיב הישיר (ר' "כיבוי מרחוק") |

חוקי הגישה (`firestore.rules`, `storage.rules`): הלקוח **קורא בלבד**, ורק את
`transit_bundles` ואת `app_config/navigation`. כל הכתיבות הן של הפונקציות דרך ה‑Admin SDK,
שעוקף את החוקים.

## להוסיף אזור נוסף

`REGIONS` ב‑`functions/src/gtfs.ts` — מוסיפים תיבה ופורסים מחדש. האפליקציה
בוחרת לבד את החבילה שמכסה את התיבה שהיא מבקשת, ואם אין כזו היא חוזרת לייבוא
המקומי.

## כיבוי מרחוק

במסמך Firestore בשם `app_config/navigation` (יוצרים אותו ידנית בקונסולה)
אפשר להגדיר שדות בוליאניים:

* `use_functions_proxy = false` — כל הקריאות חוזרות לנתיב הישיר.
* `transit_data_from_server = false` — חזרה לייבוא GTFS מקומי.

ובאותו מסמך, כמחרוזות, מפתחות לנתיב הישיר: `here_api_key`, `siri_base_url`,
`siri_api_key`. המכשיר קורא את המסמך לכל היותר פעם בשעה ושומר את הערכים.

שימושי אם נגמרת המכסה או אם משהו בשרת נשבר, בלי להתקין גרסה חדשה במכשיר.
