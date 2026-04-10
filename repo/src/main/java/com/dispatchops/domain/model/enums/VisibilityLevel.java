package com.dispatchops.domain.model.enums;

public enum VisibilityLevel {
    PUBLIC(1),
    INTERNAL(2),
    MANAGEMENT(3),
    ADMIN(4);

    private final int level;

    VisibilityLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public static VisibilityLevel fromRole(Role role) {
        switch (role) {
            case COURIER:
                return PUBLIC;
            case DISPATCHER:
                return INTERNAL;
            case OPS_MANAGER:
                return MANAGEMENT;
            case ADMIN:
                return ADMIN;
            case AUDITOR:
                return ADMIN;
            default:
                throw new IllegalArgumentException("Unknown role: " + role);
        }
    }
}
