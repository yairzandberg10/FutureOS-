package com.future.music.jam

import com.future.music.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestoreSettings
import com.google.firebase.firestore.memoryCacheSettings
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

/**
 * הגישה ל-Firebase - אותו פרויקט של צ'אט FutureOS ב-Messages (ראו
 * Messages/firebase/firestore.rules). מבנה הנתונים:
 *  jamCodes/{code}               - {jam, expireAt}. הקוד בן 6 הספרות שמקלידים כדי להצטרף.
 *  jams/{jamId}                  - המארח, ההגדרות ומצב הניגון המשותף.
 *  jams/{jamId}/members/{uid}    - המשתתפים. רק מי שכאן קורא את הג'אם.
 *  jams/{jamId}/queue/{itemId}   - התור: שם, אמן, מי הוסיף, סדר, והאם הקובץ עלה.
 *  storage: jams/{jamId}/{itemId} - קובץ השיר עצמו, למשתתפים בלבד.
 * מזהה הג'אם אקראי וארוך, והקוד רק מפנה אליו: קוד שמתפנה וחוזר לשימוש לא
 * פותח למשתתפים הקודמים שום דבר בג'אם החדש. ה-Cloud Function מוחקת הכול בסיום.
 */
internal object JamBackend {
    val configured: Boolean = BuildConfig.FIREBASE_CONFIGURED

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore by lazy {
        FirebaseFirestore.getInstance().apply {
            // הג'אם חי רק בזמן אמת - אין סיבה לשמור עותק בדיסק.
            firestoreSettings = firestoreSettings { setLocalCacheSettings(memoryCacheSettings {}) }
        }
    }
    private val storage: FirebaseStorage by lazy {
        FirebaseStorage.getInstance().apply { maxUploadRetryTimeMillis = 60_000; maxDownloadRetryTimeMillis = 30_000 }
    }

    /**
     * כניסה אנונימית: מי שמצטרף לג'אם לא צריך חשבון או אימות SMS, רק קוד. החשבון
     * נשמר במכשיר, כך שאחרי הפעלה מחדש זה אותו משתתף.
     */
    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        return auth.signInAnonymously().await().user?.uid ?: error("Anonymous sign-in returned no user")
    }

    fun code(code: String): DocumentReference = db.document("jamCodes/$code")
    fun newJam(): DocumentReference = db.collection("jams").document()
    fun jam(jamId: String): DocumentReference = db.document("jams/$jamId")
    fun members(jamId: String): CollectionReference = db.collection("jams/$jamId/members")
    fun queue(jamId: String): CollectionReference = db.collection("jams/$jamId/queue")
    fun media(jamId: String, itemId: String): StorageReference = storage.reference.child("jams/$jamId/$itemId")
}
