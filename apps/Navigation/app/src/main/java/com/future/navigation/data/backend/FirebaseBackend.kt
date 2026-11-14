package com.future.navigation.data.backend

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.future.navigation.BuildConfig
import com.future.navigation.data.network.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder

/**
 * נקודת ההחלטה היחידה בין "יש שרת Firebase" ל"אין", וגם הלקוח היחיד שלו.
 *
 * המכשיר לא מריץ Google Play Services, ולכן אין כאן את ה-SDK של Firebase
 * בכלל: כל הדיבור עם השרת הוא REST רגיל על ה-OkHttp של האפליקציה -
 *  - כניסה אנונימית ורענון טוקן (Identity Toolkit / Secure Token),
 *  - קריאה ל-Cloud Functions מסוג callable (POST עם {"data": ...}),
 *  - קריאת מסמכים מ-Firestore וקבצים מ-Storage.
 *
 * האפליקציה חייבת להמשיך לעבוד בלי Firebase: כל עוד אין פרויקט בבנייה
 * (FIREBASE_CONFIGURED הוא false, ר' app/build.gradle.kts), [isAvailable] הוא
 * false וכל ריפוזיטורי ממשיך לדבר ישירות מול השירות החיצוני בדיוק כמו קודם.
 * גם כשיש פרויקט, כל כשל כאן מחזיר null ולא זורק - והקורא נופל לנתיב הישיר.
 *
 * ר' Navigation/firebase/README.md להקמת הפרויקט בצד השרת.
 */
object FirebaseBackend {

    private const val TAG = "FirebaseBackend"

    private const val PREFS = "firebase_backend"
    private const val KEY_REFRESH_TOKEN = "refresh_token"

    /** מרווח ביטחון: טוקן שנשארו לו פחות מזה מתחלף לפני השימוש. */
    private const val TOKEN_MARGIN_MS = 5 * 60 * 1000L

    private val jsonType = "application/json; charset=utf-8".toMediaType()

    /** לעבודה ברקע שלא שייכת למסך מסוים (כניסה ראשונה, רענון הגדרות). */
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var available = false

    @Volatile
    private var initialized = false

    private var prefs: SharedPreferences? = null

    private val tokenLock = Mutex()
    private var idToken: String? = null
    private var idTokenExpiresAt = 0L

    /** true רק כשיש בבנייה הזו פרויקט Firebase ו-[init] רץ. */
    val isAvailable: Boolean get() = available

    internal val appContext: Context? get() = appContextRef
    private var appContextRef: Context? = null

    /**
     * נקרא פעם אחת מ-Activity.onCreate. בטוח לקרוא לו שוב (idempotent) ובטוח
     * לקרוא לו גם כשאין קונפיגורציה - אז הוא פשוט מסמן שהשרת לא זמין.
     *
     * הכניסה האנונימית רצה ברקע ולא חוסמת: היא נחוצה כדי שחוקי הגישה
     * (firestore.rules/storage.rules) והפונקציות יוכלו לדרוש משתמש מזוהה במקום
     * לפתוח את הפרויקט לכל העולם. קריאה שתצא לפני שהיא הסתיימה פשוט תחכה לה.
     */
    fun init(context: Context) {
        if (initialized) return
        initialized = true
        if (!BuildConfig.FIREBASE_CONFIGURED) {
            Log.i(TAG, "no Firebase project in this build - staying on the direct path")
            return
        }
        appContextRef = context.applicationContext
        prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        available = true
        scope.launch { idToken() }
    }

    /**
     * טוקן מזהה בתוקף למשתמש האנונימי, או null כשאין רשת/שרת. בפעם הראשונה
     * נוצר משתמש אנונימי חדש; אחר כך רק מרעננים, כך שהמכשיר שומר על אותו uid.
     */
    suspend fun idToken(): String? {
        if (!available) return null
        return tokenLock.withLock {
            val cached = idToken
            if (cached != null && System.currentTimeMillis() < idTokenExpiresAt - TOKEN_MARGIN_MS) {
                return@withLock cached
            }
            val refreshToken = prefs?.getString(KEY_REFRESH_TOKEN, null)
            val fresh = (if (refreshToken != null) refresh(refreshToken) else null) ?: signUpAnonymously()
            fresh
        }
    }

    private suspend fun signUpAnonymously(): String? {
        val url = "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${BuildConfig.FIREBASE_API_KEY}"
        val body = buildJsonObject { put("returnSecureToken", true) }.toString().toRequestBody(jsonType)
        val response = execute(Request.Builder().url(url).post(body).build()) ?: run {
            Log.w(TAG, "anonymous sign-in failed")
            return null
        }
        return storeToken(
            idToken = response.string("idToken"),
            refreshToken = response.string("refreshToken"),
            expiresInSeconds = response.string("expiresIn"),
        )
    }

    private suspend fun refresh(refreshToken: String): String? {
        val url = "https://securetoken.googleapis.com/v1/token?key=${BuildConfig.FIREBASE_API_KEY}"
        val body = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()
        val response = execute(Request.Builder().url(url).post(body).build()) ?: run {
            // טוקן רענון שבוטל (למשל המשתמש האנונימי נמחק בקונסולה) - מתחילים מחדש.
            Log.w(TAG, "token refresh failed - signing in again")
            prefs?.edit()?.remove(KEY_REFRESH_TOKEN)?.apply()
            return null
        }
        return storeToken(
            idToken = response.string("id_token"),
            refreshToken = response.string("refresh_token"),
            expiresInSeconds = response.string("expires_in"),
        )
    }

    private fun storeToken(idToken: String?, refreshToken: String?, expiresInSeconds: String?): String? {
        if (idToken.isNullOrBlank()) return null
        val ttlMs = (expiresInSeconds?.toLongOrNull() ?: 3600L) * 1000L
        this.idToken = idToken
        idTokenExpiresAt = System.currentTimeMillis() + ttlMs
        if (!refreshToken.isNullOrBlank()) {
            prefs?.edit()?.putString(KEY_REFRESH_TOKEN, refreshToken)?.apply()
        }
        return idToken
    }

    /**
     * קריאה ל-Cloud Function מסוג callable, לפי הפרוטוקול של Firebase:
     * POST של {"data": payload} עם הטוקן, ותשובה {"result": ...} או {"error": ...}.
     * מחזיר את ה-result, או null בכל כשל.
     */
    suspend fun callFunction(name: String, payload: Map<String, Any?>): JsonElement? {
        if (!available) return null
        val token = idToken() ?: return null
        val url = "https://${BuildConfig.FIREBASE_FUNCTIONS_REGION}-${BuildConfig.FIREBASE_PROJECT_ID}" +
            ".cloudfunctions.net/$name"
        val body = buildJsonObject { put("data", payload.toJson()) }.toString().toRequestBody(jsonType)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .post(body)
            .build()
        val response = execute(request) ?: return null
        return response["result"]
    }

    /** כל המסמכים באוסף Firestore, כ-fields (בפורמט ה-REST, ר' [fieldString]). */
    suspend fun firestoreCollection(collection: String): List<FirestoreDocument>? {
        if (!available) return null
        val token = idToken() ?: return null
        val request = Request.Builder()
            .url("${firestoreBase()}/$collection?pageSize=300")
            .header("Authorization", "Bearer $token")
            .build()
        val response = execute(request) ?: return null
        val documents = response["documents"] as? kotlinx.serialization.json.JsonArray ?: return emptyList()
        return documents.mapNotNull { (it as? JsonObject)?.toDocument() }
    }

    /** מסמך יחיד מ-Firestore, או null כשאין כזה / אין גישה. */
    suspend fun firestoreDocument(path: String): FirestoreDocument? {
        if (!available) return null
        val token = idToken() ?: return null
        val request = Request.Builder()
            .url("${firestoreBase()}/$path")
            .header("Authorization", "Bearer $token")
            .build()
        return execute(request)?.toDocument()
    }

    /**
     * בקשת הורדה לקובץ ב-Cloud Storage, עם הטוקן - חוקי storage.rules דורשים
     * משתמש מזוהה. null כשאין שרת או טוקן.
     */
    suspend fun storageDownloadRequest(path: String): Request? {
        if (!available) return null
        val token = idToken() ?: return null
        val bucket = BuildConfig.FIREBASE_STORAGE_BUCKET.ifBlank { "${BuildConfig.FIREBASE_PROJECT_ID}.appspot.com" }
        val encodedPath = URLEncoder.encode(path, "UTF-8").replace("+", "%20")
        return Request.Builder()
            .url("https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedPath?alt=media")
            .header("Authorization", "Firebase $token")
            .build()
    }

    private fun firestoreBase(): String =
        "https://firestore.googleapis.com/v1/projects/${BuildConfig.FIREBASE_PROJECT_ID}/databases/(default)/documents"

    /** מריץ בקשה ומחזיר את גוף ה-JSON בהצלחה, או null (עם לוג) בכל כשל. */
    private suspend fun execute(request: Request): JsonObject? = withContext(Dispatchers.IO) {
        try {
            NetworkModule.okHttpClient.newCall(request).execute().use { response ->
                val text = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    Log.w(TAG, "${request.url.encodedPath} -> HTTP ${response.code}")
                    return@withContext null
                }
                NetworkModule.json.parseToJsonElement(text) as? JsonObject
            }
        } catch (e: Exception) {
            Log.w(TAG, "${request.url.encodedPath} failed", e)
            null
        }
    }

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content

    private fun JsonObject.toDocument(): FirestoreDocument? {
        val name = (this["name"] as? JsonPrimitive)?.content ?: return null
        val fields = (this["fields"] as? JsonObject) ?: JsonObject(emptyMap())
        return FirestoreDocument(id = name.substringAfterLast('/'), fields = fields)
    }

    private fun Any?.toJson(): JsonElement = when (this) {
        null -> JsonNull
        is JsonElement -> this
        is String -> JsonPrimitive(this)
        is Number -> JsonPrimitive(this)
        is Boolean -> JsonPrimitive(this)
        is Map<*, *> -> JsonObject(entries.associate { (key, value) -> key.toString() to value.toJson() })
        is Iterable<*> -> kotlinx.serialization.json.JsonArray(map { it.toJson() })
        else -> JsonPrimitive(toString())
    }
}

/**
 * מסמך Firestore כפי שה-REST מחזיר אותו: כל שדה עטוף בסוג שלו
 * ({"stringValue": "..."}, {"doubleValue": 1.5}, {"integerValue": "3"} ...).
 */
data class FirestoreDocument(val id: String, val fields: JsonObject) {

    private fun value(key: String): JsonObject? = fields[key] as? JsonObject

    fun string(key: String): String? =
        value(key)?.get("stringValue")?.jsonPrimitive?.content

    fun double(key: String): Double? {
        val field = value(key) ?: return null
        return (field["doubleValue"] ?: field["integerValue"])?.jsonPrimitive?.content?.toDoubleOrNull()
    }

    fun boolean(key: String): Boolean? =
        value(key)?.get("booleanValue")?.jsonPrimitive?.content?.toBooleanStrictOrNull()
}

/** תוצאת callable שהיא אובייקט, או null. */
internal fun JsonElement?.asObject(): JsonObject? = runCatching { this?.jsonObject }.getOrNull()
