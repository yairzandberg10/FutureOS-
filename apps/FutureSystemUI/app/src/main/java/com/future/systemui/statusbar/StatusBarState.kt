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

/** One app that has a notification waiting: its own small icon, newest app first. */
data class NotificationAppIcon(
    val packageName: String,
    val bitmap: ImageBitmap?,
)

/**
 * Shared, observable state of the status bar. Other features may update the public parts
 * (e.g. the notification shade calls [setNotificationCount]); nothing here depends on them.
 */
object StatusBarState {
    private val _notificationCount = MutableStateFlow(0)

    /** Number of notifications waiting for the user (ongoing ones are state, not counted). */
    val notificationCount: StateFlow<Int> = _notificationCount

    fun setNotificationCount(count: Int) {
        _notificationCount.value = count.coerceAtLeast(0)
    }

    internal val notificationApps = MutableStateFlow<List<NotificationAppIcon>>(emptyList())
    internal val icons = MutableStateFlow<List<StatusBarSlotIcon>>(emptyList())
    internal val clockHidden = MutableStateFlow(false)
    internal val notificationIconsHidden = MutableStateFlow(false)
    internal val systemInfoHidden = MutableStateFlow(false)

    /** The app under the bar asked for dark icons (APPEARANCE_LIGHT_STATUS_BARS). */
    internal val lightBackground = MutableStateFlow(false)
    /** APPEARANCE_LOW_PROFILE_BARS: the app wants the bar dimmed. */
    internal val lowProfile = MutableStateFlow(false)
}
