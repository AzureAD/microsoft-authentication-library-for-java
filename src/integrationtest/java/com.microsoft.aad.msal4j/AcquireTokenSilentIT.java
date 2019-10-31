// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import labapi.AppIdentityProvider;
import labapi.FederationProvider;
import labapi.LabResponse;
import labapi.LabUserProvider;
import labapi.NationalCloud;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static com.microsoft.aad.msal4j.TestConstants.GRAPH_DEFAULT_SCOPE;
import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

public class AcquireTokenSilentIT {
    private LabUserProvider labUserProvider;

    @BeforeClass
    public void setUp() {
        labUserProvider = LabUserProvider.getInstance();
    }

    @Test
    public void acquireTokenSilent_OrganizationAuthority_TokenRefreshed() throws Exception {

        // When using common, organization, or consumer tenants, cache has no way
        // of determining which access token to return therefore token is always refreshed
        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                account).build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
    }

    @Test
    public void acquireTokenSilent_LabAuthority_TokenNotRefreshed() throws Exception {
        // Access token should be returned from cache, and not using refresh token

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());
        String labAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + labResponse.getUser().getTenantId();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(labAuthority).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
                build();

        IAuthenticationResult acquireSilentResult =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(acquireSilentResult.accessToken());
        Assert.assertNotNull(result.idToken());
        // Check that access and id tokens are coming from cache
        Assert.assertEquals(result.accessToken(), acquireSilentResult.accessToken());
        Assert.assertEquals(result.idToken(), acquireSilentResult.idToken());
    }

    @Test
    public void acquireTokenSilent_ForceRefresh() throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        IAuthenticationResult result =  pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        IAccount account = pca.getAccounts().join().iterator().next();
        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
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

    @Test
    public void acquireTokenSilent_MultipleAccountsInCache_UseCorrectAccount() throws Exception {

        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        // get lab user for different account
        LabResponse labResponse = labUserProvider.getAdfsUser(
                FederationProvider.ADFSV4,
                true,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        // acquire token for different account
        pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Set<IAccount> accounts = pca.getAccounts().join();
        IAccount account = accounts.stream().filter(
                x -> x.username().equalsIgnoreCase(
                        labResponse.getUser().getUpn())).findFirst().orElse(null);

        SilentParameters parameters =  SilentParameters.builder(
                Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), account).
                forceRefresh(true).
                build();

        IAuthenticationResult result =  pca.acquireTokenSilently(parameters).get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());
        Assert.assertNotNull(result.idToken());
        Assert.assertEquals(result.account().username(), labResponse.getUser().getUpn());
    }

    @Test
    public void acquireTokenSilent_usingCommonAuthority_returnCachedAt() throws Exception {
        acquireTokenSilent_returnCachedTokens(TestConstants.ORGANIZATIONS_AUTHORITY);
    }

    @Test
    public void acquireTokenSilent_usingTenantSpecificAuthority_returnCachedAt() throws Exception {
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String tenantSpecificAuthority = TestConstants.MICROSOFT_AUTHORITY_HOST + labResponse.getUser().getTenantId();
        acquireTokenSilent_returnCachedTokens(tenantSpecificAuthority);
    }

    @Test
    public void acquireTokenSilent_ConfidentialClient_acquireTokenSilent() throws Exception{

        IConfidentialClientApplication cca = getConfidentialClientApplications();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        String cachedAt = result.accessToken();

        result = cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertEquals(result.accessToken(), cachedAt);
    }

    @Test(expectedExceptions = ExecutionException.class)
    public void acquireTokenSilent_ConfidentialClient_acquireTokenSilentDifferentScopeThrowsException()
            throws Exception {

        IConfidentialClientApplication cca = getConfidentialClientApplications();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.accessToken());

        //Acquiring token for different scope, expect exception to be thrown
        cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(GRAPH_DEFAULT_SCOPE))
                .build())
                .get();
    }

    private IConfidentialClientApplication getConfidentialClientApplications() throws Exception{
        AppIdentityProvider appProvider = new AppIdentityProvider();
        final String clientId = appProvider.getDefaultLabId();
        final String password = appProvider.getDefaultLabPassword();
        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        return ConfidentialClientApplication.builder(
                clientId, credential).
                authority(TestConstants.MICROSOFT_AUTHORITY).
                build();
    }

    private void acquireTokenSilent_returnCachedTokens(String authority) throws Exception {

        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(authority).
                build();

        IAuthenticationResult interactiveAuthResult = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                .build())
                .get();

        Assert.assertNotNull(interactiveAuthResult);

        IAuthenticationResult silentAuthResult = pca.acquireTokenSilently(
                SilentParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE), interactiveAuthResult.account())
                        .build())
                .get();

        Assert.assertNotNull(silentAuthResult);
        Assert.assertEquals(interactiveAuthResult.accessToken(), silentAuthResult.accessToken());
    }

    private IPublicClientApplication getPublicClientApplicationWithTokensInCache()
            throws Exception {
        LabResponse labResponse = labUserProvider.getDefaultUser(
                NationalCloud.AZURE_CLOUD,
                false);
        String password = labUserProvider.getUserPassword(labResponse.getUser());

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getAppId()).
                authority(TestConstants.ORGANIZATIONS_AUTHORITY).
                build();

        pca.acquireToken(
                UserNamePasswordParameters.builder(
                        Collections.singleton(TestConstants.GRAPH_DEFAULT_SCOPE),
                        labResponse.getUser().getUpn(),
                        password.toCharArray())
                        .build()).get();
        return pca;
    }
}
