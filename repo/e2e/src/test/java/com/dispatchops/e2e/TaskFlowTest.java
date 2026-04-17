package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests for task inbox tabs and lifecycle.
 */
class TaskFlowTest extends BaseE2ETest {

    @Test
    void todoInboxReturns200() {
        loginAsDispatcher();
        APIResponse resp = page.request().get(BASE_URL + "/api/tasks?inbox=TODO&page=0&size=10");
        Assertions.assertEquals(200, resp.status(), "TODO inbox should return 200");
    }

    @Test
    void doneInboxReturns200() {
        loginAsDispatcher();
        APIResponse resp = page.request().get(BASE_URL + "/api/tasks?inbox=DONE&page=0&size=10");
        Assertions.assertEquals(200, resp.status(), "DONE inbox should return 200");
    }

    @Test
    void ccInboxReturns200() {
        loginAsDispatcher();
        APIResponse resp = page.request().get(BASE_URL + "/api/tasks?inbox=CC&page=0&size=10");
        Assertions.assertEquals(200, resp.status(), "CC inbox should return 200");
    }

    @Test
    void createTaskReturns201() {
        loginAsDispatcher();
        String due = java.time.LocalDateTime.now().plusDays(1).withNano(0).toString();
        APIResponse resp = page.request().post(BASE_URL + "/api/tasks",
                postOptions("{\"title\":\"Test Task\",\"body\":\"Task body\",\"assigneeId\":3,\"dueTime\":\"" + due + "\"}"));
        Assertions.assertEquals(201, resp.status(), "Task creation should return 201");
    }

    @Test
    void courierCannotCreateTask() {
        loginAsCourier();
        String due = java.time.LocalDateTime.now().plusDays(1).withNano(0).toString();
        APIResponse resp = page.request().post(BASE_URL + "/api/tasks",
                postOptions("{\"title\":\"Test\",\"body\":\"Body\",\"assigneeId\":3,\"dueTime\":\"" + due + "\"}"));
        Assertions.assertEquals(403, resp.status(), "Courier must be denied task creation");
    }

    @Test
    void calendarEndpointReturns200() {
        loginAsDispatcher();
        APIResponse resp = page.request().get(BASE_URL + "/api/tasks/calendar?from=2026-01-01&to=2026-12-31");
        Assertions.assertEquals(200, resp.status(), "Calendar endpoint should return 200");
    }
}
