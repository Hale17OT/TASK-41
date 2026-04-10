package com.dispatchops.web.controller;

import com.dispatchops.application.service.*;
import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.model.*;
import com.dispatchops.domain.model.enums.*;
import com.dispatchops.infrastructure.persistence.mapper.MediaAttachmentMapper;
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

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Exhaustive object-level authorization tests across all sensitive modules:
 * jobs, credibility, notifications, payments, contracts, profiles.
 */
@ExtendWith(MockitoExtension.class)
class ObjectLevelAuthControllerTest {

    @Mock private DeliveryJobService deliveryJobService;
    @Mock private CredibilityService credibilityService;
    @Mock private ProfileService profileService;
    @Mock private MediaAttachmentMapper mediaAttachmentMapper;
    @Mock private NotificationService notificationService;
    @Mock private PaymentService paymentService;
    @Mock private ContractService contractService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DeliveryJobController(deliveryJobService),
                new CredibilityController(credibilityService),
                new NotificationController(notificationService),
                new PaymentController(paymentService),
                new ContractController(contractService),
                new ProfileController(profileService, mediaAttachmentMapper)
        ).addInterceptors(new AuthInterceptor(), new RoleInterceptor())
         .setControllerAdvice(new GlobalExceptionHandler())
         .build();
    }

    private MockHttpSession session(Role role, Long userId) {
        User u = new User();
        u.setId(userId);
        u.setUsername(role.name().toLowerCase() + "-" + userId);
        u.setRole(role);
        u.setMustChangePassword(false);
        MockHttpSession s = new MockHttpSession();
        s.setAttribute("currentUser", u);
        s.setAttribute(CsrfInterceptor.CSRF_TOKEN_SESSION_KEY, "csrf-tok");
        return s;
    }

    private MockHttpSession courierSession(Long userId) { return session(Role.COURIER, userId); }

    // ==================== JOBS ====================

    @Test void courier_cannotViewOtherCouriersJob() throws Exception {
        DeliveryJob job = new DeliveryJob(); job.setId(1L); job.setCourierId(100L); job.setStatus(JobStatus.IN_TRANSIT);
        when(deliveryJobService.getJob(1L)).thenReturn(job);
        mockMvc.perform(get("/api/jobs/1").session(courierSession(200L))).andExpect(status().isForbidden());
    }

    @Test void courier_canViewOwnJob() throws Exception {
        DeliveryJob job = new DeliveryJob(); job.setId(1L); job.setCourierId(100L); job.setStatus(JobStatus.IN_TRANSIT);
        when(deliveryJobService.getJob(1L)).thenReturn(job);
        mockMvc.perform(get("/api/jobs/1").session(courierSession(100L))).andExpect(status().isOk());
    }

    @Test void courier_cannotTransitionOtherCouriersJob() throws Exception {
        DeliveryJob job = new DeliveryJob(); job.setId(5L); job.setCourierId(300L); job.setStatus(JobStatus.PICKED);
        when(deliveryJobService.getJob(5L)).thenReturn(job);
        mockMvc.perform(put("/api/jobs/5/status").session(courierSession(200L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_TRANSIT\",\"comment\":\"\",\"version\":1}"))
                .andExpect(status().isForbidden());
    }

    @Test void courier_canTransitionOwnJob() throws Exception {
        DeliveryJob job = new DeliveryJob(); job.setId(5L); job.setCourierId(200L); job.setStatus(JobStatus.PICKED);
        when(deliveryJobService.getJob(5L)).thenReturn(job);
        when(deliveryJobService.transitionStatus(eq(5L), anyString(), anyString(), anyInt(), eq(200L))).thenReturn(job);
        mockMvc.perform(put("/api/jobs/5/status").session(courierSession(200L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_TRANSIT\",\"comment\":\"\",\"version\":1}"))
                .andExpect(status().isOk());
    }

    @Test void courier_cannotViewOtherCouriersJobEvents() throws Exception {
        DeliveryJob job = new DeliveryJob(); job.setId(3L); job.setCourierId(100L);
        when(deliveryJobService.getJob(3L)).thenReturn(job);
        mockMvc.perform(get("/api/jobs/3/events").session(courierSession(200L))).andExpect(status().isForbidden());
    }

    // ==================== CREDIBILITY ====================

    @Test void courier_cannotViewOtherCreditLevel() throws Exception {
        mockMvc.perform(get("/api/credibility/credit/100").session(courierSession(200L))).andExpect(status().isForbidden());
    }

    @Test void courier_canViewOwnCreditLevel() throws Exception {
        CreditLevelSnapshot snap = new CreditLevelSnapshot(); snap.setCourierId(100L); snap.setLevel(CreditLevel.B);
        when(credibilityService.getCourierCredibility(100L)).thenReturn(snap);
        mockMvc.perform(get("/api/credibility/credit/100").session(courierSession(100L))).andExpect(status().isOk());
    }

    @Test void courier_cannotAppealOtherCouriersRating() throws Exception {
        doThrow(new PermissionDeniedException("Rating does not belong to courier"))
                .when(credibilityService).fileAppeal(eq(99L), isNull(), anyString(), eq(200L));
        mockMvc.perform(post("/api/credibility/appeals").session(courierSession(200L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"ratingId\":99,\"reason\":\"I want to appeal this\"}"))
                .andExpect(status().isForbidden());
    }

    @Test void courier_cannotAppealOtherCouriersViolation() throws Exception {
        doThrow(new PermissionDeniedException("Violation does not belong to courier"))
                .when(credibilityService).fileAppeal(isNull(), eq(88L), anyString(), eq(200L));
        mockMvc.perform(post("/api/credibility/appeals").session(courierSession(200L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"violationId\":88,\"reason\":\"I want to appeal this\"}"))
                .andExpect(status().isForbidden());
    }

    // ==================== NOTIFICATIONS ====================

    @Test void notification_inboxScopedToSelf() throws Exception {
        mockMvc.perform(get("/api/notifications").session(courierSession(100L))).andExpect(status().isOk());
    }

    @Test void notification_cannotMarkOtherUsersAsRead() throws Exception {
        doThrow(new PermissionDeniedException("Not yours")).when(notificationService).markAsRead(eq(100L), eq(999L));
        mockMvc.perform(put("/api/notifications/999/read").session(courierSession(100L))
                .header("X-CSRF-TOKEN", "csrf-tok")).andExpect(status().isForbidden());
    }

    // ==================== PAYMENTS ====================

    @Test void courier_cannotAccessPaymentBalance() throws Exception {
        mockMvc.perform(get("/api/payments/balance/200").session(courierSession(100L))).andExpect(status().isForbidden());
    }

    @Test void courier_cannotAccessPaymentLedger() throws Exception {
        mockMvc.perform(get("/api/payments/ledger/200").session(courierSession(100L))).andExpect(status().isForbidden());
    }

    @Test void auditor_canAccessPaymentBalance() throws Exception {
        mockMvc.perform(get("/api/payments/balance/200").session(session(Role.AUDITOR, 50L))).andExpect(status().isOk());
    }

    @Test void auditor_canAccessPaymentLedger() throws Exception {
        mockMvc.perform(get("/api/payments/ledger/200").session(session(Role.AUDITOR, 50L))).andExpect(status().isOk());
    }

    // ==================== CONTRACTS ====================

    @Test void courier_cannotViewNonSignerContract() throws Exception {
        ContractInstance inst = new ContractInstance();
        inst.setId(10L); inst.setPlaceholderValues("{\"signerIds\":[300]}"); inst.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractService.getInstance(10L)).thenReturn(inst);
        when(contractService.isDesignatedSigner(any(), eq(200L))).thenReturn(false);
        mockMvc.perform(get("/api/contracts/instances/10").session(courierSession(200L))).andExpect(status().isForbidden());
    }

    @Test void courier_canViewSignerContract() throws Exception {
        ContractInstance inst = new ContractInstance();
        inst.setId(10L); inst.setPlaceholderValues("{\"signerIds\":[200]}"); inst.setStatus(ContractStatus.PENDING_SIGNATURE);
        when(contractService.getInstance(10L)).thenReturn(inst);
        when(contractService.isDesignatedSigner(any(), eq(200L))).thenReturn(true);
        mockMvc.perform(get("/api/contracts/instances/10").session(courierSession(200L))).andExpect(status().isOk());
    }

    @Test void courier_cannotAccessContractSignatures() throws Exception {
        // Signatures endpoint is ADMIN/OPS_MANAGER/AUDITOR only
        mockMvc.perform(get("/api/contracts/instances/10/signatures").session(courierSession(200L)))
                .andExpect(status().isForbidden());
    }

    // ==================== PROFILES ====================

    @Test void courier_cannotUpdateOtherProfile() throws Exception {
        mockMvc.perform(put("/api/profiles/999").session(courierSession(100L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bio\":\"hacked\"}")).andExpect(status().isForbidden());
    }

    @Test void courier_canUpdateOwnProfile() throws Exception {
        mockMvc.perform(put("/api/profiles/100").session(courierSession(100L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"bio\":\"my bio\"}")).andExpect(status().isOk());
    }

    @Test void courier_cannotUpdateOtherVisibility() throws Exception {
        mockMvc.perform(put("/api/profiles/999/visibility").session(courierSession(100L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\":\"email\",\"tier\":2}")).andExpect(status().isForbidden());
    }

    @Test void courier_canUpdateOwnVisibility() throws Exception {
        mockMvc.perform(put("/api/profiles/100/visibility").session(courierSession(100L))
                .header("X-CSRF-TOKEN", "csrf-tok").contentType(MediaType.APPLICATION_JSON)
                .content("{\"field\":\"email\",\"tier\":2}")).andExpect(status().isOk());
    }

    @Test void courier_cannotViewOtherVisibilitySettings() throws Exception {
        mockMvc.perform(get("/api/profiles/999/visibility").session(courierSession(100L)))
                .andExpect(status().isForbidden());
    }
}
