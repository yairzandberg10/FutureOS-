package com.future.systemui

import android.content.Context

/** One SystemUI piece (status bar, notification shade, volume, power menu...). Started once by [SystemUIService]. */
interface Feature {
    fun start(context: Context, commandQueue: CommandQueue)
}
