// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Collections;
import java.util.HashMap;

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

        assertThrows(NullPointerException.class, () ->
                new ClientAssertion(null));
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
    void testCredentialPrecedenceAndMixing() throws Exception {
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        // Create different credential types for testing
        IClientCredential appLevelCredential = ClientCredentialFactory.createFromSecret("appLevelSecret");
        IClientCredential requestLevelSecret = ClientCredentialFactory.createFromSecret("requestLevelSecret");
        String assertionValue = "test_assertion_value";
        IClientCredential requestLevelAssertion = ClientCredentialFactory.createFromClientAssertion(assertionValue);

        // Create the application with the app-level credential
        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", appLevelCredential)
                        .authority("https://login.microsoftonline.com/tenant")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Set up the mock to check which credential is being used
        when(httpClientMock.send(any(HttpRequest.class))).thenAnswer(invocation -> {
            HttpRequest request = invocation.getArgument(0);
            String requestBody = request.body();

            // Check which credential type is included in the request and return a matching token
            if (requestBody.contains("client_secret=requestLevelSecret")) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "request_secret_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            } else if (requestBody.contains("client_secret=appLevelSecret")) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "app_secret_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            } else if (requestBody.contains("client_assertion=" + assertionValue)) {
                HashMap<String, String> responseParams = new HashMap<>();
                responseParams.put("access_token", "assertion_token");
                return TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseParams));
            }
            return null;
        });

        // Test 1: Request with same credential type (secret) at request level
        ClientCredentialParameters parametersWithRequestSecret =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .clientCredential(requestLevelSecret)
                        .skipCache(true)
                        .build();

        IAuthenticationResult result1 = cca.acquireToken(parametersWithRequestSecret).get();
        assertEquals("request_secret_token", result1.accessToken(),
                "Request-level secret should be used when provided");

        // Test 2: Request with different credential type (assertion) at request level
        ClientCredentialParameters parametersWithAssertion =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .clientCredential(requestLevelAssertion)
                        .skipCache(true)
                        .build();

        IAuthenticationResult result2 = cca.acquireToken(parametersWithAssertion).get();
        assertEquals("assertion_token", result2.accessToken(),
                "Request-level assertion should be used when provided");

        // Test 3: Request without credential specified should fall back to app-level
        ClientCredentialParameters parametersWithoutCredential =
                ClientCredentialParameters.builder(Collections.singleton("scope"))
                        .skipCache(true)
                        .build();

        IAuthenticationResult result3 = cca.acquireToken(parametersWithoutCredential).get();
        assertEquals("app_secret_token", result3.accessToken(),
                "App-level credential should be used when request-level credential is not provided");
    }
}
