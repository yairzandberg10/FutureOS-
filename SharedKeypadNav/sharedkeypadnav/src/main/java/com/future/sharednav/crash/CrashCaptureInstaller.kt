package com.future.sharednav.crash

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * מתקין את [CrashCapture] בכל אפליקציה שצורכת את SharedKeypadNav, לפני Application.onCreate.
 * לא חשוף (exported=false) ולא מחזיק נתונים: ספק ריק שקיים רק בשביל ה-onCreate.
 */
class CrashCaptureInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        CrashCapture.install(ctx)
        Thread({ CrashCapture.flushPending(ctx) }, "FutureCrashFlush").apply { isDaemon = true }.start()
        return true
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
