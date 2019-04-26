// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package infrastructure;

import labapi.FederationProvider;
import labapi.LabUser;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SeleniumExtensions {

    private final static Logger LOG = LoggerFactory.getLogger(SeleniumExtensions.class);

    private SeleniumExtensions(){}

    public static WebDriver createDefaultWebDriver(){
        ChromeOptions options = new ChromeOptions();
        //no visual rendering, remove when debugging
        //options.addArguments("--headless");

        System.setProperty("webdriver.chrome.driver", "C:/Windows/chromedriver.exe");
        ChromeDriver driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

        return driver;
}

    public static WebElement waitForElementToBeVisibleAndEnable(WebDriver driver, By by){
        WebDriverWait webDriverWait = new WebDriverWait(driver, 15);
        return webDriverWait.until((dr) ->
        {
            try {
                WebElement elementToBeDisplayed = driver.findElement(by);
                if(elementToBeDisplayed.isDisplayed() && elementToBeDisplayed.isEnabled()){
                    return elementToBeDisplayed;
                }
                return null;
            } catch (StaleElementReferenceException e) {
            return null;
            }
        });
    }

    public static void performADLogin(WebDriver driver, LabUser user){
        UserInformationFields fields = new UserInformationFields(user);

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(fields.getAadUserNameInputId())).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Clicking <Next> after username");
        driver.findElement(new By.ById(fields.getAadSignInButtonId())).click();

        if (user.getFederationProvider() == FederationProvider.ADFSV2 && user.isFederated()){
            LOG.info("Loggin in ... ADFS-V2 - Entering the username in ADFSv2 form");
            driver.findElement(new By.ById(SeleniumConstants.ADFSV2_WEB_USERNAME_INPUT_ID)).
                    sendKeys(user.getUpn());
        }

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(fields.getPasswordInputId());
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");
         waitForElementToBeVisibleAndEnable(driver, new By.ById(fields.getPasswordSigInButtonId())).
                click();
    }

    public static void performLocalLogin(WebDriver driver, LabUser user){

        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.B2C_LOCAL_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.B2C_LOCAL_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.B2C_LOCAL_SIGN_IN_BUTTON_ID)).
                click();
    }

    public static void performGoogleLogin(WebDriver driver, LabUser user){

        driver.findElement(new By.ById(SeleniumConstants .GOOGLE_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.GOOGLE_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Clicking <Next> after username");
        driver.findElement(new By.ById(SeleniumConstants.GOOGLE_NEXT_AFTER_USERNAME_BUTTON)).click();

        LOG.info("Loggin in ... Entering password");
        By by = new By.ByName(SeleniumConstants.GOOGLE_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        LOG.info("Loggin in ... click submit");

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.GOOGLE_NEXT_BUTTON_ID)).
                click();
    }

    public static void performFacebookLogin(WebDriver driver, LabUser user){

        driver.findElement(new By.ById(SeleniumConstants.FACEBOOK_ACCOUNT_ID)).click();

        LOG.info("Loggin in ... Entering username");
        driver.findElement(new By.ById(SeleniumConstants.FACEBOOK_USERNAME_ID)).sendKeys(user.getUpn());

        LOG.info("Loggin in ... Entering password");
        By by = new By.ById(SeleniumConstants.FACEBOOK_PASSWORD_ID);
        waitForElementToBeVisibleAndEnable(driver, by).sendKeys(user.getPassword());

        waitForElementToBeVisibleAndEnable(driver, new By.ById(SeleniumConstants.FACEBOOK_LOGIN_BUTTON_ID)).
                click();
    }

    public static void takeScreenShot(WebDriver driver){
        String file = System.getenv("BUILD_STAGINGDIRECTORY");
        File destination = new File(file + "" + "/SeleniumError.png");
        File scrFile = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
        try {
            FileUtils.copyFile(scrFile, destination);
            LOG.info("Screenshot can be found at: " + destination.getPath());
        } catch(Exception exception){
            LOG.error("Error taking screenshot: " + exception.getMessage());
        }
    }
}
