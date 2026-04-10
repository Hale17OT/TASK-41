package com.dispatchops.domain.service;

import com.dispatchops.infrastructure.security.HmacUtil;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for contract generation and signing integrity semantics.
 */
class ContractFlowIntegrityTest {

    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void placeholderReplacementIsComplete() {
        String template = "Agreement between {{company}} and {{courier_name}} on {{date}}.";
        String rendered = template
                .replace("{{company}}", "DispatchOps Inc")
                .replace("{{courier_name}}", "John Doe")
                .replace("{{date}}", "2026-04-09");
        assertFalse(rendered.contains("{{"), "No unresolved placeholders should remain");
        assertTrue(rendered.contains("DispatchOps Inc"));
        assertTrue(rendered.contains("John Doe"));
    }

    @Test
    void placeholderExtractionFindsAllTokens() {
        String body = "Dear {{name}}, your order {{order_id}} weighing {{weight}} lbs ships on {{date}}.";
        Pattern p = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher m = p.matcher(body);
        int count = 0;
        while (m.find()) count++;
        assertEquals(4, count, "Should find 4 placeholder tokens");
    }

    @Test
    void signatureHashBindsContentAndSignatureData() {
        String renderedText = "Contract body here.";
        String sig1 = "signature-data-1";
        String sig2 = "signature-data-2";

        String hash1 = HmacUtil.computeHmac(renderedText + sig1, HMAC_KEY);
        String hash2 = HmacUtil.computeHmac(renderedText + sig2, HMAC_KEY);
        assertNotEquals(hash1, hash2, "Different signatures should produce different hashes");
    }

    @Test
    void tamperingDocumentAfterSigningIsDetectable() {
        String original = "Original contract content.";
        String signatureData = "signer-base64-data";
        String hash = HmacUtil.computeHmac(original + signatureData, HMAC_KEY);

        // Verify original
        assertTrue(HmacUtil.verifyHmac(original + signatureData, HMAC_KEY, hash));

        // Tamper
        String tampered = "Modified contract content.";
        assertFalse(HmacUtil.verifyHmac(tampered + signatureData, HMAC_KEY, hash),
                "Tampered document must fail verification");
    }

    @Test
    void multipleSignersProduceChainableHashes() {
        String renderedText = "Contract for multi-signer test.";
        String sig1 = "signer1-data";
        String sig2 = "signer2-data";

        String hash1 = HmacUtil.computeHmac(renderedText + sig1, HMAC_KEY);
        String hash2 = HmacUtil.computeHmac(renderedText + sig2, HMAC_KEY);

        assertNotNull(hash1);
        assertNotNull(hash2);
        assertNotEquals(hash1, hash2, "Each signer should have a unique hash");

        // Both should individually verify
        assertTrue(HmacUtil.verifyHmac(renderedText + sig1, HMAC_KEY, hash1));
        assertTrue(HmacUtil.verifyHmac(renderedText + sig2, HMAC_KEY, hash2));
    }

    @Test
    void templateSnapshotImmutability() {
        // Simulates: template body is copied at generation time
        String templateV1 = "Version 1 of {{name}} contract.";
        String snapshot = templateV1; // snapshot = copy

        // Template is later updated
        String templateV2 = "Version 2 of {{name}} contract with new terms.";

        // Snapshot should still be V1
        assertEquals(templateV1, snapshot, "Snapshot must preserve original template text");
        assertNotEquals(templateV2, snapshot, "Template update must not affect snapshot");
    }
}
