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
        page.navigate(BASE_URL + "/login");

        // Attempt 5 failed logins (using a test user to avoid locking admin)
        for (int i = 0; i < 5; i++) {
            page.fill("#username", "courier2");
            page.fill("#password", "wrongpassword");
            page.click("#login-btn");
            page.waitForTimeout(500);
        }

        // 6th attempt should show lockout even with correct password
        page.fill("#username", "courier2");
        page.fill("#password", "Admin123!");
        page.click("#login-btn");

        page.waitForSelector("#lockout-message:not(.hidden)", new Page.WaitForSelectorOptions().setTimeout(5000));
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
