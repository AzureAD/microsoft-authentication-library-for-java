// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
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

    //Temporarily disabling: timeout occuring after 15 minutes, likely either a server-side issue or a UI change
    //Needs investigation, tracked in https://github.com/AzureAD/microsoft-authentication-library-for-java/issues/1023
    //@Test
    void DeviceCodeFlowADTest() throws Exception {
        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(app.getAppId(), TestConstants.MICROSOFT_AUTHORITY_HOST + user.getTenantId());

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> runAutomatedDeviceCodeFlow(deviceCode, user);

        IAuthenticationResult result = pca.acquireToken(DeviceCodeFlowParameters
                .builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        deviceCodeConsumer)
                .build())
                .get();

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

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
