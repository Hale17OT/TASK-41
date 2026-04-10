package com.dispatchops.application.service;

import com.dispatchops.domain.model.RegionRule;
import com.dispatchops.domain.model.ShippingTemplate;
import com.dispatchops.infrastructure.persistence.mapper.RegionRuleMapper;
import com.dispatchops.infrastructure.persistence.mapper.ShippingTemplateMapper;
import com.dispatchops.web.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class ShippingRuleService {

    private static final Logger log = LoggerFactory.getLogger(ShippingRuleService.class);

    private final ShippingTemplateMapper shippingTemplateMapper;
    private final RegionRuleMapper regionRuleMapper;

    public ShippingRuleService(ShippingTemplateMapper shippingTemplateMapper,
                               RegionRuleMapper regionRuleMapper) {
        this.shippingTemplateMapper = shippingTemplateMapper;
        this.regionRuleMapper = regionRuleMapper;
    }

    @Transactional(readOnly = true)
    public ValidationResult validateAddress(String state, String zip,
                                            BigDecimal weightLbs, BigDecimal orderAmount) {
        log.debug("Validating address: state={}, zip={}, weight={}, amount={}",
                state, zip, weightLbs, orderAmount);

        List<RegionRule> matchingRules = regionRuleMapper.findMatchingRules(state, zip, weightLbs, orderAmount);

        if (matchingRules == null || matchingRules.isEmpty()) {
            log.warn("No shipping rules match address: state={}, zip={}", state, zip);
            return new ValidationResult(false, "No shipping rules match this address");
        }

        // Rules are returned ordered by priority DESC; first match wins
        for (RegionRule rule : matchingRules) {
            if (!rule.isAllowed()) {
                log.info("Address denied by rule id={} (priority={}): state={}, zip={}",
                        rule.getId(), rule.getPriority(), state, zip);
                return new ValidationResult(false,
                        "Shipping to this address is not allowed by rule: " + rule.getId());
            }

            // Check weight constraint
            if (rule.getMaxWeightLbs() != null && weightLbs.compareTo(rule.getMaxWeightLbs()) > 0) {
                log.info("Weight {} exceeds max {} for rule id={}",
                        weightLbs, rule.getMaxWeightLbs(), rule.getId());
                return new ValidationResult(false,
                        "Package weight " + weightLbs + " lbs exceeds maximum allowed "
                                + rule.getMaxWeightLbs() + " lbs");
            }

            // Check order amount constraints
            if (rule.getMinOrderAmount() != null && orderAmount.compareTo(rule.getMinOrderAmount()) < 0) {
                log.info("Order amount {} below minimum {} for rule id={}",
                        orderAmount, rule.getMinOrderAmount(), rule.getId());
                return new ValidationResult(false,
                        "Order amount $" + orderAmount + " is below the minimum required $"
                                + rule.getMinOrderAmount());
            }

            if (rule.getMaxOrderAmount() != null && orderAmount.compareTo(rule.getMaxOrderAmount()) > 0) {
                log.info("Order amount {} exceeds maximum {} for rule id={}",
                        orderAmount, rule.getMaxOrderAmount(), rule.getId());
                return new ValidationResult(false,
                        "Order amount $" + orderAmount + " exceeds the maximum allowed $"
                                + rule.getMaxOrderAmount());
            }

            // First matching allowed rule passes all checks
            log.debug("Address validated successfully by rule id={}", rule.getId());
            return new ValidationResult(true, null);
        }

        // Should not reach here, but defensive
        return new ValidationResult(false, "No shipping rules match this address");
    }

    @Transactional(readOnly = true)
    public PageResult<ShippingTemplate> listTemplates(int page, int size) {
        log.debug("Listing shipping templates: page={}, size={}", page, size);

        int offset = page * size;
        List<ShippingTemplate> templates = shippingTemplateMapper.findAll(offset, size);
        long total = shippingTemplateMapper.countAll();

        return new PageResult<>(templates, page, size, total);
    }

    public ShippingTemplate createTemplate(ShippingTemplate t) {
        log.info("Creating shipping template: name='{}'", t.getName());

        t.setCreatedAt(LocalDateTime.now());
        t.setUpdatedAt(LocalDateTime.now());
        shippingTemplateMapper.insert(t);

        log.info("Shipping template created with id={}", t.getId());
        return t;
    }

    public ShippingTemplate updateTemplate(ShippingTemplate t) {
        log.info("Updating shipping template id={}", t.getId());

        t.setUpdatedAt(LocalDateTime.now());
        shippingTemplateMapper.update(t);

        log.info("Shipping template id={} updated", t.getId());
        return t;
    }

    @Transactional(readOnly = true)
    public List<RegionRule> listRulesByTemplate(Long templateId) {
        log.debug("Listing region rules for templateId={}", templateId);
        return regionRuleMapper.findByTemplateId(templateId);
    }

    public RegionRule createRule(RegionRule r) {
        log.info("Creating region rule for templateId={}, stateCode={}", r.getTemplateId(), r.getStateCode());

        r.setCreatedAt(LocalDateTime.now());
        regionRuleMapper.insert(r);

        log.info("Region rule created with id={}", r.getId());
        return r;
    }

    public RegionRule updateRule(RegionRule r) {
        log.info("Updating region rule id={}", r.getId());

        regionRuleMapper.update(r);

        log.info("Region rule id={} updated", r.getId());
        return r;
    }

    public void deleteRule(Long id) {
        log.info("Deleting region rule id={}", id);

        regionRuleMapper.delete(id);

        log.info("Region rule id={} deleted", id);
    }

    public static class ValidationResult {

        private final boolean valid;
        private final String reason;

        public ValidationResult(boolean valid, String reason) {
            this.valid = valid;
            this.reason = reason;
        }

        public boolean isValid() {
            return valid;
        }

        public String getReason() {
            return reason;
        }
    }
}
