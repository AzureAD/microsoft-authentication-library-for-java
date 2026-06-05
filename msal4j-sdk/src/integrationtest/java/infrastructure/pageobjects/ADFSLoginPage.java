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
 * Page Object Model for ADFS login page.
 * Represents the Active Directory Federation Services authentication flow.
 * <p>
 * Uses fallback locators to handle ADFS version differences where element IDs
 * may vary between ADFS 2019, 2022, and other versions.
 */
public class ADFSLoginPage {

    private static final Logger LOG = LoggerFactory.getLogger(ADFSLoginPage.class);

    private final WebDriver driver;

    // Element locators with fallbacks for different ADFS versions
    private static final By[] USERNAME_LOCATORS = {
            By.id("userNameInput"),
            By.id("ContentPlaceHolder1_UsernameTextBox"),
            By.cssSelector("input[type='text'][name='UserName']"),
    };
    private static final By[] PASSWORD_LOCATORS = {
            By.id("passwordInput"),
            By.id("ContentPlaceHolder1_PasswordTextBox"),
            By.cssSelector("input[type='password'][name='Password']"),
    };
    private static final By[] SUBMIT_LOCATORS = {
            By.id("submitButton"),
            By.id("ContentPlaceHolder1_SubmitButton"),
            By.cssSelector("span[id='submitButton']"),
    };

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);

    public ADFSLoginPage(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * Enter the username in the ADFS login page.
     *
     * @param username The username/UPN to enter
     * @return This page object for method chaining
     */
    public ADFSLoginPage enterUsername(String username) {
        LOG.info("Entering username: {}", username);
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, USERNAME_LOCATORS)
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
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, PASSWORD_LOCATORS)
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
        SeleniumExtensions.findWithFallback(driver, DEFAULT_TIMEOUT, SUBMIT_LOCATORS)
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
