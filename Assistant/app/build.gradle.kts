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

android {
    namespace = "com.future.assistant"
    compileSdk = 37
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.future.assistant"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // מנוע זיהוי דיבור מקומי (offline) - whisper.cpp, native, שפה נבחרת
        // בזמן ריצה. בונים רק ל-arm64-v8a (הארכיטקטורה של מכשירי הבדיקה) -
        // מצמצם משמעותית זמן בנייה וגודל APK.
        ndk { abiFilters += "arm64-v8a" }
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                arguments += "-DCMAKE_BUILD_TYPE=Release"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
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
    }
}

dependencies {
    implementation(project(":sharedkeypadnav"))
    // Piper TTS (קול neural) + ReNikud (טקסט עברי בלי ניקוד -> הגייה), שניהם
    // מודלי ONNX שרצים ישירות על ONNX Runtime - אין מנוע TTS כלשהו מותקן ברמת
    // המערכת במכשירי הבדיקה. ר' PiperTts.
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.30.0")
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
