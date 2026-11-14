package com.future.messages.chat

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Push מ-FCM: ה-Cloud Function שולחת רק "יש משהו בתור", בלי תוכן. כאן מושכים
 * את התור ומפענחים במכשיר. onMessageReceived רץ ב-thread רקע ויש לו כ-20 שניות,
 * ולכן מותר לחסום כאן.
 */
class ChatPushService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        FutureChat.syncBlocking(applicationContext)
    }

    override fun onNewToken(token: String) {
        FutureChat.init(applicationContext)
        FutureChat.onNewPushToken(token)
    }
}
