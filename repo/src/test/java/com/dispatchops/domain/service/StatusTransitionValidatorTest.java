package com.dispatchops.domain.service;

import com.dispatchops.domain.exception.StaleTransitionException;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TDD tests for status transition validation.
 * Written BEFORE implementation per dev plan.
 */
class StatusTransitionValidatorTest {

    // === Job Status Transitions ===

    @Test
    void createdToPickedIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.CREATED, JobStatus.PICKED));
    }

    @Test
    void pickedToInTransitIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.PICKED, JobStatus.IN_TRANSIT));
    }

    @Test
    void inTransitToDeliveredIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.IN_TRANSIT, JobStatus.DELIVERED));
    }

    @Test
    void anyStatusToExceptionIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.CREATED, JobStatus.EXCEPTION));
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.PICKED, JobStatus.EXCEPTION));
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.IN_TRANSIT, JobStatus.EXCEPTION));
    }

    @Test
    void deliveredToPickedIsInvalid() {
        assertThrows(StaleTransitionException.class, () ->
                StatusTransitionValidator.validateJobTransition(JobStatus.DELIVERED, JobStatus.PICKED));
    }

    @Test
    void exceptionToAnythingIsInvalid() {
        assertThrows(StaleTransitionException.class, () ->
                StatusTransitionValidator.validateJobTransition(JobStatus.EXCEPTION, JobStatus.CREATED));
    }

    @Test
    void createdToManualValidationIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.CREATED, JobStatus.MANUAL_VALIDATION));
    }

    @Test
    void manualValidationToPickedIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateJobTransition(JobStatus.MANUAL_VALIDATION, JobStatus.PICKED));
    }

    @Test
    void deliveredIsTerminal() {
        assertTrue(StatusTransitionValidator.getAllowedJobTransitions(JobStatus.DELIVERED).isEmpty());
    }

    @Test
    void exceptionIsTerminal() {
        assertTrue(StatusTransitionValidator.getAllowedJobTransitions(JobStatus.EXCEPTION).isEmpty());
    }

    // === Payment Status Transitions ===

    @Test
    void pendingToSettledIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validatePaymentTransition(
                PaymentStatus.PENDING_SETTLEMENT, PaymentStatus.SETTLED));
    }

    @Test
    void settledToRefundPendingIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validatePaymentTransition(
                PaymentStatus.SETTLED, PaymentStatus.REFUND_PENDING));
    }

    @Test
    void refundPendingToRefundedIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validatePaymentTransition(
                PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED));
    }

    @Test
    void refundedIsTerminal() {
        assertTrue(StatusTransitionValidator.getAllowedPaymentTransitions(PaymentStatus.REFUNDED).isEmpty());
    }

    @Test
    void pendingDirectToRefundedIsInvalid() {
        assertThrows(StaleTransitionException.class, () ->
                StatusTransitionValidator.validatePaymentTransition(
                        PaymentStatus.PENDING_SETTLEMENT, PaymentStatus.REFUNDED));
    }

    // === Task Status Transitions ===

    @Test
    void todoToInProgressIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(TaskStatus.TODO, TaskStatus.IN_PROGRESS));
    }

    @Test
    void todoToBlockedIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(TaskStatus.TODO, TaskStatus.BLOCKED));
    }

    @Test
    void inProgressToDoneIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(TaskStatus.IN_PROGRESS, TaskStatus.DONE));
    }

    @Test
    void doneIsTerminal() {
        assertTrue(StatusTransitionValidator.getAllowedTaskTransitions(TaskStatus.DONE).isEmpty());
    }

    @Test
    void blockedToInProgressIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(TaskStatus.BLOCKED, TaskStatus.IN_PROGRESS));
    }
}
