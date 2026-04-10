package com.dispatchops.web.interceptor;

import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AuthInterceptor including must_change_password enforcement.
 */
class AuthInterceptorTest {

    private final AuthInterceptor interceptor = new AuthInterceptor();

    private User createUser(boolean mustChange) {
        User u = new User();
        u.setId(1L);
        u.setUsername("testuser");
        u.setRole(Role.DISPATCHER);
        u.setMustChangePassword(mustChange);
        return u;
    }

    // --- No session = 401 ---

    @Test
    void noSessionReturns401() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(401, resp.getStatus());
    }

    // --- Login endpoint passes without session ---

    @Test
    void loginEndpointAlwaysPasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    // --- Normal user passes ---

    @Test
    void authenticatedUserPasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.getSession(true).setAttribute("currentUser", createUser(false));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    // --- must_change_password blocks non-password endpoints ---

    @Test
    void mustChangePasswordBlocksGeneralEndpoints() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    @Test
    void mustChangePasswordAllowsPasswordChange() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/api/users/1/password");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void mustChangePasswordAllowsLogout() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/logout");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void mustChangePasswordAllowsMe() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/me");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void mustChangePasswordAllowsHeartbeat() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/auth/heartbeat");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    // --- Additional forced-password-change coverage ---

    @Test
    void mustChangePasswordBlocksDashboardApi() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/dashboard/metrics");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
        assertTrue(resp.getContentAsString().contains("Password change required"));
    }

    @Test
    void mustChangePasswordBlocksSearchApi() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/search");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    @Test
    void mustChangePasswordBlocksCredibilityApi() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/credibility/ratings");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    @Test
    void mustChangePasswordAllowsOtherUserPasswordChange() throws Exception {
        // Must allow /api/users/{anyId}/password — the path pattern is /api/users/\d+/password
        MockHttpServletRequest req = new MockHttpServletRequest("PUT", "/api/users/99/password");
        req.getSession(true).setAttribute("currentUser", createUser(true));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void normalUserNotBlockedByPasswordChange() throws Exception {
        // User without mustChangePassword can access everything
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/dashboard/metrics");
        req.getSession(true).setAttribute("currentUser", createUser(false));
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }
}
