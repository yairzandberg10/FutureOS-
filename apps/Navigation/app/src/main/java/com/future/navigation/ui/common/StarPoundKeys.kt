package com.future.navigation.ui.common

import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.future.sharednav.nav.onPoundKeyPress
import com.future.sharednav.nav.onStarKeyPress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * מקשי * ו-# באפליקציה. הם מגיעים בשתי דרכים: כשידור מ-FutureUI (כשהמערכת צורכת
 * את המקש), או כאירוע מקש רגיל שמגיע ל-MainActivity. בטלפון נמצא ש-* הגיע כאירוע
 * מקש ולא כשידור, ולכן תפריטי * לא נפתחו בכלל. כאן שתי הדרכים מתאחדות, עם הגנה
 * מפני הפעלה כפולה כשהלחיצה מגיעה בשתיהן.
 */
object StarPoundKeys {
    private val _star = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    private val _pound = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val star: SharedFlow<Unit> = _star
    val pound: SharedFlow<Unit> = _pound

    private var lastStarAt = 0L
    private var lastPoundAt = 0L

    /** נקרא מ-MainActivity על אירוע מקש *; מחזיר true כשהאירוע נצרך. */
    fun emitStar() {
        _star.tryEmit(Unit)
    }

    fun emitPound() {
        _pound.tryEmit(Unit)
    }

    internal fun acceptStar(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastStarAt < DEBOUNCE_MS) return false
        lastStarAt = now
        return true
    }

    internal fun acceptPound(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPoundAt < DEBOUNCE_MS) return false
        lastPoundAt = now
        return true
    }

    private const val DEBOUNCE_MS = 400L
}

/** לחיצה על * - גם משידור המערכת וגם מאירוע מקש. */
@Composable
fun onStarKey(onTrigger: () -> Unit) {
    val current by rememberUpdatedState(onTrigger)
    onStarKeyPress { if (StarPoundKeys.acceptStar()) current() }
    LaunchedEffect(Unit) {
        StarPoundKeys.star.collect { if (StarPoundKeys.acceptStar()) current() }
    }
}

/** לחיצה על # - גם משידור המערכת וגם מאירוע מקש. */
@Composable
fun onPoundKey(onTrigger: () -> Unit) {
    val current by rememberUpdatedState(onTrigger)
    onPoundKeyPress { if (StarPoundKeys.acceptPound()) current() }
    LaunchedEffect(Unit) {
        StarPoundKeys.pound.collect { if (StarPoundKeys.acceptPound()) current() }
    }
}
