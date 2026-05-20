// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.TreeMap;
import java.util.function.Function;

import com.microsoft.aad.msal4j.labapi.KeyVaultSecretsProvider;

/**
 * Integration tests for FMI (Federated Managed Identity) support.
 * Validates real Entra ID interactions for the FMI token acquisition flows.
 *
 * <p>Test apps are in MSID Lab 4:
 * <ul>
 *   <li>RMA (Resource Management Application): see {@link TestConstants#AGENTIC_RMA_CLIENT_ID}</li>
 *   <li>Web API resource: see {@link TestConstants#AGENTIC_WEB_API_SCOPE}</li>
 * </ul>
 *
 * <p>Flows tested:
 * <ul>
 *   <li>Flow 1: RMA gets FMI credential from cert (scope: api://AzureFMITokenExchange/.default)</li>
 *   <li>Flow 2: RMA gets FMI token for a resource (scope: webapi/.default)</li>
 *   <li>Flow 3: Sub-RMA gets FMI credential using assertion callback</li>
 *   <li>Flow 5: Sub-RMA gets FMI token for a resource using assertion callback</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FmiIT {

    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TestConstants.AGENTIC_TENANT_ID + "/";

    private PrivateKey privateKey;
    private X509Certificate certificate;

    @BeforeAll
    void init() throws KeyStoreException, NoSuchProviderException,
            IOException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException {
        KeyStore keystore = CertificateHelper.createKeyStore();
        keystore.load(null, null);

        privateKey = (PrivateKey) keystore.getKey(KeyVaultSecretsProvider.CERTIFICATE_ALIAS, null);
        certificate = (X509Certificate) keystore.getCertificate(KeyVaultSecretsProvider.CERTIFICATE_ALIAS);

        assertNotNull(privateKey, "Lab private key not found. Ensure the lab cert is installed.");
        assertNotNull(certificate, "Lab certificate not found. Ensure the lab cert is installed.");
    }

    /**
     * Flow 1: RMA getting FMI credential for a leaf entity or sub-RMA.
     * Uses certificate with SN+I (sendX5c=true) and fmi_path to acquire a credential
     * scoped to api://AzureFMITokenExchange/.default.
     */
    @Test
    void flow1_FmiCredential_FromCert() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify token
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");

        // Verify cache uses "atext" credential type with fmi_path hash
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();
        assertTrue(cacheKey.contains("-atext-"),
                "Cache key should use 'atext' credential type for FMI tokens, got: " + cacheKey);

        // Verify hash for "SomeFmiPath/FmiCredentialPath" matches expected value
        String expectedHash = "zm2n0E62zwTsnNsozptLsoOoB_C7i-GfpxHYQQINJUw".toLowerCase();
        assertTrue(cacheKey.endsWith(expectedHash),
                "Cache key should end with the expected fmi_path hash, got: " + cacheKey);
    }

    /**
     * Flow 2: RMA getting FMI token for a leaf entity (resource scope, not exchange scope).
     * Validates that fmi_path works with any resource scope, not just the exchange scope.
     */
    @Test
    void flow2_FmiToken_FromCert() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_WEB_API_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify token
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");

        // Verify cache isolation
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();
        assertTrue(cacheKey.contains("-atext-"),
                "Cache key should use 'atext' credential type");
    }

    /**
     * Flow 3: Sub-RMA getting FMI credential from another FMI credential (assertion callback).
     * Uses context-aware assertion callback where fmi_path is available in AssertionRequestOptions.
     */
    @Test
    void flow3_FmiCredential_FromAnotherFmiCredential() throws Exception {
        // Context-aware assertion callback that acquires an FMI credential from the RMA
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                assertNotNull(options.clientAssertionFmiPath(), "clientAssertionFmiPath should be available in assertion context");
                return acquireFmiCredentialFromRma();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get FMI credential from RMA", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                        "urn:microsoft:identity:fmi", credential)
                .authority(AUTHORITY)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/Path")
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify token
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");

        // Verify cache key uses expected hash for "SomeFmiPath/Path"
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();
        assertTrue(cacheKey.contains("-atext-"),
                "Cache key should use 'atext' credential type");
        String expectedHash = "7CX57Q63os7benQ6ER0sxgJPtNQSv7TGb5zexcidFoI".toLowerCase();
        assertTrue(cacheKey.endsWith(expectedHash),
                "Cache key should end with expected fmi_path hash for 'SomeFmiPath/Path', got: " + cacheKey);
    }

    /**
     * Flow 5: Sub-RMA getting FMI token for leaf entity using assertion callback.
     * Uses a resource scope (WebAPI) rather than exchange scope.
     */
    @Test
    void flow5_FmiToken_FromFmiCredential() throws Exception {
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialFromRma();
            } catch (Exception e) {
                throw new RuntimeException("Failed to get FMI credential from RMA", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                        "urn:microsoft:identity:fmi", credential)
                .authority(AUTHORITY)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_WEB_API_SCOPE))
                .fmiPath("SomeFmiPath/Path")
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify token
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");

        // Verify cache isolation
        assertEquals(1, cca.tokenCache.accessTokens.size());
        String cacheKey = cca.tokenCache.accessTokens.keySet().iterator().next();
        assertTrue(cacheKey.contains("-atext-"),
                "Cache key should use 'atext' credential type");
    }

    /**
     * Validates that cache correctly isolates tokens for different fmi_paths.
     * Acquires tokens with two different fmi_path values and verifies cache isolation.
     */
    @Test
    void fmiPath_CacheIsolation_Integration() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        // Acquire with first fmi_path
        ClientCredentialParameters params1 = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();
        IAuthenticationResult result1 = cca.acquireToken(params1).get();

        // Acquire with different fmi_path (same scope)
        ClientCredentialParameters params2 = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/Path")
                .build();
        IAuthenticationResult result2 = cca.acquireToken(params2).get();

        // Should have 2 separate cache entries
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "Different fmi_paths should produce separate cache entries");
        assertNotEquals(result1.accessToken(), result2.accessToken(),
                "Tokens for different fmi_paths should be different");
    }

    /**
     * Validates that the same fmi_path results in a cache hit on second request.
     */
    @Test
    void fmiPath_CacheHit_Integration() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result1 = cca.acquireToken(params).get();
        IAuthenticationResult result2 = cca.acquireToken(params).get();

        // Second call should be a cache hit (same token)
        assertEquals(result1.accessToken(), result2.accessToken(),
                "Same fmi_path should produce a cache hit");
        assertEquals(1, cca.tokenCache.accessTokens.size(),
                "Should have only one cache entry");
    }

    /**
     * Helper: acquires an FMI credential from the RMA using certificate + SN+I.
     * This is the Leg 1 operation that produces a token usable as client_assertion.
     */
    private String acquireFmiCredentialFromRma() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication rma = ConfidentialClientApplication.builder(TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result = rma.acquireToken(params).get();
        return result.accessToken();
    }
}
