package com.future.navigation.data.backend

import android.content.Context
import android.util.Log
import com.future.navigation.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage

/**
 * נקודת ההחלטה היחידה בין "יש שרת Firebase" ל"אין". כל שאר שכבת הנתונים
 * שואלת רק את [isAvailable] ואת שלושת ה-getters כאן.
 *
 * האפליקציה חייבת להמשיך לעבוד בלי Firebase: כל עוד app/google-services.json
 * לא קיים, תוסף google-services לא מוחל בכלל (ר' app/build.gradle.kts),
 * FIREBASE_CONFIGURED הוא false, [init] לא עושה כלום, וכל ריפוזיטורי ממשיך
 * לדבר ישירות מול השירות החיצוני בדיוק כמו קודם. ברגע שהקובץ מתווסף
 * ונבנים מחדש - אותו קוד עובר דרך ה-proxy בלי שינוי נוסף.
 *
 * ר' Navigation/firebase/README.md להקמת הפרויקט בצד השרת.
 */
object FirebaseBackend {

    private const val TAG = "FirebaseBackend"

    @Volatile
    private var available = false

    @Volatile
    private var initialized = false

    /** true רק אחרי ש-[init] הצליח באמת לאתחל FirebaseApp מהקונפיגורציה. */
    val isAvailable: Boolean get() = available

    /**
     * נקרא פעם אחת מ-Activity.onCreate. בטוח לקרוא לו שוב (idempotent) ובטוח
     * לקרוא לו גם כשאין קונפיגורציה - אז הוא פשוט מסמן שהשרת לא זמין.
     *
     * הכניסה האנונימית רצה ברקע ולא חוסמת: היא נחוצה כדי שחוקי הגישה
     * (firestore.rules/storage.rules) יוכלו לדרוש משתמש מזוהה במקום לפתוח
     * את הפרויקט לכל העולם, אבל קריאה שתצא לפני שהיא הסתיימה פשוט תיכשל
     * ותיפול חזרה לנתיב הישיר.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        if (!BuildConfig.FIREBASE_CONFIGURED) return

        val app = try {
            FirebaseApp.initializeApp(context.applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Firebase init failed", e)
            null
        }
        if (app == null) {
            Log.i(TAG, "no Firebase config on device - staying on the direct path")
            return
        }
        available = true

        // המטמון המקומי של Firestore דולק כברירת מחדל באנדרואיד, כך שמטא-דאטה
        // של חבילות התחבורה נקרא גם בלי רשת - אין צורך בהגדרה נוספת כאן.
        runCatching {
            val auth = FirebaseAuth.getInstance()
            if (auth.currentUser == null) {
                auth.signInAnonymously().addOnFailureListener { Log.w(TAG, "anonymous sign-in failed", it) }
            }
        }.onFailure { Log.w(TAG, "auth unavailable", it) }
    }

    /** null כשאין Firebase - הקורא חייב ליפול חזרה לנתיב הישיר. */
    val functions: FirebaseFunctions?
        get() = if (available) {
            runCatching { FirebaseFunctions.getInstance(BuildConfig.FIREBASE_FUNCTIONS_REGION) }.getOrNull()
        } else null

    val firestore: FirebaseFirestore?
        get() = if (available) runCatching { FirebaseFirestore.getInstance() }.getOrNull() else null

    val storage: FirebaseStorage?
        get() = if (available) runCatching { FirebaseStorage.getInstance() }.getOrNull() else null
}
