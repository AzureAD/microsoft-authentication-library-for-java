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

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
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

    String readResource(String resource) {
        try {
            return new String(Files.readAllBytes(Paths.get(getClass().getResource(resource).toURI())));
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
