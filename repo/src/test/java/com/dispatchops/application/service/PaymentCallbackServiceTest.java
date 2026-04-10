package com.dispatchops.application.service;

import com.dispatchops.domain.model.CallbackEvent;
import com.dispatchops.domain.model.DeviceCredential;
import com.dispatchops.infrastructure.persistence.mapper.*;
import com.dispatchops.infrastructure.security.HmacUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Real unit tests for PaymentService.processDeviceCallback with mocked mappers.
 * Tests the actual service method branches, not just contract documentation.
 */
@ExtendWith(MockitoExtension.class)
class PaymentCallbackServiceTest {

    private static final String HMAC_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";
    private static final String DEVICE_SECRET = "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789";
    private static final String DEVICE_ID = "test-device-1";

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
        paymentService = new PaymentService(
                paymentMapper, ledgerEntryMapper, reconciliationItemMapper,
                deviceCredentialMapper, callbackEventMapper, notificationService,
                org.mockito.Mockito.mock(SearchService.class), userMapper, HMAC_KEY);
    }

    private String canonical(String deviceId, String eventId, String timestamp, String payload) {
        return deviceId + "|" + eventId + "|" + timestamp + "|" + payload;
    }

    // --- Timestamp rejection ---

    @Test
    void missingTimestampReturnsRejectedWithReasonCode() {
        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-1", "payload", "sig", "127.0.0.1", null, null);
        assertEquals("REJECTED", result.get("status"));
        assertEquals("TIMESTAMP_MISSING", result.get("reasonCode"));
    }

    @Test
    void blankTimestampReturnsRejectedWithReasonCode() {
        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-2", "payload", "sig", "127.0.0.1", null, "   ");
        assertEquals("REJECTED", result.get("status"));
        assertEquals("TIMESTAMP_MISSING", result.get("reasonCode"));
    }

    @Test
    void staleTimestampReturnsRejectedWithReasonCode() {
        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-3", "payload", "sig", "127.0.0.1", null, "2020-01-01T00:00:00Z");
        assertEquals("REJECTED", result.get("status"));
        assertEquals("TIMESTAMP_STALE", result.get("reasonCode"));
    }

    @Test
    void invalidTimestampFormatReturnsRejectedWithReasonCode() {
        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-4", "payload", "sig", "127.0.0.1", null, "not-a-date");
        assertEquals("REJECTED", result.get("status"));
        assertEquals("TIMESTAMP_INVALID", result.get("reasonCode"));
    }

    // --- Duplicate detection ---

    @Test
    void duplicateEventIdReturnsDuplicate() {
        String ts = Instant.now().toString();
        CallbackEvent existing = new CallbackEvent();
        existing.setStatus("PROCESSED");
        when(callbackEventMapper.findByEventId("evt-dup")).thenReturn(existing);

        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-dup", "payload", "sig", "127.0.0.1", null, ts);
        assertEquals("DUPLICATE", result.get("status"));
        verify(callbackEventMapper, never()).insert(any());
    }

    @Test
    void concurrentDuplicateKeyReturnsDuplicate() {
        String ts = Instant.now().toString();
        when(callbackEventMapper.findByEventId("evt-race")).thenReturn(null);
        doThrow(new DuplicateKeyException("Duplicate entry")).when(callbackEventMapper).insert(any());

        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-race", "payload", "sig", "127.0.0.1", null, ts);
        assertEquals("DUPLICATE", result.get("status"));
    }

    // --- Signature verification ---

    @Test
    void invalidSignatureReturnsRejected() {
        String ts = Instant.now().toString();
        when(callbackEventMapper.findByEventId("evt-badsig")).thenReturn(null);
        when(callbackEventMapper.insert(any())).thenReturn(1);

        DeviceCredential cred = new DeviceCredential();
        cred.setSharedSecret(DEVICE_SECRET);
        when(deviceCredentialMapper.findByDeviceId(DEVICE_ID)).thenReturn(cred);

        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-badsig", "payload", "wrong-signature", "127.0.0.1", null, ts);
        assertEquals("REJECTED", result.get("status"));
        assertEquals("AUTH_SIGNATURE_INVALID", result.get("reasonCode"));
        verify(callbackEventMapper).updateStatus(any(), eq("FAILED"), isNull(), isNull(), contains("signature"));
    }

    // --- Successful processing ---

    @Test
    void validCallbackReturnsProcessed() {
        String ts = Instant.now().toString();
        String payload = "{\"paymentRef\":\"pay-1\",\"action\":\"CONFIRM\"}";
        String sig = HmacUtil.computeHmac(canonical(DEVICE_ID, "evt-ok", ts, payload), DEVICE_SECRET);

        when(callbackEventMapper.findByEventId("evt-ok")).thenReturn(null);
        when(callbackEventMapper.insert(any())).thenReturn(1);

        DeviceCredential cred = new DeviceCredential();
        cred.setSharedSecret(DEVICE_SECRET);
        when(deviceCredentialMapper.findByDeviceId(DEVICE_ID)).thenReturn(cred);

        Map<String, Object> result = paymentService.processDeviceCallback(
                DEVICE_ID, "evt-ok", payload, sig, "127.0.0.1", 1L, ts);
        assertEquals("PROCESSED", result.get("status"));

        // Verify lifecycle: RECEIVED -> VERIFIED -> PROCESSED
        verify(callbackEventMapper).insert(any());
        verify(callbackEventMapper).updateStatus(any(), eq("VERIFIED"), any(), isNull(), isNull());
        verify(callbackEventMapper).updateStatus(any(), eq("PROCESSED"), isNull(), any(), isNull());
    }

    @Test
    void unknownDeviceReturnsRejected() {
        String ts = Instant.now().toString();
        when(callbackEventMapper.findByEventId("evt-nodev")).thenReturn(null);
        when(callbackEventMapper.insert(any())).thenReturn(1);
        when(deviceCredentialMapper.findByDeviceId("unknown-device")).thenReturn(null);

        Map<String, Object> result = paymentService.processDeviceCallback(
                "unknown-device", "evt-nodev", "payload", "sig", "127.0.0.1", null, ts);
        assertEquals("REJECTED", result.get("status"));
    }
}
