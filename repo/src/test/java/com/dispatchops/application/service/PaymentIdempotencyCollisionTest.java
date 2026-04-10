package com.dispatchops.application.service;

import com.dispatchops.domain.model.Payment;
import com.dispatchops.domain.model.enums.PaymentMethod;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.infrastructure.persistence.mapper.*;
import com.dispatchops.web.dto.PaymentCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests concurrent idempotency key collision handling in PaymentService.
 * Verifies that DuplicateKeyException on insert is caught and returns existing payment.
 */
@ExtendWith(MockitoExtension.class)
class PaymentIdempotencyCollisionTest {

    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    @Mock private PaymentMapper paymentMapper;
    @Mock private LedgerEntryMapper ledgerEntryMapper;
    @Mock private ReconciliationItemMapper reconciliationItemMapper;
    @Mock private DeviceCredentialMapper deviceCredentialMapper;
    @Mock private CallbackEventMapper callbackEventMapper;
    @Mock private NotificationService notificationService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = org.mockito.Mockito.mock(UserMapper.class);
        com.dispatchops.domain.model.User systemUser = new com.dispatchops.domain.model.User();
        systemUser.setId(1L);
        systemUser.setUsername("system");
        when(userMapper.findByUsername("system")).thenReturn(systemUser);

        paymentService = new PaymentService(paymentMapper, ledgerEntryMapper,
                reconciliationItemMapper, deviceCredentialMapper, callbackEventMapper,
                notificationService, org.mockito.Mockito.mock(SearchService.class), userMapper, HMAC_KEY);
    }

    @Test
    void concurrentInsert_duplicateKey_returnsExistingPayment() {
        String idemKey = "test-key-123";

        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setIdempotencyKey(idemKey);
        dto.setJobId(10L);
        dto.setAmount(BigDecimal.TEN);
        dto.setMethod("CASH");

        // First findByIdempotencyKey returns null (no existing payment yet)
        // Then insert throws DuplicateKeyException (concurrent insert won the race)
        // Then second findByIdempotencyKey returns the winner's payment
        Payment winnerPayment = new Payment();
        winnerPayment.setId(99L);
        winnerPayment.setIdempotencyKey(idemKey);
        winnerPayment.setStatus(PaymentStatus.PENDING_SETTLEMENT);

        when(paymentMapper.findByIdempotencyKey(idemKey))
                .thenReturn(null)          // first call: check before insert
                .thenReturn(winnerPayment); // second call: after DuplicateKeyException

        doThrow(new DuplicateKeyException("Duplicate entry for key 'idempotency_key'"))
                .when(paymentMapper).insert(any(Payment.class));

        Payment result = paymentService.processPayment(dto, 5L);

        assertNotNull(result);
        assertEquals(99L, result.getId());
        assertEquals(idemKey, result.getIdempotencyKey());
    }

    @Test
    void normalInsert_noCollision_createsNewPayment() {
        String idemKey = "unique-key-456";

        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setIdempotencyKey(idemKey);
        dto.setJobId(20L);
        dto.setAmount(BigDecimal.valueOf(50));
        dto.setMethod("CASH");

        when(paymentMapper.findByIdempotencyKey(idemKey)).thenReturn(null);
        doAnswer(invocation -> {
            Payment p = invocation.getArgument(0);
            p.setId(42L);
            return 1;
        }).when(paymentMapper).insert(any(Payment.class));

        Payment result = paymentService.processPayment(dto, 5L);

        assertNotNull(result);
        assertEquals(42L, result.getId());
        verify(paymentMapper).insert(any(Payment.class));
    }

    @Test
    void existingSettledPayment_returnsIdempotentResponse() {
        String idemKey = "settled-key";

        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setIdempotencyKey(idemKey);
        dto.setJobId(30L);
        dto.setAmount(BigDecimal.ONE);
        dto.setMethod("CASH");

        Payment existing = new Payment();
        existing.setId(77L);
        existing.setIdempotencyKey(idemKey);
        existing.setStatus(PaymentStatus.SETTLED);

        when(paymentMapper.findByIdempotencyKey(idemKey)).thenReturn(existing);

        Payment result = paymentService.processPayment(dto, 5L);

        assertEquals(77L, result.getId());
        assertEquals(PaymentStatus.SETTLED, result.getStatus());
        // Should NOT attempt insert
        verify(paymentMapper, never()).insert(any());
    }
}
