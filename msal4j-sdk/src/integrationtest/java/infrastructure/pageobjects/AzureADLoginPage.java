// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Page Object Model for Azure AD login page.
 * Represents the standard Azure AD authentication flow.
 */
public class AzureADLoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(AzureADLoginPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Element locators
    private static final By USERNAME_INPUT = By.id("i0116");
    private static final By PASSWORD_INPUT = By.id("i0118");
    private static final By NEXT_BUTTON = By.id("idSIButton9");
    private static final By SUBMIT_BUTTON = By.id("idSIButton9");

    // Optional prompts
    private static final By ARE_YOU_TRYING_TO_SIGN_IN_BUTTON = By.id("idSIButton9");
    private static final By STAY_SIGNED_IN_NO_BUTTON = By.id("idBtn_Back");

    // Authentication complete page
    private static final By AUTH_COMPLETE_BODY = By.tagName("body");
    private static final String AUTH_COMPLETE_TEXT = "Authentication complete";

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration SHORT_TIMEOUT = Duration.ofSeconds(5);

    public AzureADLoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    /**
     * Enter the username in the login page.
     *
     * @param username The username/UPN to enter
     * @return This page object for method chaining
     */
    public AzureADLoginPage enterUsername(String username) {
        LOG.info("Entering username: {}", username);
        wait.until(ExpectedConditions.elementToBeClickable(USERNAME_INPUT))
                .sendKeys(username);
        return this;
    }

    /**
     * Click the Next button after entering username.
     *
     * @return This page object for method chaining
     */
    public AzureADLoginPage clickNext() {
        LOG.info("Clicking Next button");
        wait.until(ExpectedConditions.elementToBeClickable(NEXT_BUTTON))
                .click();
        return this;
    }

    /**
     * Enter the password in the login page.
     *
     * @param password The password to enter
     * @return This page object for method chaining
     */
    public AzureADLoginPage enterPassword(String password) {
        LOG.info("Entering password");
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_INPUT))
                .sendKeys(password);
        return this;
    }

    /**
     * Click the Submit/Sign in button after entering password.
     *
     * @return This page object for method chaining
     */
    public AzureADLoginPage clickSubmit() {
        LOG.info("Clicking Submit button");
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON))
                .click();
        return this;
    }

    /**
     * Check if the authentication complete page is displayed.
     *
     * @return true if authentication is complete, false otherwise
     */
    public boolean isAuthenticationComplete() {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, SHORT_TIMEOUT);
            shortWait.until(ExpectedConditions.textToBePresentInElementLocated(
                    AUTH_COMPLETE_BODY, AUTH_COMPLETE_TEXT));
            LOG.info("Authentication complete page detected");
            return true;
        } catch (TimeoutException ex) {
            LOG.debug("Authentication complete page not found");
            return false;
        }
    }

    /**
     * Perform a complete Azure AD login flow.
     * This is a convenience method that chains all the necessary steps.
     *
     * @param username The username/UPN
     * @param password The password
     */
    public void login(String username, String password) {
        enterUsername(username)
                .clickNext()
                .enterPassword(password)
                .clickSubmit();

        if (isAuthenticationComplete()) {
            LOG.info("Authentication completed successfully");
            return;
        }

        handleOptionalPrompts();
    }

    /**
     * Handle optional prompts that may appear after login.
     * These include "Are you trying to sign in to..." and "Stay signed in?" prompts.
     */
    private void handleOptionalPrompts() {
        handleAreYouTryingToSignInPrompt();
        handleStaySignedInPrompt();
    }

    /**
     * Handle the "Are you trying to sign in to..." prompt if it appears.
     */
    private void handleAreYouTryingToSignInPrompt() {
        try {
            LOG.info("Checking for 'Are you trying to sign in' prompt");
            WebDriverWait shortWait = new WebDriverWait(driver, SHORT_TIMEOUT);
            shortWait.until(ExpectedConditions.elementToBeClickable(ARE_YOU_TRYING_TO_SIGN_IN_BUTTON))
                    .click();
            LOG.info("Clicked Continue on 'Are you trying to sign in' prompt");
        } catch (TimeoutException ex) {
            LOG.debug("No 'Are you trying to sign in' prompt found");
        }
    }

    /**
     * Handle the "Stay signed in?" prompt if it appears.
     */
    private void handleStaySignedInPrompt() {
        try {
            LOG.info("Checking for 'Stay signed in' prompt");
            WebDriverWait shortWait = new WebDriverWait(driver, SHORT_TIMEOUT);
            shortWait.until(ExpectedConditions.elementToBeClickable(STAY_SIGNED_IN_NO_BUTTON))
                    .click();
            LOG.info("Clicked No on 'Stay signed in' prompt");
        } catch (TimeoutException ex) {
            LOG.debug("No 'Stay signed in' prompt found");
        }
    }
}
