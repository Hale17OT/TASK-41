package com.dispatchops.web.controller;

import com.dispatchops.application.service.TaskService;
import com.dispatchops.domain.exception.PermissionDeniedException;
import com.dispatchops.domain.model.InternalTask;
import com.dispatchops.domain.model.TaskComment;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.Role;
import com.dispatchops.web.annotation.RequireRole;
import com.dispatchops.web.dto.ApiResult;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.StatusTransitionDTO;
import com.dispatchops.web.dto.TaskCreateDTO;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR})
    public ResponseEntity<ApiResult<PageResult<InternalTask>>> getInbox(
            @RequestParam(defaultValue = "TODO") String inbox,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting task inbox '{}' for user '{}' - page: {}, size: {}",
                inbox, currentUser.getUsername(), page, size);
        PageResult<InternalTask> result = taskService.getInbox(currentUser.getId(), inbox, page, size);
        return ResponseEntity.ok(ApiResult.success(result));
    }

    @GetMapping("/{id}")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<InternalTask>> getTask(@PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting task with id: {}", id);
        InternalTask task = taskService.getTask(id);

        // Object-level auth: user must be creator, assignee, or CC recipient, OR have OPS_MANAGER/ADMIN role
        enforceTaskAccess(currentUser, id);

        return ResponseEntity.ok(ApiResult.success(task));
    }

    @PostMapping
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR})
    public ResponseEntity<ApiResult<InternalTask>> createTask(
            @Valid @RequestBody TaskCreateDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' creating task: {}", currentUser.getUsername(), dto.getTitle());
        InternalTask task = taskService.createTask(dto, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(task));
    }

    @PutMapping("/{id}/status")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<InternalTask>> transitionStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusTransitionDTO dto,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.info("User '{}' transitioning task {} to status {}", currentUser.getUsername(), id, dto.getStatus());

        // Object-level auth: user must be a task participant or OPS_MANAGER/ADMIN
        enforceTaskAccess(currentUser, id);

        InternalTask task = taskService.transitionStatus(
                id, dto.getStatus(), dto.getComment(), dto.getVersion(), currentUser.getId());
        return ResponseEntity.ok(ApiResult.success(task));
    }

    @PostMapping("/{id}/comments")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<TaskComment>> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        String commentBody = body.get("body");
        log.info("User '{}' adding comment to task {}", currentUser.getUsername(), id);

        // Object-level auth: user must be a task participant or OPS_MANAGER/ADMIN
        enforceTaskAccess(currentUser, id);

        TaskComment comment = taskService.addComment(id, commentBody, currentUser.getId());
        return ResponseEntity.status(201).body(ApiResult.success(comment));
    }

    @GetMapping("/{id}/comments")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<List<TaskComment>>> getComments(
            @PathVariable Long id, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting comments for task {}", id);

        // Object-level auth: user must be a task participant or OPS_MANAGER/ADMIN
        enforceTaskAccess(currentUser, id);

        List<TaskComment> comments = taskService.getComments(id);
        return ResponseEntity.ok(ApiResult.success(comments));
    }

    @GetMapping("/calendar")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR})
    public ResponseEntity<ApiResult<List<InternalTask>>> getCalendarView(
            @RequestParam String from,
            @RequestParam String to,
            HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);
        log.debug("Getting calendar view for user '{}' from {} to {}",
                currentUser.getUsername(), fromDate, toDate);

        List<InternalTask> tasks = taskService.getCalendarTasks(
                currentUser.getId(), fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay());

        return ResponseEntity.ok(ApiResult.success(tasks));
    }

    @GetMapping("/job/{jobId}")
    @RequireRole({Role.ADMIN, Role.OPS_MANAGER, Role.DISPATCHER, Role.AUDITOR, Role.COURIER})
    public ResponseEntity<ApiResult<List<InternalTask>>> getTasksByJobId(
            @PathVariable Long jobId, HttpSession session) {
        User currentUser = (User) session.getAttribute("currentUser");
        log.debug("Getting tasks for job {}", jobId);
        List<InternalTask> tasks = taskService.getTasksByJobId(jobId);

        // Object-level auth: non-privileged users only see tasks they participate in
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.OPS_MANAGER) {
            tasks = tasks.stream()
                    .filter(task -> taskService.isTaskParticipant(task.getId(), currentUser.getId()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(ApiResult.success(tasks));
    }

    /**
     * Enforces that the current user has access to the specified task.
     * Access is granted if the user has role OPS_MANAGER or ADMIN, or if
     * the user is the creator, assignee, or a CC recipient of the task.
     */
    private void enforceTaskAccess(User currentUser, Long taskId) {
        if (currentUser.getRole() == Role.ADMIN || currentUser.getRole() == Role.OPS_MANAGER) {
            return;
        }
        if (!taskService.isTaskParticipant(taskId, currentUser.getId())) {
            throw new PermissionDeniedException(
                    "User " + currentUser.getId() + " does not have access to task " + taskId);
        }
    }
}
