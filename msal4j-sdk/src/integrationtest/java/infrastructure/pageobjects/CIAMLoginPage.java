// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure.pageobjects;

import infrastructure.SeleniumExtensions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Page Object Model for CIAM (Customer Identity and Access Management) login page.
 * Represents the Azure AD CIAM authentication flow, which uses a multi-step form
 * with different element IDs than the standard Azure AD login page.
 * <p>
 * CIAM login flow:
 * 1. Enter email address
 * 2. Click Next
 * 3. Enter password
 * 4. Click Sign in
 * <p>
 * Uses fallback locators to handle variations in CIAM tenant configurations.
 */
public class CIAMLoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(CIAMLoginPage.class);

    private final WebDriver driver;

    // Username step locators
    private static final By[] USERNAME_LOCATORS = {
            By.name("username"),
            By.cssSelector("input[type='text'][autocomplete*='username']"),
            By.cssSelector("input[type='email']"),
    };
    private static final By[] NEXT_BUTTON_LOCATORS = {
            By.id("usernamePrimaryButton"),
            By.cssSelector("button[type='submit'][aria-label='Next']"),
            By.cssSelector("button.ext-primary[type='submit']"),
    };

    // Password step locators
    private static final By[] PASSWORD_LOCATORS = {
            By.name("password"),
            By.id("password"),
            By.cssSelector("input[type='password']"),
    };
    private static final By[] SIGN_IN_BUTTON_LOCATORS = {
            By.id("passwordPrimaryButton"),
            By.cssSelector("button[type='submit'][aria-label='Sign in']"),
            By.cssSelector("button.ext-primary[type='submit']"),
    };

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    public CIAMLoginPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Enter the username/email in the CIAM login page.
     *
     * @param username The email address to enter
     * @return This page object for method chaining
     */
    public CIAMLoginPage enterUsername(String username) {
        LOG.info("Entering username: {}", username);
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, USERNAME_LOCATORS)
                .sendKeys(username);
        return this;
    }

    /**
     * Click the Next button after entering the username.
     *
     * @return This page object for method chaining
     */
    public CIAMLoginPage clickNext() {
        LOG.info("Clicking Next button");
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, NEXT_BUTTON_LOCATORS)
                .click();
        return this;
    }

    /**
     * Enter the password in the CIAM login page.
     *
     * @param password The password to enter
     * @return This page object for method chaining
     */
    public CIAMLoginPage enterPassword(String password) {
        LOG.info("Entering password");
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, PASSWORD_LOCATORS)
                .sendKeys(password);
        return this;
    }

    /**
     * Click the Sign in button to complete login.
     *
     * @return This page object for method chaining
     */
    public CIAMLoginPage clickSignIn() {
        LOG.info("Clicking Sign in button");
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, SIGN_IN_BUTTON_LOCATORS)
                .click();
        return this;
    }

    /**
     * Perform a complete CIAM login flow.
     * This is a convenience method that chains all the necessary steps.
     *
     * @param username The email address
     * @param password The password
     */
    public void login(String username, String password) {
        enterUsername(username)
                .clickNext()
                .enterPassword(password)
                .clickSignIn();

        LOG.info("CIAM login completed");
    }
}
