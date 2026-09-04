package com.future.sharednav.nav

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

/**
 * מיפוי מקשי 1-9 לבחירה ישירה של פריט ברשימה לפי מיקומו (מקש 1 = הפריט
 * הראשון וכו') - דפוס שהיה חוזר ידנית (למשל בבחירת פריטי תפריט ב-Tools/
 * Settings/Guide) בלי מימוש משותף. רלוונטי רק לרשימות קצרות (עד 9 פריטים
 * גלויים בבת אחת) כמו תפריטים ראשיים - לא לרשימות גלילה ארוכות.
 */
private val digitKeys = mapOf(
    Key.One to 0, Key.NumPad1 to 0,
    Key.Two to 1, Key.NumPad2 to 1,
    Key.Three to 2, Key.NumPad3 to 2,
    Key.Four to 3, Key.NumPad4 to 3,
    Key.Five to 4, Key.NumPad5 to 4,
    Key.Six to 5, Key.NumPad6 to 5,
    Key.Seven to 6, Key.NumPad7 to 6,
    Key.Eight to 7, Key.NumPad8 to 7,
    Key.Nine to 8, Key.NumPad9 to 8,
)

fun Modifier.numericShortcuts(itemCount: Int, onSelect: (index: Int) -> Unit): Modifier =
    onKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
        val index = digitKeys[event.key] ?: return@onKeyEvent false
        if (index >= itemCount) return@onKeyEvent false
        onSelect(index)
        true
    }
