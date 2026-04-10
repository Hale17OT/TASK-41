package com.dispatchops.application.service;

import com.dispatchops.domain.exception.RefundWindowClosedException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.DeviceCredential;
import com.dispatchops.domain.model.LedgerEntry;
import com.dispatchops.domain.model.Payment;
import com.dispatchops.domain.model.ReconciliationItem;
import com.dispatchops.domain.model.enums.LedgerEntryType;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.domain.model.enums.PaymentMethod;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.domain.service.StatusTransitionValidator;
import com.dispatchops.infrastructure.persistence.mapper.CallbackEventMapper;
import com.dispatchops.infrastructure.persistence.mapper.DeviceCredentialMapper;
import com.dispatchops.infrastructure.persistence.mapper.LedgerEntryMapper;
import com.dispatchops.infrastructure.persistence.mapper.PaymentMapper;
import com.dispatchops.infrastructure.persistence.mapper.ReconciliationItemMapper;
import com.dispatchops.infrastructure.security.HmacUtil;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.PaymentCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private static final long REFUND_ELIGIBLE_DAYS = 30;
    private static final String SYSTEM_ACCOUNT_USERNAME = "system";

    /** Resolved at startup from DB by username='system' */
    private Long systemAccountId;

    private final PaymentMapper paymentMapper;
    private final LedgerEntryMapper ledgerEntryMapper;
    private final ReconciliationItemMapper reconciliationItemMapper;
    private final DeviceCredentialMapper deviceCredentialMapper;
    private final CallbackEventMapper callbackEventMapper;
    private final NotificationService notificationService;
    private final SearchService searchService;
    private final String hmacKey;

    public PaymentService(PaymentMapper paymentMapper,
                          LedgerEntryMapper ledgerEntryMapper,
                          ReconciliationItemMapper reconciliationItemMapper,
                          DeviceCredentialMapper deviceCredentialMapper,
                          CallbackEventMapper callbackEventMapper,
                          NotificationService notificationService,
                          SearchService searchService,
                          com.dispatchops.infrastructure.persistence.mapper.UserMapper userMapper,
                          @Value("${security.hmac.key}") String hmacKey) {
        this.paymentMapper = paymentMapper;
        this.ledgerEntryMapper = ledgerEntryMapper;
        this.reconciliationItemMapper = reconciliationItemMapper;
        this.deviceCredentialMapper = deviceCredentialMapper;
        this.callbackEventMapper = callbackEventMapper;
        this.searchService = searchService;
        this.notificationService = notificationService;
        this.hmacKey = hmacKey;

        // Resolve system account ID by username at startup — fail-fast if missing
        com.dispatchops.domain.model.User sysUser = userMapper.findByUsername(SYSTEM_ACCOUNT_USERNAME);
        if (sysUser == null) {
            throw new IllegalStateException(
                    "System account '" + SYSTEM_ACCOUNT_USERNAME + "' not found in database. " +
                    "Payments require a system user for ledger entries. Seed the database before startup.");
        }
        this.systemAccountId = sysUser.getId();
        log.info("System account resolved: id={}", this.systemAccountId);
    }

    public Payment processPayment(PaymentCreateDTO dto, Long actorId) {
        log.info("Processing payment for jobId={} with idempotencyKey={}", dto.getJobId(), dto.getIdempotencyKey());

        Payment existing = paymentMapper.findByIdempotencyKey(dto.getIdempotencyKey());
        if (existing != null) {
            if (existing.getStatus() == PaymentStatus.SETTLED
                    || existing.getStatus() == PaymentStatus.PENDING_SETTLEMENT) {
                log.info("Idempotent return for existing payment id={} with status={}",
                        existing.getId(), existing.getStatus());
                return existing;
            }
            if (existing.getStatus() != PaymentStatus.CANCELLED) {
                log.info("Existing payment id={} has status={}, returning as-is",
                        existing.getId(), existing.getStatus());
                return existing;
            }
            log.info("Previous payment id={} was CANCELLED, allowing retry", existing.getId());
        }

        PaymentMethod method = PaymentMethod.valueOf(dto.getMethod());

        // For INTERNAL_BALANCE: check available funds before proceeding
        if (method == PaymentMethod.INTERNAL_BALANCE) {
            BigDecimal available = ledgerEntryMapper.calculateBalance(actorId);
            if (available == null) available = BigDecimal.ZERO;
            if (available.compareTo(dto.getAmount()) < 0) {
                throw new com.dispatchops.domain.exception.InsufficientFundsException(
                        "Insufficient internal balance. Available: $" + available + ", Requested: $" + dto.getAmount(),
                        available, dto.getAmount());
            }
        }

        Payment payment = new Payment();
        payment.setIdempotencyKey(dto.getIdempotencyKey());
        payment.setJobId(dto.getJobId());
        payment.setPayerId(actorId);
        payment.setAmount(dto.getAmount());
        payment.setMethod(method);
        payment.setStatus(PaymentStatus.PENDING_SETTLEMENT);
        payment.setCheckNumber(dto.getCheckNumber());
        payment.setDeviceId(dto.getDeviceId());
        payment.setDeviceSeqId(dto.getDeviceSeqId());
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());

        try {
            paymentMapper.insert(payment);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // Concurrent insert with same idempotency_key — return existing payment
            log.info("Concurrent idempotency collision for key={}, returning existing", dto.getIdempotencyKey());
            Payment concurrent = paymentMapper.findByIdempotencyKey(dto.getIdempotencyKey());
            if (concurrent != null) {
                return concurrent;
            }
            throw e; // Should not happen, but re-throw if lookup fails
        }
        log.info("Payment id={} created with status PENDING_SETTLEMENT", payment.getId());

        // Ledger entries are posted at SETTLEMENT, not at creation.
        // For INTERNAL_BALANCE: payer debit is reserved at settlement to maintain atomicity.
        // This keeps PENDING_SETTLEMENT as a pure "intent" state with no ledger impact,
        // so cancellation requires no compensating entries.
        log.debug("Payment id={} created in PENDING_SETTLEMENT — ledger entries deferred to settlement", payment.getId());

        notificationService.notify(
                actorId,
                NotificationType.PAYMENT_EVENT,
                "Payment Processed",
                "Your payment of $" + dto.getAmount() + " has been submitted for settlement.",
                "Payment",
                payment.getId());

        try { searchService.indexEntity("PAYMENT", payment.getId(), payment.getIdempotencyKey(),
                "Job " + payment.getJobId() + " " + payment.getMethod() + " " + payment.getAmount(),
                payment.getStatus().name(), payment.getPayerId()); }
        catch (Exception e) { log.warn("Failed to index payment: {}", e.getMessage()); }

        return payment;
    }

    public Payment settlePayment(Long paymentId, Long auditorId) {
        log.info("Settling payment id={} by auditorId={}", paymentId, auditorId);

        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found with id: " + paymentId, "Payment", paymentId);
        }

        if (payment.getStatus() != PaymentStatus.PENDING_SETTLEMENT) {
            throw new IllegalStateException(
                    "Cannot settle payment in status " + payment.getStatus() + ". Must be PENDING_SETTLEMENT.");
        }

        StatusTransitionValidator.validatePaymentTransition(payment.getStatus(), PaymentStatus.SETTLED);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime refundEligibleUntil = now.plusDays(REFUND_ELIGIBLE_DAYS);

        int settled = paymentMapper.settle(paymentId, auditorId, now, refundEligibleUntil);
        if (settled == 0) {
            throw new IllegalStateException(
                    "Payment " + paymentId + " is no longer in PENDING_SETTLEMENT status (concurrent modification)");
        }
        payment.setStatus(PaymentStatus.SETTLED);
        payment.setSettledBy(auditorId);
        payment.setSettledAt(now);
        payment.setRefundEligibleUntil(refundEligibleUntil);

        // For INTERNAL_BALANCE: debit payer account at settlement
        if (payment.getMethod() == PaymentMethod.INTERNAL_BALANCE) {
            BigDecimal payerBalance = ledgerEntryMapper.calculateBalance(payment.getPayerId());
            BigDecimal payerAfter = (payerBalance != null ? payerBalance : BigDecimal.ZERO)
                    .subtract(payment.getAmount());

            LedgerEntry debitEntry = new LedgerEntry();
            debitEntry.setPaymentId(paymentId);
            debitEntry.setAccountId(payment.getPayerId());
            debitEntry.setEntryType(LedgerEntryType.DEBIT);
            debitEntry.setAmount(payment.getAmount());
            debitEntry.setBalanceAfter(payerAfter);
            debitEntry.setDescription("Settlement debit for payment " + paymentId);
            debitEntry.setReferenceType("Payment");
            debitEntry.setReferenceId(paymentId);
            debitEntry.setCreatedAt(LocalDateTime.now());
            ledgerEntryMapper.insert(debitEntry);
            log.debug("DEBIT ledger entry created for payer at settlement, payment id={}", paymentId);
        }

        // All methods: credit org account at settlement
        Long orgAccountId = systemAccountId;
        BigDecimal orgBalance = ledgerEntryMapper.calculateBalance(orgAccountId);
        BigDecimal orgBalanceAfter = (orgBalance != null ? orgBalance : BigDecimal.ZERO)
                .add(payment.getAmount());

        LedgerEntry creditEntry = new LedgerEntry();
        creditEntry.setPaymentId(paymentId);
        creditEntry.setAccountId(orgAccountId);
        creditEntry.setEntryType(LedgerEntryType.CREDIT);
        creditEntry.setAmount(payment.getAmount());
        creditEntry.setBalanceAfter(orgBalanceAfter);
        creditEntry.setDescription("Settlement for payment " + paymentId);
        creditEntry.setReferenceType("Payment");
        creditEntry.setReferenceId(paymentId);
        creditEntry.setCreatedAt(LocalDateTime.now());

        ledgerEntryMapper.insert(creditEntry);

        log.info("Payment id={} settled by auditor {}", paymentId, auditorId);
        try { searchService.indexEntity("PAYMENT", payment.getId(), payment.getIdempotencyKey(),
                "Job " + payment.getJobId() + " " + payment.getMethod() + " SETTLED",
                "SETTLED", payment.getPayerId()); }
        catch (Exception e) { log.warn("Failed to reindex payment: {}", e.getMessage()); }
        return payment;
    }

    public List<Payment> settleBatch(List<Long> paymentIds, Long auditorId) {
        log.info("Settling batch of {} payments by auditorId={}", paymentIds.size(), auditorId);

        List<Payment> results = new ArrayList<>();
        for (Long paymentId : paymentIds) {
            try {
                Payment settled = settlePayment(paymentId, auditorId);
                results.add(settled);
            } catch (Exception e) {
                log.error("Failed to settle payment id={}: {}", paymentId, e.getMessage());
                Payment failed = paymentMapper.findById(paymentId);
                if (failed != null) {
                    results.add(failed);
                }
            }
        }

        log.info("Batch settlement complete: {} of {} payments settled", results.size(), paymentIds.size());
        return results;
    }

    public Payment processRefund(Long paymentId, BigDecimal amount, String reason, Long actorId) {
        log.info("Processing refund for paymentId={}, amount={}, by actorId={}", paymentId, amount, actorId);

        Payment payment = paymentMapper.findById(paymentId);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found with id: " + paymentId, "Payment", paymentId);
        }

        if (payment.getStatus() != PaymentStatus.SETTLED) {
            throw new IllegalStateException(
                    "Cannot refund payment in status " + payment.getStatus() + ". Must be SETTLED.");
        }

        if (payment.getRefundEligibleUntil() == null) {
            throw new RefundWindowClosedException(
                    "Refund window not established for this payment. Cannot process refund without eligibility date.");
        }
        if (payment.getRefundEligibleUntil().isBefore(LocalDateTime.now())) {
            throw new RefundWindowClosedException(
                    "Refund window has closed. Refund was eligible until " + payment.getRefundEligibleUntil());
        }

        if (amount.compareTo(payment.getAmount()) > 0) {
            throw new IllegalArgumentException(
                    "Refund amount $" + amount + " exceeds original payment amount $" + payment.getAmount());
        }

        StatusTransitionValidator.validatePaymentTransition(payment.getStatus(), PaymentStatus.REFUND_PENDING);
        paymentMapper.updateStatus(paymentId, PaymentStatus.REFUND_PENDING.name(), PaymentStatus.SETTLED.name());

        StatusTransitionValidator.validatePaymentTransition(PaymentStatus.REFUND_PENDING, PaymentStatus.REFUNDED);
        paymentMapper.updateStatus(paymentId, PaymentStatus.REFUNDED.name(), PaymentStatus.REFUND_PENDING.name());

        payment.setStatus(PaymentStatus.REFUNDED);

        // CREDIT to payer (refund back to payer)
        BigDecimal payerBalance = ledgerEntryMapper.calculateBalance(payment.getPayerId());
        BigDecimal payerBalanceAfter = (payerBalance != null ? payerBalance : BigDecimal.ZERO).add(amount);

        LedgerEntry creditToPayer = new LedgerEntry();
        creditToPayer.setPaymentId(paymentId);
        creditToPayer.setAccountId(payment.getPayerId());
        creditToPayer.setEntryType(LedgerEntryType.CREDIT);
        creditToPayer.setAmount(amount);
        creditToPayer.setBalanceAfter(payerBalanceAfter);
        creditToPayer.setDescription("Refund for payment " + paymentId + ": " + reason);
        creditToPayer.setReferenceType("Payment");
        creditToPayer.setReferenceId(paymentId);
        creditToPayer.setCreatedAt(LocalDateTime.now());
        ledgerEntryMapper.insert(creditToPayer);

        // DEBIT from org account
        Long orgAccountId = systemAccountId;
        BigDecimal orgBalance = ledgerEntryMapper.calculateBalance(orgAccountId);
        BigDecimal orgBalanceAfter = (orgBalance != null ? orgBalance : BigDecimal.ZERO).subtract(amount);

        LedgerEntry debitFromOrg = new LedgerEntry();
        debitFromOrg.setPaymentId(paymentId);
        debitFromOrg.setAccountId(orgAccountId);
        debitFromOrg.setEntryType(LedgerEntryType.DEBIT);
        debitFromOrg.setAmount(amount);
        debitFromOrg.setBalanceAfter(orgBalanceAfter);
        debitFromOrg.setDescription("Refund debit for payment " + paymentId + ": " + reason);
        debitFromOrg.setReferenceType("Payment");
        debitFromOrg.setReferenceId(paymentId);
        debitFromOrg.setCreatedAt(LocalDateTime.now());
        ledgerEntryMapper.insert(debitFromOrg);

        notificationService.notify(
                payment.getPayerId(),
                NotificationType.PAYMENT_EVENT,
                "Refund Processed",
                "A refund of $" + amount + " has been processed for your payment. Reason: " + reason,
                "Payment",
                paymentId);

        log.info("Refund of {} processed for payment id={}", amount, paymentId);
        try { searchService.indexEntity("PAYMENT", payment.getId(), payment.getIdempotencyKey(),
                "Job " + payment.getJobId() + " " + payment.getMethod() + " REFUNDED",
                "REFUNDED", payment.getPayerId()); }
        catch (Exception e) { log.warn("Failed to reindex payment after refund: {}", e.getMessage()); }
        return payment;
    }

    /**
     * Verify device callback signature.
     * Canonical signed string: deviceId + "|" + eventId + "|" + timestamp + "|" + payload
     * This cryptographically binds all metadata fields to prevent replay with substituted eventId/timestamp.
     */
    @Transactional(readOnly = true)
    public boolean verifyDeviceCallback(String deviceId, String eventId, String timestamp, String payload, String signature) {
        log.debug("Verifying device callback for deviceId={}, eventId={}", deviceId, eventId);

        DeviceCredential credential = deviceCredentialMapper.findByDeviceId(deviceId);
        if (credential == null) {
            log.warn("No device credential found for deviceId={}", deviceId);
            return false;
        }

        // Canonical string binds all fields: prevents replay with different eventId or timestamp
        String canonical = deviceId + "|" + eventId + "|" + timestamp + "|" + payload;
        String computed = HmacUtil.computeHmac(canonical, credential.getSharedSecret());
        boolean valid = java.security.MessageDigest.isEqual(
                computed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                signature.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        log.debug("Device callback verification for deviceId={}: valid={}", deviceId, valid);
        return valid;
    }

    /** Maximum allowed clock drift for callback timestamps (5 minutes). */
    private static final long CALLBACK_TIMESTAMP_DRIFT_SECONDS = 300;

    /**
     * Full device callback ingestion pipeline: receive -> persist -> verify -> process.
     * Timestamp is mandatory. Signature covers canonical string: deviceId|eventId|timestamp|payload.
     */
    public java.util.Map<String, Object> processDeviceCallback(
            String deviceId, String eventId, String payload, String signature,
            String sourceIp, Long deviceSeqId, String timestamp) {
        log.info("Processing device callback: deviceId={}, eventId={}", deviceId, eventId);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();

        // Step 1: Timestamp is mandatory and must be fresh
        if (timestamp == null || timestamp.isBlank()) {
            result.put("status", "REJECTED");
            result.put("reasonCode", "TIMESTAMP_MISSING");
            result.put("message", "Missing required field: timestamp (ISO-8601)");
            result.put("eventId", eventId);
            return result;
        }
        try {
            java.time.Instant callbackTime = java.time.Instant.parse(timestamp);
            long driftSeconds = Math.abs(java.time.Duration.between(callbackTime, java.time.Instant.now()).getSeconds());
            if (driftSeconds > CALLBACK_TIMESTAMP_DRIFT_SECONDS) {
                log.warn("Callback timestamp too stale: drift={}s, eventId={}", driftSeconds, eventId);
                result.put("status", "REJECTED");
                result.put("reasonCode", "TIMESTAMP_STALE");
                result.put("message", "Callback timestamp outside acceptable window (" + CALLBACK_TIMESTAMP_DRIFT_SECONDS + "s)");
                result.put("eventId", eventId);
                return result;
            }
        } catch (java.time.format.DateTimeParseException e) {
            log.warn("Invalid callback timestamp format for eventId={}: {}", eventId, timestamp);
            result.put("status", "REJECTED");
            result.put("reasonCode", "TIMESTAMP_INVALID");
            result.put("message", "Invalid timestamp format. Use ISO-8601.");
            result.put("eventId", eventId);
            return result;
        }

        // Step 2: Replay protection - check if eventId already exists (pre-check)
        com.dispatchops.domain.model.CallbackEvent existing = callbackEventMapper.findByEventId(eventId);
        if (existing != null) {
            log.info("Duplicate callback eventId={} (status={})", eventId, existing.getStatus());
            result.put("status", "DUPLICATE");
            result.put("message", "Callback already processed");
            result.put("eventId", eventId);
            result.put("originalStatus", existing.getStatus());
            return result;
        }

        // Step 3: Persist raw event (receipt durability) with DB-level unique constraint
        String payloadHash = HmacUtil.computeHmac(payload, hmacKey);
        com.dispatchops.domain.model.CallbackEvent event = new com.dispatchops.domain.model.CallbackEvent();
        event.setEventId(eventId);
        event.setDeviceId(deviceId);
        event.setDeviceSeqId(deviceSeqId);
        event.setPayloadHash(payloadHash);
        event.setRawPayload(payload);
        event.setSignature(signature);
        event.setSourceIp(sourceIp);
        event.setStatus("RECEIVED");
        event.setReceivedAt(java.time.LocalDateTime.now());

        try {
            callbackEventMapper.insert(event);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            log.info("Concurrent duplicate callback eventId={}, returning idempotent response", eventId);
            result.put("status", "DUPLICATE");
            result.put("message", "Callback already processed (concurrent)");
            result.put("eventId", eventId);
            return result;
        }
        log.debug("Callback event persisted: id={}, eventId={}", event.getId(), eventId);

        // Step 4: Verify HMAC signature over canonical string (binds deviceId+eventId+timestamp+payload)
        boolean valid = verifyDeviceCallback(deviceId, eventId, timestamp, payload, signature);
        if (!valid) {
            callbackEventMapper.updateStatus(event.getId(), "FAILED", null, null, "Invalid HMAC signature");
            log.warn("Callback signature verification failed for eventId={}", eventId);
            result.put("status", "REJECTED");
            result.put("reasonCode", "AUTH_SIGNATURE_INVALID");
            result.put("message", "Invalid callback signature");
            result.put("eventId", eventId);
            return result;
        }
        callbackEventMapper.updateStatus(event.getId(), "VERIFIED", java.time.LocalDateTime.now(), null, null);

        // Step 5: Business processing - reconcile payment state based on callback payload
        String processingError = null;
        try {
            processCallbackBusinessLogic(deviceId, payload, deviceSeqId);
        } catch (Exception e) {
            processingError = e.getMessage();
            log.error("Callback business processing failed for eventId={}: {}", eventId, processingError);
        }

        if (processingError != null) {
            callbackEventMapper.updateStatus(event.getId(), "FAILED", null, null, processingError);
            result.put("status", "PROCESSED_WITH_ERROR");
            result.put("message", "Callback verified but processing failed: " + processingError);
            result.put("eventId", eventId);
            return result;
        }

        callbackEventMapper.updateStatus(event.getId(), "PROCESSED", null, java.time.LocalDateTime.now(), null);
        log.info("Callback processed successfully: eventId={}, deviceId={}", eventId, deviceId);
        result.put("status", "PROCESSED");
        result.put("message", "Callback verified and processed");
        result.put("eventId", eventId);
        result.put("deviceId", deviceId);
        return result;
    }

    /**
     * Domain-side business processing for a verified device callback.
     * Parses the payload and drives payment state transitions.
     */
    private void processCallbackBusinessLogic(String deviceId, String payload, Long deviceSeqId) {
        // Parse payload for payment reference and action
        // Expected format: JSON with fields like paymentRef, action (CONFIRM/CANCEL), amount
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = mapper.readValue(payload, java.util.Map.class);

            String paymentRef = (String) data.get("paymentRef");
            String action = (String) data.get("action");

            if (paymentRef != null && action != null) {
                Payment payment = paymentMapper.findByIdempotencyKey(paymentRef);
                if (payment != null) {
                    if ("CONFIRM".equals(action) && payment.getStatus() == PaymentStatus.PENDING_SETTLEMENT) {
                        log.info("Device callback confirms payment ref={}", paymentRef);
                        // Device confirms receipt - no state change needed, payment stays PENDING_SETTLEMENT
                        // until auditor settles via daily drawer close
                    } else if ("CANCEL".equals(action) && payment.getStatus() == PaymentStatus.PENDING_SETTLEMENT) {
                        log.info("Device callback cancels payment ref={}", paymentRef);
                        paymentMapper.updateStatus(payment.getId(),
                                PaymentStatus.CANCELLED.name(), PaymentStatus.PENDING_SETTLEMENT.name());
                        try { searchService.indexEntity("PAYMENT", payment.getId(), payment.getIdempotencyKey(),
                                "Job " + payment.getJobId() + " " + payment.getMethod() + " CANCELLED",
                                "CANCELLED", payment.getPayerId()); }
                        catch (Exception e) { log.warn("Failed to reindex cancelled payment: {}", e.getMessage()); }
                    }
                } else {
                    log.debug("No payment found for ref={}, callback recorded for reconciliation", paymentRef);
                }
            } else {
                log.debug("Callback payload has no paymentRef/action, recorded as informational");
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.debug("Callback payload is not JSON, recorded as raw event");
        }
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> listPaymentsByStatus(String status, int page, int size) {
        return listPayments(status, null, null, page, size);
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> listPayments(String status, String from, String to, int page, int size) {
        log.debug("Listing payments status={}, from={}, to={}, page={}, size={}", status, from, to, page, size);
        int offset = page * size;
        List<Payment> payments = paymentMapper.findFiltered(status, from, to, offset, size);
        int total = paymentMapper.countFiltered(status, from, to);
        return new PageResult<>(payments, page, size, total);
    }

    @Transactional(readOnly = true)
    public byte[] generateReconciliationExport(LocalDate from, LocalDate to) {
        log.info("Generating reconciliation export from {} to {}", from, to);

        List<LedgerEntry> entries = ledgerEntryMapper.findByDateRange(from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("id,payment_id,account_id,entry_type,amount,balance_after,description,reference_type,reference_id,created_at\n");

        BigDecimal totalDebit = BigDecimal.ZERO;
        BigDecimal totalCredit = BigDecimal.ZERO;
        BigDecimal totalAdjustments = BigDecimal.ZERO;
        int adjustmentCount = 0;
        int paymentCount = 0;
        int refundCount = 0;

        for (LedgerEntry entry : entries) {
            csv.append(entry.getId()).append(',')
                    .append(entry.getPaymentId()).append(',')
                    .append(entry.getAccountId()).append(',')
                    .append(entry.getEntryType()).append(',')
                    .append(entry.getAmount()).append(',')
                    .append(entry.getBalanceAfter()).append(',')
                    .append(escapeCsv(entry.getDescription())).append(',')
                    .append(entry.getReferenceType()).append(',')
                    .append(entry.getReferenceId()).append(',')
                    .append(entry.getCreatedAt()).append('\n');

            if (entry.getEntryType() == LedgerEntryType.DEBIT) {
                totalDebit = totalDebit.add(entry.getAmount());
            } else {
                totalCredit = totalCredit.add(entry.getAmount());
            }

            // Track adjustments vs payments vs refunds
            String refType = entry.getReferenceType();
            if ("ADJUSTMENT".equalsIgnoreCase(refType)) {
                adjustmentCount++;
                totalAdjustments = totalAdjustments.add(entry.getAmount());
            } else if ("REFUND".equalsIgnoreCase(refType) || (entry.getDescription() != null && entry.getDescription().toLowerCase().contains("refund"))) {
                refundCount++;
            } else {
                paymentCount++;
            }
        }

        csv.append('\n');
        csv.append("SUMMARY\n");
        csv.append("Period,").append(from).append(" to ").append(to).append('\n');
        csv.append("Total Entries,").append(entries.size()).append('\n');
        csv.append("Total Payments,").append(paymentCount).append('\n');
        csv.append("Total Refunds,").append(refundCount).append('\n');
        csv.append("Total Adjustments,").append(adjustmentCount).append('\n');
        csv.append("Adjustment Amount,").append(totalAdjustments).append('\n');
        csv.append("Total Debits,").append(totalDebit).append('\n');
        csv.append("Total Credits,").append(totalCredit).append('\n');
        csv.append("Net,").append(totalCredit.subtract(totalDebit)).append('\n');

        log.info("Reconciliation export generated: {} entries", entries.size());
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getLedgerTrail(Long paymentId) {
        log.debug("Fetching ledger trail for paymentId={}", paymentId);
        return ledgerEntryMapper.findByPaymentId(paymentId);
    }

    @Transactional(readOnly = true)
    public PageResult<LedgerEntry> getLedgerByAccount(Long accountId, int page, int size) {
        log.debug("Fetching ledger for accountId={}, page={}, size={}", accountId, page, size);
        int offset = page * size;
        List<LedgerEntry> entries = ledgerEntryMapper.findByAccountId(accountId, offset, size);
        long total = ledgerEntryMapper.countByAccountId(accountId);
        return new PageResult<>(entries, page, size, total);
    }

    @Transactional(readOnly = true)
    public BigDecimal getBalance(Long accountId) {
        log.debug("Fetching balance for accountId={}", accountId);
        BigDecimal balance = ledgerEntryMapper.calculateBalance(accountId);
        return balance != null ? balance : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public PageResult<Payment> listPendingSettlements(int page, int size) {
        log.debug("Listing pending settlements: page={}, size={}", page, size);

        int offset = page * size;
        String status = PaymentStatus.PENDING_SETTLEMENT.name();
        List<Payment> payments = paymentMapper.findByStatus(status, offset, size);
        int total = paymentMapper.countByStatus(status);

        return new PageResult<>(payments, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResult<ReconciliationItem> listReconciliationItems(String status, int page, int size) {
        log.debug("Listing reconciliation items: status={}, page={}, size={}", status, page, size);

        int offset = page * size;
        List<ReconciliationItem> items = reconciliationItemMapper.findByStatus(status, offset, size);
        long total = reconciliationItemMapper.countByStatus(status);

        return new PageResult<>(items, page, size, total);
    }

    public ReconciliationItem resolveReconciliation(Long id, String status, String note, Long resolvedBy) {
        log.info("Resolving reconciliation item id={} with status={}", id, status);

        ReconciliationItem item = reconciliationItemMapper.findById(id);
        if (item == null) {
            throw new ResourceNotFoundException(
                    "Reconciliation item not found with id: " + id, "ReconciliationItem", id);
        }

        reconciliationItemMapper.resolve(id, status, resolvedBy, note, LocalDateTime.now());

        item.setStatus(com.dispatchops.domain.model.enums.ReconStatus.valueOf(status));
        item.setResolvedBy(resolvedBy);
        item.setResolutionNote(note);
        item.setResolvedAt(LocalDateTime.now());

        log.info("Reconciliation item id={} resolved with status={}", id, status);
        return item;
    }

    @Transactional(readOnly = true)
    public Payment getPayment(Long id) {
        log.debug("Fetching payment with id={}", id);
        Payment payment = paymentMapper.findById(id);
        if (payment == null) {
            throw new ResourceNotFoundException("Payment not found with id: " + id, "Payment", id);
        }
        return payment;
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByJob(Long jobId) {
        log.debug("Fetching payments for jobId={}", jobId);
        return paymentMapper.findByJobId(jobId);
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
