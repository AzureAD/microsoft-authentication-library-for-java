// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import infrastructure.SeleniumExtensions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.util.Collections;
import java.util.function.Consumer;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DeviceCodeIT {
    private static final Logger LOG = LoggerFactory.getLogger(DeviceCodeIT.class);

    private WebDriver seleniumDriver;

    @BeforeAll
    void setUp() {
        seleniumDriver = SeleniumExtensions.createDefaultWebDriver();
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void DeviceCodeFlowADTest(String environment) throws Exception {
        Config cfg = new Config(environment);

        LabResponse labResponse = LabUserHelper.getDefaultUser(environment);
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(labResponse.getApp().getAppId(), cfg.commonAuthority());

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> runAutomatedDeviceCodeFlow(deviceCode, user);

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()),
                        deviceCodeConsumer)
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    // TODO: labapi2 doesn't have on-prem ADFS user configuration yet - will be pulled from MSAL.NET
//    @Test()
//    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
//    void DeviceCodeFlowADFSv2019Test() throws Exception {
//
//        LabResponse labResponse = LabUserHelper.getOnPremAdfsUser(LabServiceParameters.FederationProvider.ADFS_V2019);
//        LabUser user = labResponse.getUser();
//
//        PublicClientApplication pca = PublicClientApplication.builder(
//                TestConstants.ADFS_APP_ID).
//                authority(TestConstants.ADFS_AUTHORITY).validateAuthority(false).
//                build();
//
//        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
//            runAutomatedDeviceCodeFlow(deviceCode, user);
//        };
//
//        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
//                .builder(Collections.singleton(TestConstants.ADFS_SCOPE),
//                        deviceCodeConsumer)
//                .build())
//                .get();
//
//        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
//    }

    // TODO: labapi2 doesn't have MSA user configuration yet - will be pulled from MSAL.NET
    // NOTE: This test was also failing intermittently in the pipeline runs for the same commit, but always passed locally.
    //@Test()
//    void DeviceCodeFlowMSATest() throws Exception {
//
//        LabResponse labResponse = LabUserHelper.getMSAUser();
//        LabUser user = labResponse.getUser();
//
//        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(user.getAppId(), TestConstants.CONSUMERS_AUTHORITY);
//
//        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
//            runAutomatedDeviceCodeFlow(deviceCode, user);
//        };
//
//        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
//                .builder(Collections.singleton(""),
//                        deviceCodeConsumer)
//                .build())
//                .get();
//
//        assertNotNull(result);
//        assertNotNull(result.accessToken());
//
//        result = pca.acquireTokenSilently(SilentParameters.
//                builder(Collections.singleton(""), result.account()).
//                build())
//                .get();
//
//        assertNotNull(result);
//        assertNotNull(result.accessToken());
//    }

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, LabUser user) {
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
                SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
            }
        } catch (Exception e) {
            if (!isRunningLocally) {
                SeleniumExtensions.takeScreenShot(seleniumDriver);
            }
            LOG.error("Browser automation failed: {}", e.getMessage());
            throw new RuntimeException("Browser automation failed: " + e.getMessage());
        }
    }

    @AfterAll
    void cleanUp() {
        if (seleniumDriver != null) {
            seleniumDriver.close();
        }
    }
}
