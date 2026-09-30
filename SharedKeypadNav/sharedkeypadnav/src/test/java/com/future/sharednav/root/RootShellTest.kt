package com.future.sharednav.root

import java.io.File
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Everything that reaches `su` goes through RootShell. These tests pin the two
 * defences against command injection: [RootShell.isSafeToken] (an allow-list
 * for package names, permissions and language tags) and [RootShell.quote]
 * (for values that cannot be allow-listed). quote() is checked against a real
 * POSIX shell, not just by eye: the quoted text must come back byte-for-byte
 * as ONE argument, and nothing inside it may run.
 */
class RootShellTest {

    private val values = listOf(
        "com.example.app",
        "pkg; reboot",
        "pkg && reboot",
        "pkg | tee /data/local/tmp/x",
        "\$(reboot)",
        "`reboot`",
        "it's",
        "'; reboot; echo '",
        "a b\tc",
        "line1\nline2; reboot",
        "\"quoted\" \\\\ back",
        "*",
        "\$IFS\$9",
        "",
        "-rf /",
    )

    @Test
    fun `quote round-trips every hostile value as a single argument`() {
        assumeTrue("needs a POSIX sh", shellAvailable())
        val marker = File.createTempFile("rootshell-injection", ".marker").also { it.delete() }
        val payloads = values + listOf(
            "x; touch ${marker.path}",
            "x' ; touch ${marker.path} ; '",
            "\$(touch ${marker.path})",
            "`touch ${marker.path}`",
        )
        for (value in payloads) {
            val process = ProcessBuilder("sh", "-c", "printf %s ${RootShell.quote(value)}")
                .redirectErrorStream(true)
                .start()
            val out = process.inputStream.readBytes().toString(Charsets.UTF_8)
            assertTrue("shell did not finish", process.waitFor(5, TimeUnit.SECONDS))
            assertEquals("the shell altered the value", value, out)
            assertFalse("a hostile value was executed: <$value>", marker.exists())
        }
    }

    @Test
    fun `quote never leaves a bare quote that could end the literal early`() {
        for (value in values) {
            val quoted = RootShell.quote(value)
            assertTrue(quoted.startsWith("'") && quoted.endsWith("'"))
            // every single quote inside the literal must be part of the '\'' escape
            val inner = quoted.substring(1, quoted.length - 1).replace("'\\''", "")
            assertFalse("unescaped quote in <$quoted>", inner.contains("'"))
        }
    }

    @Test
    fun `isSafeToken accepts real package names permissions and language tags`() {
        listOf(
            "com.future.settings",
            "android.permission.WRITE_SETTINGS",
            "he-IL",
            "com.android.vending:background",
            "a",
        ).forEach { assertTrue(it, RootShell.isSafeToken(it)) }
    }

    @Test
    fun `isSafeToken rejects shell syntax whitespace and oversized input`() {
        listOf(
            "",
            "pkg; reboot",
            "pkg reboot",
            "\$(id)",
            "`id`",
            "pkg\nreboot",
            "pkg'x",
            "pkg\"x",
            "pkg|x",
            "pkg&x",
            "pkg>x",
            "pkg*",
            "a".repeat(256),
        ).forEach { assertFalse("accepted <$it>", RootShell.isSafeToken(it)) }
    }

    private fun shellAvailable(): Boolean = try {
        ProcessBuilder("sh", "-c", "true").start().waitFor(5, TimeUnit.SECONDS)
    } catch (e: Exception) {
        false
    }
}
