package com.future.sharednav.nav

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * ניהול "איזה פריט ברשימה ממוקד עכשיו" עם D-pad - מחלץ דפוס שהיה קיים,
 * זהה כמעט לחלוטין, בשני מקומות בלבד בכל המערכת (Terminal/MainActivity.kt
 * ו-FutureUI/recents/RecentAppsScreen.kt): focusedIndex + LazyListState +
 * LaunchedEffect(focusedIndex) שקורא ל-animateScrollToItem. בכל שאר 57
 * המקומות עם LazyColumn/LazyRow ברחבי המערכת הדפוס הזה פשוט לא היה קיים -
 * מה שאומר שברשימה ארוכה, הפריט הממוקד יוצא מגבולות המסך והמשתמש מאבד
 * מגע איתו.
 *
 * focusedIndex נשמר ב-rememberSaveable כדי לשרוד סיבוב מסך/מוות תהליך -
 * לפני זה לא היה בכל הריפו אף שימוש ב-rememberSaveable.
 */
class FocusListState internal constructor(
    val listState: LazyListState,
    private val indexHolder: MutableState<Int>,
) {
    var itemCount: Int = 0
        internal set

    // הגטר עצמו מהדק לטווח (לא רק ה-setter) כדי שהאובייקט יישאר עקבי גם
    // כשitemCount משתנה בלי שמישהו קרא ל-focusedIndex = ... מפורשות (למשל
    // אחרי סינון/מחיקה שמקטינה את הרשימה) - ללא זה, קריאה ל-focusedIndex
    // יכולה להחזיר אינדקס שכבר לא קיים ברשימה עד ל-LaunchedEffect הבא.
    var focusedIndex: Int
        get() = if (itemCount <= 0) 0 else indexHolder.value.coerceIn(0, itemCount - 1)
        set(value) {
            indexHolder.value = if (itemCount <= 0) 0 else value.coerceIn(0, itemCount - 1)
        }

    fun moveDown() {
        if (itemCount > 0) focusedIndex = (focusedIndex + 1).coerceAtMost(itemCount - 1)
    }

    fun moveUp() {
        if (itemCount > 0) focusedIndex = (focusedIndex - 1).coerceAtLeast(0)
    }

    fun isFocused(index: Int): Boolean = index == focusedIndex
}

/**
 * @param itemCount גודל הרשימה הנוכחי - כשהוא משתנה (למשל אחרי סינון/מחיקה)
 *   focusedIndex מתוקן אוטומטית כדי לא להצביע מחוץ לתחום, והרשימה מגלגלת
 *   אליו מחדש.
 */
@Composable
fun rememberFocusListState(itemCount: Int, initialIndex: Int = 0): FocusListState {
    val indexHolder = rememberSaveable { mutableIntStateOf(initialIndex) }
    val listState = rememberLazyListState()
    val state = remember(listState, indexHolder) { FocusListState(listState, indexHolder) }
    state.itemCount = itemCount

    LaunchedEffect(itemCount) {
        if (itemCount > 0 && state.focusedIndex > itemCount - 1) {
            state.focusedIndex = itemCount - 1
        }
    }
    LaunchedEffect(state.focusedIndex, itemCount) {
        if (itemCount > 0) {
            listState.animateScrollToItem(state.focusedIndex)
        }
    }
    return state
}
