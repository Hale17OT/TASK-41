package com.dispatchops.domain.service;

import com.dispatchops.domain.exception.StaleTransitionException;
import com.dispatchops.domain.model.enums.TaskStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for task EXCEPTION status transitions.
 */
class TaskExceptionTransitionTest {

    @Test
    void todoToExceptionIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(
                TaskStatus.TODO, TaskStatus.EXCEPTION));
    }

    @Test
    void inProgressToExceptionIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(
                TaskStatus.IN_PROGRESS, TaskStatus.EXCEPTION));
    }

    @Test
    void blockedToExceptionIsValid() {
        assertDoesNotThrow(() -> StatusTransitionValidator.validateTaskTransition(
                TaskStatus.BLOCKED, TaskStatus.EXCEPTION));
    }

    @Test
    void exceptionIsTerminal() {
        assertTrue(StatusTransitionValidator.getAllowedTaskTransitions(TaskStatus.EXCEPTION).isEmpty());
    }

    @Test
    void exceptionToTodoIsInvalid() {
        assertThrows(StaleTransitionException.class, () ->
                StatusTransitionValidator.validateTaskTransition(TaskStatus.EXCEPTION, TaskStatus.TODO));
    }

    @Test
    void doneToExceptionIsInvalid() {
        assertThrows(StaleTransitionException.class, () ->
                StatusTransitionValidator.validateTaskTransition(TaskStatus.DONE, TaskStatus.EXCEPTION));
    }
}
