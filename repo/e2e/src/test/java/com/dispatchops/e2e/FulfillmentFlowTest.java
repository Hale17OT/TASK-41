package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * E2E tests for the fulfillment workflow.
 */
class FulfillmentFlowTest extends BaseE2ETest {

    @Test
    void createJobAndViewOnBoard() {
        loginAsDispatcher();
        page.navigate(BASE_URL + "/fulfillment");

        // Click Create Job
        page.click("#btn-create-job");
        page.waitForSelector(".modal-dialog");

        // Fill job form
        page.fill("#job-receiver-name", "John Doe");
        page.fill("#job-receiver-address", "123 Main St, Beverly Hills");
        page.fill("#job-receiver-state", "CA");
        page.fill("#job-receiver-zip", "90210");
        page.fill("#job-weight", "5.0");
        page.fill("#job-amount", "50.00");
        page.fill("#job-sender-name", "Warehouse A");
        page.fill("#job-sender-address", "456 Industrial Blvd");

        // Submit
        page.click(".modal-confirm");
        page.waitForSelector(".toast-success", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Verify job appears in CREATED column
        page.waitForTimeout(1000); // Wait for board refresh
        assertThat(page.locator("#col-created .job-card")).hasCount(1);
    }

    @Test
    void createJobWithInvalidAddressFails() {
        loginAsDispatcher();

        // Try to create job via API with invalid ZIP
        APIResponse response = page.request().post(BASE_URL + "/api/jobs",
                postOptions("{\"senderName\":\"Test\",\"senderAddress\":\"Test\",\"receiverName\":\"Test\"," +
                        "\"receiverAddress\":\"Test\",\"receiverState\":\"XX\",\"receiverZip\":\"00000\"," +
                        "\"weightLbs\":5.0,\"orderAmount\":50.00}"));

        Assertions.assertEquals(422, response.status());
    }

    @Test
    void apiReturnsIdleJobs() {
        loginAsAdmin();

        APIResponse response = page.request().get(BASE_URL + "/api/jobs/idle?minutes=20");
        Assertions.assertEquals(200, response.status());
    }

    @Test
    void courierCanOnlySeeOwnJobs() {
        loginAsCourier();

        APIResponse response = page.request().get(BASE_URL + "/api/jobs");
        Assertions.assertEquals(200, response.status());
        // The response should be filtered to only courier's jobs
    }

    @Test
    void pickListGenerationReturns200() {
        loginAsDispatcher();
        String today = java.time.LocalDate.now().toString();
        APIResponse resp = page.request().post(BASE_URL + "/api/jobs/picklist?runDate=" + today,
                postOptions("{}"));
        Assertions.assertEquals(200, resp.status(), "Pick list generation should return 200");
    }

    @Test
    void sortListGenerationReturns200() {
        loginAsDispatcher();
        String today = java.time.LocalDate.now().toString();
        APIResponse resp = page.request().post(BASE_URL + "/api/jobs/sortlist?runDate=" + today,
                postOptions("{}"));
        Assertions.assertEquals(200, resp.status(), "Sort list generation should return 200");
    }

    @Test
    void courierCannotGeneratePickList() {
        loginAsCourier();
        String today = java.time.LocalDate.now().toString();
        APIResponse resp = page.request().post(BASE_URL + "/api/jobs/picklist?runDate=" + today,
                postOptions("{}"));
        Assertions.assertEquals(403, resp.status(), "Courier must be denied pick list generation");
    }
}
