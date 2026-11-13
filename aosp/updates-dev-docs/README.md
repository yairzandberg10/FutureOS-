# updates-dev-docs: overlay ותיעוד לעדכונים, אמינות, יומנים ותיעוד (קטגוריות 14, 21-25)

טיוטות מוכנות להשלכה לעץ AOSP/LineageOS. **שום דבר לא נבנה ולא נבדק**: העץ לא קיים במחשב ולא נגענו במכשיר. טבלת ההחלטות לכל 65 הסעיפים: [STATUS.md](STATUS.md).

## מבנה
| נתיב | מה |
|---|---|
| `overlay/vendor/futureos/config/` | `futureos_updates.mk` (OTA, A/B, חתימה), `BoardConfigUpdates.mk` (AVB, מחיצות), `futureos_logging.mk` (logd, tombstones), `futureos_backup.mk`, `futureos_lifecycle.mk` (גרסה, הסרת אפליקציות) |
| `overlay/vendor/futureos/overlay-src/` | משאבי framework ו-SettingsProvider (מצב בטוח, ברירות מחדל לגיבוי) |
| `overlay/vendor/futureos/permissions/` | הרשאות privileged ל-FutureUpdater |
| `overlay/vendor/futureos/release/` | הערות גרסה שנטענות למכשיר |
| `overlay/vendor/futureos/apps-template/` | תבניות כללי גיבוי לאפליקציות |
| `overlay/vendor/futureos/tools/` | `make_release.sh` (חתימה, OTA מלא ודלתא), `perf_snapshot.sh`, `verify_updates_dev.sh` (סקריפטים למחשב, קוראים בלבד מהמכשיר) |
| `docs/user/` | מדריך משתמש, פתרון תקלות, עדכון/גיבוי/איפוס |
| `docs/developer/` | SDK, API ו-deprecation, release, יומנים, אמינות, אישורי גישה, עיצוב גיבוי |
| `docs/public/` | בעיות ידועות, מפת דרכים, מעקב באגים, מחזור חיים, רישוי, תבנית הערות גרסה |

## הטמעה בעץ
1. להעתיק את `overlay/vendor/futureos/` ל-`vendor/futureos/` בעץ (הנתיב הזה תואם ל-`docs/aosp-port-plan.md` סעיף 3א). אם כבר קיימת שם תיקייה, למזג ולא לדרוס.
2. ב-`device/<vendor>/f22pro/BoardConfig.mk`: `include vendor/futureos/config/BoardConfigUpdates.mk`.
3. ב-`device.mk`, **לפי הסדר**:
   ```
   FUTUREOS_OTA_MODE := none   # vab | ab | none, לפי M0
   $(call inherit-product, vendor/futureos/config/futureos_updates.mk)
   $(call inherit-product, vendor/futureos/config/futureos_logging.mk)
   $(call inherit-product, vendor/futureos/config/futureos_backup.mk)
   $(call inherit-product, vendor/futureos/config/futureos_lifecycle.mk)   # אחרון (filter-out)
   ```
4. לאמת מול העץ את כל מה שמסומן "לאמת": שמות מודולים ב-`FUTUREOS_REMOVED_MODULES`, שמות `def_backup_*`, `config_safeModeDisabled`, `virtual_ab_ota.mk`, `PRODUCT_OTA_PUBLIC_KEYS`.
5. אחרי צריבה: `overlay/vendor/futureos/tools/verify_updates_dev.sh` (דורש adb, קורא בלבד).

## מה לא נעשה, ודורש אותך
- `FUTUREOS_OTA_MODE` ו-`AB_OTA_PARTITIONS`: תלויים ב-dump של M0.
- מפתחות חתימה ו-AVB (מחשב לא מחובר, לא בגיט), והחלטה על rollback index.
- FutureUpdater, FutureBackup ושינויי FutureUI (אזהרות מערכת, מצב בטוח, ערכי DropBox): לא נגענו באפליקציות.
- sepolicy ל-update_engine, והחלטות מוצר: גיבוי לענן, קוד שחזור ל-PIN, תמיכה לטווח ארוך, רישיון.
- אין שינוי בתיקיות האחרות בריפו; הכול כאן בלבד.