package com.dispatchops.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for HMAC tamper-evidence utility.
 */
class HmacUtilTest {

    private static final String TEST_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void computeHash_isDeterministic() {
        String data = "Hello World";
        String hash1 = HmacUtil.computeHmac(data, TEST_KEY);
        String hash2 = HmacUtil.computeHmac(data, TEST_KEY);
        assertEquals(hash1, hash2);
    }

    @Test
    void computeHash_differsWithDifferentContent() {
        String hash1 = HmacUtil.computeHmac("Data A", TEST_KEY);
        String hash2 = HmacUtil.computeHmac("Data B", TEST_KEY);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void computeHash_differsWithDifferentKey() {
        String data = "Same data";
        String key2 = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        String hash1 = HmacUtil.computeHmac(data, TEST_KEY);
        String hash2 = HmacUtil.computeHmac(data, key2);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void verify_passesForValidHash() {
        String data = "Document content + signature data";
        String hash = HmacUtil.computeHmac(data, TEST_KEY);
        assertTrue(HmacUtil.verifyHmac(data, TEST_KEY, hash));
    }

    @Test
    void verify_failsWhenContentTampered() {
        String data = "Original content";
        String hash = HmacUtil.computeHmac(data, TEST_KEY);
        assertFalse(HmacUtil.verifyHmac("Tampered content", TEST_KEY, hash));
    }

    @Test
    void verify_failsWhenHashTampered() {
        String data = "Some content";
        assertFalse(HmacUtil.verifyHmac(data, TEST_KEY, "0000000000000000000000000000000000000000000000000000000000000000"));
    }

    @Test
    void hashIsHexEncoded64Chars() {
        String hash = HmacUtil.computeHmac("test", TEST_KEY);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 = 32 bytes = 64 hex chars
        assertTrue(hash.matches("[0-9a-f]+"));
    }
}
