# STATUS: סעיפים 14, 21-25 (עדכונים, פיתוח, אמינות, ביצועים ותיעוד)

תאריך: 2026-10-28. **כל הקבצים הם טיוטות שלא נבנו ולא נבדקו** (אין עץ AOSP ולא נגענו במכשיר). "נכתב" פירושו שהקובץ קיים, לא שהתכונה עובדת.

**קבוצה:** ל = לבצע/לבדוק, ק = כבר קיים (לאמת), א = כיסוי של אנדרואיד 12.
**החלטות:** overlay = מוגדר בקבצי product/Board/overlay. אימות = הגדרה של AOSP שרק בודקים (`tools/verify_updates_dev.sh` או ידנית). מסמך = נכתב מסמך. אין = אין מה לעשות עכשיו (יש סיבה).
**קטגוריה 23 (וירטואליזציה ומכולות):** אין אף סעיף בשלוש הקבוצות (הכול ב"לא מתאים"), ולכן אין שורות.

קיצורי נתיב (יחסית ל-`updates-dev-docs/`): M = `overlay/vendor/futureos/config/`, T = `overlay/vendor/futureos/tools/`, D = `docs/developer/`, Pu = `docs/public/`, Us = `docs/user/`.

## 14. משתמשים וחשבונות

| # | סעיף | קב' | החלטה | קובץ | מה נשאר ידני |
|---|---|---|---|---|---|
| 527 | התחברות ביומטרית | א | אימות | D/credentials-and-reset.md | לבדוק זיהוי פנים במכשיר. אין חיישן אצבע בחומרה |
| 528 | קוד PIN מקומי | ק | אימות | D/credentials-and-reset.md, KNOWN_ISSUES K8 | החלטה: לחבר את ה-PIN ל-LockSettings (FBE) או להשאיר מקביל |
| 529 | מדיניות סיסמאות | ל | מסמך | D/credentials-and-reset.md | אישור המדיניות; חסימת PIN חלשים בקוד FutureUI |
| 530 | איפוס סיסמה מאובטח | ל | מסמך | D/credentials-and-reset.md | החלטה אם לבנות קוד שחזור, אחרת איפוס מלא בלבד |
| 545 | תמונת פרופיל | א | אימות | D/credentials-and-reset.md | לבדוק שיש בחירת תמונה ב-Settings |

## 21. התקנה, עדכונים וחבילות

| # | סעיף | קב' | החלטה | קובץ | מה נשאר ידני |
|---|---|---|---|---|---|
| 810 | חבילות חתומות דיגיטלית | א | overlay + מסמך | M/futureos_updates.mk, T/make_release.sh, D/release-process.md | ליצור מפתחות (מחשב לא מחובר) ולהריץ חתימה |
| 815 | עדכונים אטומיים | א | overlay | M/futureos_updates.mk, M/BoardConfigUpdates.mk | `FUTUREOS_OTA_MODE` לפי M0; להשלים `AB_OTA_PARTITIONS` |
| 816 | חזרה לגרסה קודמת | א | overlay + מסמך | M/BoardConfigUpdates.mk, D/release-process.md | החלטה על הגדלת rollback index (ברירת מחדל 0) |
| 817 | הורדת עדכונים ברקע | א | overlay + ידני | M/futureos_updates.mk (חבילות, הרשאות) | לכתוב FutureUpdater (מחוץ למסגרת) + sepolicy ל-update_engine |
| 820 | עדכוני דלתא | א | מסמך + כלי | T/make_release.sh, D/release-process.md | לשמור target-files חתום לכל גרסה |
| 825 | Release Notes ברורים | ל | מסמך | Pu/RELEASE_NOTES_TEMPLATE.md, overlay/.../release/release_notes_he.txt | למלא לכל גרסה |
| 826 | מחזור חיים מוצהר | ל | מסמך | Pu/support-lifecycle.md | החלטה עסקית |
| 827 | תמיכה לטווח ארוך (5 שנים) | ל | מסמך | Pu/support-lifecycle.md, KNOWN_ISSUES K15 | החלטה. לא מומלץ להתחייב כרגע |
| 828 | הודעה מוקדמת על סוף תמיכה | ל | מסמך | Pu/support-lifecycle.md, M/futureos_lifecycle.mk (מושבת בהערה) | החלטה + מימוש בהודעת FutureUpdater |
| 838 | איפוס להגדרות יצרן | א | אימות + מסמך | D/credentials-and-reset.md, Us/updates-backup-reset.md | לוודא שיש מסך איפוס ב-Settings עם אישור כפול |
| 839 | איפוס עם שמירת קבצים | א | מסמך (מוגבלות) | D/credentials-and-reset.md, KNOWN_ISSUES K10 | מותנה בגיבוי לכרטיס (FutureBackup) |
| 840 | מינימום תוכנות מותקנות מראש | ל | overlay | M/futureos_lifecycle.mk, T/verify_updates_dev.sh | לאמת שמות מודולים מול העץ ולסרוק את רשימת החבילות |

## 22. כלי פיתוח, API ושורת פקודה

| # | סעיף | קב' | החלטה | קובץ | מה נשאר ידני |
|---|---|---|---|---|---|
| 841 | SDK מתועד | ל | מסמך | D/sdk-guide.md | לעבור עליו מול קוד SharedKeypadNav |
| 845 | אמולטור טרמינל איכותי | ק | אימות | D/sdk-guide.md סעיף 7 | לבדוק Terminal ב-user build (בלי su) |
| 865 | מצב מפתחים | ק | אימות | T/verify_updates_dev.sh, D/sdk-guide.md | לבדוק שהאפשרויות מוסתרות עד הקשה על build |
| 879 | גרסאות API ו-Deprecation | ל | מסמך | D/api-versioning-deprecation.md | אישור המדיניות; להגדיר `ro.futureos.api_level` |

## 24. אמינות, גיבוי והתאוששות

| # | סעיף | קב' | החלטה | קובץ | מה נשאר ידני |
|---|---|---|---|---|---|
| 921 | פועלת חודשים בלי אתחול | א | אימות + כלי | T/perf_snapshot.sh, D/logging-diagnostics.md סעיף 6 | soak של 7 ימים (M6) |
| 922 | אין דליפות זיכרון | א | אימות + כלי | T/perf_snapshot.sh, D/logging-diagnostics.md | השוואת PSS בין צילומים |
| 923 | גיבוי אוטומטי מובנה | ל | overlay + ידני | M/futureos_backup.mk, overlay-src (SettingsProvider), apps-template/ | לכתוב FutureBackup (transport). בלעדיו אין גיבוי בפועל (K9) |
| 924 | גיבוי מצטבר | א | מסמך | D/backup-design.md | מימוש ב-FutureBackup |
| 925 | גיבוי לענן | ל | אין | D/backup-design.md | דחוי: דורש שרת, הסכמה (503) והצפנה. החלטת מוצר |
| 926 | גיבוי לדיסק חיצוני | א | מסמך | D/backup-design.md | מימוש ב-FutureBackup |
| 927 | שחזור קובץ בודד | ל | מסמך | D/backup-design.md, Us/updates-backup-reset.md | מימוש |
| 928 | שחזור מערכת מלא | ל | מסמך | D/reliability-recovery.md | לבדוק sideload ו-wipe במכשיר, אחרי גיבוי nvram |
| 933 | מצב בטוח | א | overlay + אימות | overlay-src/.../core/res/.../config.xml, D/reliability-recovery.md | מיפוי מקש תפריט/OK ב-.kl; חיווי ו"אתחל למצב בטוח" ב-FutureUI |
| 935 | Snapshot אוטומטי לפני עדכון | א | overlay + מסמך | M/futureos_updates.mk (vab), D/release-process.md | תלוי ב-Virtual A/B (M0) |
| 938 | שירותים שמתקנים את עצמם | א | אימות + מסמך | D/reliability-recovery.md | `android:persistent="true"` ב-FutureUI וב-FutureLauncher; בדיקת kill |
| 939 | דוחות קריסה מפורטים | א | overlay | M/futureos_logging.mk, D/logging-diagnostics.md | ערכי DropBox בהפעלה ראשונה ב-FutureUI |
| 940 | כלים לניתוח Crash Dumps | א | מסמך + כלי | T/make_release.sh (symbols.zip), D/logging-diagnostics.md | לשמור symbols ו-mapping.txt לכל גרסה |
| 941 | אבחון בעיות חומרה | א | מסמך | D/logging-diagnostics.md סעיף 4 | אין אפליקציית אבחון; בדיקות לפי device-qa |
| 944 | Journaling | א | אימות | M/BoardConfigUpdates.mk (הערות), T/verify_updates_dev.sh | לבדוק fstab של היצרן ו-checkpoint |
| 945 | API לשמירה אוטומטית | א | מסמך | D/sdk-guide.md סעיף 3 | אכיפה בקוד האפליקציות |
| 946 | שחזור חלונות ואפליקציות | ל | מסמך | D/sdk-guide.md סעיף 3 | מימוש "פתח אחרון" ב-FutureLauncher |
| 947 | התאוששות מקריסת GPU | א | אימות | D/reliability-recovery.md | בדיקה ב-userdebug |
| 952 | עמידות בניתוקי רשת | א | מסמך | D/sdk-guide.md סעיף 4 | אכיפה בקוד האפליקציות |
| 953 | עבודה מלאה לא מקוון | ל | מסמך | D/sdk-guide.md, KNOWN_ISSUES K16 | בדיקה ידנית לכל אפליקציה |
| 954 | סנכרון אחרי חזרת חיבור | ל | מסמך | D/sdk-guide.md סעיף 4 | WorkManager באפליקציות; ערוץ push (K4) |
| 955 | גיבוי ושחזור הגדרות מערכת | ל | overlay + אימות | M/futureos_backup.mk, overlay-src (SettingsProvider) | דורש transport; לאמת שמות def_backup_* בעץ |
| 959 | התראה מוקדמת על בעיות | א | אימות + מסמך | D/logging-diagnostics.md סעיף 7, KNOWN_ISSUES K7 | לוודא ש-FutureUI מציג אזהרות אחסון, סוללה וחום |

## 25. ביצועים, ניטור, יומנים ותיעוד

| # | סעיף | קב' | החלטה | קובץ | מה נשאר ידני |
|---|---|---|---|---|---|
| 961 | מוניטור ביצועים מובנה | א | מסמך + כלי | T/perf_snapshot.sh, D/logging-diagnostics.md | מסך במכשיר: יכולת עתידית ב-Tools |
| 963 | יומן מערכת מרכזי | א | overlay | M/futureos_logging.mk | אימות `logcat -g` |
| 964 | סבב יומנים | א | overlay | M/futureos_logging.mk (`ro.logd.size`) | אימות |
| 965 | חיפוש וסינון ביומנים | א | אימות + מסמך | D/logging-diagnostics.md סעיף 1 | אין |
| 966 | רמות חומרה ביומנים | א | אימות + מסמך | D/logging-diagnostics.md סעיף 1 | אין |
| 970 | מדדי מעבד | א | מסמך + כלי | T/perf_snapshot.sh | אין |
| 971 | מדדי זיכרון | א | מסמך + כלי | T/perf_snapshot.sh | אין |
| 972 | מדדי דיסק | א | מסמך + כלי | T/perf_snapshot.sh | אין |
| 973 | מדדי רשת | א | מסמך + כלי | T/perf_snapshot.sh | אין |
| 974 | מדדי GPU | א | מסמך + כלי | T/perf_snapshot.sh, D/logging-diagnostics.md | אין מדד עומס כללי ל-Mali; רק gfxinfo (K12) |
| 986 | מסך מידע מערכת | ל | overlay + אימות | M/futureos_lifecycle.mk (`ro.futureos.*`) | מסך "אודות" ב-Settings שקורא אותם |
| 988 | תיעוד למשתמשי קצה | ל | מסמך | Us/guide.md | לעבור מול המכשיר |
| 990 | מאגר ידע ופתרון תקלות | ל | מסמך | Us/troubleshooting.md | להרחיב מהמשוב |
| 993 | Changelog מפורט | ק | מסמך | Pu/RELEASE_NOTES_TEMPLATE.md (`CHANGELOG.md` קיים בשורש) | אין |
| 994 | מפת דרכים פומבית | ל | מסמך | Pu/ROADMAP.md | אישור לפרסום |
| 995 | מעקב באגים פומבי | ל | מסמך | Pu/bug-tracking.md | הקמת labels ותבנית |
| 996 | שקיפות בעיות ידועות | ל | מסמך | Pu/KNOWN_ISSUES.md | לעדכן אחרי בדיקות |
| 997 | עזרה בלי אינטרנט | ל | מסמך | Us/guide.md (מקור לאפליקציית Guide) | להעביר ל-GuideContent.kt (לא נגענו באפליקציות) |
| 998 | Tooltips והסברים | ל | מסמך | D/sdk-guide.md סעיף 6 | מימוש בממשק |
| 999 | קוד פתוח או זמין לביקורת | ל | מסמך | Pu/open-source-and-license.md | החלטת בעל הפרויקט |
| 1000 | רישיון שימוש ברור והוגן | ל | מסמך | Pu/open-source-and-license.md | ניסוח משפטי בידי המשתמש או עורך דין |

## סיכום

65 סעיפים (קטגוריה 23: אפס). רובם מסמך או אימות; סעיף 925 (גיבוי לענן) נדחה במכוון. הפער המרכזי: אין FutureBackup ואין FutureUpdater, ולכן גיבוי ועדכון מוגדרים ברמת התשתית בלבד.