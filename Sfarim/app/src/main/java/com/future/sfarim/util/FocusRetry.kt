package com.future.sfarim.util

import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay

/**
 * בקשת פוקוס שמחכה שהרכיב יתחבר: בכניסה למסך (אנימציית מעבר, דיאלוג) הבקשה
 * הראשונה נכשלת בשקט או נבלעת, והטבעת לא מופיעה עד מקש ראשון. מנסים כמה פעמים,
 * ובסוף מבקשים שוב פעם אחת אחרי שהמעבר נגמר.
 */
suspend fun FocusRequester.requestFocusWhenReady(attempts: Int = 6) {
    repeat(attempts) { attempt ->
        val ok = runCatching { requestFocus() }.isSuccess
        if (ok && attempt > 0) return
        delay(if (attempt == 0) 80L else 120L)
    }
}
