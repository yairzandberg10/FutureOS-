# אפליקציית הודעות חכמה - הרחבת פיצ'רים ותמיכה בעברית (RTL)

הוספת יכולות ניהול הודעות מתקדמות (ארכיון, נעילה, ספאם), תמיכה מלאה בעברית, החלפת ערכות נושא ושיפור הניווט ללא מגע.

## שינויים מוצעים

### [Component Name] נתונים (Data Layer)
עדכון המודלים כדי לתמוך במצבים החדשים של השיחות.

#### [MODIFY] [Models.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/data/Models.kt)
- הוספת שדות ל-`Conversation`: `isArchived`, `isLocked`, `isSpam`.
- הוספת שדה ל-`Message`: `isSpam`.

#### [MODIFY] [MockData.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/data/MockData.kt)
- עדכון נתוני דוגמה כך שיכללו שיחות בארכיון, שיחות חסומות ושיחות נעולות.

---

### [Component Name] ממשק משתמש (UI Layer)
התאמה לעברית, ניהול פוקוס ותתי-תפריטים.

#### [MODIFY] [strings.xml](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/res/values/strings.xml)
- הוספת מחרוזות בעברית לכל הכפתורים והתוויות.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/MainActivity.kt)
- ניהול מצב "ערכת נושא" (Light/Dark).
- הוספת ניווט למסכים החדשים (ארכיון, ספאם, נעול).

#### [MODIFY] [ConversationListScreen.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/ui/screens/ConversationListScreen.kt)
- שימוש ב-`LocalLayoutDirection` כדי להבטיח RTL.
- הוספת "תפריט פעולות" (Context Menu) שניתן לפתוח באמצעות מקש (למשל מקש תפריט או לחיצה ארוכה על מקש אישור) לביצוע פעולות: מחיקה, העברה לארכיון, נעילה.
- הוספת כפתור לשינוי ערכת נושא בראש המסך.

#### [NEW] [ArchiveScreen.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/ui/screens/ArchiveScreen.kt)
- מסך המציג את השיחות שהועברו לארכיון.

#### [NEW] [LockedConversationsScreen.kt](file:///C:/Users/yairz/AndroidStudioProjects/Messages/app/src/main/java/com/future/messages/ui/screens/LockedConversationsScreen.kt)
- מסך המציג שיחות נעולות (עם דמה של "פתיחת נעילה").

---

### [Component Name] הגנה וסינון
מימוש לוגיקת ספאם בסיסית.

- פונקציית סינון שמעבירה הודעות ממספרים לא מוכרים או עם תוכן חשוד לתיקיית הספאם.

## תוכנית אימות

### בדיקה ידנית
- לוודא שכל הטקסט מופיע מימין לשמאל (RTL).
- לוודא שניתן לעבור בין ערכות נושא ושהצבעים משתנים בהתאם.
- ביצוע פעולות "ארכיון" ו"מחיקה" באמצעות המקלדת בלבד ולוודא שהשיחה נעלמת מהרשימה הראשית ומופיעה בארכיון.
- בדיקה שהפוקוס עובר בצורה חלקה בין האלמנטים השונים.
