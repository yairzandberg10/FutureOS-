package com.future.keyboard

import android.provider.Settings
import android.content.Context
import com.future.sharednav.widget.FutureContentWidget
import com.future.sharednav.widget.WidgetContent

/** האם המקלדת של FutureOS היא מקלדת ברירת המחדל, ואיך מחליפים שפה/מכתיבים. */
class KeyboardWidgetProvider : FutureContentWidget() {
    override fun content(context: Context): WidgetContent {
        val current = Settings.Secure.getString(context.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD).orEmpty()
        val active = current.startsWith(context.packageName + "/")
        return if (active) WidgetContent(value = "המקלדת פעילה", subtitle = "* ארוך שפה · 0 ארוך הכתבה")
        else WidgetContent(value = "המקלדת לא פעילה", subtitle = "פתח כדי להגדיר אותה כברירת מחדל")
    }
}
