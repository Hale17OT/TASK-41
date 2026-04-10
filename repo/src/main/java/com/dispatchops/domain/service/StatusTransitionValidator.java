package com.dispatchops.domain.service;

import com.dispatchops.domain.exception.StaleTransitionException;
import com.dispatchops.domain.model.enums.JobStatus;
import com.dispatchops.domain.model.enums.PaymentStatus;
import com.dispatchops.domain.model.enums.TaskStatus;

import java.util.*;

/**
 * Pure domain service for validating state transitions.
 * No Spring dependency -- testable in isolation.
 */
public final class StatusTransitionValidator {

    private static final Map<JobStatus, Set<JobStatus>> JOB_TRANSITIONS;
    private static final Map<PaymentStatus, Set<PaymentStatus>> PAYMENT_TRANSITIONS;
    private static final Map<TaskStatus, Set<TaskStatus>> TASK_TRANSITIONS;

    static {
        Map<JobStatus, Set<JobStatus>> jt = new EnumMap<>(JobStatus.class);
        jt.put(JobStatus.CREATED, EnumSet.of(JobStatus.PICKED, JobStatus.EXCEPTION, JobStatus.MANUAL_VALIDATION));
        jt.put(JobStatus.MANUAL_VALIDATION, EnumSet.of(JobStatus.CREATED, JobStatus.PICKED, JobStatus.EXCEPTION));
        jt.put(JobStatus.PICKED, EnumSet.of(JobStatus.IN_TRANSIT, JobStatus.EXCEPTION));
        jt.put(JobStatus.IN_TRANSIT, EnumSet.of(JobStatus.DELIVERED, JobStatus.EXCEPTION));
        jt.put(JobStatus.DELIVERED, EnumSet.noneOf(JobStatus.class));
        jt.put(JobStatus.EXCEPTION, EnumSet.noneOf(JobStatus.class));
        JOB_TRANSITIONS = Collections.unmodifiableMap(jt);

        Map<PaymentStatus, Set<PaymentStatus>> pt = new EnumMap<>(PaymentStatus.class);
        pt.put(PaymentStatus.PENDING_SETTLEMENT, EnumSet.of(PaymentStatus.SETTLED, PaymentStatus.CANCELLED));
        pt.put(PaymentStatus.SETTLED, EnumSet.of(PaymentStatus.REFUND_PENDING));
        pt.put(PaymentStatus.REFUND_PENDING, EnumSet.of(PaymentStatus.REFUNDED));
        pt.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        pt.put(PaymentStatus.CANCELLED, EnumSet.noneOf(PaymentStatus.class));
        PAYMENT_TRANSITIONS = Collections.unmodifiableMap(pt);

        Map<TaskStatus, Set<TaskStatus>> tt = new EnumMap<>(TaskStatus.class);
        tt.put(TaskStatus.TODO, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.BLOCKED, TaskStatus.EXCEPTION, TaskStatus.DONE));
        tt.put(TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.DONE, TaskStatus.BLOCKED, TaskStatus.EXCEPTION));
        tt.put(TaskStatus.BLOCKED, EnumSet.of(TaskStatus.IN_PROGRESS, TaskStatus.TODO, TaskStatus.EXCEPTION));
        tt.put(TaskStatus.EXCEPTION, EnumSet.noneOf(TaskStatus.class));
        tt.put(TaskStatus.DONE, EnumSet.noneOf(TaskStatus.class));
        TASK_TRANSITIONS = Collections.unmodifiableMap(tt);
    }

    private StatusTransitionValidator() {}

    public static void validateJobTransition(JobStatus from, JobStatus to) {
        Set<JobStatus> allowed = JOB_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(JobStatus.class));
        if (!allowed.contains(to)) {
            throw new StaleTransitionException(
                    "Invalid job status transition: " + from + " -> " + to,
                    from.name(), to.name(),
                    allowed.stream().map(Enum::name).toList()
            );
        }
    }

    public static void validatePaymentTransition(PaymentStatus from, PaymentStatus to) {
        Set<PaymentStatus> allowed = PAYMENT_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class));
        if (!allowed.contains(to)) {
            throw new StaleTransitionException(
                    "Invalid payment status transition: " + from + " -> " + to,
                    from.name(), to.name(),
                    allowed.stream().map(Enum::name).toList()
            );
        }
    }

    public static void validateTaskTransition(TaskStatus from, TaskStatus to) {
        Set<TaskStatus> allowed = TASK_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TaskStatus.class));
        if (!allowed.contains(to)) {
            throw new StaleTransitionException(
                    "Invalid task status transition: " + from + " -> " + to,
                    from.name(), to.name(),
                    allowed.stream().map(Enum::name).toList()
            );
        }
    }

    public static Set<JobStatus> getAllowedJobTransitions(JobStatus from) {
        return JOB_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(JobStatus.class));
    }

    public static Set<PaymentStatus> getAllowedPaymentTransitions(PaymentStatus from) {
        return PAYMENT_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(PaymentStatus.class));
    }

    public static Set<TaskStatus> getAllowedTaskTransitions(TaskStatus from) {
        return TASK_TRANSITIONS.getOrDefault(from, EnumSet.noneOf(TaskStatus.class));
    }
}
