// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import com.microsoft.aad.msal4j.TestConstants;
import com.microsoft.aad.msal4j.labapi2.LabUser;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SeleniumExtensions {

    private static final Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    //These timeout values define how long Selenium will wait for elements to be visible and enabled
    private static final int DEFAULT_TIMEOUT_IN_SEC = 15;
    private static final int COMMON_ELEMENT_TIMEOUT_IN_SEC = 5; //Used for most elements in a sign-in flow

    private SeleniumExtensions() {
    }

    public static WebDriver createDefaultWebDriver() {
        ChromeOptions options = new ChromeOptions();

        //No visual rendering, remove to see browser window when debugging
//        options.addArguments("--headless");
        //Add to avoid issues if your real browser's history/cookies are affecting tests, should not be needed in ADO pipelines
        options.addArguments("--incognito");

        System.setProperty("webdriver.chrome.driver", "C:/Windows/chromedriver.exe");
        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        return driver;
    }

    public static WebElement waitForElementToBeVisibleAndEnable(WebDriver driver, By by, int timeOutInSeconds) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, timeOutInSeconds);
        return webDriverWait.until(dr ->
        {
            try {
                WebElement elementToBeDisplayed = driver.findElement(by);
                if (elementToBeDisplayed.isDisplayed() && elementToBeDisplayed.isEnabled()) {
                    return elementToBeDisplayed;
                }
                return null;
            } catch (StaleElementReferenceException e) {
                LOG.info("Stale element waitForElementToBeVisibleAndEnable: " + e.getMessage());
                return null;
            }
        });
    }

    public static WebElement waitForElementToBeVisibleAndEnable(WebDriver driver, By by) {
        return waitForElementToBeVisibleAndEnable(driver, by, DEFAULT_TIMEOUT_IN_SEC);
    }

    public static void performADOrCiamLogin(WebDriver driver, LabUser user) {
        LOG.info("performADOrCiamLogin");

        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(fields.getAadUserNameInputId())).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Clicking <Next> after username");
        driver.findElement(new By.ById(fields.getAadSignInButtonId())).click();

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(fields.getPasswordInputId());
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");
        waitForElementToBeVisibleAndEnable(driver, new By.ById(fields.getPasswordSigInButtonId())).
                click();

        try {
            checkAuthenticationCompletePage(driver);
            return;
        } catch (TimeoutException ex) {
            LOG.error("Timeout Exception while checking authentication complete page: " + ex.getMessage());
        }

        LOG.info("Checking optional questions");

        try {
            LOG.info("Are you trying to sign in to ... ? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.ARE_YOU_TRYING_TO_SIGN_IN_TO), COMMON_ELEMENT_TIMEOUT_IN_SEC).
                    click();
            LOG.info("Are you trying to sign in to ... ? click Continue");

        } catch (TimeoutException ex) {
            LOG.error("Timeout Exception while checking sign in prompt: " + ex.getMessage());
        }

        try {
            LOG.info("Stay signed in? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.STAY_SIGN_IN_NO_BUTTON_ID), COMMON_ELEMENT_TIMEOUT_IN_SEC).
                    click();
            LOG.info("Stay signed in?  click NO");
        } catch (TimeoutException ex) {
            LOG.error("Timeout Exception while checking stay signed in prompt: " + ex.getMessage());
        }
    }

    private static void checkAuthenticationCompletePage(WebDriver driver) {
        new WebDriverWait(driver, COMMON_ELEMENT_TIMEOUT_IN_SEC).until((ExpectedCondition<Boolean>) d -> {
            WebElement we = d.findElement(new By.ByTagName("body"));
            try {
                if (we != null && we.getText().contains("Authentication complete"))
                    //The authentication is complete and the WebDriverWait can end
                    return true;
            } catch (StaleElementReferenceException e) {
                //It is possible for this method to begin executing before the redirect happens, in which case the WebElement
                // will reference something on the previous page and cause a StaleElementReferenceException
                return false;
            }
            return false;
        });
    }

    public static void performADFSLogin(WebDriver driver, LabUser user) {
        LOG.info("PerformADFSLogin");

        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(fields.getADFSUserNameInputId())).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(fields.getPasswordInputId());
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");
        waitForElementToBeVisibleAndEnable(driver, new By.ById(fields.getPasswordSigInButtonId())).
                click();
    }

    public static void performLocalLogin(WebDriver driver, LabUser user) {
        LOG.info("PerformLocalLogin");

        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_USERNAME_ID)).sendKeys(TestConstants.B2C_UPN);

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.B2C_LOCAL_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.B2C_LOCAL_SIGN_IN_BUTTON_ID)).
                click();
    }
}
