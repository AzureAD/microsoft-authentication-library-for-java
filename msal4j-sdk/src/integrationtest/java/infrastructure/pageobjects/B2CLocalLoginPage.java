// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure.pageobjects;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Page Object Model for B2C Local Account login page.
 * Represents the Azure AD B2C local account authentication flow.
 */
public class B2CLocalLoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(B2CLocalLoginPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Element locators
    private static final By LOCAL_ACCOUNT_BUTTON = By.id("SignInWithLogonNameExchange");
    private static final By USERNAME_INPUT = By.id("cred_userid_inputtext");
    private static final By PASSWORD_INPUT = By.id("cred_password_inputtext");
    private static final By SIGN_IN_BUTTON = By.id("cred_sign_in_button");

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    public B2CLocalLoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
    }

    /**
     * Click the local account sign-in option.
     *
     * @return This page object for method chaining
     */
    public B2CLocalLoginPage clickLocalAccount() {
        LOG.info("Clicking local account button");
        wait.until(ExpectedConditions.elementToBeClickable(LOCAL_ACCOUNT_BUTTON))
                .click();
        return this;
    }

    /**
     * Enter the username in the B2C login page.
     *
     * @param username The username to enter
     * @return This page object for method chaining
     */
    public B2CLocalLoginPage enterUsername(String username) {
        LOG.info("Entering username: {}", username);
        wait.until(ExpectedConditions.elementToBeClickable(USERNAME_INPUT))
                .sendKeys(username);
        return this;
    }

    /**
     * Enter the password in the B2C login page.
     *
     * @param password The password to enter
     * @return This page object for method chaining
     */
    public B2CLocalLoginPage enterPassword(String password) {
        LOG.info("Entering password");
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_INPUT))
                .sendKeys(password);
        return this;
    }

    /**
     * Click the Sign in button to complete login.
     *
     * @return This page object for method chaining
     */
    public B2CLocalLoginPage clickSignIn() {
        LOG.info("Clicking sign in button");
        wait.until(ExpectedConditions.elementToBeClickable(SIGN_IN_BUTTON))
                .click();
        return this;
    }

    /**
     * Perform a complete B2C local account login flow.
     * This is a convenience method that chains all the necessary steps.
     *
     * @param username The username
     * @param password The password
     */
    public void login(String username, String password) {
        clickLocalAccount()
                .enterUsername(username)
                .enterPassword(password)
                .clickSignIn();

        LOG.info("B2C local login completed");
    }
}
