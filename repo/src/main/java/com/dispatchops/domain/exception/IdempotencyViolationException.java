package com.dispatchops.domain.exception;

public class IdempotencyViolationException extends RuntimeException {

    private final Long existingPaymentId;

    public IdempotencyViolationException(String message, Long existingPaymentId) {
        super(message);
        this.existingPaymentId = existingPaymentId;
    }

    public Long getExistingPaymentId() {
        return existingPaymentId;
    }
}
