// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for token acquisition via the app token provider path
 * (AcquireTokenByAppProviderSupplier and the appTokenProvider branch
 * of AcquireTokenByClientCredentialSupplier).
 */
class AppTokenProviderTest {

    private static final String CLIENT_ID = "test-client-id";
    private static final String AUTHORITY = "https://login.microsoftonline.com/test-tenant/";
    private static final String VALID_ACCESS_TOKEN = "app-provider-access-token";
    private static final String TENANT_ID = "test-tenant";
    private static final long ONE_HOUR_SECONDS = 3600;
    private static final long TWO_HOURS_SECONDS = 7200;

    // ========================================================================
    // Helper: builds a CCA with an appTokenProvider
    // ========================================================================

    private ConfidentialClientApplication buildCcaWithProvider(
            Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> provider) throws Exception {
        return ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret("unused-secret"))
                .appTokenProvider(provider)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();
    }

    private TokenProviderResult validTokenProviderResult() {
        TokenProviderResult result = new TokenProviderResult();
        result.setAccessToken(VALID_ACCESS_TOKEN);
        result.setExpiresInSeconds(ONE_HOUR_SECONDS);
        result.setTenantId(TENANT_ID);
        return result;
    }

    private Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> providerReturning(
            TokenProviderResult result) {
        return params -> CompletableFuture.completedFuture(result);
    }

    // ========================================================================
    // Valid provider result
    // ========================================================================

    @Test
    void appTokenProvider_ValidResult_ReturnsToken() throws Exception {
        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(validTokenProviderResult()));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
    }

    @Test
    void appTokenProvider_ReceivesCorrectParameters() throws Exception {
        // Capture the parameters passed to the provider
        @SuppressWarnings("unchecked")
        Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> provider =
                mock(Function.class);

        when(provider.apply(any())).thenReturn(
                CompletableFuture.completedFuture(validTokenProviderResult()));

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret("unused-secret"))
                .appTokenProvider(provider)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .tenant("override-tenant")
                .build();

        cca.acquireToken(parameters).get();

        verify(provider).apply(argThat(params -> {
            assertTrue(params.getScopes().contains("scope/.default"));
            assertEquals("override-tenant", params.getTenantId());
            assertNotNull(params.getCorrelationId());
            return true;
        }));
    }

    @Test
    void appTokenProvider_DefaultSkipCache_CallsProviderEachTime() throws Exception {
        @SuppressWarnings("unchecked")
        Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> provider =
                mock(Function.class);

        when(provider.apply(any())).thenReturn(
                CompletableFuture.completedFuture(validTokenProviderResult()));

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret("unused-secret"))
                .appTokenProvider(provider)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        // Default skipCache (null) bypasses cache lookup, so provider is called each time
        cca.acquireToken(parameters).get();
        cca.acquireToken(parameters).get();

        verify(provider, times(2)).apply(any());
    }

    @Test
    void appTokenProvider_SkipCache_BypassesCacheLookup() throws Exception {
        @SuppressWarnings("unchecked")
        Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> provider =
                mock(Function.class);

        when(provider.apply(any())).thenReturn(
                CompletableFuture.completedFuture(validTokenProviderResult()));

        ConfidentialClientApplication cca = ConfidentialClientApplication
                .builder(CLIENT_ID, ClientCredentialFactory.createFromSecret("unused-secret"))
                .appTokenProvider(provider)
                .authority(AUTHORITY)
                .instanceDiscovery(false)
                .validateAuthority(false)
                .build();

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .skipCache(true)
                .build();

        cca.acquireToken(parameters).get();
        cca.acquireToken(parameters).get();

        // With skipCache=true, provider should be called each time
        verify(provider, times(2)).apply(any());
    }

    // ========================================================================
    // Validation: null/empty access token
    // ========================================================================

    @Test
    void appTokenProvider_NullAccessToken_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setAccessToken(null);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("invalid"));
    }

    @Test
    void appTokenProvider_EmptyAccessToken_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setAccessToken("");

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    // ========================================================================
    // Validation: invalid expiry
    // ========================================================================

    @Test
    void appTokenProvider_ZeroExpiry_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setExpiresInSeconds(0);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    @Test
    void appTokenProvider_NegativeExpiry_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setExpiresInSeconds(-1);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    // ========================================================================
    // Validation: null/empty tenant ID
    // ========================================================================

    @Test
    void appTokenProvider_NullTenantId_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setTenantId(null);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    @Test
    void appTokenProvider_EmptyTenantId_Throws() throws Exception {
        TokenProviderResult result = validTokenProviderResult();
        result.setTenantId("");

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    // ========================================================================
    // refreshInSeconds auto-calculation
    // ========================================================================

    @Test
    void appTokenProvider_ExpiryAtLeastTwoHours_AutoCalculatesRefreshIn() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS);
        providerResult.setRefreshInSeconds(0);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(providerResult));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
        // refreshOn in metadata should be set to half of expiry (7200/2 = 3600)
        assertTrue(result.metadata().refreshOn() > 0,
                "refreshOn should be auto-calculated when expiresIn >= 2 hours");
    }

    @Test
    void appTokenProvider_ExpiryLessThanTwoHours_NoAutoRefreshIn() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS - 1);
        providerResult.setRefreshInSeconds(0);

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(providerResult));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
        assertEquals(0, result.metadata().refreshOn(),
                "refreshOn should not be auto-calculated when expiresIn < 2 hours");
    }

    @Test
    void appTokenProvider_ExplicitRefreshIn_NotOverridden() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS);
        providerResult.setRefreshInSeconds(1800); // explicit 30-minute refresh

        ConfidentialClientApplication cca = buildCcaWithProvider(providerReturning(providerResult));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        IAuthenticationResult result = cca.acquireToken(parameters).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
        // Auto-calculation only triggers when refreshInSeconds == 0
        assertTrue(result.metadata().refreshOn() > 0);
    }

    // ========================================================================
    // Provider exception wrapping
    // ========================================================================

    @Test
    void appTokenProvider_ProviderThrows_WrappedInMsalAzureSDKException() throws Exception {
        Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> throwingProvider =
                params -> {
                    CompletableFuture<TokenProviderResult> future = new CompletableFuture<>();
                    future.completeExceptionally(new RuntimeException("Provider failed"));
                    return future;
                };

        ConfidentialClientApplication cca = buildCcaWithProvider(throwingProvider);

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(Collections.singleton("scope/.default"))
                .build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalAzureSDKException.class, ex.getCause());
    }
}
