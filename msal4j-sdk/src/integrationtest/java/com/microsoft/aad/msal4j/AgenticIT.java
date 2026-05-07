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
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Integration tests for agentic (agent identity) scenarios using MSAL Java APIs.
 * Corresponds to .NET's Agentic.cs — tests the MSAL-level APIs for the agent identity flow
 * (specifically the FMI portions that are available on this branch, plus FIC user_fic flows).
 *
 * <p>These tests use MSAL token acquisition APIs (unlike AgenticRawHttpIT which uses raw HTTP).
 *
 * <p>Test configuration:
 * <ul>
 *   <li>RMA app: see {@link TestConstants#AGENTIC_RMA_CLIENT_ID}</li>
 *   <li>Agent app: see {@link TestConstants#AGENTIC_AGENT_APP_ID}</li>
 *   <li>Tenant: see {@link TestConstants#AGENTIC_TENANT_ID}</li>
 * </ul>
 *
 * <p>Flows tested:
 * <ul>
 *   <li>Assertion callback receives correct context (AssertionRequestOptions)</li>
 *   <li>Cache isolation between different fmi_path values</li>
 *   <li>Full 3-leg flow: FMI → assertion → user_fic → user token</li>
 *   <li>Multi-user cache isolation via user_fic</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AgenticIT {

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
     * Agent gets an app-only token for Graph using an FMI-sourced client assertion.
     * This tests Leg 2 of the agent identity flow:
     * 1. Blueprint CCA acquires FMI credential (fmi_path = agentAppId)
     * 2. Agent CCA uses that credential as client_assertion to get Graph token
     *
     * Corresponds to .NET's AgentGetsAppTokenForGraphTest.
     */
    @Test
    void agentGetsAppToken_UsingFmiAssertion() throws Exception {
        // The assertion callback simulates what an SDK or middleware would do:
        // it calls the blueprint app to get an FMI credential for the agent
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialForAgent(TestConstants.AGENTIC_AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();

        IAuthenticationResult result = agentCca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE))
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
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath(TestConstants.AGENTIC_AGENT_APP_ID)
                .skipCache(true)
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Verify assertion callback received the correct context
        assertNotNull(capturedOptions.get(), "AssertionRequestOptions should have been passed to callback");
        assertEquals(TestConstants.AGENTIC_AGENT_APP_ID, capturedOptions.get().clientAssertionFmiPath(),
                "clientAssertionFmiPath in callback should match the one set in parameters");
        assertEquals("urn:microsoft:identity:fmi", capturedOptions.get().clientId(),
                "clientId in callback should match the CCA client ID");
        assertNotNull(capturedOptions.get().tokenEndpoint(),
                "tokenEndpoint should be available in callback");

        // Verify token was acquired
        assertNotNull(result.accessToken(), "Access token should not be null");
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
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        // Acquire with first fmi_path
        ClientCredentialParameters params1 = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath(TestConstants.AGENTIC_AGENT_APP_ID)
                .build();
        IAuthenticationResult result1 = cca.acquireToken(params1).get();

        // Acquire with different fmi_path
        ClientCredentialParameters params2 = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
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
     * Full 3-leg agent identity flow: FMI → assertion → user_fic → user-scoped Graph token.
     * Uses the assertion callback pattern where the blueprint CCA acquires the FMI credential
     * and the agent CCA exchanges it for a user token.
     */
    @Test
    void agentUserIdentity_GetsTokenForGraph() throws Exception {
        // Build agent CCA with assertion callback that acquires FMI credential
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialForAgent(TestConstants.AGENTIC_AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();

        // Get instance token (T2) for user_fic exchange
        String t2 = acquireInstanceTokenForAgent();

        // Exchange T2 for user-scoped token via user_fic grant
        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE), TestConstants.AGENTIC_USER_UPN, t2)
                .build();

        IAuthenticationResult result = agentCca.acquireToken(params).get();

        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
        assertNotNull(result.account(), "Account should not be null (user token)");

        // Verify token is cached and silent retrieval works
        Set<IAccount> accounts = agentCca.getAccounts().get();
        assertFalse(accounts.isEmpty(), "Accounts should be in cache");

        IAccount account = accounts.iterator().next();
        IAuthenticationResult silentResult = agentCca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE), account).build()).get();

        assertEquals(result.accessToken(), silentResult.accessToken(),
                "Silent call should return cached token");
    }

    /**
     * Verifies that user_fic tokens and app-only tokens are isolated in cache
     * on the same agent CCA instance. App token acquisition should not interfere
     * with user token acquisition.
     */
    @Test
    void agentCca_AppAndUserTokens_CacheIsolation() throws Exception {
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredentialForAgent(TestConstants.AGENTIC_AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();

        // Acquire app-only token
        IAuthenticationResult appResult = agentCca.acquireToken(ClientCredentialParameters
                        .builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE))
                        .build())
                .get();
        assertNotNull(appResult.accessToken());

        // Acquire user token via user_fic (needs T2 = instance token)
        String t2 = acquireInstanceTokenForAgent();
        UserFederatedIdentityCredentialParameters userParams = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_GRAPH_SCOPE), TestConstants.AGENTIC_USER_UPN, t2)
                .build();

        IAuthenticationResult userResult = agentCca.acquireToken(userParams).get();
        assertNotNull(userResult.accessToken());
        assertNotNull(userResult.account(), "User token should have an account");

        // Tokens should be different (app vs user scoped)
        assertNotEquals(appResult.accessToken(), userResult.accessToken(),
                "App token and user token should be different");

        // App cache should have 1 entry, user cache should have user account
        assertEquals(2, agentCca.tokenCache.accessTokens.size(),
                "Cache should have exactly 2 entries (app + user)");
    }

    // ========================================================================
    // High-level AcquireTokenForAgent tests (composite API)
    // ========================================================================

    /**
     * Tests the high-level acquireTokenForAgent API with a UPN-based AgentIdentity.
     * Exercises the full 3-leg flow orchestrated internally by AcquireTokenForAgentSupplier.
     */
    @Test
    void acquireTokenForAgent_withUpn_fullFlow() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        AgentIdentity agentId = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);

        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();

        assertNotNull(result, "Result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
        assertNotNull(result.account(), "Account should not be null for user token");
    }

    /**
     * Tests the high-level acquireTokenForAgent API for app-only (no user) scenarios.
     * Only Legs 1-2 are performed.
     */
    @Test
    void acquireTokenForAgent_appOnly() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        AgentIdentity agentId = AgentIdentity.appOnly(AGENT_APP_ID);

        IAuthenticationResult result = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();

        assertNotNull(result, "Result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
    }

    /**
     * Tests the high-level acquireTokenForAgent API with ForceRefresh.
     * First call populates cache, second call (forceRefresh) bypasses it.
     */
    @Test
    void acquireTokenForAgent_forceRefresh() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        AgentIdentity agentId = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);

        // First call — populates cache
        IAuthenticationResult result1 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();
        assertNotNull(result1.accessToken());

        // Second call without forceRefresh — should return cached token
        IAuthenticationResult result2 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();
        assertEquals(result1.accessToken(), result2.accessToken(),
                "Second call should return cached token");

        // Third call with forceRefresh — should get a fresh token
        IAuthenticationResult result3 = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId)
                        .forceRefresh(true).build()
        ).get();
        assertNotNull(result3.accessToken());
        // The fresh token may be the same string (if not expired) but the flow exercised network
    }

    /**
     * Tests cache isolation between two blueprint CCA instances.
     * Each blueprint should have its own agent CCA cache.
     */
    @Test
    void acquireTokenForAgent_cacheIsolation_twoBlueprintCcas() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprint1 = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        ConfidentialClientApplication blueprint2 = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        AgentIdentity agentId = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);

        // Acquire via blueprint1
        IAuthenticationResult result1 = blueprint1.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();
        assertNotNull(result1.accessToken());

        // Blueprint1 should have agent CCA cached, blueprint2 should not
        assertEquals(1, blueprint1.agentCcaCache.size(),
                "Blueprint1 should have one cached agent CCA");
        assertTrue(blueprint2.agentCcaCache.isEmpty(),
                "Blueprint2 should have no cached agent CCAs (no bleed)");

        // Acquire via blueprint2
        IAuthenticationResult result2 = blueprint2.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), agentId).build()
        ).get();
        assertNotNull(result2.accessToken());

        // Both should now have their own cache entries
        assertEquals(1, blueprint1.agentCcaCache.size());
        assertEquals(1, blueprint2.agentCcaCache.size());
    }

    /**
     * Tests that a UPN-based token can be found by OID lookup on the same blueprint.
     * Discovers the OID via the UPN flow, then verifies OID-based call returns cached token.
     */
    @Test
    void acquireTokenForAgent_upnThenOid_sharesCache() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        // Step 1: Acquire via UPN
        AgentIdentity upnIdentity = AgentIdentity.withUsername(AGENT_APP_ID, USER_UPN);
        IAuthenticationResult upnResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), upnIdentity).build()
        ).get();
        assertNotNull(upnResult.account(), "Account should not be null");

        // Extract OID from account's homeAccountId (format: oid.tid)
        String homeAccountId = upnResult.account().homeAccountId();
        assertNotNull(homeAccountId);
        String oidString = homeAccountId.contains(".")
                ? homeAccountId.substring(0, homeAccountId.indexOf('.'))
                : homeAccountId;
        java.util.UUID userOid = java.util.UUID.fromString(oidString);

        // Step 2: Acquire via OID — should come from cache
        AgentIdentity oidIdentity = new AgentIdentity(AGENT_APP_ID, userOid);
        IAuthenticationResult oidResult = blueprintCca.acquireTokenForAgent(
                AcquireTokenForAgentParameters.builder(
                        Collections.singleton(GRAPH_SCOPE), oidIdentity).build()
        ).get();

        // Should return the same cached token
        assertEquals(upnResult.accessToken(), oidResult.accessToken(),
                "OID-based call should return the same cached token as UPN-based call");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Helper: acquires an FMI credential from the RMA (Resource Management Application).
     * Uses TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE, matching FmiIT's Flow3 pattern.
     * Suitable for use as client_assertion when client_id = "urn:microsoft:identity:fmi".
     */
    private String acquireFmiCredentialFromRma() throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication rmaCca = ConfidentialClientApplication.builder(
                        TestConstants.AGENTIC_RMA_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_FMI_EXCHANGE_SCOPE))
                .fmiPath("SomeFmiPath/FmiCredentialPath")
                .build();

        IAuthenticationResult result = rmaCca.acquireToken(params).get();
        return result.accessToken();
    }

    /**
     * Helper: acquires an FMI credential from the blueprint app for the given agent app ID.
     * This is Leg 1 of the agent identity flow — returns T1.
     */
    private String acquireFmiCredentialForAgent(String agentAppId) throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        TestConstants.AGENTIC_BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(TestConstants.AGENTIC_AZURE_REGION)
                .build();

        ClientCredentialParameters params = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_TOKEN_EXCHANGE_SCOPE))
                .fmiPath(agentAppId)
                .build();

        IAuthenticationResult result = blueprintCca.acquireToken(params).get();
        return result.accessToken();
    }

    /**
     * Helper: acquires an instance token (T2) for the agent app via the full 2-leg flow.
     * Leg 1: Blueprint → T1 (FMI credential)
     * Leg 2: Agent uses T1 as client_assertion → T2 (instance token)
     * T2 is used as the user_federated_identity_credential in Leg 3 (user_fic exchange).
     */
    private String acquireInstanceTokenForAgent() throws Exception {
        String t1 = acquireFmiCredentialForAgent(TestConstants.AGENTIC_AGENT_APP_ID);

        IClientCredential agentCredential = ClientCredentialFactory.createFromClientAssertion(t1);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(TestConstants.AGENTIC_AGENT_APP_ID, agentCredential)
                .authority(AUTHORITY)
                .build();

        ClientCredentialParameters instanceParams = ClientCredentialParameters
                .builder(Collections.singleton(TestConstants.AGENTIC_TOKEN_EXCHANGE_SCOPE))
                .skipCache(true)
                .build();

        IAuthenticationResult instanceResult = agentCca.acquireToken(instanceParams).get();
        return instanceResult.accessToken();
    }
}
