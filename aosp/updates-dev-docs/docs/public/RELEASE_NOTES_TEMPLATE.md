# תבנית הערות גרסה (סעיפים 825, 993)

`CHANGELOG.md` בשורש הוא לוג הפרויקט. הערות גרסה הן הגרסה הקצרה למשתמש, והן גם נשמרות במכשיר (`/product/etc/futureos/release_notes_he.txt`, מקור: `overlay/vendor/futureos/release/`).

```
FutureOS <ro.futureos.version>  (ערוץ: dev | beta | stable)  תאריך: YYYY-MM-DD
תיקון אבטחה: YYYY-MM-DD  |  Rollback index: <מספר>  |  סוג עדכון: מלא | דלתא מגרסה <X>

מה חדש
- ...

תיקונים
- ...

שינויים שדורשים תשומת לב (מחיקת נתונים? שינוי בקיצורי מקשים?)
- ...

בעיות ידועות (קישור ל-KNOWN_ISSUES)
- ...

הוצא משימוש (ר' api-versioning-deprecation.md)
- ...
```

## כללי כתיבה
משפט אחד לכל פריט, בלשון משתמש ("תוקן: הודעה לא נשלחה כשהרשת חזרה"), בלי מספרי commit. אם העדכון מוחק נתונים, זה מופיע בראש.
