package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.EventType;
import com.dispatchops.domain.model.enums.JobStatus;
import java.time.LocalDateTime;

public class FulfillmentEvent {

    private Long id;
    private Long jobId;
    private EventType eventType;
    private JobStatus oldStatus;
    private JobStatus newStatus;
    private Long adjustmentRef;
    private Long actorId;
    private String comment;
    private Long deviceSeqId;
    private String deviceId;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public JobStatus getOldStatus() {
        return oldStatus;
    }

    public void setOldStatus(JobStatus oldStatus) {
        this.oldStatus = oldStatus;
    }

    public JobStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(JobStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Long getAdjustmentRef() {
        return adjustmentRef;
    }

    public void setAdjustmentRef(Long adjustmentRef) {
        this.adjustmentRef = adjustmentRef;
    }

    public Long getActorId() {
        return actorId;
    }

    public void setActorId(Long actorId) {
        this.actorId = actorId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Long getDeviceSeqId() {
        return deviceSeqId;
    }

    public void setDeviceSeqId(Long deviceSeqId) {
        this.deviceSeqId = deviceSeqId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
