package com.future.sharednav.security

import java.util.concurrent.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.fail
import org.junit.Test

/**
 * The system shell decodes icons that belong to other apps. A hostile app can make
 * that decode run out of memory; if the shell dies with it, so do the lock screen
 * and the key filter. loadUntrusted must turn every such failure into null.
 */
class UntrustedResourcesTest {

    @Test
    fun `returns the value when loading works`() {
        assertEquals("icon", loadUntrusted("T", "icon") { "icon" })
    }

    @Test
    fun `an exception becomes null`() {
        assertNull(loadUntrusted<String>("T", "icon") { throw IllegalStateException("bad drawable") })
    }

    @Test
    fun `OutOfMemoryError from an oversized bitmap becomes null instead of killing the process`() {
        assertNull(loadUntrusted<String>("T", "icon") { throw OutOfMemoryError("Failed to allocate 1.6 GB") })
    }

    @Test
    fun `a null result stays null`() {
        assertNull(loadUntrusted<String>("T", "icon") { null })
    }

    @Test
    fun `coroutine cancellation is not swallowed`() {
        try {
            loadUntrusted<String>("T", "icon") { throw CancellationException("cancelled") }
            fail("CancellationException must propagate")
        } catch (expected: CancellationException) {
            // ok
        }
    }

    @Test
    fun `other errors are not hidden`() {
        try {
            loadUntrusted<String>("T", "icon") { throw StackOverflowError() }
            fail("StackOverflowError is a programming error and must propagate")
        } catch (expected: StackOverflowError) {
            // ok
        }
    }
}
