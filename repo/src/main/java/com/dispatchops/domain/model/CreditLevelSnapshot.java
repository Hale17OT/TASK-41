package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.CreditLevel;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreditLevelSnapshot {

    private Long id;
    private Long courierId;
    private CreditLevel level;
    private int maxConcurrent;
    private BigDecimal avgRating30d;
    private int violationsActive;
    private LocalDateTime calculatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }

    public CreditLevel getLevel() {
        return level;
    }

    public void setLevel(CreditLevel level) {
        this.level = level;
    }

    public int getMaxConcurrent() {
        return maxConcurrent;
    }

    public void setMaxConcurrent(int maxConcurrent) {
        this.maxConcurrent = maxConcurrent;
    }

    public BigDecimal getAvgRating30d() {
        return avgRating30d;
    }

    public void setAvgRating30d(BigDecimal avgRating30d) {
        this.avgRating30d = avgRating30d;
    }

    public int getViolationsActive() {
        return violationsActive;
    }

    public void setViolationsActive(int violationsActive) {
        this.violationsActive = violationsActive;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }
}
