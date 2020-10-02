// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.net.MalformedURLException;
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
        IAuthenticationResult result = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(result);
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

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult acquireSilentResult = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(result);

        // Check that access and id tokens are coming from cache
        Assert.assertEquals(result.accessToken(), acquireSilentResult.accessToken());
        Assert.assertEquals(result.idToken(), acquireSilentResult.idToken());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_ForceRefresh(String environment) throws Exception {
        cfg = new Config(environment);

        User user = labUserProvider.getDefaultUser(environment);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult resultAfterRefresh = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), true);
        assertResultNotNull(resultAfterRefresh);

        // Check that new refresh and id tokens are being returned
        assertResultRefreshed(result, resultAfterRefresh);
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_MultipleAccountsInCache_UseCorrectAccount(String environment) throws Exception {
        cfg = new Config(environment);

        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        // get lab user for different account
        User user = labUserProvider.getFederatedAdfsUser(cfg.azureEnvironment, FederationProvider.ADFS_4);

        // acquire token for different account
        acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount account = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        user.getUpn())).findFirst().orElse(null);

        IAuthenticationResult result = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(result);
        Assert.assertEquals(result.account().username(), user.getUpn());
    }

    @Test(dataProvider = "environments", dataProviderClass = EnvironmentsProvider.class)
    public void acquireTokenSilent_ADFS2019(String environment) throws Exception{
        cfg = new Config(environment);

        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.AZURE_ENVIRONMENT, cfg.azureEnvironment);
        query.parameters.put(UserQueryParameters.FEDERATION_PROVIDER,  FederationProvider.ADFS_2019);
        query.parameters.put(UserQueryParameters.USER_TYPE,  UserType.FEDERATED);

        User user = labUserProvider.getLabUser(query);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult acquireSilentResult = acquireTokenSilently(pca, account, TestConstants.ADFS_SCOPE, false);
        assertResultNotNull(acquireSilentResult);

        account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult resultAfterRefresh = acquireTokenSilently(pca, account, TestConstants.ADFS_SCOPE, true);
        assertResultNotNull(resultAfterRefresh);

        assertResultRefreshed(result, resultAfterRefresh);
    }

    @Test
    public void acquireTokenSilent_B2C() throws Exception{
        UserQueryParameters query = new UserQueryParameters();
        query.parameters.put(UserQueryParameters.USER_TYPE, UserType.B2C);
        query.parameters.put(UserQueryParameters.B2C_PROVIDER, B2CProvider.LOCAL);
        User user = labUserProvider.getLabUser(query);

        PublicClientApplication pca = PublicClientApplication.builder(
                user.getAppId()).
                b2cAuthority(TestConstants.B2C_AUTHORITY_ROPC).
                build();

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, TestConstants.B2C_READ_SCOPE);
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult resultAfterRefresh = acquireTokenSilently(pca, account, TestConstants.B2C_READ_SCOPE, true);
        assertResultNotNull(resultAfterRefresh);

        assertResultRefreshed(result, resultAfterRefresh);
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

        IAuthenticationResult interactiveAuthResult = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());

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

        acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());
        return pca;
    }

    private IAuthenticationResult acquireTokenSilently(IPublicClientApplication pca, IAccount account, String scope, Boolean forceRefresh) throws InterruptedException, ExecutionException, MalformedURLException {
        return pca.acquireTokenSilently(SilentParameters.
                builder(Collections.singleton(scope), account).
                forceRefresh(forceRefresh).
                build())
                .get();
    }

    private IAuthenticationResult acquireTokenUsernamePassword(User user, IPublicClientApplication pca, String scope) throws InterruptedException, ExecutionException {
        return pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(scope),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();
    }

    private void assertResultNotNull(IAuthenticationResult result) {
        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    private void assertResultRefreshed(IAuthenticationResult result, IAuthenticationResult resultAfterRefresh) {
        Assert.assertNotEquals(result.accessToken(), resultAfterRefresh.accessToken());
        Assert.assertNotEquals(result.idToken(), resultAfterRefresh.idToken());
    }
}
