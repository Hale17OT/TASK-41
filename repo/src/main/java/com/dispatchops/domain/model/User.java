package com.dispatchops.domain.model;

import com.dispatchops.domain.model.enums.Role;
import java.time.LocalDateTime;

public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private String displayName;
    private byte[] emailEncrypted;
    private byte[] phoneEncrypted;
    private boolean isActive;
    private boolean mustChangePassword;
    private int failedAttempts;
    private LocalDateTime lockoutExpiry;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public byte[] getEmailEncrypted() {
        return emailEncrypted;
    }

    public void setEmailEncrypted(byte[] emailEncrypted) {
        this.emailEncrypted = emailEncrypted;
    }

    public byte[] getPhoneEncrypted() {
        return phoneEncrypted;
    }

    public void setPhoneEncrypted(byte[] phoneEncrypted) {
        this.phoneEncrypted = phoneEncrypted;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public LocalDateTime getLockoutExpiry() {
        return lockoutExpiry;
    }

    public void setLockoutExpiry(LocalDateTime lockoutExpiry) {
        this.lockoutExpiry = lockoutExpiry;
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
