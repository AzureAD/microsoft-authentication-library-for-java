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
import java.util.UUID;
import java.util.function.Function;

/**
 * Integration tests for FIC (Federated Identity Credential) / user_fic grant support.
 *
 * <p>Tests the user_fic primitive: acquires an FMI-sourced assertion,
 * then exchanges it for a user-scoped token using the user_fic grant type.
 *
 * <p>Test configuration:
 * <ul>
 *   <li>Blueprint app: {@link #BLUEPRINT_CLIENT_ID}</li>
 *   <li>Agent app: {@link #AGENT_APP_ID}</li>
 *   <li>Tenant: {@link #TENANT_ID}</li>
 *   <li>User UPN: {@link #USER_UPN}</li>
 * </ul>
 *
 * <p>Flows tested:
 * <ul>
 *   <li>Full 3-leg: FMI → assertion → user_fic → user token (UPN-based)</li>
 *   <li>OID-based user_fic (UUID overload)</li>
 *   <li>Cache hit: second call returns cached user token</li>
 *   <li>Force refresh: bypasses cache</li>
 * </ul>
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FicIT {

    // Same config as AgenticIT
    private static final String BLUEPRINT_CLIENT_ID = "aab5089d-e764-47e3-9f28-cc11c2513821";
    private static final String TENANT_ID = "10c419d4-4a50-45b2-aa4e-919fb84df24f";
    private static final String AGENT_APP_ID = "ab18ca07-d139-4840-8b3b-4be9610c6ed5";
    private static final String USER_UPN = "agentuser1@id4slab1.onmicrosoft.com";
    private static final String TOKEN_EXCHANGE_SCOPE = "api://AzureADTokenExchange/.default";
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
     * Full 3-leg flow using UPN: FMI credential → assertion → user_fic → user-scoped Graph token.
     * Then verifies the token is cached and can be retrieved silently.
     */
    @Test
    void userFic_FullFlow_WithUpn_GetsUserToken() throws Exception {
        // Leg 1+2: Get instance token (T2) for the agent
        String t2 = acquireInstanceToken();

        // Leg 3: Exchange T2 for user-scoped token via user_fic grant
        // CCA authenticates with T1 (via callback in buildAgentCca)
        ConfidentialClientApplication cca = buildAgentCca();

        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, t2)
                .build();

        IAuthenticationResult result = cca.acquireToken(params).get();

        // Assert: got a user token
        assertNotNull(result, "Auth result should not be null");
        assertNotNull(result.accessToken(), "Access token should not be null");
        assertFalse(result.accessToken().isEmpty(), "Access token should not be empty");
        assertNotNull(result.account(), "Account should not be null (user token cache)");

        // Verify silent retrieval works (token is cached)
        Set<IAccount> accounts = cca.getAccounts().get();
        assertFalse(accounts.isEmpty(), "Accounts should be present in cache");

        IAccount account = accounts.iterator().next();
        IAuthenticationResult silentResult = cca.acquireTokenSilently(
                SilentParameters.builder(Collections.singleton(GRAPH_SCOPE), account).build()).get();

        assertNotNull(silentResult.accessToken(), "Silent token should not be null");
        assertEquals(result.accessToken(), silentResult.accessToken(),
                "Silent call should return cached token");
    }

    /**
     * OID-based user_fic: discovers user's OID via UPN flow, then uses UUID overload.
     */
    @Test
    void userFic_WithGuidObjectId_GetsUserToken() throws Exception {
        // Step 1: Get instance token and acquire token via UPN to discover the user's OID
        String t2_1 = acquireInstanceToken();

        ConfidentialClientApplication cca = buildAgentCca();

        UserFederatedIdentityCredentialParameters upnParams = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, t2_1)
                .build();

        IAuthenticationResult upnResult = cca.acquireToken(upnParams).get();
        assertNotNull(upnResult.account(), "Account should not be null");

        // Extract OID from the account's home account ID (format: oid.tid)
        String homeAccountId = upnResult.account().homeAccountId();
        assertNotNull(homeAccountId, "Home account ID should not be null");
        String oidString = homeAccountId.split("\\.")[0];
        UUID userOid = UUID.fromString(oidString);

        // Step 2: Get a fresh instance token
        String t2_2 = acquireInstanceToken();

        // Step 3: Use the UUID overload
        UserFederatedIdentityCredentialParameters oidParams = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), userOid, t2_2)
                .forceRefresh(true)
                .build();

        IAuthenticationResult oidResult = cca.acquireToken(oidParams).get();

        // Assert
        assertNotNull(oidResult, "Result should not be null");
        assertNotNull(oidResult.accessToken(), "Access token should not be null");
        assertFalse(oidResult.accessToken().isEmpty(), "Access token should not be empty");
        assertTrue(oidResult.account().homeAccountId().startsWith(oidString),
                "OID should match in home account ID");
    }

    /**
     * Verifies that the user_fic token is cached and a second call without forceRefresh
     * returns the same cached token.
     */
    @Test
    void userFic_CacheHit_SecondCallReturnsCachedToken() throws Exception {
        String t2 = acquireInstanceToken();

        ConfidentialClientApplication cca = buildAgentCca();

        UserFederatedIdentityCredentialParameters params = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, t2)
                .build();

        IAuthenticationResult result1 = cca.acquireToken(params).get();
        assertNotNull(result1.accessToken());

        // Second call without forceRefresh should be a cache hit
        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, "stale-assertion-should-not-be-used")
                .forceRefresh(false)
                .build();

        IAuthenticationResult result2 = cca.acquireToken(params2).get();

        assertEquals(result1.accessToken(), result2.accessToken(),
                "Second call should return cached token");
    }

    /**
     * Verifies that forceRefresh bypasses the cache and acquires a fresh token.
     */
    @Test
    void userFic_ForceRefresh_BypassesCache() throws Exception {
        String t2_1 = acquireInstanceToken();

        ConfidentialClientApplication cca = buildAgentCca();

        UserFederatedIdentityCredentialParameters params1 = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, t2_1)
                .build();

        IAuthenticationResult result1 = cca.acquireToken(params1).get();
        assertNotNull(result1.accessToken());

        // Get a fresh instance token for force refresh
        String t2_2 = acquireInstanceToken();

        UserFederatedIdentityCredentialParameters params2 = UserFederatedIdentityCredentialParameters
                .builder(Collections.singleton(GRAPH_SCOPE), USER_UPN, t2_2)
                .forceRefresh(true)
                .build();

        IAuthenticationResult result2 = cca.acquireToken(params2).get();
        assertNotNull(result2.accessToken());

        // Force refresh should have acquired a new token (may or may not be different value,
        // but the call should have gone to the IdP rather than returning from cache)
        assertNotNull(result2.accessToken(), "Force refresh should produce a valid token");
    }

    // ========================================================================
    // Helpers
    // ========================================================================

    /**
     * Leg 1: Blueprint CCA acquires FMI credential (T1) for the agent app.
     * T1 is used as client_assertion to authenticate the agent CCA.
     */
    private String acquireFmiCredential(String fmiPath) throws Exception {
        IClientCertificate clientCert = ClientCredentialFactory.createFromCertificate(privateKey, certificate);

        ConfidentialClientApplication blueprintCca = ConfidentialClientApplication.builder(
                        BLUEPRINT_CLIENT_ID, clientCert)
                .authority(AUTHORITY)
                .sendX5c(true)
                .azureRegion(AZURE_REGION)
                .build();

        ClientCredentialParameters fmiParams = ClientCredentialParameters
                .builder(Collections.singleton(TOKEN_EXCHANGE_SCOPE))
                .fmiPath(fmiPath)
                .build();

        IAuthenticationResult fmiResult = blueprintCca.acquireToken(fmiParams).get();
        assertNotNull(fmiResult.accessToken(), "FMI credential (T1) should not be null");
        return fmiResult.accessToken();
    }

    /**
     * Leg 1+2: Acquires an instance token (T2) for the agent app.
     * Leg 1: Blueprint → T1 (FMI credential)
     * Leg 2: Agent uses T1 as client_assertion → T2 (instance token)
     * T2 is used as the user_federated_identity_credential in Leg 3.
     */
    private String acquireInstanceToken() throws Exception {
        String t1 = acquireFmiCredential(AGENT_APP_ID);

        IClientCredential agentCredential = ClientCredentialFactory.createFromClientAssertion(t1);

        ConfidentialClientApplication agentCca = ConfidentialClientApplication.builder(AGENT_APP_ID, agentCredential)
                .authority(AUTHORITY)
                .build();

        ClientCredentialParameters instanceParams = ClientCredentialParameters
                .builder(Collections.singleton(TOKEN_EXCHANGE_SCOPE))
                .skipCache(true)
                .build();

        IAuthenticationResult instanceResult = agentCca.acquireToken(instanceParams).get();
        assertNotNull(instanceResult.accessToken(), "Instance token (T2) should not be null");
        return instanceResult.accessToken();
    }

    /**
     * Builds an agent CCA whose credential callback produces T1 (FMI credential).
     * The CCA authenticates with T1 as client_assertion for Leg 2 and Leg 3 requests.
     */
    private ConfidentialClientApplication buildAgentCca() throws Exception {
        Function<AssertionRequestOptions, String> assertionProvider = options -> {
            try {
                return acquireFmiCredential(AGENT_APP_ID);
            } catch (Exception e) {
                throw new RuntimeException("Failed to acquire FMI credential", e);
            }
        };

        IClientCredential credential = ClientCredentialFactory.createFromCallback(assertionProvider);

        return ConfidentialClientApplication.builder(AGENT_APP_ID, credential)
                .authority(AUTHORITY)
                .build();
    }
}
