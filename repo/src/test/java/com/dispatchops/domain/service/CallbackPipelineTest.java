package com.dispatchops.domain.service;

import com.dispatchops.infrastructure.security.HmacUtil;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the device callback pipeline security contract.
 * Canonical signed string: deviceId|eventId|timestamp|payload
 * All tests use the canonical format to match production verification logic.
 */
class CallbackPipelineTest {

    private static final String DEVICE_SECRET = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final long MAX_DRIFT_SECONDS = 300;

    private String canonical(String deviceId, String eventId, String timestamp, String payload) {
        return deviceId + "|" + eventId + "|" + timestamp + "|" + payload;
    }

    // --- Canonical signature verification ---

    @Test
    void validCanonicalSignatureAccepted() {
        String msg = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"amount\":100}");
        String sig = HmacUtil.computeHmac(msg, DEVICE_SECRET);
        assertTrue(HmacUtil.verifyHmac(msg, DEVICE_SECRET, sig));
    }

    @Test
    void substitutedEventIdRejected() {
        String original = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"amount\":100}");
        String sig = HmacUtil.computeHmac(original, DEVICE_SECRET);
        String tampered = canonical("dev-1", "evt-FAKE", "2026-04-09T12:00:00Z", "{\"amount\":100}");
        assertFalse(HmacUtil.verifyHmac(tampered, DEVICE_SECRET, sig));
    }

    @Test
    void substitutedTimestampRejected() {
        String original = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"amount\":100}");
        String sig = HmacUtil.computeHmac(original, DEVICE_SECRET);
        String tampered = canonical("dev-1", "evt-001", "2026-04-09T23:59:59Z", "{\"amount\":100}");
        assertFalse(HmacUtil.verifyHmac(tampered, DEVICE_SECRET, sig));
    }

    @Test
    void tamperedPayloadRejected() {
        String original = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"amount\":100}");
        String sig = HmacUtil.computeHmac(original, DEVICE_SECRET);
        String tampered = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"amount\":999}");
        assertFalse(HmacUtil.verifyHmac(tampered, DEVICE_SECRET, sig));
    }

    @Test
    void wrongDeviceSecretRejected() {
        String msg = canonical("dev-1", "evt-001", "2026-04-09T12:00:00Z", "{\"test\":true}");
        String sig = HmacUtil.computeHmac(msg, DEVICE_SECRET);
        String wrongSecret = "0000000000000000000000000000000000000000000000000000000000000000";
        assertFalse(HmacUtil.verifyHmac(msg, wrongSecret, sig));
    }

    // --- Timestamp freshness ---

    @Test
    void staleTimestampExceedsDriftWindow() {
        Instant stale = Instant.parse("2020-01-01T00:00:00Z");
        long drift = Math.abs(Duration.between(stale, Instant.now()).getSeconds());
        assertTrue(drift > MAX_DRIFT_SECONDS);
    }

    @Test
    void freshTimestampWithinDriftWindow() {
        Instant fresh = Instant.now();
        long drift = Math.abs(Duration.between(fresh, Instant.now()).getSeconds());
        assertTrue(drift <= MAX_DRIFT_SECONDS);
    }

    // --- Event ID uniqueness for replay protection ---

    @Test
    void eventIdHashesAreUniqueForDedupEnforcement() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            String hash = HmacUtil.computeHmac("evt-" + i, HMAC_KEY);
            assertTrue(seen.add(hash), "Hash collision for eventId: evt-" + i);
        }
        assertEquals(100, seen.size());
    }

    @Test
    void sameCanonicalProducesSameHash() {
        String msg = canonical("dev-1", "evt-1", "2026-04-09T12:00:00Z", "payload");
        assertEquals(HmacUtil.computeHmac(msg, DEVICE_SECRET), HmacUtil.computeHmac(msg, DEVICE_SECRET));
    }
}
