package com.dispatchops.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class VisibilityUpdateDTO {
    @NotBlank(message = "field is required")
    @Pattern(regexp = "email|phone|bio|address|emergencyContact|idNumber",
             message = "field must be one of: email, phone, bio, address, emergencyContact, idNumber")
    private String field;

    @Min(value = 1, message = "tier must be between 1 (Public) and 4 (Admin)")
    @Max(value = 4, message = "tier must be between 1 (Public) and 4 (Admin)")
    private int tier;

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public int getTier() { return tier; }
    public void setTier(int tier) { this.tier = tier; }
}
