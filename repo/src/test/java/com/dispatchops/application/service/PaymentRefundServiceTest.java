package com.dispatchops.application.service;

import com.dispatchops.domain.exception.RefundWindowClosedException;
import com.dispatchops.domain.model.Payment;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.infrastructure.persistence.mapper.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentRefundServiceTest {

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
        com.dispatchops.infrastructure.persistence.mapper.UserMapper userMapper =
                org.mockito.Mockito.mock(com.dispatchops.infrastructure.persistence.mapper.UserMapper.class);
        com.dispatchops.domain.model.User systemUser = new com.dispatchops.domain.model.User();
        systemUser.setId(1L);
        systemUser.setUsername("system");
        org.mockito.Mockito.when(userMapper.findByUsername("system")).thenReturn(systemUser);
        paymentService = new PaymentService(paymentMapper, ledgerEntryMapper,
                reconciliationItemMapper, deviceCredentialMapper, callbackEventMapper,
                notificationService, org.mockito.Mockito.mock(SearchService.class), userMapper, HMAC_KEY);
    }

    @Test
    void refundWithinWindowSucceeds() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setPayerId(2L);
        payment.setRefundEligibleUntil(LocalDateTime.now().plusDays(15));

        when(paymentMapper.findById(1L)).thenReturn(payment);
        when(paymentMapper.updateStatus(anyLong(), anyString(), anyString())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(anyLong())).thenReturn(BigDecimal.ZERO);
        when(ledgerEntryMapper.insert(any())).thenReturn(1);

        Payment result = paymentService.processRefund(1L, new BigDecimal("50.00"), "test", 3L);
        assertEquals(PaymentStatus.REFUNDED, result.getStatus());
    }

    @Test
    void refundAfterWindowThrows() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setRefundEligibleUntil(LocalDateTime.now().minusDays(1));

        when(paymentMapper.findById(1L)).thenReturn(payment);

        assertThrows(RefundWindowClosedException.class,
                () -> paymentService.processRefund(1L, new BigDecimal("50.00"), "test", 3L));
    }

    @Test
    void refundExceedingAmountThrows() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setAmount(new BigDecimal("100.00"));
        payment.setRefundEligibleUntil(LocalDateTime.now().plusDays(15));

        when(paymentMapper.findById(1L)).thenReturn(payment);

        assertThrows(IllegalArgumentException.class,
                () -> paymentService.processRefund(1L, new BigDecimal("150.00"), "test", 3L));
    }

    @Test
    void refundOnNonSettledPaymentThrows() {
        Payment payment = new Payment();
        payment.setId(1L);
        payment.setStatus(PaymentStatus.PENDING_SETTLEMENT);

        when(paymentMapper.findById(1L)).thenReturn(payment);

        assertThrows(IllegalStateException.class,
                () -> paymentService.processRefund(1L, new BigDecimal("50.00"), "test", 3L));
    }

    @Test
    void idempotentPaymentReturnsSameRecord() {
        Payment existing = new Payment();
        existing.setId(1L);
        existing.setStatus(PaymentStatus.SETTLED);
        existing.setIdempotencyKey("key-123");

        when(paymentMapper.findByIdempotencyKey("key-123")).thenReturn(existing);

        var dto = new com.dispatchops.web.dto.PaymentCreateDTO();
        dto.setIdempotencyKey("key-123");
        dto.setJobId(1L);
        dto.setAmount(new BigDecimal("50.00"));
        dto.setMethod("CASH");

        Payment result = paymentService.processPayment(dto, 2L);
        assertEquals(1L, result.getId());
        verify(paymentMapper, never()).insert(any());
    }
}
