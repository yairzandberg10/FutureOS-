package com.future.clock.ui

sealed class ClockRoute {
    object Home : ClockRoute()
    object Alarms : ClockRoute()
    object WorldClock : ClockRoute()
    object Stopwatch : ClockRoute()
    object Timer : ClockRoute()
}
