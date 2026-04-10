package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public class TaskCreateDTO {

    @NotBlank
    @Size(max = 256)
    private String title;

    private String body;

    @NotNull
    private Long assigneeId;

    private List<Long> ccUserIds;

    @NotNull
    private LocalDateTime dueTime;

    private boolean showOnCalendar = true;

    private Long jobId;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public List<Long> getCcUserIds() {
        return ccUserIds;
    }

    public void setCcUserIds(List<Long> ccUserIds) {
        this.ccUserIds = ccUserIds;
    }

    public LocalDateTime getDueTime() {
        return dueTime;
    }

    public void setDueTime(LocalDateTime dueTime) {
        this.dueTime = dueTime;
    }

    public boolean isShowOnCalendar() {
        return showOnCalendar;
    }

    public void setShowOnCalendar(boolean showOnCalendar) {
        this.showOnCalendar = showOnCalendar;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }
}
