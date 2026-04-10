package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;

public class AppealResolveDTO {
    @NotBlank(message = "status is required (APPROVED or REJECTED)")
    private String status;
    private String comment;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}
