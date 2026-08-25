package com.future.dialer.data.model

data class CallRecord(
    val id: String,
    val name: String?,
    val phoneNumber: String,
    val timestamp: Long,
    val duration: Long,
    val type: CallType
)

enum class CallType {
    INCOMING, OUTGOING, MISSED, REJECTED
}
