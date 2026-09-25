package com.future.messages.chat

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * מצב מקומי של הצ'אט. ההודעות עצמן נשמרות כשורות רגילות ב-content://sms,
 * ולכן הן מופיעות בשיחות, בחיפוש ובגיבוי כמו כל הודעה. כאן נשמרים רק הנתונים
 * שספק ה-SMS לא יודע להחזיק: מזהה ההודעה בצ'אט, מצב "נקרא", נתיב התמונה,
 * וספר הכתובות של מי שרשום לצ'אט, כולל המפתחות המוצמדים שלו (TOFU).
 */
internal class ChatStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "chat.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE messages (
                sms_id INTEGER PRIMARY KEY,
                msg_id TEXT NOT NULL,
                phone TEXT NOT NULL,
                thread_id INTEGER NOT NULL DEFAULT 0,
                incoming INTEGER NOT NULL,
                read_receipt_pending INTEGER NOT NULL DEFAULT 0,
                read INTEGER NOT NULL DEFAULT 0,
                image_path TEXT,
                image_only INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE UNIQUE INDEX messages_msg ON messages(msg_id, incoming)")
        db.execSQL(
            """CREATE TABLE peers (
                phone TEXT PRIMARY KEY,
                uid TEXT,
                agree_key TEXT,
                sign_key TEXT,
                checked_at INTEGER NOT NULL
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    // ---------------------------------------------------------------- הודעות

    class Info(val read: Boolean, val imagePath: String?, val imageOnly: Boolean)

    fun putOutgoing(smsId: Long, msgId: String, phone: String, threadId: Long, imagePath: String? = null, imageOnly: Boolean = false) =
        put(smsId, msgId, phone, threadId, incoming = false, readPending = false, imagePath, imageOnly)

    fun putIncoming(smsId: Long, msgId: String, phone: String, threadId: Long, imagePath: String?, imageOnly: Boolean) =
        put(smsId, msgId, phone, threadId, incoming = true, readPending = true, imagePath, imageOnly)

    private fun put(
        smsId: Long, msgId: String, phone: String, threadId: Long, incoming: Boolean,
        readPending: Boolean, imagePath: String?, imageOnly: Boolean,
    ) {
        writableDatabase.insertWithOnConflict("messages", null, ContentValues().apply {
            put("sms_id", smsId); put("msg_id", msgId); put("phone", phone); put("thread_id", threadId)
            put("incoming", if (incoming) 1 else 0); put("read_receipt_pending", if (readPending) 1 else 0)
            put("image_path", imagePath); put("image_only", if (imageOnly) 1 else 0)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun hasIncoming(msgId: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM messages WHERE msg_id = ? AND incoming = 1", arrayOf(msgId)).use { it.moveToFirst() }

    /** מזהה שורת ה-SMS של הודעה יוצאת - רק אם היא נשלחה לאותו מספר (קבלה
     * ממספר אחר על אותו מזהה נדחית). */
    fun outgoingSmsId(msgId: String, phone: String): Long? =
        readableDatabase.rawQuery("SELECT sms_id FROM messages WHERE msg_id = ? AND incoming = 0 AND phone = ?", arrayOf(msgId, phone))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    fun markRead(smsId: Long) {
        writableDatabase.update("messages", ContentValues().apply { put("read", 1) }, "sms_id = ?", arrayOf(smsId.toString()))
    }

    fun remove(smsId: Long) {
        writableDatabase.delete("messages", "sms_id = ?", arrayOf(smsId.toString()))
    }

    /** הודעות נכנסות בשיחה שעוד לא נשלח עליהן אישור קריאה: טלפון → מזהים. */
    fun takeReadPending(threadId: Long): Map<String, List<String>> {
        val result = readableDatabase.rawQuery(
            "SELECT phone, msg_id FROM messages WHERE thread_id = ? AND incoming = 1 AND read_receipt_pending = 1",
            arrayOf(threadId.toString())
        ).use { c ->
            val map = HashMap<String, MutableList<String>>()
            while (c.moveToNext()) map.getOrPut(c.getString(0)) { mutableListOf() }.add(c.getString(1))
            map
        }
        if (result.isNotEmpty()) {
            writableDatabase.update("messages", ContentValues().apply { put("read_receipt_pending", 0) },
                "thread_id = ? AND incoming = 1", arrayOf(threadId.toString()))
        }
        return result
    }

    fun infoForThread(threadId: Long): Map<Long, Info> =
        readableDatabase.rawQuery("SELECT sms_id, read, image_path, image_only FROM messages WHERE thread_id = ?", arrayOf(threadId.toString()))
            .use { c -> buildMap { while (c.moveToNext()) put(c.getLong(0), Info(c.getInt(1) == 1, c.getString(2), c.getInt(3) == 1)) } }

    // ---------------------------------------------------------------- רשומים

    class Peer(val phone: String, val uid: String?, val agreeKey: String?, val signKey: String?, val checkedAt: Long) {
        val registered get() = uid != null && agreeKey != null && signKey != null
    }

    fun peer(phone: String): Peer? =
        readableDatabase.rawQuery("SELECT uid, agree_key, sign_key, checked_at FROM peers WHERE phone = ?", arrayOf(phone)).use {
            if (it.moveToFirst()) Peer(phone, it.getString(0), it.getString(1), it.getString(2), it.getLong(3)) else null
        }

    fun peerByUid(uid: String): Peer? =
        readableDatabase.rawQuery("SELECT phone, agree_key, sign_key, checked_at FROM peers WHERE uid = ?", arrayOf(uid)).use {
            if (it.moveToFirst()) Peer(it.getString(0), uid, it.getString(1), it.getString(2), it.getLong(3)) else null
        }

    fun putPeer(phone: String, uid: String?, agreeKey: String?, signKey: String?) {
        writableDatabase.insertWithOnConflict("peers", null, ContentValues().apply {
            put("phone", phone); put("uid", uid); put("agree_key", agreeKey); put("sign_key", signKey)
            put("checked_at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun clearAll() {
        writableDatabase.delete("peers", null, null)
    }

    companion object {
        @Volatile private var instance: ChatStore? = null
        fun get(context: Context): ChatStore = instance ?: synchronized(this) {
            instance ?: ChatStore(context).also { instance = it }
        }
    }
}
