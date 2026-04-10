package com.dispatchops.application.service;

import com.dispatchops.domain.model.LedgerEntry;
import com.dispatchops.domain.model.enums.LedgerEntryType;
import com.dispatchops.infrastructure.persistence.mapper.*;
import com.dispatchops.web.dto.PaymentCreateDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests that ledger entries are created correctly per payment method at SETTLEMENT:
 * - CASH/CHECK: only org CREDIT (no payer DEBIT)
 * - INTERNAL_BALANCE: payer DEBIT + org CREDIT
 *
 * Ledger entries are deferred to settlement, not created at payment creation.
 * This aligns with PaymentService behavior and PaymentLifecycleLedgerTest invariants.
 */
@ExtendWith(MockitoExtension.class)
class PaymentLedgerMethodTest {

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

    private com.dispatchops.domain.model.Payment makePendingPayment(Long id, String method) {
        com.dispatchops.domain.model.Payment payment = new com.dispatchops.domain.model.Payment();
        payment.setId(id);
        payment.setStatus(com.dispatchops.domain.model.enums.PaymentStatus.PENDING_SETTLEMENT);
        payment.setMethod(com.dispatchops.domain.model.enums.PaymentMethod.valueOf(method));
        payment.setAmount(BigDecimal.valueOf(100));
        payment.setPayerId(ACTOR_ID);
        return payment;
    }

    @Test
    void cashPayment_onlyCreditsOrgAccount_noActorDebit() {
        com.dispatchops.domain.model.Payment pending = makePendingPayment(1L, "CASH");
        when(paymentMapper.findById(1L)).thenReturn(pending);
        when(paymentMapper.settle(anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(ORG_ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        paymentService.settlePayment(1L, AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, times(1)).insert(captor.capture());

        LedgerEntry entry = captor.getValue();
        // Cash: only org CREDIT at settlement, no actor DEBIT
        assertEquals(LedgerEntryType.CREDIT, entry.getEntryType());
        assertEquals(ORG_ACCOUNT_ID, entry.getAccountId(), "Should credit system/org account");
    }

    @Test
    void checkPayment_onlyCreditsOrgAccount_noActorDebit() {
        com.dispatchops.domain.model.Payment pending = makePendingPayment(2L, "CHECK");
        when(paymentMapper.findById(2L)).thenReturn(pending);
        when(paymentMapper.settle(anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(ORG_ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        paymentService.settlePayment(2L, AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, times(1)).insert(captor.capture());

        LedgerEntry entry = captor.getValue();
        assertEquals(LedgerEntryType.CREDIT, entry.getEntryType());
        assertEquals(ORG_ACCOUNT_ID, entry.getAccountId());
    }

    @Test
    void internalBalancePayment_debitsActorAndCreditsOrg() {
        com.dispatchops.domain.model.Payment pending = makePendingPayment(3L, "INTERNAL_BALANCE");
        when(paymentMapper.findById(3L)).thenReturn(pending);
        when(paymentMapper.settle(anyLong(), anyLong(), any(), any())).thenReturn(1);
        when(ledgerEntryMapper.calculateBalance(ACTOR_ID)).thenReturn(BigDecimal.valueOf(500));
        when(ledgerEntryMapper.calculateBalance(ORG_ACCOUNT_ID)).thenReturn(BigDecimal.ZERO);

        paymentService.settlePayment(3L, AUDITOR_ID);

        ArgumentCaptor<LedgerEntry> captor = ArgumentCaptor.forClass(LedgerEntry.class);
        verify(ledgerEntryMapper, times(2)).insert(captor.capture());

        List<LedgerEntry> entries = captor.getAllValues();

        // First: DEBIT to payer at settlement
        LedgerEntry debit = entries.get(0);
        assertEquals(LedgerEntryType.DEBIT, debit.getEntryType());
        assertEquals(ACTOR_ID, debit.getAccountId(), "Should debit the actor/payer account");

        // Second: CREDIT to org
        LedgerEntry credit = entries.get(1);
        assertEquals(LedgerEntryType.CREDIT, credit.getEntryType());
        assertEquals(ORG_ACCOUNT_ID, credit.getAccountId(), "Should credit system/org account");
    }
}
