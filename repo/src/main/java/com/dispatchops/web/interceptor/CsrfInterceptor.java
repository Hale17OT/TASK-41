package com.dispatchops.web.interceptor;

import com.dispatchops.web.dto.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;
import java.util.UUID;

/**
 * Server-side CSRF protection for session-authenticated mutating requests.
 * Token is generated on session creation and validated on POST/PUT/DELETE.
 * GET/HEAD/OPTIONS are exempt (safe methods).
 */
@Component
public class CsrfInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(CsrfInterceptor.class);
    public static final String CSRF_TOKEN_SESSION_KEY = "_csrfToken";
    public static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Safe methods don't need CSRF check
        if (SAFE_METHODS.contains(request.getMethod().toUpperCase())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            // No session = no CSRF risk (auth interceptor will catch this)
            return true;
        }

        // Token must exist (generated at login by AuthController)
        String sessionToken = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        if (sessionToken == null) {
            // Session exists but no token = session was not created via login flow
            log.warn("CSRF token not found in session for {} {}", request.getMethod(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResult.error(403, "CSRF token not initialized. Re-login required."));
            return false;
        }

        // Validate token from request header
        String requestToken = request.getHeader(CSRF_TOKEN_HEADER);
        if (requestToken == null || !requestToken.equals(sessionToken)) {
            log.warn("CSRF token mismatch for {} {} from {}", request.getMethod(), request.getRequestURI(), request.getRemoteAddr());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResult.error(403, "CSRF token missing or invalid"));
            return false;
        }

        return true;
    }

    /**
     * Generate a CSRF token for a session (called after login).
     */
    public static String generateToken(HttpSession session) {
        String token = UUID.randomUUID().toString();
        session.setAttribute(CSRF_TOKEN_SESSION_KEY, token);
        return token;
    }

    /**
     * Get the current CSRF token for a session.
     */
    public static String getToken(HttpSession session) {
        if (session == null) return null;
        String token = (String) session.getAttribute(CSRF_TOKEN_SESSION_KEY);
        if (token == null) {
            token = generateToken(session);
        }
        return token;
    }
}
