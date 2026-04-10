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
 * Comprehensive role authorization matrix covering all core controllers.
 * Tests 401 (no session), 403 (wrong role), 200 (correct role) for each module.
 */
@ExtendWith(MockitoExtension.class)
class FullRoleMatrixTest {

    @Mock private DeliveryJobService deliveryJobService;
    @Mock private TaskService taskService;
    @Mock private PaymentService paymentService;
    @Mock private CredibilityService credibilityService;
    @Mock private ContractService contractService;
    @Mock private SearchService searchService;
    @Mock private ProfileService profileService;
    @Mock private com.dispatchops.infrastructure.persistence.mapper.MediaAttachmentMapper mediaAttachmentMapper;
    @Mock private UserService userService;
    @Mock private ShippingRuleService shippingRuleService;
    @Mock private NotificationService notificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DeliveryJobController(deliveryJobService),
                new UserController(userService),
                new TaskController(taskService),
                new PaymentController(paymentService),
                new CredibilityController(credibilityService),
                new ContractController(contractService),
                new SearchController(searchService),
                new ProfileController(profileService, mediaAttachmentMapper),
                new ShippingRuleController(shippingRuleService),
                new NotificationController(notificationService)
        ).addInterceptors(new AuthInterceptor(), new RoleInterceptor())
         .setControllerAdvice(new GlobalExceptionHandler())
         .build();
    }

    private MockHttpSession session(Role role) {
        User u = new User();
        u.setId(99L);
        u.setUsername("test-" + role.name().toLowerCase());
        u.setRole(role);
        u.setMustChangePassword(false);
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("currentUser", u);
        s.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, "test-csrf");
        return s;
    }

    // ===== 401: No session =====

    @Test void jobs_noSession_401() throws Exception { mockMvc.perform(get("/api/jobs")).andExpect(status().isUnauthorized()); }
    @Test void tasks_noSession_401() throws Exception { mockMvc.perform(get("/api/tasks")).andExpect(status().isUnauthorized()); }
    @Test void payments_noSession_401() throws Exception { mockMvc.perform(get("/api/payments/pending")).andExpect(status().isUnauthorized()); }
    @Test void contracts_noSession_401() throws Exception { mockMvc.perform(get("/api/contracts/templates")).andExpect(status().isUnauthorized()); }
    @Test void users_noSession_401() throws Exception { mockMvc.perform(get("/api/users")).andExpect(status().isUnauthorized()); }
    @Test void credibility_noSession_401() throws Exception { mockMvc.perform(get("/api/credibility/appeals")).andExpect(status().isUnauthorized()); }
    @Test void shipping_noSession_401() throws Exception { mockMvc.perform(get("/api/shipping/templates")).andExpect(status().isUnauthorized()); }
    @Test void notifications_noSession_401() throws Exception { mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized()); }
    @Test void search_noSession_401() throws Exception { mockMvc.perform(get("/api/search?q=test")).andExpect(status().isUnauthorized()); }
    @Test void profiles_noSession_401() throws Exception { mockMvc.perform(get("/api/profiles/1")).andExpect(status().isUnauthorized()); }

    // ===== 403: COURIER restricted =====

    @Test void courier_createJob_403() throws Exception {
        mockMvc.perform(post("/api/jobs").session(session(Role.COURIER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void courier_users_403() throws Exception {
        mockMvc.perform(get("/api/users").session(session(Role.COURIER))).andExpect(status().isForbidden());
    }
    @Test void courier_submitRating_403() throws Exception {
        mockMvc.perform(post("/api/credibility/ratings").session(session(Role.COURIER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void courier_pendingPayments_403() throws Exception {
        mockMvc.perform(get("/api/payments/pending").session(session(Role.COURIER))).andExpect(status().isForbidden());
    }
    @Test void courier_createTemplate_403() throws Exception {
        mockMvc.perform(post("/api/contracts/templates").session(session(Role.COURIER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void courier_shippingTemplates_403() throws Exception {
        mockMvc.perform(get("/api/shipping/templates").session(session(Role.COURIER))).andExpect(status().isForbidden());
    }

    // ===== 403: DISPATCHER restricted =====

    @Test void dispatcher_settlePayment_403() throws Exception {
        mockMvc.perform(post("/api/payments/1/settle").session(session(Role.DISPATCHER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void dispatcher_createUser_403() throws Exception {
        mockMvc.perform(post("/api/users").session(session(Role.DISPATCHER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }
    @Test void dispatcher_resolveAppeal_403() throws Exception {
        mockMvc.perform(put("/api/credibility/appeals/1/resolve").session(session(Role.DISPATCHER))
                .header("X-CSRF-TOKEN", "test-csrf").contentType(MediaType.APPLICATION_JSON)
                .content("{}")).andExpect(status().isForbidden());
    }

    // ===== 200: Correct roles =====

    @Test void admin_users_200() throws Exception {
        mockMvc.perform(get("/api/users").session(session(Role.ADMIN))).andExpect(status().isOk());
    }
    @Test void opsManager_users_200() throws Exception {
        mockMvc.perform(get("/api/users").session(session(Role.OPS_MANAGER))).andExpect(status().isOk());
    }
    @Test void admin_shippingTemplates_200() throws Exception {
        mockMvc.perform(get("/api/shipping/templates").session(session(Role.ADMIN))).andExpect(status().isOk());
    }
    @Test void auditor_pendingPayments_200() throws Exception {
        mockMvc.perform(get("/api/payments/pending").session(session(Role.AUDITOR))).andExpect(status().isOk());
    }
    @Test void admin_appeals_200() throws Exception {
        mockMvc.perform(get("/api/credibility/appeals").session(session(Role.ADMIN))).andExpect(status().isOk());
    }
    @Test void dispatcher_tasks_200() throws Exception {
        mockMvc.perform(get("/api/tasks").session(session(Role.DISPATCHER))).andExpect(status().isOk());
    }
    @Test void admin_contractTemplates_200() throws Exception {
        mockMvc.perform(get("/api/contracts/templates").session(session(Role.ADMIN))).andExpect(status().isOk());
    }
}
