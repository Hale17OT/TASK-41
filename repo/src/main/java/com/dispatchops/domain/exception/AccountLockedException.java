package com.dispatchops.domain.exception;

public class AccountLockedException extends RuntimeException {

    private final long remainingSeconds;

    public AccountLockedException(String message, long remainingSeconds) {
        super(message);
        this.remainingSeconds = remainingSeconds;
    }

    public long getRemainingSeconds() {
        return remainingSeconds;
    }
}
