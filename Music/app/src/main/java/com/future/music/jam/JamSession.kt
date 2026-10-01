package com.future.music.jam

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.future.music.data.Song
import com.future.music.playback.MusicPlaybackService
import com.google.android.gms.tasks.Task
import com.google.common.util.concurrent.ListenableFuture
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Source
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.UploadTask
import java.io.IOException
import java.security.SecureRandom
import java.util.Date
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * ג'אם - האזנה משותפת כמו ב-Spotify Premium: מארח פותח ג'אם ומקבל קוד בן 6
 * ספרות, חברים מצטרפים עם הקוד, וכולם מוסיפים שירים מהטלפון שלהם לאותו תור.
 *
 * כל שיר שנוסף עולה ל-Storage, כך שכל משתתף יכול לשמוע אותו במכשיר שלו
 * ("האזנה במכשיר הזה"), בסנכרון לפי מצב הניגון המשותף במסמך הג'אם: איזה שיר,
 * האם מתנגן, ומיקום בשעון השרת. המארח שולט בניגון ומתקדם לשיר הבא כשהשיר
 * נגמר; אם הוא מאפשר, גם כל המשתתפים שולטים. מי שלא מאזין במכשיר שלו
 * (למשל כשכולם באותו חדר ליד הרמקול של המארח) הוא שלט ועורך תור.
 *
 * חי ברמת התהליך ולא במסך: הג'אם ממשיך כשהמסך כבוי או כשיוצאים מהאפליקציה,
 * דרך MediaController משלו אל MusicPlaybackService.
 */
object JamSession {
    private const val TAG = "JamSession"
    private const val PREFS = "jam"
    private const val KEY_NAME = "name"
    private const val KEY_JAM = "jam"
    private const val KEY_LISTENING = "listening"

    /** אותה מגבלה כמו ב-storage.rules. שיר רגיל הוא 3-10MB. */
    private const val MAX_UPLOAD_BYTES = 40L * 1024 * 1024
    /** ג'אם נמחק לבד אחרי 12 שעות (ה-Cloud Function jamExpiry). */
    private const val LIFETIME_MS = 12L * 60 * 60 * 1000
    private const val NAME_MAX = 30
    private const val TEXT_MAX = 200
    private const val NET_TIMEOUT_MS = 15_000L
    private const val TICK_MS = 1_000L
    /** פער מותר בין המכשיר למצב המשותף לפני שקופצים למיקום הנכון. */
    private const val DRIFT_TOLERANCE_MS = 1_500L
    private const val DRIFT_COOLDOWN_MS = 4_000L
    /** משתתף עם שליטה מקדם לשיר הבא רק אם המארח לא עשה את זה בזמן הזה. */
    private const val STALL_GRACE_MS = 5_000L
    private const val RESTART_THRESHOLD_MS = 3_000L
    const val ALBUM_LABEL = "ג'אם"

    var state by mutableStateOf(JamUiState())
        private set

    /** השם שמוצג לשאר המשתתפים. נשמר במכשיר. */
    var name by mutableStateOf("")
        private set

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    /** משוב קצר ל-Snackbar ("נוסף לתור", "הג'אם הסתיים"). */
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val configured: Boolean get() = JamBackend.configured
    val isActive: Boolean get() = state.phase == JamPhase.ACTIVE

    private lateinit var appContext: Context
    private val prefs by lazy { appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val registrations = mutableListOf<ListenerRegistration>()
    private var connectJob: Job? = null
    private var applyJob: Job? = null
    private var tickJob: Job? = null
    private var uploadJob: Job? = null
    private var uploads = Channel<PendingUpload>(Channel.UNLIMITED)
    private var activeUpload: UploadTask? = null
    private var activeUploadItem: String? = null
    private val removedItems = mutableSetOf<String>()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** שעון השרת פחות השעון המקומי - ראו [estimateClockOffset]. */
    private var clockOffsetMs = 0L
    /** השירים שהמכשיר הזה הוסיף: מנגנים מהקובץ המקומי, בלי להוריד את מה שהעלינו. */
    private val localUris = mutableMapOf<String, Uri>()
    private val remoteUris = mutableMapOf<String, Uri>()
    /** מה שהג'אם עצמו ביקש מהנגן - כל שינוי אחר הגיע מבחוץ (התראה, אוזניות). */
    private var expectedMediaId: String? = null
    private var expectedPlayWhenReady: Boolean? = null
    /** משתתף בלי שליטה שעצר רק את ההאזנה שלו. */
    private var localPaused = false
    private var lastDriftFix = 0L
    /** שיר שהנגן נכשל בו - לא מנסים אותו שוב בלולאה, רק כשעוברים אליו מחדש. */
    private var failedItem: String? = null

    /** מה שמתנגן אצל המארח כשהוא פותח ג'אם - ממשיך באותה נקודה כשיר הראשון. */
    class Carry(val songs: List<Song>, val positionMs: Long, val playing: Boolean)

    private class PendingUpload(val jamId: String, val itemId: String, val song: Song, val created: Task<Void>)

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        name = prefs.getString(KEY_NAME, "").orEmpty()
    }

    fun updateName(value: String) {
        val clean = value.trim().take(NAME_MAX)
        if (clean.isEmpty()) return
        name = clean
        prefs.edit().putString(KEY_NAME, clean).apply()
    }

    fun serverNow(): Long = System.currentTimeMillis() + clockOffsetMs

    // ------------------------------------------------------------ פתיחה והצטרפות

    fun create(carry: Carry?) {
        if (!configured || state.phase != JamPhase.IDLE) return
        state = JamUiState(phase = JamPhase.CONNECTING)
        connectJob = scope.launch {
            try {
                val uid = JamBackend.ensureSignedIn()
                val (jamId, code) = createJam(uid)
                enter(jamId, uid, code)
                if (carry != null) addSongs(carry.songs, startFirst = carry, announce = false)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Create failed", e)
                failed(setupProblem(e) ?: "לא הצלחנו לפתוח ג'אם. יש חיבור לאינטרנט?")
            }
        }
    }

    /** קוד אקראי פנוי; הקוד, הג'אם והמארח כמשתתף נכתבים יחד בטרנזקציה אחת. */
    private suspend fun createJam(uid: String): Pair<String, String> {
        val random = SecureRandom()
        repeat(8) {
            val code = "%06d".format(Locale.ROOT, random.nextInt(1_000_000))
            val codeRef = JamBackend.code(code)
            val jamRef = JamBackend.newJam()
            val expireAt = Timestamp(Date(System.currentTimeMillis() + LIFETIME_MS))
            val created = net {
                JamBackend.db.runTransaction { tx ->
                    if (tx.get(codeRef).exists()) return@runTransaction false
                    tx.set(jamRef, mapOf(
                        "hostUid" to uid, "hostName" to displayName(), "code" to code,
                        "open" to true, "guestsControl" to false,
                        "cur" to null, "playing" to false, "pos" to 0L,
                        "at" to FieldValue.serverTimestamp(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "expireAt" to expireAt,
                    ))
                    tx.set(jamRef.collection("members").document(uid), mapOf(
                        "name" to displayName(), "joinedAt" to FieldValue.serverTimestamp(),
                    ))
                    tx.set(codeRef, mapOf("jam" to jamRef.id, "expireAt" to expireAt))
                    true
                }.await()
            }
            if (created) return jamRef.id to code
        }
        throw IOException("No free jam code")
    }

    fun join(rawCode: String) {
        val code = rawCode.filter { it.isDigit() }
        if (code.length != 6) {
            message("הקוד הוא 6 ספרות")
            return
        }
        if (!configured || state.phase != JamPhase.IDLE) return
        state = JamUiState(phase = JamPhase.CONNECTING)
        connectJob = scope.launch {
            try {
                val uid = JamBackend.ensureSignedIn()
                val jamId = net { JamBackend.code(code).get(Source.SERVER).await() }.getString("jam")
                if (jamId == null) {
                    failed("לא נמצא ג'אם עם הקוד $code")
                    return@launch
                }
                val me = JamBackend.members(jamId).document(uid)
                if (!net { me.get(Source.SERVER).await() }.exists()) {
                    net { me.set(mapOf("name" to displayName(), "joinedAt" to FieldValue.serverTimestamp())).await(); Unit }
                }
                enter(jamId, uid, code)
            } catch (e: CancellationException) {
                throw e
            } catch (e: FirebaseFirestoreException) {
                Log.w(TAG, "Join failed", e)
                failed(
                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) "הג'אם הזה סגור להצטרפות"
                    else "לא הצלחנו להצטרף. יש חיבור לאינטרנט?"
                )
            } catch (e: Exception) {
                Log.w(TAG, "Join failed", e)
                failed(setupProblem(e) ?: "לא הצלחנו להצטרף. יש חיבור לאינטרנט?")
            }
        }
    }

    /**
     * חזרה לג'אם אחרי שהתהליך נסגר (המערכת סגרה את האפליקציה ברקע). בלי רשת
     * הג'אם נשמר לפתיחה הבאה; אם כבר לא חברים בו, הוא נשכח.
     */
    fun restore() {
        if (!::appContext.isInitialized || !configured || state.phase != JamPhase.IDLE) return
        val jamId = prefs.getString(KEY_JAM, null) ?: return
        state = JamUiState(phase = JamPhase.CONNECTING)
        connectJob = scope.launch {
            try {
                val uid = JamBackend.ensureSignedIn()
                val me = net { JamBackend.members(jamId).document(uid).get(Source.SERVER).await() }
                if (!me.exists()) {
                    prefs.edit().remove(KEY_JAM).apply()
                    state = JamUiState()
                    return@launch
                }
                enter(jamId, uid, code = null)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Restore failed", e)
                state = JamUiState()
            }
        }
    }

    private suspend fun enter(jamId: String, uid: String, code: String?) {
        prefs.edit().putString(KEY_JAM, jamId).apply()
        state = JamUiState(
            phase = JamPhase.ACTIVE, jamId = jamId, code = code, myUid = uid,
            listening = prefs.getBoolean(KEY_LISTENING, true),
        )
        localPaused = false
        listen(jamId, uid)
        connectController()
        startTicker()
        startUploads()
        estimateClockOffset(jamId, uid)
    }

    /**
     * היציאה של משתתף, או הסיום של המארח - לכולם (המארח מוחק את הג'אם, וה-Cloud
     * Function מוחקת את התור, המשתתפים והקבצים).
     */
    fun leave() {
        val s = state
        val jamId = s.jamId
        if (jamId != null && s.phase == JamPhase.ACTIVE) {
            if (s.isHost) {
                val batch = JamBackend.db.batch()
                s.code?.let { batch.delete(JamBackend.code(it)) }
                batch.delete(JamBackend.jam(jamId))
                batch.commit().addOnFailureListener { Log.w(TAG, "End failed", it) }
            } else {
                s.myUid?.let { uid ->
                    JamBackend.members(jamId).document(uid).delete().addOnFailureListener { Log.w(TAG, "Leave failed", it) }
                }
            }
        }
        teardown()
        state = JamUiState()
    }

    // ------------------------------------------------------------ האזנה ל-Firestore

    private fun listen(jamId: String, uid: String) {
        registrations += JamBackend.jam(jamId).addSnapshotListener { snap, error ->
            if (state.jamId != jamId) return@addSnapshotListener
            if (error != null) {
                lostAccess(error)
                return@addSnapshotListener
            }
            if (snap == null) return@addSnapshotListener
            if (!snap.exists()) {
                if (!snap.metadata.isFromCache) ended("הג'אם הסתיים")
                return@addSnapshotListener
            }
            val at = snap.getTimestamp("at", DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            state = state.copy(
                code = snap.getString("code") ?: state.code,
                hostUid = snap.getString("hostUid"),
                hostName = snap.getString("hostName").orEmpty(),
                open = snap.getBoolean("open") ?: true,
                guestsControl = snap.getBoolean("guestsControl") ?: false,
                playback = JamPlayback(
                    cur = snap.getString("cur"),
                    playing = snap.getBoolean("playing") ?: false,
                    posMs = snap.getLong("pos") ?: 0L,
                    atMs = at?.toDate()?.time ?: 0L,
                ),
            )
            onSharedStateChanged()
        }
        registrations += JamBackend.members(jamId).orderBy("joinedAt").addSnapshotListener { qs, error ->
            if (state.jamId != jamId) return@addSnapshotListener
            if (error != null) {
                lostAccess(error)
                return@addSnapshotListener
            }
            if (qs == null) return@addSnapshotListener
            val members = qs.documents.map { JamMember(it.id, it.getString("name").orEmpty()) }
            state = state.copy(members = members)
            if (!qs.metadata.isFromCache && members.none { it.uid == uid }) ended("הוסרת מהג'אם")
        }
        registrations += JamBackend.queue(jamId).orderBy("order").addSnapshotListener { qs, error ->
            if (state.jamId != jamId) return@addSnapshotListener
            if (error != null) {
                lostAccess(error)
                return@addSnapshotListener
            }
            if (qs == null) return@addSnapshotListener
            state = state.copy(queue = qs.documents.map { d ->
                JamItem(
                    id = d.id,
                    title = d.getString("title").orEmpty(),
                    artist = d.getString("artist").orEmpty(),
                    durationMs = d.getLong("durationMs") ?: 0L,
                    addedBy = d.getString("addedBy").orEmpty(),
                    addedByName = d.getString("addedByName").orEmpty(),
                    order = d.getDouble("order") ?: 0.0,
                    ready = d.getBoolean("ready") ?: false,
                )
            })
            onSharedStateChanged()
        }
    }

    /** מאזין של Firestore שנכשל לא חוזר. אין הרשאה = הג'אם נמחק או שהוסרנו ממנו. */
    private fun lostAccess(error: FirebaseFirestoreException) {
        Log.w(TAG, "Jam listener failed", error)
        ended(
            if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) "הג'אם הסתיים או שהוסרת ממנו"
            else "החיבור לג'אם נקטע"
        )
    }

    private fun onSharedStateChanged() {
        hostDuties()
        scheduleApply()
    }

    /** המארח מתחיל לנגן כשמגיע השיר הראשון, וממשיך כשמגיע שיר אחרי שהתור נגמר. */
    private fun hostDuties() {
        val s = state
        if (!s.isHost || s.phase != JamPhase.ACTIVE) return
        val pb = s.playback
        if (pb.cur == null) {
            nextPlayableAfter(null)?.let { publish(cur = it.id, playing = true, pos = 0L) }
            return
        }
        val cur = s.current ?: return
        val finished = !pb.playing && cur.durationMs > 0 && pb.posMs >= cur.durationMs
        if (finished) nextPlayableAfter(cur.id)?.let { publish(cur = it.id, playing = true, pos = 0L) }
    }

    /**
     * השיר הבא שכל המשתתפים יכולים לשמוע - כזה שכבר עלה. שיר של המארח שעוד עולה
     * מותר רק כשאין עדיין אף אחד אחר בג'אם, אחרת המשתתפים היו מצטרפים באמצעו.
     */
    private fun nextPlayableAfter(id: String?): JamItem? {
        val s = state
        val start = if (id == null) 0 else s.queue.indexOfFirst { it.id == id } + 1
        val alone = s.members.size <= 1
        return s.queue.drop(start).firstOrNull { it.ready || (alone && localUris.containsKey(it.id)) }
    }

    private fun advance(fromId: String) {
        val next = nextPlayableAfter(fromId)
        if (next != null) {
            publish(cur = next.id, playing = true, pos = 0L)
        } else {
            // סוף התור: השיר נשאר "בסופו", ושיר חדש שיתווסף יתחיל לבד (hostDuties).
            val duration = state.queue.firstOrNull { it.id == fromId }?.durationMs ?: 0L
            publish(cur = fromId, playing = false, pos = duration)
        }
    }

    /**
     * כתיבת מצב ניגון חדש. update ולא טרנזקציה: Firestore מציג כתיבה מקומית מיד,
     * כך שהלחיצה מורגשת מיד גם אצל מי שלחץ, והשרת מעביר אותה לכל השאר.
     */
    private fun publish(cur: String? = state.playback.cur, playing: Boolean = state.playback.playing, pos: Long) {
        val jamId = state.jamId ?: return
        val at = serverNow()
        val position = pos.coerceIn(0L, 24L * 60 * 60 * 1000)
        state = state.copy(playback = JamPlayback(cur, playing, position, at))
        JamBackend.jam(jamId)
            .update("cur", cur, "playing", playing, "pos", position, "at", Timestamp(Date(at)))
            .addOnFailureListener {
                Log.w(TAG, "Publish failed", it)
                message("הפעולה לא נשמרה בג'אם")
            }
        scheduleApply()
    }

    // ------------------------------------------------------------ ניגון במכשיר הזה

    private fun connectController() {
        if (controllerFuture != null) return
        val token = SessionToken(appContext, ComponentName(appContext, MusicPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            if (controllerFuture !== future) return@addListener
            val c = try {
                future.get()
            } catch (e: Exception) {
                Log.w(TAG, "Player connection failed", e)
                controllerFuture = null
                return@addListener
            }
            controller = c
            c.addListener(playerListener)
            scheduleApply()
        }, ContextCompat.getMainExecutor(appContext))
    }

    private fun releaseController() {
        controller?.removeListener(playerListener)
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
    }

    private fun scheduleApply() {
        if (state.phase != JamPhase.ACTIVE || !state.listening) return
        applyJob?.cancel()
        applyJob = scope.launch { applyNow() }
    }

    /** מביא את הנגן במכשיר הזה למצב המשותף: השיר, נגן/עצור, והמיקום. */
    private suspend fun applyNow() {
        val c = controller ?: return
        val item = state.current
        if (item == null) {
            if (isJamMedia(c)) {
                expectedPlayWhenReady = false
                c.pause()
            }
            return
        }
        val mediaId = item.songId.toString()
        if (c.currentMediaItem?.mediaId != mediaId) {
            val uri = playableUri(item) ?: return // עוד עולה - ננסה שוב כשיהיה מוכן
            if (controller !== c || state.playback.cur != item.id) return
            val pb = state.playback
            val position = pb.positionAt(serverNow())
            if (item.durationMs > 0 && position >= item.durationMs) return
            val play = pb.playing && !localPaused
            expectedMediaId = mediaId
            expectedPlayWhenReady = play
            failedItem = null
            c.setMediaItem(item.toMediaItem(uri), position)
            c.prepare()
            c.playWhenReady = play
            return
        }
        val pb = state.playback
        val play = pb.playing && !localPaused
        expectedMediaId = mediaId
        expectedPlayWhenReady = play
        if (c.playWhenReady != play) c.playWhenReady = play
        if (c.playbackState == Player.STATE_IDLE && failedItem != item.id) c.prepare()
        // קפיצה של מישהו אחר (או הצטרפות באמצע): מיישרים את המיקום.
        if (c.playbackState == Player.STATE_READY || !play) {
            val target = pb.positionAt(serverNow())
            if (abs(c.currentPosition - target) > DRIFT_TOLERANCE_MS && (item.durationMs <= 0 || target < item.durationMs)) {
                c.seekTo(target)
            }
        }
    }

    private suspend fun playableUri(item: JamItem): Uri? {
        if (item.addedBy == state.myUid) localUris[item.id]?.let { return it }
        remoteUris[item.id]?.let { return it }
        if (!item.ready) return null
        val jamId = state.jamId ?: return null
        return try {
            net { JamBackend.media(jamId, item.id).downloadUrl.await() }.also { remoteUris[item.id] = it }
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "No download URL for ${item.id}", e)
            null
        }
    }

    private fun JamItem.toMediaItem(uri: Uri): MediaItem = MediaItem.Builder()
        .setUri(uri)
        .setMediaId(songId.toString())
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artist)
                .setAlbumTitle(ALBUM_LABEL)
                .build()
        )
        .build()

    private fun isJamMedia(player: Player): Boolean =
        player.currentMediaItem?.mediaId?.toLongOrNull()?.let(::isJamSongId) == true

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val s = state
            if (s.phase != JamPhase.ACTIVE || !s.listening) return
            val mediaId = player.currentMediaItem?.mediaId
            val itemChanged = events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION) || events.contains(Player.EVENT_TIMELINE_CHANGED)
            if (itemChanged && expectedMediaId != null && mediaId != expectedMediaId && !isJamMedia(player)) {
                // מישהו בחר מוזיקה אחרת במכשיר (קובץ מאפליקציה אחרת, למשל) - לא נלחמים בו.
                stopListening("ההאזנה לג'אם במכשיר הזה הופסקה - מתנגנת מוזיקה אחרת")
                return
            }
            val item = s.current ?: return
            if (mediaId != item.songId.toString()) return
            if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED) && player.playbackState == Player.STATE_ENDED) {
                if (s.isHost) advance(item.id)
                return
            }
            if (events.contains(Player.EVENT_PLAY_WHEN_READY_CHANGED)) {
                val expected = expectedPlayWhenReady
                val now = player.playWhenReady
                if (expected != null && now != expected) {
                    expectedPlayWhenReady = now
                    if (s.isHost) {
                        // התראת המדיה, אוזניות שנותקו: אצל המארח זה לכולם.
                        publish(playing = now, pos = player.currentPosition)
                    } else {
                        // אצל משתתף זו רק ההאזנה שלו.
                        localPaused = !now
                        if (now) scheduleApply()
                    }
                }
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            val s = state
            if (s.phase != JamPhase.ACTIVE || !s.listening) return
            val item = s.current ?: return
            Log.w(TAG, "Playback failed for ${item.id}", error)
            if (failedItem == item.id) return
            failedItem = item.id
            remoteUris.remove(item.id)
            message("לא ניתן לנגן את \"${item.title}\"")
            if (s.isHost) advance(item.id)
        }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (true) {
                delay(TICK_MS)
                tick()
            }
        }
    }

    private fun tick() {
        val s = state
        if (s.phase != JamPhase.ACTIVE) return
        val pb = s.playback
        val cur = s.current ?: return
        val now = serverNow()
        val c = controller
        val here = s.listening && c != null && c.currentMediaItem?.mediaId == cur.songId.toString()
        if (pb.playing && cur.durationMs > 0) {
            val position = pb.positionAt(now)
            // המארח לא משמיע את הג'אם (או שהנגן שלו נתקע): מתקדמים לפי השעון.
            if (s.isHost && (!here || c?.playbackState == Player.STATE_ENDED) && position >= cur.durationMs) {
                advance(cur.id)
                return
            }
            if (!s.isHost && s.canControl && position >= cur.durationMs + STALL_GRACE_MS) {
                advance(cur.id)
                return
            }
        }
        if (here && c != null && pb.playing && !localPaused && c.playWhenReady && c.playbackState == Player.STATE_READY) {
            val drift = c.currentPosition - pb.positionAt(now)
            val elapsed = SystemClock.elapsedRealtime()
            if (abs(drift) > DRIFT_TOLERANCE_MS && elapsed - lastDriftFix > DRIFT_COOLDOWN_MS) {
                lastDriftFix = elapsed
                // הנגן של המארח הוא המקור (למשל אחרי שנתקע לטעון): מעדכנים את כולם.
                // אצל משתתף - קופצים למיקום של כולם.
                if (s.isHost) publish(pos = c.currentPosition) else c.seekTo(pb.positionAt(now))
            }
        }
    }

    /**
     * שעון השרת: כותבים חותמת זמן של השרת וקוראים אותה בחזרה. השרת קבע אותה
     * בין השליחה לאישור, כך שהשגיאה היא לכל היותר חצי מזמן הסבב.
     */
    private suspend fun estimateClockOffset(jamId: String, uid: String) {
        try {
            val me = JamBackend.members(jamId).document(uid)
            val t0 = System.currentTimeMillis()
            net { me.update("ping", FieldValue.serverTimestamp()).await(); Unit }
            val t1 = System.currentTimeMillis()
            val server = net { me.get(Source.SERVER).await() }.getTimestamp("ping")?.toDate()?.time ?: return
            clockOffsetMs = server - (t0 + t1) / 2
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Clock offset estimate failed", e)
        }
    }

    // ------------------------------------------------------------ פעולות מהמסך

    /** OK על שיר מהספרייה בזמן ג'אם מוסיף אותו לתור, כמו ב-Spotify. */
    fun addSongs(songs: List<Song>, startFirst: Carry? = null, announce: Boolean = true) {
        val s = state
        val jamId = s.jamId ?: return
        val uid = s.myUid ?: return
        if (s.phase != JamPhase.ACTIVE) return
        // id שלילי: שיר מהג'אם עצמו או קובץ שנפתח מבחוץ - אין לו קובץ בספרייה להעלות.
        val usable = songs.filter { it.id >= 0 }
        if (usable.isEmpty()) return
        scope.launch {
            var added = 0
            var startPending = startFirst
            val base = serverNow().toDouble()
            for ((index, song) in usable.withIndex()) {
                val size = withContext(Dispatchers.IO) { fileSize(song.uri) }
                if (size != null && size > MAX_UPLOAD_BYTES) {
                    message("\"${song.title}\" גדול מדי לג'אם (עד 40MB)")
                    continue
                }
                if (state.jamId != jamId) return@launch
                val ref = JamBackend.queue(jamId).document()
                localUris[ref.id] = song.uri
                val created = ref.set(mapOf(
                    "title" to song.title.take(TEXT_MAX),
                    "artist" to song.artist.take(TEXT_MAX),
                    "durationMs" to song.durationMs.coerceAtLeast(0L),
                    "addedBy" to uid,
                    "addedByName" to displayName(),
                    "addedAt" to FieldValue.serverTimestamp(),
                    "order" to base + index,
                    "ready" to false,
                ))
                uploads.trySend(PendingUpload(jamId, ref.id, song, created))
                startPending?.let { carry ->
                    // השיר המשיך להתנגן בזמן שהג'אם נפתח - המיקום נלקח מהנגן עכשיו,
                    // לא מהרגע שנלחץ "פתיחה", אחרת היה קופץ שתי שניות אחורה.
                    val live = controller?.takeIf { it.currentMediaItem?.mediaId == song.id.toString() }
                    publish(cur = ref.id, playing = live?.playWhenReady ?: carry.playing, pos = live?.currentPosition ?: carry.positionMs)
                }
                startPending = null
                added++
            }
            if (announce && added > 0) message(if (added == 1) "נוסף לתור הג'אם" else "$added שירים נוספו לתור הג'אם")
        }
    }

    fun togglePlay() {
        val s = state
        if (s.phase != JamPhase.ACTIVE) return
        val pb = s.playback
        val cur = s.current
        if (s.canControl) {
            val finished = cur != null && cur.durationMs > 0 && !pb.playing && pb.posMs >= cur.durationMs
            if (cur == null || finished) {
                val next = nextPlayableAfter(cur?.id)
                if (next != null) publish(cur = next.id, playing = true, pos = 0L)
                else message(if (s.queue.isEmpty()) "התור ריק - הוסיפו שירים" else "השיר הבא עוד עולה")
                return
            }
            publish(playing = !pb.playing, pos = positionNow())
            return
        }
        if (!s.listening) {
            message("רק המארח שולט בניגון")
            return
        }
        if (!pb.playing) {
            message("הג'אם מושהה כרגע")
            return
        }
        localPaused = !localPaused
        message(if (localPaused) "ההאזנה במכשיר הזה הושהתה" else "חזרה להאזנה לג'אם")
        scheduleApply()
    }

    fun next() {
        if (!requireControl()) return
        val s = state
        val next = nextPlayableAfter(s.playback.cur)
        when {
            next != null -> publish(cur = next.id, playing = true, pos = 0L)
            s.upcoming.isEmpty() -> message("אין עוד שירים בתור")
            else -> message("השיר הבא עוד עולה")
        }
    }

    fun previous() {
        if (!requireControl()) return
        val s = state
        val index = s.queue.indexOfFirst { it.id == s.playback.cur }
        val previous = if (index > 0) s.queue.subList(0, index).lastOrNull { it.ready || localUris.containsKey(it.id) } else null
        if (previous == null || positionNow() > RESTART_THRESHOLD_MS) publish(pos = 0L)
        else publish(cur = previous.id, playing = true, pos = 0L)
    }

    fun seekRelative(deltaMs: Long) {
        if (!requireControl()) return
        val cur = state.current ?: return
        val max = if (cur.durationMs > 0) (cur.durationMs - 1_000L).coerceAtLeast(0L) else Long.MAX_VALUE
        publish(pos = (positionNow() + deltaMs).coerceIn(0L, max))
    }

    /** נגן עכשיו: השיר עובר לראש התור ומתחיל, ומה שדילגנו עליו נשאר בתור. */
    fun playNow(item: JamItem) {
        if (!requireControl()) return
        val s = state
        val jamId = s.jamId ?: return
        if (!item.ready && !(item.addedBy == s.myUid && localUris.containsKey(item.id))) {
            message("השיר עוד עולה")
            return
        }
        val cur = s.current
        val upcoming = s.upcoming
        if (cur != null && upcoming.firstOrNull()?.id != item.id) {
            val nextOrder = upcoming.firstOrNull()?.order ?: (cur.order + 2.0)
            JamBackend.queue(jamId).document(item.id).update("order", (cur.order + nextOrder) / 2.0)
                .addOnFailureListener { Log.w(TAG, "Reorder failed", it) }
        }
        publish(cur = item.id, playing = true, pos = 0L)
    }

    fun move(item: JamItem, up: Boolean) {
        if (!requireControl()) return
        val s = state
        val jamId = s.jamId ?: return
        val list = s.upcoming
        val i = list.indexOfFirst { it.id == item.id }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in list.indices) return
        val other = list[j]
        val batch = JamBackend.db.batch()
        batch.update(JamBackend.queue(jamId).document(item.id), "order", other.order)
        batch.update(JamBackend.queue(jamId).document(other.id), "order", item.order)
        batch.commit().addOnFailureListener {
            Log.w(TAG, "Move failed", it)
            message("השינוי לא נשמר")
        }
    }

    fun remove(item: JamItem) {
        val s = state
        val jamId = s.jamId ?: return
        if (!s.canEdit(item)) {
            message("אפשר להסיר רק שירים שהוספת")
            return
        }
        if (item.id == s.playback.cur) {
            message("השיר הזה מתנגן עכשיו - אפשר לדלג עליו")
            return
        }
        removedItems += item.id
        if (activeUploadItem == item.id) activeUpload?.cancel()
        JamBackend.queue(jamId).document(item.id).delete().addOnFailureListener {
            Log.w(TAG, "Remove failed", it)
            message("ההסרה נכשלה")
        }
    }

    fun kick(member: JamMember) {
        val s = state
        val jamId = s.jamId ?: return
        if (!s.isHost || member.uid == s.myUid) return
        JamBackend.members(jamId).document(member.uid).delete().addOnFailureListener {
            Log.w(TAG, "Kick failed", it)
            message("ההסרה נכשלה")
        }
    }

    fun setOpen(open: Boolean) = hostSetting("open", open)

    fun setGuestsControl(enabled: Boolean) = hostSetting("guestsControl", enabled)

    private fun hostSetting(field: String, value: Boolean) {
        val s = state
        val jamId = s.jamId ?: return
        if (!s.isHost) return
        JamBackend.jam(jamId).update(field, value).addOnFailureListener {
            Log.w(TAG, "Setting $field failed", it)
            message("השינוי לא נשמר")
        }
    }

    /** האזנה במכשיר הזה, או שלט ועורך תור בלבד (כשכולם ליד אותו רמקול). */
    fun setListening(on: Boolean) {
        if (state.listening == on) return
        prefs.edit().putBoolean(KEY_LISTENING, on).apply()
        state = state.copy(listening = on)
        if (on) {
            localPaused = false
            scheduleApply()
        } else {
            releasePlayer(clear = false)
        }
    }

    private fun stopListening(text: String) {
        applyJob?.cancel()
        expectedMediaId = null
        expectedPlayWhenReady = null
        state = state.copy(listening = false)
        message(text)
    }

    /** המיקום בשיר הנוכחי - מהנגן אם הוא משמיע אותו, אחרת לפי השעון המשותף. */
    fun positionNow(): Long {
        val s = state
        val cur = s.current
        val c = controller
        if (s.listening && c != null && cur != null && c.currentMediaItem?.mediaId == cur.songId.toString()) {
            return c.currentPosition
        }
        val position = s.playback.positionAt(serverNow())
        return if (cur != null && cur.durationMs > 0) position.coerceAtMost(cur.durationMs) else position
    }

    private fun requireControl(): Boolean {
        if (state.phase != JamPhase.ACTIVE) return false
        if (state.canControl) return true
        message("רק המארח שולט בניגון")
        return false
    }

    // ------------------------------------------------------------ העלאות

    /** העלאות בזו אחר זו, לא במקביל - חיבור סלולרי של טלפון מקשים לא יחזיק יותר. */
    private fun startUploads() {
        uploadJob?.cancel()
        uploads.close()
        val channel = Channel<PendingUpload>(Channel.UNLIMITED)
        uploads = channel
        uploadJob = scope.launch {
            for (pending in channel) upload(pending)
        }
    }

    private suspend fun upload(u: PendingUpload) {
        if (state.jamId != u.jamId || u.itemId in removedItems) return
        val itemRef = JamBackend.queue(u.jamId).document(u.itemId)
        try {
            // חוקי ה-Storage בודקים ב-Firestore מי הוסיף את השיר - המסמך צריך להיות שם קודם.
            net { u.created.await(); Unit }
            val mime = appContext.contentResolver.getType(u.song.uri)?.takeIf { it.startsWith("audio/") } ?: "audio/mpeg"
            val task = JamBackend.media(u.jamId, u.itemId)
                .putFile(u.song.uri, StorageMetadata.Builder().setContentType(mime).build())
            activeUpload = task
            activeUploadItem = u.itemId
            task.addOnProgressListener { snap ->
                if (state.jamId == u.jamId && snap.totalByteCount > 0) {
                    val percent = (100L * snap.bytesTransferred / snap.totalByteCount).toInt()
                    state = state.copy(uploads = state.uploads + (u.itemId to percent))
                }
            }
            task.await()
            net { itemRef.update("ready", true).await(); Unit }
        } catch (e: Exception) {
            // העלאה שבוטלה (השיר הוסר) זורקת CancellationException כמו ביטול של
            // הקורוטינה עצמה - רק ביטול אמיתי של הקורוטינה עוצר את התור.
            currentCoroutineContext().ensureActive()
            Log.w(TAG, "Upload failed for ${u.itemId}", e)
            if (state.jamId == u.jamId && u.itemId !in removedItems) {
                message("ההעלאה של \"${u.song.title}\" נכשלה")
                // השיר שמתנגן עכשיו נשאר (המארח שומע אותו מהקובץ שלו); את השאר מסירים.
                if (state.playback.cur != u.itemId) itemRef.delete()
            }
        } finally {
            activeUpload = null
            activeUploadItem = null
            if (state.jamId == u.jamId) state = state.copy(uploads = state.uploads - u.itemId)
        }
    }

    private fun fileSize(uri: Uri): Long? = runCatching {
        appContext.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()

    // ------------------------------------------------------------ סיום

    private fun releasePlayer(clear: Boolean) {
        applyJob?.cancel()
        expectedMediaId = null
        expectedPlayWhenReady = null
        val c = controller ?: return
        if (!isJamMedia(c)) return
        if (clear) {
            c.stop()
            c.clearMediaItems()
        } else {
            c.pause()
        }
    }

    private fun teardown() {
        connectJob?.cancel()
        connectJob = null
        registrations.forEach { it.remove() }
        registrations.clear()
        releasePlayer(clear = true)
        tickJob?.cancel()
        uploadJob?.cancel()
        uploads.close()
        activeUpload?.cancel()
        activeUpload = null
        activeUploadItem = null
        releaseController()
        localUris.clear()
        remoteUris.clear()
        removedItems.clear()
        localPaused = false
        failedItem = null
        clockOffsetMs = 0L
        if (::appContext.isInitialized) prefs.edit().remove(KEY_JAM).apply()
    }

    private fun ended(text: String) {
        if (state.phase == JamPhase.IDLE) return
        teardown()
        state = JamUiState()
        message(text)
    }

    private fun failed(text: String) {
        teardown()
        state = JamUiState()
        message(text)
    }

    private fun message(text: String) {
        _messages.tryEmit(text)
    }

    /** הודעה קצרה מהמסכים, באותו Snackbar של הג'אם. */
    fun notify(text: String) = message(text)

    /**
     * התקלות של ההקמה הראשונה, בשם שלהן - אחרת הן נראות כמו "אין אינטרנט".
     * כניסה אנונימית כבויה בקונסולה, או חוקים שלא נפרסו (firebase deploy).
     */
    private fun setupProblem(e: Exception): String? = when {
        e is FirebaseAuthException -> "הכניסה ל-Firebase נכשלה. צריך להפעיל כניסה אנונימית (Music/README.md)"
        e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "Firebase דחה את הבקשה. צריך לפרוס את החוקים החדשים (firebase deploy)"
        else -> null
    }

    private fun displayName(): String = name.ifBlank { "משתתף" }.take(NAME_MAX)

    /** בלי רשת משימות של Firebase לא נכשלות - הן מחכות. כאן הן נכשלות אחרי 15 שניות. */
    private suspend fun <T : Any> net(block: suspend () -> T): T =
        withTimeoutOrNull(NET_TIMEOUT_MS) { block() } ?: throw IOException("Network timeout")
}
