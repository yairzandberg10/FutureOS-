package com.future.settings.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class UpdatePolicyTest {

    // נוצר ב-Node עם WebCrypto (אותו קוד כמו server/feedback/src/updates.js): חתימת r||s של 64 בתים
    private val serverPub =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAERbErD2qZhW7i0Rgh0IvnAVF30HtLhOVUPXctakZGiS9zyfvCvLMyKSVLKzCqazFDcK/IIJJzVNTUH+yPhn99Mg=="
    private val serverManifest =
        "{\"v\":1,\"device_id\":\"11111111-2222-3333-4444-555555555555\",\"issued_at\":1,\"expires_at\":2,\"updates\":[{\"notes_he\":\"תיקון\"}]}"
    private val serverSig =
        "GkuQBgd1S1XihIWLeRj27DUwr0ONS8vZH52gdEHm6n3S+xwFIdLN91ZZlJgCr7Qmulp9oOEywyGMb5WV7qJ8xg=="

    @Test
    fun verifiesASignatureMadeByWebCryptoOnTheServer() {
        assertTrue(UpdatePolicy.verifySignature(serverManifest, serverSig, serverPub))
    }

    @Test
    fun rejectsAChangedManifestWrongKeyAndGarbage() {
        assertFalse(UpdatePolicy.verifySignature(serverManifest.replace("תיקון", "תיקוו"), serverSig, serverPub))
        val other = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val otherPub = Base64.getEncoder().encodeToString(other.public.encoded)
        assertFalse(UpdatePolicy.verifySignature(serverManifest, serverSig, otherPub))
        assertFalse(UpdatePolicy.verifySignature(serverManifest, "not base64!", serverPub))
        assertFalse(UpdatePolicy.verifySignature(serverManifest, serverSig, ""))
    }

    @Test
    fun p1363ToDerRoundTripsWithManyRandomSignatures() {
        // r או s עם ביט עליון דולק, או עם אפסים מובילים - כל המקרים של קידוד DER
        val pair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val pub = Base64.getEncoder().encodeToString(pair.public.encoded)
        repeat(64) { i ->
            val text = "manifest $i"
            val raw = Signature.getInstance("SHA256withECDSAinP1363Format").run {
                initSign(pair.private)
                update(text.toByteArray())
                sign()
            }
            assertEquals(64, raw.size)
            assertTrue("round $i", UpdatePolicy.verifySignature(text, Base64.getEncoder().encodeToString(raw), pub))
        }
    }

    @Test
    fun manifestMustBeForThisDeviceAndFresh() {
        val now = 1_000_000_000L
        val hour = 3_600_000L
        assertTrue(UpdatePolicy.manifestValid("a", "a", now, now + hour, now))
        assertFalse(UpdatePolicy.manifestValid("a", "b", now, now + hour, now))
        assertFalse("expired", UpdatePolicy.manifestValid("a", "a", now - 3 * hour, now - 2 * hour, now))
        assertFalse("from the future", UpdatePolicy.manifestValid("a", "a", now + 2 * hour, now + 3 * hour, now))
        assertTrue("small clock skew", UpdatePolicy.manifestValid("a", "a", now + 60_000, now + hour, now))
    }

    private fun offer(
        pkg: String = "com.future.camera",
        versionCode: Long = 20,
        size: Long = 2048,
        sha: String = "a".repeat(64),
        path: String = "update/download/12345678-1234-1234-1234-123456789abc",
        kind: String = "apk",
    ) = UpdatePolicy.Offer("12345678-1234-1234-1234-123456789abc", kind, pkg, versionCode, "1.20", size, sha, "", path)

    @Test
    fun offerRules() {
        assertNull(UpdatePolicy.checkOffer(offer(), 10))
        assertEquals(UpdatePolicy.Rejection.NotNewer, UpdatePolicy.checkOffer(offer(), 20))
        assertEquals(UpdatePolicy.Rejection.NotNewer, UpdatePolicy.checkOffer(offer(), 30))
        assertEquals(UpdatePolicy.Rejection.NotInstalled, UpdatePolicy.checkOffer(offer(), null))
        assertEquals(UpdatePolicy.Rejection.NotFuturePackage, UpdatePolicy.checkOffer(offer(pkg = "com.evil.camera"), 1))
        assertEquals(UpdatePolicy.Rejection.NotFuturePackage, UpdatePolicy.checkOffer(offer(pkg = "com.future.Camera"), 1))
        assertEquals(UpdatePolicy.Rejection.BadSize, UpdatePolicy.checkOffer(offer(size = 0), 1))
        assertEquals(UpdatePolicy.Rejection.BadSize, UpdatePolicy.checkOffer(offer(size = UpdatePolicy.MAX_SIZE + 1), 1))
        assertEquals(UpdatePolicy.Rejection.BadHash, UpdatePolicy.checkOffer(offer(sha = "A".repeat(64)), 1))
        assertEquals(UpdatePolicy.Rejection.BadPath, UpdatePolicy.checkOffer(offer(path = "https://evil.example/x.apk"), 1))
        assertEquals(UpdatePolicy.Rejection.BadPath, UpdatePolicy.checkOffer(offer(path = "update/download/../../admin"), 1))
        assertEquals(UpdatePolicy.Rejection.UnsupportedKind, UpdatePolicy.checkOffer(offer(kind = "ota"), 1))
    }

    @Test
    fun downloadStaysOnOurServerAndOnHttps() {
        assertEquals(
            "https://api.example/v1/update/download/12345678-1234-1234-1234-123456789abc",
            UpdatePolicy.downloadUrl("https://api.example/v1/", offer()),
        )
        assertNull(UpdatePolicy.downloadUrl("https://api.example/v1", offer(path = "//evil.example/a")))
        assertTrue(UpdatePolicy.transportAllowed("https://api.example/v1/x", debug = false))
        assertFalse(UpdatePolicy.transportAllowed("http://api.example/v1/x", debug = false))
        assertFalse(UpdatePolicy.transportAllowed("http://127.0.0.1:8787/v1/x", debug = false))
        assertTrue(UpdatePolicy.transportAllowed("http://127.0.0.1:8787/v1/x", debug = true))
        assertFalse(UpdatePolicy.transportAllowed("http://127.0.0.1.evil.example/v1", debug = true))
    }

    @Test
    fun signersMustMatchExactlyAndNeverEmpty() {
        assertTrue(UpdatePolicy.sameSigners(setOf("a"), setOf("a")))
        assertFalse(UpdatePolicy.sameSigners(setOf("a"), setOf("b")))
        assertFalse(UpdatePolicy.sameSigners(setOf("a", "b"), setOf("a")))
        assertFalse(UpdatePolicy.sameSigners(emptySet(), emptySet()))
    }

    @Test
    fun sha256Hex() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            UpdatePolicy.sha256Hex(ByteArray(0)),
        )
    }
}
