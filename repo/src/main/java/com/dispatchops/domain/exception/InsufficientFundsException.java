package com.dispatchops.domain.exception;

import java.math.BigDecimal;

public class InsufficientFundsException extends RuntimeException {

    private final BigDecimal available;
    private final BigDecimal requested;

    public InsufficientFundsException(String message, BigDecimal available, BigDecimal requested) {
        super(message);
        this.available = available;
        this.requested = requested;
    }

    public BigDecimal getAvailable() {
        return available;
    }

    public BigDecimal getRequested() {
        return requested;
    }
}
