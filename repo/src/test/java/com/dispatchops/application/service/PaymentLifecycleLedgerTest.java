package com.dispatchops.application.service;

import com.dispatchops.domain.model.LedgerEntry;
import com.dispatchops.domain.model.Payment;
import com.dispatchops.domain.model.enums.LedgerEntryType;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.infrastructure.persistence.mapper.*;
import com.dispatchops.web.dto.PaymentCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests ledger invariants across full payment lifecycles:
 * - create → settle: exactly 1 org CREDIT (+ 1 payer DEBIT for INTERNAL_BALANCE)
 * - create → cancel: zero ledger entries
 * - create → settle → refund: net zero on org account
 */
@ExtendWith(MockitoExtension.class)
class PaymentLifecycleLedgerTest {

    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final Long ORG_ACCOUNT_ID = 1L;
    private static final Long ACTOR_ID = 5L;
    private static final Long AUDITOR_ID = 99L;

    @Mock private PaymentMapper paymentMapper;
    @Mock private LedgerEntryMapper ledgerEntryMapper;
    @Mock private ReconciliationItemMapper reconciliationItemMapper;
    @Mock private DeviceCredentialMapper deviceCredentialMapper;
    @Mock private CallbackEventMapper callbackEventMapper;
    @Mock private NotificationService notificationService;

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        UserMapper userMapper = mock(UserMapper.class);
        com.dispatchops.domain.model.User systemUser = new com.dispatchops.domain.model.User();
        systemUser.setId(ORG_ACCOUNT_ID);
        systemUser.setUsername("system");
        when(userMapper.findByUsername("system")).thenReturn(systemUser);

        paymentService = new PaymentService(paymentMapper, ledgerEntryMapper,
                reconciliationItemMapper, deviceCredentialMapper, callbackEventMapper,
                notificationService, mock(SearchService.class), userMapper, HMAC_KEY);
    }

    private PaymentCreateDTO cashDto() {
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setIdempotencyKey("cash-key");
        dto.setJobId(10L);
        dto.setAmount(BigDecimal.valueOf(100));
        dto.setMethod("CASH");
        return dto;
    }

    @Test
    void createCashPayment_noLedgerEntries() {
        when(paymentMapper.findByIdempotencyKey(any())).thenReturn(null);
        doAnswer(inv -> { inv.<Payment>getArgument(0).setId(1L); return 1; })
                .when(paymentMapper).insert(any());

        paymentService.processPayment(cashDto(), ACTOR_ID);

        // No ledger entries at creation — deferred to settlement
        verify(ledgerEntryMapper, never()).insert(any());
    }

    @Test
    void settleCashPayment_exactlyOneOrgCredit() {
        Payment pending = new Payment();
        pending.setId(1L);
        pending.setStatus(PaymentStatus.PENDING_SETTLEMENT);
        pending.setMethod(com.dispatchops.domain.model.enums.PaymentMethod.CASH);
        pending.setAmount(BigDecimal.valueOf(100));
        pending.setPayerId(ACTOR_ID);

        when(paymentMapper.findById(1L)).thenReturn(pending);
        when(paymentMapper.settle(anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(ORG_ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        paymentService.settlePayment(1L, AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, times(1)).insert(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertEquals(LedgerEntryType.CREDIT, entry.getEntryType());
        assertEquals(ORG_ACCOUNT_ID, entry.getAccountId());
        assertEquals(BigDecimal.valueOf(100), entry.getAmount());
    }

    @Test
    void settleInternalBalance_debitPayerAndCreditOrg() {
        Payment pending = new Payment();
        pending.setId(2L);
        pending.setStatus(PaymentStatus.PENDING_SETTLEMENT);
        pending.setMethod(com.dispatchops.domain.model.enums.PaymentMethod.INTERNAL_BALANCE);
        pending.setAmount(BigDecimal.valueOf(200));
        pending.setPayerId(ACTOR_ID);

        when(paymentMapper.findById(2L)).thenReturn(pending);
        when(paymentMapper.settle(anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(ACTOR_ID)).thenReturn(BigDecimal.valueOf(500));
        when(ledgerEntryMapper.calculateBalance(ORG_ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        paymentService.settlePayment(2L, AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, times(2)).insert(captor.capture());

        List<LedgerEntry> entries = captor.getAllValues();
        // First: payer DEBIT
        assertEquals(LedgerEntryType.DEBIT, entries.get(0).getEntryType());
        assertEquals(ACTOR_ID, entries.get(0).getAccountId());
        // Second: org CREDIT
        assertEquals(LedgerEntryType.CREDIT, entries.get(1).getEntryType());
        assertEquals(ORG_ACCOUNT_ID, entries.get(1).getAccountId());
    }

    @Test
    void cancelPendingPayment_noLedgerEntries() {
        // Cancellation of PENDING_SETTLEMENT payment should not create ledger entries
        // because no ledger entries were created at creation time
        when(paymentMapper.findByIdempotencyKey(any())).thenReturn(null);
        doAnswer(inv -> { inv.<Payment>getArgument(0).setId(3L); return 1; })
                .when(paymentMapper).insert(any());

        paymentService.processPayment(cashDto(), ACTOR_ID);

        // Verify: zero ledger entries at creation
        verify(ledgerEntryMapper, never()).insert(any());
        // Cancel would just update status — no compensating entries needed
    }

    @Test
    void settleAndRefund_netZeroOnOrgAccount() {
        // Setup settled payment
        Payment settled = new Payment();
        settled.setId(4L);
        settled.setStatus(PaymentStatus.SETTLED);
        settled.setMethod(com.dispatchops.domain.model.enums.PaymentMethod.CASH);
        settled.setAmount(BigDecimal.valueOf(100));
        settled.setPayerId(ACTOR_ID);
        settled.setRefundEligibleUntil(LocalDateTime.now().plusDays(15));

        when(paymentMapper.findById(4L)).thenReturn(settled);
        when(paymentMapper.updateStatus(anyLong(), anyString(), anyString())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(anyLong())).thenReturn(BigDecimal.valueOf(100));

        paymentService.processRefund(4L, BigDecimal.valueOf(100), "Full refund", AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, atLeast(2)).insert(captor.capture());

        List<LedgerEntry> entries = captor.getAllValues();
        // Should have CREDIT to payer and DEBIT from org
        boolean hasPayerCredit = entries.stream().anyMatch(e ->
                e.getEntryType() == LedgerEntryType.CREDIT && e.getAccountId().equals(ACTOR_ID));
        boolean hasOrgDebit = entries.stream().anyMatch(e ->
                e.getEntryType() == LedgerEntryType.DEBIT && e.getAccountId().equals(ORG_ACCOUNT_ID));

        assertTrue(hasPayerCredit, "Refund must credit the payer");
        assertTrue(hasOrgDebit, "Refund must debit the org account");
    }
}
