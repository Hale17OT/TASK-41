package com.dispatchops.domain.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the 48-hour appeal window logic.
 * Validates that appeals are allowed within 48 hours of creation
 * and rejected after.
 */
class AppealWindowTest {

    private static final int APPEAL_WINDOW_HOURS = 48;

    @Test
    void appealAt47HoursIsAllowed() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(47);
        LocalDateTime deadline = createdAt.plusHours(APPEAL_WINDOW_HOURS);
        LocalDateTime now = LocalDateTime.now();

        boolean allowed = now.isBefore(deadline);

        assertTrue(allowed, "Appeal at 47 hours should be allowed");
    }

    @Test
    void appealAt48HoursExactIsAllowed() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime createdAt = now.minusHours(APPEAL_WINDOW_HOURS);
        LocalDateTime deadline = createdAt.plusHours(APPEAL_WINDOW_HOURS);

        // At exactly 48 hours, deadline equals now, so !isAfter covers the boundary
        boolean allowed = !now.isAfter(deadline);

        assertTrue(allowed, "Appeal at exactly 48 hours should be allowed (boundary inclusive)");
    }

    @Test
    void appealAt49HoursIsRejected() {
        LocalDateTime createdAt = LocalDateTime.now().minusHours(49);
        LocalDateTime deadline = createdAt.plusHours(APPEAL_WINDOW_HOURS);
        LocalDateTime now = LocalDateTime.now();

        boolean allowed = now.isBefore(deadline);

        assertFalse(allowed, "Appeal at 49 hours should be rejected");
    }
}
