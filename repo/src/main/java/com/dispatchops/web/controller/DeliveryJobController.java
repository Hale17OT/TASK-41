package com.dispatchops.web.controller;

import com.dispatchops.application.service.DeliveryJobService;
import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.FulfillmentEvent;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.DeliveryJobCreateDTO;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.StatusTransitionDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class DeliveryJobController {

    private static final Logger log = LoggerFactory.getLogger(DeliveryJobController.class);

    private final DeliveryJobService deliveryJobService;

    public DeliveryJobController(DeliveryJobService deliveryJobService) {
        this.deliveryJobService = deliveryJobService;
    }

    @GetMapping
    public ResponseEntity<ApiResult<PageResult<DeliveryJob>>> listJobs(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Listing jobs - status: {}, page: {}, size: {}", status, page, size);

        PageResult<DeliveryJob> result;
        if (currentUser.getRole() == Role.COURIER) {
            // Query-level filtering: courier only sees their own assigned jobs with proper pagination
            result = deliveryJobService.listJobsByCourierPaged(currentUser.getId(), status, page, size);
        } else {
            result = deliveryJobService.listJobs(status, page, size);
        }

        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResult<DeliveryJob>> getJob(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting job with id: {}", id);
        DeliveryJob job = deliveryJobService.getJob(id);

        // Object-level auth: COURIER can only view their own assigned jobs
        if (currentUser.getRole() == Role.COURIER) {
            if (job.getCourierId() == null || !job.getCourierId().equals(currentUser.getId())) {
                throw new PermissionDeniedException(
                        "Courier " + currentUser.getId() + " is not assigned to job " + id);
            }
        }

        return ResponseEntity.ok(ApiResult.success(job));
    }

    @PostMapping
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<DeliveryJob>> createJob(
            @Valid @RequestBody DeliveryJobCreateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' creating delivery job", currentUser.getUsername());
        DeliveryJob job = deliveryJobService.createJob(dto, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(job));
    }

    @PutMapping("/{id}/assign")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<DeliveryJob>> assignCourier(
            @PathVariable Long id,
            @RequestParam Long courierId,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' assigning courier {} to job {}", currentUser.getUsername(), courierId, id);
        DeliveryJob job = deliveryJobService.assignCourier(id, courierId, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(job));
    }

    @PutMapping("/{id}/status")
    @RequireRole({Role.COURIER, Role.DISPATCHER, Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<DeliveryJob>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' transitioning status of job {} to {}", currentUser.getUsername(), id, dto.getStatus());

        // Object-level auth: COURIER can only transition their own jobs
        if (currentUser.getRole() == Role.COURIER) {
            DeliveryJob job = deliveryJobService.getJob(id);
            if (job.getCourierId() == null || !job.getCourierId().equals(currentUser.getId())) {
                throw new PermissionDeniedException(
                        "Courier " + currentUser.getId() + " is not assigned to job " + id);
            }
        }

        DeliveryJob job = deliveryJobService.transitionStatus(
                id, dto.getStatus(), dto.getComment(), dto.getVersion(), currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(job));
    }

    @PutMapping("/{id}/override")
    @RequireRole({Role.ADMIN})
    public ResponseEntity<ApiResult<DeliveryJob>> adminOverride(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        String comment = body.get("comment");
        log.info("Admin '{}' overriding job {}", currentUser.getUsername(), id);
        DeliveryJob job = deliveryJobService.adminOverride(id, comment, currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(job));
    }

    @GetMapping("/{id}/events")
    public ResponseEntity<ApiResult<List<FulfillmentEvent>>> getJobEvents(
            @PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting events for job {}", id);

        // Object-level auth: COURIER can only view events for their own assigned jobs
        if (currentUser.getRole() == Role.COURIER) {
            DeliveryJob job = deliveryJobService.getJob(id);
            if (job.getCourierId() == null || !job.getCourierId().equals(currentUser.getId())) {
                throw new PermissionDeniedException(
                        "Courier " + currentUser.getId() + " is not assigned to job " + id);
            }
        }

        List<FulfillmentEvent> events = deliveryJobService.getJobEvents(id);
        return ResponseEntity.ok(ApiResult.success(events));
    }

    @GetMapping("/idle")
    @RequireRole({Role.OPS_MANAGER, Role.ADMIN})
    public ResponseEntity<ApiResult<List<DeliveryJob>>> getIdleJobs(
            @RequestParam(defaultValue = "20") int minutes) {
        log.debug("Getting idle jobs longer than {} minutes", minutes);
        List<DeliveryJob> jobs = deliveryJobService.getIdleJobs(minutes);
        return ResponseEntity.ok(ApiResult.success(jobs));
    }

    @PostMapping("/picklist")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<List<DeliveryJob>>> generatePickList(@RequestParam String runDate) {
        log.info("Generating pick list for date: {}", runDate);
        LocalDate date = LocalDate.parse(runDate);
        List<DeliveryJob> pickList = deliveryJobService.generatePickList(date);
        return ResponseEntity.ok(ApiResult.success(pickList));
    }

    @PostMapping("/sortlist")
    @RequireRole({Role.DISPATCHER, Role.OPS_MANAGER})
    public ResponseEntity<ApiResult<List<DeliveryJob>>> generateSortList(@RequestParam String runDate) {
        log.info("Generating sort list for date: {}", runDate);
        LocalDate date = LocalDate.parse(runDate);
        List<DeliveryJob> sortList = deliveryJobService.generateSortList(date);
        return ResponseEntity.ok(ApiResult.success(sortList));
    }
}
