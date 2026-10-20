package com.future.fitness.ui

import java.util.Locale

/** מ"מ:שש" מסך שניות כוללים - משמש בכל מסכי המעקב החי (אימון/ריצה/Quick Start). */
fun formatElapsed(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    // Locale.US: ספרות ו-":" קבועות בלי תלות בלוקאל של המכשיר (F29).
    return "%02d:%02d".format(Locale.US, m, s)
}
