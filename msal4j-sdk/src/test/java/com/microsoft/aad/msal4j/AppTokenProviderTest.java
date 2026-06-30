// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for token acquisition via the app token provider path
 * (AcquireTokenByAppProviderSupplier and the appTokenProvider branch
 * of AcquireTokenByClientCredentialSupplier).
 */
class AppTokenProviderTest {

    private static final String VALID_ACCESS_TOKEN = "app-provider-access-token";
    private static final String TENANT_ID = "test-tenant";
    private static final long ONE_HOUR_SECONDS = 3600;
    private static final long TWO_HOURS_SECONDS = 7200;

    // ========================================================================
    // Helpers
    // ========================================================================

    private static TokenProviderResult validTokenProviderResult() {
        TokenProviderResult result = new TokenProviderResult();
        result.setAccessToken(VALID_ACCESS_TOKEN);
        result.setExpiresInSeconds(ONE_HOUR_SECONDS);
        result.setTenantId(TENANT_ID);
        return result;
    }

    private static Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> providerReturning(
            TokenProviderResult result) {
        return params -> CompletableFuture.completedFuture(result);
    }

    private static TokenProviderResult invalidResult(Consumer<TokenProviderResult> mutator) {
        TokenProviderResult result = validTokenProviderResult();
        mutator.accept(result);
        return result;
    }

    // ========================================================================
    // Valid provider result
    // ========================================================================

    @Test
    void appTokenProvider_ValidResult_ReturnsToken() throws Exception {
        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(
                providerReturning(validTokenProviderResult()));

        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build()).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
    }

    @Test
    void appTokenProvider_ReceivesCorrectParameters() throws Exception {
        @SuppressWarnings("unchecked")
        Function<AppTokenProviderParameters, CompletableFuture<TokenProviderResult>> provider =
                mock(Function.class);

        when(provider.apply(any())).thenReturn(
                CompletableFuture.completedFuture(validTokenProviderResult()));

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(provider);

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(TestHelper.TEST_SCOPE_SET)
                .tenant("override-tenant")
                .build();

        cca.acquireToken(parameters).get();

        verify(provider).apply(argThat(params -> {
            assertTrue(params.getScopes().contains(TestHelper.TEST_SCOPE));
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

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(provider);

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(TestHelper.TEST_SCOPE_SET).build();

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

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(provider);

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(TestHelper.TEST_SCOPE_SET)
                .skipCache(true)
                .build();

        cca.acquireToken(parameters).get();
        cca.acquireToken(parameters).get();

        // With skipCache=true, provider should be called each time
        verify(provider, times(2)).apply(any());
    }

    // ========================================================================
    // Validation: invalid TokenProviderResult fields
    // ========================================================================

    @ParameterizedTest(name = "appTokenProvider_InvalidResult_{0}_Throws")
    @MethodSource("invalidTokenProviderResults")
    void appTokenProvider_InvalidResult_Throws(String scenario, TokenProviderResult result) throws Exception {
        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(providerReturning(result));

        ClientCredentialParameters parameters = ClientCredentialParameters
                .builder(TestHelper.TEST_SCOPE_SET).build();

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(parameters).get());
        assertInstanceOf(MsalClientException.class, ex.getCause());
    }

    private static Stream<Arguments> invalidTokenProviderResults() {
        return Stream.of(
                Arguments.of("NullAccessToken", invalidResult(r -> r.setAccessToken(null))),
                Arguments.of("EmptyAccessToken", invalidResult(r -> r.setAccessToken(""))),
                Arguments.of("ZeroExpiry", invalidResult(r -> r.setExpiresInSeconds(0))),
                Arguments.of("NegativeExpiry", invalidResult(r -> r.setExpiresInSeconds(-1))),
                Arguments.of("NullTenantId", invalidResult(r -> r.setTenantId(null))),
                Arguments.of("EmptyTenantId", invalidResult(r -> r.setTenantId("")))
        );
    }

    // ========================================================================
    // refreshInSeconds auto-calculation
    // ========================================================================

    @Test
    void appTokenProvider_ExpiryAtLeastTwoHours_AutoCalculatesRefreshIn() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS);
        providerResult.setRefreshInSeconds(0);

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(
                providerReturning(providerResult));

        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build()).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
        assertTrue(result.metadata().refreshOn() > 0,
                "refreshOn should be auto-calculated when expiresIn >= 2 hours");
    }

    @Test
    void appTokenProvider_ExpiryLessThanTwoHours_NoAutoRefreshIn() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS - 1);
        providerResult.setRefreshInSeconds(0);

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(
                providerReturning(providerResult));

        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build()).get();

        assertEquals(VALID_ACCESS_TOKEN, result.accessToken());
        assertEquals(0, result.metadata().refreshOn(),
                "refreshOn should not be auto-calculated when expiresIn < 2 hours");
    }

    @Test
    void appTokenProvider_ExplicitRefreshIn_NotOverridden() throws Exception {
        TokenProviderResult providerResult = validTokenProviderResult();
        providerResult.setExpiresInSeconds(TWO_HOURS_SECONDS);
        providerResult.setRefreshInSeconds(1800); // explicit 30-minute refresh

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(
                providerReturning(providerResult));

        IAuthenticationResult result = cca.acquireToken(
                ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build()).get();

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

        ConfidentialClientApplication cca = TestHelper.buildCcaWithAppTokenProvider(throwingProvider);

        ExecutionException ex = assertThrows(ExecutionException.class,
                () -> cca.acquireToken(
                        ClientCredentialParameters.builder(TestHelper.TEST_SCOPE_SET).build()).get());
        assertInstanceOf(MsalAzureSDKException.class, ex.getCause());
    }
}
