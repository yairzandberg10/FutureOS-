package com.future.systemui.controlcenter

import android.view.WindowManager

/**
 * Window types for the full-screen FutureUI overlays, in order of preference:
 * TYPE_SYSTEM_DIALOG (2008), TYPE_NOTIFICATION_SHADE (2040), then a plain application overlay.
 * 2008 sits below the status bar window, which is what FutureUI's layering expects.
 */
internal val SYSTEM_DIALOG_TYPES = intArrayOf(
    2008,
    2040,
    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
)
