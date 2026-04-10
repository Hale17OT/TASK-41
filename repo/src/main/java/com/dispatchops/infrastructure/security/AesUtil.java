package com.dispatchops.infrastructure.security;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

public final class AesUtil {

    private static final String GCM_ALGORITHM = "AES/GCM/NoPadding";
    private static final String CBC_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int CBC_IV_LENGTH = 16;

    /** Version prefix for GCM-encrypted payloads. Legacy CBC payloads have no prefix. */
    private static final byte VERSION_GCM = 0x02;

    private AesUtil() {
    }

    public static byte[] encrypt(byte[] plaintext, String hexKey) {
        try {
            byte[] keyBytes = hexToBytes(hexKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            new SecureRandom().nextBytes(nonce);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, nonce);

            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
            byte[] encrypted = cipher.doFinal(plaintext);

            // Format: version (1 byte) + nonce (12 bytes) + ciphertext+tag
            byte[] result = new byte[1 + GCM_NONCE_LENGTH + encrypted.length];
            result[0] = VERSION_GCM;
            System.arraycopy(nonce, 0, result, 1, GCM_NONCE_LENGTH);
            System.arraycopy(encrypted, 0, result, 1 + GCM_NONCE_LENGTH, encrypted.length);
            return result;
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM encryption failed", e);
        }
    }

    public static byte[] decrypt(byte[] ciphertext, String hexKey) {
        if (ciphertext.length > 0 && ciphertext[0] == VERSION_GCM) {
            return decryptGcm(ciphertext, hexKey);
        }
        return decryptLegacyCbc(ciphertext, hexKey);
    }

    private static byte[] decryptGcm(byte[] ciphertext, String hexKey) {
        try {
            byte[] keyBytes = hexToBytes(hexKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            System.arraycopy(ciphertext, 1, nonce, 0, GCM_NONCE_LENGTH);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_BITS, nonce);

            byte[] encryptedData = new byte[ciphertext.length - 1 - GCM_NONCE_LENGTH];
            System.arraycopy(ciphertext, 1 + GCM_NONCE_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(GCM_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("AES-GCM decryption failed", e);
        }
    }

    /** Backward-compatible decryption for legacy AES-CBC payloads (no version prefix). */
    private static byte[] decryptLegacyCbc(byte[] ciphertext, String hexKey) {
        try {
            byte[] keyBytes = hexToBytes(hexKey);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            byte[] iv = new byte[CBC_IV_LENGTH];
            System.arraycopy(ciphertext, 0, iv, 0, CBC_IV_LENGTH);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            byte[] encryptedData = new byte[ciphertext.length - CBC_IV_LENGTH];
            System.arraycopy(ciphertext, CBC_IV_LENGTH, encryptedData, 0, encryptedData.length);

            Cipher cipher = Cipher.getInstance(CBC_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("AES-CBC legacy decryption failed", e);
        }
    }

    public static byte[] encryptString(String plaintext, String hexKey) {
        if (plaintext == null) return null;
        return encrypt(plaintext.getBytes(StandardCharsets.UTF_8), hexKey);
    }

    public static String decryptString(byte[] ciphertext, String hexKey) {
        if (ciphertext == null) return null;
        byte[] decrypted = decrypt(ciphertext, hexKey);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}
