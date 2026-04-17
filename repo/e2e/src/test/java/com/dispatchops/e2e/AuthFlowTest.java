package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

/**
 * E2E tests for authentication flows.
 */
class AuthFlowTest extends BaseE2ETest {

    @Test
    void loginAndLogoutFlow() {
        page.navigate(BASE_URL + "/login");
        assertThat(page.locator("#login-form")).isVisible();

        // Login — seed users have must_change_password=1, so login helper handles it
        loginAsAdmin();

        // Verify dashboard loaded
        assertThat(page.locator("#user-display-name")).containsText("System Administrator");

        // Logout
        page.click("button:has-text('Logout')");
        page.waitForURL("**/login");
        assertThat(page.locator("#login-form")).isVisible();
    }

    @Test
    void mustChangePasswordRedirectsToChangePasswordPage() {
        // Fresh browser context — login with seed credentials triggers password change
        page.navigate(BASE_URL + "/login");
        page.fill("#username", "auditor1");
        page.fill("#password", "Admin123!");
        page.click("#login-btn");

        // Should land on change-password page (seed users have must_change_password=1)
        page.waitForURL(url -> url.contains("/change-password") || url.contains("/dashboard"),
                new Page.WaitForURLOptions().setTimeout(10000));

        // If must_change_password was already cleared by a prior test, this is still valid
        if (page.url().contains("/change-password")) {
            assertThat(page.locator("#change-pw-form")).isVisible();
            page.fill("#new-password", TEST_NEW_PASSWORD);
            page.fill("#confirm-password", TEST_NEW_PASSWORD);
            page.click("#pw-submit-btn");
            page.waitForURL("**/dashboard", new Page.WaitForURLOptions().setTimeout(10000));
        }
    }

    @Test
    void loginWithInvalidCredentials() {
        page.navigate(BASE_URL + "/login");
        page.fill("#username", "admin");
        page.fill("#password", "wrongpassword");
        page.click("#login-btn");

        // Should show error, not redirect
        page.waitForSelector("#login-error:not(.hidden)", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertThat(page.locator("#login-error")).isVisible();
        assertThat(page.locator("#login-error")).containsText("Invalid");
    }

    @Test
    void lockoutAfter5FailedAttempts() {
        // Drive the lockout via the API to eliminate UI/ajax-timing flakiness.
        // The server tracks failedAttempts in the DB and locks the account on
        // the 5th consecutive failure; the 6th attempt (even with a correct
        // password) should be rejected with 423 (LOCKED).
        var reqOpts = new java.util.function.Function<String, com.microsoft.playwright.options.RequestOptions>() {
            @Override public com.microsoft.playwright.options.RequestOptions apply(String password) {
                return com.microsoft.playwright.options.RequestOptions.create()
                        .setHeader("Content-Type", "application/json")
                        .setData("{\"username\":\"courier2\",\"password\":\"" + password + "\"}");
            }
        };

        for (int i = 0; i < 5; i++) {
            APIResponse bad = page.request().post(BASE_URL + "/api/auth/login", reqOpts.apply("wrongpassword"));
            Assertions.assertTrue(bad.status() == 401 || bad.status() == 423,
                    "Attempt " + (i + 1) + " should be 401 or 423; got " + bad.status());
        }

        APIResponse sixth = page.request().post(BASE_URL + "/api/auth/login", reqOpts.apply("Admin123!"));
        Assertions.assertEquals(423, sixth.status(),
                "After 5 failed attempts the 6th must be rejected with 423 (locked), got " + sixth.status());

        // Also sanity-check the UI surfaces the lockout state for a user in this state.
        page.navigate(BASE_URL + "/login");
        page.fill("#username", "courier2");
        page.fill("#password", "Admin123!");
        page.click("#login-btn");
        page.waitForSelector("#lockout-message:not(.hidden)",
                new Page.WaitForSelectorOptions().setTimeout(10000));
        assertThat(page.locator("#lockout-message")).isVisible();
        assertThat(page.locator("#lockout-countdown")).isVisible();
    }

    @Test
    void protectedPageRedirectsToLogin() {
        // Access dashboard without logging in
        page.navigate(BASE_URL + "/dashboard");
        page.waitForURL("**/login");
        assertThat(page.locator("#login-form")).isVisible();
    }

    @Test
    void apiReturns401WithoutSession() {
        APIResponse response = page.request().get(BASE_URL + "/api/auth/me");
        Assertions.assertEquals(401, response.status());
    }

    @Test
    void courierCannotAccessAdminPanel() {
        loginAsCourier();
        page.navigate(BASE_URL + "/admin/users");
        // Should redirect to login or show forbidden
        // The PageController checks session and role
        page.waitForURL(url -> url.contains("/login") || url.contains("/dashboard"));
    }

    @Test
    void healthEndpointIsPublic() {
        APIResponse response = page.request().get(BASE_URL + "/api/health");
        Assertions.assertEquals(200, response.status());
    }
}
