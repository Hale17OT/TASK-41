package com.dispatchops.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RegionRule {

    private Long id;
    private Long templateId;
    private String stateCode;
    private String zipRangeStart;
    private String zipRangeEnd;
    private BigDecimal minWeightLbs;
    private BigDecimal maxWeightLbs;
    private BigDecimal minOrderAmount;
    private BigDecimal maxOrderAmount;
    private boolean isAllowed;
    private int priority;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getStateCode() {
        return stateCode;
    }

    public void setStateCode(String stateCode) {
        this.stateCode = stateCode;
    }

    public String getZipRangeStart() {
        return zipRangeStart;
    }

    public void setZipRangeStart(String zipRangeStart) {
        this.zipRangeStart = zipRangeStart;
    }

    public String getZipRangeEnd() {
        return zipRangeEnd;
    }

    public void setZipRangeEnd(String zipRangeEnd) {
        this.zipRangeEnd = zipRangeEnd;
    }

    public BigDecimal getMinWeightLbs() {
        return minWeightLbs;
    }

    public void setMinWeightLbs(BigDecimal minWeightLbs) {
        this.minWeightLbs = minWeightLbs;
    }

    public BigDecimal getMaxWeightLbs() {
        return maxWeightLbs;
    }

    public void setMaxWeightLbs(BigDecimal maxWeightLbs) {
        this.maxWeightLbs = maxWeightLbs;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public void setMinOrderAmount(BigDecimal minOrderAmount) {
        this.minOrderAmount = minOrderAmount;
    }

    public BigDecimal getMaxOrderAmount() {
        return maxOrderAmount;
    }

    public void setMaxOrderAmount(BigDecimal maxOrderAmount) {
        this.maxOrderAmount = maxOrderAmount;
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    public void setAllowed(boolean allowed) {
        isAllowed = allowed;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
