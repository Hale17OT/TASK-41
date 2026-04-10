package com.dispatchops.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FieldEncryptionService {

    private final String aesKey;

    public FieldEncryptionService(@Value("${security.aes.key}") String aesKey) {
        this.aesKey = aesKey;
    }

    public byte[] encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        return AesUtil.encryptString(plaintext, aesKey);
    }

    public String decrypt(byte[] ciphertext) {
        if (ciphertext == null) {
            return null;
        }
        return AesUtil.decryptString(ciphertext, aesKey);
    }
}
