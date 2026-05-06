// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import com.microsoft.aad.msal4j.labapi.KeyVaultSecretsProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Integration tests for agentic (agent identity) scenarios using MSAL Java APIs.
 * Tests the MSAL-level APIs for the agent identity flow
 * (specifically the FMI portions that are available on this branch).
 *
 * <p>These tests use MSAL token acquisition APIs (unlike AgenticRawHttpIT which uses raw HTTP).
 *
 * <p>Test configuration:
 * <ul>
 *   <li>Blueprint app: {@link #BLUEPRINT_CLIENT_ID}</li>
 *   <li>Agent app: {@link #AGENT_APP_ID}</li>
 *   <li>Tenant: {@link #TENANT_ID}</li>
 * </ul>
 *
 * <p>Flows tested (FMI-only, no FIC/user_fic on this branch):
 * <ul>
 *   <li>Agent gets app token using FMI-sourced assertion (Leg 2 of agent identity)</li>
 *   <li>Assertion callback receives correct context (AssertionRequestOptions)</li>
 *   <li>Cache isolation between different assertion-based flows</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgenticIT {

    // Lab test configuration
    private static final String BLUEPRINT_CLIENT_ID = "aab5089d-e764-47e3-9f28-cc11c2513821";
    private static final String RMA_CLIENT_ID = "3bf56293-fbb5-42bd-a407-248ba7431a8c";
    private static final String TENANT_ID = "10c419d4-4a50-45b2-aa4e-919fb84df24f";
    private static final String AGENT_APP_ID = "ab18ca07-d139-4840-8b3b-4be9610c6ed5";
    private static final String TOKEN_EXCHANGE_SCOPE = "api://AzureADTokenExchange/.default";
    private static final String FMI_EXCHANGE_SCOPE = "api://AzureFMITokenExchange/.default";
    private static final String GRAPH_SCOPE = "https://graph.microsoft.com/.default";
    private static final String AZURE_REGION = "westus3";

    private static final String AUTHORITY = "https://login.microsoftonline.com/" + TENANT_ID + "/";

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
     * Agent gets an app-only token for Graph using an FMI-sourced client assertion.
     * This tests Leg 2 of the agent identity flow:
     * 1. Blueprint CCA acquires FMI credential (fmi_path = agentAppId)
     * 2. Agent CCA uses that credential as client_assertion to get Graph token
     */
    @Test
    void agentGetsAppToken_UsingFmiAssertion() throws Exception {
        // The assertion callback simulates what an SDK or middleware would do:
        // it calls the blueprint app to get an FMI credential for the agent
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialForAgent(AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();

        IAuthenticationResult result = agentCca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(GRAPH_SCOPE))
                        .build())
                .get();

        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
    }

    /**
     * Verifies that the context-aware assertion callback receives the correct fmiPath
     * when the ClientCredentialParameters include an fmiPath.
     *
     * This tests the assertion context propagation: when acquiring an FMI credential
     * using a context-aware callback, the fmiPath from the parameters flows to the callback.
     */
    @Test
    void assertionCallback_ReceivesFmiPathContext() throws Exception {
        AtomicReference<AssertionRequestOptions> capturedOptions = new AtomicReference<>();

        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            capturedOptions.set(options);
            try {
                return acquireFmiCredentialFromRma();
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                        "urn:microsoft:identity:fmi", credential)
                .authority(AUTHORITY)
                .azureRegion(AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(FMI_EXCHANGE_SCOPE))
                .fmiPath(AGENT_APP_ID)
                .skipCache(true)
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify assertion callback received the correct context
        assertNotNull(capturedOptions.get(), "AssertionRequestOptions should have been passed to callback");
        assertEquals(AGENT_APP_ID, capturedOptions.get().fmiPath(),
                "fmiPath in callback should match the one set in parameters");
        assertEquals("urn:microsoft:identity:fmi", capturedOptions.get().clientId(),
                "clientId in callback should match the CCA client ID");
        assertNotNull(capturedOptions.get().tokenEndpoint(),
                "tokenEndpoint should be available in callback");

        // Verify token was acquired
        assertNotNull(result.accessToken(), "Access token should not be null");
    }

    /**
     * Verifies that the agent CCA can acquire a token and it gets cached,
     * then the second request is a cache hit.
     */
    @Test
    void agentAppToken_CacheHit() throws Exception {
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialForAgent(AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE))
                .build();

        IAuthenticationResult result1 = agentCca.acquireToken(params).get();
        IAuthenticationResult result2 = agentCca.acquireToken(params).get();

        // Second call should be a cache hit
        assertEquals(result1.accessToken(), result2.accessToken(),
                "Second request should be a cache hit returning the same token");
        assertEquals(1, agentCca.tokenCache.accessTokens.size(),
                "Should have only one cache entry");
    }

    /**
     * Verifies that tokens acquired with different fmi_paths are isolated in cache
     * even when using the same agent CCA.
     */
    @Test
    void agentFmiToken_CacheIsolation_DifferentFmiPaths() throws Exception {
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialFromRma();
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication cca = ConfidentialClientApplication.builder(
                        "urn:microsoft:identity:fmi", credential)
                .authority(AUTHORITY)
                .azureRegion(AZURE_REGION)
                .build();

        // Acquire with first fmi_path
        ClientCredentialParameters params1 = ClientCredentialParameters
                .builder(Collections.singleton(FMI_EXCHANGE_SCOPE))
                .fmiPath(AGENT_APP_ID)
                .build();
        IAuthenticationResult result1 = cca.acquireToken(params1).get();

        // Acquire with different fmi_path
        ClientCredentialParameters params2 = ClientCredentialParameters
                .builder(Collections.singleton(FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/DifferentAgent")
                .build();
        IAuthenticationResult result2 = cca.acquireToken(params2).get();

        // Should have separate cache entries
        assertEquals(2, cca.tokenCache.accessTokens.size(),
                "Different fmi_paths should produce separate cache entries");
        assertNotEquals(result1.accessToken(), result2.accessToken(),
                "Tokens for different fmi_paths should be different");
    }

    /**
     * Helper: acquires an FMI credential from the blueprint app for the given agent app ID.
     * Uses the agent token exchange scope (api://AzureADTokenExchange).
     */
    private String acquireFmiCredentialForAgent(String agentAppId) throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TOKEN_EXCHANGE_SCOPE))
                .fmiPath(agentAppId)
                .build();

        IAuthenticationResult result = blueprintCca.acquireToken(params).get();
        return result.accessToken();
    }

    /**
     * Helper: acquires an FMI credential from the RMA using a certificate.
     * Uses the FMI-specific exchange scope (api://AzureFMITokenExchange).
     */
    private String acquireFmiCredentialFromRma() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication rmaCca = ConfidentialClientApplication.builder(
                        RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result = rmaCca.acquireToken(params).get();
        return result.accessToken();
    }
}
