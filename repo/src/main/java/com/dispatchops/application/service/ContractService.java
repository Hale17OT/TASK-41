package com.dispatchops.application.service;

import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.ContractInstance;
import com.dispatchops.domain.model.ContractTemplate;
import com.dispatchops.domain.model.ContractTemplateVersion;
import com.dispatchops.domain.model.SigningRecord;
import com.dispatchops.domain.model.enums.ContractStatus;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.infrastructure.persistence.mapper.ContractInstanceMapper;
import com.dispatchops.infrastructure.persistence.mapper.ContractTemplateMapper;
import com.dispatchops.infrastructure.persistence.mapper.ContractTemplateVersionMapper;
import com.dispatchops.infrastructure.persistence.mapper.SigningRecordMapper;
import com.dispatchops.infrastructure.security.HmacUtil;
import com.dispatchops.web.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);
    private final SearchService searchService;

    private final ContractTemplateMapper contractTemplateMapper;
    private final ContractTemplateVersionMapper contractTemplateVersionMapper;
    private final ContractInstanceMapper contractInstanceMapper;
    private final SigningRecordMapper signingRecordMapper;
    private final NotificationService notificationService;
    private final String hmacKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ContractService(ContractTemplateMapper contractTemplateMapper,
                           ContractTemplateVersionMapper contractTemplateVersionMapper,
                           ContractInstanceMapper contractInstanceMapper,
                           SigningRecordMapper signingRecordMapper,
                           NotificationService notificationService,
                           SearchService searchService,
                           @Value("${security.hmac.key}") String hmacKey) {
        this.contractTemplateMapper = contractTemplateMapper;
        this.contractTemplateVersionMapper = contractTemplateVersionMapper;
        this.contractInstanceMapper = contractInstanceMapper;
        this.signingRecordMapper = signingRecordMapper;
        this.notificationService = notificationService;
        this.searchService = searchService;
        this.hmacKey = hmacKey;
    }

    public ContractTemplate createTemplate(String name, String desc, String body, Long createdBy) {
        log.info("Creating contract template '{}' by userId={}", name, createdBy);

        ContractTemplate template = new ContractTemplate();
        template.setName(name);
        template.setDescription(desc);
        template.setActive(true);
        template.setCreatedBy(createdBy);
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());

        contractTemplateMapper.insert(template);
        log.info("Contract template created with id={}", template.getId());

        ContractTemplateVersion version = new ContractTemplateVersion();
        version.setTemplateId(template.getId());
        version.setVersionNumber(1);
        version.setBodyText(body);
        version.setPlaceholderSchema(extractPlaceholderSchema(body));
        version.setCreatedBy(createdBy);
        version.setCreatedAt(LocalDateTime.now());

        contractTemplateVersionMapper.insert(version);
        log.info("Initial version 1 created for template id={}", template.getId());

        try { searchService.indexEntity("CONTRACT", template.getId(), template.getName(), template.getDescription(), "", createdBy); }
        catch (Exception e) { log.warn("Failed to index contract template: {}", e.getMessage()); }

        return template;
    }

    public ContractTemplateVersion createNewVersion(Long templateId, String body, Long createdBy) {
        log.info("Creating new version for templateId={} by userId={}", templateId, createdBy);

        ContractTemplate template = contractTemplateMapper.findById(templateId);
        if (template == null) {
            throw new ResourceNotFoundException(
                    "Contract template not found with id: " + templateId, "ContractTemplate", templateId);
        }

        ContractTemplateVersion latest = contractTemplateVersionMapper.findLatestVersion(templateId);
        int nextVersionNumber = (latest != null) ? latest.getVersionNumber() + 1 : 1;

        ContractTemplateVersion version = new ContractTemplateVersion();
        version.setTemplateId(templateId);
        version.setVersionNumber(nextVersionNumber);
        version.setBodyText(body);
        version.setPlaceholderSchema(extractPlaceholderSchema(body));
        version.setCreatedBy(createdBy);
        version.setCreatedAt(LocalDateTime.now());

        contractTemplateVersionMapper.insert(version);
        log.info("Version {} created for template id={}", nextVersionNumber, templateId);

        return version;
    }

    /** Extract {{placeholder}} tokens from template body and return as JSON array. */
    private String extractPlaceholderSchema(String body) {
        List<Map<String, String>> schema = new ArrayList<>();
        if (body != null) {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\{\\{(\\w+)\\}\\}").matcher(body);
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            while (matcher.find()) {
                seen.add(matcher.group(1));
            }
            for (String key : seen) {
                Map<String, String> entry = new HashMap<>();
                entry.put("key", key);
                entry.put("label", key.replace("_", " "));
                entry.put("required", "true");
                schema.add(entry);
            }
        }
        try {
            return objectMapper.writeValueAsString(schema);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public ContractInstance generateDocument(Long templateVersionId, Map<String, String> placeholderValues,
                                              List<Long> signerIds, Long jobId, Long generatedBy) {
        log.info("Generating contract document from templateVersionId={} by userId={}", templateVersionId, generatedBy);

        ContractTemplateVersion templateVersion = contractTemplateVersionMapper.findById(templateVersionId);
        if (templateVersion == null) {
            throw new ResourceNotFoundException(
                    "Contract template version not found with id: " + templateVersionId,
                    "ContractTemplateVersion", templateVersionId);
        }

        String snapshotBodyText = templateVersion.getBodyText();
        String renderedText = snapshotBodyText;

        if (placeholderValues != null) {
            for (Map.Entry<String, String> entry : placeholderValues.entrySet()) {
                renderedText = renderedText.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }

        ContractStatus initialStatus;
        if (signerIds != null && !signerIds.isEmpty()) {
            initialStatus = ContractStatus.PENDING_SIGNATURE;
        } else {
            initialStatus = ContractStatus.DRAFT;
        }

        ContractInstance instance = new ContractInstance();
        instance.setTemplateVersionId(templateVersionId);
        instance.setSnapshotBodyText(snapshotBodyText);
        instance.setRenderedText(renderedText);
        try {
            Map<String, Object> jsonValues = new HashMap<>();
            if (placeholderValues != null) jsonValues.put("placeholders", placeholderValues);
            if (signerIds != null) jsonValues.put("signerIds", signerIds);
            instance.setPlaceholderValues(objectMapper.writeValueAsString(jsonValues));
        } catch (JsonProcessingException e) {
            instance.setPlaceholderValues("{}");
        }
        instance.setJobId(jobId);
        instance.setGeneratedBy(generatedBy);
        instance.setStatus(initialStatus);
        instance.setCreatedAt(LocalDateTime.now());

        contractInstanceMapper.insert(instance);
        log.info("Contract instance created with id={}, status={}", instance.getId(), initialStatus);

        if (signerIds != null && !signerIds.isEmpty()) {
            notificationService.notify(
                    signerIds.get(0),
                    NotificationType.SYSTEM,
                    "Contract Awaiting Signature",
                    "A contract is waiting for your signature. You are signer #1.",
                    "ContractInstance",
                    instance.getId());
        }

        try { searchService.indexEntity("CONTRACT", instance.getId(), "Contract #" + instance.getId(),
                instance.getRenderedText() != null ? instance.getRenderedText().substring(0, Math.min(200, instance.getRenderedText().length())) : "",
                instance.getStatus().name(), generatedBy); }
        catch (Exception e) { log.warn("Failed to index contract instance: {}", e.getMessage()); }

        return instance;
    }

    public SigningRecord recordSignature(Long contractInstanceId, Long signerId,
                                          String signatureData, String ipAddress) {
        log.info("Recording signature for contractInstanceId={} by signerId={}", contractInstanceId, signerId);

        ContractInstance instance = contractInstanceMapper.findById(contractInstanceId);
        if (instance == null) {
            throw new ResourceNotFoundException(
                    "Contract instance not found with id: " + contractInstanceId,
                    "ContractInstance", contractInstanceId);
        }

        if (instance.getStatus() != ContractStatus.PENDING_SIGNATURE
                && instance.getStatus() != ContractStatus.PARTIALLY_SIGNED) {
            throw new IllegalStateException(
                    "Cannot sign contract in status " + instance.getStatus()
                            + ". Must be PENDING_SIGNATURE or PARTIALLY_SIGNED.");
        }

        // Parse stored signer plan from placeholderValues JSON
        List<Long> expectedSignerIds = getExpectedSignerIds(instance);

        List<SigningRecord> existingRecords = signingRecordMapper.findByContractInstanceId(contractInstanceId);
        int nextOrder = existingRecords.size() + 1;

        // Enforce signer order: check if this signer is the next expected signer
        if (!expectedSignerIds.isEmpty()) {
            if (nextOrder > expectedSignerIds.size()) {
                throw new IllegalStateException("All expected signers have already signed this contract.");
            }
            Long expectedSignerId = expectedSignerIds.get(nextOrder - 1);
            if (!expectedSignerId.equals(signerId)) {
                throw new com.dispatchops.domain.exception.PermissionDeniedException(
                        "Signer " + signerId + " is not the next expected signer. Expected signer ID: " + expectedSignerId);
            }
        }

        // Check signer hasn't already signed
        for (SigningRecord existing : existingRecords) {
            if (existing.getSignerId().equals(signerId)) {
                throw new IllegalStateException("Signer " + signerId + " has already signed this contract.");
            }
        }

        String documentHash = HmacUtil.computeHmac(instance.getRenderedText() + signatureData, hmacKey);

        SigningRecord record = new SigningRecord();
        record.setContractInstanceId(contractInstanceId);
        record.setSignerId(signerId);
        record.setSignerOrder(nextOrder);
        record.setSignatureData(signatureData);
        record.setDocumentHash(documentHash);
        record.setSignedAt(LocalDateTime.now());
        record.setIpAddress(ipAddress);

        signingRecordMapper.insert(record);
        log.info("Signing record id={} created, signer order={}", record.getId(), nextOrder);

        int totalSigned = existingRecords.size() + 1;
        int totalExpected = expectedSignerIds.isEmpty() ? 1 : expectedSignerIds.size();

        if (totalSigned >= totalExpected) {
            contractInstanceMapper.updateStatus(contractInstanceId, ContractStatus.FULLY_SIGNED.name());
            log.info("Contract {} is now FULLY_SIGNED ({}/{} signatures)", contractInstanceId, totalSigned, totalExpected);
        } else {
            contractInstanceMapper.updateStatus(contractInstanceId, ContractStatus.PARTIALLY_SIGNED.name());
            // Notify next signer
            Long nextSignerId = expectedSignerIds.get(totalSigned);
            notificationService.notify(
                    nextSignerId,
                    NotificationType.SYSTEM,
                    "Contract Awaiting Your Signature",
                    "A contract is waiting for your signature. You are signer #" + (totalSigned + 1) + ".",
                    "ContractInstance",
                    contractInstanceId);
            log.info("Contract {} is PARTIALLY_SIGNED ({}/{} signatures), notified next signer {}",
                    contractInstanceId, totalSigned, totalExpected, nextSignerId);
        }

        // Reindex contract after signature
        try { String status = totalSigned >= totalExpected ? "FULLY_SIGNED" : "PARTIALLY_SIGNED";
            searchService.indexEntity("CONTRACT", contractInstanceId, "Contract #" + contractInstanceId, "", status, signerId); }
        catch (Exception e) { log.warn("Failed to reindex contract after signing: {}", e.getMessage()); }

        return record;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> verifyIntegrity(Long contractInstanceId) {
        log.info("Verifying integrity for contractInstanceId={}", contractInstanceId);

        ContractInstance instance = contractInstanceMapper.findById(contractInstanceId);
        if (instance == null) {
            throw new ResourceNotFoundException(
                    "Contract instance not found with id: " + contractInstanceId,
                    "ContractInstance", contractInstanceId);
        }

        List<SigningRecord> records = signingRecordMapper.findByContractInstanceId(contractInstanceId);

        boolean allValid = true;
        List<Map<String, Object>> details = new ArrayList<>();

        for (SigningRecord record : records) {
            String recomputed = HmacUtil.computeHmac(
                    instance.getRenderedText() + record.getSignatureData(), hmacKey);
            boolean matches = java.security.MessageDigest.isEqual(
                    recomputed.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                    record.getDocumentHash().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            Map<String, Object> detail = new HashMap<>();
            detail.put("signingRecordId", record.getId());
            detail.put("signerId", record.getSignerId());
            detail.put("signerOrder", record.getSignerOrder());
            detail.put("valid", matches);
            if (!matches) {
                detail.put("reason", "HMAC mismatch: document may have been tampered with");
                allValid = false;
            }
            details.add(detail);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("valid", allValid);
        result.put("details", details);
        result.put("totalRecords", records.size());

        log.info("Integrity verification for contract {}: valid={}", contractInstanceId, allValid);
        return result;
    }

    @Transactional(readOnly = true)
    public PageResult<ContractTemplate> listTemplates(int page, int size) {
        log.debug("Listing contract templates: page={}, size={}", page, size);

        int offset = page * size;
        List<ContractTemplate> templates = contractTemplateMapper.findAll(offset, size);
        long total = contractTemplateMapper.countAll();

        return new PageResult<>(templates, page, size, total);
    }

    @Transactional(readOnly = true)
    public List<ContractTemplateVersion> getTemplateVersions(Long templateId) {
        log.debug("Fetching versions for templateId={}", templateId);
        return contractTemplateVersionMapper.findByTemplateId(templateId);
    }

    @Transactional(readOnly = true)
    public ContractInstance getInstance(Long id) {
        log.debug("Fetching contract instance with id={}", id);
        ContractInstance instance = contractInstanceMapper.findById(id);
        if (instance == null) {
            throw new ResourceNotFoundException(
                    "Contract instance not found with id: " + id, "ContractInstance", id);
        }
        return instance;
    }

    @Transactional(readOnly = true)
    public List<SigningRecord> getSigningRecords(Long contractInstanceId) {
        log.debug("Fetching signing records for contractInstanceId={}", contractInstanceId);
        return signingRecordMapper.findByContractInstanceId(contractInstanceId);
    }

    /**
     * Check if a user is a designated signer on a contract instance.
     * Uses parsed signer plan from JSON (exact ID match) OR existing signing records.
     */
    @Transactional(readOnly = true)
    public boolean isDesignatedSigner(ContractInstance instance, Long userId) {
        // Check signer plan from stored JSON (exact match, not string contains)
        List<Long> expectedSigners = getExpectedSignerIds(instance);
        if (expectedSigners.contains(userId)) {
            return true;
        }
        // Also check if already signed
        List<SigningRecord> records = signingRecordMapper.findByContractInstanceId(instance.getId());
        return records.stream().anyMatch(r -> userId.equals(r.getSignerId()));
    }

    public void voidContract(Long id) {
        log.info("Voiding contract instance id={}", id);

        ContractInstance instance = contractInstanceMapper.findById(id);
        if (instance == null) {
            throw new ResourceNotFoundException(
                    "Contract instance not found with id: " + id, "ContractInstance", id);
        }

        contractInstanceMapper.updateStatus(id, ContractStatus.VOIDED.name());
        log.info("Contract instance id={} voided", id);
        try { searchService.deindexEntity("CONTRACT", id); }
        catch (Exception e) { log.warn("Failed to deindex voided contract: {}", e.getMessage()); }
    }

    /** Parse expected signer IDs from the stored JSON in placeholderValues. */
    @SuppressWarnings("unchecked")
    private List<Long> getExpectedSignerIds(ContractInstance instance) {
        if (instance.getPlaceholderValues() == null || instance.getPlaceholderValues().isBlank()) {
            return List.of();
        }
        try {
            Map<String, Object> parsed = objectMapper.readValue(instance.getPlaceholderValues(),
                    new TypeReference<Map<String, Object>>() {});
            Object signerIdsObj = parsed.get("signerIds");
            if (signerIdsObj instanceof List<?> rawList) {
                List<Long> result = new ArrayList<>();
                for (Object item : rawList) {
                    if (item instanceof Number num) {
                        result.add(num.longValue());
                    } else if (item instanceof String s) {
                        result.add(Long.parseLong(s));
                    }
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Failed to parse signerIds from contract instance {}: {}", instance.getId(), e.getMessage());
        }
        return List.of();
    }
}
