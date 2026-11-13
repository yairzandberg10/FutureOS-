# security-privacy: overlay לאבטחה, הרשאות והפרטיות

קטגוריות 10-13 של הצ'קליסט. מה הוחלט לכל סעיף: [STATUS.md](STATUS.md). **לא נבנה ולא נבדק** (אין עץ AOSP ואין מכשיר).

## מה כאן
- `overlay/device/futureos/qin/` : מבנה תיקיות שמועתק כמות שהוא לעץ ה-AOSP. התיקייה `qin` היא שם זמני, להחליף בשם המכשיר האמיתי (ולעדכן נתיבים ב-`futureos_security.mk`).
- `tools/check-overlay-names.sh` : בודק שכל שם משאב ב-overlay קיים בעץ. **להריץ לפני הבנייה הראשונה.**
- `tools/verify-on-device.sh` : בדיקות קריאה בלבד במכשיר (adb) לסעיפי "לאמת".

## הטמעה
1. אחרי סנכרון העץ: להעתיק את `overlay/device/futureos/qin/` ל-`device/futureos/qin/` (למזג עם מה שקיים, בלי לדרוס).
2. ב-`device.mk`: `$(call inherit-product, device/futureos/qin/futureos_security.mk)`
3. בסוף `BoardConfig.mk`: `-include device/futureos/qin/BoardConfigSecurity.mk`
4. `tools/check-overlay-names.sh <AOSP_ROOT>`. כל שורה MISSING: למחוק את המשאב מה-overlay או להמיר ל-RRO (בעיקר ה-Wi-Fi אם הוא APEX מוכן).
5. למזג את `fstab-encryption.snippet.txt` לשורת /data ב-fstab הקיים (ידנית, לפי ה-fstab שחולץ מהמכשיר).
6. לבחור ערכים בראש `security/futureos-privacy.sh` : PRIVATE_DNS_HOST, NTP_SERVER, CAPTIVE_*, DEFAULT_LOCATION_OFF.
7. לבנות `user` build. בהפעלה ראשונה: `logcat -s futureos-privacy` ו-`logcat | grep avc.*futureos`. כללי sepolicy ב-`futureos_privacy.te` הם ניחוש ראשון וכנראה ידרשו audit2allow.
8. להריץ `tools/verify-on-device.sh` ולסמן ב-STATUS מה אומת. להשלים את סימוני [לאימות] במסמכי `security/*.he.md` לפני פרסום.

## זהירות
- מסמכי המדיניות הם טיוטה ואין לפרסם אותם לפני לכידת תעבורה אמיתית.
- ה-overlay לא מכיל משהו שמשנה מחיצות או את המכשיר. כל השינויים נכנסים רק דרך בניית image.
