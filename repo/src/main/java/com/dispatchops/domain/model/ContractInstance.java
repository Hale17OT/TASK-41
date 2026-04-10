package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.ContractStatus;
import java.time.LocalDateTime;

public class ContractInstance {

    private Long id;
    private Long templateVersionId;
    private String snapshotBodyText;
    private String renderedText;
    private String placeholderValues;
    private Long jobId;
    private Long generatedBy;
    private ContractStatus status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTemplateVersionId() {
        return templateVersionId;
    }

    public void setTemplateVersionId(Long templateVersionId) {
        this.templateVersionId = templateVersionId;
    }

    public String getSnapshotBodyText() {
        return snapshotBodyText;
    }

    public void setSnapshotBodyText(String snapshotBodyText) {
        this.snapshotBodyText = snapshotBodyText;
    }

    public String getRenderedText() {
        return renderedText;
    }

    public void setRenderedText(String renderedText) {
        this.renderedText = renderedText;
    }

    public String getPlaceholderValues() {
        return placeholderValues;
    }

    public void setPlaceholderValues(String placeholderValues) {
        this.placeholderValues = placeholderValues;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public Long getGeneratedBy() {
        return generatedBy;
    }

    public void setGeneratedBy(Long generatedBy) {
        this.generatedBy = generatedBy;
    }

    public ContractStatus getStatus() {
        return status;
    }

    public void setStatus(ContractStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
