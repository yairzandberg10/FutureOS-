package com.future.navigation.data.backend

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FutureServerTest {

    /** בלי ספק (אין init, אין Context) ובלי כתובת - השרת לא זמין, והאפליקציה נופלת לנתיב הקיים. */
    @Test
    fun noPropertyAndEmptyBuildConfig_isNotAvailable() {
        assertFalse(FutureServer.isAvailable)
    }

    /** הכתובת מה-property גוברת; בלעדיה נופלים ל-BuildConfig; נגררת / מוסרת. */
    @Test
    fun resolveBaseUrl_prefersPropertyThenBuildConfig() {
        assertEquals("", FutureServer.resolveBaseUrl("", ""))
        assertEquals("https://build.example", FutureServer.resolveBaseUrl("", "https://build.example/"))
        assertEquals("https://prop.example", FutureServer.resolveBaseUrl("https://prop.example", "https://build.example"))
    }
}
