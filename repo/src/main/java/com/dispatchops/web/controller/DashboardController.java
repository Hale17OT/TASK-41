package com.dispatchops.web.controller;

import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.infrastructure.persistence.mapper.DeliveryJobMapper;
import com.dispatchops.web.dto.ApiResult;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private static final Logger log = LoggerFactory.getLogger(DashboardController.class);

    private final com.dispatchops.application.service.DeliveryJobService deliveryJobService;
    private final com.dispatchops.application.service.PaymentService paymentService;
    private final com.dispatchops.application.service.CredibilityService credibilityService;
    private final DeliveryJobMapper deliveryJobMapper;

    public DashboardController(com.dispatchops.application.service.DeliveryJobService deliveryJobService,
                               com.dispatchops.application.service.PaymentService paymentService,
                               com.dispatchops.application.service.CredibilityService credibilityService,
                               DeliveryJobMapper deliveryJobMapper) {
        this.deliveryJobService = deliveryJobService;
        this.paymentService = paymentService;
        this.credibilityService = credibilityService;
        this.deliveryJobMapper = deliveryJobMapper;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResult<Map<String, Object>>> getDashboardMetrics(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting dashboard metrics for user '{}' with role {}", currentUser.getUsername(), currentUser.getRole());

        Map<String, Object> metrics = new HashMap<>();
        Role role = currentUser.getRole();

        if (role == Role.DISPATCHER) {
            metrics.put("activeJobs", deliveryJobMapper.countAll());
            metrics.put("exceptionsToday", deliveryJobMapper.countByStatus("EXCEPTION"));

        } else if (role == Role.AUDITOR) {
            metrics.put("pendingSettlements", paymentService.listPendingSettlements(0, 1).getTotalElements());

        } else if (role == Role.COURIER) {
            metrics.put("creditLevel", credibilityService.getCourierCredibility(currentUser.getId()) != null
                    ? credibilityService.getCourierCredibility(currentUser.getId()).getLevel().name() : "D");

        } else if (role == Role.OPS_MANAGER || role == Role.ADMIN) {
            metrics.put("totalJobs", deliveryJobMapper.countAll());
            metrics.put("pendingSettlements", paymentService.listPendingSettlements(0, 1).getTotalElements());
            metrics.put("exceptionsToday", deliveryJobMapper.countByStatus("EXCEPTION"));
            metrics.put("idleJobs", deliveryJobService.getIdleJobs(20).size());
        }

        metrics.put("role", role.name());
        return ResponseEntity.ok(ApiResult.success(metrics));
    }

    @GetMapping("/activity")
    public ResponseEntity<ApiResult<List<Map<String, Object>>>> getRecentActivity(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting recent activity for user '{}'", currentUser.getUsername());

        List<Map<String, Object>> activities = new ArrayList<>();

        try {
            List<com.dispatchops.domain.model.DeliveryJob> recentJobs;

            if (currentUser.getRole() == Role.COURIER) {
                // Couriers only see activity for their own assigned jobs
                recentJobs = deliveryJobService.listJobsByCourier(currentUser.getId(), 0, 20);
            } else {
                var result = deliveryJobService.listJobs(null, 0, 20);
                recentJobs = result != null ? result.getContent() : java.util.Collections.emptyList();
            }

            if (recentJobs != null) {
                for (var dj : recentJobs) {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("timestamp", dj.getLastEventAt() != null ? dj.getLastEventAt().toString() : dj.getCreatedAt().toString());
                    entry.put("message", "Job " + dj.getTrackingNumber() + " status: " + dj.getStatus());
                    activities.add(entry);
                    if (activities.size() >= 20) break;
                }
            }
        } catch (Exception e) {
            log.warn("Unable to fetch recent job activity: {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResult.success(activities));
    }
}
