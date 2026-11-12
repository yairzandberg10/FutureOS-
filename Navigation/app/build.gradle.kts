import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

/**
 * מפתחות API (HERE, SIRI/MOT) לא נכתבים בקוד - הם נקראים מ-local.properties
 * (קובץ שכבר ב-.gitignore של כל הפרויקט, לא מגיע ל-git) ונחשפים כ-BuildConfig
 * fields. אם הקובץ/המפתח חסר, מתקבל מחרוזת ריקה - הריפוזיטורי הרלוונטי בודק
 * את זה (isConfigured) ומחזיר null בלי לקרוס, כדי שהאפליקציה עדיין תיבנה
 * ותרוץ לפני שהמפתחות מגיעים בפועל.
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun localProperty(name: String): String = localProperties.getProperty(name) ?: ""

// מפתח החתימה של הרילייס לא נמצא במאגר. הערכים נקראים מ-keystore.properties
// בשורש המאגר (ראו .gitignore) או ממשתני סביבה ב-CI. בלי הקובץ,
// בניית release עדיין רצה - היא פשוט יוצאת לא חתומה, במקום להיכשל.
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("../keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

/**
 * פרטי פרויקט ה-Firebase נקראים מ-app/google-services.json (שהקונסולה נותנת,
 * ושלא נכנס ל-git) - אבל בלי תוסף google-services ובלי שום ספריית Google:
 * המכשיר לא מריץ Google Play Services, אז האפליקציה מדברת עם Firebase ב-REST
 * רגיל (ר' data/backend/FirebaseBackend). מכאן נלקחים רק שלושה ערכים - מזהה
 * הפרויקט, מפתח ה-Web API וה-bucket - וכל אחד מהם אפשר גם לדרוס
 * ב-local.properties. בלי הקובץ הם ריקים, FIREBASE_CONFIGURED הוא false,
 * והאפליקציה עובדת ישירות מול השירותים החיצוניים בדיוק כמו קודם.
 */
val googleServicesJson = file("google-services.json")
@Suppress("UNCHECKED_CAST")
val googleServices: Map<String, Any?> = if (googleServicesJson.exists()) {
    groovy.json.JsonSlurper().parse(googleServicesJson) as Map<String, Any?>
} else emptyMap()
val googleProjectInfo = googleServices["project_info"] as? Map<*, *> ?: emptyMap<String, Any?>()
val googleApiKey = ((googleServices["client"] as? List<*>)?.firstOrNull() as? Map<*, *>)
    ?.let { (it["api_key"] as? List<*>)?.firstOrNull() as? Map<*, *> }
    ?.get("current_key") as? String ?: ""
fun firebaseValue(localName: String, fromJson: String): String =
    localProperty(localName).ifBlank { fromJson }
val firebaseProjectId = firebaseValue("FIREBASE_PROJECT_ID", googleProjectInfo["project_id"] as? String ?: "")
val firebaseApiKey = firebaseValue("FIREBASE_API_KEY", googleApiKey)
val firebaseStorageBucket = firebaseValue("FIREBASE_STORAGE_BUCKET", googleProjectInfo["storage_bucket"] as? String ?: "")

android {
    namespace = "com.future.navigation"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.future.navigation"
        minSdk = 31
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "HERE_API_KEY", "\"${localProperty("HERE_API_KEY")}\"")
        buildConfigField("String", "SIRI_API_KEY", "\"${localProperty("SIRI_API_KEY")}\"")
        buildConfigField("String", "SIRI_BASE_URL", "\"${localProperty("SIRI_BASE_URL")}\"")
        // כתובת שרת FutureOS (ר' FutureServer). נדרסת ב-ro.futureos.api_base במכשיר; ריקה = אין שרת.
        buildConfigField("String", "FUTURE_API_BASE", "\"${localProperty("future.api.base")}\"")
        // נקרא ב-FirebaseBackend כדי לדעת בזמן ריצה אם בכלל יש פרויקט Firebase
        // בבנייה הזו. מפתח ה-Web API של Firebase אינו סוד (הוא מזהה את הפרויקט
        // בלבד); ההרשאות נאכפות בחוקי הגישה ובדרישת משתמש מזוהה בפונקציות.
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"$firebaseProjectId\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"$firebaseApiKey\"")
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", "\"$firebaseStorageBucket\"")
        buildConfigField(
            "boolean",
            "FIREBASE_CONFIGURED",
            (firebaseProjectId.isNotBlank() && firebaseApiKey.isNotBlank()).toString(),
        )
        // אזור ה-Cloud Functions - חייב להיות זהה לאזור שבו נפרסו הפונקציות
        // (ר' firebase/functions/src/index.ts), אחרת כל קריאה מחזירה NOT_FOUND.
        buildConfigField("String", "FIREBASE_FUNCTIONS_REGION", "\"${localProperties.getProperty("FIREBASE_FUNCTIONS_REGION") ?: "europe-west1"}\"")
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
    // מפורש כמו ב-3 הצרכנים האחרים של Java 21 (Fitness/Music/notes) - בלי
    // זה jvmTarget נגזר משתמע מ-compileOptions, שעבד בפועל אבל היה חוסר
    // עקביות (הפתעה אפשרית אם AGP ישנה את התנהגות ברירת המחדל בגרסה עתידית).
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
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.google.material)

    // מפה (MapLibre + אריחי OpenFreeMap - חינמי, בלי מפתח, בלי Google Play Services)
    implementation(libs.maplibre.android.sdk)

    // רשת (HERE לניתוב נהיגה+פקקים, SIRI לתחבורה ציבורית בזמן אמת, הורדת קובץ ה-GTFS)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    // Firebase (Auth אנונימי, Cloud Functions, Firestore, Storage) עובר ב-REST
    // על OkHttp שכבר כאן - בכוונה בלי ה-SDK, שתלוי ב-Google Play Services
    // שאין במכשיר. ר' data/backend.

    // מסד נתונים מקומי (מטמון GTFS + מקומות שמורים)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
