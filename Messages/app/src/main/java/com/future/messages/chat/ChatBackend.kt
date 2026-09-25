package com.future.messages.chat

import com.future.messages.BuildConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * הגישה ל-Firebase. מבנה הנתונים (ראו firebase/firestore.rules):
 *  users/{uid}                  - מספר ומפתחות ציבוריים. קריא לכל משתמש מאומת.
 *  phones/{e164}                - {uid}. ספר הכתובות: מי רשום לצ'אט.
 *  private/{uid}                - טוקן ה-FCM. רק ל-Cloud Function.
 *  inbox/{uid}/messages/{id}    - תור הודעות מוצפנות לנמען. הנמען קורא ומוחק.
 *  storage: media/{uid}/{id}    - תמונות מוצפנות לנמען.
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
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance().apply { maxUploadRetryTimeMillis = 30_000; maxDownloadRetryTimeMillis = 30_000 }
    }

    fun uid(): String? = if (configured) auth.currentUser?.uid else null
    fun phone(): String? = if (configured) auth.currentUser?.phoneNumber else null

    fun signOut() {
        if (configured) auth.signOut()
    }

    suspend fun publishIdentity(agreeKey: String, signKey: String, fcmToken: String?) {
        val uid = uid() ?: error("Not signed in")
        val phone = phone() ?: error("No verified phone")
        db.runBatch { batch ->
            batch.set(db.document("users/$uid"), mapOf(
                "phone" to phone, "agreeKey" to agreeKey, "signKey" to signKey, "updatedAt" to FieldValue.serverTimestamp()
            ))
            batch.set(db.document("phones/$phone"), mapOf("uid" to uid))
            if (fcmToken != null) batch.set(db.document("private/$uid"), mapOf("fcmToken" to fcmToken))
        }.await()
    }

    suspend fun updateToken(token: String) {
        val uid = uid() ?: return
        db.document("private/$uid").set(mapOf("fcmToken" to token), SetOptions.merge()).await()
    }

    class Identity(val uid: String, val phone: String, val agreeKey: String, val signKey: String)

    /** null = המספר לא רשום לצ'אט. חריגה = אין רשת. */
    suspend fun lookupPhone(phone: String): Identity? {
        val uid = db.document("phones/$phone").get(Source.SERVER).await().getString("uid") ?: return null
        return lookupUid(uid)?.takeIf { it.phone == phone }
    }

    suspend fun lookupUid(uid: String): Identity? {
        val doc = db.document("users/$uid").get(Source.SERVER).await()
        return Identity(
            uid,
            doc.getString("phone") ?: return null,
            doc.getString("agreeKey") ?: return null,
            doc.getString("signKey") ?: return null,
        )
    }

    /**
     * כתיבה בטרנזקציה ולא ב-set רגיל: טרנזקציה נכשלת כשאין רשת, ולא נכנסת
     * לתור אופליין. כך "נכשל" באמת אומר שלא נשלח, והנפילה ל-SMS לא יוצרת כפילות.
     */
    suspend fun send(toUid: String, id: String, kind: String, sealed: ChatCrypto.Sealed) {
        val uid = uid() ?: error("Not signed in")
        val ref = db.document("inbox/$toUid/messages/$id")
        val data = mapOf(
            "from" to uid, "fromPhone" to phone(), "kind" to kind,
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

    suspend fun upload(toUid: String, id: String, data: ByteArray): String {
        val uid = uid() ?: error("Not signed in")
        val path = "media/$toUid/$id"
        storage.reference.child(path).putBytes(
            data, StorageMetadata.Builder().setContentType("application/octet-stream").setCustomMetadata("from", uid).build()
        ).await()
        return path
    }

    /** מוריד ומוחק - הקובץ המוצפן לא נשאר בשרת אחרי שהנמען קיבל אותו. */
    suspend fun downloadAndDelete(path: String): ByteArray {
        val ref = storage.reference.child(path)
        val bytes = ref.getBytes(40L * 1024 * 1024).await()
        runCatching { ref.delete().await() }
        return bytes
    }
}
