package com.dispatchops.domain.service;

import com.dispatchops.domain.model.enums.CreditLevel;

import java.math.BigDecimal;

/**
 * Pure domain service for credit level calculation.
 * No Spring dependency -- testable in isolation.
 *
 * Formula:
 *   avgScore >= 4.5 AND activeViolations == 0 -> A (max 8 concurrent)
 *   avgScore >= 3.5 AND activeViolations <= 1 -> B (max 5 concurrent)
 *   avgScore >= 2.5 AND activeViolations <= 3 -> C (max 3 concurrent)
 *   else -> D (max 1 concurrent)
 */
public final class CreditLevelCalculator {

    private static final BigDecimal THRESHOLD_A = new BigDecimal("4.5");
    private static final BigDecimal THRESHOLD_B = new BigDecimal("3.5");
    private static final BigDecimal THRESHOLD_C = new BigDecimal("2.5");

    private CreditLevelCalculator() {}

    /**
     * Calculate credit level from 30-day average rating and active violation count.
     *
     * @param avgRating30d 30-day rolling average (can be null if no ratings)
     * @param activeViolations count of violations where penalty_end > now AND is_active
     * @return the calculated CreditLevel
     */
    public static CreditLevel calculate(BigDecimal avgRating30d, int activeViolations) {
        if (avgRating30d == null) {
            return CreditLevel.D;
        }

        if (avgRating30d.compareTo(THRESHOLD_A) >= 0 && activeViolations == 0) {
            return CreditLevel.A;
        }
        if (avgRating30d.compareTo(THRESHOLD_B) >= 0 && activeViolations <= 1) {
            return CreditLevel.B;
        }
        if (avgRating30d.compareTo(THRESHOLD_C) >= 0 && activeViolations <= 3) {
            return CreditLevel.C;
        }
        return CreditLevel.D;
    }
}
