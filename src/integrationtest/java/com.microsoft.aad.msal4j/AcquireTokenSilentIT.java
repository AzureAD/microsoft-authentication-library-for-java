// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

public class AcquireTokenSilentIT {
    private LabUserProvider labUserProvider;

    private Config cfg;

    @BeforeClass
    public void setUp(){
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_OrganizationAuthority_TokenRefreshed(String environment) throws Exception {
        cfg = new Config(environment);

        // When using common, organization, or consumer tenants, cache has no way
        // of determining which access token to return therefore token is always refreshed
        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(cfg.graphDefaultScope()),
                account).build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_LabAuthority_TokenNotRefreshed(String environment) throws Exception {
        cfg = new Config(environment);

        // Access token should be returned from cache, and not using refresh token

        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);


        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(cfg.graphDefaultScope()), account).
                build();

        IAuthenticationResult acquireSilentResult =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(acquireSilentResult.accessToken());
        Assert.assertNotNull(result.idToken());
        // Check that access and id tokens are coming from cache
        Assert.assertEquals(result.accessToken(), acquireSilentResult.accessToken());
        Assert.assertEquals(result.idToken(), acquireSilentResult.idToken());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_ForceRefresh(String environment) throws Exception {
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(cfg.graphDefaultScope()), account).
                forceRefresh(true).
                build();

        IAuthenticationResult resultAfterRefresh =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(resultAfterRefresh);
        Assert.assertNotNull(resultAfterRefresh.accessToken());
        Assert.assertNotNull(resultAfterRefresh.idToken());
        // Check that new refresh and id tokens are being returned
        Assert.assertNotEquals(result.accessToken(), resultAfterRefresh.accessToken());
        Assert.assertNotEquals(result.idToken(), resultAfterRefresh.idToken());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_MultipleAccountsInCache_UseCorrectAccount(String environment) throws Exception {
        cfg = new Config(environment);

        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        // get lab user for different account
        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_4);

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount account = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        user.getUpn())).findFirst().orElse(null);

        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(cfg.graphDefaultScope()), account).
                forceRefresh(true).
                build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(result.account().username(), user.getUpn());
    }

    @Test
    public void acquireTokenSilent_usingCommonAuthority_returnCachedAt() throws Exception {
        acquireTokenSilent_returnCachedTokens(cfg.organizationsAuthority());
    }

    @Test
    public void acquireTokenSilent_usingTenantSpecificAuthority_returnCachedAt() throws Exception {
        acquireTokenSilent_returnCachedTokens(cfg.tenantSpecificAuthority());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_ConfidentialClient_acquireTokenSilent(String environment) throws Exception{
        cfg = new Config(environment);

        IConfidentialClientApplication cca = getConfidentialClientApplications();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        String cachedAt = result.accessToken();

        result = cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertEquals(result.accessToken(), cachedAt);
    }

    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenSilent_ConfidentialClient_acquireTokenSilentDifferentScopeThrowsException()
            throws Exception {
        cfg = new Config(AzureEnvironment.AZURE);

        IConfidentialClientApplication cca = getConfidentialClientApplications();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        //Acquiring token for different scope, expect exception to be thrown
        cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()))
                .build())
                .get();
    }

    private IConfidentialClientApplication getConfidentialClientApplications() throws Exception{
        String clientId = cfg.appProvider.getOboAppId();
        String password = cfg.appProvider.getOboAppPassword();

        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        return ConfidentialClientApplication.builder(
                clientId, credential).
                //authority(MICROSOFT_AUTHORITY)
                authority(cfg.tenantSpecificAuthority()).
                build();
    }

    private void acquireTokenSilent_returnCachedTokens(String authority) throws Exception {
        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(authority).
                build();

        IAuthenticationResult interactiveAuthResult = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();

        Assert.assertNotNull(interactiveAuthResult);

        IAuthenticationResult silentAuthResult = pca.acquireTokenSilently(
                SilentParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()), interactiveAuthResult.account())
                        .build())
                .get();

        Assert.assertNotNull(silentAuthResult);
        Assert.assertEquals(interactiveAuthResult.accessToken(), silentAuthResult.accessToken());
    }

    private IPublicClientApplication getPublicClientApplicationWithTokensInCache()
            throws Exception {
        User user = labUserProvider.getDefaultUser(cfg.azureEnvironment);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        pca.acquireToken(
                UserNamePasswordParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                        .build()).get();
        return pca;
    }
}
