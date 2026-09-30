package com.future.sharednav.security

import android.util.Log
import java.util.concurrent.CancellationException

/**
 * Runs [block] - decoding a drawable that belongs to ANOTHER app (its launcher
 * icon, a notification's small icon) - and returns null if it cannot be loaded.
 *
 * That code runs another package's resources inside our process. A hostile app
 * can declare a bitmap tens of thousands of pixels on a side: a few KB
 * compressed, gigabytes to decode. The failure is an OutOfMemoryError - an
 * Error, not an Exception - so `catch (e: Exception)` lets it through and kills
 * the process that hosts the accessibility services (status bar, key filter,
 * lock screen). The notification that triggered it is still posted after the
 * restart, so the process crashes again: a persistent denial of service for the
 * whole system shell, with the lock screen missing for the seconds between a
 * crash and its restart.
 *
 * Coroutine cancellation is never swallowed.
 */
inline fun <T> loadUntrusted(tag: String, what: String, block: () -> T?): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (e: Exception) {
    Log.w(tag, "$what failed", e)
    null
} catch (e: OutOfMemoryError) {
    Log.w(tag, "$what failed: resource too large")
    null
}
