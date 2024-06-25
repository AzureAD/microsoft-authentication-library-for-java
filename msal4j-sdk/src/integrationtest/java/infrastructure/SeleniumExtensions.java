// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import labapi.FederationProvider;
import labapi.LabConstants;
import labapi.User;
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

    private final static Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private SeleniumExtensions() {
    }

    public static WebDriver createDefaultWebDriver() {
        ChromeOptions options = new ChromeOptions();
        //no visual rendering, remove when debugging
        options.addArguments("--headless");

        System.setProperty("webdriver.chrome.driver", "C:/Windows/chromedriver.exe");
        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        return driver;
    }

    public static WebElement waitForElementToBeVisibleAndEnable(WebDriver driver, By by, int timeOutInSeconds) {
        WebDriverWait webDriverWait = new WebDriverWait(driver, timeOutInSeconds);
        return webDriverWait.until((dr) ->
        {
            try {
                WebElement elementToBeDisplayed = driver.findElement(by);
                if (elementToBeDisplayed.isDisplayed() && elementToBeDisplayed.isEnabled()) {
                    return elementToBeDisplayed;
                }
                return null;
            } catch (StaleElementReferenceException e) {
                return null;
            }
        });
    }

    public static WebElement waitForElementToBeVisibleAndEnable(WebDriver driver, By by) {
        int DEFAULT_TIMEOUT_IN_SEC = 15;

        return waitForElementToBeVisibleAndEnable(driver, by, DEFAULT_TIMEOUT_IN_SEC);
    }

    public static void performADOrCiamLogin(WebDriver driver, User user) {
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
            LOG.error(ex.getMessage());
        }

        LOG.info("Checking optional questions");

        try {
            LOG.info("Are you trying to sign in to ... ? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.ARE_YOU_TRYING_TO_SIGN_IN_TO), 3).
                    click();
            LOG.info("Are you trying to sign in to ... ? click Continue");

        } catch (TimeoutException ex) {
            LOG.error(ex.getMessage());
        }

        try {
            LOG.info("Stay signed in? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.STAY_SIGN_IN_NO_BUTTON_ID), 3).
                    click();
            LOG.info("Stay signed in?  click NO");
        } catch (TimeoutException ex) {
            LOG.error(ex.getMessage());
        }
    }

    private static void checkAuthenticationCompletePage(WebDriver driver) {
        (new WebDriverWait(driver, 5)).until((ExpectedCondition<Boolean>) d -> {
            boolean condition = false;
            WebElement we = d.findElement(new By.ByTagName("body"));
            if (we != null && we.getText().contains("Authentication complete")) {
                condition = true;
            }
            return condition;
        });
    }

    public static void performADFS2019Login(WebDriver driver, User user) {
        LOG.info("PerformADFS2019Login");

        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(fields.getADFS2019UserNameInputId())).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(fields.getPasswordInputId());
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");
        waitForElementToBeVisibleAndEnable(driver, new By.ById(fields.getPasswordSigInButtonId())).
                click();
    }

    public static void performLocalLogin(WebDriver driver, User user) {
        LOG.info("PerformLocalLogin");

        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.B2C_LOCAL_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.B2C_LOCAL_SIGN_IN_BUTTON_ID)).
                click();
    }

    public static void performGoogleLogin(WebDriver driver, User user) {
        LOG.info("PerformGoogleLogin");

        driver.findElement(new By.ById(SeleniumConstants.GOOGLE_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.GOOGLE_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Clicking <Next> after username");
        driver.findElement(new By.ById(SeleniumConstants.GOOGLE_NEXT_AFTER_USERNAME_BUTTON)).click();

        LOG.info("Loggin in ... Entering password");
        By by = new By.ByName(SeleniumConstants.GOOGLE_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.GOOGLE_NEXT_BUTTON_ID)).click();
    }

    public static void performFacebookLogin(WebDriver driver, User user) {
        LOG.info("PerformFacebookLogin");

        driver.findElement(new By.ById(SeleniumConstants.FACEBOOK_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.FACEBOOK_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.FACEBOOK_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.FACEBOOK_LOGIN_BUTTON_ID)).
                click();
    }

    public static void takeScreenShot(WebDriver driver) {
        String file = System.getenv("BUILD_STAGINGDIRECTORY");
        File destination = new File(file + "" + "/SeleniumError.png");
        File scrFile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, destination);
            LOG.info("Screenshot can be found at: " + destination.getPath());
        } catch (Exception exception) {
            LOG.error("Error taking screenshot: " + exception.getMessage());
        }
    }
}
