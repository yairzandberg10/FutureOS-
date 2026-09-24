package com.future.messages.rcs

import android.content.Context
import android.net.Uri
import android.telephony.ims.ImsManager
import android.util.Log
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.Executor

/**
 * גשר ל-System API של IMS (SipDelegateManager, RcsUceAdapter) - הממשקים שדרכם
 * אפליקציית ההודעות של המערכת משתמשת ברישום ה-IMS של המודם כדי לדבר RCS.
 *
 * הם @SystemApi ולכן לא קיימים ב-android.jar של ה-SDK - הכל כאן ב-reflection,
 * וה-callbacks (ממשקים) נבנים ב-java.lang.reflect.Proxy. זה גם מה שמאפשר לתמוך
 * בשתי הגרסאות: ב-Android 12 ההגדרות מגיעות ב-onImsConfigurationChanged
 * (SipDelegateImsConfiguration), ומ-Android 13 ב-onConfigurationChanged
 * (SipDelegateConfiguration).
 *
 * כל כישלון (אין הרשאה, API חסום לאפליקציה שאינה של המערכת, אין ImsService
 * שתומך) מוחזר כ-null/false - המשמעות היא "אין RCS", והשליחה נופלת ל-SMS.
 */
internal object ImsApi {
    private const val TAG = "RcsImsApi"

    fun sipDelegateManager(context: Context, subId: Int): Any? = try {
        val ims = context.getSystemService(ImsManager::class.java) ?: return null
        ims.javaClass.getMethod("getSipDelegateManager", Int::class.javaPrimitiveType).invoke(ims, subId)
    } catch (e: Throwable) {
        Log.w(TAG, "SipDelegateManager unavailable: ${e.rootMessage()}")
        null
    }

    fun isSupported(manager: Any): Boolean = try {
        manager.javaClass.getMethod("isSupported").invoke(manager) as Boolean
    } catch (e: Throwable) {
        Log.w(TAG, "isSupported failed: ${e.rootMessage()}")
        false
    }

    interface DelegateListener {
        fun onCreated(connection: Any)
        fun onFeatureTagsChanged(registered: Set<String>, denied: Set<String>)
        fun onConfigurationChanged(config: ImsSipConfig)
        fun onDestroyed(reason: Int)
        fun onMessageReceived(message: SipMessageData)
        fun onMessageSent(viaTransactionId: String)
        fun onMessageSendFailure(viaTransactionId: String, reason: Int)
    }

    fun createSipDelegate(manager: Any, featureTags: Set<String>, executor: Executor, listener: DelegateListener): Boolean = try {
        val requestClass = Class.forName("android.telephony.ims.DelegateRequest")
        val request = requestClass.getConstructor(Set::class.java).newInstance(featureTags)
        val stateClass = Class.forName("android.telephony.ims.stub.DelegateConnectionStateCallback")
        val messageClass = Class.forName("android.telephony.ims.stub.DelegateConnectionMessageCallback")
        val stateCallback = proxy(stateClass) { name, args ->
            when (name) {
                "onCreated" -> listener.onCreated(args[0]!!)
                "onFeatureTagStatusChanged" -> listener.onFeatureTagsChanged(
                    registeredTags(args[0]), deniedTags(args[1])
                )
                "onImsConfigurationChanged", "onConfigurationChanged" ->
                    readConfig(args[0]!!)?.let(listener::onConfigurationChanged)
                "onDestroyed" -> listener.onDestroyed(args[0] as Int)
            }
        }
        val messageCallback = proxy(messageClass) { name, args ->
            when (name) {
                "onMessageReceived" -> listener.onMessageReceived(readMessage(args[0]!!))
                "onMessageSent" -> listener.onMessageSent(args[0] as String)
                "onMessageSendFailure" -> listener.onMessageSendFailure(args[0] as String, args[1] as Int)
            }
        }
        manager.javaClass.getMethod(
            "createSipDelegate", requestClass, Executor::class.java, stateClass, messageClass
        ).invoke(manager, request, executor, stateCallback, messageCallback)
        true
    } catch (e: Throwable) {
        Log.w(TAG, "createSipDelegate failed: ${e.rootMessage()}")
        false
    }

    fun destroySipDelegate(manager: Any, connection: Any) {
        try {
            val connClass = Class.forName("android.telephony.ims.SipDelegateConnection")
            manager.javaClass.getMethod("destroySipDelegate", connClass, Int::class.javaPrimitiveType)
                .invoke(manager, connection, 2 /* SIP_DELEGATE_DESTROY_REASON_REQUESTED_BY_APP */)
        } catch (e: Throwable) {
            Log.w(TAG, "destroySipDelegate failed: ${e.rootMessage()}")
        }
    }

    /** שולח הודעת SIP דרך ה-delegate. configVersion חייב להיות הגרסה האחרונה
     * שהתקבלה, אחרת ה-ImsService דוחה ב-STALE_IMS_CONFIGURATION. */
    fun send(connection: Any, message: SipMessageData, configVersion: Long): Boolean = try {
        val sipClass = Class.forName("android.telephony.ims.SipMessage")
        val sip = sipClass.getConstructor(String::class.java, String::class.java, ByteArray::class.java)
            .newInstance(message.startLine, message.headerSection, message.content)
        connection.javaClass.getMethod("sendMessage", sipClass, Long::class.javaPrimitiveType)
            .invoke(connection, sip, configVersion)
        true
    } catch (e: Throwable) {
        Log.w(TAG, "sendMessage failed: ${e.rootMessage()}")
        false
    }

    fun notifyReceived(connection: Any, viaTransactionId: String) {
        invokeQuietly(connection, "notifyMessageReceived", viaTransactionId)
    }

    fun notifyReceiveError(connection: Any, viaTransactionId: String, reason: Int) {
        try {
            connection.javaClass.getMethod("notifyMessageReceiveError", String::class.java, Int::class.javaPrimitiveType)
                .invoke(connection, viaTransactionId, reason)
        } catch (e: Throwable) {
            Log.w(TAG, "notifyMessageReceiveError failed: ${e.rootMessage()}")
        }
    }

    /**
     * בדיקת יכולות (UCE) של מספר - דרך ה-ImsService (OPTIONS או Presence, לפי
     * הספק). מחזיר true/false כשיש תשובה, null כשאין מנגנון או שהבקשה נכשלה.
     */
    fun requestCapabilities(context: Context, subId: Int, contact: Uri, executor: Executor, onResult: (Boolean?) -> Unit) {
        try {
            val ims = context.getSystemService(ImsManager::class.java) ?: return onResult(null)
            val rcs = ims.getImsRcsManager(subId)
            val uce = rcs.javaClass.getMethod("getUceAdapter").invoke(rcs)!!
            val callbackClass = Class.forName("android.telephony.ims.RcsUceAdapter\$CapabilitiesCallback")
            var answered = false
            val callback = proxy(callbackClass) { name, args ->
                when (name) {
                    "onCapabilitiesReceived" -> {
                        val caps = args[0] as List<*>
                        if (!answered && caps.isNotEmpty()) {
                            answered = true
                            onResult(caps.any { supportsMessaging(it!!) })
                        }
                    }
                    "onComplete" -> if (!answered) { answered = true; onResult(null) }
                    "onError" -> if (!answered) { answered = true; onResult(null) }
                }
            }
            uce.javaClass.getMethod("requestAvailability", Uri::class.java, Executor::class.java, callbackClass)
                .invoke(uce, contact, executor, callback)
        } catch (e: Throwable) {
            Log.w(TAG, "requestCapabilities failed: ${e.rootMessage()}")
            onResult(null)
        }
    }

    private fun supportsMessaging(capability: Any): Boolean {
        val cls = capability.javaClass
        val result = cls.getMethod("getRequestResult").invoke(capability) as Int
        if (result != 3 /* REQUEST_RESULT_FOUND */) return false
        val mechanism = cls.getMethod("getCapabilityMechanism").invoke(capability) as Int
        return if (mechanism == 2 /* CAPABILITY_MECHANISM_OPTIONS */) {
            @Suppress("UNCHECKED_CAST")
            val tags = cls.getMethod("getFeatureTags").invoke(capability) as Set<String>
            tags.any { it.contains("oma.cpm.msg") || it.contains("oma.cpm.session") }
        } else {
            val tuples = cls.getMethod("getCapabilityTuples").invoke(capability) as List<*>
            tuples.any { tuple ->
                val id = tuple!!.javaClass.getMethod("getServiceId").invoke(tuple) as String
                id == "org.openmobilealliance:StandaloneMsg" || id.startsWith("org.openmobilealliance:ChatSession") ||
                    id == "org.openmobilealliance:IM-session"
            }
        }
    }

    private fun readMessage(sip: Any): SipMessageData {
        val cls = sip.javaClass
        return SipMessageData(
            startLine = cls.getMethod("getStartLine").invoke(sip) as String,
            headerSection = cls.getMethod("getHeaderSection").invoke(sip) as String,
            content = cls.getMethod("getContent").invoke(sip) as ByteArray,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun registeredTags(state: Any?): Set<String> = try {
        state!!.javaClass.getMethod("getRegisteredFeatureTags").invoke(state) as Set<String>
    } catch (e: Throwable) {
        emptySet()
    }

    private fun deniedTags(states: Any?): Set<String> = try {
        (states as Set<*>).mapTo(HashSet()) { it!!.javaClass.getMethod("getFeatureTag").invoke(it) as String }
    } catch (e: Throwable) {
        emptySet()
    }

    private fun readConfig(config: Any): ImsSipConfig? = try {
        val cls = config.javaClass
        if (cls.name.endsWith("SipDelegateConfiguration")) {
            fun str(name: String) = cls.getMethod(name).invoke(config) as String?
            val local = cls.getMethod("getLocalAddress").invoke(config) as java.net.InetSocketAddress
            val server = cls.getMethod("getSipServerAddress").invoke(config) as java.net.InetSocketAddress
            val transport = cls.getMethod("getTransportType").invoke(config) as Int
            ImsSipConfig(
                version = cls.getMethod("getVersion").invoke(config) as Long,
                transport = if (transport == 1) "TCP" else "UDP",
                publicUserId = str("getPublicUserIdentifier"),
                homeDomain = str("getHomeDomain"),
                localIp = local.address.hostAddress ?: "",
                localPort = local.port,
                serverIp = server.address.hostAddress ?: "",
                serverPort = server.port,
                serviceRoute = str("getSipServiceRouteHeader"),
                associatedUri = str("getSipAssociatedUriHeader"),
                userAgent = str("getSipUserAgentHeader"),
                accessNetworkInfo = str("getSipPaniHeader"),
                contactUser = str("getSipContactUserParameter"),
                publicGruu = (cls.getMethod("getPublicGruuUri").invoke(config) as Uri?)?.toString(),
            )
        } else {
            fun str(key: String) = cls.getMethod("getString", String::class.java).invoke(config, key) as String?
            fun int(key: String) = cls.getMethod("getInt", String::class.java, Int::class.javaPrimitiveType).invoke(config, key, -1) as Int
            ImsSipConfig(
                version = cls.getMethod("getVersion").invoke(config) as Long,
                transport = str("sip_config_protocol_type_string") ?: "UDP",
                publicUserId = str("sip_config_ue_public_user_id_string"),
                homeDomain = str("sip_config_home_domain_string"),
                localIp = str("sip_config_ue_default_ipaddress_string") ?: "",
                localPort = int("sip_config_ue_default_port_int"),
                serverIp = str("sip_config_server_default_ipaddress_string") ?: "",
                serverPort = int("sip_config_server_default_port_int"),
                serviceRoute = str("sip_config_service_route_header_string"),
                associatedUri = str("sip_config_p_associated_uri_header_string"),
                userAgent = str("sip_config_sip_user_agent_header_string"),
                accessNetworkInfo = str("sip_config_p_access_network_info_header_string"),
                contactUser = str("sip_config_uri_user_part_string"),
                publicGruu = str("sip_config_ue_public_gruu_string"),
            )
        }
    } catch (e: Throwable) {
        Log.w(TAG, "Unreadable IMS configuration: ${e.rootMessage()}")
        null
    }

    private fun invokeQuietly(target: Any, method: String, arg: String) {
        try {
            target.javaClass.getMethod(method, String::class.java).invoke(target, arg)
        } catch (e: Throwable) {
            Log.w(TAG, "$method failed: ${e.rootMessage()}")
        }
    }

    /** מימוש ממשק callback של המערכת. מתודות Object מטופלות כאן, מתודות default
     * שלא מעניינות אותנו פשוט לא עושות כלום. */
    private fun proxy(iface: Class<*>, handler: (String, Array<Any?>) -> Unit): Any {
        lateinit var self: Any
        self = Proxy.newProxyInstance(iface.classLoader, arrayOf(iface), InvocationHandler { _, method: Method, args ->
            when (method.name) {
                "hashCode" -> System.identityHashCode(self)
                "equals" -> self === args?.get(0)
                "toString" -> "FutureRcs\$${iface.simpleName}"
                else -> {
                    try {
                        handler(method.name, args ?: emptyArray())
                    } catch (e: Throwable) {
                        Log.e(TAG, "Callback ${method.name} failed", e)
                    }
                    null
                }
            }
        })
        return self
    }

    private fun Throwable.rootMessage(): String {
        var t: Throwable = this
        while (t.cause != null && t.cause !== t) t = t.cause!!
        return "${t.javaClass.simpleName}: ${t.message}"
    }
}

/** ההגדרות שה-ImsService מוסר ל-delegate: הזהות הרשומה, הכתובות וה-Route. */
internal data class ImsSipConfig(
    val version: Long,
    val transport: String,
    val publicUserId: String?,
    val homeDomain: String?,
    val localIp: String,
    val localPort: Int,
    val serverIp: String,
    val serverPort: Int,
    val serviceRoute: String?,
    val associatedUri: String?,
    val userAgent: String?,
    val accessNetworkInfo: String?,
    val contactUser: String?,
    val publicGruu: String?,
)

internal class SipMessageData(val startLine: String, val headerSection: String, val content: ByteArray)
