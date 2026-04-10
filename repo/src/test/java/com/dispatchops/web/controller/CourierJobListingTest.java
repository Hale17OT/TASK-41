package com.dispatchops.web.controller;

import com.dispatchops.application.service.DeliveryJobService;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.advice.GlobalExceptionHandler;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.interceptor.AuthInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that courier job listing uses query-level filtering (not post-pagination).
 */
@ExtendWith(MockitoExtension.class)
class CourierJobListingTest {

    @Mock private DeliveryJobService deliveryJobService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(
                new DeliveryJobController(deliveryJobService)
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

    private MockHttpSession dispatcherSession() {
        MockHttpSession session = new MockHttpSession();
        User user = new User();
        user.setId(50L);
        user.setUsername("dispatcher1");
        user.setRole(Role.DISPATCHER);
        user.setDisplayName("Dispatcher");
        user.setActive(true);
        session.setAttribute("currentUser", user);
        return session;
    }

    @Test
    void courier_listJobs_usesQueryLevelCourierFilter() throws Exception {
        DeliveryJob job = new DeliveryJob();
        job.setId(1L);
        job.setTrackingNumber("DO-1");
        job.setStatus(JobStatus.IN_TRANSIT);
        job.setCourierId(10L);

        when(deliveryJobService.listJobsByCourierPaged(eq(10L), isNull(), eq(0), eq(25)))
                .thenReturn(new PageResult<>(List.of(job), 0, 25, 1));

        mockMvc.perform(get("/api/jobs").session(courierSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(1)))
                .andExpect(jsonPath("$.data.totalElements", is(1)));

        // Must call courier-scoped query, never global listJobs
        verify(deliveryJobService).listJobsByCourierPaged(10L, null, 0, 25);
        verify(deliveryJobService, never()).listJobs(any(), anyInt(), anyInt());
    }

    @Test
    void courier_listJobs_withStatusFilter() throws Exception {
        when(deliveryJobService.listJobsByCourierPaged(eq(10L), eq("DELIVERED"), eq(0), eq(25)))
                .thenReturn(new PageResult<>(List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/jobs").param("status", "DELIVERED").session(courierSession(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content", hasSize(0)));

        verify(deliveryJobService).listJobsByCourierPaged(10L, "DELIVERED", 0, 25);
    }

    @Test
    void dispatcher_listJobs_usesGlobalQuery() throws Exception {
        when(deliveryJobService.listJobs(isNull(), eq(0), eq(25)))
                .thenReturn(new PageResult<>(List.of(), 0, 25, 0));

        mockMvc.perform(get("/api/jobs").session(dispatcherSession()))
                .andExpect(status().isOk());

        verify(deliveryJobService).listJobs(null, 0, 25);
        verify(deliveryJobService, never()).listJobsByCourierPaged(anyLong(), any(), anyInt(), anyInt());
    }
}
