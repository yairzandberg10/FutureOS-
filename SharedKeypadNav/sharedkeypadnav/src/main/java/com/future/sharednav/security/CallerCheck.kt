package com.future.sharednav.security

import android.content.Context
import android.os.Binder
import android.os.Process

/**
 * בדיקת "מי קורא לספק התוכן שלי", לכל ה-ContentProvider-ים של FutureOS.
 *
 * הרשאת החתימה במניפסט נאכפת על ידי המערכת. ב-ROM של מכשיר ה-QA השוואת החתימות של
 * המערכת לא אמינה (ראו SameSigner), אז כל ספק בודק גם בעצמו את תעודות הקורא.
 * ב-AOSP הבדיקה הזו נשארת כשכבת הגנה שנייה.
 *
 * מותר: התהליך של הספק עצמו, root (QA דרך adb), ואפליקציה בחתימה של FutureOS.
 * עם [onlyPackages]: רק החבילות האלה (ממשק אבטחה - רק ההגדרות).
 */
object CallerCheck {

    fun isAllowed(context: Context, onlyPackages: Set<String>? = null): Boolean {
        val uid = Binder.getCallingUid()
        if (uid == Process.myUid() || uid == 0) return true
        val token = Binder.clearCallingIdentity()
        try {
            val pkgs = runCatching { context.packageManager.getPackagesForUid(uid) }.getOrNull().orEmpty()
            return pkgs.any { pkg ->
                (onlyPackages == null || pkg in onlyPackages) && SameSigner.matches(context, pkg)
            }
        } finally {
            Binder.restoreCallingIdentity(token)
        }
    }

    fun enforce(context: Context, onlyPackages: Set<String>? = null) {
        if (!isAllowed(context, onlyPackages)) throw SecurityException("caller is not allowed")
    }
}
