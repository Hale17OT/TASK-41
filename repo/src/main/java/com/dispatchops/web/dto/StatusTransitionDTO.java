package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotNull;

public class StatusTransitionDTO {

    @NotNull
    private String status;

    private String comment;

    @NotNull
    private Integer version;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
