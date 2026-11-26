package com.future.systemui.lockscreen

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public lock state for the other SystemUI features (shade, recents, heads-up, status bar...).
 * Anything that shows private content (notification text, recent apps, quick settings details) must
 * hide it while [locked] or [secured] is true.
 *
 * Both start as true: until [LockScreenFeature] has decided, the device is treated as locked (fail closed).
 */
object LockState {
    private val _locked = MutableStateFlow(true)
    private val _secured = MutableStateFlow(true)

    /** The lock window is up. */
    val locked: StateFlow<Boolean> = _locked

    /**
     * The device is still protected even if [locked] is false: the lock window is temporarily
     * stepped aside for a call, an alarm or the emergency dialer.
     */
    val secured: StateFlow<Boolean> = _secured

    internal fun update(locked: Boolean, secured: Boolean) {
        _locked.value = locked
        _secured.value = secured
    }
}
