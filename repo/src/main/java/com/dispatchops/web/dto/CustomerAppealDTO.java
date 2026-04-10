package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomerAppealDTO {
    @NotBlank private String trackingNumber;
    @NotBlank private String receiverName;
    @NotBlank private String customerToken;
    @NotBlank private String reason;
    private Long ratingId;

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String v) { this.trackingNumber = v; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }
    public String getCustomerToken() { return customerToken; }
    public void setCustomerToken(String v) { this.customerToken = v; }
    public String getReason() { return reason; }
    public void setReason(String v) { this.reason = v; }
    public Long getRatingId() { return ratingId; }
    public void setRatingId(Long v) { this.ratingId = v; }
}
