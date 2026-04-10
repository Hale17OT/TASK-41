package com.dispatchops.application.service;

import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.Notification;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.infrastructure.notification.LanRelayChannel;
import com.dispatchops.infrastructure.persistence.mapper.LanRelaySubMapper;
import com.dispatchops.infrastructure.persistence.mapper.NotificationMapper;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.async.DeferredResult;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationMapper notificationMapper;
    private final LanRelaySubMapper lanRelaySubMapper;
    private final LanRelayChannel lanRelayChannel;

    @Value("${notification.poll.timeout-seconds:30}")
    private int pollTimeout;

    private final ConcurrentHashMap<Long, List<DeferredResult<ApiResult<?>>>> pendingPolls =
            new ConcurrentHashMap<>();

    public NotificationService(NotificationMapper notificationMapper,
                               LanRelaySubMapper lanRelaySubMapper,
                               LanRelayChannel lanRelayChannel) {
        this.notificationMapper = notificationMapper;
        this.lanRelaySubMapper = lanRelaySubMapper;
        this.lanRelayChannel = lanRelayChannel;
    }

    public void notify(Long recipientId, NotificationType type, String title,
                       String body, String linkType, Long linkId) {
        log.debug("Creating notification for recipientId={}, type={}, title='{}'",
                recipientId, type, title);

        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setBody(body);
        notification.setLinkType(linkType);
        notification.setLinkId(linkId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());

        notificationMapper.insert(notification);
        log.info("Notification id={} created for recipientId={}", notification.getId(), recipientId);

        // Relay to LAN if enabled (fail-safe, non-blocking)
        try {
            lanRelayChannel.relay(notification);
        } catch (Exception e) {
            log.warn("LAN relay failed for notification id={}: {}", notification.getId(), e.getMessage());
        }

        resolvePendingPolls(recipientId, Collections.singletonList(notification));
    }

    public void notifyAll(List<Long> recipientIds, NotificationType type, String title,
                          String body, String linkType, Long linkId) {
        log.debug("Sending notification to {} recipients, type={}, title='{}'",
                recipientIds.size(), type, title);

        for (Long recipientId : recipientIds) {
            notify(recipientId, type, title, body, linkType, linkId);
        }
    }

    @Transactional(readOnly = true)
    public PageResult<Notification> getInbox(Long userId, Boolean readFilter, int page, int size) {
        log.debug("Fetching inbox for userId={}, readFilter={}, page={}, size={}",
                userId, readFilter, page, size);

        int offset = page * size;

        List<Notification> notifications;
        long total;

        if (readFilter != null) {
            if (readFilter) {
                notifications = notificationMapper.findReadByRecipientId(userId, offset, size);
                total = notificationMapper.countReadByRecipientId(userId);
            } else {
                notifications = notificationMapper.findUnreadByRecipientIdPaged(userId, offset, size);
                total = notificationMapper.countUnread(userId);
            }
        } else {
            notifications = notificationMapper.findByRecipientId(userId, offset, size);
            total = notificationMapper.countByRecipientId(userId);
        }

        return new PageResult<>(notifications, page, size, total);
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        log.debug("Fetching unread count for userId={}", userId);
        return notificationMapper.countUnread(userId);
    }

    public void markAsRead(Long userId, Long notificationId) {
        log.debug("Marking notification id={} as read for userId={}", notificationId, userId);

        Notification notification = notificationMapper.findById(notificationId);
        if (notification == null) {
            throw new ResourceNotFoundException(
                    "Notification not found with id: " + notificationId,
                    "Notification", notificationId);
        }

        if (!notification.getRecipientId().equals(userId)) {
            throw new PermissionDeniedException(
                    "Notification " + notificationId + " does not belong to user " + userId);
        }

        notificationMapper.markRead(notificationId);
        log.info("Notification id={} marked as read", notificationId);
    }

    public void markAllAsRead(Long userId) {
        log.debug("Marking all notifications as read for userId={}", userId);
        int count = notificationMapper.markAllRead(userId);
        log.info("Marked {} notifications as read for userId={}", count, userId);
    }

    public void registerPoll(Long userId, DeferredResult<ApiResult<?>> result) {
        log.debug("Registering long-poll for userId={}, timeout={}s", userId, pollTimeout);

        List<DeferredResult<ApiResult<?>>> userPolls =
                pendingPolls.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        userPolls.add(result);

        result.onTimeout(() -> {
            log.debug("Long-poll timed out for userId={}", userId);
            result.setResult(ApiResult.success(Collections.emptyList()));
            List<DeferredResult<ApiResult<?>>> polls = pendingPolls.get(userId);
            if (polls != null) {
                polls.remove(result);
                if (polls.isEmpty()) {
                    pendingPolls.remove(userId);
                }
            }
        });

        result.onCompletion(() -> {
            List<DeferredResult<ApiResult<?>>> polls = pendingPolls.get(userId);
            if (polls != null) {
                polls.remove(result);
                if (polls.isEmpty()) {
                    pendingPolls.remove(userId);
                }
            }
        });
    }

    public void resolvePendingPolls(Long userId, List<Notification> newNotifications) {
        List<DeferredResult<ApiResult<?>>> userPolls = pendingPolls.remove(userId);
        if (userPolls == null || userPolls.isEmpty()) {
            log.debug("No pending polls for userId={}", userId);
            return;
        }

        log.debug("Resolving {} pending poll(s) for userId={} with {} notification(s)",
                userPolls.size(), userId, newNotifications.size());

        for (DeferredResult<ApiResult<?>> poll : userPolls) {
            poll.setResult(ApiResult.success(newNotifications));
        }
    }
}
