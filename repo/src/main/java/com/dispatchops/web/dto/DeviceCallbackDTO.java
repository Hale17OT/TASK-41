package com.dispatchops.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Validated DTO for device callback requests.
 * All fields are mandatory for proper HMAC verification and freshness checks.
 */
public class DeviceCallbackDTO {

    @NotBlank(message = "deviceId is required")
    private String deviceId;

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotBlank(message = "payload is required")
    private String payload;

    @NotBlank(message = "signature is required (HMAC-SHA256 of canonical string)")
    private String signature;

    @NotBlank(message = "timestamp is required (ISO-8601)")
    private String timestamp;

    private String deviceSeqId;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    public String getDeviceSeqId() { return deviceSeqId; }
    public void setDeviceSeqId(String deviceSeqId) { this.deviceSeqId = deviceSeqId; }
}
