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
                        .instanceDiscovery(false)
                        .validateAuthority(false)
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
                        .instanceDiscovery(false)
                        .validateAuthority(false)
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
                        .instanceDiscovery(false)
                        .validateAuthority(false)
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
    // Exact cache key string validation (cross-SDK compatibility)
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
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureFMITokenExchange/.default"))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        cca.acquireToken(params).get();

        // Verify the full cache key matches the expected cross-SDK format:
        // "{homeAccountId}-{env}-{credType}-{clientId}-{tenantId}-{scopes}-{hash}" (all lowercased)
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();

        String expectedKey = "-login.windows.net-atext-3bf56293-fbb5-42bd-a407-248ba7431a8c-10c419d4-4a50-45b2-aa4e-919fb84df24f-openid profile offline_access api://azurefmitokenexchange/.default-"
                + "zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw".toLowerCase();
        assertEquals(expectedKey, cacheKey, "Full cache key should match cross-SDK format");
    }

    @Test
    void fmiPath_HashValueMatchesCrossSDK() {
        // Verify that the Java hash computation matches other MSAL SDKs for known inputs
        TreeMap<String, String> components = new TreeMap<>();
        components.put("fmi_path", "SomeFmiPath/FmiCredentialPath");

        String hash = StringHelper.computeExtCacheKeyHash(components);
        assertEquals("zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw", hash,
                "Hash for 'SomeFmiPath/FmiCredentialPath' should match cross-SDK value");

        // Second known value
        TreeMap<String, String> components2 = new TreeMap<>();
        components2.put("fmi_path", "SomeFmiPath/Path");

        String hash2 = StringHelper.computeExtCacheKeyHash(components2);
        assertEquals("7CX57Q63os7benQ6ER0sxgJPtNQSv7TGb5zexcidFoI", hash2,
                "Hash for 'SomeFmiPath/Path' should match cross-SDK value");
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
                        .instanceDiscovery(false)
                        .validateAuthority(false)
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
    // AssertionResponse (object-returning callback)
    // ========================================================================

    @Test
    void assertionResponseCallback_WithoutCert_UsesJwtBearerType() throws Exception {
        // Arrange: callback returns AssertionResponse with no certificate
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        Function<AssertionRequestOptions, AssertionResponse> responseProvider =
                options -> new AssertionResponse("my-test-assertion-jwt");

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId",
                        ClientCredentialFactory.createFromAssertionResponseCallback(responseProvider))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert: verify the request used jwt-bearer assertion type
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("client_assertion=my-test-assertion-jwt")
                    && body.contains("client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-bearer");
        }));
    }

    @Test
    void assertionResponseCallback_WithCert_UsesJwtPopType() throws Exception {
        // Arrange: callback returns AssertionResponse with a certificate
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        java.security.cert.X509Certificate mockCert = mock(java.security.cert.X509Certificate.class);

        Function<AssertionRequestOptions, AssertionResponse> responseProvider =
                options -> new AssertionResponse("my-pop-assertion-jwt", mockCert);

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId",
                        ClientCredentialFactory.createFromAssertionResponseCallback(responseProvider))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert: verify the request used jwt-pop assertion type
        verify(httpClientMock).send(argThat(request -> {
            String body = request.body();
            return body.contains("client_assertion=my-pop-assertion-jwt")
                    && body.contains("client_assertion_type=urn%3Aietf%3Aparams%3Aoauth%3Aclient-assertion-type%3Ajwt-pop");
        }));
    }

    @Test
    void assertionResponseCallback_ReceivesCorrectContext() throws Exception {
        // Arrange: track the options passed to the callback
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        AtomicReference<AssertionRequestOptions> capturedOptions = new AtomicReference<>();

        Function<AssertionRequestOptions, AssertionResponse> responseProvider = options -> {
            capturedOptions.set(options);
            return new AssertionResponse("context-assertion");
        };

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("myClientId",
                        ClientCredentialFactory.createFromAssertionResponseCallback(responseProvider))
                        .authority("https://login.microsoftonline.com/myTenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("scope"))
                .fmiPath("myAgent/path")
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert: callback received the correct context
        assertNotNull(capturedOptions.get());
        assertEquals("myClientId", capturedOptions.get().clientId());
        assertEquals("myAgent/path", capturedOptions.get().fmiPath());
        assertNotNull(capturedOptions.get().tokenEndpoint());
    }

    @Test
    void assertionResponseCallback_NullAssertion_ThrowsException() {
        // Arrange: callback returns AssertionResponse with null assertion
        Function<AssertionRequestOptions, AssertionResponse> responseProvider =
                options -> new AssertionResponse(null);

        ClientAssertion clientAssertion = new ClientAssertion(responseProvider, true);

        // Act & Assert
        assertThrows(MsalClientException.class, () -> {
            clientAssertion.assertionResponse(new AssertionRequestOptions("clientId", "endpoint", null));
        });
    }

    @Test
    void assertionResponseCallback_NullResponse_ThrowsException() {
        // Arrange: callback returns null
        Function<AssertionRequestOptions, AssertionResponse> responseProvider =
                options -> null;

        ClientAssertion clientAssertion = new ClientAssertion(responseProvider, true);

        // Act & Assert
        assertThrows(MsalClientException.class, () -> {
            clientAssertion.assertionResponse(new AssertionRequestOptions("clientId", "endpoint", null));
        });
    }

    @Test
    void assertionResponseCallback_NullProvider_ThrowsNullPointerException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            ClientCredentialFactory.createFromAssertionResponseCallback(null);
        });
    }

    @Test
    void assertionResponseCallback_WithFmiPath_CacheKeyUsesExtendedType() throws Exception {
        // Arrange: AssertionResponse callback with fmiPath should use atext credential type
        DefaultHttpClient httpClientMock = mock(DefaultHttpClient.class);

        when(httpClientMock.send(any(HttpRequest.class))).thenReturn(
                TestHelper.expectedResponse(HttpStatus.HTTP_OK,
                        TestHelper.getSuccessfulTokenResponse(new HashMap<>())));

        Function<AssertionRequestOptions, AssertionResponse> responseProvider =
                options -> new AssertionResponse("fmi-assertion");

        ConfidentialClientApplication cca =
                ConfidentialClientApplication.builder("clientId",
                        ClientCredentialFactory.createFromAssertionResponseCallback(responseProvider))
                        .authority("https://login.microsoftonline.com/tenant/")
                        .instanceDiscovery(false)
                        .validateAuthority(false)
                        .httpClient(httpClientMock)
                        .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton("api://AzureADTokenExchange/.default"))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        // Act
        cca.acquireToken(params).get();

        // Assert: cache key uses "atext" credential type with hash
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();

        String expectedKey = "-login.windows.net-atext-clientid-tenant-api://azureadtokenexchange/.default openid profile offline_access-zm2n0e62zwtsnnsozptlsooob_c7i-gfpxhyqqinjuw";
        assertEquals(expectedKey, cacheKey);
    }
}
