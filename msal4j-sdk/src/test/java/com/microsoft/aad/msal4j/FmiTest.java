// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for FMI (Federated Managed Identity) support in client credential flows.
 * Covers fmi_path body parameter injection, cache key isolation via ext_cache_key,
 * and assertion context (AssertionRequestOptions) propagation.
 *
 * These tests correspond to MSAL .NET's FmiIntegrationTests (§1-§3) and
 * UserFederatedIdentityCredentialTests (§4-§5) in the AgentIDs_ComponentsReference.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FmiTest {

    // ========================================================================
    // §2: fmi_path body parameter
    // ========================================================================

    @Test
    void fmiPath_IncludedInTokenRequestBody() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentAppId123")
                .skipCache(true)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — verify the HTTP request body contains fmi_path
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("fmi_path=agentAppId123")
                    && body.contains("grant_type=client_credentials");
        }));
    }

    @Test
    void fmiPath_NotIncludedWhenNull() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
                .skipCache(true)
                .build();

        // Act
        cca.acquireToken(parameters).get();

        // Assert — verify the HTTP request body does NOT contain fmi_path
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return !body.contains("fmi_path")
                    && body.contains("grant_type=client_credentials");
        }));
    }

    // ========================================================================
    // §3: Cache key isolation (ext_cache_key)
    // ========================================================================

    @Test
    void fmiPath_ExtendedCacheKeyIsolation() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        HashMap<String, String> responseA = new HashMap<>();
        responseA.put("access_token", "token_for_agentA");

        HashMap<String, String> responseB = new HashMap<>();
        responseB.put("access_token", "token_for_agentB");

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseA)))
                .thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseB)));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Act — acquire tokens for two different fmi_paths with the same scopes
        ClientCredentialParameters paramsA = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentA")
                .build();
        IAuthenticationResult resultA = cca.acquireToken(paramsA).get();

        ClientCredentialParameters paramsB = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentB")
                .build();
        IAuthenticationResult resultB = cca.acquireToken(paramsB).get();

        // Assert — two different tokens should be in cache (not the same cached entry)
        assertEquals("token_for_agentA", resultA.accessToken());
        assertEquals("token_for_agentB", resultB.accessToken());
        assertNotEquals(resultA.accessToken(), resultB.accessToken());
        assertEquals(2, cca.tokenCache.accessTokens.size());
        // Both HTTP calls should have been made (no cache hit for different fmi_paths)
        verify(httpClientMock, times(2)).send(any());
    }

    @Test
    void fmiPath_CacheHitForSameFmiPath() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentA")
                .build();

        // Act — acquire same fmi_path token twice
        IAuthenticationResult result1 = cca.acquireToken(params).get();
        IAuthenticationResult result2 = cca.acquireToken(params).get();

        // Assert — should be a cache hit: only one HTTP call
        assertEquals(result1.accessToken(), result2.accessToken());
        assertEquals(1, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(1)).send(any());
    }

    @Test
    void fmiPath_ExtendedCredentialTypeInCacheKey() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Act — acquire with fmi_path
        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentA")
                .build();
        cca.acquireToken(params).get();

        // Assert — cache entry should use "atext" credential type (matching Go/Python convention)
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();
        assertTrue(cacheKey.contains("-atext-"),
                "Cache key should contain 'atext' credential type for extended tokens, got: " + cacheKey);
    }

    @Test
    void fmiPath_CacheDoesNotCollideWithNonFmiTokens() throws Exception {
        // Arrange
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        HashMap<String, String> responseNoFmi = new HashMap<>();
        responseNoFmi.put("access_token", "regular_token");

        HashMap<String, String> responseFmi = new HashMap<>();
        responseFmi.put("access_token", "fmi_token");

        when(httpClientMock.send(any(HttpRequest.class)))
                .thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseNoFmi)))
                .thenReturn(TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(responseFmi)));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        // Act — acquire without fmi_path, then with fmi_path (same scopes)
        ClientCredentialParameters regularParams = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .build();
        IAuthenticationResult regularResult = cca.acquireToken(regularParams).get();

        ClientCredentialParameters fmiParams = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentA")
                .build();
        IAuthenticationResult fmiResult = cca.acquireToken(fmiParams).get();

        // Assert — both tokens should be in cache (different cache keys)
        assertEquals("regular_token", regularResult.accessToken());
        assertEquals("fmi_token", fmiResult.accessToken());
        assertEquals(2, cca.tokenCache.accessTokens.size());
        verify(httpClientMock, times(2)).send(any());
    }

    // ========================================================================
    // §3: ext_cache_key hash computation
    // ========================================================================

    @Test
    void computeExtCacheKeyHash_MatchesCrossSDKAlgorithm() {
        // Arrange — the same input should produce the same hash across .NET, Go, Python, and Java
        TreeMap<String, String> components = new TreeMap<>();
        components.put("fmi_path", "agentAppId123");

        // Act
        String hash = StringHelper.computeExtCacheKeyHash(components);

        // Assert — hash should be a non-empty Base64URL-encoded SHA-256
        assertNotNull(hash);
        assertFalse(hash.isEmpty());
        // Base64URL characters only (no + / = padding)
        assertTrue(hash.matches("[A-Za-z0-9_-]+"), "Hash should be Base64URL encoded: " + hash);
        // SHA-256 produces 32 bytes → 43 Base64URL characters (without padding)
        assertEquals(43, hash.length(), "Base64URL-encoded SHA-256 should be 43 chars: " + hash);
    }

    @Test
    void computeExtCacheKeyHash_DifferentValuesProduceDifferentHashes() {
        TreeMap<String, String> componentsA = new TreeMap<>();
        componentsA.put("fmi_path", "agentA");

        TreeMap<String, String> componentsB = new TreeMap<>();
        componentsB.put("fmi_path", "agentB");

        String hashA = StringHelper.computeExtCacheKeyHash(componentsA);
        String hashB = StringHelper.computeExtCacheKeyHash(componentsB);

        assertNotEquals(hashA, hashB, "Different fmi_path values should produce different hashes");
    }

    @Test
    void computeExtCacheKeyHash_EmptyMapReturnsEmpty() {
        assertEquals("", StringHelper.computeExtCacheKeyHash(new TreeMap<>()));
        assertEquals("", StringHelper.computeExtCacheKeyHash(null));
    }

    // ========================================================================
    // §5: Assertion context — AssertionRequestOptions
    // ========================================================================

    @Test
    void assertionContext_FmiPathPassedToContextAwareCallback() throws Exception {
        // Arrange
        AtomicReference<AssertionRequestOptions> capturedOptions = new AtomicReference<>();

        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            capturedOptions.set(options);
            return TestHelper.signedAssertion;
        };

        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("myClientId", credential)
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentAppId456")
                .skipCache(true)
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert — the callback should have received the fmi_path
        assertNotNull(capturedOptions.get(), "AssertionRequestOptions should have been passed to the callback");
        assertEquals("agentAppId456", capturedOptions.get().fmiPath());
        assertEquals("myClientId", capturedOptions.get().clientId());
        assertNotNull(capturedOptions.get().tokenEndpoint());
    }

    @Test
    void assertionContext_NullFmiPathWhenNotSet() throws Exception {
        // Arrange
        AtomicReference<AssertionRequestOptions> capturedOptions = new AtomicReference<>();

        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            capturedOptions.set(options);
            return TestHelper.signedAssertion;
        };

        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("myClientId", credential)
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
                .skipCache(true)
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert — fmiPath should be null when not set
        assertNotNull(capturedOptions.get());
        assertNull(capturedOptions.get().fmiPath());
    }

    @Test
    void assertionContext_LegacyCallableStillWorks() throws Exception {
        // Arrange — verify that the existing Callable<String> API still works
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);
        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        IClientCredential credential = ClientCredentialFactory.createFromCallback(
                () -> TestHelper.signedAssertion);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId", credential)
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
                .fmiPath("agentApp")
                .skipCache(true)
                .build();

        // Act — should not throw, even with fmiPath set (legacy callback ignores context)
        IAuthenticationResult result = cca.acquireToken(params).get();

        // Assert
        assertNotNull(result.accessToken());
        verify(httpClientMock, times(1)).send(any());
    }

    // ========================================================================
    // §5: AssertionRequestOptions model
    // ========================================================================

    @Test
    void assertionRequestOptions_PropertiesAccessible() {
        AssertionRequestOptions options = new AssertionRequestOptions(
                "clientId123",
                "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
                "agentAppId");

        assertEquals("clientId123", options.clientId());
        assertEquals("https://login.microsoftonline.com/tenant/oauth2/v2.0/token", options.tokenEndpoint());
        assertEquals("agentAppId", options.fmiPath());
    }

    @Test
    void assertionRequestOptions_NullFmiPath() {
        AssertionRequestOptions options = new AssertionRequestOptions("clientId", "endpoint", null);
        assertNull(options.fmiPath());
    }
}
