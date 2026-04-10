package com.dispatchops.web.controller;

import com.dispatchops.application.service.ProfileService;
import com.dispatchops.domain.model.MediaAttachment;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.UserProfile;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.domain.model.enums.VisibilityLevel;
import com.dispatchops.infrastructure.persistence.mapper.MediaAttachmentMapper;
import com.dispatchops.web.dto.ApiResult;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
public class ProfileController {

    private static final Logger log = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private final MediaAttachmentMapper mediaAttachmentMapper;

    public ProfileController(ProfileService profileService,
                             MediaAttachmentMapper mediaAttachmentMapper) {
        this.profileService = profileService;
        this.mediaAttachmentMapper = mediaAttachmentMapper;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResult<java.util.Map<String, Object>>> getProfile(
            @PathVariable Long userId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("User '{}' getting profile for user {}", currentUser.getUsername(), userId);
        UserProfile profile = profileService.getProfile(userId, currentUser.getId(), currentUser.getRole().name());

        java.util.Map<String, Object> composite = new java.util.LinkedHashMap<>();
        composite.put("userId", profile.getUserId());
        composite.put("bio", profile.getBio());
        composite.put("avatarPath", profile.getAvatarPath());
        composite.put("visibilityLevel", profile.getVisibilityLevel());

        // Fetch fresh user data from DB and apply per-field visibility
        boolean isOwner = currentUser.getId().equals(userId);
        int viewerLevel = com.dispatchops.domain.model.enums.VisibilityLevel.fromRole(currentUser.getRole()).getLevel();
        try {
            com.dispatchops.domain.model.User dbUser = profileService.getUserForProfile(userId);
            if (dbUser != null) {
                composite.put("displayName", dbUser.getDisplayName()); // always visible

                // Apply per-field visibility for email/phone
                java.util.Map<String, Integer> fieldTiers = new java.util.HashMap<>();
                fieldTiers.put("email", 2); // default INTERNAL
                fieldTiers.put("phone", 3); // default MANAGEMENT
                java.util.List<java.util.Map<String, Object>> overrides = profileService.getFieldVisibility(userId);
                for (java.util.Map<String, Object> row : overrides) {
                    String fn = (String) row.get("fieldName");
                    Object tv = row.get("visibilityTier");
                    if (fn != null && tv instanceof Number) fieldTiers.put(fn, ((Number) tv).intValue());
                }

                // Email: decrypt and apply tier-based visibility
                if (dbUser.getEmailEncrypted() != null) {
                    if (isOwner || viewerLevel >= fieldTiers.getOrDefault("email", 2)) {
                        try {
                            String email = profileService.decryptField(dbUser.getEmailEncrypted());
                            if (email != null) {
                                composite.put("email", email);
                            }
                        } catch (Exception ex) { /* skip if decrypt fails */ }
                    }
                }

                // Phone: decrypt and apply tier-based visibility (owner sees full, others see masked or nothing)
                if (dbUser.getPhoneEncrypted() != null) {
                    if (isOwner) {
                        try {
                            String phone = profileService.decryptField(dbUser.getPhoneEncrypted());
                            composite.put("phone", phone);
                        } catch (Exception ex) { /* skip if decrypt fails */ }
                    } else if (viewerLevel >= fieldTiers.getOrDefault("phone", 3)) {
                        try {
                            String phone = profileService.decryptField(dbUser.getPhoneEncrypted());
                            // Mask phone: show last 4 digits only
                            if (phone != null && phone.length() > 4) {
                                composite.put("phone", "***" + phone.substring(phone.length() - 4));
                            }
                        } catch (Exception ex) { /* skip */ }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch user fields for profile: {}", e.getMessage());
        }
        return ResponseEntity.ok(ApiResult.success(composite));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResult<UserProfile>> updateProfile(
            @PathVariable Long userId,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        // Self or ADMIN only
        if (!currentUser.getId().equals(userId) && currentUser.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Access denied: can only update own profile or require ADMIN role"));
        }

        log.info("User '{}' updating profile for user {}", currentUser.getUsername(), userId);
        UserProfile profile = profileService.updateProfile(userId, body, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(profile));
    }

    @PostMapping("/{userId}/avatar")
    public ResponseEntity<ApiResult<MediaAttachment>> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        // Self only
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Access denied: can only upload own avatar"));
        }

        log.info("User '{}' uploading avatar", currentUser.getUsername());
        try {
            byte[] content = file.getBytes();
            String filename = file.getOriginalFilename();
            String mimeType = file.getContentType();
            MediaAttachment attachment = profileService.uploadMedia(
                    userId, content, filename, mimeType, currentUser.getId());
            return ResponseEntity.ok(ApiResult.success(attachment));
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResult.error(500, "Failed to read uploaded file"));
        }
    }

    @PostMapping("/{userId}/media")
    public ResponseEntity<ApiResult<MediaAttachment>> uploadMedia(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        // Self or OPS_MANAGER+
        boolean isSelf = currentUser.getId().equals(userId);
        boolean isPrivileged = currentUser.getRole() == Role.OPS_MANAGER
                || currentUser.getRole() == Role.ADMIN;
        if (!isSelf && !isPrivileged) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Access denied: insufficient permissions to upload media"));
        }

        log.info("User '{}' uploading media for user {}", currentUser.getUsername(), userId);
        try {
            byte[] content = file.getBytes();
            String filename = file.getOriginalFilename();
            String mimeType = file.getContentType();
            MediaAttachment attachment = profileService.uploadMedia(
                    userId, content, filename, mimeType, currentUser.getId());
            return ResponseEntity.ok(ApiResult.success(attachment));
        } catch (IOException e) {
            log.error("Failed to read uploaded file: {}", e.getMessage());
            return ResponseEntity.status(500).body(ApiResult.error(500, "Failed to read uploaded file"));
        }
    }

    @GetMapping("/{userId}/media")
    public ResponseEntity<ApiResult<List<MediaAttachment>>> getMedia(
            @PathVariable Long userId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("User '{}' getting media for user {}", currentUser.getUsername(), userId);

        // Determine visibility level based on viewer's role
        VisibilityLevel viewerVisibility = VisibilityLevel.fromRole(currentUser.getRole());
        int maxVisibility = viewerVisibility.getLevel();
        // Self can see all their own media
        if (currentUser.getId().equals(userId)) {
            maxVisibility = VisibilityLevel.ADMIN.getLevel();
        }

        List<MediaAttachment> media = mediaAttachmentMapper.findByEntity("UserProfile", userId, maxVisibility);
        return ResponseEntity.ok(ApiResult.success(media));
    }

    @PutMapping("/{userId}/visibility")
    public ResponseEntity<ApiResult<Void>> updateVisibility(
            @PathVariable Long userId,
            @Valid @RequestBody com.dispatchops.web.dto.VisibilityUpdateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Access denied: can only update own visibility settings"));
        }

        log.info("User '{}' updating visibility for field '{}' to tier '{}'",
                currentUser.getUsername(), dto.getField(), dto.getTier());
        profileService.updateVisibility(userId, dto.getField(), dto.getTier(), currentUser.getId());
        return ResponseEntity.ok(ApiResult.success());
    }

    @GetMapping("/{userId}/visibility")
    public ResponseEntity<ApiResult<java.util.List<java.util.Map<String, Object>>>> getVisibility(
            @PathVariable Long userId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).body(ApiResult.error(403, "Access denied"));
        }
        java.util.List<java.util.Map<String, Object>> settings = profileService.getFieldVisibility(userId);
        return ResponseEntity.ok(ApiResult.success(settings));
    }
}
