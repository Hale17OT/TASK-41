package com.dispatchops.domain.exception;

public class RefundWindowClosedException extends RuntimeException {

    public RefundWindowClosedException(String message) {
        super(message);
    }
}
