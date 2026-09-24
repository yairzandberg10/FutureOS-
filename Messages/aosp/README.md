# Messages בתמונת AOSP - RCS

RCS ב-FutureOS עובר דרך רישום ה-IMS של המודם (SipDelegate, אנדרואיד 12 ומעלה). ככה אין צורך בשרת צ'אט משלנו ולא בשרת Jibe של Google. הקוד נמצא ב-`app/src/main/java/com/future/messages/rcs`.

## מה נתמך
- הודעות טקסט 1:1 כ-RCS Standalone Messaging במצב Pager: בקשת SIP MESSAGE עם גוף CPIM.
- אישורי מסירה וקריאה (IMDN). בבועה מוצג "נמסר" או "נקרא", עם סיומת "RCS".
- בדיקת יכולות (UCE) של הנמען. התשובה נשמרת 24 שעות.
- קבלת קבצים (FT over HTTP) מוצגת כשם הקובץ וקישור.
- נפילה אוטומטית ל-SMS על אותה שורה בכל אחד מהמקרים: אין RCS, הנמען לא רשום, כישלון, או הודעה מעל 1300 בתים.

## עוד לא נתמך
- שיחת צ'אט (INVITE + MSRP). בקשת INVITE נכנסת נדחית עם 488, והשולח עובר להודעות עצמאיות.
- שליחת קבצים, קבוצות, חיווי "מקליד...".

## התקנה בתמונה
1. בונים עם `./gradlew assembleRelease` ומעתיקים את ה-APK לכאן בשם `FutureMessages.apk`.
2. מוסיפים את `aosp/` לעץ, ומוסיפים `FutureMessages` ל-`PRODUCT_PACKAGES`.
3. מחילים את `frameworks-base-rcs-privileged.patch` על `frameworks/base`. ב-Android 12 שתי הרשאות ה-RCS ניתנות רק ל-role של SYSTEM_SHELL (נבדק ב-roles.xml במכשיר), ולא לאפליקציית ה-SMS.
4. מגדירים את Messages כברירת המחדל ל-SMS (`config_defaultSms`).

## מה נדרש מהמודם ומהספק
- ה-ImsService של היצרן צריך לתמוך ב-SipTransport. `SipDelegateManager.isSupported()` מחזיר true רק כשזה מתקיים.
- הספק צריך להקצות RCS ברשת ה-IMS שלו (carrier config: `ims.rcs_feature_tag_allowed_string_array`, provisioning).

כשאחד התנאים חסר, `RcsStack.state` נשאר `UNAVAILABLE`, השירות לא עולה, ולא מוצגת שום התראה. כל ההודעות ממשיכות לצאת כ-SMS/MMS בדיוק כמו קודם.
