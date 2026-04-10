package com.dispatchops.web.controller;

import com.dispatchops.application.service.UserService;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.UserCreateDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<PageResult<User>>> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) String role) {
        log.debug("Listing users - page: {}, size: {}, role: {}", page, size, role);
        PageResult<User> result = userService.listUsers(page, size, role);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/{id}")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<User>> getUser(@PathVariable Long id) {
        log.debug("Getting user with id: {}", id);
        User user = userService.findById(id);
        scrubSensitiveFields(user);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    @PostMapping
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<User>> createUser(@RequestBody @Valid UserCreateDTO dto) {
        log.info("Creating user with username: {}", dto.getUsername());
        User user = userService.createUser(dto);
        scrubSensitiveFields(user);
        return ResponseEntity.status(201).body(ApiResult.success(user));
    }

    @PutMapping("/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<User>> updateUser(@PathVariable Long id,
                                                       @RequestBody @Valid UserCreateDTO dto) {
        log.info("Updating user with id: {}", id);
        User user = userService.updateUser(id, dto);
        scrubSensitiveFields(user);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    @PutMapping("/{id}/deactivate")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<Void>> deactivateUser(@PathVariable Long id) {
        log.info("Deactivating user with id: {}", id);
        userService.deactivateUser(id);
        return ResponseEntity.ok(ApiResult.success());
    }

    @PutMapping("/{id}/unlock")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<Void>> unlockUser(@PathVariable Long id) {
        log.info("Unlocking user with id: {}", id);
        userService.unlockUser(id);
        return ResponseEntity.ok(ApiResult.success());
    }

    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResult<Void>> changePassword(@PathVariable Long id,
                                                           @RequestBody Map<String, String> body,
                                                           HttpServletRequest request) {
        String newPassword = body.get("newPassword");
        if (newPassword == null || newPassword.isBlank()) {
            log.warn("Change password request with empty password for user id: {}", id);
            return ResponseEntity.badRequest().body(ApiResult.error(400, "New password is required"));
        }

        // Allow self-service: check if session user id matches path id
        HttpSession session = request.getSession(false);
        if (session != null) {
            User currentUser = (User) session.getAttribute("currentUser");
            if (currentUser != null) {
                boolean isSelf = currentUser.getId() != null && currentUser.getId().equals(id);
                boolean isAdmin = currentUser.getRole() == Role.ADMIN;
                if (!isSelf && !isAdmin) {
                    log.warn("User {} attempted to change password for user {} without admin role",
                            currentUser.getId(), id);
                    return ResponseEntity.status(403).body(ApiResult.error(403, "Access denied"));
                }
            }
        }

        log.info("Changing password for user with id: {}", id);
        userService.changePassword(id, newPassword);
        return ResponseEntity.ok(ApiResult.success());
    }

    private void scrubSensitiveFields(User user) {
        if (user != null) {
            user.setPasswordHash(null);
            user.setEmailEncrypted(null);
            user.setPhoneEncrypted(null);
        }
    }
}
