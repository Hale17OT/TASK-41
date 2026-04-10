package com.dispatchops.application.service;

import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.model.CredibilityRating;
import com.dispatchops.domain.model.Violation;
import com.dispatchops.infrastructure.persistence.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourierAppealOwnershipTest {

    @Mock private CredibilityRatingMapper ratingMapper;
    @Mock private CreditLevelMapper creditLevelMapper;
    @Mock private ViolationMapper violationMapper;
    @Mock private AppealMapper appealMapper;
    @Mock private DeliveryJobMapper deliveryJobMapper;
    @Mock private NotificationService notificationService;
    @Mock private UserMapper userMapper;

    private CredibilityService service;

    private static final String TEST_HMAC_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() {
        service = new CredibilityService(ratingMapper, creditLevelMapper, violationMapper,
                appealMapper, deliveryJobMapper, notificationService, userMapper, TEST_HMAC_KEY);
    }

    @Test
    void courier_canAppealOwnRating() {
        CredibilityRating rating = new CredibilityRating();
        rating.setId(1L);
        rating.setCourierId(10L);
        rating.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(ratingMapper.findById(1L)).thenReturn(rating);
        when(appealMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.fileAppeal(1L, null, "Valid appeal", 10L));
    }

    @Test
    void courier_cannotAppealOtherCouriersRating() {
        CredibilityRating rating = new CredibilityRating();
        rating.setId(1L);
        rating.setCourierId(10L); // belongs to courier 10
        rating.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(ratingMapper.findById(1L)).thenReturn(rating);

        // Courier 20 tries to appeal courier 10's rating
        assertThrows(PermissionDeniedException.class,
                () -> service.fileAppeal(1L, null, "Hijack attempt", 20L));
    }

    @Test
    void courier_canAppealOwnViolation() {
        Violation violation = new Violation();
        violation.setId(5L);
        violation.setCourierId(10L);
        violation.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(violationMapper.findById(5L)).thenReturn(violation);
        when(appealMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.fileAppeal(null, 5L, "Valid appeal", 10L));
    }

    @Test
    void courier_cannotAppealOtherCouriersViolation() {
        Violation violation = new Violation();
        violation.setId(5L);
        violation.setCourierId(10L); // belongs to courier 10
        violation.setCreatedAt(LocalDateTime.now().minusHours(1));
        when(violationMapper.findById(5L)).thenReturn(violation);

        // Courier 20 tries to appeal courier 10's violation
        assertThrows(PermissionDeniedException.class,
                () -> service.fileAppeal(null, 5L, "Hijack attempt", 20L));
    }
}
