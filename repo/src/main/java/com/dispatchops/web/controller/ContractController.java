package com.dispatchops.web.controller;

import com.dispatchops.application.service.ContractService;
import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.model.ContractInstance;
import com.dispatchops.domain.model.ContractTemplate;
import com.dispatchops.domain.model.ContractTemplateVersion;
import com.dispatchops.domain.model.SigningRecord;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.ContractGenerateDTO;
import com.dispatchops.web.dto.PageResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private static final Logger log = LoggerFactory.getLogger(ContractController.class);

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping("/templates")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<PageResult<ContractTemplate>>> listTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing contract templates - page: {}, size: {}", page, size);
        PageResult<ContractTemplate> result = contractService.listTemplates(page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PostMapping("/templates")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<ContractTemplate>> createTemplate(
            @Valid @RequestBody com.dispatchops.web.dto.ContractTemplateCreateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Admin '{}' creating contract template: {}", currentUser.getUsername(), dto.getName());
        ContractTemplate created = contractService.createTemplate(
                dto.getName(), dto.getDescription(), dto.getBody(), currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(created));
    }

    @PostMapping("/templates/{id}/versions")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<ContractTemplateVersion>> createNewVersion(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        String versionBody = body.get("body");
        log.info("Admin '{}' creating new version for template {}", currentUser.getUsername(), id);
        ContractTemplateVersion version = contractService.createNewVersion(id, versionBody, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(version));
    }

    @GetMapping("/templates/{id}/versions")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.AUDITOR})
    public ResponseEntity<ApiResult<List<ContractTemplateVersion>>> getVersions(@PathVariable Long id) {
        log.debug("Getting versions for template {}", id);
        List<ContractTemplateVersion> versions = contractService.getTemplateVersions(id);
        return ResponseEntity.ok(ApiResult.success(versions));
    }

    @PostMapping("/instances")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<ContractInstance>> generateDocument(
            @Valid @RequestBody ContractGenerateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' generating contract document from version {}",
                currentUser.getUsername(), dto.getTemplateVersionId());
        ContractInstance instance = contractService.generateDocument(
                dto.getTemplateVersionId(), dto.getPlaceholderValues(),
                dto.getSignerIds(), dto.getJobId(), currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(instance));
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<ApiResult<ContractInstance>> getInstance(
            @PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting contract instance {}", id);
        ContractInstance instance = contractService.getInstance(id);

        // Object-level auth: COURIER can only see contracts they are a designated signer on
        if (currentUser.getRole() == Role.COURIER) {
            boolean isDesignatedSigner = contractService.isDesignatedSigner(instance, currentUser.getId());
            if (!isDesignatedSigner) {
                throw new PermissionDeniedException(
                        "Courier " + currentUser.getId() + " is not a designated signer on contract instance " + id);
            }
        }

        return ResponseEntity.ok(ApiResult.success(instance));
    }

    @PostMapping("/instances/{id}/sign")
    @RequireRole({Role.COURIER, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<SigningRecord>> recordSignature(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session,
            HttpServletRequest request) {
        User currentUser = (User) session.getAttribute("currentUser");
        String signatureData = body.get("signatureData");
        String ipAddress = request.getRemoteAddr();
        log.info("User '{}' signing contract instance {} from IP {}", currentUser.getUsername(), id, ipAddress);
        SigningRecord record = contractService.recordSignature(id, currentUser.getId(), signatureData, ipAddress);
        return ResponseEntity.status(201).body(ApiResult.success(record));
    }

    @GetMapping("/instances/{id}/verify")
    @RequireRole({Role.AUDITOR, Role.ADMIN})
    public ResponseEntity<ApiResult<Map<String, Object>>> verifyIntegrity(@PathVariable Long id) {
        log.debug("Verifying integrity of contract instance {}", id);
        Map<String, Object> result = contractService.verifyIntegrity(id);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PutMapping("/instances/{id}/void")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<Void>> voidContract(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Admin '{}' voiding contract instance {}", currentUser.getUsername(), id);
        contractService.voidContract(id);
        return ResponseEntity.ok(ApiResult.success());
    }

    @GetMapping("/instances/{id}/signatures")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.AUDITOR})
    public ResponseEntity<ApiResult<List<Map<String, Object>>>> getSigningRecords(@PathVariable Long id) {
        log.debug("Getting signing records for contract instance {}", id);
        List<SigningRecord> records = contractService.getSigningRecords(id);
        // Scrub sensitive fields: remove raw signatureData and documentHash from API response
        List<Map<String, Object>> sanitized = new java.util.ArrayList<>();
        for (SigningRecord r : records) {
            Map<String, Object> safe = new java.util.LinkedHashMap<>();
            safe.put("id", r.getId());
            safe.put("contractInstanceId", r.getContractInstanceId());
            safe.put("signerId", r.getSignerId());
            safe.put("signerOrder", r.getSignerOrder());
            safe.put("signedAt", r.getSignedAt());
            // Omit signatureData, documentHash, ipAddress
            sanitized.add(safe);
        }
        return ResponseEntity.ok(ApiResult.success(sanitized));
    }
}
