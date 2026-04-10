package com.dispatchops.domain.service;

import com.dispatchops.domain.model.enums.CreditLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for credit level formula.
 * Written BEFORE implementation per dev plan.
 */
class CreditLevelCalculatorTest {

    @Test
    void levelA_whenAvgAbove4_5AndNoViolations() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("4.8"), 0);
        assertEquals(CreditLevel.A, result);
        assertEquals(8, result.getMaxConcurrent());
    }

    @Test
    void levelA_whenExactly4_5AndNoViolations() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("4.5"), 0);
        assertEquals(CreditLevel.A, result);
    }

    @Test
    void levelB_whenAvgAbove3_5AndOneViolation() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("4.0"), 1);
        assertEquals(CreditLevel.B, result);
        assertEquals(5, result.getMaxConcurrent());
    }

    @Test
    void levelB_whenHighAvgButOneViolation() {
        // Even with 4.8 avg, one violation drops from A to B
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("4.8"), 1);
        assertEquals(CreditLevel.B, result);
    }

    @Test
    void levelC_whenAvgAbove2_5AndThreeViolations() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("3.0"), 3);
        assertEquals(CreditLevel.C, result);
        assertEquals(3, result.getMaxConcurrent());
    }

    @Test
    void levelD_whenAvgBelow2_5() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("2.0"), 0);
        assertEquals(CreditLevel.D, result);
        assertEquals(1, result.getMaxConcurrent());
    }

    @Test
    void levelD_whenManyViolations() {
        CreditLevel result = CreditLevelCalculator.calculate(new BigDecimal("4.0"), 4);
        assertEquals(CreditLevel.D, result);
    }

    @Test
    void levelD_whenNullRating() {
        CreditLevel result = CreditLevelCalculator.calculate(null, 0);
        assertEquals(CreditLevel.D, result);
    }

    @Test
    void maxConcurrentMatchesLevel() {
        assertEquals(8, CreditLevel.A.getMaxConcurrent());
        assertEquals(5, CreditLevel.B.getMaxConcurrent());
        assertEquals(3, CreditLevel.C.getMaxConcurrent());
        assertEquals(1, CreditLevel.D.getMaxConcurrent());
    }
}
