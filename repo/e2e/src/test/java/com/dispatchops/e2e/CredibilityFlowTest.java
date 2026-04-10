package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests for credibility with strict body assertions.
 */
class CredibilityFlowTest extends BaseE2ETest {

    @Test
    void getCreditLevelReturns200WithLevel() {
        loginAsCourier();
        APIResponse resp = page.request().get(BASE_URL + "/api/credibility/credit/5");
        Assertions.assertEquals(200, resp.status());
        String body = resp.text();
        Assertions.assertTrue(body.contains("\"level\""), "Response should contain credit level");
        Assertions.assertTrue(body.contains("\"maxConcurrent\""), "Response should contain maxConcurrent");
    }

    @Test
    void courierDeniedOtherCreditLevel_returns403() {
        loginAsCourier();
        APIResponse resp = page.request().get(BASE_URL + "/api/credibility/credit/6");
        Assertions.assertEquals(403, resp.status());
        Assertions.assertTrue(resp.text().contains("own credit level"), "Body should explain denial");
    }

    @Test
    void appealOnNonExistentViolation_returns404() {
        loginAsCourier();
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/appeals",
                postOptions("{\"violationId\":99999,\"reason\":\"This violation does not exist in the system.\"}"));
        Assertions.assertEquals(404, resp.status());
        Assertions.assertTrue(resp.text().contains("not found"), "Body should indicate not found");
    }

    @Test
    void dispatcherDeniedAppealFiling_returns403() {
        loginAsDispatcher();
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/appeals",
                postOptions("{\"violationId\":1,\"reason\":\"test\"}"));
        Assertions.assertEquals(403, resp.status());
    }

    @Test
    void viewCreditLevelPage() {
        loginAsCourier();
        page.navigate(BASE_URL + "/credibility");
        page.waitForSelector(".credit-badge", new Page.WaitForSelectorOptions().setTimeout(5000));
    }
}
