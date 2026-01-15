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
 * Page Object Model for ADFS login page.
 * Represents the Active Directory Federation Services authentication flow.
 */
public class ADFSLoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(ADFSLoginPage.class);

    private final WebDriver driver;
    private final WebDriverWait wait;

    // Element locators
    private static final By USERNAME_INPUT = By.id("userNameInput");
    private static final By PASSWORD_INPUT = By.id("passwordInput");
    private static final By SUBMIT_BUTTON = By.id("submitButton");

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    public ADFSLoginPage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_TIMEOUT.getSeconds());
    }

    /**
     * Enter the username in the ADFS login page.
     *
     * @param username The username/UPN to enter
     * @return This page object for method chaining
     */
    public ADFSLoginPage enterUsername(String username) {
        LOG.info("Entering username: {}", username);
        wait.until(ExpectedConditions.elementToBeClickable(USERNAME_INPUT))
                .sendKeys(username);
        return this;
    }

    /**
     * Enter the password in the ADFS login page.
     *
     * @param password The password to enter
     * @return This page object for method chaining
     */
    public ADFSLoginPage enterPassword(String password) {
        LOG.info("Entering password");
        wait.until(ExpectedConditions.elementToBeClickable(PASSWORD_INPUT))
                .sendKeys(password);
        return this;
    }

    /**
     * Click the Submit button to complete login.
     *
     * @return This page object for method chaining
     */
    public ADFSLoginPage clickSubmit() {
        LOG.info("Clicking submit button");
        wait.until(ExpectedConditions.elementToBeClickable(SUBMIT_BUTTON))
                .click();
        return this;
    }

    /**
     * Perform a complete ADFS login flow.
     * This is a convenience method that chains all the necessary steps.
     *
     * @param username The username/UPN
     * @param password The password
     */
    public void login(String username, String password) {
        enterUsername(username)
                .enterPassword(password)
                .clickSubmit();

        LOG.info("ADFS login completed");
    }
}
