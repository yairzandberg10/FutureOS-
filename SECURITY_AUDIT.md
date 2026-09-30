# דוח אבטחה - FutureOS (Red Team, 2026-09-30)

ביקורת אבטחה מלאה על המאגר: 31 אפליקציות, הספרייה המשותפת SharedKeypadNav, שרתי ה-Firebase של Messages ו-Navigation, ה-CI, הקוד ה-Native של Assistant וקושחת המקלדת. הדוח הזה הוא נקודת התחלה לסבב הבא, לא הצהרה שהמערכת מאובטחת.

> **FutureOS אינה "100% מאובטחת".** התהליך הוא: מצא, נסה לנצל בצורה בטוחה, תקן, בדוק, בדוק שוב. סעיף "סיכונים שעדיין לא נפתרו" למטה חשוב לא פחות מסעיף התיקונים.

## סיכום

- נבדקו כל ה-Manifests (32), כל הרכיבים המיוצאים, כל ה-BroadcastReceivers הדינמיים, PendingIntents, ContentProviders, FileProviders, שימוש ב-`su`, קריפטו, רשת/TLS, WebView (אין), Sockets (אין), דה-סריאליזציה (אין), SQL, לוגים, Secrets בכל היסטוריית git (50 קומיטים), CI, תלויות npm, JNI וקושחת החומרה.
- **הממצא החמור ביותר:** העץ שב-git **לא מתקמפל**. הקבוע `FutureUIActions.PERMISSION_SYSTEM` (הרשאת החתימה שמגינה על שידורי מקשים ושיחות) מופיע בעשרה מקומות ולא מוגדר באף קובץ. ה-CI לא תפס את זה כי כל ה-jobs נכשלים קודם, על `./gradlew: Permission denied`.
- נמצאו וטופלו 12 חולשות (2 HIGH, 7 MEDIUM, 3 LOW). חמישה סיכונים (בהם אחד HIGH תכנוני) נשארו פתוחים - ראו "סיכונים שעדיין לא נפתרו".
- `security-check.py` הורחב בעשרה כללים חדשים או מורחבים. הורץ על הקומיט הקודם: **61 בעיות**. על העץ המתוקן: **0**.
- **מה לא נעשה:** לא בניתי ולא התקנתי על מכשיר (אין Android SDK בסביבת הענן, והורדות מ-`dl.google.com` חסומות). הקוד של Android שונה בלי קומפילציה - ראו "מגבלות הבדיקה" ו"מה לבדוק אצלך".

## מודל איום

| | תוקף | מה נבדק |
|---|---|---|
| A, B | אפליקציה זרה (עם/בלי הרשאות) | כל הרכיבים המיוצאים, Intents, Broadcasts, Providers, Notifications, אייקונים |
| C | גישה פיזית | מסך הנעילה, Terminal, Bootloader, Verified Boot, הצפנת נתונים במנוחה |
| D, E | USB / ADB | ניפוי באגים, `adb backup`, קושחת המקלדת |
| F | חבילת עדכון זדונית | אין מנגנון OTA במאגר (ראו למטה) |
| G | קלט זדוני | Intents נכנסים, הודעות צ'אט, BLE, QR, GTFS |
| H | תוקף רשת | TLS, HTTP, Firebase, Cloud Functions |
| I, J, K | הסלמת הרשאות, בריחה מה-sandbox, קריאת נתוני אפליקציה אחרת | `su`, Providers, FileProvider, Share, Backup |
| L | עקיפת נעילה | FutureUI LockScreenController, PinStore, זיהוי פנים |
| M | ניצול שירותי מערכת | שירותי נגישות, NotificationListener, InCallService |

## Attack Surface

**רכיבים מיוצאים ללא הרשאה (כולם מכוונים ונבדקו):**

| רכיב | סיבה | מסקנה |
|---|---|---|
| `MainActivity` של כל אפליקציה | מפעיל | תקין. `dispatchTouchEvent` חסום |
| Clock `SET_ALARM`/`SET_TIMER`, Camera `IMAGE_CAPTURE`/`VIDEO_CAPTURE` | Intents סטנדרטיים | המסך לא קורא Extras בכלל, אין ניצול |
| Contact `INSERT`/`VIEW`/`EDIT`, Messages `SENDTO`, dialer `DIAL`, Notes/Translate `SEND` | Intents סטנדרטיים | מילוי מראש בלבד, קלט מסונן ומוגבל באורך, בלי חיוג/שליחה אוטומטיים |
| Files `VIEW`/`GET_CONTENT`/`OPEN_DOCUMENT`, Gallery `PICK`/`GET_CONTENT` | בוררים | Files מתעלם מהנתונים הנכנסים. Gallery מחזיר רק את מה שהמשתמש בחר |
| FutureUI `ShareActivity` | חלון שיתוף | רק ACTION_SEND, הקבצים חייבים הרשאה מהשולח, Intent פנימי עם component מפורש, תהליך נפרד |
| `AppWidgetProvider` (16 אפליקציות) | widgets | `APPWIDGET_UPDATE` בלבד |
| Music `MusicPlaybackService` | בקרי מדיה | **ממצא FOS-08** |
| Messages `SmsDeliverReceiver`, `MmsReceiver`, `HeadlessSmsSendService`, Dialer `CallService`, Keyboard `KeyboardService`, שירותי הנגישות | מוגנים בהרשאת מערכת (BROADCAST_SMS, BIND_*) | תקין |

**מוגנים בהרשאת החתימה של הסוויטה:** `ThemeProvider`, `SystemUiSettingsProvider`, `KeyboardSettingsProvider`, `WallpaperProvider`, `LockSettingsActivity`, `SettingsActivity`, `CallActionReceiver`, `BlockNumberReceiver`, `AsrWarmupReceiver`, וכל שידורי המקשים והשיחה. ההרשאות מוגדרות בכל אפליקציה (הגנה מפני תפיסת שם) והלקוחות בודקים חתימה לפני גישה (`TrustedProviders`).

**שאר הממשקים:** אין WebView, אין Sockets, אין `ObjectInputStream`, אין טעינת קוד דינמית (רק `System.loadLibrary` של ספריות מה-APK). כל ה-SQL עם פרמטרים. שימוש ב-`su`: `RootShell` (פקודות קבועות, בוליאנים, מספרים; ערכים חיצוניים עוברים `isSafeToken` + `quote`) ו-Terminal (מעטפת root בכוונה). Native: `whisper_jni.cpp` (68 שורות, מודל מה-APK), espeak, ggml (קוד צד שלישי, לא נסרק לעומק).

**אין מנגנון OTA במאגר.** עדכון = `adb install` או Files שמעביר למתקין המערכת. הגנת rollback/חתימה היא של PackageManager (אותה חתימה, `versionCode` לא יורד).

## חולשות שהתגלו

מקרא: **אומת** = הורץ בדיקה אמיתית. **נבדק בקוד** = הנתיב אושר בקריאת הקוד, לא הורץ על מכשיר.

### FOS-01 · HIGH · הקבוע `PERMISSION_SYSTEM` חסר, העץ לא מתקמפל
- **רכיב:** `SharedKeypadNav/.../actions/FutureUIActions.kt`, `KeyPressBroadcasts.kt`, שירותי FutureUI/SystemUI.
- **תרחיש:** `git clone` נקי לא נבנה. ההגנה על שידורי `OPTIONS/STAR/POUND` ו-`CALL_RINGING/ENDED` (שבלעדיה כל אפליקציה "לוחצת" מקשים או מזייפת שיחה נכנסת ומשהה את מסך הנעילה) קיימת רק במכשיר שבנה מעץ עבודה שלא נשמר ל-git. מי שיתקן את שגיאת הקומפילציה ב-`""` או `null` ישבית אותה בשקט.
- **Root Cause:** קומיט `a64cb8f` הוסיף עשרה שימושים בלי להוסיף את ההגדרה.
- **Evidence:** `git log -S PERMISSION_SYSTEM` מצביע רק על `a64cb8f`. אין `const val PERMISSION_SYSTEM` באף קובץ בעץ.
- **תיקון:** `const val PERMISSION_SYSTEM = "${SystemUiTarget.PACKAGE}.permission.SYSTEM_SETTINGS"`.
- **Regression:** `FutureUIActionsTest` (הקבוע קיים, וההרשאה מוצהרת `protectionLevel="signature"` ב-Manifest של הספרייה) + כלל ב-`security-check.py`. **אומת:** ההרצה עוברת; על העץ הישן הכלל נכשל.
- **סיכון שנותר:** לא קומפלתי עם Android SDK. ב-CI הראשון שירוץ בצבע ירוק יתגלו שגיאות קומפילציה אחרות אם יש (בדקתי ייחוס לקבועים אחרים של הספרייה: אין חסרים).

### FOS-02 · MEDIUM · ה-CI לא בונה ולא בודק כלום
- **רכיב:** `.github/workflows/build.yml`, כל ה-`gradlew`.
- **תרחיש:** כל שבירה (קומפילציה, בדיקות, FOS-01) נכנסת ל-`main` בלי שאף בדיקה תעצור אותה.
- **Root Cause:** 31 קובצי `gradlew` ו-`build-all.sh` נשמרו ב-git כ-mode `100644` (פיתוח ב-Windows).
- **Evidence:** ריצות #1 ו-#90-94 נכשלו. ב-#94: 33 מתוך 34 jobs נכשלו ב-`./gradlew: Permission denied` (exit 126), רק `Discover apps` ו-`Security gate` עברו.
- **תיקון:** `git update-index --chmod=+x` לכל הסקריפטים, `.gitattributes` (`gradlew` ו-`*.sh` עם LF).
- **Regression:** כלל ב-`security-check.py` שבודק mode `100755` דרך `git ls-files -s`. **אומת** על העץ הישן (31 כשלים).
- **סיכון שנותר:** ה-workflow רץ רק על `main` ו-PR ל-`main`. לא ראיתי אותו ירוק עדיין. מעבר ל-CI תקין יחשוף כנראה כשלי בדיקות קיימים.

### FOS-03 · HIGH · קריסה חוזרת של FutureUI מאייקון של אפליקציה זרה
- **רכיב:** `StatusBarScreen` (אייקון ההתראה), `NotificationCenterScreen`, `HeadsUpNotificationScreen`, `RecentAppsManager` (גם בשיקוף SystemUI).
- **תרחיש:** אפליקציה שמותקנת במכשיר מפרסמת התראה עם אייקון bitmap ענק (קובץ PNG קטן, מאות מגה-בייט בפענוח). הפענוח רץ בתהליך של FutureUI ומפיל אותו ב-`OutOfMemoryError`. הקוד תפס `Exception`, ו-OOM הוא `Error`. ההתראה נשארת פעילה אחרי הריצה מחדש, לכן הקריסה חוזרת בכל עלייה: שורת המצב, מסנן המקשים ומסך הנעילה לא יציבים, ובין קריסה לעליה מחדש מסך הנעילה לא קיים.
- **Root Cause:** `catch (e: Exception)` סביב `Icon.loadDrawable`/`getApplicationIcon` של חבילה זרה.
- **Evidence:** `StatusBarScreen.kt:176` קורא ל-`smallIcon.loadDrawable` בכל סבב של 15 שניות לכל אפליקציה שמפרסמת התראה ושומר במטמון רק הצלחות.
- **תיקון:** `loadUntrusted()` (ב-SharedKeypadNav): מחזיר `null` על כל `Exception` ועל `OutOfMemoryError`, לא בולע ביטול קורוטינה ולא מסתיר `StackOverflowError`.
- **Regression:** `UntrustedResourcesTest` (6 בדיקות, **אומת**) + כלל ב-`security-check.py` ל-FutureUI/SystemUI.
- **סיכון שנותר / אימות:** **נבדק בקוד, לא שוחזר על מכשיר** (לא בניתי אפליקציית בדיקה זדונית). תקרת הפענוח נשארת בידי המערכת: אייקון שלא נכשל ב-OOM אבל איטי מאוד עדיין מעכב את ה-thread הראשי.

### FOS-04 · MEDIUM · צ'אט: מפתח ההסכמה לא ננעץ
- **רכיב:** `FutureChat.kt` (`checkPinnedKey`), `ChatBackend.lookupPhone`.
- **תרחיש:** שרת/מנהל Firebase פרוץ מחליף את `agreeKey` של איש קשר ומשאיר את `signKey`. כל הודעה שנשלחת אליו מוצפנת למפתח של התוקף, והמשתמש לא רואה כלום. זה בדיוק מה שהצפנה מקצה לקצה אמורה למנוע, וההערה בקוד טענה שהנעיצה מונעת את זה.
- **Root Cause:** נעיצה של מפתח החתימה בלבד, בזמן שההצפנה נעשית למפתח ההסכמה שנשלף מהשרת פעם ביום.
- **תיקון:** `KeyPins` נועץ את שני המפתחות. שינוי בכל אחד מהם מציג את ההודעה הקיימת "מפתח ההצפנה השתנה" עם שתי טביעות אצבע. נעיצה ישנה (מפתח חתימה בלבד) משודרגת בשקט.
- **Regression:** `KeyPinsTest` (7 בדיקות, **אומת**).
- **סיכון שנותר:** זה TOFU: זיהוי ולא מניעה. הודעה שנשלחה למפתח מוחלף בחלון שלפני האזהרה אבודה לתמיד. בנוסף, אין מסך שמציג את טביעת האצבע של המפתחות שלך, כך שהאימות ההדדי המוצע בהודעה לא אפשרי כרגע. מומלץ מסך "קוד אימות".

### FOS-05 · MEDIUM · גיבוי ענן של כל נתוני האפליקציות
- **רכיב:** 15 אפליקציות עם `allowBackup="true"` וקבצי כללים ריקים מהתבנית: Messages, Keyboard, Terminal, Tools, Files, Gallery, Camera, Clock, Calculator, Flashlight, Frixa, FutureLauncher, Guide, Sfarim, Wallpapers.
- **תרחיש:** `chat.db` ותמונות צ'אט מפוענחות (תיקיית `chat_media`), נעיצות המפתחות, מילים שהמקלדת למדה (כולל מה שהוקלד לפני שזוהה כשדה רגיל), היסטוריית Terminal והפתקים המהירים ב-Tools עולים לגיבוי ענן ולהעברה בין מכשירים. הצפנת הצ'אט נשברת בקצה ההתקן.
- **Root Cause:** ברירת המחדל של תבנית Android Studio לא שונתה.
- **תיקון:** `allowBackup="false"` בכולן. `adb install -r` שומר נתונים ולכן ההתקנה מעל הקיימת לא מאבדת כלום.
- **Regression:** כלל ב-`security-check.py` על כל אפליקציה. **אומת** על העץ הישן (15 כשלים).
- **סיכון שנותר:** אין שחזור נתונים אוטומטי בהחלפת מכשיר (לא היה נדרש עד עכשיו).

### FOS-06 · MEDIUM · חלון הנעילה מעביר מגע לחלונות שמתחתיו
- **רכיב:** `LockScreenController.showWindow` (`FLAG_NOT_TOUCHABLE`).
- **תרחיש:** על מכשיר עם לוח מגע (ה-README מתאר חומרה בלי מגע, אבל ההערות ב-`dispatchTouchEvent` של כל אפליקציה מעידות שמכשיר הבדיקה כן מקבל מגע) מסך המגע עובר דרך החלון האטום אל חלונות המערכת מתחתיו, למשל וילון ההתראות של אנדרואיד, ומשם להגדרות מהירות. ההגדרה `policy_control immersive.full=*` שמסתירה את הפסים המקוריים **לא קיימת מאנדרואיד 11**, והקוד מציין שהיא לא נבדקה.
- **תיקון:** הסרת `FLAG_NOT_TOUCHABLE` מחלון הנעילה. חלון שמקבל מגע בולע אותו, והמקשים ממשיכים לעבור דרך מסנן המקשים.
- **Regression:** כלל ב-`security-check.py` (קובץ עם `TYPE_ACCESSIBILITY_OVERLAY` בתיקיית lockscreen לא יכיל `FLAG_NOT_TOUCHABLE`).
- **אימות:** **נבדק בקוד, לא על מכשיר.** ראו "מה לבדוק אצלך". אם מסתבר שהוילון מקורי מושך גם בלי החלק הזה, צריך לחסום אותו ברמת חלון נפרד.

### FOS-07 · MEDIUM · מפתחות HERE/SIRI בתוך ה-APK
- **רכיב:** `Navigation/app/build.gradle.kts` (`buildConfigField`), `RemoteKeys.kt`.
- **תרחיש:** מפתח מ-`local.properties` נצרב ב-`BuildConfig`. `base.apk` קריא לכל אפליקציה במכשיר, וערכי Remote Config קריאים לכל לקוח שמחזיק את `google-services.json` (שנמצא ב-APK). מי שמחלץ את המפתח מנצל את המכסה ואת החשבון המחויב של HERE, וה-proxy שנבנה כדי למנוע את זה לא עוזר.
- **תיקון:** כשיש `google-services.json` המפתחות לא נצרבים, אלא אם `EMBED_API_KEYS=true` ב-`local.properties`. בלי Firebase ההתנהגות זהה לקודם. אזהרה מתועדת ב-`RemoteKeys`.
- **Regression:** כלל ב-`security-check.py` על `buildConfigField` עם `*_KEY` ישיר מ-`local.properties`.
- **סיכון שנותר:** אם בנית גרסה קודמת עם מפתחות, הם כבר ב-APK שמותקן. **מומלץ לסובב (rotate) את המפתחות.** כשה-proxy נופל אין נפילה אוטומטית לקריאה ישירה. ערכי Remote Config לא מתאימים לסודות בכלל.

### FOS-08 · MEDIUM · כל אפליקציה יכולה לגרום לנגן המוזיקה לפתוח תוכן לבחירתה
- **רכיב:** `MusicPlaybackService` (מיוצא, `MediaSessionService`).
- **תרחיש:** ב-`onConnect` ברירת המחדל של media3 נותנת לכל בקר `COMMAND_SET_MEDIA_ITEM`. אפליקציה זרה מתחברת ומבקשת מהנגן (שרץ עם הרשאות המוזיקה) לפתוח `content://` או `file://` לבחירתה, ויכולה להשתלט על ההשמעה.
- **תיקון:** לחבילות אחרות מוסרות `COMMAND_SET_MEDIA_ITEM` ו-`COMMAND_CHANGE_MEDIA_ITEMS`. נגן/השהה/הבא/הקודם והחיפוש נשארים (בקרי מדיה, כפתורי אוזניות).
- **Regression:** כלל ב-`security-check.py` ל-`MediaSessionService` מיוצא.
- **אימות:** **לא קומפל**. שמות ה-API (`Player.Commands.buildUpon().remove(...)`, `ConnectionResult.accept(...)`) נלקחו מהקוד הקיים ומתיעוד media3 1.5.1, ולא אומתו מול ה-JAR (`dl.google.com` חסום בסביבה).
- **סיכון שנותר:** כל אפליקציה עדיין יכולה להשהות/להפעיל ולקרוא מטא-דאטה של השיר.

### FOS-09 · MEDIUM · `geocodeSearch` ללא מכסה
- **רכיב:** `Navigation/firebase/functions/src/index.ts`.
- **תרחיש:** הכניסה אנונימית, כך שכל מי שמוציא את הגדרות Firebase מה-APK יוצר משתמשים ללא הגבלה. כל שאילתה שלא במטמון מחכה לתור של בקשה אחת לשנייה מול Nominatim ונוספת לתור בלי גבול, והמתנה מוגבלת ל-10 שניות, כלומר בקשות אמיתיות עוברות את קצב השרת הציבורי של OpenStreetMap ואפשר לחסום את כל הפרויקט (וגם לנפח את חשבון Firebase).
- **תיקון:** מכסה יומית (20,000) על בקשות upstream (החמצות מטמון), כמו ל-HERE ול-SIRI.
- **אימות:** `tsc --noEmit` עובר, `npm audit`: 0 חולשות (Navigation ו-Messages). לא הורץ באמולטור.
- **סיכון שנותר:** המכסה גלובלית, כלומר תוקף יכול לצרוך אותה למשתמשים אחרים (DoS). פתרון מלא: Firebase App Check. **נדרש `firebase deploy` על ידך.**

### FOS-10 · LOW · העלאות מדיה לצ'אט בלי הגבלת סוג תוכן
- **רכיב:** `Messages/firebase/storage.rules`.
- **תרחיש:** כל מספר מאומת יכול להעלות קובץ בגודל אפס או בסוג תוכן שרירותי (HTML, APK) לתיקיית כל משתמש אחר.
- **תיקון:** `size > 0` ו-`contentType == 'application/octet-stream'` (זה מה ש-`ChatBackend.upload` שולח).
- **Regression:** כלל ב-`security-check.py` על `storage.rules`. **נדרש `firebase deploy`.** לא הורץ באמולטור.

### FOS-11 · LOW · מספר טלפון ב-`Log.e`
`ContactRepository.kt` (dialer) רשם את המספר בשגיאת חיפוש שם. R8 מסיר רק `Log.v/d`. תוקן, וכלל ב-`security-check.py` על `Log.e/w/i` עם `$number/$phone/$address/$body/$pin`.

### FOS-12 · LOW · מפתח הקטלוג של Wallpapers לא ב-`.gitignore`
`Wallpapers/app/src/main/assets/firebase.json` (`projectId` + `apiKey`) לא היה מוחרג, ו-`security-check.py` פוטר קבצי `firebase.json` מסריקה. נוסף ל-`.gitignore`, וכלל בודק שהרשומות הנדרשות קיימות. סריקת Secrets בכל 50 הקומיטים: נקייה.

## שיפורי אבטחה שבוצעו

- `security-check.py`: עשרה כללים חדשים או מורחבים (גיבוי בכל אפליקציה, bit הרצה, `PERMISSION_SYSTEM`, `MediaSessionService`, חלון הנעילה, לוגים, כללי Firebase, מפתחות ב-BuildConfig, `.gitignore`, אייקונים זרים).
- 19 בדיקות JVM חדשות (RootShell quote/tokens מול `sh` אמיתי, `PERMISSION_SYSTEM`, `loadUntrusted`, `KeyPins`). הורצו בפועל בסביבה מבודדת, מול קבצי המקור האמיתיים.
- מסקנות ללא צורך בתיקון (אומתו): הזרקת פקודות ל-`su` - כל הערכים קבועים/בוליאניים/מספריים או עוברים `isSafeToken`+`quote`; אין TLS מוחלש; אין SQL מרוכב; `Icon`/`RemoteViews` של צד שלישי לא נטענים מעבר לאייקון; אין Secrets בהיסטוריה; `PinStore` (HMAC ב-Keystore, השוואה בזמן קבוע, השהיה מצטברת שנכתבת ב-`commit`), `FaceTemplateStore` (AES-GCM, IV מה-Keystore), `ChatCrypto` (ECDH זמני + HKDF + AES-GCM, חתימה על כל המעטפה, הקשר ב-AAD) - ללא ממצאים.

## סיכונים שעדיין לא נפתרו

1. **גישה פיזית (C), HIGH לפני ייצור - תכנון, לא באג.** נעילת ה-PIN היא שכבת אפליקציה על אנדרואיד בלי אישור נעילה מערכתי. מפתחות ה-FBE לא קשורים לסוד של המשתמש, ה-bootloader פתוח ו-Magisk מותקן (Verified Boot כבוי), כך שמי שמחזיק מכשיר כבוי יכול לחלץ את כל הנתונים. Terminal נותן מעטפת root בלי אימות משלו. החלון של הנעילה עולה רק אחרי שירות הנגישות, כלומר אחרי אתחול, אחרי קריסה ואחרי הפסקת השירות המכשיר פתוח לשניות. **להמשך:** קביעת אישור נעילה אמיתי (`cmd lock_settings set-pin`) כדי לקשור את ה-FBE, נעילת bootloader בדגם הייצור, ואימות PIN בכניסה ל-Terminal.
2. **הפרדת הרשאות בתוך הסוויטה.** ההרשאה `SYSTEM_SETTINGS` אחת לכל הסוויטה: כל אפליקציה שלה יכולה לענות/לדחות שיחות (`CallActionReceiver`), לחסום מספרים ולשלוט בהגדרות FutureUI. אפליקציה שנפרצת (למשל Tools עם סורק QR) מקבלת את כולן. **להמשך:** הרשאה נפרדת לכל יכולת (שליטה בשיחות, הגדרות).
3. **ספאם ושימוש לרעה בצ'אט (LOW-MEDIUM).** כל מספר מאומת יכול לכתוב ל-inbox של כל משתמש, וכל הודעה מעירה את המכשיר ב-FCM בעדיפות גבוהה. אין חסימה, הגבלת קצב ולא רשימת מותרים. מערכת אימות המספרים גם נותנת "האם המספר רשום" לכל מאומת (`phones/{number}`). **להמשך:** App Check, הגבלת קצב ב-Function, חסימת שולחים.
4. **אובדן הודעות בשגיאה זמנית.** `process()` מוחק את המסמך מהשרת גם כש-`handle()` נכשל בשגיאת רשת בשליפת המפתח של השולח. **להמשך:** למחוק רק כשל קבוע (חתימה, פורמט).
5. **הורדה לשיחה רגילה (SMS) ללא סימון.** כשהצ'אט נכשל ההודעה יוצאת כ-SMS גלוי, והמשתמש לא מקבל סימון ברור. תוקף רשת יכול לכפות את זה.

## ממצאים אינפורמטיביים

- **חתימה:** בלי `keystore.properties` הבנייה נחתמת במפתח ה-debug של המחשב (סיסמה ידועה `android`), וכל אמון ההרשאות בסוויטה תלוי בו. מומלץ מפתח release ייעודי (התמיכה קיימת).
- **זיהוי פנים:** "בסיסי", בלי חיות (תמונה עלולה לעבור). כבוי כברירת מחדל והמסך אומר זאת.
- **SystemUI (`com.android.sistemui`):** השיקוף של FutureUI נשאר מאחור: אין בו מסך נעילה ואין בו את כל ההקשחות. מעבר אליו ישמיט את הנעילה. `sync-from-futureui.sh` לא מסנכרן אותם.
- **קושחת המקלדת (`hardware/keyboard_prototype`):** `boot.py` לא מכבה את כונן ה-USB ואת ה-CDC של CircuitPython. בדגם ייצור יש לכבות אותם (`storage.disable_usb_drive()`, `usb_cdc.disable()`).
- **צילומי מסך של אפליקציות:** `RecentSnapshots` שומר WebP של כל אפליקציה בחזית ב-`cacheDir/recents`, גם של Terminal ו-Messages. נגיש לשורש/ADB בלבד, אך אין `FLAG_SECURE` באף אפליקציה רגישה.
- **היגיינת מאגר:** מטא-דאטה של IDE ו-`.artifacts` עם שם המשתמש ב-Windows, ו-36 קבצי פלט (צילומי מסך) תחת `Sfarim/tools/output`. לא סודי; מומלץ להוציא מ-git.
- **קוד צד שלישי:** ggml/whisper/espeak לא נסרקו לעומק. מעקב גרסאות מומלץ.

## בדיקות נוספות מומלצות

1. הרצת `./build-all.sh` ו-CI ירוק מלא (בדיקות יחידה כבר רצות שם).
2. בדיקת חדירה על מכשיר: אפליקציה זדונית שמפרסמת התראה עם אייקון ענק (FOS-03), ומנסה להתחבר ל-`MusicPlaybackService` (FOS-08), ולשלוח Broadcasts (`CALL_RINGING`, מקשים).
3. Fuzzing ל-`Sip.kt`/`Cpim.kt` (RCS) ולפענוח `pdu_alt` (MMS).
4. בדיקת Firebase Rules באמולטור (`firebase emulators`) עם `@firebase/rules-unit-testing`.
5. `npm audit` ותלויות Gradle (`dependencyCheck`) בכל סבב.
6. סקירה ידנית של Native (ggml) מול CVE ידועים.

## מגבלות הבדיקה

- אין Android SDK ואין מכשיר בסביבת הענן: **לא בוצעה קומפילציה של אף קובץ Android** (Kotlin מול `android.jar`, Compose). הבדיקות שרצו הן JVM טהורות, מול קוד המקור האמיתי עם stub ל-`android.util.Log`.
- `FutureChat.kt`, `MusicPlaybackService.kt`, `LockScreenController.kt`, `StatusBarScreen.kt` ושאר קבצי ה-UI שונו בלי קומפילציה.
- לא הורצו כללי Firebase באמולטור, ולא בוצע `firebase deploy`.
- לא נבדק נתיב אמיתי של מכשיר (מגע מעל חלון הנעילה, קריסת OOM, בקרי מדיה).
- CI לא רץ על הענף הזה (ה-workflow מופעל על `main` ו-PR ל-`main`).

## מה לבדוק אצלך (לפי `CLAUDE.md`: בלי בדיקות ממשק מצידי)

בנה והתקן מעל הקיים: `./gradlew installRelease` ואז `adb shell cmd package compile -m speed -f <חבילה>` ל-**FutureUI, Messages, Music, dialer, Navigation** וגם לכל אפליקציה ששונה לה Manifest (15 האפליקציות ברשימת FOS-05). אחרי כל התקנה:

1. **מסך נעילה (FOS-06):** כשהמכשיר נעול, גע במסך בקצה העליון ומשוך מטה. וילון ההתראות של אנדרואיד לא אמור להיפתח. מקשי OK/ספרות/תפריט ממשיכים לעבוד כרגיל.
2. **מסך נעילה:** שיחה נכנסת מעל הנעילה עדיין מופיעה ונענית, ומסך הנעילה חוזר בסיומה.
3. **צ'אט (FOS-04):** שליחת הודעה לאיש קשר רשום עובדת. אין הודעת "מפתח השתנה" אצל אנשי קשר קיימים אחרי העדכון (נעיצה ישנה משודרגת בשקט).
4. **Music (FOS-08):** הנגן עובד כרגיל מתוך האפליקציה, מרכז הבקרה מציג ושולט (השהה/הבא), כפתורי אוזניות עובדים.
5. **התראות:** שורת המצב, מרכז ההתראות והבאנר מציגים אייקוני אפליקציות כרגיל.
6. **Navigation (FOS-07):** אם יש `google-services.json`, חיפוש כתובת, ניווט ותחבורה בזמן אמת עובדים דרך ה-proxy. אם לא - אין שינוי.
7. **פריסה:** `firebase deploy` מ-`Navigation/firebase` ומ-`Messages/firebase` (חוקים + פונקציות).
8. **סיבוב מפתחות:** אם ה-APK של Navigation נבנה אי פעם עם מפתחות HERE/SIRI ב-`local.properties`, החלף אותם.
