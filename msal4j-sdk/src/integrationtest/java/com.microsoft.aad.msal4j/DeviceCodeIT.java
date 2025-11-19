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

    @Test
    void DeviceCodeFlowADTest() throws Exception {
        Config cfg = new Config();

        LabResponse labResponse = LabUserHelper.getDefaultUser();
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

        try {
            String deviceCodeFormId;
            String continueButtonId;
            deviceCodeFormId = "otc";
            continueButtonId = "idSIButton9";
            LOG.info("Loggin in ... Entering device code");
            seleniumDriver.navigate().to(deviceCode.verificationUri());
            seleniumDriver.findElement(new By.ById(deviceCodeFormId)).sendKeys(deviceCode.userCode());

            LOG.info("Loggin in ... click continue");
            WebElement continueBtn = SeleniumExtensions.waitForElementToBeVisibleAndEnable(
                    seleniumDriver,
                    new By.ById(continueButtonId));
            continueBtn.click();

            SeleniumExtensions.performADOrCiamLogin(seleniumDriver, user);
        } catch (Exception e) {
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
