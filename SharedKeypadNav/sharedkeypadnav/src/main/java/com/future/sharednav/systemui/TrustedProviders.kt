package com.future.sharednav.systemui

import android.content.Context
import android.content.pm.PackageManager
import java.util.concurrent.ConcurrentHashMap

/**
 * האם ספק התוכן בכתובת הזו שייך לאפליקציה של FutureOS (אותה חתימה כמו שלנו).
 *
 * ה-ContentProvider-ים של FutureUI ושל המקלדת מוגנים בהרשאת חתימה - אבל ההגנה
 * הזו היא על הספק, לא על הלקוח. כשהאפליקציה האמיתית לא מותקנת, אפליקציה זרה יכולה
 * לתפוס את אותה כתובת (authority), ואז כל אפליקציות הסוויטה קוראות ממנה ערכים
 * ושולחות אליה עדכונים. לפני כל גישה בודקים למי הכתובת שייכת בפועל.
 */
object TrustedProviders {
    private val cache = ConcurrentHashMap<String, Pair<String, Boolean>>()

    fun isTrusted(context: Context, authority: String): Boolean {
        val pm = context.packageManager
        val owner = runCatching { pm.resolveContentProvider(authority, 0)?.packageName }.getOrNull() ?: return false
        cache[authority]?.let { (pkg, ok) -> if (pkg == owner) return ok }
        val ok = owner == context.packageName ||
            runCatching { pm.checkSignatures(context.packageName, owner) == PackageManager.SIGNATURE_MATCH }.getOrDefault(false)
        cache[authority] = owner to ok
        return ok
    }
}
