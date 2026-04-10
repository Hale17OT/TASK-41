package com.dispatchops.domain.exception;

public class InsufficientCreditException extends RuntimeException {

    private final String currentLevel;
    private final int maxConcurrent;
    private final int activeJobs;

    public InsufficientCreditException(String message, String currentLevel, int maxConcurrent, int activeJobs) {
        super(message);
        this.currentLevel = currentLevel;
        this.maxConcurrent = maxConcurrent;
        this.activeJobs = activeJobs;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public int getActiveJobs() {
        return activeJobs;
    }
}
