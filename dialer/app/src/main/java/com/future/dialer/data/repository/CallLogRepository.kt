package com.future.dialer.data.repository

import android.content.Context
import android.provider.CallLog
import android.util.Log
import com.future.dialer.data.model.CallRecord
import com.future.dialer.data.model.CallType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CallLogRepository(private val context: Context) {

    suspend fun getCallLogs(): List<CallRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<CallRecord>()
        try {
            val cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )

            cursor?.use {
                val idIndex = it.getColumnIndex(CallLog.Calls._ID)
                val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
                val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
                val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)

                while (it.moveToNext()) {
                    val type = if (typeIndex != -1) {
                        when (it.getInt(typeIndex)) {
                            CallLog.Calls.INCOMING_TYPE -> CallType.INCOMING
                            CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                            CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                            CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
                            else -> CallType.INCOMING
                        }
                    } else CallType.INCOMING

                    val id = if (idIndex != -1) it.getString(idIndex) else ""
                    if (id.isNotEmpty()) {
                        records.add(
                            CallRecord(
                                id = id,
                                name = if (nameIndex != -1) it.getString(nameIndex) else null,
                                phoneNumber = if (numberIndex != -1) it.getString(numberIndex) ?: "" else "",
                                timestamp = if (dateIndex != -1) it.getLong(dateIndex) else 0L,
                                duration = if (durationIndex != -1) it.getLong(durationIndex) else 0L,
                                type = type
                            )
                        )
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e("CallLogRepository", "Permission denied for call logs", e)
        } catch (e: Exception) {
            Log.e("CallLogRepository", "Error reading call logs", e)
        }
        records
    }
}
