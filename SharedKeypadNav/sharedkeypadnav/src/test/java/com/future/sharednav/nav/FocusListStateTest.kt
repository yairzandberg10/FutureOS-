package com.future.sharednav.nav

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableIntStateOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * בדיקות ל-FocusListState - המחלקה שעליה נשען כל KeypadLazyColumn בכל
 * מסך רשימה במערכת. בודקת ישירות את מחלקת ה-state (לא את
 * rememberFocusListState שהוא @Composable ודורש קומפוזיציה) כדי לתפוס
 * רגרסיות בלוגיקת הגבולות בלי צורך במכשיר/אמולטור.
 */
class FocusListStateTest {

    private fun stateOf(itemCount: Int, initialIndex: Int = 0): FocusListState {
        val state = FocusListState(LazyListState(), mutableIntStateOf(initialIndex))
        state.itemCount = itemCount
        return state
    }

    @Test
    fun `moveDown advances index`() {
        val state = stateOf(itemCount = 5, initialIndex = 0)
        state.moveDown()
        assertEquals(1, state.focusedIndex)
    }

    @Test
    fun `moveDown does not go past last index`() {
        val state = stateOf(itemCount = 3, initialIndex = 2)
        state.moveDown()
        assertEquals(2, state.focusedIndex)
    }

    @Test
    fun `moveUp does not go below zero`() {
        val state = stateOf(itemCount = 3, initialIndex = 0)
        state.moveUp()
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun `focusedIndex is clamped to zero when itemCount is zero`() {
        val state = stateOf(itemCount = 0, initialIndex = 4)
        assertEquals(0, state.focusedIndex)
    }

    @Test
    fun `isFocused reports only the current index`() {
        val state = stateOf(itemCount = 4, initialIndex = 2)
        assertEquals(false, state.isFocused(1))
        assertEquals(true, state.isFocused(2))
    }

    @Test
    fun `setting focusedIndex out of range clamps to bounds`() {
        val state = stateOf(itemCount = 5)
        state.focusedIndex = 99
        assertEquals(4, state.focusedIndex)
        state.focusedIndex = -10
        assertEquals(0, state.focusedIndex)
    }
}
