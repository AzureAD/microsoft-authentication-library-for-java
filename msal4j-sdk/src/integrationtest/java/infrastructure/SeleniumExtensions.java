// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import com.microsoft.aad.msal4j.TestConstants;
import com.microsoft.aad.msal4j.labapi2.LabUser;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class SeleniumExtensions {

    private static final Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration COMMON_ELEMENT_TIMEOUT = Duration.ofSeconds(5);

    private SeleniumExtensions() {
    }

    public static WebDriver createDefaultWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--incognito");

        System.setProperty("webdriver.chrome.driver", "C:/Windows/chromedriver.exe");
        return new ChromeDriver(options);
    }

    public static WebElement waitForElementToBeVisibleAndEnabled(WebDriver driver, By by, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout.getSeconds());
        return wait.until(dr -> {
            try {
                WebElement element = driver.findElement(by);
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
                return null;
            } catch (StaleElementReferenceException e) {
                LOG.debug("Stale element in waitForElementToBeVisibleAndEnabled: {}", e.getMessage());
                return null;
            }
        });
    }

    public static WebElement waitForElementToBeVisibleAndEnabled(WebDriver driver, By by) {
        return waitForElementToBeVisibleAndEnabled(driver, by, DEFAULT_TIMEOUT);
    }

    public static void performADOrCiamLogin(WebDriver driver, LabUser user) {
        LOG.info("performADOrCiamLogin for user: {}", user.getUpn());

        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Entering username");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getAadUserNameInputId()))
                .sendKeys(user.getUpn());

        LOG.info("Clicking Next after username");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getAadSignInButtonId()))
                .click();

        LOG.info("Entering password");
        System.out.println("Using password ID: " + fields.getPasswordInputId());
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getPasswordInputId()));
        System.out.println(By.id(fields.getPasswordInputId()));
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getPasswordInputId()))
                .sendKeys(user.getPassword());

        LOG.info("Clicking submit");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getPasswordSigInButtonId()))
                .click();

        if (checkAuthenticationCompletePage(driver)) {
            return;
        }

        handleOptionalPrompts(driver);
    }

    private static boolean checkAuthenticationCompletePage(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, COMMON_ELEMENT_TIMEOUT.getSeconds());
            wait.until(ExpectedConditions.textToBePresentInElementLocated(
                    By.tagName("body"), "Authentication complete"));
            return true;
        } catch (TimeoutException ex) {
            LOG.debug("Authentication complete page not found: {}", ex.getMessage());
            return false;
        }
    }

    private static void handleOptionalPrompts(WebDriver driver) {
        // Handle "Are you trying to sign in to..." prompt
        try {
            LOG.info("Checking for 'Are you trying to sign in' prompt");
            waitForElementToBeVisibleAndEnabled(
                    driver,
                    By.id(SeleniumConstants.ARE_YOU_TRYING_TO_SIGN_IN_TO),
                    COMMON_ELEMENT_TIMEOUT)
                    .click();
            LOG.info("Clicked Continue on sign-in prompt");
        } catch (TimeoutException ex) {
            LOG.debug("No 'Are you trying to sign in' prompt found");
        }

        // Handle "Stay signed in?" prompt
        try {
            LOG.info("Checking for 'Stay signed in' prompt");
            waitForElementToBeVisibleAndEnabled(
                    driver,
                    By.id(SeleniumConstants.STAY_SIGN_IN_NO_BUTTON_ID),
                    COMMON_ELEMENT_TIMEOUT)
                    .click();
            LOG.info("Clicked No on 'Stay signed in' prompt");
        } catch (TimeoutException ex) {
            LOG.debug("No 'Stay signed in' prompt found");
        }
    }

    public static void performADFSLogin(WebDriver driver, LabUser user) {
        LOG.info("performADFSLogin for user: {}", user.getUpn());

        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Entering username");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getADFSUserNameInputId()))
                .sendKeys(user.getUpn());

        LOG.info("Entering password");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getPasswordInputId()))
                .sendKeys(user.getPassword());

        LOG.info("Clicking submit");
        waitForElementToBeVisibleAndEnabled(driver, By.id(fields.getPasswordSigInButtonId()))
                .click();
    }

    public static void performLocalLogin(WebDriver driver, LabUser user) {
        LOG.info("performLocalLogin");

        waitForElementToBeVisibleAndEnabled(driver, By.id(SeleniumConstants.B2C_LOCAL_ACCOUNT_ID))
                .click();

        LOG.info("Entering username");
        waitForElementToBeVisibleAndEnabled(driver, By.id(SeleniumConstants.B2C_LOCAL_USERNAME_ID))
                .sendKeys(TestConstants.B2C_UPN);

        LOG.info("Entering password");
        waitForElementToBeVisibleAndEnabled(driver, By.id(SeleniumConstants.B2C_LOCAL_PASSWORD_ID))
                .sendKeys(user.getPassword());

        LOG.info("Clicking sign in");
        waitForElementToBeVisibleAndEnabled(driver, By.id(SeleniumConstants.B2C_LOCAL_SIGN_IN_BUTTON_ID))
                .click();
    }
}
