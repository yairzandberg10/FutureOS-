package com.future.bluetooth.data

import com.future.sharednav.i18n.tr

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

/** קובץ שנבחר לשליחה: ה-Uri שקיבלנו (שיתוף / בורר קבצים), שם וגודל. */
data class OutgoingFile(val uri: Uri, val name: String, val size: Long, val mime: String?)

enum class TransferStatus { Connecting, WaitingForAccept, Sending, Done, Failed, Rejected, Cancelled }

/** המצב החי של שליחה - מה שמסך ההתקדמות מציג. */
data class Transfer(
    val address: String,
    val deviceName: String,
    val files: List<OutgoingFile>,
    val status: TransferStatus = TransferStatus.Connecting,
    /** הקובץ שנשלח עכשיו (אינדקס ב-[files]). */
    val index: Int = 0,
    /** בתים שנשלחו מכל הקבצים יחד. */
    val sentBytes: Long = 0,
    /** כמה קבצים הגיעו בשלמותם. */
    val doneCount: Int = 0,
) {
    val totalBytes: Long get() = files.sumOf { it.size.coerceAtLeast(0) }
    val current: OutgoingFile get() = files[index.coerceIn(0, files.lastIndex)]
    val running: Boolean
        get() = status == TransferStatus.Connecting || status == TransferStatus.WaitingForAccept || status == TransferStatus.Sending
    val fraction: Float get() = if (totalBytes <= 0) 0f else (sentBytes.toFloat() / totalBytes).coerceIn(0f, 1f)
}

/** שורה בהיסטוריית השליחה (נשמרת בין הפעלות). */
data class TransferRecord(
    val title: String,
    val deviceName: String,
    val address: String,
    val time: Long,
    val status: TransferStatus,
    val count: Int,
)

/** בקשת ניווט מחוץ לממשק (שיתוף מאפליקציה אחרת, תוצאה מבורר הקבצים). */
enum class TransferNav { PickDevice, Progress }

/**
 * שליחת קבצים בבלוטות' - מחזיק את השליחה הפעילה, את הקבצים שממתינים לבחירת
 * מכשיר ואת ההיסטוריה. אובייקט ברמת התהליך (ולא חלק מה-Compose או מה-
 * BluetoothController) כדי שהשליחה תמשיך גם כשיוצאים מהמסך - ה-ON_STOP של
 * MainActivity סוגר את הרסיבר של הבקר, לא את השליחה.
 */
@SuppressLint("MissingPermission") // BLUETOOTH_CONNECT/SCAN מאושרות לפני שמגיעים לכאן
object TransferManager {
    private const val TAG = "TransferManager"
    private val OPP_UUID: UUID = UUID.fromString("00001105-0000-1000-8000-00805F9B34FB")
    private const val HISTORY_PREFS = "transfer_history"
    private const val HISTORY_KEY = "items"
    private const val HISTORY_MAX = 30

    private val main = Handler(Looper.getMainLooper())

    /** השליחה האחרונה (פעילה או שהסתיימה ועוד מוצגת). */
    var transfer by mutableStateOf<Transfer?>(null)
        private set

    /** קבצים שנבחרו ומחכים לבחירת מכשיר. */
    val pending = mutableStateListOf<OutgoingFile>()

    /** בקשת ניווט שהממשק צורך ומאפס ([consumeNav]). */
    var nav by mutableStateOf<TransferNav?>(null)
        private set

    val history = mutableStateListOf<TransferRecord>()
    private var historyLoaded = false

    /** יעד קבוע לבורר הקבצים: "שלח קובץ" ממסך מכשיר שולח ישר אליו. */
    private var pickTarget: Pair<String, String>? = null

    @Volatile private var socket: BluetoothSocket? = null
    @Volatile private var cancelled = false

    fun consumeNav() { nav = null }

    fun setPickTarget(address: String?, name: String?) {
        pickTarget = if (address != null && name != null) address to name else null
    }

    /**
     * קבצים הגיעו (שיתוף או בורר הקבצים). עם יעד שנקבע מראש - שולחים מיד;
     * אחרת הם ממתינים ונפתח מסך בחירת המכשיר.
     */
    fun onFilesChosen(context: Context, uris: List<Uri>) {
        val files = uris.distinct().mapNotNull { describe(context, it) }
        val target = pickTarget
        pickTarget = null
        if (files.isEmpty()) return
        if (target != null && start(context, target.first, target.second, files)) {
            nav = TransferNav.Progress
        } else {
            pending.clear()
            pending.addAll(files)
            nav = TransferNav.PickDevice
        }
    }

    /** מתחיל שליחה. false כשכבר יש שליחה פעילה. */
    fun start(context: Context, address: String, deviceName: String, files: List<OutgoingFile>): Boolean {
        if (transfer?.running == true || files.isEmpty()) return false
        loadHistory(context)
        cancelled = false
        val t = Transfer(address = address, deviceName = deviceName, files = files)
        transfer = t
        val app = context.applicationContext
        Thread({ run(app, t) }, "bt-opp-send").start()
        return true
    }

    /** שליחה חוזרת של אותם קבצים (אחרי כשלון / ביטול). */
    fun retry(context: Context): Boolean {
        val t = transfer ?: return false
        if (t.running) return false
        return start(context, t.address, t.deviceName, t.files)
    }

    fun cancel() {
        if (transfer?.running != true) return
        cancelled = true
        runCatching { socket?.close() }
    }

    /** מסך ההתקדמות נסגר אחרי שהשליחה הסתיימה - לא מציגים אותה יותר כפעילה. */
    fun dismissFinished() {
        if (transfer?.running == false) transfer = null
    }

    fun clearHistory(context: Context) {
        history.clear()
        prefs(context).edit().remove(HISTORY_KEY).apply()
    }

    // ---- השליחה עצמה (בחוט משלה) ----

    private fun run(context: Context, start: Transfer) {
        var state = start
        fun publish(next: Transfer) {
            state = next
            main.post { if (!cancelled || !next.running) transfer = next }
        }
        val adapter = (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter
        val result: TransferStatus = try {
            adapter ?: throw IOException("no adapter")
            // סריקה פעילה מאטה ומכשילה חיבור RFCOMM.
            runCatching { if (adapter.isDiscovering) adapter.cancelDiscovery() }
            val device = adapter.getRemoteDevice(start.address)
            val s = connect(device)
            socket = s
            if (cancelled) throw IOException("cancelled")
            val obex = ObexPush(s.inputStream, s.outputStream)
            obex.connect()
            var before = 0L
            start.files.forEachIndexed { i, file ->
                if (cancelled) throw IOException("cancelled")
                publish(state.copy(index = i, status = TransferStatus.WaitingForAccept, sentBytes = before))
                val stream = context.contentResolver.openInputStream(file.uri) ?: throw IOException("cannot open")
                stream.use { input ->
                    var lastPost = 0L
                    obex.put(
                        name = file.name,
                        mime = file.mime,
                        length = file.size,
                        data = input,
                        onAccepted = { publish(state.copy(status = TransferStatus.Sending)) },
                        onProgress = { sent ->
                            val now = System.currentTimeMillis()
                            // עדכון ממשק לכל היותר 10 פעמים בשנייה.
                            if (now - lastPost > 100) {
                                lastPost = now
                                publish(state.copy(sentBytes = before + sent))
                            }
                        },
                    )
                }
                before += file.size.coerceAtLeast(0)
                publish(state.copy(sentBytes = before, doneCount = i + 1))
            }
            obex.disconnect()
            TransferStatus.Done
        } catch (e: ObexPush.ObexRefused) {
            Log.w(TAG, "refused: 0x" + e.code.toString(16))
            if (cancelled) TransferStatus.Cancelled
            else if (e.code == ObexPush.RESP_FORBIDDEN || e.code == ObexPush.RESP_UNAUTHORIZED || e.code == ObexPush.RESP_NOT_ACCEPTABLE ||
                e.code == ObexPush.RESP_UNSUPPORTED_TYPE
            ) TransferStatus.Rejected
            else TransferStatus.Failed
        } catch (e: Exception) {
            Log.w(TAG, "send failed: ${e.javaClass.simpleName}")
            if (cancelled) TransferStatus.Cancelled else TransferStatus.Failed
        } finally {
            runCatching { socket?.close() }
            socket = null
        }
        val finished = state.copy(status = result)
        main.post {
            transfer = finished
            record(context, finished)
        }
    }

    /** קודם חיבור מאובטח; מכשירים ישנים בלי הצפנה מקבלים רק את הלא-מאובטח. */
    private fun connect(device: android.bluetooth.BluetoothDevice): BluetoothSocket {
        val secure = device.createRfcommSocketToServiceRecord(OPP_UUID)
        socket = secure
        try {
            secure.connect()
            return secure
        } catch (e: IOException) {
            runCatching { secure.close() }
            if (cancelled) throw e
        }
        val insecure = device.createInsecureRfcommSocketToServiceRecord(OPP_UUID)
        socket = insecure
        insecure.connect()
        return insecure
    }

    // ---- עזרים ----

    private fun describe(context: Context, uri: Uri): OutgoingFile? {
        var name: String? = null
        var size = -1L
        runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val n = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val s = c.getColumnIndex(OpenableColumns.SIZE)
                    if (n >= 0 && !c.isNull(n)) name = c.getString(n)
                    if (s >= 0 && !c.isNull(s)) size = c.getLong(s)
                }
            }
        }
        if (name == null && uri.scheme == "file") name = uri.lastPathSegment
        if (size < 0) {
            size = runCatching { context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length } }.getOrNull() ?: -1L
        }
        val mime = runCatching { context.contentResolver.getType(uri) }.getOrNull()
        return OutgoingFile(uri = uri, name = name ?: uri.lastPathSegment ?: "file", size = size, mime = mime)
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(HISTORY_PREFS, Context.MODE_PRIVATE)

    fun loadHistory(context: Context) {
        if (historyLoaded) return
        historyLoaded = true
        val raw = prefs(context).getString(HISTORY_KEY, null) ?: return
        runCatching {
            val array = JSONArray(raw)
            val items = (0 until array.length()).map { i ->
                val o = array.getJSONObject(i)
                TransferRecord(
                    title = o.getString("title"),
                    deviceName = o.getString("device"),
                    address = o.getString("address"),
                    time = o.getLong("time"),
                    status = runCatching { TransferStatus.valueOf(o.getString("status")) }.getOrDefault(TransferStatus.Failed),
                    count = o.optInt("count", 1),
                )
            }
            history.clear()
            history.addAll(items)
        }
    }

    private fun record(context: Context, t: Transfer) {
        val title = if (t.files.size == 1) t.files.first().name
        else tr("{0} ועוד {1}", t.files.first().name, t.files.size - 1)
        history.add(0, TransferRecord(title, t.deviceName, t.address, System.currentTimeMillis(), t.status, t.files.size))
        while (history.size > HISTORY_MAX) history.removeAt(history.lastIndex)
        val array = JSONArray()
        history.forEach {
            array.put(
                JSONObject()
                    .put("title", it.title)
                    .put("device", it.deviceName)
                    .put("address", it.address)
                    .put("time", it.time)
                    .put("status", it.status.name)
                    .put("count", it.count)
            )
        }
        prefs(context).edit().putString(HISTORY_KEY, array.toString()).apply()
    }
}
