package com.dispatchops.domain.exception;

import java.util.List;

public class StaleTransitionException extends RuntimeException {

    private final String currentStatus;
    private final String attemptedStatus;
    private final List<String> allowedStatuses;

    public StaleTransitionException(String message, String currentStatus, String attemptedStatus, List<String> allowedStatuses) {
        super(message);
        this.currentStatus = currentStatus;
        this.attemptedStatus = attemptedStatus;
        this.allowedStatuses = allowedStatuses;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public String getAttemptedStatus() {
        return attemptedStatus;
    }

    public List<String> getAllowedStatuses() {
        return allowedStatuses;
    }
}
