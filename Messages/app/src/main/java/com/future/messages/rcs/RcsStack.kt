package com.future.messages.rcs

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.PhoneNumberUtils
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import com.future.messages.receiver.SmsDeliverReceiver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import kotlin.coroutines.resume

/**
 * לקוח RCS (GSMA Universal Profile, הודעות עצמאיות במצב Pager) מעל רישום
 * ה-IMS של המכשיר.
 *
 * איך זה עובד: אנדרואיד (12+) מאפשר לאפליקציית ההודעות של המערכת לבקש
 * "SipDelegate" - ערוץ SIP שרוכב על הרישום הקיים של המודם ל-IMS, עם תגי
 * השירות של RCS. המודם/ImsService מטפל ברישום, באבטחה (IPSec) ובתעבורה;
 * האפליקציה היא ה-User Agent: בונה בקשות MESSAGE, עונה לבקשות נכנסות,
 * ומנהלת את גוף ה-CPIM ואת אישורי המסירה/הקריאה (IMDN).
 *
 * מה צריך כדי שזה יעבוד בפועל:
 *  - ההרשאה PERFORM_IMS_SINGLE_REGISTRATION. ב-Android 12 היא ניתנת רק
 *    ל-role של SYSTEM_SHELL, ולכן תמונת FutureOS מסמנת אותה privileged
 *    (ראו Messages/aosp/README.md).
 *  - ImsService של היצרן שתומך ב-SipTransport (single registration).
 *  - ספק סלולרי שמקצה RCS ברשת ה-IMS שלו.
 * כשאחד מהם חסר - המצב נשאר UNAVAILABLE וכל ההודעות יוצאות כ-SMS, בלי שום
 * שינוי בהתנהגות.
 */
object RcsStack {
    private const val TAG = "RcsStack"
    private const val PERMISSION = "android.permission.PERFORM_IMS_SINGLE_REGISTRATION"
    private const val RESPONSE_TIMEOUT_MS = 20_000L
    private const val CAPABILITY_TIMEOUT_MS = 6_000L
    private const val RCS_CAPABILITY_TTL_MS = 24 * 60 * 60 * 1000L
    private const val RETRY_DELAY_MS = 60_000L
    /** מעבר לזה במצב Pager צריך MSRP (Large Message Mode) - שולחים כ-SMS. */
    private const val MAX_PAGER_BYTES = 1300

    enum class State { UNAVAILABLE, CONNECTING, READY }

    private val _state = MutableStateFlow(State.UNAVAILABLE)
    val state: StateFlow<State> = _state

    private val executor = Executors.newSingleThreadExecutor { Thread(it, "rcs-sip") }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val pending = ConcurrentHashMap<String, CompletableDeferred<Int>>()

    private lateinit var appContext: Context
    private var subId = SubscriptionManager.INVALID_SUBSCRIPTION_ID
    private var manager: Any? = null
    @Volatile private var connection: Any? = null
    @Volatile private var config: ImsSipConfig? = null
    @Volatile private var msgTagRegistered = false
    @Volatile private var started = false

    fun isPermitted(context: Context): Boolean =
        context.checkSelfPermission(PERMISSION) == PackageManager.PERMISSION_GRANTED

    /**
     * מנסה לפתוח את ה-delegate. מחזיר false אם אין בכלל תמיכה במכשיר/ספק -
     * אז אין טעם להחזיק שירות רץ.
     */
    @Synchronized
    fun start(context: Context): Boolean {
        appContext = context.applicationContext
        if (started) return true
        if (!isPermitted(appContext)) {
            Log.i(TAG, "RCS off: $PERMISSION not granted")
            return false
        }
        subId = SubscriptionManager.getDefaultSmsSubscriptionId().takeIf { it != SubscriptionManager.INVALID_SUBSCRIPTION_ID }
            ?: SubscriptionManager.getDefaultSubscriptionId()
        if (subId == SubscriptionManager.INVALID_SUBSCRIPTION_ID) {
            Log.i(TAG, "RCS off: no active subscription")
            return false
        }
        val mgr = ImsApi.sipDelegateManager(appContext, subId)
        if (mgr == null || !ImsApi.isSupported(mgr)) {
            Log.i(TAG, "RCS off: IMS single registration not supported on sub $subId")
            return false
        }
        manager = mgr
        _state.value = State.CONNECTING
        started = ImsApi.createSipDelegate(mgr, setOf(Sip.FEATURE_TAG_CPM_MSG), executor, listener)
        if (!started) _state.value = State.UNAVAILABLE
        return started
    }

    @Synchronized
    fun stop() {
        val mgr = manager
        val conn = connection
        if (mgr != null && conn != null) ImsApi.destroySipDelegate(mgr, conn)
        reset()
    }

    private fun reset() {
        connection = null
        config = null
        msgTagRegistered = false
        started = false
        _state.value = State.UNAVAILABLE
        pending.values.forEach { it.complete(-1) }
        pending.clear()
    }

    private fun updateReady() {
        _state.value = when {
            connection != null && config != null && msgTagRegistered -> State.READY
            started -> State.CONNECTING
            else -> State.UNAVAILABLE
        }
        Log.i(TAG, "RCS state: ${_state.value}")
    }

    private val listener = object : ImsApi.DelegateListener {
        override fun onCreated(connection: Any) {
            this@RcsStack.connection = connection
            updateReady()
        }

        override fun onFeatureTagsChanged(registered: Set<String>, denied: Set<String>) {
            msgTagRegistered = registered.any { it.contains("oma.cpm.msg") }
            if (denied.isNotEmpty()) Log.w(TAG, "Denied feature tags: $denied")
            updateReady()
        }

        override fun onConfigurationChanged(config: ImsSipConfig) {
            this@RcsStack.config = config
            updateReady()
        }

        override fun onDestroyed(reason: Int) {
            Log.w(TAG, "SipDelegate destroyed, reason=$reason")
            val retry = started && reason != 2 /* REQUESTED_BY_APP */
            reset()
            // השירות של המודם נפל / המנוי התחלף - מנסים שוב אחרי דקה.
            if (retry) scope.launch { delay(RETRY_DELAY_MS); start(appContext) }
        }

        override fun onMessageReceived(message: SipMessageData) = handleIncoming(message)

        override fun onMessageSent(viaTransactionId: String) = Unit

        override fun onMessageSendFailure(viaTransactionId: String, reason: Int) {
            Log.w(TAG, "SIP send failure $viaTransactionId reason=$reason")
            pending.remove(viaTransactionId)?.complete(-reason.coerceAtLeast(1))
        }
    }

    // ---------------------------------------------------------------- שליחה

    /**
     * נקרא מ-SmsRepository עם שורה שכבר נרשמה ב-content://sms כ-OUTBOX.
     * אם RCS לא מוכן, או שהנמען לא תומך, או שהשליחה נכשלה - smsFallback
     * שולח את אותה שורה כ-SMS רגיל. מחזיר false כשאין בכלל ניסיון RCS
     * (והקורא צריך לשלוח SMS מיד).
     */
    fun trySend(context: Context, smsId: Long, address: String, text: String, smsFallback: () -> Unit): Boolean {
        if (_state.value != State.READY) return false
        if (text.toByteArray(Charsets.UTF_8).size > MAX_PAGER_BYTES) return false
        val number = toE164(context, address) ?: return false
        if (RcsStore.get(context).capability(number, RCS_CAPABILITY_TTL_MS) == false) return false
        scope.launch {
            val sent = try {
                deliver(context, smsId, address, number, text)
            } catch (e: Exception) {
                Log.e(TAG, "RCS send crashed", e)
                false
            }
            if (!sent) smsFallback()
        }
        return true
    }

    private suspend fun deliver(context: Context, smsId: Long, address: String, number: String, text: String): Boolean {
        val store = RcsStore.get(context)
        if (store.capability(number, RCS_CAPABILITY_TTL_MS) == null) {
            val capable = queryCapability(number)
            if (capable == false) {
                store.putCapability(number, false)
                return false
            }
        }
        val imdnId = Sip.uuid()
        store.putOutgoing(smsId, imdnId, address, threadIdOf(context, smsId))
        val code = sendRequest(number, Cpim.buildText(imdnId, text))
        Log.i(TAG, "RCS MESSAGE to $number -> $code")
        return if (code in 200..299) {
            store.putCapability(number, true)
            updateSms(context, smsId) { put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_SENT) }
            true
        } else {
            store.remove(smsId)
            // 4xx/6xx מהרשת = הנמען לא רשום ל-RCS. כשל מקומי (שלילי) או 5xx
            // זמני - לא מסמנים את המספר, רק שולחים הפעם כ-SMS.
            if (code in 400..499 || code in 600..699) store.putCapability(number, false)
            false
        }
    }

    private suspend fun queryCapability(number: String): Boolean? = withTimeoutOrNull(CAPABILITY_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            ImsApi.requestCapabilities(appContext, subId, Uri.parse("tel:$number"), executor) { result ->
                if (cont.isActive) cont.resume(result)
            }
        }
    }

    /** שולח MESSAGE ומחכה לתשובה סופית. קוד SIP, או שלילי בכשל מקומי. */
    private suspend fun sendRequest(number: String, cpim: ByteArray): Int {
        val conn = connection ?: return -1
        val cfg = config ?: return -1
        val conversationId = UUID.nameUUIDFromBytes(number.toByteArray()).toString()
        val request = try {
            Sip.buildMessageRequest(cfg, "tel:$number", cpim, conversationId, Sip.uuid())
        } catch (e: Exception) {
            Log.e(TAG, "Cannot build MESSAGE", e)
            return -1
        }
        val result = CompletableDeferred<Int>()
        pending[request.branch] = result
        if (!ImsApi.send(conn, request.message, cfg.version)) {
            pending.remove(request.branch)
            return -1
        }
        return withTimeoutOrNull(RESPONSE_TIMEOUT_MS) { result.await() }
            ?: run { pending.remove(request.branch); 408 }
    }

    // ---------------------------------------------------------------- קבלה

    private fun handleIncoming(message: SipMessageData) {
        val conn = connection ?: return
        val sip = Sip.parse(message)
        val branch = sip.branch ?: ""
        try {
            if (sip.isResponse) {
                if (sip.statusCode >= 200) pending.remove(branch)?.complete(sip.statusCode)
                return
            }
            val reply = when (sip.method) {
                "MESSAGE" -> {
                    onIncomingMessage(sip)
                    Sip.buildResponse(sip, 200, "OK")
                }
                "ACK" -> null
                "OPTIONS" -> Sip.buildResponse(sip, 200, "OK")
                // שיחת צ'אט (INVITE + MSRP) עוד לא נתמכת - דחייה מסודרת גורמת
                // לשולח לעבור להודעות עצמאיות.
                "INVITE" -> Sip.buildResponse(sip, 488, "Not Acceptable Here")
                else -> Sip.buildResponse(sip, 501, "Not Implemented")
            }
            reply?.let { config?.let { cfg -> ImsApi.send(conn, it, cfg.version) } }
        } catch (e: Exception) {
            Log.e(TAG, "Failed handling incoming ${sip.startLine}", e)
        } finally {
            if (branch.isNotEmpty()) ImsApi.notifyReceived(conn, branch)
        }
    }

    private fun onIncomingMessage(sip: Sip.Parsed) {
        val context = appContext
        val sender = (sip.first("P-Asserted-Identity")?.split(',')?.firstNotNullOfOrNull { Sip.numberFromUri(it) }
            ?: sip.first("From")?.let { Sip.numberFromUri(it) }) ?: return
        val contentType = sip.first("Content-Type")?.substringBefore(';')?.trim()?.lowercase() ?: ""
        val cpim = if (contentType == "message/cpim") Cpim.parse(sip.body) ?: return else null
        val innerType = cpim?.contentType ?: contentType
        val content = cpim?.content ?: sip.body

        when (innerType) {
            Cpim.IMDN_XML -> Cpim.parseImdn(String(content, Charsets.UTF_8))?.let { onNotification(context, it) }
            Cpim.TEXT_PLAIN, Cpim.FT_HTTP_XML -> {
                val body = if (innerType == Cpim.FT_HTTP_XML) {
                    val (name, url) = Cpim.parseFileTransfer(String(content, Charsets.UTF_8))
                    listOfNotNull("קובץ: ${name ?: "ללא שם"}", url).joinToString("\n")
                } else String(content, Charsets.UTF_8)
                val store = RcsStore.get(context)
                val imdnId = cpim?.messageId
                if (imdnId != null && store.hasIncoming(imdnId)) return // שידור חוזר של הודעה שכבר נשמרה
                val smsId = insertIncoming(context, sender, body) ?: return
                val dispositions = cpim?.dispositionNotification ?: emptySet()
                if (imdnId != null) {
                    store.putIncoming(smsId, imdnId, sender, threadIdOf(context, smsId), cpim.dateTime, "display" in dispositions)
                    if ("positive-delivery" in dispositions) {
                        sendNotification(sender, imdnId, cpim.dateTime, Cpim.Disposition.DELIVERED)
                    }
                }
                SmsDeliverReceiver.notifyIncoming(context, sender, body)
            }
            else -> Log.i(TAG, "Unsupported RCS content $innerType from $sender")
        }
    }

    private fun onNotification(context: Context, notification: Cpim.Notification) {
        val store = RcsStore.get(context)
        val smsId = store.outgoingSmsId(notification.messageId) ?: return
        when {
            notification.disposition == Cpim.Disposition.DISPLAYED -> {
                store.markRead(smsId)
                updateSms(context, smsId) { put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE) }
            }
            notification.disposition == Cpim.Disposition.DELIVERED ->
                updateSms(context, smsId) { put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_COMPLETE) }
            notification.failed ->
                updateSms(context, smsId) { put(Telephony.Sms.STATUS, Telephony.Sms.STATUS_FAILED) }
        }
    }

    /** נקרא כשהמשתמש פותח שיחה - שולח אישורי קריאה למי שביקש. */
    fun onThreadRead(context: Context, threadId: Long) {
        if (_state.value != State.READY) return
        scope.launch {
            RcsStore.get(context).takeDisplayPending(threadId).forEach {
                sendNotification(it.address, it.imdnId, it.dateTime, Cpim.Disposition.DISPLAYED)
            }
        }
    }

    private fun sendNotification(address: String, imdnId: String, dateTime: String?, disposition: Cpim.Disposition) {
        val number = toE164(appContext, address) ?: return
        val xml = Cpim.imdnXml(imdnId, dateTime, disposition)
        scope.launch { sendRequest(number, Cpim.buildImdn(Sip.uuid(), xml)) }
    }

    // ---------------------------------------------------------------- ספק ה-SMS

    private fun insertIncoming(context: Context, address: String, body: String): Long? = try {
        val values = ContentValues().apply {
            put(Telephony.Sms.ADDRESS, address)
            put(Telephony.Sms.BODY, body)
            put(Telephony.Sms.DATE, System.currentTimeMillis())
            put(Telephony.Sms.READ, 0)
            put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
        }
        context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)?.let { ContentUris.parseId(it) }
    } catch (e: Exception) {
        Log.e(TAG, "Cannot store incoming RCS message", e)
        null
    }

    private fun threadIdOf(context: Context, smsId: Long): Long = try {
        context.contentResolver.query(
            ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), arrayOf(Telephony.Sms.THREAD_ID), null, null, null
        )?.use { if (it.moveToFirst()) it.getLong(0) else 0L } ?: 0L
    } catch (e: Exception) {
        0L
    }

    private fun updateSms(context: Context, smsId: Long, block: ContentValues.() -> Unit) {
        try {
            context.contentResolver.update(
                ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, smsId), ContentValues().apply(block), null, null
            )
        } catch (e: Exception) {
            Log.e(TAG, "Cannot update sms $smsId", e)
        }
    }

    /** "050-1234567" → "+972501234567", לפי מדינת ה-SIM. */
    private fun toE164(context: Context, address: String): String? {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val iso = (tm?.simCountryIso?.takeIf { it.isNotEmpty() } ?: tm?.networkCountryIso ?: "il").uppercase()
        return PhoneNumberUtils.formatNumberToE164(address, iso)
            ?: address.filter { it.isDigit() || it == '+' }.takeIf { it.startsWith("+") && it.length > 8 }
    }
}
