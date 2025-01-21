// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.net.MalformedURLException;
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
    void expiredToken_ExpiredTokenInCache() throws Exception {
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
        responseParameters.put("access_token", "OriginalToken");
        responseParameters.put("expires_in", "-3600");

        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        IAuthenticationResult resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is one token in the cache, and that it came from the (mocked) HTTP call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("OriginalToken", resultNoAccount.accessToken());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultNoAccount.metadata().tokenSource());
        verify(httpClientMock, times(1)).send(any());

        //Attempt to retrieve a token with the same scopes, which should return the cached token. However, since the cached token
        //  is expired it should be refreshed by a new HTTP call
        responseParameters.put("access_token", "RefreshedToken");
        responseParameters.put("expires_in", "600000");

        clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the first mocked call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("RefreshedToken", resultNoAccount.accessToken());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultNoAccount.metadata().tokenSource());
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void expiredToken_RefreshBufferTest() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> responseParameters = new HashMap<>();
        //Acquire a token that expires in 60 seconds, so that it's within the 5-minute refresh buffer but will not expire before test is done
        responseParameters.put("access_token", "OriginalToken");
        responseParameters.put("expires_in", "60");

        ClientCredentialParameters clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        IAuthenticationResult resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is one token in the cache, and that it came from the (mocked) HTTP call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("OriginalToken", resultNoAccount.accessToken());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultNoAccount.metadata().tokenSource());
        verify(httpClientMock, times(1)).send(any());

        //Attempt to retrieve a token with the same scopes, which should return the cached token. However, since the cached token
        //  expires within the 5-minute refresh buffer it should be refreshed by a new HTTP call
        responseParameters.put("access_token", "RefreshedToken");
        responseParameters.put("expires_in", "600000");

        clientCredentialParameters = ClientCredentialParameters.builder(Collections.singleton("someScopes")).build();
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(200, TestHelper.getSuccessfulTokenResponse(responseParameters)));
        resultNoAccount = cca.acquireToken(clientCredentialParameters).get();

        //Ensure there is still one token in the cache, however it is the new refreshed token rather than the token from the first mocked call
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals("RefreshedToken", resultNoAccount.accessToken());
        assertEquals(TokenSource.IDENTITY_PROVIDER, resultNoAccount.metadata().tokenSource());
        verify(httpClientMock, times(2)).send(any());
    }

    String readResource(String resource) {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
