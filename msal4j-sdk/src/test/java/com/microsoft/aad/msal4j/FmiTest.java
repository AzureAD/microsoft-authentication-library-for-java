// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedMap;
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

    @Test
    void fmiPath_NullValueThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                ClientCredentialParameters
                        .builder(Collections.singleton("scope"))
                        .fmiPath(null)
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
        // Expected hash (case-sensitive): cojvFy5tZae3nJPKVceBguvVx5vvMNJ8hPHQRbOgjOI
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
                + "cojvFy5tZae3nJPKVceBguvVx5vvMNJ8hPHQRbOgjOI".toLowerCase();
        assertEquals(expectedKey, cacheKey, "Full cache key should match expected format");
    }

    @Test
    void fmiPath_HashValueMatchesCrossSDK() {
        // Verify that the hash computation produces expected values for known inputs
        TreeMap<String, String> components = new TreeMap<>();
        components.put("fmi_path", "SomeFmiPath/FmiCredentialPath");

        String hash = StringHelper.computeExtCacheKeyHash(components);
        assertEquals("cojvFy5tZae3nJPKVceBguvVx5vvMNJ8hPHQRbOgjOI", hash,
                "Hash for 'SomeFmiPath/FmiCredentialPath' should match expected value");

        // Second known value
        TreeMap<String, String> components2 = new TreeMap<>();
        components2.put("fmi_path", "SomeFmiPath/Path");

        String hash2 = StringHelper.computeExtCacheKeyHash(components2);
        assertEquals("HaI-Va57U1u3bj1ELRa_dz5BpgHfTDMYv5vUyFoPBQo", hash2,
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

    // ========================================================================
    // Cache filter isolation: FMI tokens not returned for non-FMI requests (and vice versa)
    // ========================================================================

    @Test
    void fmiPath_CacheIsolation_FmiTokenNotReturnedForNonFmiRequest() throws Exception {
        // Seed cache with an FMI-tagged token, then verify a non-FMI request does NOT
        // return it from cache (goes to IdP instead).
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

        // First request WITH fmi_path — seeds cache with an FMI-tagged token
        ClientCredentialParameters fmiParams = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .fmiPath("agentApp1")
                .build();
        cca.acquireToken(fmiParams).get();
        assertEquals(1, cca.tokenCache.accessTokens.size());

        // Second request WITHOUT fmi_path — should NOT get the FMI token from cache
        ClientCredentialParameters nonFmiParams = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .build();
        cca.acquireToken(nonFmiParams).get();

        // Both tokens should now be in cache (FMI miss → went to IdP → stored as non-FMI)
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "Non-FMI request should not match FMI-tagged cache entry; both tokens should exist");
    }

    @Test
    void fmiPath_CacheIsolation_NonFmiTokenNotReturnedForFmiRequest() throws Exception {
        // Seed cache with a non-FMI token, then verify an FMI request does NOT
        // return it from cache (goes to IdP instead).
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

        // First request WITHOUT fmi_path — seeds cache with a non-FMI token
        ClientCredentialParameters nonFmiParams = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .build();
        cca.acquireToken(nonFmiParams).get();
        assertEquals(1, cca.tokenCache.accessTokens.size());

        // Second request WITH fmi_path — should NOT get the non-FMI token from cache
        ClientCredentialParameters fmiParams = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .fmiPath("agentApp1")
                .build();
        cca.acquireToken(fmiParams).get();

        // Both tokens should now be in cache (FMI miss → went to IdP → stored as FMI)
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "FMI request should not match non-FMI cache entry; both tokens should exist");
    }

    @Test
    void fmiPath_CacheIsolation_DifferentFmiPathsNotShared() throws Exception {
        // Two different fmi_path values should produce separate cache entries
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

        // Request with fmi_path "agentA"
        ClientCredentialParameters paramsA = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .fmiPath("agentA")
                .build();
        cca.acquireToken(paramsA).get();

        // Request with fmi_path "agentB"
        ClientCredentialParameters paramsB = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .fmiPath("agentB")
                .build();
        cca.acquireToken(paramsB).get();

        // Each fmi_path produces a different hash → different cache key → 2 entries
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "Different fmi_path values should produce separate cache entries");
    }

    // ========================================================================
    // Extended cache-key hash: collision resistance & cross-SDK consistency
    //
    // computeExtCacheKeyHash serializes each sorted entry as
    // "<utf8ByteLen(key)>:<key><utf8ByteLen(value)>:<value>" then SHA-256 → Base64URL (no pad).
    // The length prefixes make it injective, so distinct component sets never collide.
    // ========================================================================

    // A deliberately adversarial alphabet: the encoding's own delimiter/digit characters,
    // escapes, the empty string, and multibyte characters (2-byte 'é', a combining sequence,
    // and a 4-byte emoji) that expose UTF-16-vs-UTF-8 length bugs.
    private static final String[] ADVERSARIAL_TOKENS = {
            "", "0", "1", "9", ":", "|", "\\", "a", "ab",
            "\u00e9",            // 'é' as a single 2-byte code point (U+00E9)
            "e\u0301",           // 'e' + combining acute accent (U+0301), 3 bytes, 2 UTF-16 units
            "\uD83D\uDE00"       // 😀 emoji, 4 bytes, 2 UTF-16 units (surrogate pair)
    };

    private static String stripPadding(String s) {
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '=') {
            end--;
        }
        return s.substring(0, end);
    }

    private static SortedMap<String, String> map(String... kv) {
        TreeMap<String, String> m = new TreeMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put(kv[i], kv[i + 1]);
        }
        return m;
    }

    @Test
    void extCacheKeyHash_Injectivity_NoCollisionsOverAdversarialAlphabet() {
        // Build a large set of DISTINCT component maps over the adversarial alphabet. Because the
        // encoding is injective, every distinct map must produce a distinct hash. Using a Set of
        // TreeMaps de-duplicates inputs by value (AbstractMap.equals), so any shortfall between the
        // number of distinct inputs and the number of distinct hashes is a genuine collision.
        Set<SortedMap<String, String>> inputs = new HashSet<>();

        // Single-entry maps: every (key, value) pair.
        for (String k : ADVERSARIAL_TOKENS) {
            for (String v : ADVERSARIAL_TOKENS) {
                inputs.add(map(k, v));
            }
        }

        // Two-entry maps: distinct keys with a couple of value assignments, including values that
        // themselves look like length prefixes ("1:x") to stress boundary ambiguity.
        String[] values = {"", "x", "1:x", ":", "9"};
        for (int i = 0; i < ADVERSARIAL_TOKENS.length; i++) {
            for (int j = i + 1; j < ADVERSARIAL_TOKENS.length; j++) {
                String k1 = ADVERSARIAL_TOKENS[i];
                String k2 = ADVERSARIAL_TOKENS[j];
                if (k1.equals(k2)) {
                    continue;
                }
                for (String v : values) {
                    inputs.add(map(k1, v, k2, "z"));
                    inputs.add(map(k1, "z", k2, v));
                }
            }
        }

        Set<String> hashes = new HashSet<>();
        for (SortedMap<String, String> input : inputs) {
            hashes.add(StringHelper.computeExtCacheKeyHash(input));
        }

        assertEquals(inputs.size(), hashes.size(),
                "Every distinct component map must hash to a distinct value (no collisions)");
    }

    @Test
    void extCacheKeyHash_KeyValueBoundaryAmbiguity_ProducesDistinctHashes() {
        // The classic delimiter-less bug: {fmi_path:"value"} and {fmi_pat:"hvalue"} both used to
        // serialize to "fmi_pathvalue". Length prefixes disambiguate them.
        assertNotEquals(
                StringHelper.computeExtCacheKeyHash(map("fmi_path", "value")),
                StringHelper.computeExtCacheKeyHash(map("fmi_pat", "hvalue")),
                "Key/value boundary-ambiguous inputs must not collide");

        // Multi-entry boundary ambiguity: {a:"b", cd:"e"} vs {ab:"c", d:"e"}.
        assertNotEquals(
                StringHelper.computeExtCacheKeyHash(map("a", "b", "cd", "e")),
                StringHelper.computeExtCacheKeyHash(map("ab", "c", "d", "e")),
                "Multi-entry boundary-ambiguous inputs must not collide");
    }

    @Test
    void extCacheKeyHash_InputOrderIndependent() {
        // Same components inserted in different orders must yield the same hash (the map is sorted).
        TreeMap<String, String> forward = new TreeMap<>();
        forward.put("a", "1");
        forward.put("b", "2");
        forward.put("fmi_path", "p");

        TreeMap<String, String> reverse = new TreeMap<>();
        reverse.put("fmi_path", "p");
        reverse.put("b", "2");
        reverse.put("a", "1");

        assertEquals(
                StringHelper.computeExtCacheKeyHash(forward),
                StringHelper.computeExtCacheKeyHash(reverse),
                "Hash must depend only on the sorted components, not insertion order");
    }

    @Test
    void extCacheKeyHash_UsesUtf8ByteLength_NotStringLength() {
        // 'é' (U+00E9) is 1 UTF-16 unit but 2 UTF-8 bytes; 'e' + combining accent (U+0301) is
        // 2 UTF-16 units and 3 UTF-8 bytes. If the encoding used String.length() (UTF-16 units)
        // instead of UTF-8 byte length, these could alias. They must not collide, and neither may
        // collide with plain ASCII "e".
        String precomposed = StringHelper.computeExtCacheKeyHash(map("\u00e9", "\u00e9"));
        String decomposed = StringHelper.computeExtCacheKeyHash(map("e\u0301", "e\u0301"));
        String ascii = StringHelper.computeExtCacheKeyHash(map("e", "e"));

        assertNotEquals(precomposed, decomposed,
                "Precomposed 'é' and 'e' + combining accent must hash differently (UTF-8 byte length)");
        assertNotEquals(precomposed, ascii);
        assertNotEquals(decomposed, ascii);

        // A 4-byte emoji vs its concatenation with an extra char must also stay distinct.
        assertNotEquals(
                StringHelper.computeExtCacheKeyHash(map("k", "\uD83D\uDE00")),
                StringHelper.computeExtCacheKeyHash(map("k", "\uD83D\uDE00x")));
    }

    @Test
    void extCacheKeyHash_EmptyAndSingleEntryEdges() {
        // Null and empty map short-circuit to "".
        assertEquals("", StringHelper.computeExtCacheKeyHash(null));
        assertEquals("", StringHelper.computeExtCacheKeyHash(new TreeMap<>()));

        // Single entry and empty-value entries are all distinct and non-empty.
        String single = StringHelper.computeExtCacheKeyHash(map("fmi_path", "p"));
        String emptyValue = StringHelper.computeExtCacheKeyHash(map("fmi_path", ""));
        String emptyKey = StringHelper.computeExtCacheKeyHash(map("", "p"));

        assertNotEquals("", single);
        assertNotEquals("", emptyValue);
        assertNotEquals("", emptyKey);
        assertNotEquals(single, emptyValue);
        assertNotEquals(single, emptyKey);
        assertNotEquals(emptyValue, emptyKey);
    }

    @Test
    void extCacheKeyHash_IsBase64UrlWithoutPadding() {
        String hash = StringHelper.computeExtCacheKeyHash(map("fmi_path", "agent-app-id"));
        assertFalse(hash.contains("="), "Hash must be Base64URL without padding");
        assertFalse(hash.contains("+"), "Hash must use the URL-safe alphabet (no '+')");
        assertFalse(hash.contains("/"), "Hash must use the URL-safe alphabet (no '/')");
    }

    @Test
    void extCacheKeyHash_GoldenVectors_MatchCrossSdk() {
        // Byte-identical across MSAL SDKs (Go/.NET/Python/JS share the length-prefix fix). Reference
        // vectors are lowercased Base64URL-no-pad; this method preserves the encoder's mixed case,
        // so compare case-insensitively (padding already absent, but strip defensively).
        assertGoldenVector(map("fmi_path", "agent-app-id"),
                "a0ry_zl4gccsdp7gnw927x8s0mrmnodv6tyilt0u07m");
        assertGoldenVector(map("a", "b", "cd", "e"),
                "cybgactkrvlzlen1aiwzwl3ay5krkyixommrobc-ri4");
        assertGoldenVector(map("fmi_path", "value"),
                "n_lucewkadzv_nybtg-2wtorgf2nrns6ihlfa7vbuzg");
        assertGoldenVector(map("fmi_pat", "hvalue"),
                "tjtm16m-suk2_bkniblr25lyuki40qyceco7knuyu0k");
        assertGoldenVector(map("\u00e9", "\u00e9"),
                "xskzaoz4ibr3mznftyxctvg1ptuh-0fuzpty7ndbfls");
    }

    private static void assertGoldenVector(SortedMap<String, String> components, String expectedLower) {
        String actual = stripPadding(StringHelper.computeExtCacheKeyHash(components));
        assertTrue(expectedLower.equalsIgnoreCase(actual),
                "Cross-SDK golden vector mismatch: expected " + expectedLower + " but got " + actual);
    }

}
