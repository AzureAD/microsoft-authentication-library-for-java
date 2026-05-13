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
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FmiTest {

    // ========================================================================
    // fmi_path body parameter
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentAppId123")
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
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
    // Cache key isolation (ext_cache_key)
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
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
    // ext_cache_key hash computation
    // ========================================================================

    @Test
    void computeExtCacheKeyHash_EmptyMapReturnsEmpty() {
        assertEquals("", StringHelper.computeExtCacheKeyHash(new TreeMap<>()));
        assertEquals("", StringHelper.computeExtCacheKeyHash(null));
    }

    // ========================================================================
    // Assertion context — AssertionRequestOptions
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("agentAppId456")
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert — the callback should have received the fmi_path
        assertNotNull(capturedOptions.get(), "AssertionRequestOptions should have been passed to the callback");
        assertEquals("agentAppId456", capturedOptions.get().clientAssertionFmiPath());
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert — fmiPath should be null when not set
        assertNotNull(capturedOptions.get());
        assertNull(capturedOptions.get().clientAssertionFmiPath());
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
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scopes"))
                .fmiPath("agentApp")
                .build();

        // Act — should not throw, even with fmiPath set (legacy callback ignores context)
        IAuthenticationResult result = cca.acquireToken(params).get();

        // Assert
        assertNotNull(result.accessToken());
        verify(httpClientMock, times(1)).send(any());
    }

    // ========================================================================
    // Input validation
    // ========================================================================

    @Test
    void fmiPath_BlankValueThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ClientCredentialParameters
                        .builder(Collections.singleton("scope"))
                        .fmiPath("")
                        .build());
    }

    @Test
    void fmiPath_WhitespaceOnlyThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ClientCredentialParameters
                        .builder(Collections.singleton("scope"))
                        .fmiPath("   ")
                        .build());
    }

    // ========================================================================
    // Exact cache key string validation
    // ========================================================================

    @Test
    void fmiPath_CacheKeyFormat_MatchesCrossSDKFormat() throws Exception {
        // This test verifies that the internal cache key produced by Java uses the correct
        // format: "-{env}-atext-{clientId}-{tenantId}-{scopes}-{hash}"
        // Using the same fmi_path as other SDKs' integration tests: "SomeFmiPath/FmiCredentialPath"
        // Expected hash (case-sensitive): zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw
        // The full cache key is lowercased.
        // Java resolves login.microsoftonline.com → login.windows.net (preferred alias).
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("3bf56293-fbb5-42bd-a407-248ba7431a8c",
                                ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/10c419d4-4a50-45b2-aa4e-919fb84df24f/")
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureFMITokenExchange/.default"))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        cca.acquireToken(params).get();

        // Verify the full cache key matches the expected format:
        // "{homeAccountId}-{env}-{credType}-{clientId}-{tenantId}-{scopes}-{hash}" (all lowercased)
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();

        String expectedKey = "-login.windows.net-atext-3bf56293-fbb5-42bd-a407-248ba7431a8c-10c419d4-4a50-45b2-aa4e-919fb84df24f-openid profile offline_access api://azurefmitokenexchange/.default-"
                + "zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw".toLowerCase();
        assertEquals(expectedKey, cacheKey, "Full cache key should match expected format");
    }

    @Test
    void fmiPath_HashValueMatchesCrossSDK() {
        // Verify that the hash computation produces expected values for known inputs
        TreeMap<String, String> components = new TreeMap<>();
        components.put("fmi_path", "SomeFmiPath/FmiCredentialPath");

        String hash = StringHelper.computeExtCacheKeyHash(components);
        assertEquals("zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw", hash,
                "Hash for 'SomeFmiPath/FmiCredentialPath' should match expected value");

        // Second known value
        TreeMap<String, String> components2 = new TreeMap<>();
        components2.put("fmi_path", "SomeFmiPath/Path");

        String hash2 = StringHelper.computeExtCacheKeyHash(components2);
        assertEquals("7CX57Q63os7benQ6ER0sxgJPtNQSv7TGb5zexcidFoI", hash2,
                "Hash for 'SomeFmiPath/Path' should match expected value");
    }

    @Test
    void fmiPath_NoFmiPath_CacheKeyUsesAccessTokenCredentialType() throws Exception {
        // Without fmi_path, the cache key should use "AccessToken" (not "atext")
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId",
                                ClientCredentialFactory.createFromSecret("secret"))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .aadInstanceDiscoveryResponse(TestHelper.getInstanceDiscoveryResponse())
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .build();

        cca.acquireToken(params).get();

        // Verify the full cache key uses "accesstoken" (no ext_cache_key_hash appended)
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();

        String expectedKey = "-login.windows.net-accesstoken-clientid-tenant-openid profile offline_access scope";
        assertEquals(expectedKey, cacheKey,
                "Cache key without fmi_path should use 'accesstoken' credential type and no hash suffix");
    }

}
