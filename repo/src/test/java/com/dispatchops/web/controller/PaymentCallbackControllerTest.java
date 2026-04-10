package com.dispatchops.web.controller;

import com.dispatchops.application.service.PaymentService;
import com.dispatchops.infrastructure.security.HmacUtil;
import com.dispatchops.web.advice.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-level tests for POST /api/payments/callback.
 * Verifies HTTP status codes, DTO validation, and response mapping for every branch.
 */
@ExtendWith(MockitoExtension.class)
class PaymentCallbackControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PaymentService paymentService;

    @InjectMocks
    private PaymentController paymentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(paymentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private Map<String, String> validPayload() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("deviceId", "dev-1");
        m.put("eventId", "evt-1");
        m.put("payload", "{\"amount\":100}");
        m.put("signature", "somesig");
        m.put("timestamp", Instant.now().toString());
        return m;
    }

    // --- DTO validation (422 from bean validation) ---

    @Test
    void missingDeviceIdReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.remove("deviceId");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingEventIdReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.remove("eventId");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingPayloadReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.remove("payload");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingSignatureReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.remove("signature");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void missingTimestampReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.remove("timestamp");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void blankTimestampReturns422() throws Exception {
        Map<String, String> body = validPayload();
        body.put("timestamp", "   ");
        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isUnprocessableEntity());
    }

    // --- Service returns REJECTED with AUTH_SIGNATURE_INVALID reasonCode -> 401 ---

    @Test
    void signatureRejectionReturns401() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "REJECTED");
        serviceResult.put("reasonCode", "AUTH_SIGNATURE_INVALID");
        serviceResult.put("message", "Invalid callback signature");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    // --- Service returns REJECTED with TIMESTAMP_* reasonCode -> 422 ---

    @Test
    void staleTimestampRejectionReturns422() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "REJECTED");
        serviceResult.put("reasonCode", "TIMESTAMP_STALE");
        serviceResult.put("message", "Callback timestamp outside acceptable window (300s)");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void invalidTimestampFormatRejectionReturns422() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "REJECTED");
        serviceResult.put("reasonCode", "TIMESTAMP_INVALID");
        serviceResult.put("message", "Invalid timestamp format. Use ISO-8601.");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    // --- Duplicate -> 200 ---

    @Test
    void duplicateEventReturns200() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "DUPLICATE");
        serviceResult.put("message", "Callback already processed");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DUPLICATE"));
    }

    // --- Processed with error -> 202 ---

    @Test
    void processedWithErrorReturns202() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "PROCESSED_WITH_ERROR");
        serviceResult.put("message", "Callback verified but processing failed: parse error");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.status").value("PROCESSED_WITH_ERROR"));
    }

    // --- Successful processing -> 201 ---

    @Test
    void successfulProcessingReturns201() throws Exception {
        Map<String, Object> serviceResult = new LinkedHashMap<>();
        serviceResult.put("status", "PROCESSED");
        serviceResult.put("message", "Callback verified and processed");
        serviceResult.put("eventId", "evt-1");

        when(paymentService.processDeviceCallback(
                anyString(), anyString(), anyString(), anyString(),
                anyString(), any(), anyString()))
                .thenReturn(serviceResult);

        mockMvc.perform(post("/api/payments/callback")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validPayload())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PROCESSED"))
                .andExpect(jsonPath("$.data.eventId").value("evt-1"));
    }
}
