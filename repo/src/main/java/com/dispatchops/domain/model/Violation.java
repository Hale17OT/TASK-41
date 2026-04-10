package com.dispatchops.domain.model;

import java.time.LocalDateTime;

public class Violation {

    private Long id;
    private Long courierId;
    private Long jobId;
    private String violationType;
    private String description;
    private LocalDateTime penaltyStart;
    private LocalDateTime penaltyEnd;
    private Long issuedBy;
    private boolean isActive;
    private LocalDateTime createdAt;

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

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public String getViolationType() {
        return violationType;
    }

    public void setViolationType(String violationType) {
        this.violationType = violationType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getPenaltyStart() {
        return penaltyStart;
    }

    public void setPenaltyStart(LocalDateTime penaltyStart) {
        this.penaltyStart = penaltyStart;
    }

    public LocalDateTime getPenaltyEnd() {
        return penaltyEnd;
    }

    public void setPenaltyEnd(LocalDateTime penaltyEnd) {
        this.penaltyEnd = penaltyEnd;
    }

    public Long getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(Long issuedBy) {
        this.issuedBy = issuedBy;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
