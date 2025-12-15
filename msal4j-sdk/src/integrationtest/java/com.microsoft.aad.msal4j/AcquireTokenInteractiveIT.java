// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.*;
import static com.microsoft.aad.msal4j.labapi.KeyVaultSecrets.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenInteractiveIT extends SeleniumTest {
    private static final Logger LOG = LoggerFactory.getLogger(AcquireTokenInteractiveIT.class);

    @AfterEach
    public void stopBrowser() {
        cleanUp();
    }

    @BeforeEach
    public void startBrowser() {
        startUpBrowser();
    }

    @Test
    void acquireTokenInteractive_ManagedUser() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_PUBLIC_CLOUD);

        assertAcquireTokenCommon(user, app.getAppId(), app.getAuthority() + "common", TestConstants.GRAPH_DEFAULT_SCOPE);
    }

    @Test
    void acquireTokenInteractive_Arlington() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_ARLINGTON);
        UserConfig user = LabResponseHelper.getUserConfig(USER_ARLINGTON);

        assertAcquireTokenCommon(user, app.getAppId(), app.getAuthority() + "common", TestConstants.GRAPH_DEFAULT_SCOPE);
    }

    @Test()
    @DisabledIfSystemProperty(named = "adfs.disabled", matches = "true")
    void acquireTokenInteractive_ADFSv2022() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_PCACLIENT);
        UserConfig user = LabResponseHelper.getUserConfig(USER_FED_DEFAULT);

        assertAcquireTokenCommon(user, app.getAppId(), app.getAuthority() + "organizations/", TestConstants.ADFS_SCOPE);
    }

    @Test
    void acquireTokenWithAuthorizationCode_B2C_Local() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_B2C);
        UserConfig user = LabResponseHelper.getUserConfig(USER_B2C);

        assertAcquireTokenB2C(user, TestConstants.B2C_AUTHORITY, app.getAppId());
    }

    @Test
    void acquireTokenWithAuthorizationCode_B2C_LegacyFormat() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_B2C);
        UserConfig user = LabResponseHelper.getUserConfig(USER_B2C);

        assertAcquireTokenB2C(user, TestConstants.B2C_AUTHORITY_LEGACY_FORMAT, app.getAppId());
    }

    @Test
    void acquireTokenInteractive_ManagedUser_InstanceAware() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_ARLINGTON);
        UserConfig user = LabResponseHelper.getUserConfig(USER_ARLINGTON);
        LabConfig lab = LabResponseHelper.getLabConfig(LAB_ARLMSIDLAB1);

        assertAcquireTokenInstanceAware(user, app.getAppId(), lab.getTenantId());
    }

    @Test
    void acquireTokenInteractive_Ciam() {
        AppConfig app = LabResponseHelper.getAppConfig(APP_CIAM);
        UserConfig user = LabResponseHelper.getUserConfig(USER_CIAM);

        Map<String, String> extraQueryParameters = new HashMap<>();

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                            app.getAppId()).
                    authority(app.getAuthority())
                    .build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result;
        try {
            URI url = new URI("http://localhost:8080");

            SystemBrowserOptions browserOptions =
                    SystemBrowserOptions
                            .builder()
                            .openBrowserAction(new SeleniumOpenBrowserAction(user, pca))
                            .build();

            InteractiveRequestParameters parameters = InteractiveRequestParameters
                    .builder(url)
                    .scopes(Collections.singleton(TestConstants.USER_READ_SCOPE))
                    .extraQueryParameters(extraQueryParameters)
                    .systemBrowserOptions(browserOptions)
                    .build();

            result = pca.acquireToken(parameters).get();

        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: {}", e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenCommon(UserConfig user, String appId, String authority, String scope) {
        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(appId, authority);

        IAuthenticationResult result = acquireTokenInteractive(
                user,
                pca,
                scope);

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(UserConfig user, String authority, String appId) {

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    appId).
                    b2cAuthority(authority + TestConstants.B2C_SIGN_IN_POLICY).
                    build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(user, pca, appId);
        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
    }

    private void assertAcquireTokenInstanceAware(UserConfig user, String appId, String tenantId) {
        PublicClientApplication pca = IntegrationTestHelper.createPublicApp(appId, TestConstants.MICROSOFT_AUTHORITY_HOST + tenantId);

        IAuthenticationResult result = acquireTokenInteractive_instanceAware(user, pca, TestConstants.GRAPH_DEFAULT_SCOPE);

        IntegrationTestHelper.assertAccessAndIdTokensNotNull(result);
        assertEquals(user.getUpn(), result.account().username());

        //This test is using a client app with the login.microsoftonline.com config to get tokens for a login.microsoftonline.us user,
        // so when using instance aware the result's environment will be for the user/account and not the client app
        assertNotEquals(pca.authenticationAuthority.host, result.environment());
        assertEquals(result.account().environment(), result.environment());
        assertEquals(result.account().environment(), pca.getAccounts().join().iterator().next().environment());

        IAuthenticationResult cachedResult;
        try {
            cachedResult = acquireTokenSilently(pca, result.account(), TestConstants.GRAPH_DEFAULT_SCOPE);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }

        //Ensure that the cached environment matches the original auth result environment (.us) instead of the client app's (.com)
        assertEquals(result.account().environment(), cachedResult.environment());
    }

    private IAuthenticationResult acquireTokenSilently(IPublicClientApplication pca, IAccount account, String scope) throws InterruptedException, ExecutionException, MalformedURLException {
        return pca.acquireTokenSilently(SilentParameters.builder(Collections.singleton(scope), account)
                .build())
                .get();
    }

    private IAuthenticationResult acquireTokenInteractive(
            UserConfig user,
            PublicClientApplication pca,
            String scope) {

        IAuthenticationResult result;
        try {
            URI url = new URI("http://localhost:8080");

            SystemBrowserOptions browserOptions =
                    SystemBrowserOptions
                            .builder()
                            .openBrowserAction(new SeleniumOpenBrowserAction(user, pca))
                            .build();

            InteractiveRequestParameters parameters = InteractiveRequestParameters
                    .builder(url)
                    .scopes(Collections.singleton(scope))
                    .systemBrowserOptions(browserOptions)
                    .build();

            result = pca.acquireToken(parameters).get();

        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: {}", e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractive_instanceAware(
            UserConfig user,
            PublicClientApplication pca,
            String scope) {

        IAuthenticationResult result;
        try {
            URI url = new URI("http://localhost:8080");

            SystemBrowserOptions browserOptions =
                    SystemBrowserOptions
                            .builder()
                            .openBrowserAction(new SeleniumOpenBrowserAction(user, pca))
                            .build();

            InteractiveRequestParameters parameters = InteractiveRequestParameters
                    .builder(url)
                    .scopes(Collections.singleton(scope))
                    .systemBrowserOptions(browserOptions).instanceAware(true)
                    .build();

            result = pca.acquireToken(parameters).get();

        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: {}", e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    class SeleniumOpenBrowserAction implements OpenBrowserAction {

        private UserConfig user;
        private PublicClientApplication pca;

        SeleniumOpenBrowserAction(UserConfig user, PublicClientApplication pca) {
            this.user = user;
            this.pca = pca;
        }

        public void openBrowser(URL url) {
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, pca);
        }
    }
}
