package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * E2E tests verifying search indexing after entity creation/update.
 * Uses unique tokens to ensure deterministic assertions.
 */
class SearchIntegrationFlowTest extends BaseE2ETest {

    @Test
    void createdJobAppearsInSearch() {
        loginAsDispatcher();
        String unique = "JOBSRCH" + System.currentTimeMillis();
        APIResponse createResp = page.request().post(BASE_URL + "/api/jobs",
                postOptions("{\"senderName\":\"W\",\"senderAddress\":\"A\",\"receiverName\":\"" + unique +
                        "\",\"receiverAddress\":\"B\",\"receiverState\":\"CA\",\"receiverZip\":\"90210\"," +
                        "\"weightLbs\":5.0,\"orderAmount\":50.0}"));
        Assertions.assertEquals(200, createResp.status());

        APIResponse searchResp = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchResp.status());
        Assertions.assertTrue(searchResp.text().contains(unique),
                "Search must contain the exact unique job receiver name: " + unique);
    }

    @Test
    void createdTaskAppearsInSearch() {
        loginAsDispatcher();
        String unique = "TSKSRCH" + System.currentTimeMillis();
        APIResponse createResp = page.request().post(BASE_URL + "/api/tasks",
                postOptions("{\"title\":\"" + unique + "\",\"body\":\"Body\",\"assigneeId\":3}"));
        Assertions.assertEquals(201, createResp.status());

        APIResponse searchResp = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchResp.status());
        Assertions.assertTrue(searchResp.text().contains(unique),
                "Search must contain the exact unique task title: " + unique);
    }

    @Test
    void createdUserAppearsInSearch() {
        loginAsAdmin();
        String unique = "USRSRCH" + System.currentTimeMillis();
        APIResponse createResp = page.request().post(BASE_URL + "/api/users",
                postOptions("{\"username\":\"" + unique.toLowerCase() + "\",\"password\":\"TestPass123!\"," +
                        "\"role\":\"DISPATCHER\",\"displayName\":\"" + unique + "\",\"email\":\"" + unique + "@test.com\"}"));
        Assertions.assertEquals(201, createResp.status());

        APIResponse searchResp = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchResp.status());
        Assertions.assertTrue(searchResp.text().contains(unique),
                "Search must contain the exact unique user displayName: " + unique);
    }

    @Test
    void synonymSuggestionsContainExpectedTerms() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search/suggest?q=parcel");
        Assertions.assertEquals(200, resp.status());
        Assertions.assertTrue(resp.text().contains("package"),
                "Synonym for 'parcel' must include 'package'");
    }

    @Test
    void searchWithTypeFilterReturns200() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search?q=test&type=JOB");
        Assertions.assertEquals(200, resp.status());
        Assertions.assertTrue(resp.text().contains("\"content\""), "Should return paginated content");
    }

    @Test
    void searchWithDateFilterReturns200() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search?q=test&dateFrom=2026-01-01&dateTo=2026-12-31");
        Assertions.assertEquals(200, resp.status());
    }

    @Test
    void createdPaymentAppearsInSearch() {
        loginAsDispatcher();
        // Create a job first, parse its ID from response
        String jobUnique = "PAYJOB" + System.currentTimeMillis();
        APIResponse jobResp = page.request().post(BASE_URL + "/api/jobs",
                postOptions("{\"senderName\":\"W\",\"senderAddress\":\"A\",\"receiverName\":\"" + jobUnique +
                        "\",\"receiverAddress\":\"B\",\"receiverState\":\"CA\",\"receiverZip\":\"90210\"," +
                        "\"weightLbs\":5.0,\"orderAmount\":50.0}"));
        Assertions.assertEquals(200, jobResp.status());
        // Extract job ID from response
        String jobBody = jobResp.text();
        int idStart = jobBody.indexOf("\"id\":") + 5;
        int idEnd = jobBody.indexOf(",", idStart);
        String jobId = jobBody.substring(idStart, idEnd).trim();

        // Create payment referencing the actual job ID
        String payKey = "PAYSRCH" + System.currentTimeMillis();
        APIResponse payResp = page.request().post(BASE_URL + "/api/payments",
                postOptions("{\"idempotencyKey\":\"" + payKey + "\",\"jobId\":" + jobId + ",\"amount\":25.00,\"method\":\"CASH\"}"));
        Assertions.assertTrue(payResp.status() == 200 || payResp.status() == 201);

        // Search for payment by its unique idempotency key
        APIResponse searchResp = page.request().get(BASE_URL + "/api/search?q=" + payKey);
        Assertions.assertEquals(200, searchResp.status());
        Assertions.assertTrue(searchResp.text().contains(payKey),
                "Search must find payment by idempotency key: " + payKey);
    }

    @Test
    void deactivatedUserDisappearsFromSearch() {
        loginAsAdmin();
        // Create a user with unique name
        String unique = "DEACTSRCH" + System.currentTimeMillis();
        APIResponse createResp = page.request().post(BASE_URL + "/api/users",
                postOptions("{\"username\":\"" + unique.toLowerCase() + "\",\"password\":\"TestPass123!\"," +
                        "\"role\":\"DISPATCHER\",\"displayName\":\"" + unique + "\",\"email\":\"" + unique + "@test.com\"}"));
        Assertions.assertEquals(201, createResp.status());
        // Extract user ID
        String body = createResp.text();
        int idStart = body.indexOf("\"id\":") + 5;
        int idEnd = body.indexOf(",", idStart);
        String userId = body.substring(idStart, idEnd).trim();

        // Verify it appears in search
        APIResponse searchBefore = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchBefore.status());
        Assertions.assertTrue(searchBefore.text().contains(unique), "User should be searchable before deactivation");

        // Deactivate the user
        APIResponse deactResp = page.request().put(BASE_URL + "/api/users/" + userId + "/deactivate",
                postOptions("{}"));
        Assertions.assertEquals(200, deactResp.status());

        // Verify it no longer appears in search
        APIResponse searchAfter = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchAfter.status());
        Assertions.assertFalse(searchAfter.text().contains(unique),
                "Deactivated user must not appear in search results");
    }

    @Test
    void updatedProfileAppearsInSearch() {
        loginAsAdmin();
        String unique = "PROFSRCH" + System.currentTimeMillis();

        // Update admin's own profile bio with unique text
        APIResponse updateResp = page.request().put(BASE_URL + "/api/profiles/2",
                postOptions("{\"bio\":\"" + unique + "\",\"displayName\":\"Admin\",\"email\":\"admin@test.com\"}"));
        Assertions.assertEquals(200, updateResp.status());

        // Search for the unique bio text
        APIResponse searchResp = page.request().get(BASE_URL + "/api/search?q=" + unique);
        Assertions.assertEquals(200, searchResp.status());
        Assertions.assertTrue(searchResp.text().contains(unique),
                "Search must find profile by updated bio text: " + unique);
    }
}
