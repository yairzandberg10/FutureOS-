package com.future.messages.chat

/**
 * Key pinning for FutureChat contacts (trust on first use), as a pure function so
 * it can be unit-tested without Android.
 *
 * A contact publishes TWO public keys: the signing key (ECDSA, proves who sent a
 * message) and the agreement key (ECDH, what we encrypt TO). Both have to be
 * pinned. The first version pinned only the signing key, so a compromised server
 * could swap the agreement key of a contact and keep the signing key: every
 * message we send would then be encrypted to the attacker, and nothing on the
 * screen would change. An honest device always regenerates both keys together
 * (reinstall), so "one changed, the other did not" is a strong tamper signal.
 */
internal object KeyPins {
    enum class Verdict {
        /** Nothing pinned yet: pin both keys, no warning. */
        FIRST_USE,

        /** Both keys are the pinned ones. */
        UNCHANGED,

        /** A pin from before agreement keys were pinned (signing key only) that still
         *  matches: pin the agreement key now, no warning. */
        UPGRADED,

        /** A pinned key is different: re-pin and warn the user in the conversation. */
        CHANGED,
    }

    fun evaluate(pinnedSign: String?, pinnedAgree: String?, sign: String, agree: String): Verdict = when {
        pinnedSign == null -> Verdict.FIRST_USE
        pinnedSign != sign -> Verdict.CHANGED
        pinnedAgree == null -> Verdict.UPGRADED
        pinnedAgree != agree -> Verdict.CHANGED
        else -> Verdict.UNCHANGED
    }
}
