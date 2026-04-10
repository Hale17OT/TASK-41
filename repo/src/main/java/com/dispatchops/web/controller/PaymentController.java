package com.dispatchops.web.controller;

import com.dispatchops.application.service.PaymentService;
import com.dispatchops.domain.model.LedgerEntry;
import com.dispatchops.domain.model.Payment;
import com.dispatchops.domain.model.ReconciliationItem;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.PaymentCreateDTO;
import com.dispatchops.web.dto.RefundRequestDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<Payment>> processPayment(
            @Valid @RequestBody PaymentCreateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' processing payment for job {}", currentUser.getUsername(), dto.getJobId());
        Payment payment = paymentService.processPayment(dto, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(payment));
    }

    @GetMapping("/{id}")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR})
    public ResponseEntity<ApiResult<Payment>> getPayment(@PathVariable Long id) {
        log.debug("Getting payment with id: {}", id);
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(ApiResult.success(payment));
    }

    @GetMapping("/job/{jobId}")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR})
    public ResponseEntity<ApiResult<List<Payment>>> getPaymentsByJob(@PathVariable Long jobId) {
        log.debug("Getting payments for job {}", jobId);
        List<Payment> payments = paymentService.getPaymentsByJob(jobId);
        return ResponseEntity.ok(ApiResult.success(payments));
    }

    @GetMapping("/list")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR})
    public ResponseEntity<ApiResult<PageResult<Payment>>> listPayments(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing payments - status: {}, from: {}, to: {}, page: {}, size: {}", status, from, to, page, size);
        PageResult<Payment> result = paymentService.listPayments(status, from, to, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/pending")
    @RequireRole({Role.AUDITOR, Role.ADMIN})
    public ResponseEntity<ApiResult<PageResult<Payment>>> listPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing pending settlements - page: {}, size: {}", page, size);
        PageResult<Payment> result = paymentService.listPendingSettlements(page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PostMapping("/{id}/settle")
    @RequireRole({Role.AUDITOR})
    public ResponseEntity<ApiResult<Payment>> settle(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Auditor '{}' settling payment {}", currentUser.getUsername(), id);
        Payment payment = paymentService.settlePayment(id, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(payment));
    }

    @PostMapping("/settle-batch")
    @RequireRole({Role.AUDITOR})
    public ResponseEntity<ApiResult<List<Payment>>> settleBatch(
            @RequestBody List<Long> paymentIds,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Auditor '{}' settling batch of {} payments", currentUser.getUsername(), paymentIds.size());
        List<Payment> payments = paymentService.settleBatch(paymentIds, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(payments));
    }

    @PostMapping("/{id}/refund")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<Payment>> refund(
            @PathVariable Long id,
            @Valid @RequestBody RefundRequestDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' processing refund for payment {}", currentUser.getUsername(), id);
        Payment payment = paymentService.processRefund(id, dto.getAmount(), dto.getReason(), currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(payment));
    }

    /**
     * Device callback endpoint. Exempt from session auth (uses HMAC signature authentication).
     * Implements: receive -> persist -> verify -> process pipeline with replay protection.
     */
    @PostMapping("/callback")
    public ResponseEntity<ApiResult<Map<String, Object>>> processCallback(
            @Valid @RequestBody com.dispatchops.web.dto.DeviceCallbackDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        String sourceIp = request.getRemoteAddr();
        Long deviceSeqId = null;
        try { if (dto.getDeviceSeqId() != null) deviceSeqId = Long.parseLong(dto.getDeviceSeqId()); } catch (NumberFormatException ignored) {}

        log.info("Device callback received: deviceId={}, eventId={}, sourceIp={}", dto.getDeviceId(), dto.getEventId(), sourceIp);

        Map<String, Object> result = paymentService.processDeviceCallback(
                dto.getDeviceId(), dto.getEventId(), dto.getPayload(), dto.getSignature(),
                sourceIp, deviceSeqId, dto.getTimestamp());

        String resultStatus = (String) result.get("status");
        if ("REJECTED".equals(resultStatus)) {
            String message = (String) result.get("message");
            String reasonCode = (String) result.get("reasonCode");
            // Auth failures -> 401, validation failures -> 422
            if ("AUTH_SIGNATURE_INVALID".equals(reasonCode)) {
                return ResponseEntity.status(401).body(ApiResult.error(401, message));
            }
            return ResponseEntity.status(422).body(ApiResult.error(422, message));
        }
        if ("DUPLICATE".equals(resultStatus)) {
            return ResponseEntity.ok(ApiResult.success(result));
        }
        if ("PROCESSED_WITH_ERROR".equals(resultStatus)) {
            return ResponseEntity.status(202).body(ApiResult.success(result));
        }

        return ResponseEntity.status(201).body(ApiResult.success(result));
    }

    @GetMapping("/reconciliation")
    @RequireRole({Role.AUDITOR, Role.ADMIN})
    public ResponseEntity<ApiResult<PageResult<ReconciliationItem>>> listRecon(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing reconciliation items - status: {}, page: {}, size: {}", status, page, size);
        PageResult<ReconciliationItem> result = paymentService.listReconciliationItems(status, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PutMapping("/reconciliation/{id}/resolve")
    @RequireRole({Role.AUDITOR})
    public ResponseEntity<ApiResult<ReconciliationItem>> resolveRecon(
            @PathVariable Long id,
            @Valid @RequestBody com.dispatchops.web.dto.ReconciliationResolveDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        String status = dto.getStatus() != null ? dto.getStatus() : "RESOLVED";
        String note = dto.getNote();
        log.info("Auditor '{}' resolving reconciliation item {}", currentUser.getUsername(), id);
        ReconciliationItem item = paymentService.resolveReconciliation(id, status, note, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(item));
    }

    @GetMapping(value = "/reconciliation/export", produces = "text/csv")
    @RequireRole({Role.AUDITOR, Role.ADMIN})
    public ResponseEntity<byte[]> exportCSV(
            @RequestParam String from,
            @RequestParam String to) {
        log.info("Exporting reconciliation CSV from {} to {}", from, to);
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        byte[] csvData = paymentService.generateReconciliationExport(fromDate, toDate);
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"reconciliation_" + from + "_" + to + ".csv\"")
                .body(csvData);
    }

    @GetMapping("/ledger/{accountId}")
    @RequireRole({Role.AUDITOR, Role.ADMIN})
    public ResponseEntity<ApiResult<PageResult<LedgerEntry>>> getLedger(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Getting ledger for account {} - page: {}, size: {}", accountId, page, size);
        PageResult<LedgerEntry> result = paymentService.getLedgerByAccount(accountId, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/balance/{accountId}")
    @RequireRole({Role.AUDITOR, Role.ADMIN, Role.COURIER, Role.DISPATCHER, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<BigDecimal>> getBalance(
            @PathVariable Long accountId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        // Allow self-access for non-auditor/admin roles
        boolean isPrivileged = currentUser.getRole() == Role.AUDITOR || currentUser.getRole() == Role.ADMIN;
        if (!isPrivileged && !currentUser.getId().equals(accountId)) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Access denied: can only view own balance"));
        }

        log.debug("Getting balance for account {}", accountId);
        BigDecimal balance = paymentService.getBalance(accountId);
        return ResponseEntity.ok(ApiResult.success(balance));
    }

    @PostMapping("/sync")
    @RequireRole({Role.DISPATCHER})
    public ResponseEntity<ApiResult<List<Payment>>> syncOffline(
            @RequestBody List<PaymentCreateDTO> payments,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Dispatcher '{}' syncing {} offline payments", currentUser.getUsername(), payments.size());
        List<Payment> results = new ArrayList<>();
        for (PaymentCreateDTO dto : payments) {
            try {
                results.add(paymentService.processPayment(dto, currentUser.getId()));
            } catch (Exception e) {
                log.warn("Failed to sync offline payment with key {}: {}",
                        dto.getIdempotencyKey(), e.getMessage());
            }
        }
        return ResponseEntity.ok(ApiResult.success(results));
    }
}
