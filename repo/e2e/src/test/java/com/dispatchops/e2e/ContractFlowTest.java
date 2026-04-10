package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests for contract template management.
 * All assertions use strict status codes and verify response body content.
 */
class ContractFlowTest extends BaseE2ETest {

    @Test
    void createTemplateAsAdmin_returns201WithId() {
        loginAsAdmin();
        APIResponse resp = page.request().post(BASE_URL + "/api/contracts/templates",
                postOptions("{\"name\":\"E2E Contract\",\"description\":\"Test\",\"body\":\"Agreement between {{company}} and {{name}}.\"}"));
        Assertions.assertEquals(201, resp.status());
        String body = resp.text();
        Assertions.assertTrue(body.contains("\"code\":200") || body.contains("\"code\":201"),
                "Response should indicate success");
        Assertions.assertTrue(body.contains("E2E Contract"), "Response should contain template name");
    }

    @Test
    void courierCannotCreateTemplate_returns403() {
        loginAsCourier();
        APIResponse resp = page.request().post(BASE_URL + "/api/contracts/templates",
                postOptions("{\"name\":\"X\",\"body\":\"Y\"}"));
        Assertions.assertEquals(403, resp.status());
        Assertions.assertTrue(resp.text().contains("403"), "Body should contain 403 code");
    }

    @Test
    void verifyIntegrityOnNonExistent_returns404WithMessage() {
        loginAsAuditor();
        APIResponse resp = page.request().get(BASE_URL + "/api/contracts/instances/99999/verify");
        Assertions.assertEquals(404, resp.status());
        Assertions.assertTrue(resp.text().contains("not found"), "Body should indicate resource not found");
    }

    @Test
    void listTemplates_returns200WithContent() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/contracts/templates");
        Assertions.assertEquals(200, resp.status());
        Assertions.assertTrue(resp.text().contains("\"content\""), "Body should contain paginated content");
    }
}
