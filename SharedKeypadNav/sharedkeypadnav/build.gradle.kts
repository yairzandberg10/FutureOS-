plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

// notes/ הוא הצרכן היחיד (מתוך 22) שמכבה את AGP 9 built-in Kotlin
// (android.builtInKotlin=false ב-gradle.properties שלו, כדי לפתור התנגשות
// עם KSP/Room - ר' הערה מקבילה ב-notes/app/build.gradle.kts). מאחר
// שהמאפיין הזה גלובלי לכל עץ ה-build כולל מודולים חיצוניים כמו זה, כשהוא
// כבוי המודול הזה נשאר בלי task קומפילציה של Kotlin בכלל (built-in Kotlin
// היה המקור היחיד שלו לתמיכת Kotlin, בלי plugin מפורש). התיקון: מפעילים
// את ה-plugin הקלאסי במפורש, אבל רק כשבאמת צריך - החלה שלו יחד עם
// built-in Kotlin הפעיל (ב-21 הצרכנים האחרים) לא נתמכת ותשבור אותם.
if (findProperty("android.builtInKotlin") == "false") {
    apply(plugin = "org.jetbrains.kotlin.android")
}

// מודול ספרייה משותף לכל הסוויטה - מכיל רק רכיבים שאין להם שום תלות
// באפליקציה ספציפית (לא ב-FutureTheme של אף אפליקציה, לא בלוגיקת עסקים
// ספציפית). כל אפליקציה צורכת אותו כ-project(":sharedkeypadnav") דרך
// include עם projectDir חוצה-תיקיות ב-settings.gradle.kts שלה - המודול הזה
// עצמו לא מכיל settings.gradle.kts ולא gradlew משלו, בדיוק כמו כל מודול
// ":app" רגיל בתוך build קיים.
android {
    namespace = "com.future.sharednav"
    compileSdk = 37

    defaultConfig {
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// כשה-plugin הקלאסי מופעל למעלה (מקרה notes בלבד), ברירת המחדל של
// jvmTarget שלו נגזרת מהצרכן (21 עבור notes) ולא מ-compileOptions כאן
// (11) - קומפילציה נכשלת על אי-התאמה בין compileDebugJavaWithJavac (11)
// ל-compileDebugKotlin (21). tasks.withType כאן (בניגוד לבלוק kotlin{}
// המקונן בתוך android{}) עובד בכל שילוב של AGP built-in Kotlin/plugin
// קלאסי, כי הוא לא תלוי בתוסף DSL ספציפי.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // תלות ישירה (לא דרך alias של קטלוג) כי לא בכל 16 האפליקציות הצורכות
    // יש alias בקטלוג שלהן ל-material-icons-extended - הגרסה נגזרת מה-BOM.
    implementation("androidx.compose.material:material-icons-extended")

    testImplementation(libs.junit)
}
