// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import infrastructure.SeleniumExtensions;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
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
        LabResponse labResponse = LabConfigHelper.getDefaultConfig();
        UserConfig user = labResponse.getUser();

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(labResponse.getApp().getAppId(), TestConstants.MICROSOFT_AUTHORITY_HOST + labResponse.getUser().getTenantId());

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> runAutomatedDeviceCodeFlow(deviceCode, user);

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        deviceCodeConsumer)
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    //TODO: This test is failing intermittently due to inconsistent login page layouts and is commented out until fixed.
    //@Test()
//    void DeviceCodeFlowMSATest() throws Exception {
//
//        LabResponse labResponse = LabConfigHelper.getMSAUser();
//        UserConfig user = labResponse.getUser();
//        AppConfig app = labResponse.getApp();
//
//        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(app.getAppId(), TestConstants.CONSUMERS_AUTHORITY);
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

    private void runAutomatedDeviceCodeFlow(DeviceCode deviceCode, UserConfig user) {
        SeleniumExtensions.performDeviceCodeLogin(
                seleniumDriver,
                deviceCode.verificationUri(),
                deviceCode.userCode(),
                user);
    }

    @AfterAll
    void cleanUp() {
        if (seleniumDriver != null) {
            seleniumDriver.close();
        }
    }
}
