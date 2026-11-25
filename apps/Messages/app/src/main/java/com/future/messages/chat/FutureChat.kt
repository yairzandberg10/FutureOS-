package com.future.messages.chat

import com.future.sharednav.i18n.tr

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
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

/**
 * צ'אט FutureOS: הודעות באינטרנט בין מכשירי FutureOS, מוצפנות מקצה לקצה,
 * עם "נמסר", "נקרא", "מקליד..." ותמונות באיכות מלאה. אם לנמען אין FutureOS או
 * שאין רשת, ההודעה יוצאת כ-SMS/MMS כרגיל. ההודעות נשמרות ב-content://sms ולכן
 * מופיעות באותה שיחה לצד ה-SMS.
 *
 * זרימה: אנשי קשר מאומתים בלחיצת יד ב-SMS (ר' למטה). השולח מצפין לנמען
 * (ChatCrypto) וכותב ל-inbox/{uid} של הנמען. ChatService מחזיק האזנה פתוחה,
 * והנמען מאמת, מפענח, שומר, מוחק מהשרת ושולח קבלת "נמסר".
 */
object FutureChat {
    private const val TAG = "FutureChat"
    private const val PEER_TTL_MS = 24 * 60 * 60 * 1000L
    private const val NOT_REGISTERED_TTL_MS = 6 * 60 * 60 * 1000L
    private const val TYPING_SHOW_MS = 6_000L
    private const val TYPING_SEND_INTERVAL_MS = 4_000L
    private const val IMAGE_MAX_SIDE = 2048
    private const val IMAGE_PLACEHOLDER = "תמונה"
    private val SAFE_ID = Regex("[A-Za-z0-9_-]{1,64}")
    private const val MAX_RECEIPT_IDS = 500

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
        _state.value = if (ChatBackend.uid() != null && myPhone() != null) State.ACTIVE else State.SIGNED_OUT
    }

    val isActive: Boolean get() = _state.value == State.ACTIVE

    private fun prefs(context: Context = appContext) = context.getSharedPreferences("future_chat", Context.MODE_PRIVATE)

    /** המספר שלנו, אחרי שאומת ב-SMS לעצמנו. */
    fun myPhone(): String? = if (::appContext.isInitialized) prefs().getString("my_phone", null) else null

    // ---------------------------------------------------------------- הרשמה
    //
    // אין קוד אימות מ-Firebase (SMS של Firebase דורש תוכנית בתשלום). במקום זה:
    //  1. כניסה אנונימית ל-Firebase ויצירת מפתחות במכשיר.
    //  2. אימות המספר שלנו: SMS אחד לעצמנו עם nonce. אם הוא חוזר אלינו - המספר שלנו.
    //  3. כל איש קשר מאומת בלחיצת יד ב-SMS (ר' "לחיצת יד" למטה), לא דרך השרת.

    interface VerificationListener {
        fun onCodeSent()
        fun onVerified()
        fun onError(message: String)
    }

    private var setupListener: VerificationListener? = null
    private var setupTimeout: Job? = null

    /** שולח SMS אחד למספר שהוקלד, ומחכה שיחזור אלינו (onControlSms "V"). */
    fun startVerification(activity: Activity, rawPhone: String, listener: VerificationListener) {
        init(activity)
        val phone = e164(activity, rawPhone) ?: return listener.onError(tr("מספר לא תקין"))
        setupListener = listener
        scope.launch {
            try {
                ChatBackend.signIn()
                ChatCrypto.ensureIdentity()
                val nonce = newNonce()
                prefs().edit().putString("pending_phone", phone).putString("pending_nonce", nonce).commit()
                sendControlSms(appContext, phone, "$PREFIX V $nonce")
                launch(Dispatchers.Main) { listener.onCodeSent() }
                setupTimeout?.cancel()
                setupTimeout = scope.launch {
                    delay(SETUP_TIMEOUT_MS)
                    if (!this@FutureChat.isActive) finishSetup(error = tr("ה-SMS לא חזר. בדוק שהמספר נכון ושיש קליטה, ונסה שוב."))
                }
            } catch (e: Exception) {
                Log.w(TAG, "Setup failed", e)
                finishSetup(error = e.localizedMessage ?: tr("ההתחברות נכשלה"))
            }
        }
    }

    private fun finishSetup(error: String?) {
        setupTimeout?.cancel()
        val listener = setupListener ?: return
        setupListener = null
        scope.launch(Dispatchers.Main) { if (error == null) listener.onVerified() else listener.onError(error) }
    }

    /** מפרסם את המפתחות הציבוריים, הרמז בספר הכתובות וטוקן ה-Push. נקרא אחרי
     * אימות ובכל פתיחה, כדי שמפתח או uid חדשים (למשל אחרי התקנה מחדש) יתעדכנו. */
    private suspend fun register() {
        val phone = myPhone() ?: return
        ChatBackend.signIn()
        val (agree, sign) = ChatCrypto.ensureIdentity()
        val token = runCatching { FirebaseMessaging.getInstance().token.await() }.getOrNull()
        ChatBackend.publishIdentity(agree, sign, token, phoneHash(phone))
        _state.value = State.ACTIVE
        ChatService.start(appContext)
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
        ChatService.stop(context)
        keepListening = false
        detach()
        ChatBackend.signOut()
        ChatStore.get(context).clearAll()
        prefs(context).edit().clear().commit()
        _state.value = State.SIGNED_OUT
    }

    // ---------------------------------------------------------------- לחיצת יד ב-SMS
    //
    // הדבר היחיד שמוכיח מספר הוא SMS שמגיע אליו וחוזר. השרת (Firebase, כניסה
    // אנונימית) לא יודע מי מחזיק איזה מספר.
    //
    //  א' → ב' (SMS):  FOS1 H <uid-א> <טביעת מפתחות-א> <nonce-א>
    //  ב' → א' (SMS):  FOS1 A <uid-ב> <טביעת מפתחות-ב> <nonce-א> <nonce-ב>
    //  א' → ב' (צ'אט): הודעה חתומה מסוג "v" שמחזירה את nonce-ב.
    //
    // א' מאמת את ב' רק אם nonce-א חזר בתשובה מהמספר שאליו שלח - כלומר מי שענה
    // קיבל את ה-SMS במספר הזה. ב' מאמת את א' רק כשהודעה חתומה במפתח של א' מחזירה
    // את nonce-ב, שנשלח רק ב-SMS למספר של א'. טביעות המפתחות ב-SMS נבדקות מול
    // המפתחות בשרת, כך שהשרת לא יכול להחליף מפתח. מה שזה מוכיח: שליטה במספר בזמן
    // לחיצת היד.
    //
    // לחיצת יד נשלחת רק למספר שיש לו רמז בספר הכתובות (מישהו טוען שהוא משתמש
    // FutureOS), ולכל היותר פעם ביום למספר - כך מי שאין לו FutureOS לא מקבל SMS מוזר.

    private const val PREFIX = "FOS1"
    private val CONTROL_SMS = Regex(
        "^FOS1 (?:H [A-Za-z0-9]{10,64} [0-9a-f]{32} [0-9a-f]{16}|A [A-Za-z0-9]{10,64} [0-9a-f]{32} [0-9a-f]{16} [0-9a-f]{16}|V [0-9a-f]{16})$"
    )
    private const val HANDSHAKE_INTERVAL_MS = 24 * 60 * 60 * 1000L
    private const val SETUP_TIMEOUT_MS = 90_000L

    /** הודעת בקרה של הצ'אט (לא מוצגת בשיחות). פירוש קפדני: הודעה רגילה שמתחילה ב-"FOS1" לא נבלעת. */
    fun isControlSms(body: String): Boolean = CONTROL_SMS.matches(body.trim())

    /** נקרא מ-SmsDeliverReceiver (ב-thread רקע) על הודעת בקרה. */
    suspend fun onControlSms(context: Context, address: String, body: String) {
        init(context)
        if (!ChatBackend.configured) return
        val parts = body.trim().split(' ')
        val from = e164(context, address) ?: return
        try {
            when (parts[1]) {
                "V" -> {
                    val p = prefs(context)
                    if (from != p.getString("pending_phone", null) || parts[2] != p.getString("pending_nonce", null)) return
                    p.edit().putString("my_phone", from).remove("pending_phone").remove("pending_nonce").commit()
                    // לא בתוך זמן ה-receiver: ההרשמה בשרת יכולה לקחת יותר מכמה שניות
                    scope.launch {
                        try {
                            register()
                            finishSetup(error = null)
                        } catch (e: Exception) {
                            Log.w(TAG, "Register after verification failed", e)
                            finishSetup(error = e.localizedMessage ?: tr("ההתחברות נכשלה"))
                        }
                    }
                }
                "H" -> onHello(context, from, uid = parts[2], fp = parts[3], theirNonce = parts[4])
                "A" -> onAnswer(context, from, uid = parts[2], fp = parts[3], echoed = parts[4], theirNonce = parts[5])
            }
        } catch (e: Exception) {
            Log.w(TAG, "Control SMS ${parts.getOrNull(1)} failed", e)
            if (parts.getOrNull(1) == "V") finishSetup(error = e.localizedMessage ?: tr("ההתחברות נכשלה"))
        }
    }

    /** קיבלנו "שלום": עונים עם nonce משלנו. השולח עוד לא מאומת (ר' handle, סוג "v"). */
    private suspend fun onHello(context: Context, from: String, uid: String, fp: String, theirNonce: String) {
        if (!isActive || uid == ChatBackend.uid()) return
        val keys = ChatBackend.lookupKeys(uid) ?: return
        if (keysFingerprint(keys.agreeKey, keys.signKey) != fp) return
        val nonce = newNonce()
        ChatStore.get(context).putHandshakeIn(ChatStore.Incoming(from, nonce, uid, keys.agreeKey, keys.signKey))
        val (agree, sign) = ChatCrypto.ensureIdentity()
        val me = ChatBackend.uid() ?: return
        sendControlSms(context, from, "$PREFIX A $me ${keysFingerprint(agree, sign)} $theirNonce $nonce")
    }

    /** קיבלנו תשובה: אם היא מחזירה את ה-nonce ששלחנו למספר הזה - המספר מאומת. */
    private suspend fun onAnswer(context: Context, from: String, uid: String, fp: String, echoed: String, theirNonce: String) {
        if (!isActive) return
        val store = ChatStore.get(context)
        if (store.handshakeOutNonce(from) != echoed) return
        val keys = ChatBackend.lookupKeys(uid) ?: return
        if (keysFingerprint(keys.agreeKey, keys.signKey) != fp) return
        store.removeHandshakeOut(from)
        checkPinnedKey(context, from, keys.signKey)
        store.putPeer(from, uid, keys.agreeKey, keys.signKey)
        // סוגר את הלחיצה אצל הצד השני: הודעה חתומה שמחזירה את ה-nonce שלו
        sendPayload(Peer(uid, from, keys.agreeKey), UUID.randomUUID().toString(), "v", JSONObject().put("t", "v").put("n", theirNonce))
    }

    /** שולח "שלום" למספר שיש לו רמז בספר הכתובות - לכל היותר פעם ביום. */
    private suspend fun startHandshake(context: Context, phone: String) {
        val store = ChatStore.get(context)
        if (System.currentTimeMillis() - store.handshakeSentAt(phone) < HANDSHAKE_INTERVAL_MS) return
        val me = ChatBackend.uid() ?: return
        val (agree, sign) = ChatCrypto.ensureIdentity()
        val nonce = newNonce()
        store.putHandshakeOut(phone, nonce)
        sendControlSms(context, phone, "$PREFIX H $me ${keysFingerprint(agree, sign)} $nonce")
    }

    /** SMS בקרה: לא נרשם בשיחות (האפליקציה היא אפליקציית ה-SMS, והמערכת לא רושמת בעצמה). */
    private fun sendControlSms(context: Context, phone: String, text: String) {
        context.getSystemService(android.telephony.SmsManager::class.java).sendTextMessage(phone, null, text, null, null)
    }

    private fun newNonce(): String = ByteArray(8).also(java.security.SecureRandom()::nextBytes).joinToString("") { "%02x".format(it) }

    private fun sha256Hex(text: String): String =
        java.security.MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    /** 16 הבתים הראשונים של SHA-256 על שני המפתחות הציבוריים - נכנס ל-SMS אחד. */
    private fun keysFingerprint(agreeKey: String, signKey: String): String = sha256Hex("$agreeKey|$signKey").take(32)

    private fun phoneHash(phone: String): String = sha256Hex("futureos-chat|$phone")

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
            // נמען שלא רשום בצ'אט - SMS רגיל מיד, בלי לשאול. שגיאה במציאת הנמען או
            // שליחה שנכשלה (timeout) היא כישלון של הצ'אט: שואלים את המשתמש (M06).
            val peer = try {
                resolvePeer(context, phone)
            } catch (e: Exception) {
                Log.w(TAG, "Chat peer lookup failed", e)
                askSmsFallback(context, smsId, address, text)
                return@launch
            }
            if (peer == null) {
                smsFallback()
                return@launch
            }
            val sent = try {
                val id = UUID.randomUUID().toString()
                ChatStore.get(context).putOutgoing(smsId, id, phone, threadIdOf(context, smsId))
                val ok = sendPayload(peer, id, "m", JSONObject().put("t", "m").put("b", text))
                if (ok) markSmsSent(context, smsId) else ChatStore.get(context).remove(smsId)
                ok
            } catch (e: Exception) {
                Log.w(TAG, "Chat send failed", e)
                false
            }
            if (!sent) askSmsFallback(context, smsId, address, text)
        }
        return true
    }

    /** שליחה בצ'אט נכשלה: השורה מסומנת "נכשל", והמשתמש מחליט אם לשלוח כ-SMS (בלי שליחה אוטומטית). */
    private fun askSmsFallback(context: Context, smsId: Long, address: String, text: String) {
        updateSms(context, smsId) { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_FAILED) }
        _smsFallbackAsk.value = SmsFallbackAsk(smsId, address, text)
    }

    /** הודעה שממתינה להחלטת המשתמש: לשלוח כ-SMS אחרי כישלון בצ'אט. */
    data class SmsFallbackAsk(val smsId: Long, val address: String, val text: String)

    private val _smsFallbackAsk = MutableStateFlow<SmsFallbackAsk?>(null)
    val smsFallbackAsk: StateFlow<SmsFallbackAsk?> = _smsFallbackAsk.asStateFlow()

    fun clearSmsFallbackAsk() {
        _smsFallbackAsk.value = null
    }

    /** מזהה השיחה הפתוחה כרגע במסך (null כשאין). הודעות צ'אט שמגיעות לשיחה הזו נקראות מיד. */
    @Volatile
    var openThreadId: Long? = null

    /**
     * תמונה בצ'אט במקום MMS: באיכות מלאה (עד 2048 פיקסלים) ולא דחוסה למגבלת
     * הספק. מחזיר false כשהצ'אט לא רלוונטי - אז הקורא שולח MMS.
     */
    suspend fun trySendImage(context: Context, address: String, caption: String, imageUri: Uri, mmsFallback: () -> Unit): Boolean {
        init(context)
        if (!isActive || !isOnline(context)) return false
        val phone = e164(context, address) ?: return false
        if (knownNotRegistered(context, phone)) return false
        // suspend ולא runBlocking - קודם thread של IO נחסם עד 8 שניות לכל תמונה
        val peer = withTimeoutOrNull(8_000) { runCatching { resolvePeer(context, phone) }.getOrNull() } ?: return false
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

    private fun markThreadReadNow(context: Context, threadId: Long) {
        runCatching {
            context.contentResolver.update(
                Telephony.Sms.CONTENT_URI,
                ContentValues().apply { put(Telephony.Sms.READ, 1) },
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
            )
        }
        onThreadRead(context, threadId)
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

    /**
     * איש קשר מאומת (לחיצת יד ב-SMS), או null - ואז ההודעה הזו יוצאת כ-SMS רגיל.
     * פעם ביום בודקים את הרמז בספר הכתובות: uid אחר (התקנה מחדש אצלו) או רמז
     * שנעלם מבטלים את האימות, כדי שלא נכתוב לתור מת. בלי אימות ועם רמז - לחיצת יד.
     */
    private suspend fun resolvePeer(context: Context, phone: String): Peer? {
        val store = ChatStore.get(context)
        val cached = store.peer(phone)
        val fresh = cached != null && System.currentTimeMillis() - cached.checkedAt < PEER_TTL_MS
        if (fresh && cached!!.registered) return Peer(cached.uid!!, phone, cached.agreeKey!!)
        if (fresh) return null
        val hint = withTimeout(8_000) { ChatBackend.lookupHint(phoneHash(phone)) }
        if (hint == null || hint == ChatBackend.uid()) {
            store.putPeer(phone, null, null, null)
            return null
        }
        if (cached != null && cached.registered && cached.uid == hint) {
            store.putPeer(phone, cached.uid, cached.agreeKey, cached.signKey)
            return Peer(cached.uid!!, phone, cached.agreeKey!!)
        }
        store.removePeer(phone)
        startHandshake(context, phone)
        return null
    }

    private suspend fun sendPayload(peer: Peer, id: String, kind: String, payload: JSONObject): Boolean {
        val me = ChatBackend.uid() ?: return false
        payload.put("id", id).put("ts", System.currentTimeMillis())
        val sealed = ChatCrypto.seal(payload.toString().toByteArray(), peer.agreeKey, "$me>${peer.uid}|$id")
        return try {
            withTimeout(20_000) { ChatBackend.send(peer.uid, id, kind, sealed) }
            true
        } catch (e: Exception) {
            Log.w(TAG, "Send $kind failed", e)
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

    /** ChatService רץ: ההאזנה נשארת פתוחה גם כשהמסך נסגר (ON_PAUSE). */
    @Volatile var keepListening = false

    fun detach() {
        if (keepListening) return
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
        // סגירות לחיצת יד קודם, כדי שהודעה ראשונה מאיש קשר חדש לא תיזרק כ"לא מוכר"
        for (doc in docs.sortedBy { if (it.getString("kind") == "v") 0 else 1 }) {
            try {
                handle(context, me, doc, delivered)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // כשל רשת/שרת זמני: ההודעה נשארת בתור ותטופל בסנכרון הבא - לא נמחקת. רק הודעה
                // שאי אפשר לפענח לעולם (חתימה, פורמט) נזרקת.
                if (e is java.io.IOException || e is com.google.firebase.FirebaseException) {
                    Log.w(TAG, "Keeping message ${doc.id} for retry", e)
                    continue
                }
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
        val peer = store.peerByUid(from)?.takeIf { it.registered }
        if (peer == null) {
            // לא מאומת: רק סגירת לחיצת יד ("v") ממי שענינו לו ב-SMS. כל השאר נזרק -
            // המספר של השולח מגיע רק מלחיצת יד, אף פעם לא מהשרת.
            val pending = store.handshakeInByUid(from) ?: return
            val json = JSONObject(String(ChatCrypto.open(sealed, pending.signKey, contextString)))
            if (json.optString("t") != "v" || json.optString("n") != pending.nonce) return
            store.removeHandshakeIn(pending.phone)
            checkPinnedKey(context, pending.phone, pending.signKey)
            store.putPeer(pending.phone, from, pending.agreeKey, pending.signKey)
            return
        }
        val plaintext = ChatCrypto.open(sealed, peer.signKey!!, contextString)
        val phone = peer.phone
        val json = JSONObject(String(plaintext))
        // המזהה מגיע מהשולח ונכנס לשם קובץ (mediaFile) - רק תווים בטוחים, בלי "../".
        val id = json.optString("id", doc.id).takeIf { SAFE_ID.matches(it) } ?: doc.id
        when (json.optString("t")) {
            "m", "i" -> {
                if (store.hasIncoming(id)) return
                val isImage = json.optString("t") == "i"
                var imagePath: String? = null
                var mediaPath: String? = null
                val caption = if (isImage) json.optString("c") else json.optString("b")
                if (isImage) {
                    // הנתיב מגיע מהשולח: רק מהתיקייה שלנו, אחרת שולח יכל לגרום לנו
                    // להוריד ולמחוק קובץ אחר (downloadAndDelete).
                    val path = json.getString("p")
                    require(path.startsWith("media/$me/") && ".." !in path && SAFE_ID.matches(path.substringAfterLast('/'))) { "Bad media path" }
                    mediaPath = path
                    val encrypted = ChatBackend.download(path)
                    val bytes = ChatCrypto.decryptFile(encrypted, ChatCrypto.FileKey(json.getString("k"), json.getString("v")))
                    imagePath = mediaFile(context, id).apply { writeBytes(bytes) }.absolutePath
                }
                val imageOnly = isImage && caption.isBlank()
                val body = if (imageOnly) IMAGE_PLACEHOLDER else caption
                val smsId = insertSms(context, phone, body, incoming = true) ?: return
                val threadId = threadIdOf(context, smsId)
                store.putIncoming(smsId, id, phone, threadId, imagePath, imageOnly)
                // נשמר אצלנו - עכשיו אפשר למחוק את הקובץ המוצפן מהשרת
                mediaPath?.let { ChatBackend.deleteMedia(it) }
                delivered.getOrPut(phone) { mutableListOf() }.add(id)
                clearTyping(phone)
                // השיחה פתוחה על המסך - ההודעה נקראת מיד, והשולח מקבל "נקרא" (M17).
                if (openThreadId == threadId) markThreadReadNow(context, threadId)
                SmsDeliverReceiver.notifyIncoming(context, phone, body)
            }
            "r" -> {
                val ids = json.optJSONArray("ids") ?: return
                val read = json.optString("k") == "r"
                // קבלה מהשולח - מספר סביר של מזהים (כל אחד הוא שאילתה ועדכון)
                for (i in 0 until minOf(ids.length(), MAX_RECEIPT_IDS)) {
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

    /**
     * נעיצת מפתח (כמו "קוד הביטחון השתנה" ב-Signal): המפתח הראשון של כל מספר
     * נשמר, ואם השרת מחזיר פתאום מפתח אחר - בשיחה מופיעה הודעה עם טביעת האצבע
     * החדשה. בלי זה שרת פרוץ יכול היה להחליף בשקט את המפתח ולקרוא/לזייף הודעות.
     * הנעיצה נשמרת בנפרד מטבלת peers (שמתאפסת כשמספר "לא רשום") וגם אחרי התנתקות.
     */
    private fun checkPinnedKey(context: Context, phone: String, signKey: String) {
        val pins = context.getSharedPreferences("chat_key_pins", Context.MODE_PRIVATE)
        val old = pins.getString(phone, null)
        if (old == signKey) return
        pins.edit().putString(phone, signKey).commit()
        if (old != null) {
            insertSms(
                context, phone,
                "מפתח ההצפנה של איש הקשר הזה השתנה (למשל אחרי התקנה מחדש). " +
                    "אם לא ציפית לזה - ודאו ביניכם את קוד האימות: ${ChatCrypto.fingerprint(signKey)}",
                incoming = true,
            )
        }
    }

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
