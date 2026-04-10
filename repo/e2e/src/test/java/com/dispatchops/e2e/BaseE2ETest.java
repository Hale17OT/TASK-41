package com.dispatchops.e2e;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.*;

/**
 * Base class for all E2E tests.
 * Manages Playwright lifecycle and provides login helper.
 */
public abstract class BaseE2ETest {

    static final String BASE_URL = System.getenv("E2E_BASE_URL") != null
            ? System.getenv("E2E_BASE_URL")
            : System.getenv("BASE_URL") != null
                    ? System.getenv("BASE_URL")
                    : "http://localhost:8080";

    static Playwright playwright;
    static Browser browser;
    BrowserContext context;
    Page page;

    @BeforeAll
    static void launchBrowser() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
    }

    @AfterAll
    static void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    @BeforeEach
    void createContext() {
        context = browser.newContext();
        page = context.newPage();
    }

    @AfterEach
    void closeContext() {
        if (context != null) context.close();
    }

    /** Default test password used after forced password change. */
    protected static final String TEST_NEW_PASSWORD = "TestPass123!";

    protected String csrfToken;

    /**
     * Login as a specific user. Handles the must-change-password flow
     * automatically: if redirected to /change-password, sets a new password
     * and continues to /dashboard. Also retries with TEST_NEW_PASSWORD if
     * the seed password was already changed by a prior test run.
     */
    protected void login(String username, String password) {
        page.navigate(BASE_URL + "/login");
        page.fill("#username", username);
        page.fill("#password", password);
        page.click("#login-btn");

        // Wait for navigation — could be /dashboard, /change-password, or stay on /login (wrong pw)
        page.waitForTimeout(2000);

        // If still on login (password already changed by prior test), retry with test password
        if (page.url().contains("/login") && !password.equals(TEST_NEW_PASSWORD)) {
            page.fill("#username", username);
            page.fill("#password", TEST_NEW_PASSWORD);
            page.click("#login-btn");
        }

        page.waitForURL(url -> url.contains("/dashboard") || url.contains("/change-password"),
                new Page.WaitForURLOptions().setTimeout(10000));

        // Handle forced password change if needed
        if (page.url().contains("/change-password")) {
            page.fill("#new-password", TEST_NEW_PASSWORD);
            page.fill("#confirm-password", TEST_NEW_PASSWORD);
            page.click("#pw-submit-btn");
            page.waitForURL("**/dashboard", new Page.WaitForURLOptions().setTimeout(10000));
        }

        // Extract CSRF token from meta tag for API requests
        csrfToken = page.getAttribute("meta[name='_csrf']", "content");
    }

    /**
     * Create RequestOptions with CSRF header for mutating API calls.
     */
    protected com.microsoft.playwright.options.RequestOptions postOptions(String jsonBody) {
        var opts = com.microsoft.playwright.options.RequestOptions.create()
                .setHeader("Content-Type", "application/json")
                .setData(jsonBody);
        if (csrfToken != null && !csrfToken.isBlank()) {
            opts.setHeader("X-CSRF-TOKEN", csrfToken);
        }
        return opts;
    }

    /**
     * Login with default admin credentials.
     */
    protected void loginAsAdmin() {
        login("admin", "Admin123!");
    }

    protected void loginAsDispatcher() {
        login("dispatcher1", "Admin123!");
    }

    protected void loginAsCourier() {
        login("courier1", "Admin123!");
    }

    protected void loginAsAuditor() {
        login("auditor1", "Admin123!");
    }
}
