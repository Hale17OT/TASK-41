package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.JobStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class DeliveryJob {

    private Long id;
    private String trackingNumber;
    private JobStatus status;
    private Long courierId;
    private Long dispatcherId;
    private String senderName;
    private String senderAddress;
    private byte[] senderPhoneEncrypted;
    private String receiverName;
    private String receiverAddress;
    private byte[] receiverPhoneEncrypted;
    private String receiverState;
    private String receiverZip;
    private BigDecimal weightLbs;
    private BigDecimal orderAmount;
    private String customerToken;
    private boolean adminOverride;
    private String overrideComment;
    private LocalDateTime lastEventAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public JobStatus getStatus() {
        return status;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public Long getCourierId() {
        return courierId;
    }

    public void setCourierId(Long courierId) {
        this.courierId = courierId;
    }

    public Long getDispatcherId() {
        return dispatcherId;
    }

    public void setDispatcherId(Long dispatcherId) {
        this.dispatcherId = dispatcherId;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public void setSenderAddress(String senderAddress) {
        this.senderAddress = senderAddress;
    }

    public byte[] getSenderPhoneEncrypted() {
        return senderPhoneEncrypted;
    }

    public void setSenderPhoneEncrypted(byte[] senderPhoneEncrypted) {
        this.senderPhoneEncrypted = senderPhoneEncrypted;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public byte[] getReceiverPhoneEncrypted() {
        return receiverPhoneEncrypted;
    }

    public void setReceiverPhoneEncrypted(byte[] receiverPhoneEncrypted) {
        this.receiverPhoneEncrypted = receiverPhoneEncrypted;
    }

    public String getReceiverState() {
        return receiverState;
    }

    public void setReceiverState(String receiverState) {
        this.receiverState = receiverState;
    }

    public String getReceiverZip() {
        return receiverZip;
    }

    public void setReceiverZip(String receiverZip) {
        this.receiverZip = receiverZip;
    }

    public BigDecimal getWeightLbs() {
        return weightLbs;
    }

    public void setWeightLbs(BigDecimal weightLbs) {
        this.weightLbs = weightLbs;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }

    public void setOrderAmount(BigDecimal orderAmount) {
        this.orderAmount = orderAmount;
    }

    public String getCustomerToken() { return customerToken; }
    public void setCustomerToken(String customerToken) { this.customerToken = customerToken; }

    public boolean isAdminOverride() {
        return adminOverride;
    }

    public void setAdminOverride(boolean adminOverride) {
        this.adminOverride = adminOverride;
    }

    public String getOverrideComment() {
        return overrideComment;
    }

    public void setOverrideComment(String overrideComment) {
        this.overrideComment = overrideComment;
    }

    public LocalDateTime getLastEventAt() {
        return lastEventAt;
    }

    public void setLastEventAt(LocalDateTime lastEventAt) {
        this.lastEventAt = lastEventAt;
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

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }
}
