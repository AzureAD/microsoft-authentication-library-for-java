// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the MSI v2 mTLS PoP flow, covering:
 * - Parameter validation (attestation requires mTLS PoP)
 * - Gating logic (both flags required for MSI v2)
 * - No silent fallback to MSI v1 on MSI v2 failure
 * - URL construction utilities
 */
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MsiV2Tests {

    private static final String RESOURCE = ManagedIdentityTestConstants.RESOURCE;

    @BeforeAll
    void setupRetryPolicies() {
        // Set retry delays to 1ms for faster test execution
        ManagedIdentityRetryPolicy.setRetryDelayMs(1);
        IMDSRetryPolicy.setRetryDelayMs(1);
    }

    @AfterAll
    void resetRetryPolicies() {
        ManagedIdentityRetryPolicy.resetToDefaults();
        IMDSRetryPolicy.resetToDefaults();
    }

    /**
     * An executor that runs tasks synchronously on the calling thread.
     * This ensures MockedStatic is active when the code executes.
     */
    private static final ExecutorService CURRENT_THREAD_EXECUTOR = new AbstractExecutorService() {
        @Override public void execute(Runnable command) { command.run(); }
        @Override public void shutdown() {}
        @Override public List<Runnable> shutdownNow() { return Collections.emptyList(); }
        @Override public boolean isShutdown() { return false; }
        @Override public boolean isTerminated() { return false; }
        @Override public boolean awaitTermination(long timeout, TimeUnit unit) { return true; }
    };

    // -------------------------------------------------------------------------
    // 1. Parameter validation tests
    // -------------------------------------------------------------------------

    @Nested
    class ParameterValidationTests {

        private ManagedIdentityApplication miApp;
        private DefaultHttpClient httpClientMock;

        @BeforeEach
        void setUp() throws Exception {
            ManagedIdentityApplication.setEnvironmentVariables(
                    new EnvironmentVariablesHelper(ManagedIdentitySourceType.DEFAULT_TO_IMDS, null));
            // Mock HTTP client returns a generic 500 error to avoid real network calls
            httpClientMock = mock(DefaultHttpClient.class);
            HttpResponse errorResponse = new HttpResponse();
            errorResponse.statusCode(500);
            errorResponse.body(ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500);
            org.mockito.Mockito.lenient().when(httpClientMock.send(any())).thenReturn(errorResponse);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .executorService(CURRENT_THREAD_EXECUTOR)
                    .build();
            miApp.tokenCache().accessTokens.clear();
        }

        @Test
        void attestationWithoutPop_throwsMsalClientException() throws Exception {
            // withAttestationSupport=true requires mtlsProofOfPossession=true
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .withAttestationSupport(true)
                    .mtlsProofOfPossession(false)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalClientException.class, ex.getCause());
            MsalClientException msalEx = (MsalClientException) ex.getCause();
            assertEquals(MsalError.MSI_V2_ATTESTATION_REQUIRES_POP, msalEx.errorCode());
            assertTrue(msalEx.getMessage().contains("withAttestationSupport"));
        }

        @Test
        void attestationWithoutPopExplicitFalse_throwsMsalClientException() throws Exception {
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .withAttestationSupport(true)
                    // mtlsProofOfPossession defaults to false
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertInstanceOf(MsalClientException.class, ex.getCause());
            assertEquals(MsalError.MSI_V2_ATTESTATION_REQUIRES_POP,
                    ((MsalClientException) ex.getCause()).errorCode());
        }

        @Test
        void popOnlyWithoutAttestation_doesNotThrowAttestationError() throws Exception {
            // mtlsProofOfPossession=true alone (without attestation) should fall through to MSI v1.
            // It should fail because IMDS is not available in tests, but NOT with MSI v2 attestation error.
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(false)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            // Must NOT be an attestation_requires_pop error
            assertFalse(ex.getCause() instanceof MsalClientException
                    && MsalError.MSI_V2_ATTESTATION_REQUIRES_POP.equals(
                            ((MsalClientException) ex.getCause()).errorCode()),
                    "PoP-only flag should NOT trigger attestation-requires-PoP validation error");
            // Must NOT be a MsiV2Exception (MSI v2 should not be called without attestation)
            assertFalse(ex.getCause() instanceof MsiV2Exception,
                    "PoP-only should not use the MSI v2 flow (no MsiV2Exception expected)");
        }

        @Test
        void neitherFlagSet_doesNotThrowMsiV2Exception() throws Exception {
            // No flags set - standard MSI v1 flow (no MSI v2)
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            // Standard MSI v1 error (network or IMDS not available), not a MsiV2Exception
            assertFalse(ex.getCause() instanceof MsiV2Exception,
                    "Standard MSI v1 path should not throw MsiV2Exception");
        }
    }

    // -------------------------------------------------------------------------
    // 2. MSI v2 gating tests (both flags set)
    // -------------------------------------------------------------------------

    @Nested
    class MsiV2GatingTests {

        private ManagedIdentityApplication miApp;
        private DefaultHttpClient httpClientMock;

        @BeforeEach
        void setUp() throws Exception {
            ManagedIdentityApplication.setEnvironmentVariables(
                    new EnvironmentVariablesHelper(ManagedIdentitySourceType.DEFAULT_TO_IMDS, null));
            // Mock HTTP client returns a generic 500 error for MSI v1 fallback tests
            httpClientMock = mock(DefaultHttpClient.class);
            HttpResponse errorResponse = new HttpResponse();
            errorResponse.statusCode(500);
            errorResponse.body(ManagedIdentityTestConstants.MSI_ERROR_RESPONSE_500);
            org.mockito.Mockito.lenient().when(httpClientMock.send(any())).thenReturn(errorResponse);

            miApp = ManagedIdentityApplication
                    .builder(ManagedIdentityId.systemAssigned())
                    .httpClient(httpClientMock)
                    .executorService(CURRENT_THREAD_EXECUTOR)
                    .build();
            miApp.tokenCache().accessTokens.clear();
        }

        @Test
        void bothFlagsSet_triggersMsiV2Path() throws Exception {
            // When both flags are set, the MSI v2 path is taken.
            // Since the native KeyGuard library is unavailable in the test environment,
            // MsiV2.obtainToken() will throw MsiV2Exception with KEYGUARD_UNAVAILABLE.
            // This proves the MSI v2 code path was entered.
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(true)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            // The MSI v2 path was entered - confirmed by MsiV2Exception with KEYGUARD_UNAVAILABLE
            assertInstanceOf(MsiV2Exception.class, ex.getCause(),
                    "Both flags set should enter MSI v2 path (evidenced by MsiV2Exception for unavailable KeyGuard)");
            assertEquals(MsalError.MSI_V2_KEYGUARD_UNAVAILABLE,
                    ((MsiV2Exception) ex.getCause()).errorCode());
        }

        @Test
        void bothFlagsSet_msiV2Exception_propagatesWithoutFallback() throws Exception {
            // Verify that a MsiV2Exception propagates without silently falling back to MSI v1.
            // Since both flags are set and native library is unavailable, we expect MsiV2Exception.
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(true)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            // Must propagate as MsiV2Exception (no fallback to MSI v1 which would produce MsalServiceException)
            assertInstanceOf(MsiV2Exception.class, ex.getCause(),
                    "MsiV2Exception must propagate without silent fallback to MSI v1");
        }

        @Test
        void onlyPopWithoutAttestation_doesNotTriggerMsiV2Path() throws Exception {
            // With PoP only (no attestation), MSI v1 is used. The error should NOT be MsiV2Exception.
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(false)
                    .build();

            CompletableFuture<IAuthenticationResult> future =
                    miApp.acquireTokenForManagedIdentity(params);

            ExecutionException ex = assertThrows(ExecutionException.class, future::get);
            assertFalse(ex.getCause() instanceof MsiV2Exception,
                    "PoP-only should use MSI v1 path (no MsiV2Exception)");
        }
    }

    // -------------------------------------------------------------------------
    // 3. ManagedIdentityParameters builder tests
    // -------------------------------------------------------------------------

    @Nested
    class ManagedIdentityParametersBuilderTests {

        @Test
        void defaultValues_bothFlagsFalse() {
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE).build();
            assertFalse(params.mtlsProofOfPossession());
            assertFalse(params.withAttestationSupport());
        }

        @Test
        void mtlsPopOnlyFlag_setCorrectly() {
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .build();
            assertTrue(params.mtlsProofOfPossession());
            assertFalse(params.withAttestationSupport());
        }

        @Test
        void bothFlags_setCorrectly() {
            ManagedIdentityParameters params = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(true)
                    .build();
            assertTrue(params.mtlsProofOfPossession());
            assertTrue(params.withAttestationSupport());
        }

        @Test
        void builderToString_includesNewFields() {
            String str = ManagedIdentityParameters.builder(RESOURCE)
                    .mtlsProofOfPossession(true)
                    .withAttestationSupport(true)
                    .toString();
            assertTrue(str.contains("mtlsProofOfPossession=true"));
            assertTrue(str.contains("withAttestationSupport=true"));
        }
    }

    // -------------------------------------------------------------------------
    // 4. MsiV2 utility method tests
    // -------------------------------------------------------------------------

    @Nested
    class MsiV2UtilityTests {

        @Test
        void buildTokenEndpointUrl_noTrailingSlash() {
            String result = MsiV2.buildTokenEndpointUrl(
                    "https://eastus.mtlsauth.microsoft.com", "tenant-id");
            assertEquals("https://eastus.mtlsauth.microsoft.com/tenant-id/oauth2/v2.0/token",
                    result);
        }

        @Test
        void buildTokenEndpointUrl_withTrailingSlash() {
            String result = MsiV2.buildTokenEndpointUrl(
                    "https://eastus.mtlsauth.microsoft.com/", "tenant-id");
            assertEquals("https://eastus.mtlsauth.microsoft.com/tenant-id/oauth2/v2.0/token",
                    result);
        }

        @Test
        void buildTokenEndpointUrl_differentTenantId() {
            String result = MsiV2.buildTokenEndpointUrl(
                    "https://westus2.mtlsauth.microsoft.com", "my-tenant-123");
            assertEquals("https://westus2.mtlsauth.microsoft.com/my-tenant-123/oauth2/v2.0/token",
                    result);
        }
    }

    // -------------------------------------------------------------------------
    // 5. MsiV2Exception tests
    // -------------------------------------------------------------------------

    @Nested
    class MsiV2ExceptionTests {

        @Test
        void msiV2Exception_hasCorrectMessageAndErrorCode() {
            MsiV2Exception ex = new MsiV2Exception("test message", MsalError.MSI_V2_ERROR);
            assertEquals("test message", ex.getMessage());
            assertEquals(MsalError.MSI_V2_ERROR, ex.errorCode());
        }

        @Test
        void msiV2Exception_withCause_hasCause() {
            RuntimeException cause = new RuntimeException("root cause");
            MsiV2Exception ex = new MsiV2Exception("msg", MsalError.MSI_V2_ERROR, cause);
            assertEquals(cause, ex.getCause());
        }

        @Test
        void msiV2Exception_isInstanceOfMsalException() {
            MsiV2Exception ex = new MsiV2Exception("msg", MsalError.MSI_V2_ERROR);
            assertInstanceOf(MsalException.class, ex);
        }
    }

    // -------------------------------------------------------------------------
    // 6. WindowsKeyGuardJNI availability test
    // -------------------------------------------------------------------------

    @Nested
    class WindowsKeyGuardJNITests {

        @Test
        void nativeLibraryNotLoaded_isNativeLibraryLoadedReturnsFalse() {
            // On non-Windows or systems without the native library, it should be false
            boolean result = WindowsKeyGuardJNI.isNativeLibraryLoaded();
            assertFalse(result, "Native library should not be loaded in test environment");
        }
    }
}
