package com.future.fitness.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.UUID

data class FoundDevice(val name: String, val address: String)

enum class HrConnectionState { DISCONNECTED, SCANNING, CONNECTING, CONNECTED }

data class RunningSpeedCadenceReading(val speedKmh: Double, val cadenceSpm: Int, val strideLengthM: Double?)
data class CyclingPowerReading(val watts: Int, val crank: CrankSample?)
/** מונה סיבובי-קראנק מצטבר + חותמת הזמן של האירוע האחרון (1/1024 שנייה) -
 * שני השדות "הגולמיים" ש-Cycling Power Measurement שולח; קצב-הדיווש עצמו
 * מחושב רק מההפרש בין שתי דגימות עוקבות (ראו cadenceRpmFromCrankSamples). */
data class CrankSample(val cumulativeRevolutions: Int, val eventTime1024ths: Int)

/** לקוח BLE GATT סטנדרטי ל-Heart Rate Service (0x180D) / Heart Rate
 * Measurement (0x2A37) - הפרוטוקול הסטנדרטי שכל שעון חכם/רצועת דופק תומכת
 * בו (Bluetooth SIG), לא SDK ספציפי ליצרן. פענוח הערך לפי המפרט: ביט 0
 * בדגלים קובע פורמט UINT8/UINT16. שומר את המכשיר האחרון שהתחבר אליו כדי
 * להתחבר אוטומטית בפעם הבאה (WorkoutStore.getPairedDevice).
 *
 * מלבד דופק, אותו חיבור BLE יחיד לשעון/חיישן גם מגלה ונרשם (אם קיימים על
 * המכשיר המחובר) לשני שירותי Bluetooth SIG סטנדרטיים נוספים - Running Speed
 * and Cadence (0x1814) ו-Cycling Power (0x1818) - כדי לספק קצב-צעדים/מהירות/
 * אורך-צעד למסך דינמיקת הריצה, ווואטים/קצב-דיווש למסך הרכיבה. אין כאן תמיכה
 * ב"עוצמת ריצה", זמן מגע עם הקרקע או תנודה אנכית - אלה לא חלק מאף שירות
 * סטנדרטי של Bluetooth SIG (יצרנים כמו Stryd/Garmin חושפים אותם רק דרך
 * שירותי GATT קנייניים משלהם), ולכן לא ניתן לתמוך בהם באופן גנרי כאן. */
@SuppressLint("MissingPermission") // הקוד הקורא אחראי לבקש BLUETOOTH_SCAN/BLUETOOTH_CONNECT לפני שימוש
class HeartRateMonitor(private val context: Context) {
    var state by mutableStateOf(HrConnectionState.DISCONNECTED)
        private set
    var connectedDeviceName by mutableStateOf<String?>(null)
        private set
    var currentBpm by mutableStateOf<Int?>(null)
        private set
    val foundDevices = mutableStateListOf<FoundDevice>()

    /**
     * למה החיבור האחרון לא הצליח - מוצג בהגדרות במקום "לא מתחבר" שקט.
     * null כשאין שגיאה.
     */
    var lastError by mutableStateOf<String?>(null)
        private set

    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val stopScanRunnable = Runnable { stopScan() }

    /**
     * פעולות GATT רצות אחת-אחת: אנדרואיד מקבל רק פעולה אחת בכל רגע. קודם
     * שלוש ההרשמות (דופק, קצב, הספק) נשלחו ברצף, והשנייה והשלישית נכשלו
     * בשקט - ולפעמים גם הראשונה, כשהגילוי עוד לא נסגר.
     */
    private val pendingSubscriptions = ArrayDeque<BluetoothGattCharacteristic>()
    private var connectAttempts = 0
    private var connectingAddress: String? = null

    // Running Speed and Cadence - null כל עוד לא זוהה שירות כזה על המכשיר המחובר
    var hasRunningCadenceSensor by mutableStateOf(false)
        private set
    var runningCadenceSpm by mutableStateOf<Int?>(null)
        private set
    var runningSpeedKmh by mutableStateOf<Double?>(null)
        private set
    var runningStrideLengthM by mutableStateOf<Double?>(null)
        private set

    // Cycling Power - קצב הדיווש מחושב מהפרש בין שני קריאות עוקבות (מונה
    // מצטבר של סיבובי קראנק + חותמת זמן ב-1/1024 שנייה, לפי המפרט).
    var hasCyclingPowerSensor by mutableStateOf(false)
        private set
    var cyclingPowerWatts by mutableStateOf<Int?>(null)
        private set
    var cyclingCadenceRpm by mutableStateOf<Int?>(null)
        private set
    private var lastCrankSample: CrankSample? = null

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adapter: BluetoothAdapter? get() = bluetoothManager?.adapter
    private var gatt: BluetoothGatt? = null

    fun isBluetoothAvailable(): Boolean = adapter != null

    /**
     * סריקה בלי סינון לפי שירות הדופק: רוב השעונים לא מפרסמים את שירות
     * 0x180D בשידור הפרסום שלהם (רק רצועות דופק עושות זאת), ולכן הסינון הקודם
     * לא מצא אף שעון. מוצגים כל מכשירי ה-BLE עם שם, ובראש הרשימה מכשירים
     * שכבר מותאמים לטלפון. השירות נבדק רק אחרי החיבור.
     */
    fun startScan() {
        val a = adapter ?: return
        if (!a.isEnabled) {
            lastError = "הבלוטות' כבוי"
            return
        }
        val scanner = a.bluetoothLeScanner ?: return
        lastError = null
        foundDevices.clear()
        try {
            a.bondedDevices.orEmpty()
                .filter { it.type == BluetoothDevice.DEVICE_TYPE_LE || it.type == BluetoothDevice.DEVICE_TYPE_DUAL }
                .forEach { d -> foundDevices.add(FoundDevice(d.name ?: d.address, d.address)) }
        } catch (e: SecurityException) {
            // אין BLUETOOTH_CONNECT - רק תוצאות סריקה
        }
        state = HrConnectionState.SCANNING
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner.startScan(null, settings, scanCallback)
            mainHandler.removeCallbacks(stopScanRunnable)
            mainHandler.postDelayed(stopScanRunnable, SCAN_TIMEOUT_MS)
        } catch (e: SecurityException) {
            state = HrConnectionState.DISCONNECTED
            lastError = "אין הרשאת בלוטות'"
        }
    }

    fun stopScan() {
        mainHandler.removeCallbacks(stopScanRunnable)
        try {
            adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            // אין הרשאה/Bluetooth כבוי - אין מה לעצור
        }
        if (state == HrConnectionState.SCANNING) state = HrConnectionState.DISCONNECTED
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            // בלי שם (תגיות, משואות) - לא שעון שאפשר לזהות ברשימה.
            val name = try { device.name } catch (e: SecurityException) { null }
                ?: result.scanRecord?.deviceName
                ?: return
            val advertisesHr = result.scanRecord?.serviceUuids?.any { it.uuid == HEART_RATE_SERVICE_UUID } == true
            if (foundDevices.none { it.address == device.address }) {
                // מי שמפרסם דופק - ראשון ברשימה.
                if (advertisesHr) foundDevices.add(0, FoundDevice(name, device.address))
                else foundDevices.add(FoundDevice(name, device.address))
            }
        }
    }

    fun connect(address: String) {
        val device = try { adapter?.getRemoteDevice(address) } catch (e: IllegalArgumentException) { null } ?: return
        stopScan()
        gatt?.close()
        lastError = null
        if (connectingAddress != address) connectAttempts = 0
        connectingAddress = address
        state = HrConnectionState.CONNECTING
        // TRANSPORT_LE במפורש: שעון שהוא גם מכשיר בלוטות' קלאסי (DUAL) נפתח
        // אחרת בערוץ הקלאסי, והחיבור נכשל (סטטוס 133) בלי שום שירות BLE.
        gatt = try {
            device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            state = HrConnectionState.DISCONNECTED
            lastError = "אין הרשאת בלוטות'"
            null
        }
    }

    fun disconnect() {
        gatt?.disconnect()
        gatt?.close()
        gatt = null
        state = HrConnectionState.DISCONNECTED
        connectedDeviceName = null
        currentBpm = null
        hasRunningCadenceSensor = false
        runningCadenceSpm = null
        runningSpeedKmh = null
        runningStrideLengthM = null
        hasCyclingPowerSensor = false
        cyclingPowerWatts = null
        cyclingCadenceRpm = null
        lastCrankSample = null
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(g: BluetoothGatt, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {
                // כשל חיבור (133 וחבריו) - ניסיון חוזר אחד, ואז הודעה.
                g.close()
                if (gatt === g) gatt = null
                val address = g.device.address
                if (connectAttempts < 1 && state == HrConnectionState.CONNECTING) {
                    connectAttempts++
                    mainHandler.postDelayed({ connect(address) }, 600)
                } else {
                    state = HrConnectionState.DISCONNECTED
                    lastError = "החיבור נכשל - ודא שהשעון קרוב ושהבלוטות' שלו פעיל"
                }
                return
            }
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectAttempts = 0
                    connectedDeviceName = try { g.device.name } catch (e: SecurityException) { null } ?: g.device.address
                    // השהיה קצרה לפני הגילוי - חלק מהשעונים עוד מסיימים את
                    // הגדרת הקישור, והגילוי המוקדם חוזר ריק.
                    mainHandler.postDelayed({ g.discoverServices() }, 400)
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    state = HrConnectionState.DISCONNECTED
                    connectedDeviceName = null
                    currentBpm = null
                    hasRunningCadenceSensor = false
                    runningCadenceSpm = null
                    runningSpeedKmh = null
                    runningStrideLengthM = null
                    hasCyclingPowerSensor = false
                    cyclingPowerWatts = null
                    cyclingCadenceRpm = null
                    lastCrankSample = null
                }
            }
        }

        override fun onServicesDiscovered(g: BluetoothGatt, status: Int) {
            pendingSubscriptions.clear()
            val hrCharacteristic = g.getService(HEART_RATE_SERVICE_UUID)?.getCharacteristic(HEART_RATE_MEASUREMENT_UUID)
            if (hrCharacteristic == null) {
                // השעון מחובר אבל לא חושף דופק בפרוטוקול הסטנדרטי. ברוב השעונים
                // צריך להפעיל בו "שידור דופק" (HR broadcast) - אחרת הדופק עובר
                // רק לאפליקציית היצרן, שאין לה ממשק ציבורי.
                state = HrConnectionState.DISCONNECTED
                lastError = "השעון לא משדר דופק. הפעל בו \"שידור דופק\" ונסה שוב"
                g.disconnect()
                g.close()
                if (gatt === g) gatt = null
                return
            }
            pendingSubscriptions.add(hrCharacteristic)
            g.getService(RSC_SERVICE_UUID)?.getCharacteristic(RSC_MEASUREMENT_UUID)?.let {
                hasRunningCadenceSensor = true
                pendingSubscriptions.add(it)
            }
            g.getService(CYCLING_POWER_SERVICE_UUID)?.getCharacteristic(CYCLING_POWER_MEASUREMENT_UUID)?.let {
                hasCyclingPowerSensor = true
                pendingSubscriptions.add(it)
            }
            state = HrConnectionState.CONNECTED
            subscribeNext(g)
        }

        @Suppress("DEPRECATION")
        override fun onDescriptorWrite(g: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            subscribeNext(g)
        }

        /** ההרשמה הבאה בתור - רק אחרי שהקודמת אושרה (onDescriptorWrite). */
        @Suppress("DEPRECATION")
        private fun subscribeNext(g: BluetoothGatt) {
            val characteristic = pendingSubscriptions.removeFirstOrNull() ?: return
            g.setCharacteristicNotification(characteristic, true)
            val descriptor = characteristic.getDescriptor(CLIENT_CONFIG_UUID)
            if (descriptor == null) {
                subscribeNext(g)
                return
            }
            descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (!g.writeDescriptor(descriptor)) subscribeNext(g)
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(g: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            val data = characteristic.value ?: return
            when (characteristic.uuid) {
                HEART_RATE_MEASUREMENT_UUID -> currentBpm = parseHeartRate(data)
                RSC_MEASUREMENT_UUID -> parseRunningSpeedAndCadence(data)?.let { reading ->
                    runningSpeedKmh = reading.speedKmh
                    runningCadenceSpm = reading.cadenceSpm
                    reading.strideLengthM?.let { runningStrideLengthM = it }
                }
                CYCLING_POWER_MEASUREMENT_UUID -> parseCyclingPower(data)?.let { reading ->
                    cyclingPowerWatts = reading.watts
                    reading.crank?.let { crank ->
                        cadenceRpmFromCrankSamples(lastCrankSample, crank)?.let { cyclingCadenceRpm = it }
                        lastCrankSample = crank
                    }
                }
            }
        }
    }

    companion object {
        val HEART_RATE_SERVICE_UUID: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val HEART_RATE_MEASUREMENT_UUID: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")
        val RSC_SERVICE_UUID: UUID = UUID.fromString("00001814-0000-1000-8000-00805f9b34fb")
        val RSC_MEASUREMENT_UUID: UUID = UUID.fromString("00002a53-0000-1000-8000-00805f9b34fb")
        val CYCLING_POWER_SERVICE_UUID: UUID = UUID.fromString("00001818-0000-1000-8000-00805f9b34fb")
        val CYCLING_POWER_MEASUREMENT_UUID: UUID = UUID.fromString("00002a63-0000-1000-8000-00805f9b34fb")
        val CLIENT_CONFIG_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        /** הסריקה נעצרת לבד - סריקת BLE פתוחה מרוקנת סוללה. */
        private const val SCAN_TIMEOUT_MS = 20_000L

        /** פענוח ערך הדופק לפי מפרט ה-Bluetooth SIG ל-Heart Rate Measurement:
         * ביט 0 של הדגלים (בית ראשון) קובע אם הערך הוא UINT8 (בית אחד) או
         * UINT16 little-endian (שני בתים) בבית שאחריו. */
        fun parseHeartRate(data: ByteArray): Int? {
            if (data.isEmpty()) return null
            val flags = data[0].toInt()
            val is16Bit = (flags and 0x01) != 0
            return if (is16Bit) {
                if (data.size < 3) return null
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else {
                if (data.size < 2) return null
                data[1].toInt() and 0xFF
            }
        }

        /** Running Speed and Cadence Measurement (0x2A53) - ראו Bluetooth SIG
         * GSS: דגלים (ביט 0: אורך צעד קיים, ביט 1: מרחק כולל קיים), אחריהם
         * מהירות רגעית (UINT16, רזולוציה 1/256 מ'/שנייה) וקצב-צעדים רגעי
         * (UINT8, צעדים לדקה) - שאר השדות אופציונליים ולא בשימוש כאן. */
        fun parseRunningSpeedAndCadence(data: ByteArray): RunningSpeedCadenceReading? {
            if (data.size < 4) return null
            val flags = data[0].toInt()
            val hasStrideLength = (flags and 0x01) != 0
            val speedRaw = (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            val speedKmh = (speedRaw / 256.0) * 3.6
            val cadenceSpm = data[3].toInt() and 0xFF
            val strideLengthM = if (hasStrideLength && data.size >= 6) {
                val strideRaw = (data[4].toInt() and 0xFF) or ((data[5].toInt() and 0xFF) shl 8)
                strideRaw / 100.0
            } else null
            return RunningSpeedCadenceReading(speedKmh, cadenceSpm, strideLengthM)
        }

        /** Cycling Power Measurement (0x2A63) - עוצמה רגעית (INT16, וואט) תמיד
         * מיד אחרי הדגלים (16 ביט). "נתוני סיבובי קראנק" (ביט 5 בדגלים) הם
         * מונה מצטבר + חותמת זמן בלבד - חישוב קצב-הדיווש בפועל נעשה בנפרד
         * ב-cadenceRpmFromCrankSamples מהפרש בין שתי דגימות עוקבות. */
        fun parseCyclingPower(data: ByteArray): CyclingPowerReading? {
            if (data.size < 4) return null
            val flags = (data[0].toInt() and 0xFF) or ((data[1].toInt() and 0xFF) shl 8)
            val powerRaw = (data[2].toInt() and 0xFF) or ((data[3].toInt() and 0xFF) shl 8)
            val watts = powerRaw.toShort().toInt() // השדה חתום (INT16) לפי המפרט

            val hasCrankData = (flags and 0x20) != 0
            if (!hasCrankData) return CyclingPowerReading(watts, crank = null)
            var offset = 4
            if ((flags and 0x01) != 0) offset += 1 // Pedal Power Balance
            if ((flags and 0x04) != 0) offset += 2 // Accumulated Torque
            if ((flags and 0x10) != 0) offset += 6 // Wheel Revolution Data
            if (data.size < offset + 4) return CyclingPowerReading(watts, crank = null)
            val revolutions = (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)
            val eventTime = (data[offset + 2].toInt() and 0xFF) or ((data[offset + 3].toInt() and 0xFF) shl 8)
            return CyclingPowerReading(watts, CrankSample(revolutions, eventTime))
        }

        /** קצב דיווש (RPM) מהפרש בין שתי דגימות "סיבובי קראנק" עוקבות - שני
         * השדות הם UINT16 ו"מתגלגלים" חזרה ל-0 אחרי 65536 (לפי המפרט), לכן
         * ה-modulo כאן מטפל נכון גם במעבר הזה, לא רק בספירה עולה רגילה.
         * מחזיר null בפעם הראשונה (אין דגימה קודמת) או אם חלף אפס זמן. */
        fun cadenceRpmFromCrankSamples(previous: CrankSample?, current: CrankSample): Int? {
            if (previous == null) return null
            val revDelta = (current.cumulativeRevolutions - previous.cumulativeRevolutions + 65536) % 65536
            val timeDelta1024ths = (current.eventTime1024ths - previous.eventTime1024ths + 65536) % 65536
            if (timeDelta1024ths == 0) return null
            val minutes = (timeDelta1024ths / 1024.0) / 60.0
            return Math.round(revDelta / minutes).toInt()
        }
    }
}
