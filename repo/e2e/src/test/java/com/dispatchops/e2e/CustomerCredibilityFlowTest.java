package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests for customer-facing credibility endpoints (no session required).
 * All assertions are strict single-status where deterministic.
 */
class CustomerCredibilityFlowTest extends BaseE2ETest {

    @Test
    void customerRatingOnNonExistentTrackingReturns422() {
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/ratings/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"DO-NONEXISTENT\",\"receiverName\":\"John\"," +
                                "\"timeliness\":5,\"attitude\":4,\"accuracy\":5,\"comment\":\"Great\"}"));
        Assertions.assertEquals(422, resp.status(), "Non-existent tracking should return 422");
    }

    @Test
    void customerRatingMissingFieldsReturns422() {
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/ratings/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"X\"}"));
        Assertions.assertEquals(422, resp.status(), "Missing fields should return 422");
    }

    @Test
    void customerAppealOnNonExistentTrackingReturns422() {
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/appeals/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"DO-NONEXISTENT\",\"receiverName\":\"John\"," +
                                "\"reason\":\"I want to appeal this rating.\"}"));
        Assertions.assertEquals(422, resp.status(), "Non-existent tracking should return 422");
    }

    @Test
    void customerAppealMissingFieldsReturns422() {
        APIResponse resp = page.request().post(BASE_URL + "/api/credibility/appeals/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"X\"}"));
        Assertions.assertEquals(422, resp.status(), "Missing fields should return 422");
    }

    @Test
    void customerEndpointsDoNotReturn401() {
        APIResponse ratingResp = page.request().post(BASE_URL + "/api/credibility/ratings/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"X\",\"receiverName\":\"Y\"," +
                                "\"timeliness\":3,\"attitude\":3,\"accuracy\":3}"));
        Assertions.assertNotEquals(401, ratingResp.status(), "Must not require session");

        APIResponse appealResp = page.request().post(BASE_URL + "/api/credibility/appeals/customer",
                com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"trackingNumber\":\"X\",\"receiverName\":\"Y\",\"reason\":\"test\"}"));
        Assertions.assertNotEquals(401, appealResp.status(), "Must not require session");
    }
}
