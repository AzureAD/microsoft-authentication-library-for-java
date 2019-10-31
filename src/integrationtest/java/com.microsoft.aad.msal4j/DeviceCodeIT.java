// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import labapi.LabResponse;
import labapi.LabUser;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.testng.util.Strings;

import java.util.Collections;
import java.util.function.Consumer;

@Test
public class DeviceCodeIT {
    private final static Logger LOG = LoggerFactory.getLogger(DeviceCodeIT.class);

    private LabUserProvider labUserProvider;
    private WebDriver seleniumDriver;

    @BeforeClass
    public void setUp(){
        labUserProvider = LabUserProvider.getInstance();
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    @Test
    public void DeviceCodeFlowTest() throws Exception {
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            runAutomatedDeviceCodeFlow(deviceCode, labResponse.getUser());
        };


        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        deviceCodeConsumer)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertTrue(!Strings.isNullOrEmpty(result.accessToken()));
    }

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, LabUser user){
        boolean isRunningLocally = true; /*!Strings.isNullOrEmpty(
                System.getenv(TestConstants.LOCAL_FLAG_ENV_VAR));*/

        LOG.info("Device code running locally: " + isRunningLocally);
        try{
            String deviceCodeFormId;
            String continueButtonId;
            if(isRunningLocally){
                deviceCodeFormId = "otc";
                continueButtonId = "idSIButton9";
            } else {
                deviceCodeFormId = "code";
                continueButtonId = "continueBtn";
            }
            LOG.info("Loggin in ... Entering device code");
            seleniumDriver.navigate().to(deviceCode.verificationUri());
            seleniumDriver.findElement(new By.ById(deviceCodeFormId)).sendKeys(deviceCode.userCode());

            LOG.info("Loggin in ... click continue");
            WebElement continueBtn = SeleniumExtensions.waitForElementToBeVisibleAndEnable(
                   seleniumDriver,
                   new By.ById(continueButtonId));
            continueBtn.click();

            SeleniumExtensions.performADLogin(seleniumDriver, user);
        } catch(Exception e){
            if(!isRunningLocally){
                SeleniumExtensions.takeScreenShot(seleniumDriver);
            }
            LOG.error("Browser automation failed: " + e.getMessage());
            throw new RuntimeException("Browser automation failed: " + e.getMessage());
        }
    }

    @AfterClass
    public void cleanUp(){
        if( seleniumDriver != null){
            seleniumDriver.close();
        }
    }
}
