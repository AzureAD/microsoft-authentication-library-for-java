// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi2.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.microsoft.aad.msal4j.TestConstants.KEYVAULT_DEFAULT_SCOPE;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenSilentIT {
    private Config cfg;

    @Test
    void acquireTokenSilent_OrganizationAuthority_TokenRefreshed() throws Exception {
        cfg = new Config();

        // When using common, organization, or consumer tenants, cache has no way
        // of determining which access token to return therefore token is always refreshed
        IPublicClientApplication pca = getPublicClientApplicationWithTokensInCache();

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult result = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(result);
    }

    @Test
    void acquireTokenSilent_LabAuthority_TokenNotRefreshed() throws Exception {
        cfg = new Config();

        // Access token should be returned from cache, and not using refresh token
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult acquireSilentResult = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(result);

        // Check that access and id tokens are coming from cache
        assertEquals(result.accessToken(), acquireSilentResult.accessToken());
        assertEquals(result.idToken(), acquireSilentResult.idToken());
        assertEquals(TokenSource.IDENTITY_PROVIDER, result.metadata().tokenSource());
        assertEquals(TokenSource.CACHE, acquireSilentResult.metadata().tokenSource());
    }

    @Test
    void acquireTokenSilent_ForceRefresh() throws Exception {
        cfg = new Config();

        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult resultAfterRefresh = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), true);
        assertResultNotNull(resultAfterRefresh);

        // Check that new refresh and id tokens are being returned
        assertTokensAreNotEqual(result, resultAfterRefresh);
        assertEquals(TokenSource.IDENTITY_PROVIDER, result.metadata().tokenSource());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultAfterRefresh.metadata().tokenSource());
    }

    @Test
    void acquireTokenSilent_usingCommonAuthority_returnCachedAt() throws Exception {
        acquireTokenSilent_returnCachedTokens(cfg.organizationsAuthority());
    }

    @Test
    void acquireTokenSilent_usingTenantSpecificAuthority_returnCachedAt() throws Exception {
        acquireTokenSilent_returnCachedTokens(cfg.tenantSpecificAuthority());
    }

    @Test
    void acquireTokenSilent_ConfidentialClient_acquireTokenSilent() throws Exception {
        cfg = new Config();

        IConfidentialClientApplication cca = getConfidentialClientApplications();
        //test that adding extra query parameters does not break the flow
        Map<String, String> extraParameters = new HashMap<>();
        extraParameters.put("test","test");
        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()))
                        .extraQueryParameters(extraParameters)
                .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());

        String cachedAt = result.accessToken();

        result = cca.acquireTokenSilently(SilentParameters
                .builder(Collections.singleton(cfg.graphDefaultScope()))
                        .extraQueryParameters(extraParameters)
                .build())
                .get();

        assertNotNull(result);
        assertEquals(result.accessToken(), cachedAt);
    }

    @Test
    void acquireTokenSilent_ConfidentialClient_acquireTokenSilentDifferentScopeThrowsException()
            throws Exception {
        cfg = new Config();

        IConfidentialClientApplication cca = getConfidentialClientApplications();

        IAuthenticationResult result = cca.acquireToken(ClientCredentialParameters
                .builder(Collections.singleton(KEYVAULT_DEFAULT_SCOPE))
                .build())
                .get();

        assertNotNull(result);
        assertNotNull(result.accessToken());

        //Acquiring token for different scope, expect exception to be thrown
        assertThrows(ExecutionException.class, () -> cca.acquireTokenSilently(SilentParameters
                        .builder(Collections.singleton(cfg.graphDefaultScope()))
                        .build())
                .get());
    }

    @Test
    void acquireTokenSilent_WithRefreshOn() throws Exception {
        cfg = new Config();

        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult resultOriginal = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());
        assertResultNotNull(resultOriginal);

        IAuthenticationResult resultSilent = acquireTokenSilently(pca, resultOriginal.account(), cfg.graphDefaultScope(), false);
        assertNotNull(resultSilent);
        assertTokensAreEqual(resultOriginal, resultSilent);

        //When this test was made, token responses did not contain the refresh_in field needed for an end-to-end test.
        //In order to test silent flow behavior as though the service returned refresh_in, we manually change a cached
        // token's refreshOn value from 0 (default if refresh_in missing) to a minute before/after the current time
        String key = pca.tokenCache.accessTokens.keySet().iterator().next();
        AccessTokenCacheEntity token = pca.tokenCache.accessTokens.get(key);
        long currTimestampSec = new Date().getTime() / 1000;

        token.refreshOn(Long.toString(currTimestampSec + 60));
        pca.tokenCache.accessTokens.put(key, token);

        IAuthenticationResult resultSilentWithRefreshOn = acquireTokenSilently(pca, resultOriginal.account(), cfg.graphDefaultScope(), false);
        //Current time is before refreshOn, so token should not have been refreshed
        assertNotNull(resultSilentWithRefreshOn);
        assertEquals(pca.tokenCache.accessTokens.get(key).refreshOn(), Long.toString(currTimestampSec + 60));
        assertTokensAreEqual(resultSilent, resultSilentWithRefreshOn);

        token = pca.tokenCache.accessTokens.get(key);
        token.refreshOn(Long.toString(currTimestampSec - 60));
        pca.tokenCache.accessTokens.put(key, token);

        resultSilentWithRefreshOn = acquireTokenSilently(pca, resultOriginal.account(), cfg.graphDefaultScope(), false);
        //Current time is after refreshOn, so token should be refreshed
        assertNotNull(resultSilentWithRefreshOn);
        assertTokensAreNotEqual(resultSilent, resultSilentWithRefreshOn);
        assertEquals(TokenSource.CACHE, resultSilent.metadata().tokenSource());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultSilentWithRefreshOn.metadata().tokenSource());
    }

    @Test
    void acquireTokenSilent_TenantAsParameter() throws Exception {
        cfg = new Config();

        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build()).get();
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult silentResult = acquireTokenSilently(pca, account, cfg.graphDefaultScope(), false);
        assertResultNotNull(silentResult);
        assertTokensAreEqual(result, silentResult);

        IAuthenticationResult resultWithTenantParam = pca.acquireTokenSilently(SilentParameters.
                builder(Collections.singleton(cfg.graphDefaultScope()), account).
                    tenant(cfg.tenant()).
                build()).get();
        assertResultNotNull(resultWithTenantParam);
        assertTokensAreNotEqual(result, resultWithTenantParam);
    }

    @Test
    void acquireTokenSilent_emptyStringScope() throws Exception {
        cfg = new Config();
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        String emptyScope = StringHelper.EMPTY_STRING;
        IAuthenticationResult result = acquireTokenUsernamePassword(user, pca, emptyScope);
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult silentResult = acquireTokenSilently(pca, account, emptyScope, false);
        assertResultNotNull(silentResult);
        assertEquals(result.accessToken(), silentResult.accessToken());
    }

    @Test
    void acquireTokenSilent_emptyScopeSet() throws Exception {
        cfg = new Config();
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        Set<String> scopes = new HashSet<>();
        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                builder(scopes,
                        user.getUpn(),
                        user.getPassword().toCharArray())
                .build())
                .get();
        assertResultNotNull(result);

        IAccount account = pca.getAccounts().join().iterator().next();
        IAuthenticationResult silentResult = pca.acquireTokenSilently(SilentParameters.
                builder(scopes, account)
                .build())
                .get();

        assertResultNotNull(silentResult);
        assertEquals(result.accessToken(), silentResult.accessToken());
    }

    @Test
    public void acquireTokenSilent_ClaimsForceRefresh() throws Exception {
        cfg = new Config();
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        Set<String> scopes = new HashSet<>();
        PublicClientApplication pca = PublicClientApplication.builder(
                        labResponse.getApp().getAppId()).
                authority(cfg.organizationsAuthority()).
                build();

        IAuthenticationResult result = pca.acquireToken(UserNamePasswordParameters.
                        builder(scopes,
                                user.getUpn(),
                                user.getPassword().toCharArray())
                        .build())
                .get();

        assertResultNotNull(result);

        IAuthenticationResult silentResultWithoutClaims = pca.acquireTokenSilently(SilentParameters.
                        builder(scopes, result.account())
                        .build())
                .get();

        assertResultNotNull(silentResultWithoutClaims);
        assertEquals(result.accessToken(), silentResultWithoutClaims.accessToken());

        //If claims are added to a silent request, it should trigger the refresh flow and return a new token
        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("email", null);

        IAuthenticationResult silentResultWithClaims = pca.acquireTokenSilently(SilentParameters.
                        builder(scopes, result.account())
                        .claims(cr)
                        .build())
                .get();

        assertResultNotNull(silentResultWithClaims);
        assertNotEquals(result.accessToken(), silentResultWithClaims.accessToken());
    }

    private IConfidentialClientApplication getConfidentialClientApplications() throws Exception {
        String clientId = cfg.appProvider().getOboAppId();
        String password = cfg.appProvider().getOboAppPassword();

        IClientCredential credential = ClientCredentialFactory.createFromSecret(password);

        return ConfidentialClientApplication.builder(
                clientId, credential).
                //authority(MICROSOFT_AUTHORITY)
                        authority(cfg.tenantSpecificAuthority()).
                        build();
    }

    private void acquireTokenSilent_returnCachedTokens(String authority) throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
                authority(authority).
                build();

        IAuthenticationResult interactiveAuthResult = acquireTokenUsernamePassword(user, pca, cfg.graphDefaultScope());

        assertNotNull(interactiveAuthResult);

        IAuthenticationResult silentAuthResult = pca.acquireTokenSilently(
                SilentParameters.builder(
                        Collections.singleton(cfg.graphDefaultScope()), interactiveAuthResult.account())
                        .build())
                .get();

        assertNotNull(silentAuthResult);
        assertEquals(interactiveAuthResult.accessToken(), silentAuthResult.accessToken());
    }

    private IPublicClientApplication getPublicClientApplicationWithTokensInCache()
            throws Exception {
        LabResponse labResponse = LabUserHelper.getDefaultUser();
        LabUser user = labResponse.getUser();

        PublicClientApplication pca = PublicClientApplication.builder(
                labResponse.getApp().getAppId()).
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

    private IAuthenticationResult acquireTokenUsernamePassword(LabUser user, IPublicClientApplication pca, String scope) throws InterruptedException, ExecutionException {
        Map<String, String> map = new HashMap<>();
        map.put("test","test");
        return pca.acquireToken(UserNamePasswordParameters.
                builder(Collections.singleton(scope),
                        user.getUpn(),
                        user.getPassword().toCharArray())
                        .extraQueryParameters(map)
                .build())
                .get();
    }

    private void assertResultNotNull(IAuthenticationResult result) {
        assertNotNull(result);
        assertNotNull(result.accessToken());
        assertNotNull(result.idToken());
    }

    private void assertTokensAreNotEqual(IAuthenticationResult result, IAuthenticationResult secondResult) {
        assertNotEquals(result.accessToken(), secondResult.accessToken());
        assertNotEquals(result.idToken(), secondResult.idToken());
    }

    private void assertTokensAreEqual(IAuthenticationResult result, IAuthenticationResult secondResult) {
        assertEquals(result.accessToken(), secondResult.accessToken());
        assertEquals(result.idToken(), secondResult.idToken());
    }
}
