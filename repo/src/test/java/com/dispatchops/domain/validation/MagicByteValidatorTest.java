package com.dispatchops.domain.validation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MagicByteValidator which checks file magic bytes
 * to validate supported media upload formats.
 */
class MagicByteValidatorTest {

    @Test
    void validJpegPasses() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        assertTrue(MagicByteValidator.isValid(jpeg), "JPEG magic bytes should be valid");
    }

    @Test
    void validPngPasses() {
        byte[] png = {(byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assertTrue(MagicByteValidator.isValid(png), "PNG magic bytes should be valid");
    }

    @Test
    void validPdfPasses() {
        byte[] pdf = {(byte) 0x25, (byte) 0x50, (byte) 0x44, (byte) 0x46, 0x2D, 0x31, 0x2E};
        assertTrue(MagicByteValidator.isValid(pdf), "PDF magic bytes should be valid");
    }

    @Test
    void invalidBytesFails() {
        byte[] random = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        assertFalse(MagicByteValidator.isValid(random), "Random bytes should be invalid");
    }

    @Test
    void emptyContentFails() {
        byte[] empty = {};
        assertFalse(MagicByteValidator.isValid(empty), "Empty content should be invalid");
    }
}
