package com.future.sharednav.focus

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.WeakHashMap

/**
 * הזיכרון של מסך אחד: איזה פריט היה ממוקד בו אחרון. כשחוזרים אחורה למסך,
 * הפריט הזה מקבל את הפוקוס בחזרה - במקום השורה הראשונה, שהייתה ברירת
 * המחדל כמעט בכל מסך במערכת.
 *
 * הפריט מזוהה לפי currentCompositeKeyHash: המיקום שלו בעץ ה-composition,
 * כולל מפתחות של LazyColumn (מזהה שיחה, איש קשר...). כשהמסך נבנה מחדש עם
 * אותם נתונים, אותו פריט מקבל אותו מזהה. פריטים זהים בלולאה בלי key מקבלים
 * את אותו hash, ולכן נוסף להם מספר סידורי לפי סדר ההרכבה.
 *
 * מתי משחזרים: כשהמסך חוזר ל-composition, כלומר כשהפריט הראשון שלו נרשם
 * אחרי שכל הפריטים שלו נעלמו. גלילה ברשימה או ניקוי חיפוש לא מפעילים את
 * זה, כי יש פריטים חיים כל הזמן. השחזור פתוח לזמן קצר בלבד, כך שפריט שנטען
 * מאוחר מאוד לא חוטף פוקוס מהמשתמש.
 *
 * איפה הזיכרון חי: AnimatedScreenHost נותן זיכרון לכל מסך ומאפס אותו בכניסה
 * קדימה; ב-NavHost כל יעד הוא LifecycleOwner משלו (NavBackStackEntry) ששורד
 * כל עוד הוא במחסנית, ולכן הזיכרון נקשר אליו; דיאלוג מקבל זיכרון נפרד כדי
 * שהפוקוס בתוכו לא יימחק את זה של המסך שמתחתיו.
 */
class FocusMemory {
    internal var lastKey: Long? = null
    private var liveItems = 0
    private var armedUntil = 0L
    private val ordinals = HashMap<Int, Int>()

    internal fun ordinalFor(hash: Int): Int {
        val next = ordinals[hash] ?: 0
        ordinals[hash] = next + 1
        return next
    }

    internal fun onItemAttached() {
        if (liveItems == 0 && lastKey != null) armedUntil = SystemClock.uptimeMillis() + RestoreWindowMs
        liveItems++
    }

    internal fun onItemDetached(hash: Int) {
        ordinals[hash]?.let { if (it <= 1) ordinals.remove(hash) else ordinals[hash] = it - 1 }
        liveItems--
        if (liveItems <= 0) {
            liveItems = 0
            ordinals.clear()
        }
    }

    /** true פעם אחת בלבד - לפריט שהיה ממוקד אחרון, בזמן חלון השחזור. */
    internal fun claimRestore(key: Long): Boolean {
        if (key != lastKey || SystemClock.uptimeMillis() > armedUntil) return false
        armedUntil = 0L
        return true
    }

    private companion object {
        const val RestoreWindowMs = 1500L
    }
}

/** הזיכרון שמסופק במפורש (מסך ב-AnimatedScreenHost, דיאלוג). null - לפי ה-LifecycleOwner. */
val LocalFocusMemory = compositionLocalOf<FocusMemory?> { null }

private val lifecycleMemories = WeakHashMap<LifecycleOwner, FocusMemory>()
private val mainHandler = Handler(Looper.getMainLooper())

@Composable
private fun currentFocusMemory(): FocusMemory {
    LocalFocusMemory.current?.let { return it }
    val owner = LocalLifecycleOwner.current
    return remember(owner) { lifecycleMemories.getOrPut(owner) { FocusMemory() } }
}

/**
 * רושם פריט פוקוס בזיכרון של המסך ומחזיר פונקציה שמסמנת אותו כממוקד.
 * [requester] חייב להיות מחובר לפני ה-focus target של הפריט.
 */
@Composable
internal fun rememberFocusRestore(requester: FocusRequester, enabled: Boolean): () -> Unit {
    val memory = currentFocusMemory()
    val hash = currentCompositeKeyHash
    val key = remember(memory, hash) { (hash.toLong() shl 16) or memory.ordinalFor(hash).toLong() }
    DisposableEffect(memory, key) {
        memory.onItemAttached()
        if (enabled && memory.claimRestore(key)) {
            // אחרי הפריים: מסכים מבקשים פוקוס לשורה הראשונה ב-LaunchedEffect
            // משלהם, והבקשה הזו צריכה לבוא אחריהם.
            mainHandler.post { runCatching { requester.requestFocus() } }
        }
        onDispose { memory.onItemDetached(hash) }
    }
    return remember(memory, key) { { memory.lastKey = key } }
}
