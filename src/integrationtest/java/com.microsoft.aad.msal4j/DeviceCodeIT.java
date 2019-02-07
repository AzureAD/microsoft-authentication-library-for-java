package com.microsoft.aad.msal4j;

import Infrastructure.SeleniumExtensions;
import lapapi.LabResponse;
import lapapi.LabUser;
import lapapi.LabUserProvider;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = "integration-tests")
public class DeviceCodeIT {

    private final static Logger LOG = LoggerFactory.getLogger(DeviceCodeIT.class);

    private LabUserProvider labUserProvider;
    private static final String[] scopes = {"User.Read"};
    private WebDriver seleniumDriver;

    @BeforeClass
    public void setUp(){
        labUserProvider = new LabUserProvider();
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }


    @Test
    public void DeviceCodeFlowTest(){
        LabResponse labResponse = labUserProvider.getDefaultUser();

        LOG.info("Calling acquireTokenWithDeviceCodeAsync");
        String password = labUserProvider.getUserPassword(labResponse.getUser());

    }

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, LabUser user){
        try{
            seleniumDriver.navigate().to(deviceCode.getVerificationUrl());
            seleniumDriver.findElement(new By.ById("code")).sendKeys(deviceCode.getUserCode());

           WebElement continueBtn = SeleniumExtensions.waitForElementToBeVisibleAndEnable(
                   seleniumDriver,
                   new By.ById("continueBtn")
           );
            continueBtn.click();

            SeleniumExtensions.performLogin(seleniumDriver, user);
        } catch(Exception e){
            LOG.error("Browser automation failed: " + e.getMessage());
            throw e;
        }
    }
}
