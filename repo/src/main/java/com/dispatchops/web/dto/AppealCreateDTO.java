package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;

public class AppealCreateDTO {

    private Long ratingId;

    private Long violationId;

    @NotBlank
    private String reason;

    public Long getRatingId() {
        return ratingId;
    }

    public void setRatingId(Long ratingId) {
        this.ratingId = ratingId;
    }

    public Long getViolationId() {
        return violationId;
    }

    public void setViolationId(Long violationId) {
        this.violationId = violationId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
