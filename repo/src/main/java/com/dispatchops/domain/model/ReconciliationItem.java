package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.ReconStatus;
import java.time.LocalDateTime;

public class ReconciliationItem {

    private Long id;
    private Long paymentIdA;
    private Long paymentIdB;
    private String conflictType;
    private ReconStatus status;
    private Long resolvedBy;
    private String resolutionNote;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPaymentIdA() {
        return paymentIdA;
    }

    public void setPaymentIdA(Long paymentIdA) {
        this.paymentIdA = paymentIdA;
    }

    public Long getPaymentIdB() {
        return paymentIdB;
    }

    public void setPaymentIdB(Long paymentIdB) {
        this.paymentIdB = paymentIdB;
    }

    public String getConflictType() {
        return conflictType;
    }

    public void setConflictType(String conflictType) {
        this.conflictType = conflictType;
    }

    public ReconStatus getStatus() {
        return status;
    }

    public void setStatus(ReconStatus status) {
        this.status = status;
    }

    public Long getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(Long resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(LocalDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }
}
