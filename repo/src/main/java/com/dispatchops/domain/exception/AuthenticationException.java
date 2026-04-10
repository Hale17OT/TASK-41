package com.dispatchops.domain.exception;

public class AuthenticationException extends RuntimeException {

    private final int remainingAttempts;

    public AuthenticationException(String message) {
        this(message, -1);
    }

    public AuthenticationException(String message, int remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }

    public boolean hasRemainingAttempts() {
        return remainingAttempts >= 0;
    }
}
