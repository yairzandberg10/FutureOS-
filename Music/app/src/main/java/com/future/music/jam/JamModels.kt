package com.future.music.jam

/** משתתף בג'אם - כולל המארח, שגם הוא חבר ב-members. */
data class JamMember(val uid: String, val name: String)

/** שיר בתור המשותף. הקובץ עצמו ב-Storage (jams/{jamId}/{id}) מרגע ש-[ready]. */
data class JamItem(
    val id: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val addedBy: String,
    val addedByName: String,
    val order: Double,
    val ready: Boolean,
) {
    /** ה-Song.id/mediaId של השיר בנגן - ראו [jamSongId]. */
    val songId: Long get() = jamSongId(id)
}

/**
 * מזהה שלילי ויציב לשיר מהג'אם. מזהי MediaStore חיוביים, ו--1 שמור לקובץ שנפתח
 * מאפליקציה אחרת, כך שאין התנגשות - ומועדפים, פלייליסטים ו"המשך מהיכן שהפסקת"
 * מזהים לפי הסימן שזה לא שיר מהטלפון.
 */
fun jamSongId(itemId: String): Long = -2L - (itemId.hashCode().toLong() and 0x7fffffffL)

fun isJamSongId(id: Long): Boolean = id <= -2L

/**
 * מצב הניגון המשותף: איזה שיר, האם מתנגן, ואיפה הוא היה בשעון השרת [atMs].
 * כל מכשיר מחשב מזה את המיקום עכשיו, כך שאין צורך לשדר את המיקום כל שנייה.
 */
data class JamPlayback(
    val cur: String? = null,
    val playing: Boolean = false,
    val posMs: Long = 0L,
    val atMs: Long = 0L,
) {
    fun positionAt(serverNowMs: Long): Long =
        if (playing && atMs > 0) posMs + (serverNowMs - atMs).coerceAtLeast(0L) else posMs
}

enum class JamPhase { IDLE, CONNECTING, ACTIVE }

data class JamUiState(
    val phase: JamPhase = JamPhase.IDLE,
    val jamId: String? = null,
    val code: String? = null,
    val myUid: String? = null,
    val hostUid: String? = null,
    val hostName: String = "",
    val open: Boolean = true,
    val guestsControl: Boolean = false,
    val members: List<JamMember> = emptyList(),
    val queue: List<JamItem> = emptyList(),
    val playback: JamPlayback = JamPlayback(),
    /** האם המכשיר הזה משמיע את הג'אם, או רק שלט ועורך תור. */
    val listening: Boolean = true,
    /** אחוז ההעלאה של שירים שהמכשיר הזה מעלה כרגע, לפי מזהה בתור. */
    val uploads: Map<String, Int> = emptyMap(),
) {
    val isHost: Boolean get() = myUid != null && myUid == hostUid
    /** מי שרשאי לנגן/לעצור/לדלג ולשנות סדר: המארח, או כולם אם הוא איפשר. */
    val canControl: Boolean get() = isHost || guestsControl
    val current: JamItem? get() = queue.firstOrNull { it.id == playback.cur }
    val upcoming: List<JamItem>
        get() {
            val i = queue.indexOfFirst { it.id == playback.cur }
            return if (i < 0) queue else queue.drop(i + 1)
        }

    fun canEdit(item: JamItem): Boolean = canControl || item.addedBy == myUid
}
