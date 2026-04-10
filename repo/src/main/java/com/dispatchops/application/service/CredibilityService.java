package com.dispatchops.application.service;

import com.dispatchops.domain.exception.AppealWindowClosedException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.Appeal;
import com.dispatchops.domain.model.CredibilityRating;
import com.dispatchops.domain.model.CreditLevelSnapshot;
import com.dispatchops.domain.model.DeliveryJob;
import com.dispatchops.domain.model.Violation;
import com.dispatchops.domain.model.enums.AppealStatus;
import com.dispatchops.domain.model.enums.CreditLevel;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.domain.service.CreditLevelCalculator;
import com.dispatchops.infrastructure.persistence.mapper.AppealMapper;
import com.dispatchops.infrastructure.persistence.mapper.CredibilityRatingMapper;
import com.dispatchops.infrastructure.persistence.mapper.CreditLevelMapper;
import com.dispatchops.infrastructure.persistence.mapper.DeliveryJobMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import com.dispatchops.infrastructure.persistence.mapper.ViolationMapper;
import com.dispatchops.infrastructure.security.HmacUtil;
import com.dispatchops.web.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class CredibilityService {

    private static final Logger log = LoggerFactory.getLogger(CredibilityService.class);
    private static final long APPEAL_WINDOW_HOURS = 48;

    private final CredibilityRatingMapper credibilityRatingMapper;
    private final CreditLevelMapper creditLevelMapper;
    private final ViolationMapper violationMapper;
    private final AppealMapper appealMapper;
    private final DeliveryJobMapper deliveryJobMapper;
    private final NotificationService notificationService;
    private final UserMapper userMapper;
    private final String hmacKey;

    public CredibilityService(CredibilityRatingMapper credibilityRatingMapper,
                              CreditLevelMapper creditLevelMapper,
                              ViolationMapper violationMapper,
                              AppealMapper appealMapper,
                              DeliveryJobMapper deliveryJobMapper,
                              NotificationService notificationService,
                              UserMapper userMapper,
                              @Value("${security.hmac.key}") String hmacKey) {
        this.credibilityRatingMapper = credibilityRatingMapper;
        this.creditLevelMapper = creditLevelMapper;
        this.violationMapper = violationMapper;
        this.appealMapper = appealMapper;
        this.deliveryJobMapper = deliveryJobMapper;
        this.notificationService = notificationService;
        this.userMapper = userMapper;
        this.hmacKey = hmacKey;
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> lookupCustomerJob(String trackingNumber, String customerToken) {
        DeliveryJob job = deliveryJobMapper.findByTrackingNumber(trackingNumber);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + trackingNumber, "DeliveryJob", trackingNumber);
        }
        validateCustomerToken(job, customerToken);

        java.util.Map<String, Object> info = new java.util.LinkedHashMap<>();
        info.put("trackingNumber", job.getTrackingNumber());
        info.put("status", job.getStatus() != null ? job.getStatus().name() : null);
        info.put("receiverName", job.getReceiverName());
        info.put("canRate", job.getStatus() == JobStatus.DELIVERED);

        // Include ratings available for appeal (non-excluded, within 48-hour window)
        boolean delivered = job.getStatus() == JobStatus.DELIVERED;
        if (delivered) {
            LocalDateTime cutoff = LocalDateTime.now().minusHours(APPEAL_WINDOW_HOURS);
            List<CredibilityRating> jobRatings = credibilityRatingMapper.findByJobId(job.getId());
            List<java.util.Map<String, Object>> appealableRatings = new java.util.ArrayList<>();
            for (CredibilityRating r : jobRatings) {
                if (r.getCreatedAt() != null && r.getCreatedAt().isAfter(cutoff)) {
                    java.util.Map<String, Object> rm = new java.util.LinkedHashMap<>();
                    rm.put("id", r.getId());
                    rm.put("timeliness", r.getTimeliness());
                    rm.put("attitude", r.getAttitude());
                    rm.put("accuracy", r.getAccuracy());
                    rm.put("createdAt", r.getCreatedAt().toString());
                    appealableRatings.add(rm);
                }
            }
            info.put("canAppeal", !appealableRatings.isEmpty());
            info.put("appealableRatings", appealableRatings);
        } else {
            info.put("canAppeal", false);
            info.put("appealableRatings", java.util.Collections.emptyList());
        }
        return info;
    }

    public CredibilityRating submitRating(Long jobId, int timeliness, int attitude, int accuracy,
                                           String comment, Long raterId) {
        log.info("Submitting rating for jobId={} by raterId={}", jobId, raterId);

        DeliveryJob job = deliveryJobMapper.findById(jobId);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with id: " + jobId, "DeliveryJob", jobId);
        }

        if (job.getStatus() != JobStatus.DELIVERED) {
            throw new IllegalStateException(
                    "Cannot rate a job that is not in DELIVERED status. Current status: " + job.getStatus());
        }

        CredibilityRating rating = new CredibilityRating();
        rating.setCourierId(job.getCourierId());
        rating.setJobId(jobId);
        rating.setRaterId(raterId);
        rating.setRaterType("STAFF");
        rating.setTimeliness(timeliness);
        rating.setAttitude(attitude);
        rating.setAccuracy(accuracy);
        rating.setComment(comment);
        rating.setExcluded(false);
        rating.setCreatedAt(LocalDateTime.now());

        try {
            credibilityRatingMapper.insert(rating);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new com.dispatchops.domain.exception.IdempotencyViolationException(
                    "A rating for this job has already been submitted by this rater", jobId);
        }
        log.info("Rating id={} submitted for courier {} on job {}", rating.getId(), job.getCourierId(), jobId);

        if (job.getCourierId() != null) {
            notificationService.notify(
                    job.getCourierId(),
                    NotificationType.RATING_RECEIVED,
                    "New Rating Received",
                    "You received a new rating for job " + job.getTrackingNumber(),
                    "CredibilityRating",
                    rating.getId());

            recalculateCreditLevel(job.getCourierId());
        }

        return rating;
    }

    /**
     * Validates the customer action token against the stored token on the job.
     * Recomputes the expected token from job data + HMAC key and compares using timing-safe equality.
     */
    private void validateCustomerToken(DeliveryJob job, String customerToken) {
        if (job.getCustomerToken() == null) {
            throw new com.dispatchops.domain.exception.PermissionDeniedException(
                    "Customer action token not available for this job");
        }
        String expected = HmacUtil.computeHmac(
                job.getTrackingNumber() + "|" + job.getReceiverName() + "|" + job.getId(),
                hmacKey).substring(0, 32);
        if (!java.security.MessageDigest.isEqual(
                expected.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                customerToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
            throw new com.dispatchops.domain.exception.PermissionDeniedException(
                    "Invalid customer action token");
        }
    }

    /**
     * Customer-facing rating submission. Validates by tracking number + receiver name + customer token.
     * Per prompt: "Customers and Dispatchers rate completed deliveries."
     */
    public CredibilityRating submitCustomerRating(String trackingNumber, String receiverName,
                                                   String customerToken,
                                                   int timeliness, int attitude, int accuracy, String comment) {
        log.info("Customer rating for trackingNumber={}", trackingNumber);

        DeliveryJob job = deliveryJobMapper.findByTrackingNumber(trackingNumber);
        if (job == null) {
            throw new ResourceNotFoundException("Delivery job not found with tracking: " + trackingNumber, "DeliveryJob", trackingNumber);
        }

        if (job.getStatus() != com.dispatchops.domain.model.enums.JobStatus.DELIVERED) {
            throw new IllegalStateException("Cannot rate a job that is not DELIVERED. Current: " + job.getStatus());
        }

        // Validate receiver name matches - use generic error to prevent enumeration
        if (!job.getReceiverName().equalsIgnoreCase(receiverName)) {
            throw new com.dispatchops.domain.exception.PermissionDeniedException(
                    "Invalid tracking number or receiver name");
        }

        // Validate customer action token
        validateCustomerToken(job, customerToken);

        // Customer ratings: rater_id is NULL (no user account), rater_type=CUSTOMER.
        // customer_name provides the attribution identity. Unique per (job_id, customer_name).
        CredibilityRating rating = new CredibilityRating();
        rating.setCourierId(job.getCourierId());
        rating.setJobId(job.getId());
        rating.setRaterId(null);
        rating.setRaterType("CUSTOMER");
        rating.setTimeliness(timeliness);
        rating.setAttitude(attitude);
        rating.setAccuracy(accuracy);
        rating.setComment(comment);
        rating.setCustomerName(receiverName);
        rating.setExcluded(false);
        rating.setCreatedAt(LocalDateTime.now());

        try {
            credibilityRatingMapper.insert(rating);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new com.dispatchops.domain.exception.IdempotencyViolationException(
                    "A customer rating for this job has already been submitted", job.getId());
        }
        log.info("Customer rating id={} created for job {} by customer '{}'", rating.getId(), job.getId(), receiverName);

        if (job.getCourierId() != null) {
            recalculateCreditLevel(job.getCourierId());
        }
        return rating;
    }

    /**
     * Customer-facing appeal. Validates by tracking number + receiver name + customer token.
     * Per prompt: "Customers...may file an appeal within 48 hours."
     */
    public Appeal fileCustomerAppeal(String trackingNumber, String receiverName, String customerToken,
                                      Long ratingId, String reason) {
        log.info("Customer appeal for trackingNumber={}", trackingNumber);

        DeliveryJob job = deliveryJobMapper.findByTrackingNumber(trackingNumber);
        if (job == null) {
            throw new ResourceNotFoundException("Job not found: " + trackingNumber, "DeliveryJob", trackingNumber);
        }
        if (!job.getReceiverName().equalsIgnoreCase(receiverName)) {
            throw new com.dispatchops.domain.exception.PermissionDeniedException("Invalid tracking number or receiver name");
        }

        // Validate customer action token
        validateCustomerToken(job, customerToken);

        // Validate rating belongs to the resolved job (prevents cross-object targeting)
        Long courierId = null;
        if (ratingId != null) {
            CredibilityRating rating = credibilityRatingMapper.findById(ratingId);
            if (rating == null) {
                throw new ResourceNotFoundException("Rating not found: " + ratingId, "CredibilityRating", ratingId);
            }
            if (!rating.getJobId().equals(job.getId())) {
                throw new com.dispatchops.domain.exception.PermissionDeniedException(
                        "Rating " + ratingId + " does not belong to job " + job.getTrackingNumber());
            }
            courierId = rating.getCourierId();
        }
        if (courierId == null) {
            courierId = job.getCourierId();
        }

        String auditReason = "[Customer: " + receiverName + "] " + reason;
        return fileAppeal(ratingId, null, auditReason, courierId);
    }

    public Appeal fileAppeal(Long ratingId, Long violationId, String reason, Long courierId) {
        log.info("Filing appeal by courierId={}, ratingId={}, violationId={}", courierId, ratingId, violationId);

        // Exactly one of ratingId or violationId must be provided
        if (ratingId == null && violationId == null) {
            throw new IllegalArgumentException("Appeal must reference either a ratingId or a violationId");
        }
        if (ratingId != null && violationId != null) {
            throw new IllegalArgumentException("Appeal must reference exactly one of ratingId or violationId, not both");
        }

        LocalDateTime now = LocalDateTime.now();

        if (ratingId != null) {
            CredibilityRating rating = credibilityRatingMapper.findById(ratingId);
            if (rating == null) {
                throw new ResourceNotFoundException("Rating not found with id: " + ratingId, "CredibilityRating", ratingId);
            }
            // Ownership check: rating must be about this courier
            if (!rating.getCourierId().equals(courierId)) {
                throw new com.dispatchops.domain.exception.PermissionDeniedException(
                        "Rating " + ratingId + " does not belong to courier " + courierId);
            }
            if (rating.getCreatedAt().plusHours(APPEAL_WINDOW_HOURS).isBefore(now)) {
                throw new AppealWindowClosedException(
                        "Appeal window has closed. Ratings must be appealed within " + APPEAL_WINDOW_HOURS + " hours.");
            }
        }

        if (violationId != null) {
            Violation violation = violationMapper.findById(violationId);
            if (violation == null) {
                throw new ResourceNotFoundException("Violation not found with id: " + violationId, "Violation", violationId);
            }
            // Ownership check: violation must be about this courier
            if (!violation.getCourierId().equals(courierId)) {
                throw new com.dispatchops.domain.exception.PermissionDeniedException(
                        "Violation " + violationId + " does not belong to courier " + courierId);
            }
            if (violation.getCreatedAt().plusHours(APPEAL_WINDOW_HOURS).isBefore(now)) {
                throw new AppealWindowClosedException(
                        "Appeal window has closed. Violations must be appealed within " + APPEAL_WINDOW_HOURS + " hours.");
            }
        }

        Appeal appeal = new Appeal();
        appeal.setRatingId(ratingId);
        appeal.setViolationId(violationId);
        appeal.setCourierId(courierId);
        appeal.setReason(reason);
        appeal.setStatus(AppealStatus.PENDING);
        appeal.setCreatedAt(LocalDateTime.now());

        appealMapper.insert(appeal);
        log.info("Appeal id={} filed by courier {}", appeal.getId(), courierId);

        List<com.dispatchops.domain.model.User> admins = userMapper.findByRole(Role.ADMIN.name());
        for (com.dispatchops.domain.model.User admin : admins) {
            notificationService.notify(
                    admin.getId(),
                    NotificationType.APPEAL_UPDATE,
                    "New Appeal Filed",
                    "Courier has filed an appeal requiring review",
                    "Appeal",
                    appeal.getId());
        }

        return appeal;
    }

    public Appeal resolveAppeal(Long appealId, String statusStr, String comment, Long reviewerId) {
        log.info("Resolving appeal id={} with status={} by reviewerId={}", appealId, statusStr, reviewerId);

        Appeal appeal = appealMapper.findById(appealId);
        if (appeal == null) {
            throw new ResourceNotFoundException("Appeal not found with id: " + appealId, "Appeal", appealId);
        }

        AppealStatus status = AppealStatus.valueOf(statusStr);
        LocalDateTime now = LocalDateTime.now();

        int rowsUpdated = appealMapper.resolve(appealId, statusStr, reviewerId, comment, now);
        if (rowsUpdated == 0) {
            throw new IllegalStateException("Appeal " + appealId + " is no longer in PENDING status (already resolved)");
        }

        if (status == AppealStatus.APPROVED) {
            if (appeal.getRatingId() != null) {
                credibilityRatingMapper.excludeRating(appeal.getRatingId());
                log.info("Rating id={} excluded due to approved appeal", appeal.getRatingId());
            }
            if (appeal.getViolationId() != null) {
                violationMapper.deactivate(appeal.getViolationId());
                log.info("Violation id={} deactivated due to approved appeal", appeal.getViolationId());
            }
        }

        recalculateCreditLevel(appeal.getCourierId());

        notificationService.notify(
                appeal.getCourierId(),
                NotificationType.APPEAL_UPDATE,
                "Appeal " + status.name(),
                "Your appeal has been " + status.name().toLowerCase() + "." +
                        (comment != null ? " Reviewer comment: " + comment : ""),
                "Appeal",
                appealId);

        appeal.setStatus(status);
        appeal.setReviewerId(reviewerId);
        appeal.setReviewerComment(comment);
        appeal.setResolvedAt(now);

        log.info("Appeal {} resolved with status {}", appealId, status);
        return appeal;
    }

    public Violation recordViolation(Long courierId, String type, String desc, Long jobId, Long issuedBy) {
        log.info("Recording violation for courierId={}, type={}", courierId, type);

        LocalDateTime now = LocalDateTime.now();

        Violation violation = new Violation();
        violation.setCourierId(courierId);
        violation.setViolationType(type);
        violation.setDescription(desc);
        violation.setJobId(jobId);
        violation.setIssuedBy(issuedBy);
        violation.setPenaltyStart(now);
        violation.setPenaltyEnd(now.plusDays(90));
        violation.setActive(true);
        violation.setCreatedAt(now);

        violationMapper.insert(violation);
        log.info("Violation id={} recorded for courier {}", violation.getId(), courierId);

        recalculateCreditLevel(courierId);

        notificationService.notify(
                courierId,
                NotificationType.SYSTEM,
                "Violation Recorded",
                "A " + type + " violation has been recorded on your account. " +
                        "Penalty period: 90 days.",
                "Violation",
                violation.getId());

        return violation;
    }

    public CreditLevelSnapshot recalculateCreditLevel(Long courierId) {
        log.info("Recalculating credit level for courierId={}", courierId);

        BigDecimal avgRating30d = credibilityRatingMapper.calculateAvgRating30d(courierId);
        int activeViolations = violationMapper.countActiveByCourierId(courierId);

        CreditLevel level = CreditLevelCalculator.calculate(avgRating30d, activeViolations);

        CreditLevelSnapshot snapshot = new CreditLevelSnapshot();
        snapshot.setCourierId(courierId);
        snapshot.setLevel(level);
        snapshot.setMaxConcurrent(level.getMaxConcurrent());
        snapshot.setAvgRating30d(avgRating30d != null ? avgRating30d : BigDecimal.ZERO);
        snapshot.setViolationsActive(activeViolations);
        snapshot.setCalculatedAt(LocalDateTime.now());

        creditLevelMapper.upsert(snapshot);

        log.info("Credit level for courier {} recalculated: level={}, maxConcurrent={}, avg30d={}, violations={}",
                courierId, level, level.getMaxConcurrent(), avgRating30d, activeViolations);
        return snapshot;
    }

    @Transactional(readOnly = true)
    public CreditLevelSnapshot getCourierCredibility(Long courierId) {
        log.debug("Fetching credibility for courierId={}", courierId);
        CreditLevelSnapshot snapshot = creditLevelMapper.findByCourierId(courierId);
        if (snapshot == null) {
            throw new ResourceNotFoundException(
                    "Credit level not found for courierId: " + courierId, "CreditLevelSnapshot", courierId);
        }
        return snapshot;
    }

    @Transactional(readOnly = true)
    public List<Violation> listViolations(Long courierId) {
        log.debug("Listing violations for courierId={}", courierId);
        return violationMapper.findByCourierId(courierId);
    }

    @Transactional(readOnly = true)
    public PageResult<CredibilityRating> listRatings(Long courierId, int page, int size) {
        log.debug("Listing ratings for courierId={}, page={}, size={}", courierId, page, size);

        int offset = page * size;
        List<CredibilityRating> ratings = credibilityRatingMapper.findByCourierId(courierId, offset, size);
        long total = credibilityRatingMapper.countByCourierId(courierId);

        return new PageResult<>(ratings, page, size, total);
    }

    @Transactional(readOnly = true)
    public PageResult<Appeal> listPendingAppeals(int page, int size) {
        log.debug("Listing pending appeals: page={}, size={}", page, size);

        int offset = page * size;
        List<Appeal> appeals = appealMapper.findByStatus(AppealStatus.PENDING.name(), offset, size);
        long total = appealMapper.countByStatus(AppealStatus.PENDING.name());

        return new PageResult<>(appeals, page, size, total);
    }

    @Transactional(readOnly = true)
    public boolean canAcceptJob(Long courierId) {
        log.debug("Checking if courier {} can accept a job", courierId);

        CreditLevelSnapshot snapshot = creditLevelMapper.findByCourierId(courierId);
        if (snapshot == null) {
            log.warn("No credit level found for courier {}, defaulting to cannot accept", courierId);
            return false;
        }

        int activeJobs = deliveryJobMapper.countByCourierAndActiveStatuses(courierId);
        boolean canAccept = activeJobs < snapshot.getMaxConcurrent();

        log.debug("Courier {} has {} active jobs, max concurrent is {}, canAccept={}",
                courierId, activeJobs, snapshot.getMaxConcurrent(), canAccept);
        return canAccept;
    }
}
