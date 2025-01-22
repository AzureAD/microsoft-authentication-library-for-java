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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;


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

        //Acquire a token that expired an hour ago
        responseParameters.put("access_token", "expiredToken");
        responseParameters.put("id_token", TestHelper.createIdToken(new HashMap<>()));
        responseParameters.put("expires_in", "-3600");

        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        IAuthenticationResult result = cca.acquireToken(clientCredentialParameters).get();

        //There should be one token in the cache, and no refresh behavior should have happened yet
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("expiredToken", result.accessToken());
        assertEquals(CacheRefreshReason.NOT_APPLICABLE, result.metadata().cacheRefreshReason());
        verify(httpClientMock, times(1)).send(any());

        //Attempt to retrieve the cached token, however it is expired and should be refreshed.
        // In this test, it will be replaced with a token that expires in 1 minute
        responseParameters.put("access_token", "nearlyExpiredToken");
        responseParameters.put("expires_in", "60");

        SilentParameters silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        result = cca.acquireTokenSilently(silentParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the first mocked call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("nearlyExpiredToken", result.accessToken());
        assertEquals(CacheRefreshReason.EXPIRED, result.metadata().cacheRefreshReason());
        verify(httpClientMock, times(2)).send(any());

        //Attempt to retrieve the cached token, however it is within the 5-minute buffer and should be refreshed.
        // In this test, it will be replaced with a token that expires in 1 hour
        responseParameters.put("access_token", "normalToken");
        responseParameters.put("expires_in", "3600");

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        result = cca.acquireTokenSilently(silentParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the second mocked call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("normalToken", result.accessToken());
        assertEquals(CacheRefreshReason.EXPIRED, result.metadata().cacheRefreshReason());
        verify(httpClientMock, times(3)).send(any());

        //Finally, force the token to be refreshed
        responseParameters.put("access_token", "forcedRefreshToken");

        silentParameters = SilentParameters.builder(Collections.singleton("someScopes"), result.account()).forceRefresh(true).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        result = cca.acquireTokenSilently(silentParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the third mocked call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("forcedRefreshToken", result.accessToken());
        assertEquals(CacheRefreshReason.FORCE_REFRESH, result.metadata().cacheRefreshReason());
        verify(httpClientMock, times(4)).send(any());
    }

    String readResource(String resource) {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
