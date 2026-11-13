# STATUS: אבטחה והרשאות (10-13) ופרטיות

תאריך: 2026-10-28. מקור: os-checklist-futureos-triage.md, שלוש הקבוצות (לבצע, קיים, אנדרואיד), קטגוריות 10-13 (סעיפים 361-520).

**לא נבנה ולא נבדק:** עץ ה-AOSP אינו קיים כאן וגם לא המכשיר. הכול כתוב לפי הידע על אנדרואיד 12. שמות משאבים ו-sepolicy חייבים אימות מול העץ (tools/check-overlay-names.sh ו-audit2allow).

נתיבים בטבלה יחסיים ל-`overlay/device/futureos/qin/`, חוץ מ-`tools/` שנמצא בשורש התיקייה הזו.

## סיכום
- סה"כ 112 סעיפים: קבוצת לבצע 21, קבוצת אנדרואיד 91, קבוצת קיים 0 (אין בה סעיפים בקטגוריות 10-13).
- להגדיר ב-overlay: 26. לאמת בלבד: 34. אין מה לעשות: 52. (סעיף עם "+ לאמת" נספר לפי ההחלטה הראשונה.)
- **להגדיר** = יש קובץ ב-overlay. **לאמת** = בלי שינוי, רק בדיקה במכשיר. **אין מה לעשות** = אנדרואיד 12 עושה את זה, או שזו משימת תהליך או מוצר מחוץ ל-overlay.

## הסיכונים הגדולים (לא ניתנים לפתרון ב-overlay)
1. **419/420/421:** אם ה-bootloader נשאר פתוח אין Secure Boot ו-Verified Boot אמיתיים. נעילה מחדש עם מפתח משלנו היא משימה ידנית גדולה.
2. **401-403:** אנדרואיד 12 ישן. לבדוק עד מתי AOSP מקבל תיקוני אבטחה ולתכנן cherry-pick. ליבת MediaTek נפרדת.
3. **384/516/517:** הגנות מנעול המסך והביומטריה תלויות ב-HAL של MediaTek (Gatekeeper/KeyMint). בלעדיהם הן לא אמיתיות.
4. **472/373/453/454:** פערים של אנדרואיד 12 עצמו (ביטול תעודות, רשת מקומית, Passkeys).
5. **נגישות במקשים:** דיאלוגי הרשאה, חיוויים ו-Privacy dashboard נבדקו רק תיאורטית. טלפון בלי מגע דורש ניווט D-pad בכל אחד מהם.

## טבלה

| # | סעיף | החלטה | קובץ | מה נשאר ידני |
|-|-|-|-|-|
| 361 | מודל הרשאות משתמשים מוגדר | אין מה לעשות | - | מודל UID של לינוקס ואנדרואיד |
| 362 | משתמש יומיומי ללא הרשאות מנהל כברירת מחדל | לאמת | futureos_security.mk, tools/verify-on-device.sh | בנייה בווריאנט user בלבד. אזהרה אוטומטית אם לא |
| 363 | הרמת הרשאות מפורשת (sudo / UAC) | לאמת | tools/verify-on-device.sh | אין su ואין root ב-user build |
| 364 | בקשת אישור לפעולות רגישות | לאמת | tools/verify-on-device.sh | בדיקה במכשיר שדיאלוגי ההרשאה ואישורי פעולות רגישות נגישים במקשים (D-pad ו-OK) בלי מגע |
| 365 | בקרת גישה חובה (SELinux / AppArmor או מקביל) | להגדיר + לאמת | BoardConfigSecurity.mk | להריץ getenforce. לא להשאיר permissive בשום שלב של הפורט (בעיקר ב-bring-up) |
| 366 | Sandbox לאפליקציות | אין מה לעשות | - | App sandbox ו-SELinux של אנדרואיד |
| 367 | הרשאת גישה למצלמה | לאמת | tools/verify-on-device.sh | דיאלוג ההרשאה נגיש במקשים |
| 368 | הרשאת גישה למיקרופון | לאמת | tools/verify-on-device.sh | כמו 367 |
| 369 | הרשאת גישה למיקום | לאמת | tools/verify-on-device.sh | כמו 367, כולל רק בזמן השימוש ובחירת מיקום מקורב |
| 370 | הרשאת גישה לאנשי קשר וליומן | לאמת | tools/verify-on-device.sh | כמו 367 |
| 371 | הרשאת גישה לקבצים ותיקיות | אין מה לעשות | - | Scoped storage של אנדרואיד 12 |
| 372 | הרשאה להקלטת מסך | לאמת | tools/verify-on-device.sh | דיאלוג MediaProjection נגיש במקשים ויש חיווי בזמן הקלטה |
| 373 | הרשאה לגישה לרשת המקומית | אין מה לעשות | - | אין הרשאת רשת מקומית באנדרואיד 12 (קיימת בגרסאות מאוחרות). פער מוצהר, לא ניתן לסגור ב-overlay |
| 374 | הרשאה לגישה ל-Bluetooth | לאמת | tools/verify-on-device.sh | הרשאות BLUETOOTH_CONNECT ו-SCAN מוצגות לאפליקציות שמכוונות ל-API 31 |
| 375 | חיווי ויזואלי כשהמצלמה פעילה | להגדיר + לאמת | security/futureos-privacy.sh, tools/verify-on-device.sh | לוודא שצ׳יפ המצלמה/מיקרופון של SystemUI מוצג ב-640x960 ושלא מוסתר. אין נורית חומרה |
| 376 | חיווי ויזואלי כשהמיקרופון פעיל | להגדיר + לאמת | security/futureos-privacy.sh, tools/verify-on-device.sh | כמו 375 |
| 377 | ביטול הרשאה בכל רגע | אין מה לעשות | - | ביטול הרשאה בהגדרות האפליקציה |
| 378 | הרשאה חד-פעמית | לאמת | tools/verify-on-device.sh | אפשרות רק הפעם נגישה במקשים |
| 380 | יומן שימוש בהרשאות | לאמת | tools/verify-on-device.sh | לוודא ש-Privacy dashboard קיים בהגדרות ונגיש במקשים. אם חסר: אפליקציה שקוראת AppOps history |
| 381 | הגבלת גישה לקבצי מערכת | אין מה לעשות | - | SELinux ו-Scoped storage |
| 382 | מחיצת מערכת לקריאה בלבד (Immutable System) | להגדיר + לאמת | BoardConfigSecurity.mk | AVB ו-dm-verity. אם ה-bootloader נשאר פתוח השרשרת שבורה (ראו 419) |
| 383 | נעילת מסך אוטומטית | לאמת | tools/verify-on-device.sh | ברירת המחדל באנדרואיד 12 היא נעילה 5 שניות אחרי כיבוי מסך, אבל רק כשמוגדר PIN (סעיף 528). לא ניתן לכפות PIN ב-overlay |
| 384 | נעילה אחרי ניסיונות כניסה כושלים | לאמת | tools/verify-on-device.sh | השהיות אחרי כשלונות תלויות ב-Gatekeeper HAL של MediaTek. בלעדיו אין הגנה |
| 385 | נעילה ומחיקה מרחוק במקרה של גניבה | אין מה לעשות | - | נעילה/מחיקה מרחוק דורשת שירות ענן וחשבון, בניגוד ל-485. החלטת מוצר: ללא תכונה |
| 391 | הגבלת התקנת אפליקציות לא מאומתות | אין מה לעשות (+לאמת) | - | הרשאת התקנה ממקור לא מוכר לכל אפליקציה קיימת באנדרואיד. לבדוק שהיא נגישה במקשים |
| 392 | אימות חתימה של אפליקציות | אין מה לעשות | - | אנדרואיד דורש חתימה ולא מתקין APK לא חתום |
| 393 | מאגר אפליקציות מאומת | אין מה לעשות | - | אין חנות. החלטת מוצר: איזה מקור מאומת לספק (מחוץ ל-overlay) |
| 394 | חסימת אפליקציות לא חתומות עם אפשרות עקיפה מודעת | אין מה לעשות | - | מובנה: אין התקנה של APK לא חתום, ואין עקיפה אלא במצב מפתחים |
| 395 | הגנה מפני Keyloggers בין אפליקציות | אין מה לעשות | - | אפליקציה ברקע לא מקבלת קלט. חריג: שירותי נגישות, שדורשים אישור מפורש |
| 397 | מניעת הזרקת קוד בין תהליכים | אין מה לעשות (+לאמת) | tools/verify-on-device.sh | SELinux חוסם ptrace באפליקציות. תקף רק ב-user build |
| 398 | גישה מבוקרת ללוח הגזירים | להגדיר + לאמת | overlay/frameworks/base/core/res/res/values/config.xml, security/futureos-privacy.sh | הצגת הודעה בקריאת לוח. לוודא ששם המשאב קיים בעץ (tools/check-overlay-names.sh) |
| 400 | עקרון ההרשאה המינימלית בכל שירותי המערכת | אין מה לעשות | sepolicy/system_ext/private/* | מדיניות ה-SELinux שלנו כתובה מינימלית. neverallow נאכף בבנייה |
| 401 | עדכוני אבטחה סדירים | אין מה לעשות | - | תהליך, לא קונפיגורציה. אנדרואיד 12 ישן: לבדוק מול עלוני האבטחה עד מתי AOSP מקבל תיקונים ולתכנן cherry-pick מענפים חדשים. ליבת MediaTek היא סיכון נפרד |
| 402 | עדכוני אבטחה אוטומטיים | אין מה לעשות | - | תלוי בשרת OTA ובעדכוני A/B (סעיף 817). מחוץ לתחום זה |
| 403 | תיקון חולשות קריטיות תוך ימים | אין מה לעשות | - | תהליך. יעד SLA נקבע על ידינו |
| 404 | פרסום הודעות אבטחה עם מספרי CVE | אין מה לעשות | - | תהליך פרסום: מקום לעלונים וקבלת מספרי CVE |
| 411 | Control Flow Integrity | אין מה לעשות | - | CFI מופעל כברירת מחדל בחלק מרכיבי אנדרואיד. ליבת MediaTek מחוץ ל-overlay |
| 412 | Shadow Stacks בחומרה תומכת | אין מה לעשות | - | ShadowCallStack ברירת מחדל ב-arm64. PAC/BTI תלוי בשבב |
| 413 | קבצי מערכת מקומפלים כ-PIE | אין מה לעשות | - | כל הקבצים הרצים הם PIE |
| 414 | RELRO ו-FORTIFY_SOURCE | אין מה לעשות | - | RELRO ו-FORTIFY ברירת מחדל ב-Soong |
| 415 | שימוש בשפות בטוחות-זיכרון בקוד חדש | אין מה לעשות | - | מדיניות לקוד חדש של FutureOS: Kotlin או Rust. אין כאן קוד חדש |
| 417 | חסימת גישה ישירה לזיכרון הפיזי | לאמת | tools/verify-on-device.sh | ls /dev/mem וגם grep DEVMEM בקונפיג הליבה (config.gz) |
| 418 | הסתרת כתובות ליבה ממשתמשים רגילים | להגדיר + לאמת | security/init.futureos.security.rc, tools/verify-on-device.sh | לבדוק את הערכים. perf_event_paranoid מנוהל על ידי AOSP דרך security.perf_harden |
| 419 | שרשרת Secure Boot מלאה | לאמת | BoardConfigSecurity.mk, tools/verify-on-device.sh | בלי פתיחת bootloader יציבה או נעילה מחדש עם מפתח משלנו אין Secure Boot אמיתי. החלטה ופעולה ידניות |
| 420 | Verified Boot למחיצת המערכת | להגדיר + לאמת | BoardConfigSecurity.mk | AVB. אם unlocked המצב כתום ואין אימות. נעילה מחדש עם BOARD_AVB_KEY_PATH היא ידנית |
| 421 | אימות שלמות קבצי המערכת | לאמת | BoardConfigSecurity.mk, tools/verify-on-device.sh | dm-verity/erofs על מחיצות המערכת |
| 425 | TPM 2.0 | אין מה לעשות (+לאמת) | tools/verify-on-device.sh | אין TPM באנדרואיד. המקבילה היא KeyMint/Gatekeeper בחומרה המאובטחת. לבדוק שקיימים |
| 428 | שירותים לא נחוצים כבויים כברירת מחדל | לאמת | tools/verify-on-device.sh | רשימת שירותים רצים ו-pm list ללא חבילות מיותרות |
| 429 | מינימום פורטים פתוחים | לאמת | tools/verify-on-device.sh | netstat/ss: אין פורט מאזין. adbd כבוי ב-user build עד הפעלה |
| 430 | ביטול פרוטוקולים מיושנים (SMBv1, TLS 1.0) | לאמת | tools/verify-on-device.sh | לבדוק בפועל אם TLS 1.0/1.1 כבויים ב-Conscrypt של 12. אם לא, תיקון ב-external/conscrypt ולא ב-overlay. SMBv1 לא קיים |
| 431 | קונפיגורציה בטוחה כברירת מחדל | להגדיר | כל הקבצים | החבילה כולה: SELinux enforcing, privacy defaults, אין שירותים נוספים |
| 441 | ספריית קריפטוגרפיה מערכתית מתוחזקת | אין מה לעשות | security/crypto-policy.he.md | BoringSSL ו-Conscrypt |
| 442 | AES-GCM עם האצת חומרה | לאמת | fstab-encryption.snippet.txt, tools/verify-on-device.sh | לבדוק Features: aes בשבב. אם חסר, Adiantum ב-fstab |
| 443 | ChaCha20-Poly1305 | אין מה לעשות | - | BoringSSL כולל ChaCha20-Poly1305 |
| 444 | SHA-2 ו-SHA-3 | אין מה לעשות | - | SHA-2 קיים. SHA-3 קיים ב-BouncyCastle ולא בהכרח ב-BoringSSL |
| 445 | RSA ו-ECC | אין מה לעשות | - | קיים בספריות המערכת ובחומרה המאובטחת |
| 446 | Ed25519 | אין מה לעשות | - | Ed25519 ב-Keystore ספציפי לגרסה ולחומרה. לבדוק רק אם אפליקציה תדרוש |
| 448 | מאגר תעודות מערכתי | אין מה לעשות | - | /system/etc/security/cacerts |
| 449 | עדכון שוטף של רשימת רשויות התעודה | אין מה לעשות | security/crypto-policy.he.md | הרשימה מתעדכנת רק בעדכון מערכת. בלי OTA היא קפואה. קשור ל-402 |
| 450 | ניהול תעודות משתמש | לאמת | tools/verify-on-device.sh | הגדרות > אבטחה > התקנת תעודה נגישה במקשים |
| 451 | מנהל סיסמאות מערכתי (Keychain או מקביל) | אין מה לעשות | - | אין מנהל סיסמאות מערכתי בלי GMS. החלטת מוצר: אפליקציה חיצונית פתוחה |
| 452 | הצפנת סודות נפרדת לכל משתמש | אין מה לעשות | - | מפתחות CE נפרדים לכל משתמש באנדרואיד |
| 453 | Passkeys | אין מה לעשות | - | Passkeys דורשים רכיבים שאינם ב-AOSP 12. לא ניתן ב-overlay |
| 454 | WebAuthn | אין מה לעשות | - | WebAuthn תלוי בדפדפן וב-FIDO. לא ניתן ב-overlay |
| 455 | הצפנת דיסק מלאה כברירת מחדל | להגדיר + לאמת | fstab-encryption.snippet.txt, BoardConfigSecurity.mk | למזג את השורה ל-fstab האמיתי של Qin ולבדוק ro.crypto.state=encrypted. בלי PIN המפתחות מוגנים רק בסיסמת ברירת מחדל |
| 456 | הצפנת Swap | לאמת | tools/verify-on-device.sh | לוודא שאין swapfile על flash (zram בלבד) |
| 457 | הצפנת קובץ Hibernation | אין מה לעשות | - | אין Hibernate באנדרואיד |
| 458 | הצפנת גיבויים | אין מה לעשות | security/crypto-policy.he.md | אין גיבוי ענן ו-adb backup מוגבל. כל גיבוי עתידי חייב להיות מוצפן |
| 459 | מחיקה קריפטוגרפית | לאמת | fstab-encryption.snippet.txt, tools/verify-on-device.sh | מחיקה קריפטוגרפית באיפוס יצרן דורשת /metadata ו-keydirectory. לבדוק איפוס אמיתי |
| 460 | מחולל מספרים אקראיים מאובטח למפתחים | אין מה לעשות | - | SecureRandom ו-getrandom |
| 466 | חסימת אלגוריתמים חלשים כברירת מחדל | אין מה לעשות | - | ברירות מחדל של Conscrypt. TLS ישן: ראו 430 |
| 467 | החלפת אלגוריתמים בקלות (Crypto Agility) | לאמת | tools/verify-on-device.sh | לבדוק שהמודול com.android.conscrypt מותקן כ-APEX (ניתן לעדכון) |
| 469 | הצפנת תעבורה בין שירותים מקומיים | אין מה לעשות | - | תקשורת מקומית היא Binder ו-Unix sockets, לא רשת |
| 470 | שירותי המערכת מתקשרים ב-HTTPS בלבד | לאמת | security/data-collection.he.md, tools/verify-on-device.sh | לכידת תעבורה. בדיקת הקישוריות ב-HTTP היא חלק מהפרוטוקול (204), ויש גם בדיקת HTTPS |
| 472 | בדיקת ביטול תעודות (OCSP / CRL) | אין מה לעשות | security/crypto-policy.he.md | אנדרואיד 12 לא בודק ביטול תעודות באופן גורף. פער מתועד, לא ניתן ב-overlay |
| 474 | הגנה על זיכרון של תהליכים רגישים | אין מה לעשות | - | הגנות תהליך של הליבה ו-SELinux |
| 475 | ניקוי סודות מהזיכרון אחרי שימוש | אין מה לעשות | - | אחריות המפתח באפליקציה |
| 476 | API קריפטוגרפי נוח למפתחים | אין מה לעשות | - | JCA ו-Android Keystore |
| 477 | ממשק לניהול מפתחות | אין מה לעשות | - | Keystore וממשק התעודות בהגדרות |
| 478 | גיבוי מפתחות מאובטח | אין מה לעשות | - | מפתחות Keystore אינם ניתנים לייצוא מעצם תכנונם |
| 479 | מדיניות קריפטוגרפית מתועדת | להגדיר | security/crypto-policy.he.md | להשלים [לאימות] אחרי בדיקות 442, 430, 467 |
| 481 | מדיניות פרטיות ברורה ובשפה פשוטה | להגדיר | security/privacy-policy.he.md, security/Android.bp | להשלים סימוני [לאימות] וכתובת פנייה. קישור מתוך הגדרות > אודות באפליקציית ההגדרות |
| 482 | טלמטריה שניתן לכבות | להגדיר | futureos_security.mk, sepolicy/system_ext/private/* | אכיפה בקוד של כל אפליקציית FutureOS (לקרוא persist.sys.futureos.telemetry) ומתג בהגדרות |
| 483 | שקיפות לגבי מה נאסף ולמה | להגדיר | security/data-collection.he.md | לכידת תעבורה על מכשיר בדוק והשלמת עמודת אומת. לבדוק את SUPL בקובצי GPS של MediaTek |
| 484 | כלי לצפייה בנתוני הטלמטריה שנשלחו | להגדיר (חלקי) | futureos_security.mk, security/init.futureos.security.rc, sepolicy/system_ext/private/* | מסך בהגדרות שמציג את /data/misc/futureos/outbox. כרגע רק התיקייה והחוזה |
| 485 | עבודה מלאה בלי חשבון ענן | לאמת | tools/verify-on-device.sh | הפעלה ראשונה ושימוש מלא בלי חשבון. אם יש Provision/SetupWizard, לוודא שאפשר לדלג |
| 486 | אין פרסומות במערכת | לאמת | tools/verify-on-device.sh | סריקת חבילות המערכת ו-APK שחולצו מהמכשיר לאיתור SDK פרסומי |
| 487 | אין אפליקציות "מומלצות" שמותקנות בכפייה | לאמת | futureos_security.mk | ביקורת PRODUCT_PACKAGES והבלובים שחולצו. לא להוסיף חבילות partner |
| 488 | מזהה פרסום שניתן לאפס או לכבות | אין מה לעשות (+לאמת) | tools/verify-on-device.sh | אין GMS ולכן אין מזהה פרסום. אם GMS יתווסף בעתיד, לטפל מחדש |
| 489 | היסטוריית פעילות שניתן למחוק | להגדיר (חלקי) | security/futureos-privacy.sh, security/init.futureos.security.rc, sepolicy/system_ext/private/* | מסך מחיקה בהגדרות. Usage stats, recents ו-DropBox לא נמחקים בקבצים (system_server מחזיק אותם), נדרש API |
| 491 | שליטה מלאה על שירותי המיקום | להגדיר + לאמת | security/futureos-privacy.sh, overlay/frameworks/base/packages/SettingsProvider/res/values/defaults.xml | שליטה קיימת בהגדרות. לבדוק נגישות במקשים ושהמיקום כבוי בהפעלה ראשונה |
| 492 | אפשרות למיקום מקורב בלבד | לאמת | tools/verify-on-device.sh | דיאלוג מדויק/מקורב של אנדרואיד 12 נגיש במקשים |
| 493 | הסרת מטא-דאטה מתמונות בשיתוף | אין מה לעשות | - | אין הסרת EXIF בפלטפורמה. משימה לאפליקציית התמונות/קבצים של FutureOS (סעיף 782) |
| 494 | הסתרת תוכן התראות במסך הנעילה | להגדיר | security/futureos-privacy.sh | לוודא במכשיר שהערך נקלט (lock_screen_allow_private_notifications=0) |
| 496 | חסימת עוקבים | להגדיר (אופציונלי) | security/futureos-privacy.sh | בחירת ספק DNS עם חסימת עוקבים ב-PRIVATE_DNS_HOST. החלטת הבעלים |
| 497 | DNS מוצפן כברירת מחדל | להגדיר + לאמת | security/futureos-privacy.sh | Private DNS אופורטוניסטי כבר בברירת המחדל. מצב קשיח מחייב בחירת ספק (PRIVATE_DNS_HOST) |
| 499 | כתובת MAC אקראית לכל רשת | להגדיר + לאמת | overlay/packages/modules/Wifi/service/ServiceWifiResources/res/values/config.xml, tools/verify-on-device.sh | נדרש שדרייבר ה-Wi-Fi של MediaTek יתמוך בשינוי MAC. אם ה-Wi-Fi הוא APEX מוכן, להעביר ל-RRO |
| 500 | התראה כשאפליקציה קוראת את לוח הגזירים | להגדיר + לאמת | overlay/frameworks/base/core/res/res/values/config.xml, security/futureos-privacy.sh | כמו 398 |
| 501 | דוח פרטיות לכל אפליקציה | לאמת | tools/verify-on-device.sh | כמו 380 |
| 503 | בקשת הסכמה לפני שליחת נתונים לענן | להגדיר | futureos_security.mk | אכיפה באפליקציות: persist.sys.futureos.cloud_consent חייב להיות 1 לפני כל שליחה. כרגע אין שירות ענן |
| 504 | ייצוא הנתונים האישיים | אין מה לעשות (+לאמת) | - | אין חשבון ענן. ייצוא אנשי קשר: לוודא שאפליקציית אנשי הקשר מייצאת vCard. שאר הנתונים הם קבצים רגילים ב-USB |
| 505 | מחיקת חשבון וכל הנתונים | לאמת | - | אין חשבון. איפוס יצרן (סעיף 838) הוא המחיקה, לבדוק שהוא מוחק הכול |
| 509 | הסתרת שם המכשיר ברשתות ציבוריות | להגדיר + לאמת | overlay/frameworks/base/packages/SettingsProvider/res/values/defaults.xml, tools/verify-on-device.sh | שם מכשיר גנרי. לבדוק גם שם Bluetooth ו-net.hostname בפועל |
| 511 | שליטה על מה מוצג במצב "נא לא להפריע" | לאמת | tools/verify-on-device.sh | אפשרויות הסתרת התראות במצב נא לא להפריע נגישות במקשים |
| 512 | הגבלת גישה ליומני המערכת | לאמת | tools/verify-on-device.sh | אפליקציות בלי READ_LOGS. תקף ב-user build. לסקור דיאלוג גישה ליומנים |
| 513 | אנונימיזציה של דוחות קריסה | אין מה לעשות (+לאמת) | security/data-collection.he.md, tools/verify-on-device.sh | אין שליחה בכלל. לוודא ש-ro.error.receiver.* ריק. רכיב דוחות עתידי חייב להסיר מזהים לפני כתיבה |
| 514 | שליחת דוח קריסה רק באישור | להגדיר | futureos_security.mk | persist.sys.futureos.crash_upload=0 כברירת מחדל. אכיפה ברכיב עתידי |
| 515 | אין הקלטת מסך בלי חיווי | לאמת | tools/verify-on-device.sh | חיווי MediaProjection, ראו 372 |
| 516 | נתונים ביומטריים נשמרים מקומית בלבד | אין מה לעשות (+לאמת) | tools/verify-on-device.sh | אם אין חיישן ביומטרי במכשיר לא רלוונטי. אחרת התבניות נשארות ב-HAL |
| 517 | נתונים ביומטריים נשמרים בסביבה מאובטחת בחומרה | אין מה לעשות (+לאמת) | tools/verify-on-device.sh | תלוי ב-HAL ובחומרה המאובטחת. לבדוק מול pm list features |
| 519 | תזכורת תקופתית לבדיקת הרשאות | להגדיר (חלקי) | security/futureos-privacy.sh | הפעלת app hibernation נקבעת. תזכורת תקופתית לבדוק הרשאות דורשת קוד באפליקציית ההגדרות |
| 520 | ברירות מחדל ששומרות על פרטיות | להגדיר | security/futureos-privacy.sh, overlay/frameworks/base/packages/SettingsProvider/res/values/defaults.xml, futureos_security.mk, overlay/frameworks/base/core/res/res/values/config.xml, overlay/packages/modules/Wifi/service/ServiceWifiResources/res/values/config.xml | להחליט על PRIVATE_DNS_HOST, NTP_SERVER ו-CAPTIVE_* בראש futureos-privacy.sh, ולבדוק הכול במכשיר |
