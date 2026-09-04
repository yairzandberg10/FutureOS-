# הכנת מכשיר פיזי ל-FutureOS

מדריך שלב-אחר-שלב להכנת טלפון פיזי (מקלדת T9, ללא מסך מגע) להרצת כל חבילת האפליקציות של FutureOS.

## 1. דרישות למכשיר

- Android 12 (SDK 31) — עדיף גרסת מלאי (stock), לא ROM מותאם.
- מסך 640×960px, מקלדת מספרים (T9) + `*`/`#` + D-pad לניווט.
- **ללא Wi-Fi** — Settings בפרויקט בנוי בהנחה שלחומרת היעד אין Wi-Fi (רק נתונים סלולריים/Bluetooth). אם המכשיר שבידיך כן תומך ב-Wi-Fi זה לא באג, אך מסך "חיבורים" ב-Settings לא יציג אפשרות Wi-Fi.
- **Root** — חובה. `FutureUI`, `Settings` ו-`Terminal` מריצים פקודות דרך `su` (למשל toggles שאין להם API ציבורי, auto-grant של הרשאות, reboot/shutdown). בלי root האפליקציות האלה עדיין יעבדו חלקית אבל לא באופן מלא.

## 2. הכנת המחשב (כלים)

```bash
# Android platform-tools (adb/fastboot) — אם עוד אין
winget install Google.PlatformTools
adb version
```

ודא ש-Android Studio / Gradle מותקנים לבניית כל אפליקציה (`./gradlew assembleDebug` בכל תיקיית אפליקציה). רוב האפליקציות מתקמפלות ב-Java 11, אבל ל-`Fitness`, `Music`, `Navigation` ו-`notes` יש `sourceCompatibility`/`jvmTarget` של Java 21 — יש להתקין JDK 21 (או ערכת JDKs תואמת דרך Android Studio) כדי שכל 23 האפליקציות ייבנו, לא רק JDK 17.

## 3. הפעלת מצב מפתח + USB debugging במכשיר

1. הגדרות → אודות הטלפון → הקש 7 פעמים על "מספר build" (Build number) — פותח "אפשרויות מפתחים".
2. הגדרות → אפשרויות מפתחים → הפעל **ניפוי באגים ב-USB** (USB debugging).
3. חבר את הטלפון ל-USB, אשר את בקשת ה-RSA fingerprint שתופיע במכשיר.
4. ודא חיבור:

```bash
adb devices
```

צריך להופיע המכשיר עם `device` (לא `unauthorized`).

## 4. Root (Magisk)

הפרויקט לא נועל שיטת רוט ספציפית — Magisk הוא הדרך הסטנדרטית למכשירי Android 12 עם bootloader פתוח:

1. פתח את ה-bootloader (משתנה לפי יצרן; לרוב `fastboot flashing unlock` אחרי הפעלת "OEM unlocking" באפשרויות מפתחים — **פעולה זו מוחקת את כל הנתונים במכשיר**).
2. חלץ את ה-`boot.img` המקורי מקובץ ה-firmware הרשמי של המכשיר.
3. התקן את אפליקציית Magisk, פאץ' את ה-`boot.img` דרכה (Install → Select and Patch a File).
4. `fastboot flash boot magisk_patched.img`
5. אתחל, פתח את Magisk ואשר ש-root פעיל: `adb shell su -c id` אמור להחזיר `uid=0(root)`.

> אם המכשיר כבר מגיע רוט (למשל דרך יצרן/דגם dev), דלג על השלב הזה — רק ודא ש-`adb shell su -c id` עובד.

## 5. בניית כל האפליקציות

כל אפליקציה היא פרויקט Gradle עצמאי בתיקייה משלה. **כל 23 האפליקציות** תלויות במודול המשותף `SharedKeypadNav/` דרך נתיב יחסי (D-pad focus, T9, ומאז איחוד מערכת העיצוב — גם `FutureTheme`/`ThemeClient` המשותפים) — **אל תעביר תיקיית אפליקציה בודדת החוצה מהריפו בלי שאר התיקיות ובלי `SharedKeypadNav/`**, הבנייה תיכשל.

מה-root של הריפו, יש סקריפט אחד (`build-all.sh`) שמכסה את כל 23 האפליקציות - מקור אמת יחיד לרשימה, במקום לשמר אותה בשתי לולאות נפרדות (בנייה + התקנה) שעלולות להתפצל:

```bash
./build-all.sh              # assembleDebug בכל 23 האפליקציות, אחת אחרי השנייה
```

כל APK יימצא ב-`<App>/app/build/outputs/apk/debug/app-debug.apk`. הסקריפט בונה אפליקציה-אחר-אפליקציה בכוונה (לא במקביל) - בנייה מקבילית של כמה אפליקציות שכולן תלויות ב-`SharedKeypadNav/` עלולה להיתקל בנעילת קובץ על ה-`build/` המשותף שלו (ר' `CHANGELOG.md`).

## 6. התקנת כל ה-APK-ים

```bash
./build-all.sh --install    # בונה ומתקינה (adb install -r) כל אפליקציה שנבנתה בהצלחה
```

## 7. מאגר הטקסטים של Sfarim (sefaria.db, ~1.55GB)

הקובץ לא נכנס ל-git (מעל 100MB, ונבנה מחדש מהמקור דרך `Sfarim/tools/build_library.py` — ר' `Sfarim/README.md`), אבל אם כבר בנית אותו הוא קיים ב-`Sfarim/tools/output/sefaria.db`. יש לדחוף אותו למכשיר בנפרד (USB, לא Wi-Fi — כאמור אין Wi-Fi ביעד):

```bash
adb shell mkdir -p /sdcard/Android/data/com.future.sfarim/files
adb push "Sfarim/tools/output/sefaria.db" /sdcard/Android/data/com.future.sfarim/files/sefaria.db
```

> ודאי מול הקוד (`Sfarim/app/src/main/java/.../LibraryDatabase.kt` או שקול) מה בדיוק הנתיב שהאפליקציה מצפה לו על המכשיר — אם הוא שונה מהדוגמה לעיל, דחפי לשם. הפעלה ראשונה ללא ה-DB תיכשל/תיפול לספרייה ריקה.

## 8. הגדרות ברירת-מחדל והרשאות אחרי ההתקנה

זה השלב שהכי קל לפספס בו משהו. עברי אפליקציה-אפליקציה:

| אפליקציה | פעולה נדרשת |
|---|---|
| **FutureUI** | הגדרות → נגישות (Accessibility) → הפעילי את שירותי ה-Accessibility של FutureUI (status bar / lock screen / control center). אשרי הרשאת "הצג מעל אפליקציות אחרות" (`SYSTEM_ALERT_WINDOW`) ו-Notification access (עבור notification center). אותו שירות Accessibility של מסך הנעילה (`LockScreenAccessibilityService`) גם אחראי על דאבל-קליק גלובלי על OK שפותח את Assistant (עוזר קולי) מכל מסך במערכת - בלי להפעיל את השירות הזה, הקיצור הגלובלי לא יעבוד (Assistant עדיין נפתחת כרגיל כאפליקציה עצמאית מהלאנצ'ר). |
| **FutureLauncher** | לחצי על כפתור Home → בחרי FutureLauncher → "תמיד" (set as default launcher). |
| **Keyboard** | פתחי את האפליקציה → המסך יוביל אוטומטית ל-Settings → Input methods → הפעילי את מקלדת FutureOS ובחרי אותה כברירת מחדל. באותו מסך יש גם כפתור למתן הרשאת `RECORD_AUDIO` (ל-hold-0 voice input). |
| **dialer** | פתחי את האפליקציה — היא תבקש (`RoleManager.ROLE_DIALER`) להיות ברירת המחדל לחיוג; אשרי בדיאלוג המערכת. |
| **Messages** | פתחי את האפליקציה — היא תבקש להיות ברירת מחדל ל-SMS; אשרי בדיאלוג המערכת. |
| **Files** | תבקש הרשאת "All files access" (`MANAGE_EXTERNAL_STORAGE`) — אשרי דרך המסך שהיא פותחת. |
| **Settings** | בפעם הראשונה שהיא רצה, אם ה-root תקין, היא אמורה לבצע auto-grant לחלק מההרשאות שלה (`WRITE_SETTINGS` וכו') לבד — בדקי ב-logcat הודעת "Permission granted via Root". אם לא, תני ידנית: `adb shell pm grant com.future.settings android.permission.WRITE_SETTINGS` וכן הלאה לפי הצורך. |
| **Terminal** | דורש ש-`su` יהיה זמין ושהמשתמש יאשר בקשת root (חלון Magisk) בפעם הראשונה שהאפליקציה קוראת ל-`su -c`. |
| **Contact / Calendar** | ידרשו אישור בקשות הרשאה רגילות (contacts/calendar/location) בפעם הראשונה שנפתחות — Android 12 סטנדרטי, אין צורך בפעולה מיוחדת. |
| **Camera / Assistant** | ידרשו אישור הרשאת runtime רגילה (`CAMERA` / `RECORD_AUDIO`) בפעם הראשונה שנפתחות — דיאלוג מערכת סטנדרטי, אין צורך בפעולה מיוחדת. |
| **Remote** | `TRANSMIT_IR` היא הרשאה "רגילה" (לא dangerous) — ניתנת אוטומטית בהתקנה, בלי דיאלוג. עובד רק אם למכשיר הפיזי יש משדר אינפרא אדום (`ConsumerIrManager.hasIrEmitter()`); אם אין, האפליקציה תציג הודעה במקום לקרוס. |

## 9. בדיקת שפיות (smoke test)

1. אתחלי את המכשיר.
2. ודאי ש-status bar/lock screen של FutureUI מופיעים (במקום ה-System UI המקורי).
3. הקש Home → FutureLauncher עולה.
4. חייגי מספר → dialer בברירת מחדל, שיחה נכנסת בודקת גם.
5. שלחי SMS → Messages.
6. הקלד משהו בכל שדה טקסט → מקלדת T9 עולה, ניחושי מילים מופיעים.
7. פתחי Sfarim → ודאי שהטקסטים בעברית נטענים (לא מסך ריק) — מוודא שה-DB בנתיב הנכון.
8. Settings → שני איזשהו toggle (בהירות/עוצמת קול) → ודאי שהוא נשמר בפועל במכשיר.

## הערות

- דגם היעד המתועד בפרויקט הוא **Qin F22 Pro** — ר' `hardware/openscad/qin_f22_pro_case_keyboard.scad` (מידות מדודות: 151×61×11 מ"מ, מסך 3.54" ברזולוציית 640×960) ו-`hardware/onshape/feature_phone_motherboard.fs`. אלה קבצי CAD בלבד (לא קוד, לא חלק מאף build) — שימושיים כרפרנס למידות הפיזיות ולפריסת המקשים, אך לא מהווים תיעוד אלקטרוני מדויק (ר' ההערות בתוך אותם קבצים).
