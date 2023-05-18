// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package infrastructure;

import labapi.User;
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public static void performADLogin(WebDriver driver, User user) {
        LOG.info("PerformADLogin");
        
        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.WEB_UPN_INPUT_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Clicking <Next> after username");
        driver.findElement(new By.ById(SeleniumConstants.WEB_SUBMIT_ID)).click();

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.WEB_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");
        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.WEB_SUBMIT_ID)).
                click();

        try {
            checkAuthenticationCompletePage(driver);
            return;
        } catch (TimeoutException ex) {
        }

        LOG.info("Checking optional questions");

        try {
            LOG.info("Are you trying to sign in to ... ? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.ARE_YOU_TRYING_TO_SIGN_IN_TO), 3).
                    click();
            LOG.info("Are you trying to sign in to ... ? click Continue");

        } catch (TimeoutException ex) {
        }

        try {
            LOG.info("Stay signed in? checking");
            waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.STAY_SIGN_IN_NO_BUTTON_ID), 3).
                    click();
            LOG.info("Stay signed in?  click NO");
        } catch (TimeoutException ex) {
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
}
