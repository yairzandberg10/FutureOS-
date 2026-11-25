package com.future.messages.chat

import com.future.messages.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Blob
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * הגישה ל-Firebase. מבנה הנתונים (ראו firebase/firestore.rules):
 *  users/{uid}                  - מפתחות ציבוריים. קריא לכל משתמש מחובר.
 *  phones/{sha256}              - {uid}. רמז בלבד: "כדאי לנסות לחיצת יד ב-SMS".
 *  private/{uid}                - טוקן ה-FCM. רק ל-Cloud Function.
 *  inbox/{uid}/messages/{id}    - תור הודעות מוצפנות לנמען. הנמען קורא ומוחק.
 *  media/{uid}/parts/{id}_{n}  - תמונות מוצפנות לנמען, בחלקים של עד 700KB.
 * השרת מחזיק רק צופן: אחרי שהנמען מקבל הודעה היא נמחקת מהתור.
 */
internal object ChatBackend {
    val configured: Boolean = BuildConfig.FIREBASE_CONFIGURED
    private const val TTL_MS = 30L * 24 * 60 * 60 * 1000

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // התור הוא זמני ומוצפן - אין סיבה לשמור עותק בדיסק.
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
        }
    }

    fun uid(): String? = if (configured) auth.currentUser?.uid else null

    fun signOut() {
        if (configured) auth.signOut()
    }

    /** כניסה אנונימית: הזהות בצ'אט היא המפתחות שבמכשיר, והמספר מאומת ב-SMS (FutureChat). */
    suspend fun signIn(): String = uid() ?: auth.signInAnonymously().await().user?.uid ?: error("Sign-in failed")

    /**
     * מפרסם מפתחות ציבוריים ורמז בספר הכתובות: hash של המספר → uid. הרמז לא מוכיח
     * כלום (כל אחד יכול לכתוב כל hash); הוא רק אומר "כדאי לנסות לחיצת יד ב-SMS".
     */
    suspend fun publishIdentity(agreeKey: String, signKey: String, fcmToken: String?, phoneHash: String) {
        val uid = uid() ?: error("Not signed in")
        db.runBatch { batch ->
            batch.set(db.document("users/$uid"), mapOf(
                "agreeKey" to agreeKey, "signKey" to signKey, "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(db.document("phones/$phoneHash"), mapOf("uid" to uid))
            if (fcmToken != null) batch.set(db.document("private/$uid"), mapOf("fcmToken" to fcmToken))
        }.await()
    }

    suspend fun updateToken(token: String) {
        val uid = uid() ?: return
        db.document("private/$uid").set(mapOf("fcmToken" to token), SetOptions.merge()).await()
    }

    class Keys(val agreeKey: String, val signKey: String)

    /** רמז מספר הכתובות: ה-uid שטוען למספר הזה, או null. לא אמין - ר' publishIdentity. */
    suspend fun lookupHint(phoneHash: String): String? =
        db.document("phones/$phoneHash").get(Source.SERVER).await().getString("uid")

    suspend fun lookupKeys(uid: String): Keys? {
        val doc = db.document("users/$uid").get(Source.SERVER).await()
        return Keys(doc.getString("agreeKey") ?: return null, doc.getString("signKey") ?: return null)
    }

    /**
     * כתיבה בטרנזקציה ולא ב-set רגיל: טרנזקציה נכשלת כשאין רשת, ולא נכנסת
     * לתור אופליין. כך "נכשל" באמת אומר שלא נשלח, והנפילה ל-SMS לא יוצרת כפילות.
     */
    suspend fun send(toUid: String, id: String, kind: String, sealed: ChatCrypto.Sealed) {
        val uid = uid() ?: error("Not signed in")
        val ref = db.document("inbox/$toUid/messages/$id")
        val data = mapOf(
            "from" to uid, "kind" to kind,
            "eph" to sealed.eph, "iv" to sealed.iv, "ct" to sealed.ct, "sig" to sealed.sig,
            "sentAt" to FieldValue.serverTimestamp(),
            "expireAt" to Timestamp(Date(System.currentTimeMillis() + TTL_MS)),
        )
        db.runTransaction { tx -> tx.set(ref, data); null }.await()
    }

    fun listenInbox(onDocuments: (List<DocumentSnapshot>) -> Unit): ListenerRegistration? {
        val uid = uid() ?: return null
        return db.collection("inbox/$uid/messages").addSnapshotListener { snapshot, error ->
            if (error == null && snapshot != null && !snapshot.metadata.hasPendingWrites()) {
                onDocuments(snapshot.documents)
            }
        }
    }

    suspend fun pullInbox(): List<DocumentSnapshot> {
        val uid = uid() ?: return emptyList()
        return db.collection("inbox/$uid/messages").get(Source.SERVER).await().documents
    }

    suspend fun delete(doc: DocumentSnapshot) {
        doc.reference.delete().await()
    }

    // ---------------------------------------------------------------- תמונות
    // תמונות מוצפנות נשמרות ב-Firestore עצמו, מחולקות לחלקים של עד 700KB
    // (מסמך מוגבל ל-1MB): media/{נמען}/parts/{id}_{n}. כך לא צריך Firebase
    // Storage, שבפרויקט החינמי (Spark) לא זמין.

    suspend fun upload(toUid: String, id: String, data: ByteArray): String {
        val uid = uid() ?: error("Not signed in")
        val total = (data.size + MEDIA_CHUNK - 1) / MEDIA_CHUNK
        require(total in 1..MEDIA_MAX_PARTS) { "Image too large" }
        val expireAt = Timestamp(Date(System.currentTimeMillis() + TTL_MS))
        for (n in 0 until total) {
            val part = data.copyOfRange(n * MEDIA_CHUNK, minOf(data.size, (n + 1) * MEDIA_CHUNK))
            db.document("media/$toUid/parts/${id}_$n").set(mapOf(
                "from" to uid, "n" to n, "total" to total, "data" to Blob.fromBytes(part), "expireAt" to expireAt,
            )).await()
        }
        return "media/$toUid/$id"
    }

    /** מוריד בלי למחוק - המחיקה (deleteMedia) רק אחרי שההודעה נשמרה אצלנו, אחרת כשל באמצע מאבד את התמונה. */
    suspend fun download(path: String): ByteArray {
        val (folder, id) = mediaRef(path)
        val first = db.document("$folder/${id}_0").get(Source.SERVER).await()
        val total = (first.getLong("total") ?: error("Missing image")).toInt()
        require(total in 1..MEDIA_MAX_PARTS) { "Bad image" }
        val out = java.io.ByteArrayOutputStream()
        out.write(first.getBlob("data")?.toBytes() ?: error("Missing image"))
        for (n in 1 until total) {
            val doc = db.document("$folder/${id}_$n").get(Source.SERVER).await()
            out.write(doc.getBlob("data")?.toBytes() ?: error("Missing image part"))
        }
        return out.toByteArray()
    }

    /** הקובץ המוצפן לא נשאר בשרת אחרי שהנמען קיבל ושמר אותו. */
    suspend fun deleteMedia(path: String) {
        runCatching {
            val (folder, id) = mediaRef(path)
            val total = db.document("$folder/${id}_0").get(Source.SERVER).await().getLong("total")?.toInt() ?: 1
            for (n in 0 until total.coerceIn(1, MEDIA_MAX_PARTS)) db.document("$folder/${id}_$n").delete().await()
        }
    }

    /** "media/{uid}/{id}" -> ("media/{uid}/parts", id) */
    private fun mediaRef(path: String): Pair<String, String> {
        val parts = path.split('/')
        require(parts.size == 3 && parts[0] == "media") { "Bad media path" }
        return "media/${parts[1]}/parts" to parts[2]
    }

    private const val MEDIA_CHUNK = 700 * 1024
    private const val MEDIA_MAX_PARTS = 40
}
