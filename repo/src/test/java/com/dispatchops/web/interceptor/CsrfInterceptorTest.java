package com.dispatchops.web.interceptor;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CsrfInterceptor including preHandle request path behavior.
 */
class CsrfInterceptorTest {

    private final CsrfInterceptor interceptor = new CsrfInterceptor();

    // --- Token lifecycle ---

    @Test
    void tokenGenerationProducesUUID() {
        var session = new MockHttpServletRequest().getSession(true);
        String token = CsrfInterceptor.generateToken(session);
        assertNotNull(token);
        assertEquals(36, token.length());
    }

    @Test
    void getTokenCreatesIfAbsent() {
        var session = new MockHttpServletRequest().getSession(true);
        String token = CsrfInterceptor.getToken(session);
        assertNotNull(token);
        assertEquals(token, session.getAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY));
    }

    @Test
    void getTokenReturnsNullForNullSession() {
        assertNull(CsrfInterceptor.getToken(null));
    }

    // --- preHandle: safe methods always pass ---

    @Test
    void getRequestAlwaysAllowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/jobs");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    @Test
    void headRequestAlwaysAllowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("HEAD", "/api/jobs");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    // --- preHandle: no session = pass (auth interceptor handles) ---

    @Test
    void postWithoutSessionPasses() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/jobs");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }

    // --- preHandle: session without token = rejected (must login first) ---

    @Test
    void postWithSessionButNoTokenRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/jobs");
        req.getSession(true); // session exists but no CSRF token (not logged in properly)
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    // --- preHandle: subsequent POST without header is rejected ---

    @Test
    void postWithoutCsrfHeaderRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/payments");
        var session = req.getSession(true);
        session.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, "valid-token-123");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    // --- preHandle: wrong token rejected ---

    @Test
    void postWithWrongCsrfTokenRejected() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/payments");
        var session = req.getSession(true);
        session.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, "correct-token");
        req.addHeader("X-CSRF-TOKEN", "wrong-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(req, resp, new Object()));
        assertEquals(403, resp.getStatus());
    }

    // --- preHandle: correct token accepted ---

    @Test
    void postWithCorrectCsrfTokenAllowed() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/payments");
        var session = req.getSession(true);
        session.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, "my-csrf-token");
        req.addHeader("X-CSRF-TOKEN", "my-csrf-token");
        MockHttpServletResponse resp = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(req, resp, new Object()));
    }
}
