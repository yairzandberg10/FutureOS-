package com.future.messages.rcs

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * מה שספק ה-SMS של אנדרואיד לא יודע להחזיק על הודעות RCS. ההודעות עצמן
 * נשמרות כשורות רגילות ב-content://sms (כך הן מופיעות בשיחות, בחיפוש ובגיבוי
 * כמו כל הודעה אחרת); כאן רק המיפוי ל-Message-ID של IMDN, מצב "נקרא",
 * ומטמון היכולות של כל מספר.
 */
internal class RcsStore private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "rcs.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE messages (
                sms_id INTEGER PRIMARY KEY,
                imdn_id TEXT NOT NULL,
                address TEXT NOT NULL,
                thread_id INTEGER NOT NULL DEFAULT 0,
                incoming INTEGER NOT NULL,
                date_time TEXT,
                display_pending INTEGER NOT NULL DEFAULT 0,
                read INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL("CREATE UNIQUE INDEX messages_imdn ON messages(imdn_id, incoming)")
        db.execSQL("CREATE TABLE capabilities (number TEXT PRIMARY KEY, rcs INTEGER NOT NULL, checked_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    class Entry(val smsId: Long, val imdnId: String, val address: String, val dateTime: String?)

    fun putOutgoing(smsId: Long, imdnId: String, address: String, threadId: Long) {
        writableDatabase.insertWithOnConflict("messages", null, ContentValues().apply {
            put("sms_id", smsId); put("imdn_id", imdnId); put("address", address)
            put("thread_id", threadId); put("incoming", 0)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun putIncoming(smsId: Long, imdnId: String, address: String, threadId: Long, dateTime: String?, wantsDisplay: Boolean) {
        writableDatabase.insertWithOnConflict("messages", null, ContentValues().apply {
            put("sms_id", smsId); put("imdn_id", imdnId); put("address", address)
            put("thread_id", threadId); put("incoming", 1); put("date_time", dateTime)
            put("display_pending", if (wantsDisplay) 1 else 0)
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun hasIncoming(imdnId: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM messages WHERE imdn_id = ? AND incoming = 1", arrayOf(imdnId)).use { it.moveToFirst() }

    fun outgoingSmsId(imdnId: String): Long? =
        readableDatabase.rawQuery("SELECT sms_id FROM messages WHERE imdn_id = ? AND incoming = 0", arrayOf(imdnId))
            .use { if (it.moveToFirst()) it.getLong(0) else null }

    fun markRead(smsId: Long) {
        writableDatabase.update("messages", ContentValues().apply { put("read", 1) }, "sms_id = ?", arrayOf(smsId.toString()))
    }

    fun remove(smsId: Long) {
        writableDatabase.delete("messages", "sms_id = ?", arrayOf(smsId.toString()))
    }

    /** הודעות נכנסות בשיחה שהשולח ביקש עליהן אישור קריאה ועוד לא קיבל. */
    fun takeDisplayPending(threadId: Long): List<Entry> {
        val entries = readableDatabase.rawQuery(
            "SELECT sms_id, imdn_id, address, date_time FROM messages WHERE thread_id = ? AND incoming = 1 AND display_pending = 1",
            arrayOf(threadId.toString())
        ).use { c -> buildList { while (c.moveToNext()) add(Entry(c.getLong(0), c.getString(1), c.getString(2), c.getString(3))) } }
        if (entries.isNotEmpty()) {
            writableDatabase.update("messages", ContentValues().apply { put("display_pending", 0) },
                "thread_id = ? AND incoming = 1", arrayOf(threadId.toString()))
        }
        return entries
    }

    /** לכל sms_id של הודעת RCS - האם הנמען כבר קרא אותה. */
    fun rcsMessages(threadId: Long): Map<Long, Boolean> =
        readableDatabase.rawQuery("SELECT sms_id, read FROM messages WHERE thread_id = ?", arrayOf(threadId.toString()))
            .use { c -> buildMap { while (c.moveToNext()) put(c.getLong(0), c.getInt(1) == 1) } }

    /** null = לא ידוע או שפג תוקף הבדיקה. */
    fun capability(number: String, maxAgeMs: Long): Boolean? =
        readableDatabase.rawQuery("SELECT rcs, checked_at FROM capabilities WHERE number = ?", arrayOf(number)).use {
            if (!it.moveToFirst() || System.currentTimeMillis() - it.getLong(1) > maxAgeMs) null else it.getInt(0) == 1
        }

    fun putCapability(number: String, rcs: Boolean) {
        writableDatabase.insertWithOnConflict("capabilities", null, ContentValues().apply {
            put("number", number); put("rcs", if (rcs) 1 else 0); put("checked_at", System.currentTimeMillis())
        }, SQLiteDatabase.CONFLICT_REPLACE)
    }

    companion object {
        @Volatile private var instance: RcsStore? = null
        fun get(context: Context): RcsStore = instance ?: synchronized(this) {
            instance ?: RcsStore(context).also { instance = it }
        }
    }
}
