package com.dispatchops.web.controller;

import com.dispatchops.application.service.ShippingRuleService;
import com.dispatchops.domain.model.RegionRule;
import com.dispatchops.domain.model.ShippingTemplate;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.domain.model.User;
import com.dispatchops.web.dto.PageResult;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shipping")
public class ShippingRuleController {

    private static final Logger log = LoggerFactory.getLogger(ShippingRuleController.class);

    private final ShippingRuleService shippingRuleService;

    public ShippingRuleController(ShippingRuleService shippingRuleService) {
        this.shippingRuleService = shippingRuleService;
    }

    @GetMapping("/templates")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<PageResult<ShippingTemplate>>> listTemplates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing shipping templates - page: {}, size: {}", page, size);
        PageResult<ShippingTemplate> result = shippingRuleService.listTemplates(page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PostMapping("/templates")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<ShippingTemplate>> createTemplate(
            @RequestBody ShippingTemplate template,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        template.setCreatedBy(currentUser.getId());
        log.info("Creating shipping template: {} by user {}", template.getName(), currentUser.getUsername());
        ShippingTemplate created = shippingRuleService.createTemplate(template);
        return ResponseEntity.status(201).body(ApiResult.success(created));
    }

    @PutMapping("/templates/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<ShippingTemplate>> updateTemplate(
            @PathVariable Long id,
            @RequestBody ShippingTemplate template) {
        log.info("Updating shipping template {}", id);
        template.setId(id);
        ShippingTemplate updated = shippingRuleService.updateTemplate(template);
        return ResponseEntity.ok(ApiResult.success(updated));
    }

    @GetMapping("/templates/{id}/rules")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<List<RegionRule>>> listRules(@PathVariable Long id) {
        log.debug("Listing rules for shipping template {}", id);
        List<RegionRule> rules = shippingRuleService.listRulesByTemplate(id);
        return ResponseEntity.ok(ApiResult.success(rules));
    }

    @PostMapping("/templates/{id}/rules")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<RegionRule>> createRule(
            @PathVariable Long id,
            @RequestBody RegionRule rule) {
        log.info("Creating rule for shipping template {}", id);
        rule.setTemplateId(id);
        RegionRule created = shippingRuleService.createRule(rule);
        return ResponseEntity.status(201).body(ApiResult.success(created));
    }

    @PutMapping("/rules/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<RegionRule>> updateRule(
            @PathVariable Long id,
            @RequestBody RegionRule rule) {
        log.info("Updating region rule {}", id);
        rule.setId(id);
        RegionRule updated = shippingRuleService.updateRule(rule);
        return ResponseEntity.ok(ApiResult.success(updated));
    }

    @DeleteMapping("/rules/{id}")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<Void>> deleteRule(@PathVariable Long id) {
        log.info("Deleting region rule {}", id);
        shippingRuleService.deleteRule(id);
        return ResponseEntity.ok(ApiResult.success());
    }

    @PostMapping("/validate")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<ShippingRuleService.ValidationResult>> validateAddress(
            @jakarta.validation.Valid @RequestBody com.dispatchops.web.dto.ShippingValidateDTO dto) {
        log.debug("Validating address - state: {}, zip: {}, weight: {}, amount: {}",
                dto.getState(), dto.getZip(), dto.getWeightLbs(), dto.getOrderAmount());
        ShippingRuleService.ValidationResult result =
                shippingRuleService.validateAddress(dto.getState(), dto.getZip(), dto.getWeightLbs(), dto.getOrderAmount());
        return ResponseEntity.ok(ApiResult.success(result));
    }
}
