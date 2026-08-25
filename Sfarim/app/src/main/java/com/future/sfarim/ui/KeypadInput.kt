package com.future.sfarim.ui

import androidx.compose.ui.input.key.Key

/** ממפה מקשי ספרות פיזיים (שורה עליונה ומקלדת מספרית) לתו הספרה - משותף
 * לכל המסכים שמקבלים קלט מספרי ישירות מהמקלדת הפיזית, בלי מסך מגע. */
fun digitForKey(key: Key): String? = when (key) {
    Key.Zero, Key.NumPad0 -> "0"
    Key.One, Key.NumPad1 -> "1"
    Key.Two, Key.NumPad2 -> "2"
    Key.Three, Key.NumPad3 -> "3"
    Key.Four, Key.NumPad4 -> "4"
    Key.Five, Key.NumPad5 -> "5"
    Key.Six, Key.NumPad6 -> "6"
    Key.Seven, Key.NumPad7 -> "7"
    Key.Eight, Key.NumPad8 -> "8"
    Key.Nine, Key.NumPad9 -> "9"
    else -> null
}
