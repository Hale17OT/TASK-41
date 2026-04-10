package com.dispatchops.web.controller;

import com.dispatchops.application.service.CredibilityService;
import com.dispatchops.application.service.DeliveryJobService;
import com.dispatchops.application.service.PaymentService;
import com.dispatchops.domain.model.CreditLevelSnapshot;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.CreditLevel;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.advice.GlobalExceptionHandler;
import com.dispatchops.web.interceptor.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests that dashboard activity feed enforces object-level authorization:
 * couriers only see their own assigned jobs.
 */
@ExtendWith(MockitoExtension.class)
class DashboardAuthorizationTest {

    @Mock private DeliveryJobService deliveryJobService;
    @Mock private PaymentService paymentService;
    @Mock private CredibilityService credibilityService;
    @Mock private com.dispatchops.infrastructure.persistence.mapper.DeliveryJobMapper deliveryJobMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DashboardController(deliveryJobService, paymentService, credibilityService, deliveryJobMapper)
        ).addInterceptors(new AuthInterceptor())
         .setControllerAdvice(new GlobalExceptionHandler())
         .build();
    }

    private MockHttpSession courierSession(Long id) {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(id);
        user.setUsername("courier" + id);
        user.setRole(Role.COURIER);
        user.setDisplayName("Courier " + id);
        user.setActive(true);
        session.setAttribute("currentUser", user);
        return session;
    }

    private MockHttpSession adminSession() {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(99L);
        user.setUsername("admin");
        user.setRole(Role.ADMIN);
        user.setDisplayName("Admin");
        user.setActive(true);
        session.setAttribute("currentUser", user);
        return session;
    }

    @Test
    void courier_activityFeed_usesListJobsByCourier() throws Exception {
        // Setup: courier 10 has one assigned job
        DeliveryJob assignedJob = new DeliveryJob();
        assignedJob.setId(1L);
        assignedJob.setTrackingNumber("DO-123");
        assignedJob.setStatus(JobStatus.IN_TRANSIT);
        assignedJob.setCourierId(10L);
        assignedJob.setLastEventAt(LocalDateTime.now());

        when(deliveryJobService.listJobsByCourier(eq(10L), eq(0), eq(20)))
                .thenReturn(List.of(assignedJob));

        // Must set up credibility for metrics endpoint side-effects
        CreditLevelSnapshot snapshot = new CreditLevelSnapshot();
        snapshot.setLevel(CreditLevel.B);

        mockMvc.perform(get("/api/dashboard/activity").session(courierSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].message", containsString("DO-123")));

        // Verify the courier-scoped method was called, NOT the global listJobs
        verify(deliveryJobService).listJobsByCourier(10L, 0, 20);
        verify(deliveryJobService, never()).listJobs(any(), anyInt(), anyInt());
    }

    @Test
    void courier_activityFeed_doesNotSeeOtherCouriersJobs() throws Exception {
        // Courier 20 has no assigned jobs
        when(deliveryJobService.listJobsByCourier(eq(20L), eq(0), eq(20)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/activity").session(courierSession(20L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)));
    }

    @Test
    void admin_activityFeed_seesAllJobs() throws Exception {
        DeliveryJob job1 = new DeliveryJob();
        job1.setTrackingNumber("DO-1");
        job1.setStatus(JobStatus.CREATED);
        job1.setLastEventAt(LocalDateTime.now());

        DeliveryJob job2 = new DeliveryJob();
        job2.setTrackingNumber("DO-2");
        job2.setStatus(JobStatus.DELIVERED);
        job2.setLastEventAt(LocalDateTime.now());

        when(deliveryJobService.listJobs(isNull(), eq(0), eq(20)))
                .thenReturn(new com.dispatchops.web.dto.PageResult<>(List.of(job1, job2), 0, 20, 2));

        mockMvc.perform(get("/api/dashboard/activity").session(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)));

        // Admin uses global listJobs, not courier-scoped
        verify(deliveryJobService, never()).listJobsByCourier(anyLong(), anyInt(), anyInt());
    }

    @Test
    void unauthenticated_activityFeed_returns401() throws Exception {
        mockMvc.perform(get("/api/dashboard/activity"))
                .andExpect(status().isUnauthorized());
    }
}
