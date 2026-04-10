package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.InboxType;
import java.time.LocalDateTime;

public class TaskRecipient {

    private Long id;
    private Long taskId;
    private Long userId;
    private InboxType inboxType;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public InboxType getInboxType() {
        return inboxType;
    }

    public void setInboxType(InboxType inboxType) {
        this.inboxType = inboxType;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
