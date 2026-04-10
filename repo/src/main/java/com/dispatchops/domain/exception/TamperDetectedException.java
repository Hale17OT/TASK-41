package com.dispatchops.domain.exception;

public class TamperDetectedException extends RuntimeException {

    public TamperDetectedException(String message) {
        super(message);
    }
}
