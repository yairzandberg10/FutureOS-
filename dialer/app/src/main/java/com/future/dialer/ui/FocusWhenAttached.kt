package com.future.dialer.ui

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/**
 * פוקוס לשורה ברשימה עצלה. השורות של LazyColumn נבנות רק בשלב ה-layout,
 * אחרי ה-composition, כך ש-requestFocus מתוך LaunchedEffect מגיע לפני
 * שהשורה מחוברת ונכשל בשקט - והמסך נשאר בלי פוקוס, כלומר בלי מקשים.
 * מנסים שוב בכל פריים, עד כמה פריימים.
 */
suspend fun FocusRequester.requestFocusWhenAttached(frames: Int = 5) {
    repeat(frames) {
        // גרסאות Compose שונות: זורק כשהשורה לא מחוברת, או מחזיר false.
        val result = runCatching { requestFocus() }.getOrNull()
        if (result != null && result != false) return
        withFrameNanos { }
    }
}
