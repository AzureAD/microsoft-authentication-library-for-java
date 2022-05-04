// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import infrastructure.SeleniumExtensions;
import labapi.*;
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
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void DeviceCodeFlowADTest(String environment) throws Exception {
        Config cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.tenantSpecificAuthority()).
                build();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            runAutomatedDeviceCodeFlow(deviceCode, user);
        };

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()),
                        deviceCodeConsumer)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertFalse(Strings.isNullOrEmpty(result.accessToken()));
    }

    @Test()
    public void DeviceCodeFlowADFSv2019Test() throws Exception {

        User user = labUserProvider.getOnPremAdfsUser(FederationProvider.ADFS_2019);

        PublicClientApplication pca = PublicClientApplication.builder(
                TestConstants.ADFS_APP_ID).
                authority(TestConstants.ADFS_AUTHORITY).validateAuthority(false).
                build();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            runAutomatedDeviceCodeFlow(deviceCode, user);
        };

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(TestConstants.ADFS_SCOPE),
                        deviceCodeConsumer)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertFalse(Strings.isNullOrEmpty(result.accessToken()));
    }

    @Test()
    public void DeviceCodeFlowMSATest() throws Exception {

        User user = labUserProvider.getMSAUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(TestConstants.CONSUMERS_AUTHORITY).
                build();

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            runAutomatedDeviceCodeFlow(deviceCode, user);
        };

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(""),
                        deviceCodeConsumer)
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertFalse(Strings.isNullOrEmpty(result.accessToken()));

        result = pca.acquireTokenSilently(SilentParameters.
                builder(Collections.singleton(""), result.account()).
                build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertFalse(Strings.isNullOrEmpty(result.accessToken()));
    }

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, User user) {
        boolean isRunningLocally = true;//!Strings.isNullOrEmpty(
        //System.getenv(TestConstants.LOCAL_FLAG_ENV_VAR));

        boolean isADFS2019 = user.getFederationProvider().equals("adfsv2019");

        LOG.info("Device code running locally: " + isRunningLocally);
        try {
            String deviceCodeFormId;
            String continueButtonId;
            if (isRunningLocally) {
                if (isADFS2019) {
                    deviceCodeFormId = "userCodeInput";
                    continueButtonId = "confirmationButton";
                } else {
                    deviceCodeFormId = "otc";
                    continueButtonId = "idSIButton9";
                }
            } else {
                deviceCodeFormId = "code";
                continueButtonId = "continueBtn";
            }
            LOG.info("Loggin in ... Entering device code");
            if (isADFS2019) {
                seleniumDriver.manage().deleteAllCookies();
            }
            seleniumDriver.navigate().to(deviceCode.verificationUri());
            seleniumDriver.findElement(new By.ById(deviceCodeFormId)).sendKeys(deviceCode.userCode());

            LOG.info("Loggin in ... click continue");
            WebElement continueBtn = SeleniumExtensions.waitForElementToBeVisibleAndEnable(
                    seleniumDriver,
                    new By.ById(continueButtonId));
            continueBtn.click();

            if (isADFS2019) {
                SeleniumExtensions.performADFS2019Login(seleniumDriver, user);
            } else {
                SeleniumExtensions.performADLogin(seleniumDriver, user);
            }
        } catch (Exception e) {
            if (!isRunningLocally) {
                SeleniumExtensions.takeScreenShot(seleniumDriver);
            }
            LOG.error("Browser automation failed: " + e.getMessage());
            throw new RuntimeException("Browser automation failed: " + e.getMessage());
        }
    }

    @AfterClass
    public void cleanUp() {
        if (seleniumDriver != null) {
            seleniumDriver.close();
        }
    }
}
