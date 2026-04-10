package com.dispatchops.web.controller;

import com.dispatchops.application.service.NotificationService;
import com.dispatchops.domain.model.Notification;
import com.dispatchops.domain.model.User;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<ApiResult<PageResult<Notification>>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(required = false) Boolean read,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting notification inbox for user '{}' - page: {}, size: {}, read: {}",
                currentUser.getUsername(), page, size, read);
        PageResult<Notification> result = notificationService.getInbox(currentUser.getId(), read, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResult<Map<String, Integer>>> getUnreadCount(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting unread count for user '{}'", currentUser.getUsername());
        int count = notificationService.getUnreadCount(currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(Map.of("count", count)));
    }

    @GetMapping("/poll")
    public DeferredResult<ApiResult<?>> longPoll(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Long poll registered for user '{}'", currentUser.getUsername());

        DeferredResult<ApiResult<?>> result = new DeferredResult<>(30000L);
        notificationService.registerPoll(currentUser.getId(), result);
        return result;
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResult<Void>> markAsRead(
            @PathVariable Long id,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' marking notification {} as read", currentUser.getUsername(), id);
        notificationService.markAsRead(currentUser.getId(), id);
        return ResponseEntity.ok(ApiResult.success());
    }

    @PutMapping("/read-all")
    public ResponseEntity<ApiResult<Void>> markAllRead(HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' marking all notifications as read", currentUser.getUsername());
        notificationService.markAllAsRead(currentUser.getId());
        return ResponseEntity.ok(ApiResult.success());
    }
}
