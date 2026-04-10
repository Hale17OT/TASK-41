package com.dispatchops.application.service;

import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.MediaAttachment;
import com.dispatchops.domain.model.ProfileChangeLog;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.UserProfile;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.domain.model.enums.VisibilityLevel;
import com.dispatchops.infrastructure.persistence.mapper.MediaAttachmentMapper;
import com.dispatchops.infrastructure.persistence.mapper.ProfileChangeLogMapper;
import com.dispatchops.infrastructure.persistence.mapper.SearchIndexMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserProfileMapper;
import org.springframework.beans.factory.annotation.Value;
import com.dispatchops.infrastructure.security.FieldEncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileService.class);

    private static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;  // 5MB
    private static final long MAX_PDF_SIZE = 10L * 1024 * 1024;   // 10MB

    private final String uploadBasePath;
    private final UserProfileMapper userProfileMapper;
    private final MediaAttachmentMapper mediaAttachmentMapper;
    private final ProfileChangeLogMapper profileChangeLogMapper;
    private final UserMapper userMapper;
    private final FieldEncryptionService fieldEncryptionService;
    private final SearchIndexMapper searchIndexMapper;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private SearchService searchService;
    private final com.dispatchops.infrastructure.persistence.mapper.ProfileFieldVisibilityMapper fieldVisibilityMapper;

    public ProfileService(UserProfileMapper userProfileMapper,
                          MediaAttachmentMapper mediaAttachmentMapper,
                          ProfileChangeLogMapper profileChangeLogMapper,
                          UserMapper userMapper,
                          FieldEncryptionService fieldEncryptionService,
                          SearchIndexMapper searchIndexMapper,
                          com.dispatchops.infrastructure.persistence.mapper.ProfileFieldVisibilityMapper fieldVisibilityMapper,
                          @Value("${upload.base-path:./uploads}") String uploadBasePath) {
        this.uploadBasePath = uploadBasePath;
        this.fieldVisibilityMapper = fieldVisibilityMapper;
        this.userProfileMapper = userProfileMapper;
        this.mediaAttachmentMapper = mediaAttachmentMapper;
        this.profileChangeLogMapper = profileChangeLogMapper;
        this.userMapper = userMapper;
        this.fieldEncryptionService = fieldEncryptionService;
        this.searchIndexMapper = searchIndexMapper;
    }

    /** Fetch fresh User record from DB for profile composition. */
    public User getUserForProfile(Long userId) {
        return userMapper.findById(userId);
    }

    /** Decrypt an AES-encrypted field (phone, email, etc.) from bytes. Returns null on failure. */
    public String decryptField(byte[] encrypted) {
        return fieldEncryptionService.decrypt(encrypted);
    }

    public UserProfile getProfile(Long userId, Long viewerId, String viewerRole) {
        log.debug("Fetching profile for userId={} by viewerId={} with role={}", userId, viewerId, viewerRole);

        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            throw new ResourceNotFoundException(
                    "Profile not found for userId: " + userId, "UserProfile", userId);
        }

        // Owner always sees everything
        if (userId.equals(viewerId)) {
            log.debug("Owner access for profile {}", userId);
            return profile;
        }

        Role role = Role.valueOf(viewerRole);
        int viewerLevel = VisibilityLevel.fromRole(role).getLevel();

        // Load per-field visibility overrides
        java.util.Map<String, Integer> fieldTiers = new java.util.HashMap<>();
        // Defaults: bio=PUBLIC(1), email=INTERNAL(2), phone=MANAGEMENT(3), idNumber=ADMIN(4),
        // address=MANAGEMENT(3), emergencyContact=ADMIN(4)
        fieldTiers.put("bio", 1);
        fieldTiers.put("email", 2);
        fieldTiers.put("phone", 3);
        fieldTiers.put("idNumber", 4);
        fieldTiers.put("address", 3);
        fieldTiers.put("emergencyContact", 4);

        // Override with per-field settings from DB
        java.util.List<java.util.Map<String, Object>> overrides = fieldVisibilityMapper.findByUserId(userId);
        for (java.util.Map<String, Object> row : overrides) {
            String fieldName = (String) row.get("fieldName");
            Object tierObj = row.get("visibilityTier");
            int tier = tierObj instanceof Number ? ((Number) tierObj).intValue() : 1;
            fieldTiers.put(fieldName, tier);
        }

        // Apply per-field visibility: null out fields the viewer cannot see
        if (viewerLevel < fieldTiers.getOrDefault("bio", 1)) {
            profile.setBio(null);
        }
        if (viewerLevel < fieldTiers.getOrDefault("idNumber", 4)) {
            profile.setIdNumberEncrypted(null);
        }
        if (viewerLevel < fieldTiers.getOrDefault("address", 3)) {
            profile.setAddressEncrypted(null);
        }
        if (viewerLevel < fieldTiers.getOrDefault("emergencyContact", 4)) {
            profile.setEmergencyContactEncrypted(null);
        }
        log.debug("Per-field visibility applied for viewer {} (level {}) on profile {}", viewerId, viewerLevel, userId);

        return profile;
    }

    public UserProfile updateProfile(Long userId, Map<String, String> updates, Long actorId) {
        log.info("Updating profile for userId={} by actorId={}", userId, actorId);

        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            throw new ResourceNotFoundException(
                    "Profile not found for userId: " + userId, "UserProfile", userId);
        }

        // Only the owner or an ADMIN can update a profile
        if (!userId.equals(actorId)) {
            User actor = userMapper.findById(actorId);
            if (actor == null || actor.getRole() != Role.ADMIN) {
                throw new PermissionDeniedException(
                        "Only the profile owner or an ADMIN can update this profile");
            }
        }

        for (Map.Entry<String, String> entry : updates.entrySet()) {
            String field = entry.getKey();
            String newValue = entry.getValue();

            String maskedValue = maskValue(newValue);

            switch (field) {
                case "bio":
                    logChange(profile.getId(), field, profile.getBio(), maskedValue, actorId);
                    profile.setBio(newValue);
                    break;
                case "displayName":
                    User user = userMapper.findById(userId);
                    if (user != null) {
                        logChange(profile.getId(), field, user.getDisplayName(), maskedValue, actorId);
                        user.setDisplayName(newValue);
                        user.setUpdatedAt(LocalDateTime.now());
                        userMapper.update(user);
                    }
                    break;
                case "email":
                    User emailUser = userMapper.findById(userId);
                    if (emailUser != null) {
                        logChange(profile.getId(), field, "***", maskedValue, actorId);
                        emailUser.setEmailEncrypted(fieldEncryptionService.encrypt(newValue));
                        emailUser.setUpdatedAt(LocalDateTime.now());
                        userMapper.update(emailUser);
                    }
                    break;
                case "phone":
                    User phoneUser = userMapper.findById(userId);
                    if (phoneUser != null) {
                        logChange(profile.getId(), field, "***", maskedValue, actorId);
                        phoneUser.setPhoneEncrypted(fieldEncryptionService.encrypt(newValue));
                        phoneUser.setUpdatedAt(LocalDateTime.now());
                        userMapper.update(phoneUser);
                    }
                    break;
                case "idNumber":
                    logChange(profile.getId(), field, "***", maskedValue, actorId);
                    profile.setIdNumberEncrypted(fieldEncryptionService.encrypt(newValue));
                    break;
                case "address":
                    logChange(profile.getId(), field, "***", maskedValue, actorId);
                    profile.setAddressEncrypted(fieldEncryptionService.encrypt(newValue));
                    break;
                case "emergencyContact":
                    logChange(profile.getId(), field, "***", maskedValue, actorId);
                    profile.setEmergencyContactEncrypted(fieldEncryptionService.encrypt(newValue));
                    break;
                case "avatarPath":
                    logChange(profile.getId(), field, profile.getAvatarPath(), newValue, actorId);
                    profile.setAvatarPath(newValue);
                    break;
                default:
                    log.warn("Unknown profile field '{}' - skipping", field);
                    break;
            }
        }

        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.update(profile);

        log.info("Profile for userId={} updated successfully", userId);

        if (searchService != null) {
            try { searchService.indexEntity("PROFILE", userId,
                    profile.getBio() != null ? profile.getBio().substring(0, Math.min(100, profile.getBio().length())) : "",
                    "", "", userId); }
            catch (Exception e) { log.warn("Failed to index profile: {}", e.getMessage()); }
        }

        return profile;
    }

    public MediaAttachment uploadMedia(Long userId, byte[] content, String filename,
                                        String mimeType, Long actorId) {
        log.info("Uploading media for userId={} by actorId={}, filename={}", userId, actorId, filename);

        if (content == null || content.length == 0) {
            throw new IllegalArgumentException("File content cannot be empty");
        }

        // Validate magic bytes
        String detectedType = detectFileType(content);
        if (detectedType == null) {
            throw new IllegalArgumentException("Unsupported file format");
        }

        // Validate size
        if ("pdf".equals(detectedType)) {
            if (content.length > MAX_PDF_SIZE) {
                throw new IllegalArgumentException(
                        "PDF file exceeds maximum size of 10MB. Actual size: " + content.length + " bytes");
            }
        } else {
            // Image (jpeg or png)
            if (content.length > MAX_IMAGE_SIZE) {
                throw new IllegalArgumentException(
                        "Image file exceeds maximum size of 5MB. Actual size: " + content.length + " bytes");
            }
        }

        String extension = getExtension(detectedType);
        String generatedFilename = UUID.randomUUID().toString() + "." + extension;
        String relativePath = uploadBasePath + "/" + userId + "/" + generatedFilename;

        // Write file to disk
        try {
            Path filePath = Paths.get(relativePath);
            Files.createDirectories(filePath.getParent());
            Files.write(filePath, content);
            log.debug("File written to {}", relativePath);
        } catch (IOException e) {
            log.error("Failed to write file to disk: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save uploaded file", e);
        }

        MediaAttachment attachment = new MediaAttachment();
        attachment.setEntityType("UserProfile");
        attachment.setEntityId(userId);
        attachment.setUploaderId(actorId);
        attachment.setFileName(filename);
        attachment.setFilePath(relativePath);
        attachment.setFileSize(content.length);
        attachment.setMimeType(mimeType);
        attachment.setVisibilityLevel(VisibilityLevel.PUBLIC.getLevel());
        attachment.setActive(true);
        attachment.setCreatedAt(LocalDateTime.now());

        mediaAttachmentMapper.insert(attachment);
        log.info("Media attachment id={} created for userId={}", attachment.getId(), userId);

        return attachment;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public java.util.List<java.util.Map<String, Object>> getFieldVisibility(Long userId) {
        log.debug("Fetching field visibility for userId={}", userId);
        return fieldVisibilityMapper.findByUserId(userId);
    }

    public void updateVisibility(Long userId, String field, int tier, Long ownerId) {
        log.info("Updating visibility for userId={}, field={}, tier={} by ownerId={}", userId, field, tier, ownerId);

        if (!userId.equals(ownerId)) {
            throw new PermissionDeniedException("Only the profile owner can change visibility settings");
        }

        UserProfile profile = userProfileMapper.findByUserId(userId);
        if (profile == null) {
            throw new ResourceNotFoundException(
                    "Profile not found for userId: " + userId, "UserProfile", userId);
        }

        // Persist per-field visibility
        fieldVisibilityMapper.upsert(userId, field, tier);

        profile.setUpdatedAt(LocalDateTime.now());
        userProfileMapper.update(profile);

        log.info("Visibility updated for userId={} to tier={}", userId, tier);
    }

    private void logChange(Long profileId, String fieldName, String oldValue,
                            String newValue, Long changedBy) {
        ProfileChangeLog changeLog = new ProfileChangeLog();
        changeLog.setProfileId(profileId);
        changeLog.setFieldName(fieldName);
        changeLog.setOldValueMasked(oldValue != null ? maskValue(oldValue) : null);
        changeLog.setNewValueMasked(newValue);
        changeLog.setChangedBy(changedBy);
        changeLog.setChangedAt(LocalDateTime.now());

        profileChangeLogMapper.insert(changeLog);
        log.debug("Change log recorded for profile {}, field '{}'", profileId, fieldName);
    }

    private String maskValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Email masking: j***@example.com
        if (value.contains("@")) {
            int atIndex = value.indexOf('@');
            if (atIndex > 1) {
                return value.charAt(0) + "***" + value.substring(atIndex);
            }
            return "***" + value.substring(atIndex);
        }

        // Phone masking: ***1234
        if (value.matches(".*\\d{4,}.*")) {
            if (value.length() > 4) {
                return "***" + value.substring(value.length() - 4);
            }
        }

        // General masking: first and last char with *** in between
        if (value.length() > 2) {
            return value.charAt(0) + "***" + value.charAt(value.length() - 1);
        }

        return "***";
    }

    private String detectFileType(byte[] content) {
        if (content.length < 4) {
            return null;
        }

        // JPEG: 0xFF 0xD8 0xFF
        if ((content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return "jpeg";
        }

        // PNG: 0x89 0x50 0x4E 0x47
        if ((content[0] & 0xFF) == 0x89
                && (content[1] & 0xFF) == 0x50
                && (content[2] & 0xFF) == 0x4E
                && (content[3] & 0xFF) == 0x47) {
            return "png";
        }

        // PDF: 0x25 0x50 0x44 0x46
        if ((content[0] & 0xFF) == 0x25
                && (content[1] & 0xFF) == 0x50
                && (content[2] & 0xFF) == 0x44
                && (content[3] & 0xFF) == 0x46) {
            return "pdf";
        }

        return null;
    }

    private String getExtension(String detectedType) {
        switch (detectedType) {
            case "jpeg":
                return "jpg";
            case "png":
                return "png";
            case "pdf":
                return "pdf";
            default:
                return "bin";
        }
    }
}
