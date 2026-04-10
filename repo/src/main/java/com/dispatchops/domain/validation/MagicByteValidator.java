package com.dispatchops.domain.validation;

/**
 * Validates file content by checking magic bytes (file signatures)
 * to determine if the content matches a supported file format.
 */
public final class MagicByteValidator {

    // JPEG: FF D8 FF
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

    // PNG: 89 50 4E 47
    private static final byte[] PNG_MAGIC = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47};

    // PDF: 25 50 44 46
    private static final byte[] PDF_MAGIC = {(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46};

    private static final byte[][] SUPPORTED_SIGNATURES = {JPEG_MAGIC, PNG_MAGIC, PDF_MAGIC};

    private MagicByteValidator() {
        // utility class
    }

    /**
     * Validates that the given content starts with a recognized magic byte sequence.
     *
     * @param content the file content bytes
     * @return true if the content starts with a supported file signature
     */
    public static boolean isValid(byte[] content) {
        if (content == null || content.length == 0) {
            return false;
        }

        for (byte[] signature : SUPPORTED_SIGNATURES) {
            if (startsWith(content, signature)) {
                return true;
            }
        }
        return false;
    }

    private static boolean startsWith(byte[] content, byte[] signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if (content[i] != signature[i]) {
                return false;
            }
        }
        return true;
    }
}
