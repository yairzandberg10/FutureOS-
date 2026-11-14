package com.android.sistemui.utils

import android.app.Notification

/**
 * קריאת טקסט מהתראה של אפליקציה אחרת בלי להפיל את FutureUI.
 *
 * ה-extras של התראה נפרסים (unparcel) רק כשניגשים אליהם. אפליקציה שמכניסה לשם
 * Parcelable משלה - מחלקה שלא קיימת אצלנו - גורמת ל-BadParcelableException ברגע
 * הקריאה, והחריגה הפילה את כל התהליך: שורת המצב, מרכז ההתראות ומסך הנעילה (שנעלם
 * עד שהשירות עלה מחדש). כל קריאה של כותרת/טקסט עוברת דרך כאן.
 */
fun Notification.safeText(key: String): CharSequence? =
    runCatching { extras?.getCharSequence(key) }.getOrNull()
