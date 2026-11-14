# תוכנית: FutureOS כמערכת הפעלה סגורה מבוססת AOSP על Qin F22 Pro

**סטטוס:** תוכנית בלבד (2026-10-14). שום דבר לא נצרב ולא נבדק על המכשיר.
**מטרה:** במקום אוסף אפליקציות שרץ מעל האנדרואיד של היצרן (עם AccessibilityService ו-root), FutureOS יהיה ה-ROM עצמו. הבסיס הוא AOSP, עם הדרייברים של היצרן ל-MediaTek, ה-bootloader נעול במפתחות שלנו, ועדכונים יגיעו ב-OTA.

**המכשיר** (נמדד ב-ADB ב-25.9, ר' `hardware/HARDWARE_SPEC.md`): MT6768 (Helio P65/G85), 4GB RAM, 64GB, Android 12, מודם MediaTek LR12A עם IMS/VoLTE, מקלדת `mtk-kpd`, מצלמות 8MP+2MP, IR.

---

## 0. שורה תחתונה

| החלטה | המלצה |
|---|---|
| בסיס | **LineageOS 19.1 (Android 12L)**, בנוי כ-device tree מלא. לא GSI ולא AOSP נקי (סעיף 2) |
| BSP | שני מסלולים במקביל: חילוץ blobs מה-firmware של היצרן (מתחילים מיד) + בקשת קוד קרנל ב-GPL מ-Duoqin (סעיף 1) |
| ממשק מערכת | FutureUI מחליף את SystemUI בבנייה (`PRODUCT_PACKAGES` + overlay). FutureLauncher הופך ל-launcher היחיד. כל האפליקציות נכנסות כ-priv-app עם platform key. `su` יוצא מהמערכת (סעיף 3) |
| "סגור" | user build, AVB 2.0 עם מפתח שלנו, נעילה מחדש של ה-bootloader אחרי הצריבה, `ro.adb.secure=1` (סעיף 4) |
| מחשב לבנייה | **SSD חיצוני NVMe בגודל 1TB + WSL2** אם במחשב יש 32GB RAM. אחרת שרת ענן לפי שעה (סעיף 5) |
| זמן | כ-12-16 שבועות לגרסה יציבה, ומסלול ה-blobs לא תלוי בתשובה של Duoqin (סעיף 6) |

---

## 1. השגת ה-vendor / BSP

### 1א. חילוץ blobs (לא תלוי באף אחד, מתחילים ראשונים)
1. **השגת firmware רשמי מלא** (חבילת SP Flash Tool עם `MT6768_Android_scatter.txt`) לאותה גרסת build שרצה על המכשיר. אם אין חבילה רשמית: dump מהמכשיר עם `mtkclient` (`python mtk rl out/`) של `boot`, `vendor_boot` (אם קיים), `super`, `vbmeta*`, `dtbo`, `lk`, `preloader`, `md1img`, `nvram`, `nvdata`, `persist`, `proinfo`. **הצעד הזה נוגע בטלפון, ולכן הוא מחכה ל-phone.lock ולאישור של המשתמש.**
   - **גיבוי חובה לפני כל דבר אחר:** `nvram`, `nvdata`, `nvcfg`, `persist`, `proinfo`, `protect1/2`, `md1img`. שם נמצאים IMEI, הכיול של הרדיו וכתובות MAC. אם הם הולכים לאיבוד, אין רשת. שומרים שני עותקים מחוץ למחשב.
2. **פירוק:** `lpunpack super.img` ל-`system`, `vendor`, `product`, `system_ext`, `odm`. אחר כך `extract-files.sh` / `proprietary-files.txt` בסגנון LineageOS.
3. **device tree:** מתחילים מ-tree קיים של MT6768/MT6769 (משפחת Redmi 9 `lancelot` / Redmi Note 9 `merlinx`, שיש להם trees רשמיים של LineageOS ומשתפים פלטפורמה), ומחליפים את מה שספציפי לטלפון: מקלדת, פאנל 640×960, חיישנים, IR, מצלמות.
4. **קרנל זמני:** `prebuilt` מתוך ה-`boot.img` של היצרן (TARGET_PREBUILT_KERNEL + DTB). זה עובד לאב-טיפוס, אבל אי אפשר לתקן ככה דרייברים, וזה לא מספיק למוצר.

### 1ב. בקשת קוד הקרנל מ-Duoqin (GPLv2)
לפי GPLv2 §3, מי שמפיץ בינארי של קרנל Linux חייב לספק את קוד המקור המלא שלו, כולל הסקריפטים לבנייה. **מה לבקש, בדיוק:**

> נושא: GPL source code request - Qin F22 Pro kernel
>
> Under GPLv2 section 3, please provide the complete corresponding source code for the Linux kernel shipped on the **Qin F22 Pro** (MediaTek MT6768), build number: `<ro.build.display.id מהמכשיר>`, kernel version: `<uname -r / Settings → About → Kernel version>`.
>
> Please include:
> 1. The full kernel source tree, exactly as built for this firmware (not a generic MediaTek tree), including all out-of-tree drivers that link into the kernel: keypad (`mtk-kpd`), display panel/LCM, touch (`mtk-tpd`), camera sensor drivers (imgsensor), lens/AF, flash LED, charger/fuel gauge, accdet, IR (consumerir), sensors hub, Wi-Fi/BT (connectivity `wmt`/`gen4m`), and GPU kernel module.
> 2. The exact defconfig used (`arch/arm64/configs/<name>_defconfig`) and any config fragments.
> 3. Device-tree sources (`.dts` / `.dtsi`) for this board, including the `dtbo` overlay.
> 4. Build scripts and the toolchain version used (clang/gcc version and flags).
> 5. Source for any other GPL components in the firmware (e.g. U-Boot/LK if GPL-licensed, busybox, toybox if modified).
>
> Delivery as a downloadable archive or a public Git repository is fine. Thank you.

**אחרי שזה מגיע:** מוודאים שהקוד מתקמפל ונותן `Image.gz` שעולה עם ה-ramdisk של היצרן, לפני שמכניסים אותו לבנייה.

**מה GPL לא מכסה (ולכן לא מבקשים בתור "חובה" אלא בבקשה מנומסת נפרדת):** HALs ב-userspace (camera HAL, RIL/`mtkfusionrild`, IMS), firmware של המודם, ו-`preloader`/`lk` החתומים. את כל אלה לוקחים כ-blobs (1א). כדאי לשאול בנפרד גם:
- האם ה-bootloader נעול ב-`seccfg` ואיך פותחים אותו רשמית (`fastboot flashing unlock`?).
- האם יש להם מפתח OEM לחתימת `lk`/`preloader`. אם לא, אין לנו Root of Trust מלא (סעיף 4).

### 1ג. דרייברים שחשוב לבדוק ראשונים (סדר סיכון)
מודם/IMS ← מצלמה ← מקלדת (keylayout + `mtk-kpd`) ← פאנל ← חיישנים ← IR ← Wi-Fi/BT ← GPS.

---

## 2. הבסיס: AOSP 12 נקי, LineageOS או GSI

| | AOSP 12 נקי | **LineageOS 19.1** | GSI (phh/TrebleDroid) |
|---|---|---|---|
| תמיכה ב-MTK | אין. צריך לכתוב כל patch בעצמנו | יש: trees של MT6768/9, patches של MTK, IMS ל-MTK נשמר ב-tree | חלקית. VoLTE ב-MTK ידוע כבעייתי ב-GSI |
| שליטה במערכת | מלאה | מלאה (קוד מקור) | אין: ה-vendor של היצרן נשאר כמו שהוא, ולא שולטים ב-boot/vendor |
| "סגור" (AVB + נעילה) | כן | כן | **לא באמת**: vendor ו-boot נשארים של היצרן |
| הסרת SystemUI | כן | כן | קשה (overlay בלבד) |
| עבודה | הכי הרבה | בינונית | הכי מעט |
| עדכוני אבטחה | בעצמנו | backports של Lineage | תלוי בבונה ה-GSI |

**המלצה: LineageOS 19.1 כ-device build מלא.** בפועל זה AOSP 12 עם שכבת תאימות ל-MediaTek שאחרים כבר כתבו ובדקו. נשארים על אותה גרסת Android כמו ה-vendor של היצרן (12), כדי שה-blobs יעבדו בלי shims.
- את הרכיבים של Lineage שלא צריך מסירים: Trebuchet, Jelly, LineageParts, Updater, Recorder, Etar, ו-SystemUI עצמו.
- **GSI משמש רק ככלי בשבוע 1-2**, בלי לבנות כלום: מורידים GSI מוכן של Android 12 וצורבים אותו כדי לראות מה עובד עם ה-vendor הקיים. על דגם F21 Pro (אח של המכשיר) תועד מסלול של unlock עם mtkclient + LineageOS GSI, וזה סימן טוב. אבל הוא דורש צריבה, ולכן מחכה לאישור.
- AOSP נקי מתאים רק אם תהיה בעתיד חומרה משלנו עם BSP מלא מהספק (הדגמים ב-`hardware/MODELS_SPEC.md`).

---

## 3. FutureUI ו-FutureLauncher כחלק מהמערכת

### 3א. מבנה ב-tree
```
vendor/futureos/
  apps/<App>/Android.bp        # android_app_import ל-APK חתום, או בנייה מהקוד (סעיף 3ד)
  permissions/privapp-permissions-futureos.xml
  overlay/                      # config_systemUIServiceComponent, launcher, ניווט
  config/futureos.mk            # PRODUCT_PACKAGES + PRODUCT_PACKAGES_REMOVE
  sepolicy/
```
- אפליקציות מערכת: `privileged: true`, `certificate: "platform"`. אפליקציות בלי צורך בהרשאות signature: מפתח נפרד (סעיף 4). מותקנות ב-`/system_ext/priv-app` או `/product/priv-app`. **יש להן הרשאות מערכת בלי root.**
- `privapp-permissions-futureos.xml` מגדיר במפורש כל הרשאה signature|privileged לכל חבילה, אחרת האפליקציה לא עולה (`ro.control_privapp_permissions=enforce`).

### 3ב. FutureUI ← SystemUI אמיתי
היום: `StatusBarAccessibilityService` + `SYSTEM_ALERT_WINDOW` מציירים שכבה מעל SystemUI של היצרן. (`apps/SystemUI/` בריפו הוא עותק של FutureUI עם החבילה `com.android.sistemui` ולא SystemUI אמיתי.)

**שתי דרכים, וממליץ על (1):**
1. **FutureUI כ-SystemUI עצמאי:** מוחקים את `SystemUI` מ-`PRODUCT_PACKAGES`, ומשנים ב-overlay של framework את `config_systemUIServiceComponent` ל-`com.future.futureui/.SystemUIService`. FutureUI מקבל `STATUS_BAR_SERVICE`, `STATUS_BAR`, `BIND_NOTIFICATION_LISTENER` (במקום accessibility), ו-`INTERNAL_SYSTEM_WINDOW` כדי להוסיף חלונות `TYPE_STATUS_BAR` / `TYPE_NOTIFICATION_SHADE`, ונרשם דרך `IStatusBarService.registerStatusBar()`.
   - **מה FutureUI יצטרך לממש בעצמו, כי SystemUI עושה את זה היום:** מסך נעילה (keyguard, כולל PIN/סיסמה, דרך `KeyguardService`), דיאלוג כיבוי (`GlobalActions`), עוצמת קול, התראות heads-up, ניהול סוללה נמוכה, דיאלוג USB, דיאלוגי הרשאות (`PermissionController` נשאר), screenshot, ו-recents (או ביטול).
   - זה העבודה הכבדה ביותר בפרויקט: כ-4-6 שבועות.
2. **fork של SystemUI של AOSP** ששומר את keyguard ואת השירותים, ומחליף רק את ה-views ב-Compose של FutureUI. פחות עבודה בהתחלה, אבל קוד SystemUI ענק וקשה לתחזק.

בכל מקרה: מוחקים את `StatusBarAccessibilityService`, את `SYSTEM_ALERT_WINDOW` ואת `PACKAGE_USAGE_STATS` (הרשאות מערכת מחליפות אותם). את ההרשאה `com.future.futureui.permission.SYSTEM_SETTINGS` משנים ל-`signature`.

### 3ג. FutureLauncher ← ה-launcher של המערכת
- מסירים את Launcher3/Trebuchet מ-`PRODUCT_PACKAGES`, ו-FutureLauncher הוא ה-`HOME` היחיד, אז אין בורר. מוסיפים `LAUNCHER_PROVIDERS` ל-`overrides` של Launcher3 כדי שלא יותקן חזרה.
- מקבל `android.permission.STATUS_BAR` / `REORDER_TASKS` / `MANAGE_ACTIVITY_TASKS` אם צריך recents.
- מגדירים ב-overlay את `config_recentsComponentName` לרכיב שלנו, או מבטלים recents.

### 3ד. מה משתנה ב-root / Terminal / Settings
`apps/SharedKeypadNav/.../root/RootShell.kt` מריץ היום `su`. **במערכת סגורה אין `su`** (user build). כל קריאה מוחלפת ב-API של המערכת:

| איפה | היום (root) | במערכת |
|---|---|---|
| Settings `SystemInteractor` | `am force-stop` | `ActivityManager.forceStopPackage()` (`FORCE_STOP_PACKAGES`) |
| | `pm clear` | `PackageManager.clearApplicationUserData()` (`CLEAR_APP_USER_DATA`) |
| | `pm revoke` | `PackageManager.revokeRuntimePermission()` (`REVOKE_RUNTIME_PERMISSIONS`) |
| | `settings put system system_locales` | `LocalePicker.updateLocales()` / `ActivityManager.updatePersistentConfiguration()` (`CHANGE_CONFIGURATION`) |
| | `appops get/set PLAY_AUDIO` | `AppOpsManager.setMode()` (`MANAGE_APP_OPS_MODES`) |
| FutureUI `ControlManager`/`GridControls` | `settings get/put` | `Settings.Global/Secure/System` ישירות (`WRITE_SECURE_SETTINGS`) |
| | מצבי רדיו | `WifiManager`, `BluetoothAdapter`, `ConnectivityManager.setAirplaneMode()` (`NETWORK_SETTINGS`) |
| Terminal `ShellSession` | `su -c` | **מצב משתמש:** `sh` רגיל עם UID של האפליקציה. **מצב מפתח:** מבוטל ב-user build. אפשר להשאיר אותו רק ב-userdebug, מאחורי דגל `ro.debuggable` |

- `RootShell` נשאר רק ל-fallback בבניית `eng`/`userdebug`, וב-user build מחזיר כישלון במקום לנסות `su`.
- מוסיפים כלל ל-`security/security-check.py`: אין `"su"` ואין `Runtime.exec` ב-user build.
- **SELinux:** האפליקציות רצות ב-`platform_app` / `priv_app`. גישה לקבצי sysfs (תאורת מקלדת, פלאש, IR) דורשת כללי `sepolicy` ב-`vendor/futureos/sepolicy`. שום דבר לא רץ ב-permissive.

### 3ה. ניקוי
- מוחקים מה-ROM: GMS (אין כבר, יש וריאנט `nav-nogms`), אפליקציות של היצרן, ו-AOSP apps כפולות (Dialer, Messaging, Contacts, Camera2, Gallery2, Calculator, DeskClock, Music, Calendar) במקום כל אחת יש אפליקציית Future.
- חייגן ו-SMS ברירת מחדל: מגדירים ב-`config_defaultDialer` / `config_defaultSms`. זה פותר את החסם "חייגן/SMS ברירת מחדל" שבלוח התיאום.

### 3ו. Firebase ו-ML Kit במערכת בלי GMS (דוח בלבד, 16.10)
נבדק ב-`build.gradle.kts`, ב-`libs.versions.toml` ובקוד. ב-ROM לא יהיו Google Play Services.

| אפליקציה | ספרייה / קריאה | בלי Play Services? | חלופה |
|---|---|---|---|
| **Messages** | `firebase-auth` (`FirebaseAuth`) | עובד: REST של Identity Toolkit. אימות טלפון ו-SafetyNet/Play Integrity לא יעבדו | נשאר, או אסימון מהשרת המשותף של "מנוע עדכונים ומשוב" |
| | `firebase-firestore`, `firebase-storage` | עובד (gRPC/HTTP ישיר) | נשאר, או API בשרת המשותף |
| | `firebase-messaging` (`ChatPushService`, `FirebaseMessaging.getInstance`) | **לא עובד.** FCM דורש GMS, ולכן לא יגיעו push | **חיבור push משלנו:** WebSocket/long-poll לשרת המשותף מתוך שירות מערכת (priv-app עם `RECEIVE_BOOT_COMPLETED`, בלי הגבלות רקע), או UnifiedPush/ntfy. במערכת סגורה מותר שירות קבוע אחד לכל המערכת |
| | `kotlinx-coroutines-play-services` | עובד: רק await על Task | - |
| **Navigation** (`main`) | `firebase-auth`, `-config`, `-firestore`, `-functions`, `-storage` | Auth/Firestore/Storage/Functions עובדים. Remote Config עובד ברובו (משתמש ב-Firebase Installations, בלי GMS) | - |
| **Navigation** (`navigation-no-gms`) | בלי SDK: REST עם `FIREBASE_PROJECT_ID`/`API_KEY`, והשרת של FutureOS לניתובים ולסירי | עובד | **הענף הזה הוא הבסיס ל-ROM.** כש-SDK Firebase לא נכנס ל-main, הבעיה נעלמת |
| **Tools** | `mlkit:barcode-scanning` 17.3.0 | עובד: הגרסה ה-bundled (המודל בתוך ה-APK, לא `play-services-mlkit-*`) | - |
| | `mlkit:text-recognition` 16.0.1 | עובד: bundled (לטינית בלבד, בלי עברית) | לעברית: Tesseract (`tess-two`/`tesseract4android`) עם `heb.traineddata` |
| **Translate** | `mlkit:language-id` 17.0.6 | עובד: bundled | - |
| | `mlkit:translate` + `RemoteModelManager` (`TranslationEngine`) | **בסיכון:** המודלים יורדים בזמן ריצה מהשרתים של Google. יש דיווחים שההורדה נכשלת במכשירים בלי GMS. **לבדוק ב-M3** | (1) לארוז את מודלי en/he ב-ROM (`/product/etc/futureos/mlkit`), אם רישיון ML Kit מתיר. (2) מודל פתוח: Bergamot / Firefox Translations (MPL-2.0) או OPUS-MT. (3) תרגום דרך השרת המשותף |

**לסיכום:** רק שני דברים נשברים בוודאות: **FCM ב-Messages**, ובסבירות גבוהה **הורדת המודלים ב-Translate**. את כל השאר אפשר להשאיר. מסלול מומלץ: ערוץ push אחד של המערכת דרך השרת של "מנוע עדכונים ומשוב" (משרת גם הודעות צ'אט וגם התראות על OTA), מיזוג `navigation-no-gms` ל-main, ובחינה של Bergamot ל-Translate. אין שינויי קוד בשלב הזה.

---

## 4. חתימה, Verified Boot ונעילה

1. **מפתחות** (פעם אחת, במחשב לא מחובר; גיבוי מוצפן בשני מקומות):
   - `platform`, `shared`, `media`, `networkstack`, `releasekey`, `sdk_sandbox`, `bluetooth` דרך `development/tools/make_key`.
   - מפתח AVB: `openssl genrsa -out avb.pem 4096`, וממנו `avb_pkmd.bin`.
   - **אובדן מפתח = אי אפשר להוציא OTA למכשירים קיימים.** המפתחות לא נכנסים לגיט.
2. **בנייה:** `TARGET_BUILD_VARIANT=user`, ואז `sign_target_files_apks` + `ota_from_target_files` (חתימה של ה-target files, לא של build רגיל עם test-keys).
3. **AVB 2.0:** `BOARD_AVB_ENABLE := true`, `BOARD_AVB_KEY_PATH := avb.pem`. `vbmeta` עם `vbmeta_system` ל-system/product.
   - `BOARD_AVB_ROLLBACK_INDEX`: **מעלים אותו רק בגרסה שמתקנת בעיית אבטחה**, לא בכל גרסה. כשהמספר עולה, אי אפשר לחזור לבילד הקודם בשום דרך, גם לא עם מחיקת נתונים. חזרה אחורה רגילה נעשית roll-forward: בונים גרסה חדשה עם התוכן הישן (כמו ב-`docs/update-feedback-design.md`). **החלטה סופית: המשתמש.**
4. **נעילה מחדש:** ב-MTK, אחרי צריבה עם bootloader פתוח, צורבים את ה-`avb_pkmd.bin` למחיצה שה-`lk` קורא (`fastboot flash avb_custom_key` אם ה-lk של היצרן תומך בזה, כמו ב-Pixel). אחר כך `fastboot flashing lock`.
   - **סיכון גדול:** ה-lk של רוב יצרני ה-MTK הזולים **לא** תומך במפתח AVB מותאם. אם זה המצב, נעילה = brick, כי ה-lk יבדוק מול מפתח היצרן. **לבדוק לפני כל נעילה.** חלופה אם אין תמיכה: להשאיר פתוח עם אזהרת "orange state", או לבקש מ-Duoqin (1ב) lk חתום עם המפתח שלנו.
   - **בלי Root of Trust ב-preloader, מי שיש לו mtkclient (BROM exploit ב-MT6768) יכול לעקוף הכול.** "סגור" כאן = מוגן מפני משתמש רגיל ומפני שינויים מרחוק, לא מפני תוקף עם גישה פיזית. צריך לכתוב את זה במפורש במסמכים של המוצר.
5. **ADB:** `ro.adb.secure=1`, אפשרויות מפתח מוסתרות ב-Settings של FutureOS, ו-`ro.debuggable=0`.

### קשר ל-OTA (תואם עם "מנוע עדכונים ומשוב", 14.10. ר' `docs/update-feedback-design.md`)
- **פורמט:** `ota_from_target_files` (מלא, ואינקרמנטלי עם `-i old.zip new.zip`), חתום מקומית ב-`releasekey`. המכשיר מאמת מול `/system/etc/security/otacerts.zip`. השרת רק מפיץ ושומר hash. ל-manifest של השרת יש חתימת Ed25519 נפרדת, שחלה רק על ההוראה "איזו חבילה להתקין". שומרים כל target-files שיוצא.
- **A/B:** תלוי ב-dump (`ro.boot.slot_suffix`). אם A/B, משתמשים ב-`update_engine` דרך `UpdateEngine` API. אם non-A/B, משתמשים ב-`RecoverySystem.installPackage`, ואז **אין rollback אוטומטי**. אם המחיצות מאפשרות (dynamic partitions + snapshot), מומלץ **Virtual A/B**.
- **אישור אתחול:** המכשיר מדווח `boot_ok` לשרת רק אחרי ש-`update_verifier` סימן את ה-slot כמוצלח. כך ה-rollback index ב-AVB מתעדכן רק על בילד תקין.
- **APK בודד מחוץ ל-OTA: מאושר** (החלטת המשתמש ב-16.10, נמסרה מהצ'אט "מנוע עדכונים ומשוב"). כלל חובה: שה-`versionCode` ב-OTA הבא יהיה גבוה או שווה לכל APK שהופץ בנפרד. אחרת העותק ב-`/data` מסתיר את הגרסה שבמערכת, ו-OTA של חזרה אחורה לא ישפיע עליו.
- **צמצום השימוש ב-platform key:** אפליקציות שלא צריכות הרשאות signature של המערכת (למשל Calculator, Sfarim, Music) נחתמות במפתח נפרד (`futureos_apps`) ולא ב-`platform`. רק FutureUI, FutureLauncher, Settings, Dialer, Messages וכו' מקבלים platform.

---

## 5. מחשב לבנייה

**דרישות (LineageOS 19.1):** Linux x86-64 (Ubuntu 22.04), כ-300-400GB פנויים (קוד כ-150GB + out כ-150GB + ccache 50GB), 16GB RAM מינימום, **32GB מומלץ** (בפחות מזה, linking של framework ו-metalava נופל או לוקח שעות), 8+ ליבות. בנייה ראשונה: 3-6 שעות על 8 ליבות. בנייה חוזרת עם ccache: 20-60 דקות.

| אפשרות | עלות משוערת | יתרונות | חסרונות |
|---|---|---|---|
| **A. SSD חיצוני + WSL2** | NVMe 1TB + מארז USB 3.2/USB4: כ-₪350-550 חד-פעמי | בלי עלות חודשית, המפתחות נשארים אצלנו | **דורש 32GB RAM במחשב (לבדוק).** WSL2 על דיסק חיצוני: יוצרים VHDX על ה-SSD (`wsl --import` אל D:/E:), עובדים **רק** בתוך ה-ext4 של ה-VHDX ולא ב-`/mnt/e` (פי 10 יותר איטי). C כמעט מלא, אז שום דבר לא נשמר עליו |
| **B. שרת ענן לפי שעה** (למשל Hetzner Cloud CCX43/CCX53, AWS c6i.8xlarge spot) | כ-€0.2-0.5 לשעה + דיסק 500GB כ-€20-30 לחודש. 10-20 בניות בחודש: **כ-€40-100 לחודש** | מהיר, בלי חומרה | צריך לכבות ולשמור את הדיסק (volume). המפתחות לא לשים בענן: חותמים מקומית על target-files |
| **C. שרת ייעודי חודשי** (Hetzner AX52 / AX102) | כ-€60-120 לחודש | תמיד דלוק, ccache חם | עלות קבועה גם כשלא בונים |
| D. מחשב Linux ייעודי (Ryzen 7, 64GB, 2TB) | כ-₪4,000-5,500 חד-פעמי | הכי מהיר בטווח ארוך | השקעה גדולה לפני שיודעים שהפרויקט עובד |

**המלצה:** אם במחשב יש 32GB RAM, אז **A**. אם לא, אז **B** לחודשים הראשונים, ו-D רק אחרי שעבר אבן דרך M2. **המחירים הם הערכה ויש לבדוק אותם לפני שמזמינים. לא נפתח שום חשבון.**
- איפה שלא בונים: ה-dump של ה-firmware (כ-10GB) ושמירת target-files.
- **חתימה תמיד מקומית ובלי רשת**, גם אם הבנייה בענן.

---

## 6. לוח זמנים, אבני דרך וסיכונים

| שבוע | אבן דרך | תנאי מעבר |
|---|---|---|
| 0 | פנייה ל-Duoqin נשלחה. מחשב בנייה מוכן. **גיבוי nvram/nvdata/md1img** | גיבוי מאומת בשני מקומות |
| 1-2 | **M0:** dump מלא + ניתוח: A/B?, dynamic partitions?, lk תומך ב-avb_custom_key? GSI מוכן לבדיקה מהירה | יודעים מה עובד עם ה-vendor הקיים |
| 3-5 | **M1:** LineageOS 19.1 עולה עם device tree + קרנל prebuilt: מסך, מקלדת, ADB | boot ל-launcher, כל המקשים מגיבים |
| 6-8 | **M2:** רדיו: שיחות, SMS, נתונים. **VoLTE בישראל**. Wi-Fi/BT/GPS | שיחה של 10 דקות על VoLTE בשני ספקים |
| 7-12 | **M3:** FutureUI כ-SystemUI (מקביל ל-M2), FutureLauncher כ-HOME, priv-apps, הסרת root | אין `su` ואין AccessibilityService. כל 31 האפליקציות עוברות smoke test |
| 9-11 | **M4:** מצלמות, חיישנים, IR, תאורת מקלדת, פלאש, אודיו (כולל ACCDET) | עובר את רשימת הבדיקות של `device-qa` |
| 12-14 | **M5:** user build חתום, AVB, OTA ראשון (עם הצ'אט של העדכונים), בדיקת נעילה (רק אם lk תומך) | OTA אינקרמנטלי עובר בלי מחיקת נתונים |
| 15-16 | **M6:** יציבות: 7 ימי שימוש יומיומי, סוללה, CTS-lite לפי הצורך | אין קריסה של רדיו/SystemUI ב-7 ימים |

### סיכונים (לפי חומרה)
1. **VoLTE / IMS בישראל (גבוה מאוד).** בישראל רשתות 3G נסגרות, ולכן **בלי VoLTE אין שיחות בכלל**. ב-MTK צריך את חבילת ה-IMS של היצרן (`ImsService`, `mtkfusionrild`, `volte_*` daemons, קובצי MD config) ואת אותו `md1img`. צריך גם קונפיגורציית carrier לפרטנר, סלקום, פלאפון, הוט ו-019 (`carrierconfig` + `ecc_list` + APN). אם הספקים דורשים whitelist של IMEI/דגם, הדגם המקורי כבר רשום ולכן לא משנים את `ro.product.model` של ה-vendor. **בדיקה מוקדמת ב-M0 עם GSI.** מיטיגציה: לא מחליפים את `md1img`/vendor, נשארים על Android 12, ומעתיקים 1:1 את ה-props של IMS מה-ROM של היצרן.
2. **lk לא תומך במפתח AVB מותאם (גבוה).** אז אי אפשר לנעול. ר' סעיף 4.4. מיטיגציה: לבקש מ-Duoqin, או לשחרר עם bootloader פתוח ו-dm-verity פעיל.
3. **מצלמה (גבוה).** ה-HAL של MTK (`camerahalserver`, `libmtkcam*`) תלוי בגרסה בדיוק. צריך את אותו vendor ואת אותם libs של imgsensor בקרנל. מיטיגציה: לא לבנות HAL מחדש, להשתמש ב-blobs כמו שהם.
4. **אין קוד קרנל (בינוני).** אם Duoqin לא עונים בתוך 30 יום, ממשיכים עם prebuilt. מוצר אפשרי, אבל אי אפשר לתקן דרייברים. אפשר לפנות שוב, ובמקרה הצורך ל-Software Freedom Conservancy / gpl-violations.
5. **brick / אובדן IMEI (בינוני, ניתן למנוע).** גיבוי nvram לפני הכול. mtkclient מאפשר שחזור ב-BROM.
6. **FutureUI כ-SystemUI: keyguard ו-edge cases (בינוני).** מיטיגציה: דרך 3ב(2) כ-fallback.
7. **דיסק ו-RAM (בינוני):** סעיף 5.
8. **רישוי:** אסור להפיץ את ה-blobs של MediaTek בפומבי (בריפו ציבורי). צריך repo פרטי ל-`vendor/qin`.

---

## 7. מה לא נעשה עכשיו (דורש החלטה של המשתמש)
- כל צריבה, dump או unlock, כי הטלפון אצל צ'אט הבדיקות (`phone.lock`).
- שליחת הפנייה ל-Duoqin. הטקסט בסעיף 1ב מוכן.
- רכישת SSD או פתיחת חשבון ענן.
- ~~החלטה: האם מותר עדכון APK בודד מחוץ ל-OTA~~: **מאושר 16.10**, בכפוף לכלל ה-versionCode (סעיף 4).
- החלטה: rollback index יעלה רק בגרסאות אבטחה (מומלץ) או בכל גרסה (סעיף 4.3).
