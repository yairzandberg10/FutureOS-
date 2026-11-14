# rom-repack: FutureOS כקושחה שאפשר לצרוב (בלי לבנות AOSP)

המחשב הזה (8GB RAM, כ-40GB פנויים) לא יכול לבנות AOSP/LineageOS, כי בנייה כזו צריכה כ-300GB ו-16-32GB RAM. לכן לוקחים את הקושחה שכבר רצה על הטלפון ואופים לתוכה את FutureOS:

- **הקרנל, הדרייברים, `vendor`, המודם ו-IMS נשארים בדיוק של היצרן**, ולכן מקלדת, מסך, מצלמה, רדיו ו-VoLTE ממשיכים לעבוד. דרייברים חדשים לא נכתבים כאן: אין קוד קרנל ואין תיעוד חומרה של MediaTek (ר' `docs/aosp-port-plan.md` 1ב).
- **משתנה רק `system_a`:** כל אפליקציות FutureOS, כמו שהן מותקנות היום בטלפון, נכנסות ל-`/system/priv-app/<Name>/`. שום חבילה של היצרן לא נמחקת.
- **v2 (15.11):**
  - `gen_privapp.py` כותב את `/system/etc/permissions/privapp-permissions-futureos.xml` עם כל הרשאה privileged שכל APK מבקש. במכשיר מוגדר `ro.control_privapp_permissions=enforce`, ולכן הרשאה שחסרה בקובץ = bootloop.
  - `/system/etc/excluded-input-devices.xml` מבטל את מסך המגע (`mtk-tpd`) ברמת InputManager. הדרייבר בקרנל נשאר. כדי להחזיר את המגע: מוחקים את הקובץ.

## SystemUI אמיתי: למה עדיין לא
- `framework-res.apk` ו-`MtkSystemUI` חתומים במפתח platform **פרטי של Duoqin** (`CN=Duoqin`, SHA-256 `55:B1:B4:16...`), ולא במפתחות הציבוריים של AOSP.
- כדי להיות SystemUI צריך הרשאות שהן signature בלבד: `STATUS_BAR_SERVICE`, `INTERNAL_SYSTEM_WINDOW`, `BIND_NOTIFICATION_LISTENER_SERVICE`, `CONTROL_KEYGUARD`, `DEVICE_POWER`. בלי המפתח של Duoqin אי אפשר לקבל אותן.
- הדרך היחידה בלי Duoqin: לחתום מחדש את כל ה-ROM במפתח שלנו (framework-res וכל APK שחתום ב-platform, ב-system/system_ext/product/vendor). זה מחייב factory reset, כי החתימות ב-`/data/system/packages.xml` לא יתאימו.
- בנוסף, FutureUI בנוי היום כ-AccessibilityService עם overlay. צריך לכתוב לו `SystemUIService` אמיתי: keyguard, התראות, עוצמה, כיבוי ועוד (ר' `docs/aosp-port-plan.md` 3ב).
- מה שכן אפשרי כ-priv-app: `STATUS_BAR` (signature|privileged) נותן ל-FutureUI את `StatusBarManager.disable()`, שמסתיר את שורת המצב של MTK וחוסם את המגירה שלה. זה דורש שינוי קוד ב-FutureUI.

## עובדות המכשיר (נמדדו ב-14.11.2026)
- `1244RO.A1.AND12...20220528`, Android 12, קרנל 4.14.186, A/B (סלוט a), dynamic partitions, Virtual A/B. super בגודל 8GB, ובשימוש כ-2.9GB.
- bootloader פתוח (orange), Magisk ב-`boot_a`.
- `vbmeta_a` עם `flags=2`: האימות כבר מבוטל, ולכן system ששונה עולה בלי לגעת ב-vbmeta.
- recovery ב-`boot_a` (header v2) כולל fastbootd. הצריבה עוברת ב-`fastboot flash system_a`, ו-fastbootd מגדיל את המחיצה הלוגית בעצמו.

## שלבים
| | סקריפט | טלפון? | מה |
|---|---|---|---|
| 1 | `01_dump.sh` (Git Bash) | ADB + root, **קריאה בלבד** | גיבוי nvram/nvdata/nvcfg/proinfo/protect/seccfg/md1img/lk/preloader, ותמונות boot/vendor_boot/dtbo/vbmeta/system/vendor/product. משיכת ה-APK-ים המותקנים. SHA256 בשני הצדדים |
| 2 | `02_build_system.sh` (WSL, root) | לא | מגדיל את system_a, מבטל shared_blocks, וכותב את האפליקציות עם debugfs (owner root, 0644/0755, `u:object_r:system_file:s0`) |
| 3 | `kit/flash.bat` | **כן: צריבה** | בודק SHA256, מבקש YES, `adb reboot fastboot`, בודק userspace + סלוט a, וצורב רק את system_a. `flash.bat restore` מחזיר את המקור |

הפלט נשמר ב-`D:\futureos-rom\` ולא ב-OneDrive ולא בגיט: יש שם IMEI, וגודל התמונות כ-3GB.

## מגבלות
- **לא נבדק שהמכשיר עולה עם התמונה.** זה ייבדק רק בצריבה הראשונה, שמחכה לאישור המשתמש.
- התמונה מניחה צריבה **בלי מחיקת נתונים**. אחרי factory reset לא יוגדרו ברירות מחדל (launcher, חייגן, SMS, נגישות של FutureUI, הרשאות).
- ה-APK-ים חתומים במפתח debug, ולא ב-platform key, ולכן אין להם הרשאות signature של המערכת. `su` עדיין נדרש למה שדורש root.
- slot b ריק, ולכן הדרך היחידה חזרה היא `restore\system_a.img`.
