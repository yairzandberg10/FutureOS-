package com.future.settings.futureserver

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri

/**
 * מפרסם את טוקן השרת לאפליקציות com.future.* בלבד (readPermission = signature).
 * content://com.future.settings.futureserver/credentials - שורה אחת (device_id, token), או Cursor ריק אם עוד לא נרשמנו.
 * הקריאה לא נוגעת ברשת. אם אין טוקן, היא רק מתחילה רישום ברקע (ראו FutureServerRegistration).
 * קריאה בלבד: insert/update/delete זורקים UnsupportedOperationException.
 */
class FutureServerCredentialsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? {
        if (uri.path != PATH_CREDENTIALS) return null
        val ctx = context ?: return null

        val cursor = MatrixCursor(COLUMNS)
        val credentials = FutureServerRegistration.loadCredentials(ctx)
        if (credentials != null) {
            cursor.addRow(arrayOf<Any>(credentials.first, credentials.second))
        } else {
            // עוד אין טוקן: מתחילים רישום ברקע, והקורא ינסה שוב מאוחר יותר
            FutureServerRegistration.registerInBackgroundIfNeeded(ctx)
        }
        return cursor
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri =
        throw UnsupportedOperationException("FutureServer credentials are read-only")

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("FutureServer credentials are read-only")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("FutureServer credentials are read-only")

    companion object {
        const val AUTHORITY = "com.future.settings.futureserver"
        const val PATH_CREDENTIALS = "/credentials"
        private val COLUMNS = arrayOf("device_id", "token")
    }
}
