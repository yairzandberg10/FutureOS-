package com.future.messages.chat

import android.app.Activity
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.util.Log
import com.future.messages.receiver.SmsDeliverReceiver
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * צ'אט FutureOS: הודעות באינטרנט בין מכשירי FutureOS, מוצפנות מקצה לקצה,
 * עם "נמסר", "נקרא", "מקליד..." ותמונות באיכות מלאה. אם לנמען אין FutureOS או
 * שאין רשת, ההודעה יוצאת כ-SMS/MMS כרגיל. ההודעות נשמרות ב-content://sms ולכן
 * מופיעות באותה שיחה לצד ה-SMS.
 *
 * זרימה: השולח מצפין לנמען (ChatCrypto) וכותב ל-inbox/{uid} של הנמען.
 * Cloud Function שולחת Push (FCM), והנמען מושך, מאמת, מפענח, שומר, מוחק
 * מהשרת ושולח קבלת "נמסר". כשהאפליקציה פתוחה יש גם האזנה חיה, לכן "מקליד..."
 * וקבלות מגיעים מיד.
 */
object FutureChat {
    private const val TAG = "FutureChat"
    private const val PEER_TTL_MS = 24 * 60 * 60 * 1000L
    private const val NOT_REGISTERED_TTL_MS = 6 * 60 * 60 * 1000L
    private const val TYPING_SHOW_MS = 6_000L
    private const val TYPING_SEND_INTERVAL_MS = 4_000L
    private const val IMAGE_MAX_SIDE = 2048
    private const val IMAGE_PLACEHOLDER = "תמונה"

    enum class State { NOT_CONFIGURED, SIGNED_OUT, ACTIVE }

    private val _state = MutableStateFlow(if (ChatBackend.configured) State.SIGNED_OUT else State.NOT_CONFIGURED)
    val state: StateFlow<State> = _state

    /** מספר (E.164) → עד מתי להציג "מקליד...". */
    private val _typing = MutableStateFlow<Map<String, Long>>(emptyMap())
    val typing: StateFlow<Map<String, Long>> = _typing

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val inboxLock = Mutex()
    private var listener: ListenerRegistration? = null
    private val lastTypingSent = HashMap<String, Long>()
    private lateinit var appContext: Context

    fun init(context: Context) {
        appContext = context.applicationContext
        if (!ChatBackend.configured) return
        _state.value = if (ChatBackend.uid() != null && ChatBackend.phone() != null) State.ACTIVE else State.SIGNED_OUT
    }

    val isActive: Boolean get() = _state.value == State.ACTIVE

    fun myPhone(): String? = ChatBackend.phone()

    // ---------------------------------------------------------------- הרשמה

    interface VerificationListener {
        fun onCodeSent()
        fun onVerified()
        fun onError(message: String)
    }

    private var verificationId: String? = null

    /** שולח קוד אימות ב-SMS. אם Play Services קולט את הקוד לבד, onVerified
     * נקרא בלי שהמשתמש מקליד כלום. */
    fun startVerification(activity: Activity, rawPhone: String, listener: VerificationListener) {
        init(activity)
        val phone = e164(activity, rawPhone) ?: return listener.onError("מספר לא תקין")
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) = signIn(credential, listener)
                override fun onVerificationFailed(e: FirebaseException) {
                    Log.w(TAG, "Verification failed", e)
                    listener.onError(e.localizedMessage ?: "האימות נכשל")
                }
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    listener.onCodeSent()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun submitCode(code: String, listener: VerificationListener) {
        val id = verificationId ?: return listener.onError("צריך לשלוח קוד קודם")
        signIn(PhoneAuthProvider.getCredential(id, code.trim()), listener)
    }

    private fun signIn(credential: PhoneAuthCredential, listener: VerificationListener) {
        scope.launch {
            try {
                FirebaseAuth.getInstance().signInWithCredential(credential).await()
                register()
                launch(Dispatchers.Main) { listener.onVerified() }
            } catch (e: Exception) {
                Log.w(TAG, "Sign-in failed", e)
                launch(Dispatchers.Main) { listener.onError(e.localizedMessage ?: "ההתחברות נכשלה") }
            }
        }
    }

    /** מפרסם את המפתחות הציבוריים ואת טוקן ה-Push. נקרא אחרי אימות ובכל
     * פתיחה, כדי שמפתח חדש (למשל אחרי התקנה מחדש) יתעדכן. */
    private suspend fun register() {
        val (agree, sign) = ChatCrypto.ensureIdentity()
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        ChatBackend.publishIdentity(agree, sign, token)
        _state.value = State.ACTIVE
        sync(appContext)
    }

    fun refreshRegistration(context: Context) {
        init(context)
        if (!isActive) return
        scope.launch { runCatching { register() }.onFailure { Log.w(TAG, "Re-register failed", it) } }
    }

    fun onNewPushToken(token: String) {
        if (!ChatBackend.configured) return
        scope.launch { runCatching { ChatBackend.updateToken(token) } }
    }

    fun signOut(context: Context) {
        detach()
        ChatBackend.signOut()
        ChatStore.get(context).clearAll()
        _state.value = State.SIGNED_OUT
    }

    // ---------------------------------------------------------------- שליחה

    /**
     * נקרא מ-SmsRepository עם שורה שכבר רשומה ב-content://sms כ-OUTBOX.
     * מחזיר false כשאין בכלל ניסיון צ'אט (הקורא שולח SMS מיד). אחרת השליחה
     * אסינכרונית, ואם הנמען לא רשום או שהשליחה נכשלת - smsFallback.
     */
    fun trySend(context: Context, smsId: Long, address: String, text: String, smsFallback: () -> Unit): Boolean {
        init(context)
        if (!isActive || !isOnline(context)) return false
        val phone = e164(context, address) ?: return false
        if (knownNotRegistered(context, phone)) return false
        scope.launch {
            val sent = try {
                val peer = resolvePeer(context, phone)
                if (peer == null) false else {
                    val id = UUID.randomUUID().toString()
                    ChatStore.get(context).putOutgoing(smsId, id, phone, threadIdOf(context, smsId))
                    val ok = sendPayload(peer, id, "m", JSONObject().put("t", "m").put("b", text))
                    if (ok) markSmsSent(context, smsId) else ChatStore.get(context).remove(smsId)
                    ok
                }
            } catch (e: Exception) {
                Log.w(TAG, "Chat send failed", e)
                false
            }
            if (!sent) smsFallback()
        }
        return true
    }

    /**
     * תמונה בצ'אט במקום MMS: באיכות מלאה (עד 2048 פיקסלים) ולא דחוסה למגבלת
     * הספק. מחזיר false כשהצ'אט לא רלוונטי - אז הקורא שולח MMS.
     */
    fun trySendImage(context: Context, address: String, caption: String, imageUri: Uri, mmsFallback: () -> Unit): Boolean {
        init(context)
        if (!isActive || !isOnline(context)) return false
        val phone = e164(context, address) ?: return false
        if (knownNotRegistered(context, phone)) return false
        val peer = runBlocking { withTimeoutOrNull(8_000) { runCatching { resolvePeer(context, phone) }.getOrNull() } } ?: return false
        val id = UUID.randomUUID().toString()
        val jpeg = runCatching { encodeImage(context, imageUri) }.getOrNull() ?: return false
        val file = mediaFile(context, id).apply { writeBytes(jpeg) }
        val imageOnly = caption.isBlank()
        val smsId = insertSms(context, address, if (imageOnly) IMAGE_PLACEHOLDER else caption, incoming = false) ?: return false
        ChatStore.get(context).putOutgoing(smsId, id, phone, threadIdOf(context, smsId), file.absolutePath, imageOnly)
        scope.launch {
            val ok = try {
                val (encrypted, key) = ChatCrypto.encryptFile(jpeg)
                val path = ChatBackend.upload(peer.uid, id, encrypted)
                sendPayload(peer, id, "m", JSONObject().put("t", "i").put("p", path).put("k", key.key).put("v", key.iv).put("c", caption))
            } catch (e: Exception) {
                Log.w(TAG, "Chat image send failed", e)
                false
            }
            if (ok) {
                markSmsSent(context, smsId)
            } else {
                ChatStore.get(context).remove(smsId)
                runCatching { context.contentResolver.delete(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), null, null) }
                file.delete()
                mmsFallback()
            }
        }
        return true
    }

    /** נקרא בכל שינוי בשדה הכתיבה. לכל היותר פעם ב-4 שניות, ורק למי שכבר ידוע כרשום. */
    fun onTyping(context: Context, address: String) {
        if (!isActive) return
        val phone = e164(context, address) ?: return
        val now = System.currentTimeMillis()
        synchronized(lastTypingSent) {
            if (now - (lastTypingSent[phone] ?: 0L) < TYPING_SEND_INTERVAL_MS) return
            lastTypingSent[phone] = now
        }
        scope.launch {
            val peer = ChatStore.get(context).peer(phone)?.takeIf { it.registered } ?: return@launch
            runCatching { sendPayload(Peer(peer.uid!!, phone, peer.agreeKey!!), UUID.randomUUID().toString(), "t", JSONObject().put("t", "y")) }
        }
    }

    fun isTyping(context: Context, address: String): Boolean {
        val phone = e164(context, address) ?: return false
        return (_typing.value[phone] ?: 0L) > System.currentTimeMillis()
    }

    /** המשתמש פתח שיחה - שולחים "נקרא" על ההודעות שהתקבלו בה. */
    fun onThreadRead(context: Context, threadId: Long) {
        if (!isActive) return
        scope.launch {
            ChatStore.get(context).takeReadPending(threadId).forEach { (phone, ids) -> sendReceipt(context, phone, "r", ids) }
        }
    }

    private class Peer(val uid: String, val phone: String, val agreeKey: String)

    private fun knownNotRegistered(context: Context, phone: String): Boolean {
        val cached = ChatStore.get(context).peer(phone) ?: return false
        return !cached.registered && System.currentTimeMillis() - cached.checkedAt < NOT_REGISTERED_TTL_MS
    }

    /** מי רשום לצ'אט - מהמטמון, או מהשרת פעם ביום. null = לא רשום. */
    private suspend fun resolvePeer(context: Context, phone: String): Peer? {
        val store = ChatStore.get(context)
        val cached = store.peer(phone)
        if (cached != null && System.currentTimeMillis() - cached.checkedAt < PEER_TTL_MS) {
            return if (cached.registered) Peer(cached.uid!!, phone, cached.agreeKey!!) else null
        }
        val identity = withTimeout(8_000) { ChatBackend.lookupPhone(phone) }
        if (identity == null || identity.uid == ChatBackend.uid()) {
            store.putPeer(phone, null, null, null)
            return null
        }
        store.putPeer(phone, identity.uid, identity.agreeKey, identity.signKey)
        return Peer(identity.uid, phone, identity.agreeKey)
    }

    private suspend fun sendPayload(peer: Peer, id: String, kind: String, payload: JSONObject): Boolean {
        val me = ChatBackend.uid() ?: return false
        payload.put("id", id).put("ts", System.currentTimeMillis())
        val sealed = ChatCrypto.seal(payload.toString().toByteArray(), peer.agreeKey, "$me>${peer.uid}|$id")
        return try {
            withTimeout(20_000) { ChatBackend.send(peer.uid, id, kind, sealed) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Send $kind to ${peer.uid} failed", e)
            false
        }
    }

    private suspend fun sendReceipt(context: Context, phone: String, kind: String, ids: List<String>) {
        if (ids.isEmpty()) return
        val peer = runCatching { resolvePeer(context, phone) }.getOrNull() ?: return
        sendPayload(peer, UUID.randomUUID().toString(), "r", JSONObject().put("t", "r").put("k", kind).put("ids", JSONArray(ids)))
    }

    // ---------------------------------------------------------------- קבלה

    /** האזנה חיה בזמן שהאפליקציה פתוחה - "מקליד..." וקבלות מגיעים מיד. */
    fun attach(context: Context) {
        init(context)
        if (!isActive || listener != null) return
        listener = ChatBackend.listenInbox { docs -> if (docs.isNotEmpty()) scope.launch { process(appContext, docs) } }
    }

    fun detach() {
        listener?.remove()
        listener = null
    }

    /** משיכת התור מהשרת - נקרא מה-Push ובפתיחה. */
    suspend fun sync(context: Context) {
        init(context)
        if (!isActive) return
        val docs = runCatching { ChatBackend.pullInbox() }.getOrElse { Log.w(TAG, "Pull failed", it); return }
        process(context, docs)
    }

    fun syncBlocking(context: Context) = runBlocking { withTimeoutOrNull(18_000) { sync(context) } }

    private suspend fun process(context: Context, docs: List<DocumentSnapshot>) = inboxLock.withLock {
        val me = ChatBackend.uid() ?: return@withLock
        val delivered = HashMap<String, MutableList<String>>()
        for (doc in docs) {
            try {
                handle(context, me, doc, delivered)
            } catch (e: Exception) {
                Log.w(TAG, "Dropping undecryptable message ${doc.id}", e)
            }
            runCatching { ChatBackend.delete(doc) }
        }
        delivered.forEach { (phone, ids) -> sendReceipt(context, phone, "d", ids) }
    }

    private suspend fun handle(context: Context, me: String, doc: DocumentSnapshot, delivered: MutableMap<String, MutableList<String>>) {
        val from = doc.getString("from") ?: return
        val sealed = ChatCrypto.Sealed(
            doc.getString("eph") ?: return, doc.getString("iv") ?: return,
            doc.getString("ct") ?: return, doc.getString("sig") ?: return,
        )
        val contextString = "$from>$me|${doc.id}"
        val store = ChatStore.get(context)
        var peer = store.peerByUid(from)?.takeIf { it.registered }
        val plaintext = try {
            ChatCrypto.open(sealed, peer?.signKey ?: throw IllegalStateException("unknown sender"), contextString)
        } catch (e: Exception) {
            // שולח חדש, או שהשולח התקין מחדש והמפתחות שלו התחלפו - שולפים
            // מהשרת ומנסים שוב.
            val identity = ChatBackend.lookupUid(from) ?: throw e
            store.putPeer(identity.phone, identity.uid, identity.agreeKey, identity.signKey)
            peer = store.peerByUid(from)
            ChatCrypto.open(sealed, identity.signKey, contextString)
        }
        val phone = peer!!.phone
        val json = JSONObject(String(plaintext))
        val id = json.optString("id", doc.id)
        when (json.optString("t")) {
            "m", "i" -> {
                if (store.hasIncoming(id)) return
                val isImage = json.optString("t") == "i"
                var imagePath: String? = null
                val caption = if (isImage) json.optString("c") else json.optString("b")
                if (isImage) {
                    val encrypted = ChatBackend.downloadAndDelete(json.getString("p"))
                    val bytes = ChatCrypto.decryptFile(encrypted, ChatCrypto.FileKey(json.getString("k"), json.getString("v")))
                    imagePath = mediaFile(context, id).apply { writeBytes(bytes) }.absolutePath
                }
                val imageOnly = isImage && caption.isBlank()
                val body = if (imageOnly) IMAGE_PLACEHOLDER else caption
                val smsId = insertSms(context, phone, body, incoming = true) ?: return
                store.putIncoming(smsId, id, phone, threadIdOf(context, smsId), imagePath, imageOnly)
                delivered.getOrPut(phone) { mutableListOf() }.add(id)
                clearTyping(phone)
                SmsDeliverReceiver.notifyIncoming(context, phone, body)
            }
            "r" -> {
                val ids = json.optJSONArray("ids") ?: return
                val read = json.optString("k") == "r"
                for (i in 0 until ids.length()) {
                    val smsId = store.outgoingSmsId(ids.getString(i), phone) ?: continue
                    if (read) store.markRead(smsId)
                    updateSms(context, smsId) { put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE) }
                }
            }
            "y" -> {
                val sentAt = doc.getTimestamp("sentAt")?.toDate()?.time ?: System.currentTimeMillis()
                if (System.currentTimeMillis() - sentAt < TYPING_SHOW_MS) {
                    _typing.value = _typing.value + (phone to System.currentTimeMillis() + TYPING_SHOW_MS)
                    scope.launch { delay(TYPING_SHOW_MS + 100); _typing.value = _typing.value.filterValues { it > System.currentTimeMillis() } }
                }
            }
        }
    }

    private fun clearTyping(phone: String) {
        if (phone in _typing.value) _typing.value = _typing.value - phone
    }

    // ---------------------------------------------------------------- עזר

    private fun insertSms(context: Context, address: String, body: String, incoming: Boolean): Long? = try {
        val values = ContentValues().apply {
            if (!incoming) put(Telephony.Sms.THREAD_ID, Telephony.Threads.getOrCreateThreadId(context, address))
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, if (incoming) 0 else 1)
            put(Telephony.Sms.TYPE, if (incoming) Telephony.Sms.MESSAGE_TYPE_INBOX else Telephony.Sms.MESSAGE_TYPE_OUTBOX)
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)?.let { ContentUris.parseId(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Cannot store chat message", e)
        null
    }

    private fun markSmsSent(context: Context, smsId: Long) =
        updateSms(context, smsId) { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) }

    private fun updateSms(context: Context, smsId: Long, block: ContentValues.() -> Unit) {
        try {
            context.contentResolver.update(ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), ContentValues().apply(block), null, null)
        } catch (e: Exception) {
            Log.e(TAG, "Cannot update sms $smsId", e)
        }
    }

    private fun threadIdOf(context: Context, smsId: Long): Long = try {
        context.contentResolver.query(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), arrayOf(Telephony.Sms.THREAD_ID), null, null, null
        )?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun mediaFile(context: Context, id: String) =
        File(File(context.filesDir, "chat_media").apply { mkdirs() }, "$id.jpg")

    private fun encodeImage(context: Context, uri: Uri): ByteArray {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val size = info.size
            val scale = IMAGE_MAX_SIDE.toFloat() / maxOf(size.width, size.height)
            if (scale < 1f) decoder.setTargetSize((size.width * scale).toInt(), (size.height * scale).toInt())
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
        return ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            out.toByteArray()
        }
    }

    private fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    /** "050-1234567" → "+972501234567", לפי מדינת ה-SIM. */
    fun e164(context: Context, address: String): String? {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = (tm?.simCountryIso?.takeIf { it.isNotEmpty() } ?: tm?.networkCountryIso?.takeIf { it.isNotEmpty() } ?: "il").uppercase()
        return PhoneNumberUtils.formatNumberToE164(address, iso)
            ?: address.filter { it.isDigit() || it == '+' }.takeIf { it.startsWith("+") && it.length > 8 }
    }
}
