package com.dispatchops.domain.model;

import java.time.LocalDateTime;

public class ProfileChangeLog {

    private Long id;
    private Long profileId;
    private String fieldName;
    private String oldValueMasked;
    private String newValueMasked;
    private Long changedBy;
    private LocalDateTime changedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValueMasked() {
        return oldValueMasked;
    }

    public void setOldValueMasked(String oldValueMasked) {
        this.oldValueMasked = oldValueMasked;
    }

    public String getNewValueMasked() {
        return newValueMasked;
    }

    public void setNewValueMasked(String newValueMasked) {
        this.newValueMasked = newValueMasked;
    }

    public Long getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(Long changedBy) {
        this.changedBy = changedBy;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
