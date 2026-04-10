package com.dispatchops.domain.model;

import java.time.LocalDateTime;

public class UserProfile {

    private Long id;
    private Long userId;
    private String bio;
    private byte[] idNumberEncrypted;
    private byte[] addressEncrypted;
    private byte[] emergencyContactEncrypted;
    private String avatarPath;
    private int visibilityLevel;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public byte[] getIdNumberEncrypted() {
        return idNumberEncrypted;
    }

    public void setIdNumberEncrypted(byte[] idNumberEncrypted) {
        this.idNumberEncrypted = idNumberEncrypted;
    }

    public byte[] getAddressEncrypted() {
        return addressEncrypted;
    }

    public void setAddressEncrypted(byte[] addressEncrypted) {
        this.addressEncrypted = addressEncrypted;
    }

    public byte[] getEmergencyContactEncrypted() {
        return emergencyContactEncrypted;
    }

    public void setEmergencyContactEncrypted(byte[] emergencyContactEncrypted) {
        this.emergencyContactEncrypted = emergencyContactEncrypted;
    }

    public String getAvatarPath() {
        return avatarPath;
    }

    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public int getVisibilityLevel() {
        return visibilityLevel;
    }

    public void setVisibilityLevel(int visibilityLevel) {
        this.visibilityLevel = visibilityLevel;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
