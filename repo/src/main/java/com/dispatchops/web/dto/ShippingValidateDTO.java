package com.dispatchops.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class ShippingValidateDTO {
    @NotBlank(message = "state is required")
    private String state;

    @NotBlank(message = "zip is required")
    private String zip;

    @NotNull(message = "weightLbs is required")
    @DecimalMin(value = "0.01", message = "weightLbs must be positive")
    private BigDecimal weightLbs;

    @NotNull(message = "orderAmount is required")
    @DecimalMin(value = "0.01", message = "orderAmount must be positive")
    private BigDecimal orderAmount;

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getZip() { return zip; }
    public void setZip(String zip) { this.zip = zip; }
    public BigDecimal getWeightLbs() { return weightLbs; }
    public void setWeightLbs(BigDecimal weightLbs) { this.weightLbs = weightLbs; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
}
