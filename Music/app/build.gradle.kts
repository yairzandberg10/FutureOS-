import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// מפתח החתימה של הרילייס לא נמצא במאגר. הערכים נקראים מ-keystore.properties
// בשורש המאגר (ראו .gitignore) או ממשתני סביבה ב-CI. בלי הקובץ,
// בניית release עדיין רצה - היא פשוט יוצאת לא חתומה, במקום להיכשל.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("../keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

/**
 * הג'אם (האזנה משותפת) רץ על אותו פרויקט Firebase של Messages. google-services.json
 * של פרויקט מכיל את כל האפליקציות שלו, ולכן אחרי שמוסיפים את com.future.music
 * לפרויקט מספיק קובץ אחד: אם אין קובץ ב-Music/app, משתמשים בזה של Messages.
 * הקובץ פרטי ולא נכנס ל-git. בלי קובץ שמכיר את com.future.music התוסף לא מוחל
 * (הוא היה נכשל), והמוזיקה עובדת בדיוק כמו קודם - רק בלי ג'אם. ראו README.
 */
val googleServicesJson = file("google-services.json")
val firebaseJson = listOf(googleServicesJson, rootProject.file("../Messages/app/google-services.json"))
    .firstOrNull { it.exists() && it.readText().contains("\"com.future.music\"") }
if (firebaseJson != null) {
    if (firebaseJson != googleServicesJson) firebaseJson.copyTo(googleServicesJson, overwrite = true)
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.future.music"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.future.music"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // נקרא ב-JamBackend: בלי google-services.json אין FirebaseApp, וכל קריאה
        // ל-Firebase הייתה זורקת חריגה.
        buildConfigField("boolean", "FIREBASE_CONFIGURED", (firebaseJson != null).toString())
    }

    signingConfigs {
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // R8 היה מכובה בכל 27 האפליקציות ולא היה שום proguard-rules.pro
            // במאגר - כלומר לא הייתה דרך לייצר APK מוקטן וחתום להפצה, על
            // מכשיר שהאחסון בו כבר עמוס.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // בלי keystore.properties החתימה נופלת למפתח ה-debug של המחשב הזה,
            // ולא לבנייה לא-חתומה: זה מה שמאפשר להתקין release על המכשיר מעל
            // התקנת debug קיימת (אותה חתימה - בלי הסרה ובלי לאבד נתונים). על
            // המכשיר רץ release ולא debug כי debug של Compose איטי פי כמה: בלי
            // R8, ו-debuggable מבטל את הקומפילציה מראש של ART.
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    // המכשיר הוא Android 12 (API 31) ו-targetSdk 31 בכוונה. ExpiredTargetSdkVersion
    // הוא כלל של Google Play, לא של אנדרואיד - האפליקציות מותקנות ישירות ולא
    // עוברות דרך Play - והוא הכלל היחיד שחוסם את בניית ה-release.
    lint {
        disable += "ExpiredTargetSdkVersion"
        // lintVital רץ בכל בניית release ומוסיף דקה לכל אפליקציה; ב-Assistant
        // וב-Messages הוא גם קורס על באג פנימי של lint (נתיב עם תווים לא
        // חוקיים ב-Windows) ומפיל את הבנייה. lint מלא עדיין זמין ב-./gradlew lint.
        checkReleaseBuilds = false
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":sharedkeypadnav"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    // ג'אם: כניסה אנונימית (Auth), התור והמצב המשותף (Firestore) וקובצי השירים
    // שהמשתתפים מוסיפים (Storage). לא עושים כלום בלי google-services.json.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
