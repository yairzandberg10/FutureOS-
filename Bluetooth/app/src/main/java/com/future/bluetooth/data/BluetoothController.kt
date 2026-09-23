package com.future.bluetooth.data

import android.annotation.SuppressLint
import android.bluetooth.BluetoothA2dp
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * עטיפה סביב BluetoothAdapter הקלאסי (סריקה, התאמה, חיבור פרופילי שמע) - לא
 * BLE/GATT (זה קיים ב-Fitness/HeartRateMonitor לחיישני דופק). מטרת
 * האפליקציה היא ניהול כללי של מכשירי בלוטות', כמו מסך הבלוטות' של טלפון
 * רגיל: הפעלה, שם המכשיר, מותאמים וזמינים, חיבור וניתוק.
 *
 * חיבור וניתוק של מכשיר שכבר מותאם, ובחירת הפרופילים שלו, אינם API ציבורי
 * באנדרואיד - הם נקראים כאן ב-reflection, ובמכשיר שחוסם אותם (BLUETOOTH_PRIVILEGED)
 * הפונקציות מחזירות false. הקורא מחליט מה לעשות אז (MainActivity פותח את
 * מסך הבלוטות' של המערכת).
 */
@SuppressLint("MissingPermission") // MainActivity מבקש BLUETOOTH_SCAN/BLUETOOTH_CONNECT לפני כל שימוש כאן
class BluetoothController(private val context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private val aliases = context.getSharedPreferences("device_aliases", Context.MODE_PRIVATE)

    /** מתי כל מכשיר מותאם היה בשימוש לאחרונה - לאנדרואיד אין API לזה. */
    private val lastUsed = context.getSharedPreferences("device_last_used", Context.MODE_PRIVATE)

    /** רושם שימוש במכשיר (חיבור, או כניסה למסך שלו), כדי שיעלה בראש הרשימה. */
    fun markUsed(address: String) {
        lastUsed.edit().putLong(address, System.currentTimeMillis()).apply()
    }

    /**
     * המותאמים לפי רלוונטיות: מחובר, מתחבר, ואחר כך לפי השימוש האחרון.
     * שלושת הראשונים הם מה שמוצג במסך הראשי.
     */
    fun pairedByRelevance(): List<BluetoothDeviceInfo> = pairedDevices.sortedWith(
        compareBy<BluetoothDeviceInfo> {
            when (it.connection) {
                Connection.Connected -> 0
                Connection.Connecting -> 1
                Connection.Disconnected -> 2
            }
        }.thenByDescending { lastUsed.getLong(it.address, 0L) }.thenBy { it.name }
    )

    fun isSupported(): Boolean = adapter != null

    var isEnabled by mutableStateOf(adapter?.isEnabled == true)
        private set

    /** ההדלקה/כיבוי של הרדיו לוקחים שנייה-שתיים; בזמן הזה המתג כבר במצב החדש. */
    var isTurningOn by mutableStateOf(false)
        private set

    var isScanning by mutableStateOf(false)
        private set

    var adapterName by mutableStateOf(readAdapterName())
        private set

    val pairedDevices = mutableStateListOf<BluetoothDeviceInfo>()
    val discoveredDevices = mutableStateListOf<BluetoothDeviceInfo>()

    private var headset: BluetoothHeadset? = null
    private var a2dp: BluetoothA2dp? = null

    /** כתובות עם חיבור ACL פתוח (מכשיר מחובר בכל פרופיל, גם מקלדת/שעון). */
    private val aclConnected = mutableSetOf<String>()
    private val connecting = mutableSetOf<String>()

    /** התאמות שהתחלנו ועוד לא הסתיימו - מוצגות ברשימת המותאמים כ"מתחבר". */
    private val pairing = mutableSetOf<String>()
    private val batteryLevels = mutableMapOf<String, Int>()

    private var receiver: BroadcastReceiver? = null

    private val profileListener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            when (profile) {
                BluetoothProfile.HEADSET -> headset = proxy as BluetoothHeadset
                BluetoothProfile.A2DP -> a2dp = proxy as BluetoothA2dp
            }
            refreshPairedDevices()
        }

        override fun onServiceDisconnected(profile: Int) {
            when (profile) {
                BluetoothProfile.HEADSET -> headset = null
                BluetoothProfile.A2DP -> a2dp = null
            }
        }
    }

    /** נקרא אחרי שהרשאות אושרו ובכל ON_RESUME - המצב יכול להשתנות מחוץ לאפליקציה. */
    fun refreshState() {
        isEnabled = adapter?.isEnabled == true
        adapterName = readAdapterName()
        connectProfiles()
        refreshPairedDevices()
    }

    fun refreshPairedDevices() {
        val bonded = try {
            adapter?.bondedDevices.orEmpty()
        } catch (e: SecurityException) {
            emptySet()
        }
        val bondingNow = pairing.mapNotNull { remoteDevice(it) }
            .filter { it.bondState == BluetoothDevice.BOND_BONDING }
        val list = (bonded + bondingNow).distinctBy { it.address }.map { it.toInfo() }
        pairedDevices.clear()
        pairedDevices.addAll(list)
        // מכשיר שעבר להיות מותאם יוצא מרשימת הזמינים.
        discoveredDevices.removeAll { d -> list.any { it.address == d.address } }
    }

    // ---- הרדיו ----

    /**
     * הפעלה/כיבוי ישירים. targetSdk הוא 31, כך ש-enable()/disable() עדיין
     * זמינים (הם נחסמו רק לאפליקציות שמטרגטות 33 ומעלה). false - הקורא
     * נופל לבקשת ההפעלה של המערכת.
     */
    @Suppress("DEPRECATION")
    fun setEnabled(on: Boolean): Boolean {
        val a = adapter ?: return false
        val ok = try {
            if (on) a.enable() else a.disable()
        } catch (e: SecurityException) {
            false
        }
        if (ok) {
            isEnabled = on
            isTurningOn = on
            if (!on) {
                isScanning = false
                discoveredDevices.clear()
                aclConnected.clear()
                connecting.clear()
            }
        }
        return ok
    }

    fun requestEnableIntent(): Intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)

    fun renameAdapter(name: String): Boolean {
        val ok = try {
            adapter?.setName(name) == true
        } catch (e: SecurityException) {
            false
        }
        if (ok) adapterName = name
        return ok
    }

    private fun readAdapterName(): String = try {
        adapter?.name.orEmpty()
    } catch (e: SecurityException) {
        ""
    }

    // ---- סריקה ----

    fun startDiscovery() {
        val a = adapter ?: return
        if (!a.isEnabled) return
        discoveredDevices.clear()
        if (a.isDiscovering) a.cancelDiscovery()
        isScanning = a.startDiscovery()
    }

    fun stopDiscovery() {
        adapter?.let { if (it.isDiscovering) it.cancelDiscovery() }
        isScanning = false
    }

    // ---- מכשיר ----

    fun pair(address: String): Boolean {
        val device = remoteDevice(address) ?: return false
        // הסריקה מאטה את ההתאמה, ובחלק מהמכשירים מכשילה אותה.
        stopDiscovery()
        val ok = device.createBond()
        if (ok) {
            pairing.add(address)
            refreshPairedDevices()
        }
        return ok
    }

    /** אין API ציבורי לביטול התאמה - removeBond נגיש רק ב-reflection. */
    fun forget(address: String): Boolean {
        val device = remoteDevice(address) ?: return false
        val ok = callHidden(device, "removeBond")
        if (ok) aliases.edit().remove(address).apply()
        return ok
    }

    /** שם מקומי למכשיר. קודם ה-alias של המערכת, ואם היא דוחה - שמירה אצלנו. */
    fun renameDevice(address: String, name: String) {
        val device = remoteDevice(address)
        // ב-reflection ולא בקריאה ישירה: ב-API 31 setAlias מחזירה Boolean, ומ-33
        // Int (BluetoothStatusCodes) - קריאה מקומפלת מול ה-SDK החדש הייתה
        // נופלת ב-NoSuchMethodError על המכשיר (אנדרואיד 12).
        val systemOk = device != null && try {
            when (val result = device.javaClass.getMethod("setAlias", String::class.java).invoke(device, name)) {
                is Boolean -> result
                is Int -> result == ALIAS_SUCCESS
                else -> false
            }
        } catch (e: Exception) {
            false
        }
        if (systemOk) aliases.edit().remove(address).apply() else aliases.edit().putString(address, name).apply()
        refreshPairedDevices()
    }

    /** מחבר את כל הפרופילים שהמכשיר תומך בהם. false אם אף אחד לא הצליח. */
    fun connect(address: String): Boolean {
        val device = remoteDevice(address) ?: return false
        val info = device.toInfo()
        var ok = false
        if (info.supportsCalls) ok = callProfile(headset, "connect", device) || ok
        if (info.supportsMedia) ok = callProfile(a2dp, "connect", device) || ok
        if (ok) {
            connecting.add(address)
            refreshPairedDevices()
        }
        return ok
    }

    fun disconnect(address: String): Boolean {
        val device = remoteDevice(address) ?: return false
        var ok = false
        ok = callProfile(headset, "disconnect", device) || ok
        ok = callProfile(a2dp, "disconnect", device) || ok
        if (ok) refreshPairedDevices()
        return ok
    }

    /** מחבר או מנתק פרופיל אחד (שיחות = HEADSET, מדיה = A2DP). */
    fun setProfile(address: String, calls: Boolean, enabled: Boolean): Boolean {
        val device = remoteDevice(address) ?: return false
        val proxy = if (calls) headset else a2dp
        val ok = callProfile(proxy, if (enabled) "connect" else "disconnect", device)
        if (ok) {
            if (enabled) connecting.add(address)
            refreshPairedDevices()
        }
        return ok
    }

    /** מנתק את כל המחוברים. false אם היה מה לנתק ואף ניתוק לא הצליח. */
    fun disconnectAll(): Boolean {
        val connected = pairedDevices.filter { it.connection != Connection.Disconnected }
        if (connected.isEmpty()) return true
        return connected.map { disconnect(it.address) }.any { it }
    }

    // ---- מחזור חיים ----

    fun register() {
        if (receiver != null) return
        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_NAME_CHANGED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
            addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
            addAction(ACTION_BATTERY_LEVEL_CHANGED)
        }
        val r = object : BroadcastReceiver() {
            @Suppress("DEPRECATION")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val device = intent?.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                when (intent?.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val info = device?.toInfo() ?: return
                        if (info.isBonded || pairedDevices.any { it.address == info.address }) return
                        val index = discoveredDevices.indexOfFirst { it.address == info.address }
                        if (index >= 0) discoveredDevices[index] = info else discoveredDevices.add(info)
                    }
                    BluetoothDevice.ACTION_NAME_CHANGED -> {
                        val info = device?.toInfo() ?: return
                        val index = discoveredDevices.indexOfFirst { it.address == info.address }
                        if (index >= 0) discoveredDevices[index] = info else refreshPairedDevices()
                    }
                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> isScanning = true
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> isScanning = false
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                        isEnabled = state == BluetoothAdapter.STATE_ON || state == BluetoothAdapter.STATE_TURNING_ON
                        if (state == BluetoothAdapter.STATE_ON) {
                            isTurningOn = false
                            adapterName = readAdapterName()
                            connectProfiles()
                            refreshPairedDevices()
                            startDiscovery()
                        } else if (state == BluetoothAdapter.STATE_OFF) {
                            isTurningOn = false
                            isScanning = false
                            aclConnected.clear()
                            connecting.clear()
                            refreshPairedDevices()
                        }
                    }
                    BluetoothAdapter.ACTION_LOCAL_NAME_CHANGED -> adapterName = readAdapterName()
                    BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                        val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.ERROR)
                        val address = device?.address
                        if (address != null && state != BluetoothDevice.BOND_BONDING && pairing.remove(address)) {
                            // התאמה שנכשלה/בוטלה מחזירה את המכשיר לרשימת הזמינים.
                            if (state == BluetoothDevice.BOND_NONE && discoveredDevices.none { it.address == address }) {
                                discoveredDevices.add(device.toInfo())
                            }
                        }
                        refreshPairedDevices()
                    }
                    BluetoothDevice.ACTION_ACL_CONNECTED -> {
                        device?.address?.let { aclConnected.add(it); markUsed(it) }
                        refreshPairedDevices()
                    }
                    BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        device?.address?.let { aclConnected.remove(it); connecting.remove(it) }
                        refreshPairedDevices()
                    }
                    BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED,
                    BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED -> {
                        val address = device?.address ?: return
                        when (intent.getIntExtra(BluetoothProfile.EXTRA_STATE, -1)) {
                            BluetoothProfile.STATE_CONNECTING -> connecting.add(address)
                            else -> connecting.remove(address)
                        }
                        refreshPairedDevices()
                    }
                    ACTION_BATTERY_LEVEL_CHANGED -> {
                        val address = device?.address ?: return
                        val level = intent.getIntExtra(EXTRA_BATTERY_LEVEL, -1)
                        if (level in 0..100) batteryLevels[address] = level else batteryLevels.remove(address)
                        refreshPairedDevices()
                    }
                }
            }
        }
        receiver = r
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(r, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(r, filter)
        }
    }

    fun unregister() {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (e: IllegalArgumentException) {
                // כבר לא רשום - לא קרה כלום
            }
        }
        receiver = null
        headset?.let { adapter?.closeProfileProxy(BluetoothProfile.HEADSET, it) }
        a2dp?.let { adapter?.closeProfileProxy(BluetoothProfile.A2DP, it) }
        headset = null
        a2dp = null
    }

    private fun connectProfiles() {
        val a = adapter ?: return
        if (!a.isEnabled) return
        try {
            if (headset == null) a.getProfileProxy(context, profileListener, BluetoothProfile.HEADSET)
            if (a2dp == null) a.getProfileProxy(context, profileListener, BluetoothProfile.A2DP)
        } catch (e: SecurityException) {
            // בלי BLUETOOTH_CONNECT אין פרופילים; המצב יתעדכן כשההרשאה תאושר.
        }
    }

    // ---- עזרים ----

    private fun remoteDevice(address: String): BluetoothDevice? =
        try {
            adapter?.getRemoteDevice(address)
        } catch (e: IllegalArgumentException) {
            null
        }

    private fun callProfile(proxy: BluetoothProfile?, method: String, device: BluetoothDevice): Boolean {
        proxy ?: return false
        return try {
            proxy.javaClass.getMethod(method, BluetoothDevice::class.java).invoke(proxy, device) as? Boolean ?: false
        } catch (e: Exception) {
            // SecurityException (עטוף ב-InvocationTargetException) במכשיר שדורש
            // BLUETOOTH_PRIVILEGED, או NoSuchMethodException בגרסה שהסירה אותה.
            false
        }
    }

    private fun callHidden(device: BluetoothDevice, method: String): Boolean =
        try {
            device.javaClass.getMethod(method).invoke(device) as? Boolean ?: false
        } catch (e: Exception) {
            false
        }

    private fun connectedOn(proxy: BluetoothProfile?, device: BluetoothDevice): Boolean =
        try {
            proxy?.getConnectionState(device) == BluetoothProfile.STATE_CONNECTED
        } catch (e: SecurityException) {
            false
        }

    private fun batteryOf(device: BluetoothDevice): Int? {
        batteryLevels[device.address]?.let { return it }
        val level = try {
            device.javaClass.getMethod("getBatteryLevel").invoke(device) as? Int
        } catch (e: Exception) {
            null
        }
        return level?.takeIf { it in 0..100 }
    }

    private fun BluetoothDevice.toInfo(): BluetoothDeviceInfo {
        val systemName = try {
            alias ?: name
        } catch (e: SecurityException) {
            null
        }
        val displayName = aliases.getString(address, null) ?: systemName ?: address
        val bonded = bondState == BluetoothDevice.BOND_BONDED
        val bonding = bondState == BluetoothDevice.BOND_BONDING
        val cls = try {
            bluetoothClass
        } catch (e: SecurityException) {
            null
        }
        val kind = kindOf(cls)
        val uuidList: List<ParcelUuid>? = try {
            uuids?.toList()
        } catch (e: SecurityException) {
            null
        }
        val supportsMedia = uuidList?.any { it in MEDIA_UUIDS }
            ?: (cls?.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO)
        val supportsCalls = uuidList?.any { it in CALL_UUIDS }
            ?: (kind == DeviceKind.Headphones || kind == DeviceKind.Car)
        val calls = connectedOn(headset, this)
        val media = connectedOn(a2dp, this)
        val connection = when {
            bonding || address in connecting -> Connection.Connecting
            calls || media || address in aclConnected || callHidden(this, "isConnected") -> Connection.Connected
            else -> Connection.Disconnected
        }
        return BluetoothDeviceInfo(
            name = displayName,
            address = address,
            isBonded = bonded || bonding,
            kind = kind,
            connection = connection,
            battery = if (connection == Connection.Connected) batteryOf(this) else null,
            supportsCalls = supportsCalls,
            supportsMedia = supportsMedia,
            callsConnected = calls,
            mediaConnected = media,
        )
    }

    private fun kindOf(cls: BluetoothClass?): DeviceKind {
        cls ?: return DeviceKind.Other
        return when (cls.majorDeviceClass) {
            BluetoothClass.Device.Major.AUDIO_VIDEO -> when (cls.deviceClass) {
                BluetoothClass.Device.AUDIO_VIDEO_CAR_AUDIO -> DeviceKind.Car
                BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER,
                BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO,
                BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO -> DeviceKind.Speaker
                else -> DeviceKind.Headphones
            }
            BluetoothClass.Device.Major.PHONE -> DeviceKind.Phone
            BluetoothClass.Device.Major.COMPUTER -> DeviceKind.Computer
            BluetoothClass.Device.Major.WEARABLE -> DeviceKind.Watch
            BluetoothClass.Device.Major.PERIPHERAL ->
                if (cls.deviceClass and PERIPHERAL_KEYBOARD_BIT != 0) DeviceKind.Keyboard else DeviceKind.Mouse
            else -> DeviceKind.Other
        }
    }

    private companion object {
        /** BluetoothStatusCodes.SUCCESS - הערך ש-setAlias מחזירה כשהצליחה (API 33+). */
        const val ALIAS_SUCCESS = 0

        /** ביט המקלדת במחלקת ציוד היקפי (PERIPHERAL_KEYBOARD = 0x0540). */
        const val PERIPHERAL_KEYBOARD_BIT = 0x40

        /** ACTION_BATTERY_LEVEL_CHANGED - מוסתר ב-SDK, אבל משודר לכל מי שמחזיק BLUETOOTH_CONNECT. */
        const val ACTION_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
        const val EXTRA_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

        val MEDIA_UUIDS = setOf(
            ParcelUuid.fromString("0000110B-0000-1000-8000-00805F9B34FB"), // A2DP sink
            ParcelUuid.fromString("0000110D-0000-1000-8000-00805F9B34FB"), // Advanced audio
        )
        val CALL_UUIDS = setOf(
            ParcelUuid.fromString("0000111E-0000-1000-8000-00805F9B34FB"), // Handsfree
            ParcelUuid.fromString("00001108-0000-1000-8000-00805F9B34FB"), // Headset
        )
    }
}
