package com.dispatchops.application.service;

import com.dispatchops.domain.exception.AddressValidationException;
import com.dispatchops.domain.exception.InsufficientCreditException;
import com.dispatchops.domain.exception.OptimisticLockException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.CreditLevelSnapshot;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.FulfillmentEvent;
import com.dispatchops.domain.model.enums.EventType;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.domain.service.StatusTransitionValidator;
import com.dispatchops.infrastructure.persistence.mapper.CreditLevelMapper;
import com.dispatchops.infrastructure.persistence.mapper.DeliveryJobMapper;
import com.dispatchops.infrastructure.persistence.mapper.FulfillmentEventMapper;
import com.dispatchops.infrastructure.security.FieldEncryptionService;
import com.dispatchops.web.dto.DeliveryJobCreateDTO;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.infrastructure.security.HmacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Service
@Transactional
public class DeliveryJobService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryJobService.class);

    private final DeliveryJobMapper deliveryJobMapper;
    private final FulfillmentEventMapper fulfillmentEventMapper;
    private final ShippingRuleService shippingRuleService;
    private final NotificationService notificationService;
    private final CreditLevelMapper creditLevelMapper;
    private final FieldEncryptionService fieldEncryptionService;
    private final SearchService searchService;
    private final String hmacKey;

    public DeliveryJobService(DeliveryJobMapper deliveryJobMapper,
                              FulfillmentEventMapper fulfillmentEventMapper,
                              ShippingRuleService shippingRuleService,
                              NotificationService notificationService,
                              CreditLevelMapper creditLevelMapper,
                              FieldEncryptionService fieldEncryptionService,
                              SearchService searchService,
                              @Value("${security.hmac.key}") String hmacKey) {
        this.deliveryJobMapper = deliveryJobMapper;
        this.fulfillmentEventMapper = fulfillmentEventMapper;
        this.shippingRuleService = shippingRuleService;
        this.notificationService = notificationService;
        this.creditLevelMapper = creditLevelMapper;
        this.fieldEncryptionService = fieldEncryptionService;
        this.searchService = searchService;
        this.hmacKey = hmacKey;
    }

    private void indexJob(DeliveryJob job) {
        try {
            searchService.indexEntity("JOB", job.getId(), job.getTrackingNumber(),
                    job.getReceiverName() + " " + job.getReceiverAddress(),
                    job.getStatus() != null ? job.getStatus().name() : "", job.getDispatcherId());
        } catch (Exception e) {
            log.warn("Failed to index job {}: {}", job.getId(), e.getMessage());
        }
    }

    public DeliveryJob createJob(DeliveryJobCreateDTO dto, Long dispatcherId) {
        log.info("Creating delivery job for dispatcherId={}", dispatcherId);

        ShippingRuleService.ValidationResult validation = shippingRuleService.validateAddress(
                dto.getReceiverState(), dto.getReceiverZip(), dto.getWeightLbs(), dto.getOrderAmount());
        if (!validation.isValid()) {
            log.warn("Address validation failed: {}", validation.getReason());
            throw new AddressValidationException("Address validation failed", validation.getReason());
        }

        String trackingNumber = "DO-" + System.currentTimeMillis() + "-" + (1000 + new Random().nextInt(9000));

        DeliveryJob job = new DeliveryJob();
        job.setTrackingNumber(trackingNumber);
        job.setStatus(JobStatus.CREATED);
        job.setDispatcherId(dispatcherId);
        job.setSenderName(dto.getSenderName());
        job.setSenderAddress(dto.getSenderAddress());
        job.setReceiverName(dto.getReceiverName());
        job.setReceiverAddress(dto.getReceiverAddress());
        job.setReceiverState(dto.getReceiverState());
        job.setReceiverZip(dto.getReceiverZip());
        job.setWeightLbs(dto.getWeightLbs());
        job.setOrderAmount(dto.getOrderAmount());
        job.setAdminOverride(false);
        job.setCreatedAt(LocalDateTime.now());
        job.setUpdatedAt(LocalDateTime.now());
        job.setVersion(1);

        if (dto.getSenderPhone() != null) {
            job.setSenderPhoneEncrypted(fieldEncryptionService.encrypt(dto.getSenderPhone()));
        }
        if (dto.getReceiverPhone() != null) {
            job.setReceiverPhoneEncrypted(fieldEncryptionService.encrypt(dto.getReceiverPhone()));
        }

        deliveryJobMapper.insert(job);
        log.info("Delivery job inserted with id={}, trackingNumber={}", job.getId(), trackingNumber);

        FulfillmentEvent event = new FulfillmentEvent();
        event.setJobId(job.getId());
        event.setEventType(EventType.STATUS_CHANGE);
        event.setNewStatus(JobStatus.CREATED);
        event.setActorId(dispatcherId);
        event.setCreatedAt(LocalDateTime.now());
        fulfillmentEventMapper.insert(event);

        LocalDateTime now = LocalDateTime.now();
        job.setLastEventAt(now);
        deliveryJobMapper.updateLastEventAt(job.getId(), now);

        log.info("Delivery job id={} created successfully", job.getId());
        indexJob(job);
        return job;
    }

    public DeliveryJob assignCourier(Long jobId, Long courierId, Long actorId) {
        log.info("Assigning courier={} to jobId={} by actorId={}", courierId, jobId, actorId);

        DeliveryJob job = deliveryJobMapper.findById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with id: " + jobId, "DeliveryJob", jobId);
        }

        if (job.getStatus() != JobStatus.CREATED && job.getStatus() != JobStatus.MANUAL_VALIDATION) {
            throw new IllegalStateException(
                    "Cannot assign courier when job status is " + job.getStatus()
                            + ". Job must be in CREATED or MANUAL_VALIDATION status.");
        }

        CreditLevelSnapshot creditLevel = creditLevelMapper.findByCourierId(courierId);
        if (creditLevel == null) {
            throw new ResourceNotFoundException("Credit level not found for courierId: " + courierId, "CreditLevelSnapshot", courierId);
        }

        int activeJobs = deliveryJobMapper.countByCourierAndActiveStatuses(courierId);
        if (activeJobs >= creditLevel.getMaxConcurrent()) {
            log.warn("Courier {} has {} active jobs but max concurrent is {}",
                    courierId, activeJobs, creditLevel.getMaxConcurrent());
            throw new InsufficientCreditException(
                    "Courier has reached maximum concurrent jobs",
                    creditLevel.getLevel().name(),
                    creditLevel.getMaxConcurrent(),
                    activeJobs);
        }

        deliveryJobMapper.updateCourier(jobId, courierId);
        job.setCourierId(courierId);

        notificationService.notify(
                courierId,
                NotificationType.STATUS_CHANGE,
                "Job Assigned",
                "You have been assigned to delivery job " + job.getTrackingNumber(),
                "DeliveryJob",
                jobId);

        log.info("Courier {} assigned to job {} successfully", courierId, jobId);
        indexJob(job);
        return job;
    }

    public DeliveryJob transitionStatus(Long jobId, String newStatusStr, String comment,
                                         int expectedVersion, Long actorId) {
        log.info("Transitioning status for jobId={} to {} by actorId={}", jobId, newStatusStr, actorId);

        DeliveryJob job = deliveryJobMapper.findById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with id: " + jobId, "DeliveryJob", jobId);
        }

        JobStatus newStatus = JobStatus.valueOf(newStatusStr);
        JobStatus oldStatus = job.getStatus();

        StatusTransitionValidator.validateJobTransition(oldStatus, newStatus);

        if (newStatus == JobStatus.EXCEPTION && (comment == null || comment.isBlank())) {
            throw new IllegalArgumentException("Comment required for EXCEPTION status");
        }

        int rowsUpdated = deliveryJobMapper.updateStatus(jobId, newStatusStr, expectedVersion);
        if (rowsUpdated == 0) {
            throw new OptimisticLockException(
                    "Delivery job was modified by another transaction",
                    "DeliveryJob", jobId, expectedVersion);
        }

        FulfillmentEvent event = new FulfillmentEvent();
        event.setJobId(jobId);
        event.setEventType(EventType.STATUS_CHANGE);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setActorId(actorId);
        event.setComment(comment);
        event.setCreatedAt(LocalDateTime.now());
        fulfillmentEventMapper.insert(event);

        LocalDateTime now = LocalDateTime.now();
        deliveryJobMapper.updateLastEventAt(jobId, now);
        job.setLastEventAt(now);
        job.setStatus(newStatus);

        // Generate customer action token on DELIVERED (used for customer rating/appeal)
        if (newStatus == JobStatus.DELIVERED && job.getCustomerToken() == null) {
            String token = HmacUtil.computeHmac(
                    job.getTrackingNumber() + "|" + job.getReceiverName() + "|" + job.getId(),
                    hmacKey).substring(0, 32);
            job.setCustomerToken(token);
            deliveryJobMapper.updateCustomerToken(jobId, token);
            log.info("Customer token generated for job {}", job.getTrackingNumber());
        }

        if (job.getDispatcherId() != null) {
            notificationService.notify(
                    job.getDispatcherId(),
                    NotificationType.STATUS_CHANGE,
                    "Job Status Updated",
                    "Job " + job.getTrackingNumber() + " status changed from " + oldStatus + " to " + newStatus,
                    "DeliveryJob",
                    jobId);
        }
        if (job.getCourierId() != null) {
            notificationService.notify(
                    job.getCourierId(),
                    NotificationType.STATUS_CHANGE,
                    "Job Status Updated",
                    "Job " + job.getTrackingNumber() + " status changed from " + oldStatus + " to " + newStatus,
                    "DeliveryJob",
                    jobId);
        }

        log.info("Job {} transitioned from {} to {}", jobId, oldStatus, newStatus);
        indexJob(job);
        return job;
    }

    public DeliveryJob adminOverride(Long jobId, String comment, Long adminId) {
        log.info("Admin override for jobId={} by adminId={}", jobId, adminId);

        DeliveryJob job = deliveryJobMapper.findById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with id: " + jobId, "DeliveryJob", jobId);
        }

        job.setAdminOverride(true);
        job.setOverrideComment(comment);
        deliveryJobMapper.adminOverride(jobId, comment);

        JobStatus oldStatus = job.getStatus();
        JobStatus newStatus = JobStatus.MANUAL_VALIDATION;

        int rowsUpdated = deliveryJobMapper.updateStatus(jobId, newStatus.name(), job.getVersion());
        if (rowsUpdated == 0) {
            throw new OptimisticLockException(
                    "Delivery job was modified by another transaction",
                    "DeliveryJob", jobId, job.getVersion());
        }

        FulfillmentEvent event = new FulfillmentEvent();
        event.setJobId(jobId);
        event.setEventType(EventType.STATUS_CHANGE);
        event.setOldStatus(oldStatus);
        event.setNewStatus(newStatus);
        event.setActorId(adminId);
        event.setComment("Admin override: " + comment);
        event.setCreatedAt(LocalDateTime.now());
        fulfillmentEventMapper.insert(event);

        LocalDateTime now = LocalDateTime.now();
        deliveryJobMapper.updateLastEventAt(jobId, now);
        job.setLastEventAt(now);
        job.setStatus(newStatus);

        log.info("Admin override applied to job {}, status set to MANUAL_VALIDATION", jobId);
        indexJob(job);
        return job;
    }

    @Transactional(readOnly = true)
    public DeliveryJob getJob(Long id) {
        log.debug("Fetching delivery job with id={}", id);
        DeliveryJob job = deliveryJobMapper.findById(id);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with id: " + id, "DeliveryJob", id);
        }
        return job;
    }

    @Transactional(readOnly = true)
    public PageResult<DeliveryJob> listJobs(String status, int page, int size) {
        log.debug("Listing delivery jobs: status={}, page={}, size={}", status, page, size);

        int offset = page * size;
        List<DeliveryJob> jobs;
        long total;

        if (status != null && !status.isBlank()) {
            jobs = deliveryJobMapper.findByStatus(status, offset, size);
            total = deliveryJobMapper.countByStatus(status);
        } else {
            jobs = deliveryJobMapper.findByStatus(null, offset, size);
            total = deliveryJobMapper.countAll();
        }

        return new PageResult<>(jobs, page, size, total);
    }

    @Transactional(readOnly = true)
    public List<DeliveryJob> listJobsByCourier(Long courierId, int offset, int limit) {
        log.debug("Listing delivery jobs for courierId={}, offset={}, limit={}", courierId, offset, limit);
        return deliveryJobMapper.findByCourierId(courierId, offset, limit);
    }

    @Transactional(readOnly = true)
    public PageResult<DeliveryJob> listJobsByCourierPaged(Long courierId, String status, int page, int size) {
        log.debug("Listing delivery jobs for courierId={}, status={}, page={}, size={}", courierId, status, page, size);
        int offset = page * size;
        List<DeliveryJob> jobs = deliveryJobMapper.findByCourierIdAndStatus(courierId, status, offset, size);
        int total = deliveryJobMapper.countByCourierId(courierId, status);
        return new PageResult<>(jobs, page, size, total);
    }

    @Transactional(readOnly = true)
    public List<DeliveryJob> getIdleJobs(int thresholdMinutes) {
        log.debug("Fetching idle jobs with threshold={} minutes", thresholdMinutes);
        return deliveryJobMapper.findIdleLongerThan(thresholdMinutes);
    }

    @Transactional(readOnly = true)
    public List<FulfillmentEvent> getJobEvents(Long jobId) {
        log.debug("Fetching events for jobId={}", jobId);
        return fulfillmentEventMapper.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<DeliveryJob> generatePickList(LocalDate runDate) {
        log.info("Generating pick list for date={}", runDate);
        // Use inclusive day bounds: start at 00:00:00, end at 23:59:59
        return deliveryJobMapper.search(null, JobStatus.CREATED.name(),
                runDate, runDate.plusDays(1), 0, Integer.MAX_VALUE);
    }

    @Transactional(readOnly = true)
    public List<DeliveryJob> generateSortList(LocalDate runDate) {
        log.info("Generating sort list for date={}", runDate);
        return deliveryJobMapper.search(null, JobStatus.PICKED.name(),
                runDate, runDate.plusDays(1), 0, Integer.MAX_VALUE);
    }
}
