package com.future.dialer.data.model

data class CallRecord(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    val timestamp: Long,
    val duration: Long,
    val type: CallType,
    /** מספר חסוי/לא ידוע (PRESENTATION_RESTRICTED/UNKNOWN) - אין מספר להציג או לחייג. */
    val isPrivate: Boolean = false,
)

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED, BLOCKED, VOICEMAIL
}

/** הסינון של היומן - מהצ'יפים שמעל הרשימה או מתפריט האפשרויות. */
enum class CallFilter(val label: String) {
    ALL("הכל"),
    MISSED("לא נענו"),
    INCOMING("התקבלו"),
    OUTGOING("חויגו"),
    REJECTED("נדחו"),
    BLOCKED("חסומות");

    fun matches(type: CallType): Boolean = when (this) {
        ALL -> true
        MISSED -> type == CallType.MISSED
        INCOMING -> type == CallType.INCOMING || type == CallType.VOICEMAIL
        OUTGOING -> type == CallType.OUTGOING
        REJECTED -> type == CallType.REJECTED
        BLOCKED -> type == CallType.BLOCKED
    }
}

/** סיכום הדקות ביומן - "סטטיסטיקות" בתפריט. */
data class CallStats(
    val incomingSeconds: Long,
    val outgoingSeconds: Long,
    val incomingCount: Int,
    val outgoingCount: Int,
    val missedCount: Int,
) {
    val totalSeconds: Long get() = incomingSeconds + outgoingSeconds
}
