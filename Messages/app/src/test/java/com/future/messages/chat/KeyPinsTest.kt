package com.future.messages.chat

import com.future.messages.chat.KeyPins.Verdict
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The attack this guards against: a compromised chat server replaces a contact's
 * AGREEMENT key (the one messages are encrypted to) and leaves the signing key
 * alone. With only the signing key pinned that was invisible to the user.
 */
class KeyPinsTest {

    @Test
    fun `first contact pins both keys without a warning`() {
        assertEquals(Verdict.FIRST_USE, KeyPins.evaluate(null, null, "sign", "agree"))
    }

    @Test
    fun `same keys are unchanged`() {
        assertEquals(Verdict.UNCHANGED, KeyPins.evaluate("sign", "agree", "sign", "agree"))
    }

    @Test
    fun `swapping only the agreement key is detected`() {
        assertEquals(Verdict.CHANGED, KeyPins.evaluate("sign", "agree", "sign", "attacker-agree"))
    }

    @Test
    fun `swapping the signing key is detected even if the agreement key is kept`() {
        assertEquals(Verdict.CHANGED, KeyPins.evaluate("sign", "agree", "attacker-sign", "agree"))
    }

    @Test
    fun `a reinstall that regenerates both keys is reported`() {
        assertEquals(Verdict.CHANGED, KeyPins.evaluate("sign", "agree", "sign2", "agree2"))
    }

    @Test
    fun `a legacy pin with only the signing key is upgraded silently when it still matches`() {
        assertEquals(Verdict.UPGRADED, KeyPins.evaluate("sign", null, "sign", "agree"))
    }

    @Test
    fun `a legacy pin whose signing key changed is still reported`() {
        assertEquals(Verdict.CHANGED, KeyPins.evaluate("sign", null, "other", "agree"))
    }
}
