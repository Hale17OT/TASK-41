package com.dispatchops.application.scheduler;

import com.dispatchops.application.service.CredibilityService;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests CreditScoringJob scheduler behavior boundaries:
 * - processes all couriers
 * - continues on individual failure
 * - handles empty courier list
 */
@ExtendWith(MockitoExtension.class)
class CreditScoringJobTest {

    @Mock private UserMapper userMapper;
    @Mock private CredibilityService credibilityService;

    private CreditScoringJob job;

    @BeforeEach
    void setUp() {
        job = new CreditScoringJob(userMapper, credibilityService);
    }

    private User courier(Long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.COURIER);
        return u;
    }

    @Test
    void recalculatesAllCouriers() {
        when(userMapper.findByRole("COURIER")).thenReturn(List.of(courier(1L), courier(2L), courier(3L)));

        job.recalculateAllCreditLevels();

        verify(credibilityService).recalculateCreditLevel(1L);
        verify(credibilityService).recalculateCreditLevel(2L);
        verify(credibilityService).recalculateCreditLevel(3L);
    }

    @Test
    void continuesOnIndividualFailure() {
        when(userMapper.findByRole("COURIER")).thenReturn(List.of(courier(1L), courier(2L), courier(3L)));
        doThrow(new RuntimeException("DB error")).when(credibilityService).recalculateCreditLevel(2L);

        // Should NOT throw — catches per-courier exceptions and continues
        job.recalculateAllCreditLevels();

        // Courier 1 and 3 still get processed
        verify(credibilityService).recalculateCreditLevel(1L);
        verify(credibilityService).recalculateCreditLevel(3L);
    }

    @Test
    void handlesEmptyCourierList() {
        when(userMapper.findByRole("COURIER")).thenReturn(List.of());

        job.recalculateAllCreditLevels();

        verify(credibilityService, never()).recalculateCreditLevel(anyLong());
    }
}
