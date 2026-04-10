package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;

public class ContractTemplateCreateDTO {
    @NotBlank(message = "name is required")
    private String name;
    private String description;
    @NotBlank(message = "body is required")
    private String body;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
}
