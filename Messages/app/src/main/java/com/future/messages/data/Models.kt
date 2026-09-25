package com.future.messages.data

import android.net.Uri

data class Contact(
    val name: String,
    val phoneNumber: String
)

/** סטטוס שליחה של הודעה יוצאת - מוצג למשתמש כמשוב אמיתי אם ההודעה יצאה
 * מהמכשיר בפועל, ולא רק "נשלחה" באופן אופטימי. לא רלוונטי להודעות נכנסות.
 * READ מגיע רק ב-RCS - אישור קריאה (IMDN) מהנמען. */
enum class MessageStatus { SENDING, SENT, DELIVERED, READ, FAILED }

data class Message(
    val id: Long,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val isMms: Boolean = false,
    val imageUri: Uri? = null,
    val status: MessageStatus? = null,
    /** עברה בצ'אט RCS (דרך רשת ה-IMS) ולא כ-SMS. */
    val isRcs: Boolean = false,
    /** עברה בצ'אט FutureOS (אינטרנט, מוצפנת מקצה לקצה) ולא כ-SMS. */
    val isChat: Boolean = false
)

data class Conversation(
    val threadId: Long,
    val contact: Contact,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)
