# כללי R8 משותפים לכל אפליקציות FutureOS.
#
# הבסיס מגיע מ-proguard-android-optimize.txt; כאן רק מה שספציפי למערכת הזו.

# שמות מחלקות ומספרי שורות בדוחות קריסה - בלי זה, stack trace ממכשיר
# אמיתי חסר ערך, וזו המערכת היחידה שיש לנו לאבחון תקלות בשטח.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# רכיבי אנדרואיד שנוצרים משמם במניפסט - R8 לא רואה קורא להם בקוד.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.accessibilityservice.AccessibilityService
-keep public class * extends android.inputmethodservice.InputMethodService

# Kotlin: מטא-דאטה שנדרשת ב-runtime.
-keep class kotlin.Metadata { *; }

# פונקציות @Composable לא נשמרות כאן בכוונה. היה כאן keepclassmembers על
# כולן, בכל מחלקה - כלל שמונע מ-R8 להסיר, לשנות שם, ובעיקר לבצע inlining
# ואופטימיזציה לכל ה-UI של המערכת, ו-Compose עצמו לא צריך אותו: אף
# composable לא נקרא ב-reflection, ו-Compose מביא כללי consumer משלו.

# Room יוצרת מימושים בזמן קומפילציה ומאתרת אותם בשמם.
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# מודלים שעוברים סריאליזציה (העדפות, JSON) - שמירת שמות השדות.
-keepclassmembers class com.future.** {
    <init>(...);
    <fields>;
}

# ONNX Runtime: הספרייה הנייטיבית ניגשת למחלקות ה-Java שלה בשמן (JNI).
-keep class ai.onnxruntime.** { *; }
