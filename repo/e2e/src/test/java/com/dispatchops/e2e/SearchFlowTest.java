package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * E2E tests for search functionality.
 */
class SearchFlowTest extends BaseE2ETest {

    @Test
    void searchPageLoads() {
        loginAsAdmin();
        page.navigate(BASE_URL + "/search?q=delivery");
        assertThat(page.locator("#search-results")).isVisible();
    }

    @Test
    void searchApiReturnsResults() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search?q=test&page=0&size=10");
        Assertions.assertEquals(200, resp.status());
    }

    @Test
    void trendingEndpointWorks() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search/trending?topN=5");
        Assertions.assertEquals(200, resp.status());
    }

    @Test
    void suggestEndpointWorks() {
        loginAsAdmin();
        APIResponse resp = page.request().get(BASE_URL + "/api/search/suggest?q=parcel");
        Assertions.assertEquals(200, resp.status());
    }
}
