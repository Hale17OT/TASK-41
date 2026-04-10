package com.dispatchops.web.dto;

import jakarta.validation.constraints.*;

public class CustomerRatingDTO {
    @NotBlank private String trackingNumber;
    @NotBlank private String receiverName;
    @NotBlank private String customerToken;
    @NotNull @Min(1) @Max(5) private Integer timeliness;
    @NotNull @Min(1) @Max(5) private Integer attitude;
    @NotNull @Min(1) @Max(5) private Integer accuracy;
    private String comment;

    public String getTrackingNumber() { return trackingNumber; }
    public void setTrackingNumber(String v) { this.trackingNumber = v; }
    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String v) { this.receiverName = v; }
    public String getCustomerToken() { return customerToken; }
    public void setCustomerToken(String v) { this.customerToken = v; }
    public Integer getTimeliness() { return timeliness; }
    public void setTimeliness(Integer v) { this.timeliness = v; }
    public Integer getAttitude() { return attitude; }
    public void setAttitude(Integer v) { this.attitude = v; }
    public Integer getAccuracy() { return accuracy; }
    public void setAccuracy(Integer v) { this.accuracy = v; }
    public String getComment() { return comment; }
    public void setComment(String v) { this.comment = v; }
}
