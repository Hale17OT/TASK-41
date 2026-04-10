package com.dispatchops.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Low-level HMAC security tests for device callback verification.
 * Tests the HmacUtil primitives used by the canonical callback signature contract:
 *   canonical = deviceId|eventId|timestamp|payload
 *   signature = HMAC-SHA256(canonical, deviceSharedSecret)
 */
class CallbackSecurityTest {

    private static final String DEVICE_SECRET = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";

    private String canonical(String deviceId, String eventId, String timestamp, String payload) {
        return deviceId + "|" + eventId + "|" + timestamp + "|" + payload;
    }

    @Test
    void canonicalSignatureRoundTrip() {
        String msg = canonical("dev-1", "evt-100", "2026-04-09T10:00:00Z", "{\"amount\":100}");
        String sig = HmacUtil.computeHmac(msg, DEVICE_SECRET);
        assertNotNull(sig);
        assertEquals(64, sig.length(), "SHA-256 HMAC should be 64 hex chars");
        assertTrue(HmacUtil.verifyHmac(msg, DEVICE_SECRET, sig));
    }

    @Test
    void wrongSecretRejected() {
        String msg = canonical("dev-1", "evt-1", "2026-04-09T10:00:00Z", "data");
        String sig = HmacUtil.computeHmac(msg, DEVICE_SECRET);
        String wrong = "0000000000000000000000000000000000000000000000000000000000000000";
        assertFalse(HmacUtil.verifyHmac(msg, wrong, sig));
    }

    @Test
    void anyFieldChangeBreaksSignature() {
        String orig = canonical("dev-1", "evt-1", "2026-04-09T10:00:00Z", "payload");
        String sig = HmacUtil.computeHmac(orig, DEVICE_SECRET);

        // Change each field individually
        assertFalse(HmacUtil.verifyHmac(canonical("dev-2", "evt-1", "2026-04-09T10:00:00Z", "payload"), DEVICE_SECRET, sig));
        assertFalse(HmacUtil.verifyHmac(canonical("dev-1", "evt-2", "2026-04-09T10:00:00Z", "payload"), DEVICE_SECRET, sig));
        assertFalse(HmacUtil.verifyHmac(canonical("dev-1", "evt-1", "2026-04-09T11:00:00Z", "payload"), DEVICE_SECRET, sig));
        assertFalse(HmacUtil.verifyHmac(canonical("dev-1", "evt-1", "2026-04-09T10:00:00Z", "tampered"), DEVICE_SECRET, sig));
    }

    @Test
    void hmacIsDeterministic() {
        String msg = canonical("d", "e", "t", "p");
        assertEquals(HmacUtil.computeHmac(msg, DEVICE_SECRET), HmacUtil.computeHmac(msg, DEVICE_SECRET));
    }

    @Test
    void emptyFieldsStillProduceValidSignature() {
        String msg = canonical("", "", "", "");
        String sig = HmacUtil.computeHmac(msg, DEVICE_SECRET);
        assertNotNull(sig);
        assertEquals(64, sig.length());
        assertTrue(HmacUtil.verifyHmac(msg, DEVICE_SECRET, sig));
    }
}
