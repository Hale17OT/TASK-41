package com.dispatchops.domain.service;

import com.dispatchops.infrastructure.security.HmacUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for contract signing HMAC integrity and order validation logic.
 */
class ContractSigningOrderTest {

    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void signatureHashIsDeterministicForSameInput() {
        String renderedText = "This agreement between Company and John Doe dated 2026-04-09.";
        String signatureData = "data:image/png;base64,iVBORw0KGgo=";
        String hash1 = HmacUtil.computeHmac(renderedText + signatureData, HMAC_KEY);
        String hash2 = HmacUtil.computeHmac(renderedText + signatureData, HMAC_KEY);
        assertEquals(hash1, hash2);
    }

    @Test
    void differentSignatureDataProducesDifferentHash() {
        String renderedText = "Contract content here.";
        String sig1 = "data:image/png;base64,AAAA";
        String sig2 = "data:image/png;base64,BBBB";
        String hash1 = HmacUtil.computeHmac(renderedText + sig1, HMAC_KEY);
        String hash2 = HmacUtil.computeHmac(renderedText + sig2, HMAC_KEY);
        assertNotEquals(hash1, hash2);
    }

    @Test
    void tamperingRenderedTextBreaksHash() {
        String original = "Original contract text.";
        String signatureData = "sig-data";
        String originalHash = HmacUtil.computeHmac(original + signatureData, HMAC_KEY);

        String tampered = "Tampered contract text.";
        String tamperedHash = HmacUtil.computeHmac(tampered + signatureData, HMAC_KEY);

        assertNotEquals(originalHash, tamperedHash, "Tampered content should produce different hash");
        assertFalse(HmacUtil.verifyHmac(tampered + signatureData, HMAC_KEY, originalHash),
                "Verification should fail for tampered content");
    }

    @Test
    void verificationSucceedsForUntamperedContent() {
        String content = "Contract body with signer data";
        String hash = HmacUtil.computeHmac(content, HMAC_KEY);
        assertTrue(HmacUtil.verifyHmac(content, HMAC_KEY, hash));
    }
}
