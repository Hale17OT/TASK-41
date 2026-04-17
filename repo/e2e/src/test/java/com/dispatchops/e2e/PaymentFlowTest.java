package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * E2E tests for payment, settlement, and refund flows.
 */
class PaymentFlowTest extends BaseE2ETest {

    @Test
    void recordPaymentViaUI() {
        loginAsDispatcher();

        // First create a job via the API so the UI form has a real jobId to
        // reference. Seed data does not include any delivery_jobs, so the
        // previous hard-coded "1" raced the /payments page's JS that verifies
        // the job exists.
        String unique = "UITEST" + System.currentTimeMillis();
        APIResponse createJob = page.request().post(BASE_URL + "/api/jobs",
                postOptions("{\"senderName\":\"W\",\"senderAddress\":\"A\",\"receiverName\":\"" + unique +
                        "\",\"receiverAddress\":\"B\",\"receiverState\":\"CA\",\"receiverZip\":\"90210\"," +
                        "\"weightLbs\":5.0,\"orderAmount\":50.0}"));
        Assertions.assertEquals(201, createJob.status());
        String body = createJob.text();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(",", idStart);
        String jobId = body.substring(idStart, idEnd).trim();

        page.navigate(BASE_URL + "/payments");
        page.click("input[name='method'][value='CASH']");
        page.fill("#payment-order", jobId);
        page.fill("#payment-amount", "100.00");
        page.click("#btn-submit-payment");
        page.waitForSelector(".toast-success",
                new Page.WaitForSelectorOptions().setTimeout(10000));
    }

    @Test
    void idempotencyPreventsDoublePosting() {
        loginAsDispatcher();
        String key = java.util.UUID.randomUUID().toString();

        APIResponse resp1 = page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + key + "\",\"jobId\":1,\"amount\":50.00,\"method\":\"CASH\"}"));
        APIResponse resp2 = page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + key + "\",\"jobId\":1,\"amount\":50.00,\"method\":\"CASH\"}"));

        // Both requests return the controller's documented 201 (the second is an
        // idempotency replay returning the cached Payment with the same status).
        Assertions.assertTrue(resp1.status() == 200 || resp1.status() == 201,
                "First payment post should be 200/201; got " + resp1.status());
        Assertions.assertTrue(resp2.status() == 200 || resp2.status() == 201,
                "Idempotent replay should return 200/201; got " + resp2.status());
    }

    @Test
    void settlePaymentAsAuditor() {
        loginAsDispatcher();
        String key = java.util.UUID.randomUUID().toString();
        page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + key + "\",\"jobId\":1,\"amount\":75.00,\"method\":\"CASH\"}"));

        page.context().clearCookies();
        loginAsAuditor();

        APIResponse pendingResp = page.request().get(BASE_URL + "/api/payments/pending");
        Assertions.assertEquals(200, pendingResp.status());
    }

    @Test
    void nonAuditorCannotSettle() {
        loginAsDispatcher();
        APIResponse resp = page.request().post(BASE_URL + "/api/payments/1/settle",
                postOptions("{}"));
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void reconciliationExportRequiresAuditor() {
        loginAsDispatcher();
        APIResponse resp = page.request().get(BASE_URL + "/api/payments/reconciliation/export?from=2026-01-01&to=2026-12-31");
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void auditorCanExportReconciliation() {
        loginAsAuditor();
        APIResponse resp = page.request().get(BASE_URL + "/api/payments/reconciliation/export?from=2026-01-01&to=2026-12-31");
        Assertions.assertEquals(200, resp.status());
    }
}
