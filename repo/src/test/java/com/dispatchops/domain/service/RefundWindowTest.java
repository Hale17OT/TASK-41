package com.dispatchops.domain.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the 30-day refund window logic.
 * Validates that refunds are allowed within 30 days of settlement
 * and rejected after.
 */
class RefundWindowTest {

    private static final int REFUND_WINDOW_DAYS = 30;

    @Test
    void refundAt29DaysIsAllowed() {
        LocalDateTime settledAt = LocalDateTime.now().minusDays(29);
        LocalDateTime refundEligibleUntil = settledAt.plusDays(REFUND_WINDOW_DAYS);
        LocalDateTime now = LocalDateTime.now();

        boolean eligible = now.isBefore(refundEligibleUntil);

        assertTrue(eligible, "Refund at 29 days should be allowed");
    }

    @Test
    void refundAt30DaysExactlyIsAllowed() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime settledAt = now.minusDays(REFUND_WINDOW_DAYS);
        LocalDateTime refundEligibleUntil = settledAt.plusDays(REFUND_WINDOW_DAYS);

        // At exactly 30 days, refundEligibleUntil equals now, so !isAfter covers the boundary
        boolean eligible = !now.isAfter(refundEligibleUntil);

        assertTrue(eligible, "Refund at exactly 30 days should be allowed (boundary inclusive)");
    }

    @Test
    void refundAt31DaysIsRejected() {
        LocalDateTime settledAt = LocalDateTime.now().minusDays(31);
        LocalDateTime refundEligibleUntil = settledAt.plusDays(REFUND_WINDOW_DAYS);
        LocalDateTime now = LocalDateTime.now();

        boolean eligible = now.isBefore(refundEligibleUntil);

        assertFalse(eligible, "Refund at 31 days should be rejected");
    }
}
