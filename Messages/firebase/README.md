# צ'אט FutureOS: הקמת השרת

צ'אט FutureOS שולח הודעות בין מכשירי FutureOS דרך האינטרנט, כמו וואטסאפ. ההודעות מוצפנות מקצה לקצה, ויש "נמסר", "נקרא", "מקליד..." ותמונות באיכות מלאה. לכל נמען אחר, או כשאין רשת, ההודעה יוצאת כ-SMS/MMS כרגיל.

עד שמוסיפים את `google-services.json`, Messages עובדת בדיוק כמו קודם.

## מה השרת רואה
רק מי שלח למי ומתי. התוכן מוצפן במכשיר השולח למפתח של הנמען (P-256 ECDH, מפתח זמני לכל הודעה, AES-256-GCM) וחתום במפתח החתימה של השולח. המפתחות הפרטיים יושבים ב-AndroidKeyStore ולא יוצאים מהמכשיר. אחרי שהנמען מקבל הודעה, היא נמחקת מהשרת. מה שלא נאסף נמחק אוטומטית אחרי 30 יום (Firestore TTL).

## הקמה (פעם אחת)
1. ב-https://console.firebase.google.com יוצרים פרויקט חדש. **מומלץ פרויקט נפרד מהניווט**, כי חוקי ה-Firestore של כל פרויקט הם קובץ אחד.
2. מוסיפים אפליקציית Android עם השם `com.future.messages`, ומוסיפים לה את טביעות ה-SHA-1 וה-SHA-256 של מפתח החתימה:
   ```bash
   keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android
   ```
   בלי טביעות האצבע, אימות הטלפון ייפול ל-reCAPTCHA בדפדפן, ובמכשיר מקשים אין איך לפתור אותו.
3. מורידים את `google-services.json` ושמים אותו ב-`Messages/app/`. הקובץ לא נכנס ל-git.
4. Authentication → Sign-in method: מפעילים **Phone**.
5. יוצרים Firestore (אזור `europe-west1`) ו-Storage.
6. עוברים לתוכנית **Blaze**. היא נדרשת ל-Cloud Function של ה-Push, ובשימוש אישי העלות היא אגורות.
7. פורסים מהתיקייה הזו:
   ```bash
   cd Messages/firebase/functions && npm install && cd .. && firebase use --add && firebase deploy
   ```
   `firebase deploy` מעלה את החוקים (firestore.rules, storage.rules), את ה-TTL (firestore.indexes.json) ואת הפונקציה `pushOnMessage`.
8. בונים ומתקינים את Messages. במסך ההודעות, כפתור המנעול פותח את "צ'אט FutureOS": מקלידים מספר, והקוד נקלט לבד.

## מבנה הנתונים
| נתיב | תוכן | מי ניגש |
|---|---|---|
| `users/{uid}` | מספר ושני מפתחות ציבוריים | כל משתמש מאומת קורא, רק הבעלים כותב |
| `phones/{e164}` | uid | בדיקת "האם רשום". אין אפשרות לרשום מספר שלא אומת |
| `private/{uid}` | טוקן FCM | רק הפונקציה |
| `inbox/{uid}/messages/{id}` | מעטפה מוצפנת | השולח יוצר (מזוהה לפי החשבון), רק הנמען קורא ומוחק |
| storage `media/{uid}/{id}` | תמונה מוצפנת | רק הנמען קורא ומוחק |
