package com.dispatchops.web.controller;

import com.dispatchops.application.service.*;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.advice.GlobalExceptionHandler;
import com.dispatchops.web.interceptor.AuthInterceptor;
import com.dispatchops.web.interceptor.CsrfInterceptor;
import com.dispatchops.web.interceptor.RoleInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests the role authorization matrix across core endpoints.
 * Verifies 401 (no session), 403 (wrong role), and 200 (correct role) for each module.
 */
@ExtendWith(MockitoExtension.class)
class RoleAuthorizationMatrixTest {

    private MockMvc mockMvc;

    @Mock private DeliveryJobService deliveryJobService;
    @Mock private TaskService taskService;
    @Mock private PaymentService paymentService;
    @Mock private CredibilityService credibilityService;
    @Mock private ContractService contractService;
    @Mock private SearchService searchService;
    @Mock private ProfileService profileService;
    @Mock private UserService userService;

    @BeforeEach
    void setUp() {
        var jobCtrl = new DeliveryJobController(deliveryJobService);
        var userCtrl = new UserController(userService);

        mockMvc = MockMvcBuilders.standaloneSetup(jobCtrl, userCtrl)
                .addInterceptors(new AuthInterceptor(), new RoleInterceptor())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private MockHttpSession sessionFor(Role role) {
        User u = new User();
        u.setId(99L);
        u.setUsername("test-" + role.name().toLowerCase());
        u.setRole(role);
        u.setMustChangePassword(false);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", u);
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, token);
        return session;
    }

    // --- 401: No session ---

    @Test
    void noSessionReturns401OnJobs() throws Exception {
        mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized());
    }

    @Test
    void noSessionReturns401OnUsers() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
    }

    // --- 403: Wrong role ---

    @Test
    void courierCannotAccessUserList() throws Exception {
        mockMvc.perform(get("/api/users").session(sessionFor(Role.COURIER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void courierCannotCreateJobs() throws Exception {
        MockHttpSession session = sessionFor(Role.COURIER);
        String token = (String) session.getAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY);
        mockMvc.perform(post("/api/jobs")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senderName\":\"X\",\"senderAddress\":\"X\",\"receiverName\":\"X\",\"receiverAddress\":\"X\",\"receiverState\":\"CA\",\"receiverZip\":\"90210\",\"weightLbs\":1,\"orderAmount\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void auditorCannotCreateUsers() throws Exception {
        MockHttpSession session = sessionFor(Role.AUDITOR);
        String token = (String) session.getAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY);
        mockMvc.perform(post("/api/users")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"x\",\"password\":\"12345678\",\"role\":\"COURIER\",\"displayName\":\"X\"}"))
                .andExpect(status().isForbidden());
    }

    // --- 200: Correct role ---

    @Test
    void adminCanAccessUserList() throws Exception {
        mockMvc.perform(get("/api/users").session(sessionFor(Role.ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    void opsManagerCanAccessUserList() throws Exception {
        mockMvc.perform(get("/api/users").session(sessionFor(Role.OPS_MANAGER)))
                .andExpect(status().isOk());
    }

    // --- must_change_password blocks access ---

    @Test
    void mustChangePasswordBlocksJobAccess() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setUsername("forced");
        u.setRole(Role.ADMIN);
        u.setMustChangePassword(true);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", u);

        mockMvc.perform(get("/api/jobs").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void mustChangePasswordAllowsPasswordEndpoint() throws Exception {
        User u = new User();
        u.setId(1L);
        u.setUsername("forced");
        u.setRole(Role.ADMIN);
        u.setMustChangePassword(true);
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", u);
        String token = java.util.UUID.randomUUID().toString();
        session.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, token);

        mockMvc.perform(put("/api/users/1/password")
                        .session(session)
                        .header("X-CSRF-TOKEN", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewSecure123!\"}"))
                .andExpect(status().isOk());
    }
}
