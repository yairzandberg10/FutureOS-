package com.future.sharednav.nav

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Modifier אחד לניווט D-pad ברשימה ממוקדת, על בסיס FocusListState. מחליף
 * את הבלוק שהיה חוזר, כמעט מילה במילה, בכל מסך רשימה שכן טיפל בניווט
 * ידנית (whenDirectionUp/Down -> focusedIndex += 1/-1) - ר' לדוגמה
 * FutureUI/recents/RecentAppsScreen.kt ו-FutureUI/controlcenter/ui/
 * ControlCenterScreen.kt. מקש ה-OK תמיד אחד משלושה קודים תואמי-T9:
 * Key.DirectionCenter / Key.Enter / Key.NumPadEnter (הדפוס הקיים בכל
 * המערכת - ר' CalendarScreens.kt, RecentAppsScreen.kt, GalleryScreens.kt).
 *
 * BACK לא מטופל כאן בכוונה - הדפוס הקיים במערכת (Contact/MainActivity.kt
 * ואחרות) הוא androidx.activity.compose.BackHandler ברמת המסך, לא
 * onKeyEvent - BACK הוא ניווט מערכת (יוצא מה-Activity/ה-Composable), לא
 * תזוזת פוקוס בתוך רשימה.
 */
fun Modifier.keypadListNav(
    state: FocusListState,
    onSelect: (index: Int) -> Unit,
    horizontal: Boolean = false,
): Modifier = onKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
    when (event.key) {
        Key.DirectionDown -> { if (!horizontal) { state.moveDown(); true } else false }
        Key.DirectionUp -> { if (!horizontal) { state.moveUp(); true } else false }
        Key.DirectionRight -> { if (horizontal) { state.moveDown(); true } else false }
        Key.DirectionLeft -> { if (horizontal) { state.moveUp(); true } else false }
        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
            if (state.itemCount > 0) onSelect(state.focusedIndex)
            true
        }
        else -> false
    }
}
