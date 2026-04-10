package com.dispatchops.domain.exception;

public class OptimisticLockException extends RuntimeException {

    private final String entityType;
    private final Long entityId;
    private final int currentVersion;

    public OptimisticLockException(String message, String entityType, Long entityId, int currentVersion) {
        super(message);
        this.entityType = entityType;
        this.entityId = entityId;
        this.currentVersion = currentVersion;
    }

    public String getEntityType() {
        return entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public int getCurrentVersion() {
        return currentVersion;
    }
}
