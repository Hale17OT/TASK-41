package com.dispatchops.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PasswordService BCrypt hashing and verification.
 */
class PasswordServiceTest {

    private final PasswordService passwordService = new PasswordService(12);

    @Test
    void hashAndVerifyRoundTrip() {
        String raw = "password";
        String hash = passwordService.hash(raw);

        assertNotNull(hash, "Hash should not be null");
        assertTrue(passwordService.verify(raw, hash),
                "Verification should succeed for the correct password");
    }

    @Test
    void verifyWithWrongPasswordFails() {
        String hash = passwordService.hash("correct");

        assertFalse(passwordService.verify("wrong", hash),
                "Verification should fail for the wrong password");
    }

    @Test
    void hashIsDifferentEachTime() {
        String raw = "samePassword";
        String hash1 = passwordService.hash(raw);
        String hash2 = passwordService.hash(raw);

        assertNotEquals(hash1, hash2,
                "Two hashes of the same password should differ due to different salts");
    }
}
