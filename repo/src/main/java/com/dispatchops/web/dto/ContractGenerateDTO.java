package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class ContractGenerateDTO {

    @NotNull
    private Long templateVersionId;

    @NotNull
    private Map<String, String> placeholderValues;

    private List<Long> signerIds;

    private Long jobId;

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public void setTemplateVersionId(Long templateVersionId) {
        this.templateVersionId = templateVersionId;
    }

    public Map<String, String> getPlaceholderValues() {
        return placeholderValues;
    }

    public void setPlaceholderValues(Map<String, String> placeholderValues) {
        this.placeholderValues = placeholderValues;
    }

    public List<Long> getSignerIds() {
        return signerIds;
    }

    public void setSignerIds(List<Long> signerIds) {
        this.signerIds = signerIds;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
