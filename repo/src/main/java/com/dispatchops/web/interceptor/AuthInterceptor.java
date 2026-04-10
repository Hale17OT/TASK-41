package com.dispatchops.web.interceptor;

import com.dispatchops.domain.model.User;
import com.dispatchops.web.dto.ApiResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Endpoints allowed even when must_change_password is set */
    private static final Set<String> PASSWORD_CHANGE_ALLOWED = Set.of(
            "/api/auth/logout", "/api/auth/me", "/api/auth/heartbeat"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (path.startsWith("/api/auth/login")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("currentUser") == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getWriter(), ApiResult.error(401, "Authentication required"));
            return false;
        }

        // Enforce must_change_password: block all endpoints except password change and auth helpers
        User currentUser = (User) session.getAttribute("currentUser");
        if (currentUser.isMustChangePassword()) {
            boolean isPasswordChange = path.matches("/api/users/\\d+/password");
            boolean isAllowed = PASSWORD_CHANGE_ALLOWED.contains(path) || isPasswordChange;
            if (!isAllowed) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                objectMapper.writeValue(response.getWriter(),
                        ApiResult.error(403, "Password change required before accessing other resources"));
                return false;
            }
        }

        return true;
    }
}
