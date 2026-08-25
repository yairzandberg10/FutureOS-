# מימוש מסך נעילה מותאם אישית (FutureUI Lock Screen)

יצרתי מסך נעילה חדש בהתאם לשפת העיצוב של הפרויקט, הכולל אפשרויות התאמה אישית מתקדמות, ניווט מבוסס פוקוס ומנגנון פתיחה פשוט.

## תכונות עיקריות

- **שעון מותאם אישית**: ניתן לבחור בין 3 סגנונות שעון (Bold, Thin, Stacked) בזמן עריכה.
- **קיצורי דרך**: שני קיצורי דרך בעיגולים בתחתית המסך (ימין ושמאל). ניתן לשנות את היישום המשויך אליהם (טלפון, מצלמה, פנס, הגדרות).
- **תצוגת התראות**: הצגת התראות אחרונות בצורה קומפקטית מעל קיצורי הדרך.
- **מצב עריכה**: לחיצה ארוכה על מקש "אופציות" (Menu/Settings) מכניסה למצב עריכה המאפשר ניווט בין חלקי המסך.
- **ניווט פוקוס**: במצב עריכה, ניתן לעבור בין השעון, הווידג'טים, וקיצורי הדרך לשינוי הגדרות.
- **שחרור נעילה**: לחיצה על "אישור" (OK/Enter) משחררת את הנעילה.

## רכיבים חדשים

1.  **[LockScreenLayoutManager.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/lockscreen/logic/LockScreenLayoutManager.kt)**: מנהל את שמירת ההגדרות של המשתמש.
2.  **[LockScreenScreen.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/lockscreen/ui/LockScreenScreen.kt)**: ממשק המשתמש (Compose) הכולל את כל הלוגיקה של העריכה והתצוגה.
3.  **[LockScreenAccessibilityService.kt](file:///C:/Users/yairz/AndroidStudioProjects/FutureUI/app/src/main/java/com/future/futureui/lockscreen/service/LockScreenAccessibilityService.kt)**: השירות שאחראי להציג את מסך הנעילה כ-Overlay מעל המערכת.

## הנחיות לשימוש

1.  יש להפעיל את שירות הנגישות **FutureUI Lock Screen** דרך הגדרות הנגישות או דרך ה-MainActivity.
2.  לפתיחת הנעילה: לחץ על מקש ה-**OK**.
3.  לעריכה: לחץ לחיצה ארוכה על מקש ה-**Option** (או Menu/Settings בשלט). במצב זה תוכל לנווט עם החצים ולשנות סגנונות.
