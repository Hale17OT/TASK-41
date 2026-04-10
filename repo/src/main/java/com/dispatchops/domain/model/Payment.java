package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.PaymentMethod;
import com.dispatchops.domain.model.enums.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {

    private Long id;
    private String idempotencyKey;
    private Long jobId;
    private Long payerId;
    private BigDecimal amount;
    private PaymentMethod method;
    private PaymentStatus status;
    private String checkNumber;
    private Long settledBy;
    private LocalDateTime settledAt;
    private LocalDateTime refundEligibleUntil;
    private String deviceId;
    private Long deviceSeqId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getPayerId() {
        return payerId;
    }

    public void setPayerId(Long payerId) {
        this.payerId = payerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public void setStatus(PaymentStatus status) {
        this.status = status;
    }

    public String getCheckNumber() {
        return checkNumber;
    }

    public void setCheckNumber(String checkNumber) {
        this.checkNumber = checkNumber;
    }

    public Long getSettledBy() {
        return settledBy;
    }

    public void setSettledBy(Long settledBy) {
        this.settledBy = settledBy;
    }

    public LocalDateTime getSettledAt() {
        return settledAt;
    }

    public void setSettledAt(LocalDateTime settledAt) {
        this.settledAt = settledAt;
    }

    public LocalDateTime getRefundEligibleUntil() {
        return refundEligibleUntil;
    }

    public void setRefundEligibleUntil(LocalDateTime refundEligibleUntil) {
        this.refundEligibleUntil = refundEligibleUntil;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Long getDeviceSeqId() {
        return deviceSeqId;
    }

    public void setDeviceSeqId(Long deviceSeqId) {
        this.deviceSeqId = deviceSeqId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
