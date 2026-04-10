package com.dispatchops.web.controller;

import com.dispatchops.application.service.CredibilityService;
import com.dispatchops.domain.model.Appeal;
import com.dispatchops.domain.model.CredibilityRating;
import com.dispatchops.domain.model.CreditLevelSnapshot;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.Violation;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.AppealCreateDTO;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.RatingSubmitDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/credibility")
public class CredibilityController {

    private static final Logger log = LoggerFactory.getLogger(CredibilityController.class);

    private final CredibilityService credibilityService;

    public CredibilityController(CredibilityService credibilityService) {
        this.credibilityService = credibilityService;
    }

    @PostMapping("/ratings")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR})
    public ResponseEntity<ApiResult<CredibilityRating>> submitRating(
            @Valid @RequestBody RatingSubmitDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' submitting rating for job {}", currentUser.getUsername(), dto.getJobId());
        CredibilityRating rating = credibilityService.submitRating(
                dto.getJobId(), dto.getTimeliness(), dto.getAttitude(), dto.getAccuracy(),
                dto.getComment(), currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(rating));
    }

    /**
     * Customer rating endpoint - allows external customers to rate via tracking number.
     * Per prompt: "Customers and Dispatchers rate completed deliveries."
     * This endpoint does not require session auth; validated by tracking number + receiver name.
     */
    /** Time-windowed rate limiter with TTL eviction and capped cardinality. Max 10 per hour per IP. */
    private final java.util.concurrent.ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<Long>> customerRateLimits = new java.util.concurrent.ConcurrentHashMap<>();
    private static final int RATE_LIMIT_MAX = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 3600_000L; // 1 hour
    private static final int RATE_LIMIT_MAX_KEYS = 10_000; // cap map cardinality
    private volatile long lastEvictionMs = System.currentTimeMillis();

    private boolean isRateLimited(String ip) {
        long now = System.currentTimeMillis();
        long cutoff = now - RATE_LIMIT_WINDOW_MS;

        // Periodic global eviction: remove stale IPs every 5 minutes
        if (now - lastEvictionMs > 300_000L) {
            lastEvictionMs = now;
            customerRateLimits.entrySet().removeIf(entry -> {
                entry.getValue().removeIf(t -> t < cutoff);
                return entry.getValue().isEmpty();
            });
        }

        // Hard cardinality cap: reject if map is full and IP is new
        if (!customerRateLimits.containsKey(ip) && customerRateLimits.size() >= RATE_LIMIT_MAX_KEYS) {
            log.warn("Rate limiter cardinality cap reached ({}), rejecting new IP={}", RATE_LIMIT_MAX_KEYS, ip);
            return true;
        }

        var timestamps = customerRateLimits.computeIfAbsent(ip, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
        // Prune expired entries for this IP
        timestamps.removeIf(t -> t < cutoff);
        if (timestamps.size() >= RATE_LIMIT_MAX) {
            return true;
        }
        timestamps.add(now);
        return false;
    }

    @PostMapping("/ratings/customer")
    public ResponseEntity<ApiResult<CredibilityRating>> submitCustomerRating(
            @Valid @RequestBody com.dispatchops.web.dto.CustomerRatingDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (isRateLimited(ip)) {
            log.warn("Customer rate limit exceeded for IP={}, tracking={}", ip, dto.getTrackingNumber());
            return ResponseEntity.status(429).body(ApiResult.error(429, "Too many requests. Please try again later."));
        }
        log.info("Customer rating submission for tracking={} from IP={}", dto.getTrackingNumber(), ip);
        try {
            CredibilityRating rating = credibilityService.submitCustomerRating(
                    dto.getTrackingNumber(), dto.getReceiverName(), dto.getCustomerToken(),
                    dto.getTimeliness(), dto.getAttitude(), dto.getAccuracy(), dto.getComment());
            return ResponseEntity.status(201).body(ApiResult.success(rating));
        } catch (com.dispatchops.domain.exception.ResourceNotFoundException | com.dispatchops.domain.exception.PermissionDeniedException e) {
            // Normalize to prevent enumeration: don't distinguish not-found from permission-denied
            return ResponseEntity.status(422).body(ApiResult.error(422, "Unable to process rating. Verify tracking number and receiver name."));
        }
    }

    @GetMapping("/customer/lookup")
    public ResponseEntity<ApiResult<Map<String, Object>>> customerLookup(
            @RequestParam String trackingNumber,
            @RequestParam String customerToken) {
        log.debug("Customer lookup for tracking={}", trackingNumber);
        try {
            Map<String, Object> info = credibilityService.lookupCustomerJob(trackingNumber, customerToken);
            return ResponseEntity.ok(ApiResult.success(info));
        } catch (com.dispatchops.domain.exception.ResourceNotFoundException | com.dispatchops.domain.exception.PermissionDeniedException e) {
            return ResponseEntity.status(422).body(ApiResult.error(422, "Unable to find delivery. Verify tracking number and token."));
        }
    }

    @GetMapping("/ratings/courier/{id}")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR})
    public ResponseEntity<ApiResult<PageResult<CredibilityRating>>> listRatings(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing ratings for courier {} - page: {}, size: {}", id, page, size);
        PageResult<CredibilityRating> result = credibilityService.listRatings(id, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/credit/{courierId}")
    public ResponseEntity<ApiResult<CreditLevelSnapshot>> getCreditLevel(
            @PathVariable Long courierId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        // Object-level auth: COURIER can only view own credit level
        if (currentUser.getRole() == Role.COURIER && !currentUser.getId().equals(courierId)) {
            throw new com.dispatchops.domain.exception.PermissionDeniedException(
                    "Courier can only view their own credit level");
        }
        log.debug("Getting credit level for courier {}", courierId);
        CreditLevelSnapshot snapshot = credibilityService.getCourierCredibility(courierId);
        return ResponseEntity.ok(ApiResult.success(snapshot));
    }

    @PostMapping("/violations")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<Violation>> recordViolation(
            @RequestBody Violation violation,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' recording violation for courier {}", currentUser.getUsername(), violation.getCourierId());
        Violation result = credibilityService.recordViolation(
                violation.getCourierId(), violation.getViolationType(), violation.getDescription(),
                violation.getJobId(), currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(result));
    }

    @GetMapping("/violations/courier/{id}")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<List<Violation>>> listViolations(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");

        // COURIER can only view their own violations
        if (currentUser.getRole() == Role.COURIER && !currentUser.getId().equals(id)) {
            return ResponseEntity.status(403)
                    .body(ApiResult.error(403, "Couriers can only view their own violations"));
        }

        log.debug("Listing violations for courier {}", id);
        List<Violation> result = credibilityService.listViolations(id);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PostMapping("/appeals")
    @RequireRole({Role.COURIER})
    public ResponseEntity<ApiResult<Appeal>> fileAppeal(
            @Valid @RequestBody AppealCreateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("Courier '{}' filing appeal", currentUser.getUsername());
        Appeal appeal = credibilityService.fileAppeal(
                dto.getRatingId(), dto.getViolationId(), dto.getReason(), currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(appeal));
    }

    /**
     * Customer appeal endpoint - allows customers to appeal ratings via tracking number.
     * Per prompt: "Customers...may file an appeal within 48 hours."
     */
    @PostMapping("/appeals/customer")
    public ResponseEntity<ApiResult<Appeal>> fileCustomerAppeal(
            @Valid @RequestBody com.dispatchops.web.dto.CustomerAppealDTO dto,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        if (isRateLimited(ip)) {
            log.warn("Customer rate limit exceeded for IP={}, tracking={}", ip, dto.getTrackingNumber());
            return ResponseEntity.status(429).body(ApiResult.error(429, "Too many requests. Please try again later."));
        }
        log.info("Customer appeal for tracking={} from IP={}", dto.getTrackingNumber(), ip);
        try {
            Appeal appeal = credibilityService.fileCustomerAppeal(
                    dto.getTrackingNumber(), dto.getReceiverName(), dto.getCustomerToken(),
                    dto.getRatingId(), dto.getReason());
            return ResponseEntity.status(201).body(ApiResult.success(appeal));
        } catch (com.dispatchops.domain.exception.ResourceNotFoundException | com.dispatchops.domain.exception.PermissionDeniedException e) {
            return ResponseEntity.status(422).body(ApiResult.error(422, "Unable to process appeal. Verify tracking number and receiver name."));
        }
    }

    @GetMapping("/appeals")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<PageResult<Appeal>>> listPendingAppeals(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        log.debug("Listing pending appeals - page: {}, size: {}", page, size);
        PageResult<Appeal> result = credibilityService.listPendingAppeals(page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @PutMapping("/appeals/{id}/resolve")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<Appeal>> resolveAppeal(
            @PathVariable Long id,
            @Valid @RequestBody com.dispatchops.web.dto.AppealResolveDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        String status = dto.getStatus();
        String comment = dto.getComment();
        log.info("User '{}' resolving appeal {} with status '{}'", currentUser.getUsername(), id, status);
        Appeal appeal = credibilityService.resolveAppeal(id, status, comment, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(appeal));
    }
}
