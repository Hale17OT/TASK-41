package com.dispatchops.application.service;

import com.dispatchops.domain.exception.OptimisticLockException;
import com.dispatchops.domain.exception.ResourceNotFoundException;
import com.dispatchops.domain.model.InternalTask;
import com.dispatchops.domain.model.TaskComment;
import com.dispatchops.domain.model.TaskRecipient;
import com.dispatchops.domain.model.User;
import com.dispatchops.domain.model.enums.InboxType;
import com.dispatchops.domain.model.enums.NotificationType;
import com.dispatchops.domain.model.enums.TaskStatus;
import com.dispatchops.domain.service.StatusTransitionValidator;
import com.dispatchops.infrastructure.persistence.mapper.DeliveryJobMapper;
import com.dispatchops.infrastructure.persistence.mapper.InternalTaskMapper;
import com.dispatchops.infrastructure.persistence.mapper.TaskCommentMapper;
import com.dispatchops.infrastructure.persistence.mapper.TaskRecipientMapper;
import com.dispatchops.infrastructure.persistence.mapper.UserMapper;
import com.dispatchops.web.dto.PageResult;
import com.dispatchops.web.dto.TaskCreateDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    private final InternalTaskMapper internalTaskMapper;
    private final TaskRecipientMapper taskRecipientMapper;
    private final TaskCommentMapper taskCommentMapper;
    private final NotificationService notificationService;
    private final DeliveryJobMapper deliveryJobMapper;
    private final UserMapper userMapper;
    private final SearchService searchService;

    public TaskService(InternalTaskMapper internalTaskMapper,
                       TaskRecipientMapper taskRecipientMapper,
                       TaskCommentMapper taskCommentMapper,
                       NotificationService notificationService,
                       DeliveryJobMapper deliveryJobMapper,
                       UserMapper userMapper,
                       SearchService searchService) {
        this.internalTaskMapper = internalTaskMapper;
        this.taskRecipientMapper = taskRecipientMapper;
        this.taskCommentMapper = taskCommentMapper;
        this.notificationService = notificationService;
        this.deliveryJobMapper = deliveryJobMapper;
        this.userMapper = userMapper;
        this.searchService = searchService;
    }

    private void indexTask(InternalTask task) {
        try {
            searchService.indexEntity("TASK", task.getId(), task.getTitle(),
                    task.getBody() != null ? task.getBody() : "",
                    task.getStatus() != null ? task.getStatus().name() : "", task.getCreatorId());
        } catch (Exception e) {
            log.warn("Failed to index task {}: {}", task.getId(), e.getMessage());
        }
    }

    public InternalTask createTask(TaskCreateDTO dto, Long creatorId) {
        log.info("Creating task '{}' by creatorId={}", dto.getTitle(), creatorId);

        if (dto.getJobId() != null) {
            if (deliveryJobMapper.findById(dto.getJobId()) == null) {
                throw new ResourceNotFoundException(
                        "Delivery job not found with id: " + dto.getJobId(), "DeliveryJob", dto.getJobId());
            }
        }

        InternalTask task = new InternalTask();
        task.setTitle(dto.getTitle());
        task.setBody(dto.getBody());
        task.setStatus(TaskStatus.TODO);
        task.setCreatorId(creatorId);
        task.setAssigneeId(dto.getAssigneeId());
        task.setDueTime(dto.getDueTime());
        task.setShowOnCalendar(dto.isShowOnCalendar());
        task.setJobId(dto.getJobId());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setVersion(0);

        internalTaskMapper.insert(task);
        log.info("Task inserted with id={}", task.getId());

        TaskRecipient assigneeRecipient = new TaskRecipient();
        assigneeRecipient.setTaskId(task.getId());
        assigneeRecipient.setUserId(dto.getAssigneeId());
        assigneeRecipient.setInboxType(InboxType.TODO);
        assigneeRecipient.setCreatedAt(LocalDateTime.now());
        taskRecipientMapper.insert(assigneeRecipient);

        if (dto.getCcUserIds() != null) {
            for (Long ccUserId : dto.getCcUserIds()) {
                TaskRecipient ccRecipient = new TaskRecipient();
                ccRecipient.setTaskId(task.getId());
                ccRecipient.setUserId(ccUserId);
                ccRecipient.setInboxType(InboxType.CC);
                ccRecipient.setCreatedAt(LocalDateTime.now());
                taskRecipientMapper.insert(ccRecipient);
            }
        }

        notificationService.notify(
                dto.getAssigneeId(),
                NotificationType.TASK_ASSIGNED,
                "New Task Assigned",
                "You have been assigned task: " + dto.getTitle(),
                "InternalTask",
                task.getId());

        log.info("Task id={} created successfully with {} CC recipients",
                task.getId(), dto.getCcUserIds() != null ? dto.getCcUserIds().size() : 0);
        indexTask(task);
        return task;
    }

    public InternalTask transitionStatus(Long taskId, String newStatusStr, String comment,
                                          int expectedVersion, Long actorId) {
        log.info("Transitioning status for taskId={} to {} by actorId={}", taskId, newStatusStr, actorId);

        InternalTask task = internalTaskMapper.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId, "InternalTask", taskId);
        }

        TaskStatus newStatus = TaskStatus.valueOf(newStatusStr);
        TaskStatus oldStatus = task.getStatus();

        StatusTransitionValidator.validateTaskTransition(oldStatus, newStatus);

        if ((newStatus == TaskStatus.BLOCKED || newStatus == TaskStatus.EXCEPTION)
                && (comment == null || comment.trim().length() < 10)) {
            throw new IllegalArgumentException(
                    "Comment of at least 10 characters required for " + newStatus + " status");
        }

        int rowsUpdated = internalTaskMapper.updateStatus(taskId, newStatusStr, expectedVersion);
        if (rowsUpdated == 0) {
            throw new OptimisticLockException(
                    "Task was modified by another transaction",
                    "InternalTask", taskId, expectedVersion);
        }

        if (newStatus == TaskStatus.DONE) {
            taskRecipientMapper.updateInboxType(taskId, task.getAssigneeId(),
                    InboxType.TODO.name(), InboxType.DONE.name());
        }

        if (comment != null && !comment.isBlank()) {
            TaskComment taskComment = new TaskComment();
            taskComment.setTaskId(taskId);
            taskComment.setAuthorId(actorId);
            taskComment.setBody("[Status: " + oldStatus + " -> " + newStatus + "] " + comment);
            taskComment.setCreatedAt(LocalDateTime.now());
            taskCommentMapper.insert(taskComment);
        }

        task.setStatus(newStatus);

        List<TaskRecipient> recipients = taskRecipientMapper.findByTaskId(taskId);
        for (TaskRecipient recipient : recipients) {
            if (!recipient.getUserId().equals(actorId)) {
                notificationService.notify(
                        recipient.getUserId(),
                        NotificationType.STATUS_CHANGE,
                        "Task Status Updated",
                        "Task '" + task.getTitle() + "' status changed from " + oldStatus + " to " + newStatus,
                        "InternalTask",
                        taskId);
            }
        }

        log.info("Task {} transitioned from {} to {}", taskId, oldStatus, newStatus);
        indexTask(task);
        return task;
    }

    public TaskComment addComment(Long taskId, String body, Long authorId) {
        log.info("Adding comment to taskId={} by authorId={}", taskId, authorId);

        InternalTask task = internalTaskMapper.findById(taskId);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id: " + taskId, "InternalTask", taskId);
        }

        TaskComment comment = new TaskComment();
        comment.setTaskId(taskId);
        comment.setAuthorId(authorId);
        comment.setBody(body);
        comment.setCreatedAt(LocalDateTime.now());
        taskCommentMapper.insert(comment);
        log.info("Comment id={} added to task {}", comment.getId(), taskId);

        Matcher matcher = MENTION_PATTERN.matcher(body);
        List<String> mentionedUsernames = new ArrayList<>();
        while (matcher.find()) {
            mentionedUsernames.add(matcher.group(1));
        }

        for (String username : mentionedUsernames) {
            User mentionedUser = userMapper.findByUsername(username);
            if (mentionedUser != null) {
                notificationService.notify(
                        mentionedUser.getId(),
                        NotificationType.MENTION,
                        "You were mentioned",
                        "You were mentioned in a comment on task: " + task.getTitle(),
                        "InternalTask",
                        taskId);
                log.debug("Mention notification sent to user '{}'", username);
            }
        }

        return comment;
    }

    @Transactional(readOnly = true)
    public PageResult<InternalTask> getInbox(Long userId, String inboxType, int page, int size) {
        log.debug("Fetching inbox for userId={}, inboxType={}, page={}, size={}", userId, inboxType, page, size);

        int offset = page * size;
        List<TaskRecipient> recipients = taskRecipientMapper.findByUserAndInboxType(userId, inboxType, offset, size);
        int total = taskRecipientMapper.countByUserAndInboxType(userId, inboxType);

        List<InternalTask> tasks = new ArrayList<>();
        for (TaskRecipient recipient : recipients) {
            InternalTask task = internalTaskMapper.findById(recipient.getTaskId());
            if (task != null) {
                tasks.add(task);
            }
        }

        return new PageResult<>(tasks, page, size, total);
    }

    @Transactional(readOnly = true)
    public InternalTask getTask(Long id) {
        log.debug("Fetching task with id={}", id);
        InternalTask task = internalTaskMapper.findById(id);
        if (task == null) {
            throw new ResourceNotFoundException("Task not found with id: " + id, "InternalTask", id);
        }
        return task;
    }

    @Transactional(readOnly = true)
    public List<TaskComment> getComments(Long taskId) {
        log.debug("Fetching comments for taskId={}", taskId);
        return taskCommentMapper.findByTaskId(taskId);
    }

    @Transactional(readOnly = true)
    public List<InternalTask> getTasksByJobId(Long jobId) {
        log.debug("Fetching tasks for jobId={}", jobId);
        return internalTaskMapper.findByJobId(jobId);
    }

    @Transactional(readOnly = true)
    public List<InternalTask> getCalendarTasks(Long userId, LocalDateTime from, LocalDateTime to) {
        log.debug("Fetching calendar tasks for userId={}, from={}, to={}", userId, from, to);
        return internalTaskMapper.findCalendarTasks(userId, from, to);
    }

    /**
     * Checks whether the given user is a participant on the specified task.
     * A participant is the creator, the assignee, or any CC recipient.
     */
    @Transactional(readOnly = true)
    public boolean isTaskParticipant(Long taskId, Long userId) {
        InternalTask task = internalTaskMapper.findById(taskId);
        if (task == null) {
            return false;
        }
        if (userId.equals(task.getCreatorId()) || userId.equals(task.getAssigneeId())) {
            return true;
        }
        List<TaskRecipient> recipients = taskRecipientMapper.findByTaskId(taskId);
        return recipients.stream().anyMatch(r -> userId.equals(r.getUserId()));
    }
}
