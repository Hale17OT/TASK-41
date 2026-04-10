package com.dispatchops.web.controller;

import com.dispatchops.application.service.UserService;
import com.dispatchops.domain.exception.AccountLockedException;
import com.dispatchops.domain.exception.AuthenticationException;
import com.dispatchops.domain.model.User;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResult<?>> login(@RequestBody @Valid LoginRequest loginRequest,
                                                  HttpServletRequest request) {
        log.info("Login attempt for user: {}", loginRequest.getUsername());

        try {
            User user = userService.authenticate(loginRequest.getUsername(), loginRequest.getPassword());

            // Scrub sensitive fields before storing in session and returning
            user.setPasswordHash(null);
            user.setEmailEncrypted(null);
            user.setPhoneEncrypted(null);

            // Invalidate any pre-auth session to prevent session fixation
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", user);

            // Generate CSRF token for the session
            String csrfToken = com.dispatchops.web.interceptor.CsrfInterceptor.generateToken(session);

            java.util.Map<String, Object> responseData = new java.util.LinkedHashMap<>();
            responseData.put("user", user);
            responseData.put("csrfToken", csrfToken);
            responseData.put("mustChangePassword", user.isMustChangePassword());

            if (user.isMustChangePassword()) {
                log.warn("User '{}' has must_change_password flag set - password change required", user.getUsername());
            }

            log.info("User '{}' logged in successfully", user.getUsername());
            return ResponseEntity.ok(ApiResult.success(responseData));

        } catch (AccountLockedException ex) {
            log.warn("Login blocked - account locked: {}", loginRequest.getUsername());
            java.util.Map<String, Object> lockoutData = new java.util.LinkedHashMap<>();
            lockoutData.put("lockoutExpiry", java.time.LocalDateTime.now().plusSeconds(ex.getRemainingSeconds()).toString());
            lockoutData.put("remainingSeconds", ex.getRemainingSeconds());
            ApiResult<java.util.Map<String, Object>> result = ApiResult.success(lockoutData);
            result.setCode(423);
            result.setMessage(ex.getMessage());
            return ResponseEntity.status(423).body(result);
        } catch (AuthenticationException ex) {
            log.warn("Login failed for user: {}", loginRequest.getUsername());
            if (ex.hasRemainingAttempts()) {
                java.util.Map<String, Object> authData = new java.util.LinkedHashMap<>();
                authData.put("remainingAttempts", ex.getRemainingAttempts());
                ApiResult<java.util.Map<String, Object>> result = ApiResult.success(authData);
                result.setCode(401);
                result.setMessage(ex.getMessage());
                return ResponseEntity.status(401).body(result);
            }
            return ResponseEntity.status(401).body(ApiResult.error(401, ex.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResult<Void>> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            String username = "unknown";
            User user = (User) session.getAttribute("currentUser");
            if (user != null) {
                username = user.getUsername();
            }
            session.invalidate();
            log.info("User '{}' logged out", username);
        }
        return ResponseEntity.ok(ApiResult.success());
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResult<User>> me(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            log.debug("No session found for /me request");
            return ResponseEntity.status(401).body(ApiResult.error(401, "Authentication required"));
        }

        User user = (User) session.getAttribute("currentUser");
        if (user == null) {
            log.debug("No user in session for /me request");
            return ResponseEntity.status(401).body(ApiResult.error(401, "Authentication required"));
        }

        log.debug("Returning current user: {}", user.getUsername());
        // Scrub sensitive/encrypted fields before returning
        user.setPasswordHash(null);
        user.setEmailEncrypted(null);
        user.setPhoneEncrypted(null);
        return ResponseEntity.ok(ApiResult.success(user));
    }

    @GetMapping("/heartbeat")
    public ResponseEntity<ApiResult<?>> heartbeat(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        java.util.Map<String, String> data = new java.util.LinkedHashMap<>();
        data.put("status", "ok");
        if (session != null) {
            data.put("csrfToken", com.dispatchops.web.interceptor.CsrfInterceptor.getToken(session));
        }
        return ResponseEntity.ok(ApiResult.success(data));
    }
}
