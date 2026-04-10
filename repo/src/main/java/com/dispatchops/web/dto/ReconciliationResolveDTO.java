package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class ReconciliationResolveDTO {
    @NotBlank(message = "status is required")
    @Pattern(regexp = "RESOLVED|DISMISSED", message = "status must be RESOLVED or DISMISSED")
    private String status;
    private String note;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
