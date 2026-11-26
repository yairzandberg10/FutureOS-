package com.future.systemui.fui.notificationcenter.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * FutureSystemUI: FutureUI's notification center accessibility service is replaced by
 * com.future.systemui.notifications.ShadeController. This inert shell only keeps fui/MainActivity (which references
 * the class) compiling; delete both together. Not declared in the manifest.
 */
class NotificationCenterAccessibilityService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
