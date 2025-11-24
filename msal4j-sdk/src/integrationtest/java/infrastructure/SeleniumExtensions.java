// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import com.microsoft.aad.msal4j.TestConstants;
import com.microsoft.aad.msal4j.labapi2.LabUser;
import infrastructure.pageobjects.ADFSLoginPage;
import infrastructure.pageobjects.AzureADLoginPage;
import infrastructure.pageobjects.B2CLocalLoginPage;
import org.openqa.selenium.By;
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

    private SeleniumExtensions() {
    }

    public static WebDriver createDefaultWebDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless");
        options.addArguments("--incognito");

        return new ChromeDriver(options);
    }

    public static WebElement waitForElementToBeVisibleAndEnabled(WebDriver driver, By by, Duration timeout) {
        WebDriverWait wait = new WebDriverWait(driver, timeout.getSeconds());
        return wait.until(ExpectedConditions.elementToBeClickable(by));
    }

    public static void performADOrCiamLogin(WebDriver driver, LabUser user) {
        LOG.info("performADOrCiamLogin for user: {}", user.getUpn());

        AzureADLoginPage loginPage = new AzureADLoginPage(driver);
        loginPage.login(user.getUpn(), user.getPassword());
    }

    public static void performADFSLogin(WebDriver driver, LabUser user) {
        LOG.info("performADFSLogin for user: {}", user.getUpn());

        ADFSLoginPage loginPage = new ADFSLoginPage(driver);
        loginPage.login(user.getUpn(), user.getPassword());
    }

    public static void performLocalLogin(WebDriver driver, LabUser user) {
        LOG.info("performLocalLogin");

        B2CLocalLoginPage loginPage = new B2CLocalLoginPage(driver);
        loginPage.login(TestConstants.B2C_UPN, user.getPassword());
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
    public static void performDeviceCodeLogin(WebDriver driver, String verificationUri, String userCode, LabUser user) {
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