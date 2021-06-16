// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AzureEnvironment;
import labapi.B2CProvider;
import labapi.FederationProvider;
import labapi.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class AcquireTokenInteractiveIT extends SeleniumTest {
    private final static Logger LOG = LoggerFactory.getLogger(AuthorizationCodeIT.class);

    private Config cfg;

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenInteractive_ManagedUser(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);
        assertAcquireTokenAAD(user);
    }

    @Test()
    public void acquireTokenInteractive_ADFSv2019_OnPrem(){
        User user = labUserProvider.getOnPremAdfsUser(FederationProvider.ADFS_2019);
        assertAcquireTokenADFS2019(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenInteractive_ADFSv2019_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2019);
        assertAcquireTokenAAD(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenInteractive_ADFSv4_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_4);
        assertAcquireTokenAAD(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenInteractive_ADFSv3_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_3);
        assertAcquireTokenAAD(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenInteractive_ADFSv2_Federated(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_2);
        assertAcquireTokenAAD(user);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenWithAuthorizationCode_B2C_Local(String environment){
        cfg = new Config(environment);

        User user = labUserProvider.getB2cUser(cfg.azureEnvironment, B2CProvider.LOCAL);
        assertAcquireTokenB2C(user);
    }

    @Test
    public void acquireTokenInteractive_ManagedUser_InstanceAware(){
        cfg = new Config(AzureEnvironment.AZURE);

        User user = labUserProvider.getDefaultUser(AzureEnvironment.AZURE_US_GOVERNMENT);
        assertAcquireTokenInstanceAware(user);
    }

    private void assertAcquireTokenAAD(User user){
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    authority(cfg.organizationsAuthority()).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(
                user,
                pca,
                cfg.graphDefaultScope());

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenADFS2019(User user){
        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    TestConstants.ADFS_APP_ID).
                    authority(TestConstants.ADFS_AUTHORITY).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(user, pca, TestConstants.ADFS_SCOPE);

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());
    }

    private void assertAcquireTokenB2C(User user){

        PublicClientApplication pca;
        try {
            pca = PublicClientApplication.builder(
                    user.getAppId()).
                    b2cAuthority(TestConstants.B2C_AUTHORITY_SIGN_IN).
                    build();
        } catch(MalformedURLException ex){
            throw new RuntimeException(ex.getMessage());
        }

        IAuthenticationResult result = acquireTokenInteractive(user, pca, user.getAppId());
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
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

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getUpn(), result.account().username());

        //This test is using a client app with the login.microsoftonline.com config to get tokens for a login.microsoftonline.us user,
        // so when using instance aware the result's environment will be for the user/account and not the client app
        Assert.assertNotEquals(pca.authenticationAuthority.host, result.environment());
        Assert.assertEquals(result.account().environment(), result.environment());
        Assert.assertEquals(result.account().environment(), pca.getAccounts().join().iterator().next().environment());
    }

    @Test
    public void acquireTokensInHomeAndGuestClouds_ArlingtonAccount() throws MalformedURLException, ExecutionException, InterruptedException {
        acquireTokensInHomeAndGuestClouds(AzureEnvironment.AZURE_US_GOVERNMENT, TestConstants.AUTHORITY_ARLINGTON);
    }

    @Test
    public void acquireTokensInHomeAndGuestClouds_MooncakeAccount() throws MalformedURLException, ExecutionException, InterruptedException {
        acquireTokensInHomeAndGuestClouds(AzureEnvironment.AZURE_CHINA, TestConstants.AUTHORITY_MOONCAKE);
    }

    public void acquireTokensInHomeAndGuestClouds(String homeCloud, String homeCloudAuthority) throws MalformedURLException, ExecutionException, InterruptedException {

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
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(user.getHomeUPN(), result.account().username());

        publicCloudPca.removeAccount(publicCloudPca.getAccounts().join().iterator().next()).join();

        Assert.assertEquals(publicCloudPca.getAccounts().join().size(), 0);
    }

    private IAuthenticationResult acquireTokenInteractive(
            User user,
            PublicClientApplication pca,
            String scope){

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

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    private IAuthenticationResult acquireTokenInteractive_instanceAware(
            User user,
            PublicClientApplication pca,
            String scope){

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

        } catch(Exception e){
            LOG.error("Error acquiring token with authCode: " + e.getMessage());
            throw new RuntimeException("Error acquiring token with authCode: " + e.getMessage());
        }
        return result;
    }

    class SeleniumOpenBrowserAction implements OpenBrowserAction {

        private User user;
        private PublicClientApplication pca;

        SeleniumOpenBrowserAction(User user, PublicClientApplication pca){
            this.user = user;
            this.pca = pca;
        }

        public void openBrowser(URL url){
            seleniumDriver.navigate().to(url);
            runSeleniumAutomatedLogin(user, pca);
        }
    }
}
