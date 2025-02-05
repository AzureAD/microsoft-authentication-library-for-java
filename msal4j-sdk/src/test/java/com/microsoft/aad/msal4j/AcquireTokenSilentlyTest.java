// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AcquireTokenSilentlyTest {

    Account basicAccount = new Account("home_account_id", "login.windows.net", "username", null);
    String cache = readResource("/AAD_cache_data/full_cache.json");

    @Test
    void publicAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        PublicClientApplication application = PublicClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID)
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();

        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);

        assertInstanceOf(MsalClientException.class, ex.getCause());
        assertTrue(ex.getMessage().contains(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE));
    }

    @Test
    void confidentialAppAcquireTokenSilently_emptyCache_MsalClientException() throws Throwable {

        ConfidentialClientApplication application = ConfidentialClientApplication
                .builder(TestConfiguration.AAD_CLIENT_ID, ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_DUMMYSECRET))
                .b2cAuthority(TestConfiguration.B2C_AUTHORITY).build();

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scope")).build();
        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);

        assertInstanceOf(MsalClientException.class, ex.getCause());
        assertTrue(ex.getMessage().contains(AuthenticationErrorMessage.NO_TOKEN_IN_CACHE));
    }

    @Test
    void publicAppAcquireTokenSilently_claimsSkipCache() throws Throwable {

        PublicClientApplication application = PublicClientApplication.builder("client_id")
                .instanceDiscovery(false)
                .authority("https://some.authority.com/realm")
                .build();

        application.tokenCache.deserialize(cache);

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scopes"), basicAccount).build();

        IAuthenticationResult result = application.acquireTokenSilently(parameters).get();

        //Confirm cached dummy token returned from silent request
        assertNotNull(result);
        assertEquals("token", result.accessToken());

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("something", null);

        parameters = SilentParameters.builder(Collections.singleton("scopes"), basicAccount).claims(cr).build();
        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        //Confirm cached dummy token ignored when claims are part of request
        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(MsalInteractionRequiredException.class, ex.getCause());
    }

    @Test
    void confidentialAppAcquireTokenSilently_claimsSkipCache() throws Throwable {

        ConfidentialClientApplication application = ConfidentialClientApplication
                .builder("client_id", ClientCredentialFactory.createFromSecret(TestConfiguration.AAD_CLIENT_DUMMYSECRET))
                .instanceDiscovery(false)
                .authority("https://some.authority.com/realm").build();

        application.tokenCache.deserialize(cache);

        SilentParameters parameters = SilentParameters.builder(Collections.singleton("scopes"), basicAccount).build();

        IAuthenticationResult result = application.acquireTokenSilently(parameters).get();

        assertNotNull(result);
        assertEquals("token", result.accessToken());

        ClaimsRequest cr = new ClaimsRequest();
        cr.requestClaimInAccessToken("something", null);

        parameters = SilentParameters.builder(Collections.singleton("scopes"), basicAccount).claims(cr).build();
        CompletableFuture<IAuthenticationResult> future = application.acquireTokenSilently(parameters);

        ExecutionException ex = assertThrows(ExecutionException.class, future::get);
        assertInstanceOf(MsalInteractionRequiredException.class, ex.getCause());
    }

    @Test
    void testTokenRefreshReasons() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> responseParameters = new HashMap<>();

        //Acquire a token that expires at the same time it is acquired, so it will expire before the next acquire token call
        responseParameters.put("access_token", "expiredToken");
        responseParameters.put("id_token", TestHelper.createIdToken(new HashMap<>()));
        responseParameters.put("expires_in", "0");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        OnBehalfOfParameters parameters = OnBehalfOfParameters.builder(Collections.singleton("someScopes"), new UserAssertion(TestHelper.signedAssertion)).build();
        IAuthenticationResult result = cca.acquireToken(parameters).get();

        //There should be one token in the cache, and no refresh behavior should have happened yet
        assertRefreshedToken(result, "expiredToken", CacheRefreshReason.NOT_APPLICABLE, cca.tokenCache.accessTokens.size());

        //Attempt to retrieve the cached token, however it is expired and should be refreshed.
        // In this test, it will be replaced with a token that expires in 1 minute
        responseParameters.put("access_token", "nearlyExpiredToken");
        responseParameters.put("expires_in", "60");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        SilentParameters silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).build();
        result = cca.acquireTokenSilently(silentParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the first mocked call
        assertRefreshedToken(result, "nearlyExpiredToken", CacheRefreshReason.EXPIRED, cca.tokenCache.accessTokens.size());

        //Attempt to retrieve the cached token, however it is within the 5-minute buffer and should be refreshed.
        // In this test, it will be replaced with a token that expires in 1 hour but has a refresh_in time of 1 second
        responseParameters.put("access_token", "refreshInToken");
        responseParameters.put("expires_in", "3600");
        responseParameters.put("refresh_in", "1");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).build();
        result = cca.acquireTokenSilently(silentParameters).get();

        assertRefreshedToken(result, "refreshInToken", CacheRefreshReason.EXPIRED, cca.tokenCache.accessTokens.size());

        //Attempt to retrieve the cached token, however it is within the 5-minute buffer and should be refreshed.
        // In this test, it will be replaced with a token that expires in 1 hour (and does not have a valid refresh_in time)
        responseParameters.put("access_token", "normalToken");
        responseParameters.put("expires_in", "3600");
        responseParameters.put("refresh_in", "0");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        //refresh_in values are in seconds, so we must wait to guarantee it is past the proactive refresh time
        TimeUnit.SECONDS.sleep(2);

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).build();
        result = cca.acquireTokenSilently(silentParameters).get();

        assertRefreshedToken(result, "normalToken", CacheRefreshReason.PROACTIVE_REFRESH, cca.tokenCache.accessTokens.size());

        //Force the token to be refreshed
        responseParameters.put("access_token", "forcedRefreshToken");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).forceRefresh(true).build();
        result = cca.acquireTokenSilently(silentParameters).get();

        assertRefreshedToken(result, "forcedRefreshToken", CacheRefreshReason.FORCE_REFRESH, cca.tokenCache.accessTokens.size());

        //Finally, force a refresh by setting claims
        responseParameters.put("access_token", "claimsToken");
        TestHelper.createTokenRequestMock(httpClientMock, TestHelper.getSuccessfulTokenResponse(responseParameters), 200);

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).claims(new ClaimsRequest()).build();
        result = cca.acquireTokenSilently(silentParameters).get();

        assertRefreshedToken(result, "claimsToken", CacheRefreshReason.CLAIMS, cca.tokenCache.accessTokens.size());
    }

    //Asserts that there is one expected token in the cache, and that it was refreshed with the expected reason
    private void assertRefreshedToken(IAuthenticationResult result, String expectedToken, CacheRefreshReason expectedReason, int cacheSize) {
        assertEquals(1, cacheSize);
        assertEquals(expectedToken, result.accessToken());
        assertEquals(expectedReason, result.metadata().cacheRefreshReason());
    }

    String readResource(String resource) {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
