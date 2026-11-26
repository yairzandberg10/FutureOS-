package com.future.systemui.fui.lockscreen.settings

import android.content.Context
import android.os.Bundle
import com.future.systemui.lockscreen.FutureUiLockClient

/**
 * ב-FutureUI זה היה צד השרת של הגדרות הנעילה (בתהליך שמחזיק את הקוד). ב-SystemUI הקוד וההגדרות
 * נשארים ב-FutureUI, וההגדרות מתנהלות מולו (ר' FutureUiLockClient) - אז כאן אין צד שרת:
 * [handle] לא מטפל בשום קריאה (לא קיים נתיב שמשנה קוד או הגדרות דרך SystemUI), והאסימון שייך
 * ל-FutureUI ונבדק מולו.
 */
object LockRemote {
    const val METHOD_STATE = FutureUiLockClient.METHOD_STATE
    const val METHOD_VERIFY = FutureUiLockClient.METHOD_VERIFY
    const val METHOD_END = FutureUiLockClient.METHOD_END
    const val EXTRA_TOKEN = FutureUiLockClient.EXTRA_TOKEN

    /** חוסם (קורא ל-FutureUI) - לא מה-main thread. */
    fun isValid(context: Context, candidate: String?): Boolean = FutureUiLockClient(context).tokenValid(candidate)

    @Suppress("UNUSED_PARAMETER")
    fun handle(context: Context, method: String, arg: String?, extras: Bundle?): Bundle? = null
}
