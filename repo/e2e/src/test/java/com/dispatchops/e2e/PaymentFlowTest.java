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
        page.navigate(BASE_URL + "/payments");
        page.click("input[name='method'][value='CASH']");
        page.fill("#payment-order", "1");
        page.fill("#payment-amount", "100.00");
        page.click("#btn-submit-payment");
        page.waitForSelector(".toast-success", new Page.WaitForSelectorOptions().setTimeout(5000));
    }

    @Test
    void idempotencyPreventsDoublePosting() {
        loginAsDispatcher();
        String key = java.util.UUID.randomUUID().toString();

        APIResponse resp1 = page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + key + "\",\"jobId\":1,\"amount\":50.00,\"method\":\"CASH\"}"));
        APIResponse resp2 = page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + key + "\",\"jobId\":1,\"amount\":50.00,\"method\":\"CASH\"}"));

        Assertions.assertTrue(resp1.status() == 200 || resp1.status() == 201);
        Assertions.assertEquals(200, resp2.status());
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
