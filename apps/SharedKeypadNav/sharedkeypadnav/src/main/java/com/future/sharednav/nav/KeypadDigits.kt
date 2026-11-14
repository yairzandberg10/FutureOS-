package com.future.sharednav.nav

import androidx.compose.ui.input.key.Key

/**
 * ממפה מקש ספרה פיזי (שורה עליונה או מקלדת מספרית) לתו הספרה.
 *
 * הפרימיטיב הכי בסיסי בממשק שמבוסס מקשים בלבד, והוא היה קיים כשבעה
 * עותקים זהים בייט-לבייט (Calculator, Clock, Contact, Fitness, Music,
 * Sfarim, Tools - כל אחד בקובץ ui/KeypadInput.kt משלו, רק שורת ה-package
 * שונה), ועוד שני עותקים פרטיים ב-FutureUI ו-SystemUI.
 */
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

/** אותו מיפוי כערך מספרי - נוח למי שסופר או מאנדקס לפי הספרה. */
fun digitValueForKey(key: Key): Int? = digitForKey(key)?.toInt()
