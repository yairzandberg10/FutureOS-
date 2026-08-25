package com.future.fitness.ui

/** מ"מ:שש" מסך שניות כוללים - משמש בכל מסכי המעקב החי (אימון/ריצה/Quick Start). */
fun formatElapsed(totalSeconds: Int): String {
    val m = totalSeconds / 60
    val s = totalSeconds % 60
    return "%02d:%02d".format(m, s)
}
