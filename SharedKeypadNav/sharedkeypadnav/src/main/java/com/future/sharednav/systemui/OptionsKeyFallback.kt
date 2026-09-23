package com.future.sharednav.systemui

import android.app.Activity
import android.content.Intent
import android.view.KeyEvent
import android.view.Window
import com.future.sharednav.actions.FutureUIActions
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy

/**
 * מקש Options שהגיע לאפליקציה עצמה.
 *
 * בדרך כלל FutureUI צורך את המקש ברמת המערכת ומשדר ACTION_OPTIONS_SHORT_PRESS
 * (onOptionsKeyPress). אבל כששירות הנגישות שלו לא רץ, או מעביר את המקש
 * הלאה (למשל כשהוא חושב שמסך הנעילה גלוי), KEYCODE_MENU מגיע כמקש רגיל -
 * ואף מסך ב-FutureOS לא מאזין לו, כך שמקש Options "לא עבד" בכלל. כאן כל
 * Activity עוטף את ה-Window.Callback שלו: MENU שהגיע נבלע ומתורגם לאותו
 * שידור, רק לאפליקציה עצמה - ותפריט האפשרויות נפתח בדיוק כמו תמיד.
 */
internal object OptionsKeyFallback {

    fun install(activity: Activity) {
        val window = activity.window ?: return
        val original = window.callback ?: return
        if (Proxy.isProxyClass(original.javaClass)) return
        val packageName = activity.packageName
        window.callback = Proxy.newProxyInstance(
            Window.Callback::class.java.classLoader,
            arrayOf(Window.Callback::class.java),
        ) { _, method, args ->
            if (method.name == "dispatchKeyEvent") {
                val event = args?.getOrNull(0) as? KeyEvent
                if (event != null && event.keyCode == KeyEvent.KEYCODE_MENU) {
                    if (event.action == KeyEvent.ACTION_UP && !event.isCanceled && event.repeatCount == 0) {
                        activity.sendBroadcast(Intent(FutureUIActions.ACTION_OPTIONS_SHORT_PRESS).setPackage(packageName))
                    }
                    return@newProxyInstance true
                }
            }
            try {
                if (args == null) method.invoke(original) else method.invoke(original, *args)
            } catch (e: InvocationTargetException) {
                throw e.targetException
            }
        } as Window.Callback
    }
}
