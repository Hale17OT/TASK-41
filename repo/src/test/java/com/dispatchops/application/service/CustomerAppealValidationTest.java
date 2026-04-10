package com.dispatchops.application.service;

import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.CredibilityRating;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.infrastructure.persistence.mapper.*;
import com.dispatchops.infrastructure.security.HmacUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests customer appeal cross-object validation:
 * ensures ratingId must belong to the resolved job.
 */
@ExtendWith(MockitoExtension.class)
class CustomerAppealValidationTest {

    @Mock private CredibilityRatingMapper ratingMapper;
    @Mock private CreditLevelMapper creditLevelMapper;
    @Mock private ViolationMapper violationMapper;
    @Mock private AppealMapper appealMapper;
    @Mock private DeliveryJobMapper deliveryJobMapper;
    @Mock private NotificationService notificationService;
    @Mock private UserMapper userMapper;

    private static final String TEST_HMAC_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private CredibilityService service;

    @BeforeEach
    void setUp() {
        service = new CredibilityService(ratingMapper, creditLevelMapper, violationMapper,
                appealMapper, deliveryJobMapper, notificationService, userMapper, TEST_HMAC_KEY);
    }

    /** Compute a valid customer token for the given job fields. */
    private String validToken(String trackingNumber, String receiverName, Long jobId) {
        return HmacUtil.computeHmac(trackingNumber + "|" + receiverName + "|" + jobId, TEST_HMAC_KEY)
                .substring(0, 32);
    }

    @Test
    void customerAppeal_ratingBelongsToJob_succeeds() {
        String token = validToken("TRK-1", "John", 10L);

        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setCourierId(5L);
        job.setReceiverName("John");
        job.setCustomerToken(token);
        job.setStatus(JobStatus.DELIVERED);
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);

        CredibilityRating rating = new CredibilityRating();
        rating.setId(20L);
        rating.setJobId(10L); // matches job
        rating.setCourierId(5L);
        rating.setCreatedAt(java.time.LocalDateTime.now().minusHours(1));
        when(ratingMapper.findById(20L)).thenReturn(rating);
        when(appealMapper.insert(any())).thenReturn(1);

        assertDoesNotThrow(() -> service.fileCustomerAppeal("TRK-1", "John", token, 20L, "Appeal reason"));
    }

    @Test
    void customerAppeal_ratingDoesNotBelongToJob_rejected() {
        String token = validToken("TRK-1", "John", 10L);

        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setCourierId(5L);
        job.setReceiverName("John");
        job.setCustomerToken(token);
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);

        CredibilityRating rating = new CredibilityRating();
        rating.setId(20L);
        rating.setJobId(999L); // different job!
        rating.setCourierId(5L);
        when(ratingMapper.findById(20L)).thenReturn(rating);

        assertThrows(PermissionDeniedException.class,
                () -> service.fileCustomerAppeal("TRK-1", "John", token, 20L, "Appeal reason"));
    }

    @Test
    void customerAppeal_ratingNotFound_rejected() {
        String token = validToken("TRK-1", "John", 10L);

        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setReceiverName("John");
        job.setCustomerToken(token);
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);
        when(ratingMapper.findById(999L)).thenReturn(null);

        assertThrows(ResourceNotFoundException.class,
                () -> service.fileCustomerAppeal("TRK-1", "John", token, 999L, "Appeal"));
    }

    @Test
    void customerAppeal_wrongReceiverName_rejected() {
        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setReceiverName("John");
        job.setCustomerToken("sometoken");
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);

        assertThrows(PermissionDeniedException.class,
                () -> service.fileCustomerAppeal("TRK-1", "WrongName", "badtoken", 20L, "Appeal"));
    }

    @Test
    void customerAppeal_invalidToken_rejected() {
        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setReceiverName("John");
        job.setCustomerToken("validtoken");
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);

        assertThrows(PermissionDeniedException.class,
                () -> service.fileCustomerAppeal("TRK-1", "John", "wrong-token-value-here!!", 20L, "Appeal"));
    }

    @Test
    void customerAppeal_nullTokenOnJob_rejected() {
        DeliveryJob job = new DeliveryJob();
        job.setId(10L);
        job.setTrackingNumber("TRK-1");
        job.setReceiverName("John");
        job.setCustomerToken(null); // no token generated yet
        when(deliveryJobMapper.findByTrackingNumber("TRK-1")).thenReturn(job);

        assertThrows(PermissionDeniedException.class,
                () -> service.fileCustomerAppeal("TRK-1", "John", "anytoken", 20L, "Appeal"));
    }
}
