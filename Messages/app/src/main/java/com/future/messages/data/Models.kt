package com.future.messages.data

import android.net.Uri

data class Contact(
    val name: String,
    val phoneNumber: String
)

/** סטטוס שליחה של הודעה יוצאת - מוצג למשתמש כמשוב אמיתי אם ההודעה יצאה
 * מהמכשיר בפועל, ולא רק "נשלחה" באופן אופטימי. לא רלוונטי להודעות נכנסות. */
enum class MessageStatus { SENDING, SENT, DELIVERED, FAILED }

data class Message(
    val id: Long,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val isMms: Boolean = false,
    val imageUri: Uri? = null,
    val status: MessageStatus? = null
)

data class Conversation(
    val threadId: Long,
    val contact: Contact,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)
