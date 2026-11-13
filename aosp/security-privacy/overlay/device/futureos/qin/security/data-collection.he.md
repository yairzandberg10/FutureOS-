# מה המערכת שולחת ולמה (שקיפות)

**מצב: טיוטה.** העמודה "אומת" ריקה עד שבוצעה לכידת תעבורה של מכשיר בדוק (ראו tools/verify-on-device.sh).

| פעולה | למי | למה | ניתן לשינוי | אומת |
|-|-|-|-|-|
| בדיקת קישוריות (Captive Portal) | שרת ברירת המחדל של AOSP (Google) אלא אם הוגדר אחר | לדעת אם יש אינטרנט | כן, CAPTIVE_* ב-futureos-privacy.sh | |
| סנכרון שעה (NTP) | time.android.com אלא אם הוגדר אחר | שעון מדויק | כן, NTP_SERVER | |
| שאילתות DNS | ספק ה-DNS של הרשת, או הספק שנבחר ב-Private DNS | פתיחת אתרים | כן, PRIVATE_DNS_HOST | |
| SUPL / עזרי GPS של ספק השבב | נקבע בקושחת MediaTek | מיקום מהיר | [לבדוק בקובצי gps.conf של הספק] | |
| טלמטריה של FutureOS | אין. persist.sys.futureos.telemetry=0 | | כן, בהסכמה | |
| דוחות קריסה | אין. persist.sys.futureos.crash_upload=0 | | כן, בהסכמה | |

## כלל לכל רכיב של FutureOS
1. לפני שליחה בודקים persist.sys.futureos.telemetry ו-persist.sys.futureos.cloud_consent.
2. כל אירוע שיוצא נכתב קודם לתיקיית /data/misc/futureos/outbox כדי שהמשתמש יוכל לראות אותו (כלי הצפייה הוא אפליקציית ההגדרות).
3. דוחות קריסה עוברים הסרת מזהים (שם משתמש, מספרי טלפון, כתובות, מזהה מכשיר) לפני שהם נכתבים.

## פער ידוע
אנדרואיד 12 לא בודק ביטול תעודות (OCSP/CRL) באופן גורף. מתועד ב-crypto-policy.he.md.
