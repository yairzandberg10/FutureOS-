package com.future.messages.data

import android.net.Uri

data class Contact(
    val name: String,
    val phoneNumber: String
)

data class Message(
    val id: Long,
    val text: String,
    val timestamp: Long,
    val isFromMe: Boolean,
    val isRead: Boolean,
    val isMms: Boolean = false,
    val imageUri: Uri? = null
)

data class Conversation(
    val threadId: Long,
    val contact: Contact,
    val lastMessageText: String,
    val lastMessageTimestamp: Long,
    val unreadCount: Int
)
