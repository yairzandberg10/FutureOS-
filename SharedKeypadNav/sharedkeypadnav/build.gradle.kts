plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
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

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // תלות ישירה (לא דרך alias של קטלוג) כי לא בכל 4 האפליקציות הצורכות
    // יש alias בקטלוג שלהן ל-material-icons-extended - הגרסה נגזרת מה-BOM.
    implementation("androidx.compose.material:material-icons-extended")
}
