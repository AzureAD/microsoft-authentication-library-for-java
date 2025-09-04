// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ClientCredentialTest {

    @Test
    void testAssertionNullAndEmpty() {
        assertThrows(NullPointerException.class, () ->
                new ClientAssertion(""));

        // Cast to String to resolve ambiguity between constructors
        assertThrows(NullPointerException.class, () ->
                new ClientAssertion((String) null));
    }

    @Test
    void testSecretNullAndEmpty() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(""));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));

        assertThrows(IllegalArgumentException.class, () ->
                new ClientSecret(null));

        assertTrue(ex.getMessage().contains("clientSecret is null or empty"));
    }

    @Test
    void OnBehalfOf_InternalCacheLookup_Success() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();
        IAuthenticationResult result2 = cca.acquireToken(parameters).get();

        //OBO flow should perform an internal cache lookup, so similar parameters should only cause one HTTP client call
        assertEquals(result.accessToken(), result2.accessToken());
        verify(httpClientMock, times(1)).send(any());
    }

    @Test
    void OnBehalfOf_TenantOverride() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("password"))
                        .authority("https://login.microsoftonline.com/tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        HashMap<String, String> tokenResponseValues = new HashMap<>();
        tokenResponseValues.put("access_token", "accessTokenFirstCall");

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(tokenResponseValues)));
        ClientCredentialParameters parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).build();

        //The two acquireToken calls have the same parameters...
        IAuthenticationResult resultAppLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultAppLevelTenantCached = cca.acquireToken(parameters).get();
        //...so only one token should be added to the cache, and the mocked HTTP client's "send" method should only have been called once
        assertEquals(1, cca.tokenCache.accessTokens.size());
        assertEquals(resultAppLevelTenant.accessToken(), resultAppLevelTenantCached.accessToken());
        verify(httpClientMock, times(1)).send(any());

        tokenResponseValues.put("access_token", "accessTokenSecondCall");

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK, TestHelper.getSuccessfulTokenResponse(tokenResponseValues)));
        parameters = ClientCredentialParameters.builder(Collections.singleton("scopes")).tenant("otherTenant").build();

        //Overriding the tenant parameter in the request should lead to a new token call being made...
        IAuthenticationResult resultRequestLevelTenant = cca.acquireToken(parameters).get();
        IAuthenticationResult resultRequestLevelTenantCached = cca.acquireToken(parameters).get();
        //...which should be different from the original token, and thus the cache should have two tokens created from two HTTP calls
        assertEquals(2, cca.tokenCache.accessTokens.size());
        assertEquals(resultRequestLevelTenant.accessToken(), resultRequestLevelTenantCached.accessToken());
        assertNotEquals(resultAppLevelTenant.accessToken(), resultRequestLevelTenant.accessToken());
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void acquireTokenClientCredentials_Callback() {
        // Create a counter to track how many times our callback is invoked
        final AtomicInteger callCounter = new AtomicInteger(0);

        // Create a callable that returns a different value each time it's called
        // by including the counter value in the returned string
        Callable<String> callable = () -> {
            int currentCount = callCounter.incrementAndGet();
            return "assertion_" + currentCount;
        };

        // Create the client assertion using our callback
        IClientAssertion credential = ClientCredentialFactory.createFromCallback(callable);

        // Each call to assertion() should invoke our callable
        String assertion1 = credential.assertion();
        String assertion2 = credential.assertion();
        String assertion3 = credential.assertion();

        // Verify the callable was called three times, generating three different assertions
        assertEquals(3, callCounter.get(), "Callable should have been invoked exactly three times");
        assertEquals("assertion_1", assertion1, "First assertion value should match first call");
        assertEquals("assertion_2", assertion2, "Second assertion value should match second call");
        assertEquals("assertion_3", assertion3, "Third assertion value should match third call");

        // Verify assertions are different from each other
        assertNotEquals(assertion1, assertion2, "First and second assertions should be different");
        assertNotEquals(assertion2, assertion3, "Second and third assertions should be different");
    }
}
