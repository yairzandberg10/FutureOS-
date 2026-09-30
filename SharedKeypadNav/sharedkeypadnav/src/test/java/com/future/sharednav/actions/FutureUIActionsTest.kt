package com.future.sharednav.actions

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression test for a security-critical constant that went missing: the
 * key-press and call broadcasts are protected by PERMISSION_SYSTEM, the code
 * used it in ten places, and it was defined nowhere - the suite did not
 * compile. The name must also match the signature permission the library
 * manifest declares, otherwise a receiver would demand a permission nobody
 * can hold (or, worse, one a foreign app could define at a lower level).
 */
class FutureUIActionsTest {

    // Gradle runs unit tests with the module directory as the working directory.
    private val manifest = File("src/main/AndroidManifest.xml").readText()

    @Test
    fun `system permission is a signature permission declared by the library manifest`() {
        val name = FutureUIActions.PERMISSION_SYSTEM
        assertTrue("PERMISSION_SYSTEM is blank", name.isNotBlank())
        val declaration = Regex(
            """<permission\s+android:name="${Regex.escape(name)}"\s+android:protectionLevel="signature"\s*/>"""
        )
        assertTrue("$name is not declared with protectionLevel=signature in the library manifest", declaration.containsMatchIn(manifest))
    }

    @Test
    fun `system permission follows the active System UI package`() {
        assertEquals(
            "${com.future.sharednav.systemui.SystemUiTarget.PACKAGE}.permission.SYSTEM_SETTINGS",
            FutureUIActions.PERMISSION_SYSTEM,
        )
    }
}
