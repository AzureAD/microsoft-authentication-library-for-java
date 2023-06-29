// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenInteractiveIT extends SeleniumTest {
    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private Config cfg;

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenInteractive_ManagedUser(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope());
    }

    @Test()
    void acquireTokenInteractive_ADFSv2019_OnPrem() {
        User user = labUserProvider.getOnPremAdfsUser(FederationProvider.ADFS_2019);
        assertAcquireTokenCommon(user, TestConstants.ADFS_AUTHORITY, TestConstants.ADFS_SCOPE);
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenInteractive_ADFSv2019_Federated(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2019);
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenInteractive_ADFSv4_Federated(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_4);
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenInteractive_ADFSv3_Federated(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_3);
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenInteractive_ADFSv2_Federated(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2);
        assertAcquireTokenCommon(user, cfg.organizationsAuthority(), cfg.graphDefaultScope());
    }

    @Test
    void acquireTokenInteractive_Ciam() {
        User user = labUserProvider.getCiamUser();

        Map<String, String> extraQueryParameters = new HashMap<>();
        extraQueryParameters.put("dc","ESTS-PUB-EUS-AZ1-FD000-TEST1");

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                            user.getAppId()).
                    authority("https://" + user.getLabName() + ".ciamlogin.com/")
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
                    .scopes(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE))
                    .extraQueryParameters(extraQueryParameters)
                    .systemBrowserOptions(browserOptions)
                    .build();

            result = pca.acquireToken(parameters).get();

        } catch (Exception e) {
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }

        assertTokenResultNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithAuthorizationCode_B2C_Local(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getB2cUser(cfg.azureEnvironment, B2CProvider.LOCAL);
        assertAcquireTokenB2C(user, TestConstants.B2C_AUTHORITY);
    }

    @ParameterizedTest
    @MethodSource("com.microsoft.aad.msal4j.EnvironmentsProvider#createData")
    void acquireTokenWithAuthorizationCode_B2C_LegacyFormat(String environment) {
        cfg = new Config(environment);

        User user = labUserProvider.getB2cUser(cfg.azureEnvironment, B2CProvider.LOCAL);
        assertAcquireTokenB2C(user, TestConstants.B2C_AUTHORITY_LEGACY_FORMAT);
    }

    @Test
    void acquireTokenInteractive_ManagedUser_InstanceAware() {
        cfg = new Config(AzureEnvironment.AZURE);

        User user = labUserProvider.getDefaultUser(AzureEnvironment.AZURE_US_GOVERNMENT);
        assertAcquireTokenInstanceAware(user);
    }

    private void assertAcquireTokenCommon(User user, String authority, String scope) {
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    authority(authority).
                    build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(
                user,
                pca,
                scope);

        assertTokenResultNotNull(result);
        assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user, String authority) {

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    b2cAuthority(authority + TestConstants.B2C_SIGN_IN_POLICY).
                    build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(user, pca, user.getAppId());
        assertTokenResultNotNull(result);
    }

    private void assertAcquireTokenInstanceAware(User user) {
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    authority(cfg.organizationsAuthority()).
                    build();
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive_instanceAware(user, pca, cfg.graphDefaultScope());

        assertTokenResultNotNull(result);
        assertEquals(user.getUpn(), result.account().username());

        //This test is using a client app with the login.microsoftonline.com config to get tokens for a login.microsoftonline.us user,
        // so when using instance aware the result's environment will be for the user/account and not the client app
        assertNotEquals(pca.authenticationAuthority.host, result.environment());
        assertEquals(result.account().environment(), result.environment());
        assertEquals(result.account().environment(), pca.getAccounts().join().iterator().next().environment());

        IAuthenticationResult cachedResult;
        try {
            cachedResult = acquireTokenSilently(pca, result.account(), cfg.graphDefaultScope());
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

    public void acquireTokensInHomeAndGuestClouds(String homeCloud) throws MalformedURLException {

        User user = labUserProvider.getUserByGuestHomeAzureEnvironments
                (AzureEnvironment.AZURE, homeCloud);

        // use user`s upn from home cloud
        user.setUpn(user.getHomeUPN());

        ITokenCacheAccessAspect persistenceAspect = new ITokenCacheAccessAspect() {
            String data;

            @Override
            public void beforeCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
                iTokenCacheAccessContext.tokenCache().deserialize(data);
            }

            @Override
            public void afterCacheAccess(ITokenCacheAccessContext iTokenCacheAccessContext) {
                data = iTokenCacheAccessContext.tokenCache().serialize();
            }
        };

        PublicClientApplication publicCloudPca = PublicClientApplication.builder(
                user.getAppId()).
                authority(TestConstants.AUTHORITY_PUBLIC_TENANT_SPECIFIC).setTokenCacheAccessAspect(persistenceAspect).
                build();

        IAuthenticationResult result = acquireTokenInteractive(user, publicCloudPca, TestConstants.USER_READ_SCOPE);
        assertTokenResultNotNull(result);
        assertEquals(user.getHomeUPN(), result.account().username());

        publicCloudPca.removeAccount(publicCloudPca.getAccounts().join().iterator().next()).join();

        assertEquals(publicCloudPca.getAccounts().join().size(), 0);
    }

    private IAuthenticationResult acquireTokenInteractive(
            User user,
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
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private void assertTokenResultNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
    }

    private IAuthenticationResult acquireTokenInteractive_instanceAware(
            User user,
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
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    class SeleniumOpenBrowserAction implements OpenBrowserAction {

        private User user;
        private PublicClientApplication pca;

        SeleniumOpenBrowserAction(User user, PublicClientApplication pca) {
            this.user = user;
            this.pca = pca;
        }

        public void openBrowser(URL url) {
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, pca);
        }
    }
}
