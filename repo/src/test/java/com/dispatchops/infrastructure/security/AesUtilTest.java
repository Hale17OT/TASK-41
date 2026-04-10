package com.dispatchops.infrastructure.security;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for AES encryption utility (AES-GCM with legacy CBC backward compatibility).
 */
class AesUtilTest {

    private static final String TEST_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void encryptDecryptRoundTrip() {
        String plaintext = "Hello, World!";
        byte[] encrypted = AesUtil.encryptString(plaintext, TEST_KEY);
        assertNotNull(encrypted);
        assertTrue(encrypted.length > 0);

        String decrypted = AesUtil.decryptString(encrypted, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptUsesGcmVersionPrefix() {
        byte[] encrypted = AesUtil.encryptString("test", TEST_KEY);
        assertEquals(0x02, encrypted[0], "First byte should be GCM version prefix 0x02");
    }

    @Test
    void gcmDetectsTampering() {
        byte[] encrypted = AesUtil.encryptString("sensitive data", TEST_KEY);
        // Tamper with the ciphertext payload (after version + nonce)
        encrypted[encrypted.length - 1] ^= 0xFF;
        assertThrows(RuntimeException.class, () -> AesUtil.decryptString(encrypted, TEST_KEY),
                "GCM must reject tampered ciphertext");
    }

    @Test
    void decryptLegacyCbcPayload() throws Exception {
        // Simulate a legacy CBC-encrypted payload (no version prefix, format: IV + ciphertext)
        String plaintext = "legacy encrypted data";
        byte[] keyBytes = AesUtil.hexToBytes(TEST_KEY);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        byte[] legacyCiphertext = new byte[16 + encrypted.length];
        System.arraycopy(iv, 0, legacyCiphertext, 0, 16);
        System.arraycopy(encrypted, 0, legacyCiphertext, 16, encrypted.length);

        // AesUtil.decrypt should detect legacy format and use CBC path
        String decrypted = AesUtil.decryptString(legacyCiphertext, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptDecryptWithUnicodeData() {
        String plaintext = "Test with special chars: cafe\u0301 naif \u00FC \u00F1";
        byte[] encrypted = AesUtil.encryptString(plaintext, TEST_KEY);
        String decrypted = AesUtil.decryptString(encrypted, TEST_KEY);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptProducesDifferentCiphertextEachTime() {
        String plaintext = "same data";
        byte[] enc1 = AesUtil.encryptString(plaintext, TEST_KEY);
        byte[] enc2 = AesUtil.encryptString(plaintext, TEST_KEY);
        // Different nonces should produce different ciphertexts
        assertNotEquals(bytesToHex(enc1), bytesToHex(enc2));
    }

    @Test
    void decryptWithWrongKeyFails() {
        String plaintext = "Secret data";
        byte[] encrypted = AesUtil.encryptString(plaintext, TEST_KEY);
        String wrongKey = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
        // GCM authentication tag verification fails with wrong key
        assertThrows(RuntimeException.class, () -> AesUtil.decryptString(encrypted, wrongKey),
                "GCM must reject decryption with wrong key");
    }

    @Test
    void encryptNullReturnsNull() {
        assertNull(AesUtil.encryptString(null, TEST_KEY));
    }

    @Test
    void decryptNullReturnsNull() {
        assertNull(AesUtil.decryptString(null, TEST_KEY));
    }

    @Test
    void hexToBytesConversion() {
        byte[] bytes = AesUtil.hexToBytes("48656c6c6f");
        assertEquals("Hello", new String(bytes));
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
