package com.future.fitness.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester

/**
 * מבקש פוקוס התחלתי לפריט הראשון של מסך. ה-NavHost מבקש פוקוס על העוגן שלו
 * בכל כניסה למסך, ובקשה כזו יכולה לגנוב את הפוקוס מהפריט אחרי שהוא כבר נתפס.
 * לכן לא עוצרים בבקשה הראשונה שלא נכשלה (requestFocus לא נכשל גם כשהבקשה
 * נדרסת), אלא מבקשים שוב בכמה פריימים הראשונים.
 */
@Composable
fun rememberInitialFocus(): FocusRequester {
    val focusRequester = remember { FocusRequester() }
    InitialFocusEffect(focusRequester)
    return focusRequester
}

/** אותו דבר ל-FocusRequester שכבר נמצא בשימוש אצל המסך. בקשה לפני שהפריט מצויר נכשלת - לכן נסיון חוזר ושגיאות נבלעות. */
@Composable
fun InitialFocusEffect(focusRequester: FocusRequester) {
    LaunchedEffect(Unit) {
        repeat(12) {
            withFrameNanos { }
            runCatching { focusRequester.requestFocus() }
        }
    }
}
