package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests for role-based access control across all endpoints.
 */
class RBACFlowTest extends BaseE2ETest {

    @Test
    void unauthenticatedGetReturns401() {
        APIResponse resp = page.request().get(BASE_URL + "/api/jobs");
        Assertions.assertEquals(401, resp.status());
    }

    @Test
    void courierCannotCreateJobs() {
        loginAsCourier();
        APIResponse resp = page.request().post(BASE_URL + "/api/jobs",
                postOptions("{\"senderName\":\"Test\",\"senderAddress\":\"Test\",\"receiverName\":\"Test\"," +
                        "\"receiverAddress\":\"Test\",\"receiverState\":\"CA\",\"receiverZip\":\"90210\"," +
                        "\"weightLbs\":5.0,\"orderAmount\":50.00}"));
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void courierCannotAccessUserManagement() {
        loginAsCourier();
        APIResponse resp = page.request().get(BASE_URL + "/api/users");
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void dispatcherCannotSettlePayments() {
        loginAsDispatcher();
        APIResponse resp = page.request().post(BASE_URL + "/api/payments/1/settle", postOptions("{}"));
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void auditorCanAccessReconciliation() {
        loginAsAuditor();
        APIResponse resp = page.request().get(BASE_URL + "/api/payments/reconciliation");
        Assertions.assertEquals(200, resp.status());
    }

    @Test
    void adminCanAccessEverything() {
        loginAsAdmin();

        Assertions.assertEquals(200, page.request().get(BASE_URL + "/api/jobs").status());
        Assertions.assertEquals(200, page.request().get(BASE_URL + "/api/users").status());
        Assertions.assertEquals(200, page.request().get(BASE_URL + "/api/payments/pending").status());
        Assertions.assertEquals(200, page.request().get(BASE_URL + "/api/credibility/appeals").status());
    }

    @Test
    void opsManagerCanManageCredibility() {
        login("ops_manager", "Admin123!");
        APIResponse resp = page.request().get(BASE_URL + "/api/credibility/appeals");
        Assertions.assertEquals(200, resp.status());
    }
}
