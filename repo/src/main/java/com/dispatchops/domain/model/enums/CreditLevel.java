package com.dispatchops.domain.model.enums;

public enum CreditLevel {
    A(8),
    B(5),
    C(3),
    D(1);

    private final int maxConcurrent;

    CreditLevel(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }
}
