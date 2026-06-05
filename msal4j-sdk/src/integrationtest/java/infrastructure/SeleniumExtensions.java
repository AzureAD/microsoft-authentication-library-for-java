// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import com.microsoft.aad.msal4j.labapi.UserConfig;
import infrastructure.pageobjects.ADFSLoginPage;
import infrastructure.pageobjects.AzureADLoginPage;
import infrastructure.pageobjects.CIAMLoginPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.logging.LogType;
import org.openqa.selenium.logging.LoggingPreferences;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.logging.Level;

public class SeleniumExtensions {

    private static final Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private SeleniumExtensions() {
    }

    public static WebDriver createDefaultWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--incognito");

        // Enable browser console logging for diagnostic capture on failure
        LoggingPreferences logPrefs = new LoggingPreferences();
        logPrefs.enable(LogType.BROWSER, Level.ALL);
        options.setCapability("goog:loggingPrefs", logPrefs);

        return new ChromeDriver(options);
    }

    public static WebElement waitForElementToBeVisibleAndEnabled(WebDriver driver, By by, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    /**
     * Wait for any one of several locators to match a clickable element, returning the first match.
     * <p>
     * This uses a single wait loop that checks all locators on each poll cycle, so the total
     * wait time is bounded by {@code timeout} regardless of how many locators are provided.
     * When a fallback locator matches (not the primary), a warning is logged to flag that
     * the page structure may have changed.
     *
     * @param driver   the WebDriver instance
     * @param timeout  maximum time to wait for any locator to match
     * @param locators one or more locators to try, in priority order (primary first)
     * @return the first clickable WebElement found
     * @throws org.openqa.selenium.TimeoutException if no locator matches within the timeout
     */
    public static WebElement findWithFallback(WebDriver driver, Duration timeout, By... locators) {
        if (locators.length == 0) {
            throw new IllegalArgumentException("At least one locator must be provided");
        }

        if (locators.length == 1) {
            return waitForElementToBeVisibleAndEnabled(driver, locators[0], timeout);
        }

        WebDriverWait wait = new WebDriverWait(driver, timeout);
        final By primaryLocator = locators[0];

        return wait.until(d -> {
            for (By locator : locators) {
                try {
                    WebElement element = d.findElement(locator);
                    if (element != null && element.isDisplayed() && element.isEnabled()) {
                        if (!locator.equals(primaryLocator)) {
                            LOG.warn("Primary locator {} not found, matched fallback: {}",
                                    primaryLocator, locator);
                        }
                        return element;
                    }
                } catch (Exception ignored) {
                    // Element not found with this locator, try next
                }
            }
            return null;
        });
    }

    public static void performADOrCiamLogin(WebDriver driver, UserConfig user) {
        LOG.info("performADOrCiamLogin for user: {}", user.getUpn());

        AzureADLoginPage loginPage = new AzureADLoginPage(driver);
        loginPage.login(user.getUpn(), user.getPassword());
    }

    public static void performADFSLogin(WebDriver driver, UserConfig user) {
        LOG.info("performADFSLogin for user: {}", user.getUpn());

        ADFSLoginPage loginPage = new ADFSLoginPage(driver);
        loginPage.login(user.getUpn(), user.getPassword());
    }

    public static void performCiamLogin(WebDriver driver, UserConfig user) {
        LOG.info("performCiamLogin for user: {}", user.getUpn());

        CIAMLoginPage loginPage = new CIAMLoginPage(driver);
        loginPage.login(user.getUpn(), user.getPassword());
    }

    /**
     * Perform device code flow authentication.
     * Navigates to the verification URI, enters the device code, and completes Azure AD login.
     *
     * @param driver The WebDriver instance
     * @param verificationUri The URI to navigate to for device code entry
     * @param userCode The device code to enter
     * @param user The lab user credentials for login
     */
    public static void performDeviceCodeLogin(WebDriver driver, String verificationUri, String userCode, UserConfig user) {
        LOG.info("performDeviceCodeLogin for user: {}", user.getUpn());

        try {
            // Navigate to device code verification page
            LOG.info("Navigating to verification URI");
            driver.navigate().to(verificationUri);

            // Enter device code
            LOG.info("Entering device code");
            By deviceCodeInputField = By.id("otc");
            waitForElementToBeVisibleAndEnabled(driver, deviceCodeInputField, Duration.ofSeconds(15))
                    .sendKeys(userCode);

            // Click continue button
            LOG.info("Clicking continue button");
            By continueButton = By.id("idSIButton9");
            waitForElementToBeVisibleAndEnabled(driver, continueButton, Duration.ofSeconds(15))
                    .click();

            // Perform standard Azure AD login
            performADOrCiamLogin(driver, user);
        } catch (Exception e) {
            LOG.error("Device code flow automation failed: {}", e.getMessage());
            throw new RuntimeException("Device code flow automation failed", e);
        }
    }
}