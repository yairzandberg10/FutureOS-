package com.future.systemui.statusbar

import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** One icon received through IStatusBar.setIcon, already decoded for drawing. */
data class StatusBarSlotIcon(
    val slot: String,
    val bitmap: ImageBitmap?,
    val description: CharSequence?,
    val number: Int,
)

/**
 * Shared, observable state of the status bar. Other features may update the public parts
 * (e.g. the notification shade calls [setNotificationCount]); nothing here depends on them.
 */
object StatusBarState {
    private val _notificationCount = MutableStateFlow(0)

    /** Number of active notifications, shown as a small indicator next to the clock. */
    val notificationCount: StateFlow<Int> = _notificationCount

    fun setNotificationCount(count: Int) {
        _notificationCount.value = count.coerceAtLeast(0)
    }

    internal val icons = MutableStateFlow<List<StatusBarSlotIcon>>(emptyList())
    internal val clockHidden = MutableStateFlow(false)
    internal val notificationIconsHidden = MutableStateFlow(false)
    internal val systemInfoHidden = MutableStateFlow(false)
}
